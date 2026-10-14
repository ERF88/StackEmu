import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID, createHash } from "node:crypto";
import { readFile } from "node:fs/promises";

const emulatorURL = process.env.STACKEMU_BROWSER_EMULATOR || "http://127.0.0.1:4566";
const emulator = new URL(emulatorURL);
if (
  !["127.0.0.1", "localhost", "ministack", "host.docker.internal"].includes(emulator.hostname) ||
  emulator.protocol !== "http:" ||
  emulator.port !== "4566"
) {
  throw new Error("Fixture cleanup must target the local emulator on port 4566");
}

async function audit(page, testInfo, label) {
  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"])
    .analyze();
  await testInfo.attach(`axe-${label}`, {
    body: JSON.stringify(results, null, 2),
    contentType: "application/json",
  });
  expect(
    results.violations,
    `${label}: ${JSON.stringify(results.violations.map((v) => ({ id: v.id, nodes: v.nodes.map((n) => n.target) })))}`,
  ).toEqual([]);
}

test("local navigation timing sample", async ({ page }, testInfo) => {
  const samples = [];
  await page.goto("/?lang=en");
  for (let index = 0; index < 5; index++) {
    await page.goto("/?lang=en");
    samples.push(
      await page.evaluate(() => {
        const navigation = performance.getEntriesByType("navigation")[0];
        return {
          responseMs: navigation.responseStart - navigation.requestStart,
          domContentLoadedMs: navigation.domContentLoadedEventEnd - navigation.startTime,
        };
      }),
    );
  }
  await testInfo.attach("navigation-sample", {
    body: JSON.stringify(
      {
        environment: "local packaged GUI, sequential warm navigations, no load simulation",
        samples,
      },
      null,
      2,
    ),
    contentType: "application/json",
  });
  expect(samples).toHaveLength(5);
});

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    test(`accessible navigation: ${language}, ${theme}`, async ({ page }, testInfo) => {
      await page.setViewportSize({ width: 390, height: 844 });
      await page.goto(`/?lang=${language}&theme=${theme}`);
      await expect(page.locator("html")).toHaveAttribute("lang", language);
      await page.keyboard.press("Tab");
      await expect(page.locator(".skip")).toBeFocused();
      await page.keyboard.press("Enter");
      await expect(page.locator("#content")).toBeFocused();
      await audit(page, testInfo, "dashboard-mobile");
      await expect
        .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
        .toBe(true);
      const refresh = page.locator("[hx-get]");
      const response = page.waitForResponse((r) => r.url().includes("/buckets?"));
      await refresh.focus();
      await page.keyboard.press("Enter");
      expect((await response).status()).toBe(200);
      await expect(refresh).toBeFocused();
      for (const path of ["/connection", "/components", "/s3/write?action=bucket"]) {
        await page.goto(`${path}${path.includes("?") ? "&" : "?"}lang=${language}&theme=${theme}`);
        await audit(page, testInfo, path);
      }
    });
  }
}

test("browser S3 writes, original file download, validation and security", async ({
  page,
  request,
}, testInfo) => {
  const bucket = `stackemu-browser-${randomUUID().replaceAll("-", "")}`;
  const filename = "ação +%.txt";
  const prefix = "browser/";
  const key = prefix + filename;
  const bytes = Buffer.from("StackEmu original bytes — browser fixture\n", "utf8");
  const hash = (value) => createHash("sha256").update(value).digest("hex");
  const browserErrors = [];
  page.on("pageerror", (error) => browserErrors.push(error.message));
  const consoleErrors = [];
  page.on("console", (message) => {
    if (message.type() === "error" && /Content Security Policy|Refused to/.test(message.text()))
      consoleErrors.push(message.text());
  });
  let created = false;
  try {
    await page.goto("/s3/write?action=bucket&lang=en");
    const rejected = await request.post("/s3/write/bucket", { form: { bucket, confirm: "yes" } });
    expect(rejected.status()).toBe(400);
    await page.getByLabel("Target bucket", { exact: true }).fill("INVALID");
    await page.getByRole("checkbox").check();
    await page.getByRole("button", { name: "Create bucket", exact: true }).click();
    await expect(page.locator("#mutation-bucket")).toHaveAttribute("aria-invalid", "true");
    await expect(page.locator("#bucket-error")).toBeVisible();
    await audit(page, testInfo, "validation");
    await page.getByLabel("Target bucket", { exact: true }).fill(bucket);
    await page.getByRole("checkbox").check();
    await page.getByRole("button", { name: "Create bucket", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Operation completed" })).toBeVisible();
    created = true;
    await page.getByRole("link", { name: "Open created resource" }).click();
    await audit(page, testInfo, "empty-bucket");
    await page.getByRole("link", { name: "Create folder marker", exact: true }).click();
    await page.getByLabel("Folder marker prefix").fill(prefix);
    await page.getByRole("checkbox").check();
    await page.getByRole("button", { name: "Create folder marker", exact: true }).click();
    await page.getByRole("link", { name: "Open created resource" }).click();
    await page.getByRole("link", { name: "Upload new object", exact: true }).click();
    await expect(page.locator('input[name="key"]')).toHaveCount(0);
    await audit(page, testInfo, "upload");
    await page
      .getByLabel("Select a file", { exact: true })
      .setInputFiles({ name: filename, mimeType: "text/plain", buffer: bytes });
    await page.getByRole("checkbox").check();
    await page.getByRole("button", { name: "Upload new object", exact: true }).click();
    await page.getByRole("link", { name: "Open created resource" }).click();
    await expect(page.locator(".object-key")).toHaveText(key);
    await audit(page, testInfo, "object");
    const downloadPromise = page.waitForEvent("download");
    await page.getByRole("link", { name: "Download as attachment" }).click();
    const download = await downloadPromise;
    expect(download.suggestedFilename()).toBe(filename);
    expect(hash(await readFile(await download.path()))).toBe(hash(bytes));
    const response = await request.get(
      `/s3/download?bucket=${bucket}&key=${encodeURIComponent(key)}`,
    );
    expect(response.headers()["content-type"]).toContain("text/plain");
    expect(response.headers()["content-disposition"]).toContain("attachment");
    expect(response.headers()["x-content-type-options"]).toBe("nosniff");
    expect(response.headers()["content-security-policy"]).toContain("frame-ancestors 'none'");
    await page.getByRole("link", { name: "Back to objects" }).click();
    await expect(page.getByRole("link", { name: key, exact: true })).toBeVisible();
    expect(browserErrors).toEqual([]);
    expect(consoleErrors).toEqual([]);
  } finally {
    // Only exact UUID-owned fixture keys; never list or delete unrelated resources.
    if (created) {
      for (const ownedKey of [key, prefix]) {
        const response = await request.delete(
          `${emulatorURL}/${bucket}/${encodeURIComponent(ownedKey)}`,
        );
        expect(response.ok(), `fixture cleanup: ${ownedKey}`).toBe(true);
      }
      const response = await request.delete(`${emulatorURL}/${bucket}`);
      expect(response.ok(), "fixture bucket cleanup").toBe(true);
    }
  }
});
