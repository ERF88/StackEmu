import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulator = "http://127.0.0.1:4566";
const fixtures = new Map();
const original = "original explorer bytes\n";

test.beforeEach(async ({ request }, info) => {
  const bucket = `stackemu-explorer-${randomUUID().replaceAll("-", "")}`;
  const keys = [
    "samples/report.txt",
    `samples/${"ação-".repeat(18)} +% <tag>.json`,
    "samples/nested/",
  ];
  expect((await request.put(`${emulator}/${bucket}`)).status()).toBe(200);
  fixtures.set(info.testId, { bucket, keys });
  for (const key of keys)
    expect(
      (
        await request.put(`${emulator}/${bucket}/${encodeURIComponent(key)}`, {
          data: key.endsWith("/") ? "" : original,
          headers: { "Content-Type": "text/plain" },
        })
      ).status(),
    ).toBe(200);
});

test.afterEach(async ({ playwright }, info) => {
  const fixture = fixtures.get(info.testId);
  if (!fixture) return;
  const cleanup = await playwright.request.newContext();
  try {
    for (const key of fixture.keys)
      expect(
        (await cleanup.delete(`${emulator}/${fixture.bucket}/${encodeURIComponent(key)}`)).status(),
      ).toBe(204);
    expect((await cleanup.delete(`${emulator}/${fixture.bucket}`)).status()).toBe(204);
  } finally {
    fixtures.delete(info.testId);
    await cleanup.dispose();
  }
});

for (const lang of ["en", "pt-BR"]) {
  for (const theme of ["light", "dark"]) {
    test(`compact explorer: ${lang}, ${theme}`, async ({ page, request }, info) => {
      const { bucket, keys } = fixtures.get(info.testId);
      const listing = `/s3/bucket?bucket=${bucket}&prefix=samples%2F&lang=${lang}&theme=${theme}&density=compact`;
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.setViewportSize({ width: 1366, height: 768 });
      await page.goto(`/s3?lang=${lang}&theme=${theme}`);
      const bucketRow = page.locator(".bucket-table tbody tr").filter({ hasText: bucket });
      const bucketNameBounds = await bucketRow.locator("th a").boundingBox();
      const bucketActionBounds = await bucketRow.locator(".icon-action").first().boundingBox();
      expect(
        Math.abs(
          bucketNameBounds.y +
            bucketNameBounds.height / 2 -
            bucketActionBounds.y -
            bucketActionBounds.height / 2,
        ),
      ).toBeLessThanOrEqual(1);
      await page.screenshot({ path: info.outputPath("buckets-1366.png"), fullPage: true });
      await page.goto(listing);
      const table = page.locator(".object-table");
      expect((await table.boundingBox()).y).toBeLessThan(340);
      const row = table.locator("tbody tr").filter({ hasText: "samples/report.txt" });
      const actions = row.locator(".resource-action-links .icon-action");
      await expect(actions).toHaveCount(3);
      for (const action of await actions.all()) {
        expect(await action.getAttribute("aria-label")).toContain("samples/report.txt");
        const bounds = await action.boundingBox();
        expect(bounds.height).toBeGreaterThanOrEqual(44);
        expect(bounds.width).toBeGreaterThanOrEqual(44);
      }
      await actions.first().focus();
      await expect(actions.first().locator(".action-tooltip")).toBeVisible();
      await page.keyboard.press("Escape");
      await expect(actions.first()).toBeFocused();
      await expect(actions.first().locator(".action-tooltip")).toBeHidden();
      const details = row.locator(".resource-action-links [data-object-detail]");
      await details.click();
      await expect(page.locator("#object-drawer .object-key")).toHaveText("samples/report.txt");
      await page.keyboard.press("Escape");
      await expect(details).toBeFocused();
      const download = await request.get(await row.locator(".table-action").getAttribute("href"));
      expect(download.status()).toBe(200);
      expect(await download.text()).toBe(original);
      expect(download.headers()["content-disposition"]).toContain("report.txt");

      const folder = page.locator('.page-actions a[href*="action=folder"]');
      await folder.click();
      await expect(page.locator("#action-dialog #mutation-bucket")).toHaveValue(bucket);
      await expect(page.locator("#action-dialog #mutation-key")).toHaveValue("samples/");
      await page.keyboard.press("Escape");
      await expect(folder).toBeFocused();
      await row.locator('a[href^="/s3/delete-object?"]').click();
      await expect(page.locator("#action-dialog .object-key")).toHaveText("samples/report.txt");
      await expect(page.locator("#action-dialog [data-action-close]")).toBeFocused();
      await page.keyboard.press("Escape");
      expect(
        (await request.get(`${emulator}/${bucket}/${encodeURIComponent(keys[0])}`)).status(),
      ).toBe(200);

      await expect(
        page.locator(".explorer-help, .bucket-metadata, .explorer-information"),
      ).toHaveCount(0);
      await expect(page.locator(".resource-pagination")).toHaveCount(0);
      const nameBounds = await row.locator(".object-name a").boundingBox();
      const actionBounds = await actions.first().boundingBox();
      const longRow = table.locator("tbody tr").filter({ hasText: keys[1] });
      const longName = await longRow.locator(".object-name a").boundingBox();
      const longAction = await longRow.locator(".icon-action").first().boundingBox();
      expect(
        Math.abs(longName.y + longName.height / 2 - longAction.y - longAction.height / 2),
      ).toBeLessThanOrEqual(1);
      expect(
        Math.abs(nameBounds.y + nameBounds.height / 2 - actionBounds.y - actionBounds.height / 2),
      ).toBeLessThanOrEqual(1);
      const titleBounds = await page.locator(".s3-resource-toolbar h2").boundingBox();
      const toolbarBounds = await page.locator(".s3-resource-toolbar .page-actions").boundingBox();
      expect(
        Math.abs(
          titleBounds.y + titleBounds.height / 2 - toolbarBounds.y - toolbarBounds.height / 2,
        ),
      ).toBeLessThanOrEqual(1);
      await expect(table.locator("code").filter({ hasText: keys[1] })).toHaveText(keys[1]);
      for (const width of [1366, 390, 1440]) {
        await page.setViewportSize({ width, height: 768 });
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        await expect(page.locator(".page-actions .primary")).toBeVisible();
        expect(
          (
            await new AxeBuilder({ page })
              .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
              .analyze()
          ).violations,
        ).toEqual([]);
        await page.screenshot({ path: info.outputPath(`explorer-${width}.png`), fullPage: true });
      }
      await page.locator('.page-actions a[href^="/s3/delete-bucket?"]').click();
      await expect(page.locator("#action-dialog form")).toHaveCount(0);
      await expect(page.locator("#action-dialog")).toContainText(
        lang === "en" ? "not empty" : "não está vazio",
      );
      expect(
        (await request.get(`${emulator}/${bucket}/${encodeURIComponent(keys[0])}`)).status(),
      ).toBe(200);
    });
  }
}

test("refresh retains exact prefix, opaque cursor and preferences", async ({
  page,
  request,
}, info) => {
  const fixture = fixtures.get(info.testId);
  for (let i = 0; i < 51; i++) {
    const key = `samples/rows/row-${String(i).padStart(2, "0")}.txt`;
    fixture.keys.push(key);
    expect(
      (
        await request.put(`${emulator}/${fixture.bucket}/${encodeURIComponent(key)}`, {
          data: original,
        })
      ).status(),
    ).toBe(200);
  }
  await page.goto(
    `/s3/bucket?bucket=${fixture.bucket}&prefix=samples%2Frows%2F&lang=pt-BR&theme=dark&density=compact`,
  );
  await page.locator("#object-filter").fill("row-50.txt");
  await expect(page.locator(".object-table tbody tr:visible")).toHaveCount(0);
  await expect(page.locator("[data-filter-empty]")).toBeVisible();
  await page.locator("#next-page").click();
  await expect(page.locator("#object-filter")).toHaveValue("");
  await expect(page.locator(".object-table tbody tr:visible")).toHaveCount(1);
  await expect(page.locator(".resource-pagination a[href]:not(#next-page)")).toHaveCount(1);
  const current = new URL(page.url());
  expect(current.searchParams.get("token")).toBeTruthy();
  const refresh = page.locator('.page-actions a[title="Atualizar lista"]');
  const target = new URL(await refresh.getAttribute("href"), page.url());
  expect(target.href).toBe(current.href);
  await refresh.focus();
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(current.href);
  await expect(page.locator(".object-table tbody tr")).toHaveCount(1);
  await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
});
