import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

// Fixtures and cleanup are restricted to this UUID bucket on local MiniStack.
const emulatorURL = process.env.STACKEMU_BROWSER_EMULATOR || "http://127.0.0.1:4566";
const emulator = new URL(emulatorURL);
if (
  emulator.protocol !== "http:" ||
  emulator.port !== "4566" ||
  !["127.0.0.1", "localhost", "ministack", "host.docker.internal"].includes(emulator.hostname)
) {
  throw new Error("Density fixtures may only target the local emulator");
}
const bucket = `stackemu-density-${randomUUID().replaceAll("-", "")}`;
const longKey = `samples/${"ação-".repeat(35)} +% <tag>.txt`;
const body = "original density fixture\n";
const keys = [
  longKey,
  "samples/nested/",
  ...Array.from({ length: 55 }, (_, i) => `samples/row-${String(i).padStart(2, "0")}.txt`),
];
const objectURL = (key) =>
  `${emulatorURL}/${bucket}/${key.split("/").map(encodeURIComponent).join("/")}`;
const bucketPath = `/s3/bucket?bucket=${bucket}&prefix=samples%2F`;
let created = false;

test.beforeAll(async ({ request }) => {
  expect((await request.put(`${emulatorURL}/${bucket}`)).status()).toBe(200);
  created = true;
  for (const key of keys) {
    expect(
      (
        await request.put(objectURL(key), {
          data: key.endsWith("/") ? "" : body,
          headers: { "Content-Type": "text/plain" },
        })
      ).status(),
    ).toBe(200);
  }
});

test.afterAll(async ({ request }) => {
  if (!created) return;
  for (const key of keys) expect((await request.delete(objectURL(key))).status()).toBe(204);
  expect((await request.delete(`${emulatorURL}/${bucket}`)).status()).toBe(204);
});

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    test(`Legacy density URLs and S3 navigation: ${language}, ${theme}`, async ({
      page,
    }, testInfo) => {
      await page.setViewportSize({ width: 1440, height: 1000 });
      await page.goto(`${bucketPath}&lang=${language}&theme=${theme}`);
      await expect(page.locator("html")).toHaveAttribute("data-density", "comfortable");
      const row = page.locator(".object-table tbody tr").filter({ hasText: "samples/row-00.txt" });
      const comfortableHeight = (await row.boundingBox()).height;
      await page.screenshot({
        path: testInfo.outputPath("comfortable-long-key.png"),
        fullPage: true,
      });
      await page.goto(`${bucketPath}&lang=${language}&theme=${theme}&density=compact`);
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      expect((await row.boundingBox()).height).toBe(comfortableHeight);
      await expect(page.locator("[data-density-choice]")).toHaveCount(0);
      await expect(page.locator(".object-table code").filter({ hasText: longKey })).toHaveText(
        longKey,
      );
      await expect(page.locator(".object-table code").filter({ hasText: longKey })).toHaveCSS(
        "white-space",
        "pre-wrap",
      );
      await page.screenshot({ path: testInfo.outputPath("compact-long-key.png"), fullPage: true });
      const action = row.locator(".table-action");
      await expect(action).toBeVisible();
      expect((await action.boundingBox()).height).toBeGreaterThanOrEqual(44);
      await row.locator(".object-name [data-object-detail]").focus();
      await expect(action).toHaveCSS("text-decoration-line", "underline");
      await expect(row.locator("svg").first()).toBeVisible();
      const region = page.locator(".object-list-panel .table-scroll");
      await region.evaluate((element) => {
        element.scrollTop = 400;
      });
      expect(await region.evaluate((element) => element.scrollTop)).toBeGreaterThan(0);
      const header = await page.locator(".object-table thead th").first().boundingBox();
      const bounds = await region.boundingBox();
      expect(Math.abs(header.y - bounds.y)).toBeLessThanOrEqual(1);
      for (const width of [390, 1440]) {
        await page.setViewportSize({ width, height: 1000 });
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        expect(
          (
            await new AxeBuilder({ page })
              .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"])
              .analyze()
          ).violations,
        ).toEqual([]);
        await page.screenshot({
          path: testInfo.outputPath(`compact-${width}.png`),
          fullPage: true,
        });
      }
      for (const link of await page.locator(".language-controls a, .theme-controls a").all()) {
        const href = new URL(await link.getAttribute("href"), page.url());
        expect(href.searchParams.getAll("density")).toHaveLength(1);
        expect(href.searchParams.get("prefix")).toBe("samples/");
      }
      const next = page.locator("#next-page");
      await next.click();
      const token = new URL(page.url()).searchParams.get("token");
      expect(token).toBeTruthy();
      const comfortableUrl = new URL(page.url());
      comfortableUrl.searchParams.delete("density");
      await page.goto(comfortableUrl.href);
      expect(new URL(page.url()).searchParams.get("token")).toBe(token);
      await expect(page.locator("html")).toHaveAttribute("data-density", "comfortable");
      await page.goto(`${bucketPath}&lang=${language}&theme=${theme}&density=compact`);
      const object = page.locator("[data-object-detail]").filter({ hasText: longKey });
      await object.click();
      await expect(page.locator("#object-drawer")).toBeVisible();
      await expect(object).toHaveAttribute("href", /density=compact/);
      await page.goto(await object.getAttribute("href"));
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      const preview = page.locator('a[href^="/s3/preview?"]');
      await expect(preview).toHaveAttribute("href", /density=compact/);
      await preview.click();
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      const download = page.locator('a[href^="/s3/download?"]');
      const response = await page.request.get(await download.getAttribute("href"));
      expect(response.status()).toBe(200);
      expect(await response.text()).toBe(body);
      const prefix = `v3-${language.toLowerCase()}-${theme}/`;
      keys.push(prefix);
      await page.goto(
        `/s3/write?action=folder&bucket=${bucket}&prefix=${encodeURIComponent(prefix)}&lang=${language}&theme=${theme}&density=compact`,
      );
      await expect(page.locator('input[name="density"]')).toHaveValue("compact");
      await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
      await page.locator('button[type="submit"]').click();
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      await expect(page.locator("main a.button.primary")).toHaveAttribute(
        "href",
        /density=compact/,
      );
    });
  }
}

for (const language of ["en", "pt-BR"]) {
  test(`V3 no-JS preferences and validation: ${language}`, async ({ browser, baseURL }) => {
    const context = await browser.newContext({ baseURL, javaScriptEnabled: false });
    try {
      const page = await context.newPage();
      await page.goto(`/s3?lang=${language}&theme=light&density=untrusted`);
      await expect(page.locator("html")).toHaveAttribute("data-density", "comfortable");
      await expect(page.locator("[data-density-choice]")).toHaveCount(0);
      await page.goto(`/s3?lang=${language}&theme=light&density=compact`);
      await page.locator("[data-refresh-panel]").click();
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      await page.locator(".preferences-trigger").click();
      await page
        .getByRole("link", { name: language === "en" ? "Português" : "English", exact: true })
        .click();
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      await page.goto(`/s3/write?action=bucket&lang=${language}&theme=light&density=compact`);
      await page.locator("#mutation-bucket").fill("INVALID");
      await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
      await page.locator('button[type="submit"]').click();
      await expect(page.locator("#mutation-bucket")).toHaveAttribute("aria-invalid", "true");
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      await expect(page.locator('input[name="density"]')).toHaveValue("compact");
      expect((await context.cookies()).some((cookie) => /density/i.test(cookie.name))).toBe(false);
    } finally {
      await context.close();
    }
  });
}
