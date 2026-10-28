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
      await page.locator(".service-card a").click();
      await audit(page, testInfo, "s3-mobile");
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
      const policy = (await page.request.get("/components")).headers()["content-security-policy"];
      for (const scene of ["unavailable", "error"]) {
        const html = await readFile(
          new URL(
            `../../work/browser-fixtures/${scene}-${language}-${theme}.html`,
            import.meta.url,
          ),
          "utf8",
        );
        await page.route("**/*", async (route) =>
          route.request().isNavigationRequest()
            ? route.fulfill({
                status: scene === "error" ? 503 : 200,
                contentType: "text/html; charset=utf-8",
                headers: { "Content-Security-Policy": policy },
                body: html,
              })
            : route.fulfill({ response: await route.fetch() }),
        );
        await page.goto(`/?lang=${language}&theme=${theme}`);
        await expect(page.locator(".sidebar")).toHaveCSS("display", "flex");
        await audit(page, testInfo, scene);
        if (scene === "unavailable") {
          await expect(page.locator("[data-bucket-count]")).toHaveCount(0);
          await expect(page.locator(".service-connection .error")).toBeVisible();
        }
        await page.unroute("**/*");
      }
    });
  }
}

test("browser S3 writes, original file download, validation and security", async ({
  page,
  request,
  browser,
  baseURL,
}, testInfo) => {
  const bucket = `stackemu-browser-${randomUUID().replaceAll("-", "")}`;
  const filename = `ação +% ${"long-name-".repeat(16)}.txt`;
  const prefix = "browser/";
  const key = prefix + filename;
  const bytes = Buffer.alloc(1536, "a");
  Buffer.from("StackEmu original bytes — browser fixture\n", "utf8").copy(bytes);
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
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    await page.getByRole("button", { name: "Create bucket", exact: true }).click();
    await expect(page.locator("#mutation-bucket")).toHaveAttribute("aria-invalid", "true");
    await expect(page.locator("#bucket-error")).toBeVisible();
    await audit(page, testInfo, "validation");
    await page.getByLabel("Target bucket", { exact: true }).fill(bucket);
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    await page.getByRole("button", { name: "Create bucket", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Operation completed" })).toBeVisible();
    created = true;
    await page.getByRole("link", { name: "Open created resource" }).click();
    await audit(page, testInfo, "empty-bucket");
    await page.getByRole("link", { name: "Create folder", exact: true }).click();
    await page.getByLabel("Folder path").fill(prefix);
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    await page.getByRole("button", { name: "Create folder", exact: true }).click();
    await page.getByRole("link", { name: "Open created resource" }).click();
    await page.getByRole("link", { name: "Upload new object", exact: true }).click();
    await expect(page.locator('input[name="key"]')).toHaveCount(0);
    await audit(page, testInfo, "upload");
    await page
      .getByLabel("Select a file", { exact: true })
      .setInputFiles({ name: filename, mimeType: "text/plain", buffer: bytes });
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
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
    for (const language of ["en", "pt-BR"]) {
      for (const theme of ["dark", "light"]) {
        const query = `bucket=${bucket}&prefix=${encodeURIComponent(prefix)}&lang=${language}&theme=${theme}`;
        await page.goto(`/s3/bucket?${query}`);
        const tableRegion = page.locator(".table-scroll").last();
        await expect(tableRegion).toHaveAttribute("tabindex", "0");
        await expect(tableRegion.locator("caption")).toHaveCount(1);
        await expect(
          tableRegion.locator('th[scope="row"] a').filter({ hasText: key }),
        ).toBeVisible();
        await expect(page.locator('.breadcrumbs [aria-current="page"]')).toHaveCount(1);
        await expect(tableRegion.locator("thead th").first()).toHaveCSS("position", "sticky");
        const objectRow = tableRegion.locator("tbody tr").filter({ hasText: key });
        await expect(objectRow.locator(".file-kind")).toHaveText(
          language === "en" ? "Text" : "Texto",
        );
        await expect(objectRow.locator("td").first()).toHaveText(
          language === "en" ? "1.5 KiB (1,536 B)" : "1,5 KiB (1.536 B)",
        );
        const quickDownload = objectRow.locator(".table-action");
        await expect(quickDownload).toHaveAccessibleName(
          `${language === "en" ? "Download as attachment" : "Baixar como anexo"}: ${key}`,
        );
        await expect(quickDownload).toHaveCSS("min-height", "48px");
        await audit(page, testInfo, `e4-bucket-${language}-${theme}`);
        for (const width of [360, 390, 1440, 1920]) {
          await page.setViewportSize({ width, height: 900 });
          expect(
            await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
          ).toBe(true);
          if (width === 390) {
            await tableRegion.focus();
            await page.keyboard.press("ArrowRight");
            await expect
              .poll(() => tableRegion.evaluate((element) => element.scrollLeft))
              .toBeGreaterThan(0);
          }
          if (width === 390 || width === 1440) {
            await page.screenshot({
              path: testInfo.outputPath(`e4-bucket-${language}-${theme}-${width}.png`),
              fullPage: true,
            });
          }
        }
        await page.goto(
          `/s3/object?bucket=${bucket}&key=${encodeURIComponent(key)}&lang=${language}&theme=${theme}`,
        );
        await expect(page.locator(".object-key")).toHaveText(key);
        await expect(page.locator(".object-panel-actions .primary")).toBeVisible();
        await audit(page, testInfo, `e4-object-${language}-${theme}`);
        for (const width of [360, 390, 1440, 1920]) {
          await page.setViewportSize({ width, height: 900 });
          expect(
            await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
          ).toBe(true);
          if (width === 390 || width === 1440) {
            await page.screenshot({
              path: testInfo.outputPath(`e4-object-${language}-${theme}-${width}.png`),
              fullPage: true,
            });
          }
        }
      }
    }
    const nativeContext = await browser.newContext({ javaScriptEnabled: false, baseURL });
    try {
      const nativePage = await nativeContext.newPage();
      await nativePage.goto(
        `/s3/bucket?bucket=${bucket}&prefix=${encodeURIComponent(prefix)}&lang=en`,
      );
      const nativeDownloadPromise = nativePage.waitForEvent("download");
      await nativePage
        .getByRole("link", { name: `Download as attachment: ${key}`, exact: true })
        .click();
      const nativeDownload = await nativeDownloadPromise;
      expect(nativeDownload.suggestedFilename()).toBe(filename);
      expect(hash(await readFile(await nativeDownload.path()))).toBe(hash(bytes));
      await nativePage.getByRole("link", { name: key, exact: true }).click();
      await expect(nativePage).toHaveURL(/\/s3\/object\?/);
      await expect(nativePage.locator(".object-key")).toHaveText(key);
    } finally {
      await nativeContext.close();
    }
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
