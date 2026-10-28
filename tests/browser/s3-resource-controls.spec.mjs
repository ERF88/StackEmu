import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulator = "http://127.0.0.1:4566";
const fixtures = new Map();
const keys = ["samples/ação +%.txt", "samples/other.txt", "samples/<img>.txt"];

test.beforeEach(async ({ request }, info) => {
  const bucket = `stackemu-controls-${randomUUID().replaceAll("-", "")}`;
  expect((await request.put(`${emulator}/${bucket}`)).status()).toBe(200);
  fixtures.set(info.testId, bucket);
  for (const key of keys)
    expect(
      (
        await request.put(`${emulator}/${bucket}/${encodeURIComponent(key)}`, {
          data: key,
          headers: { "Content-Type": "text/plain" },
        })
      ).status(),
    ).toBe(200);
});

test.afterEach(async ({ request }, info) => {
  const bucket = fixtures.get(info.testId);
  if (!bucket) return;
  for (const key of keys)
    expect(
      (await request.delete(`${emulator}/${bucket}/${encodeURIComponent(key)}`)).status(),
    ).toBe(204);
  expect((await request.delete(`${emulator}/${bucket}`)).status()).toBe(204);
  fixtures.delete(info.testId);
});

for (const lang of ["en", "pt-BR"]) {
  for (const theme of ["light", "dark"]) {
    test(`contextual S3 controls: ${lang}, ${theme}`, async ({ page }, info) => {
      const bucket = fixtures.get(info.testId);
      const query = `lang=${lang}&theme=${theme}&density=compact`;
      await page.goto(`/s3?${query}`);
      const bucketFilter = page.locator("#bucket-filter");
      await bucketFilter.fill(bucket);
      await expect(page.locator(".bucket-table tbody tr:visible")).toHaveCount(1);
      await page.locator("[data-refresh-panel]").click();
      await expect(bucketFilter).toHaveValue(bucket);
      await bucketFilter.fill(bucket);
      await expect(page.locator(".bucket-table tbody tr:visible")).toHaveCount(1);
      await page.goto(`/s3/bucket?bucket=${bucket}&prefix=samples%2F&${query}`);
      const filter = page.locator("#object-filter");
      const rows = page.locator(".object-table tbody tr:visible");
      let requests = 0;
      page.on("request", (request) => {
        if (new URL(request.url()).pathname.startsWith("/s3")) requests += 1;
      });
      await filter.fill("AÇÃO +%");
      await expect(rows).toHaveCount(1);
      await expect(rows.locator("th code")).toHaveText(keys[0]);
      await expect(page.locator("[data-filter-count]")).toHaveText("1 / 3");
      await filter.fill("not-present");
      await expect(rows).toHaveCount(0);
      await expect(page.locator("[data-filter-empty]")).toBeVisible();
      await filter.fill("<img>");
      await expect(rows).toHaveCount(1);
      await expect(rows.locator("th img")).toHaveCount(0);
      await expect(page.locator("[data-filter-clear]")).toHaveCSS("margin-top", "0px");
      await page.locator("[data-filter-clear]").click();
      await expect(filter).toBeFocused();
      await expect(rows).toHaveCount(3);
      expect(requests).toBe(0);
      await expect(page.locator(".s3-resource-toolbar .page-actions a")).toHaveCount(4);
      await expect(
        page.locator('.bucket-explorer-heading a[href^="/s3/delete-bucket?"]'),
      ).toHaveCount(1);
      for (const width of [390, 1366, 1440]) {
        await page.setViewportSize({ width, height: 900 });
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
        await page.screenshot({ path: info.outputPath(`controls-${width}.png`), fullPage: true });
      }
      const download = new URL(
        await rows.first().locator('a[href^="/s3/download?"]').getAttribute("href"),
        page.url(),
      );
      expect(download.searchParams.get("bucket")).toBe(bucket);
      expect(download.searchParams.get("theme")).toBe(theme);
    });
  }
  test.describe(`native S3 controls: ${lang}`, () => {
    test.use({ javaScriptEnabled: false });
    test("filters stay hidden and canonical actions remain", async ({ page }, info) => {
      const bucket = fixtures.get(info.testId);
      await page.goto(`/s3/bucket?bucket=${bucket}&prefix=samples%2F&lang=${lang}`);
      await expect(page.locator("[data-resource-filter]")).toBeHidden();
      await expect(page.locator(".object-table tbody tr")).toHaveCount(3);
      await expect(page.locator('.page-actions a[href*="action=upload"]')).toBeVisible();
    });
  });
}
