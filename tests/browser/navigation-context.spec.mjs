import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const endpoint = "http://127.0.0.1:4566";
const fixtures = new Map();
const key = "assets/2026/ação +% <sample>.txt";
const other = "assets/2026/not-preview.docx";
test.beforeEach(async ({ request }, info) => {
  const fixture = {
    bucket: `stackemu-navigation-${randomUUID().replaceAll("-", "")}`,
    keys: [key, other],
  };
  fixtures.set(info.testId, fixture);
  expect((await request.put(`${endpoint}/${fixture.bucket}`)).status()).toBe(200);
  for (const name of fixture.keys)
    expect(
      (
        await request.put(`${endpoint}/${fixture.bucket}/${encodeURIComponent(name)}`, {
          data: "navigation fixture",
          headers: {
            "Content-Type": name.endsWith(".txt") ? "text/plain" : "application/octet-stream",
          },
        })
      ).status(),
    ).toBe(200);
});
test.afterEach(async ({ request }, info) => {
  const fixture = fixtures.get(info.testId);
  if (!fixture) return;
  for (const name of fixture.keys)
    expect(
      (await request.delete(`${endpoint}/${fixture.bucket}/${encodeURIComponent(name)}`)).status(),
    ).toBe(204);
  expect((await request.delete(`${endpoint}/${fixture.bucket}`)).status()).toBe(204);
  fixtures.delete(info.testId);
});
function listing(bucket, lang = "pt-BR", theme = "dark") {
  return `/s3/bucket?bucket=${bucket}&prefix=assets%2F2026%2F&lang=${lang}&theme=${theme}&density=compact`;
}
function object(bucket) {
  return `/s3/object?bucket=${bucket}&key=${encodeURIComponent(key)}&lang=pt-BR&theme=dark&density=compact`;
}

for (const lang of ["en", "pt-BR"])
  for (const theme of ["light", "dark"]) {
    test(`inspector round trips retain geometry: ${lang}, ${theme}`, async ({ page }, info) => {
      const { bucket } = fixtures.get(info.testId);
      await page.emulateMedia({ reducedMotion: "reduce" });
      for (const width of [390, 1366, 1440]) {
        await page.setViewportSize({ width, height: 900 });
        await page.goto(listing(bucket, lang, theme));
        await page.locator("#object-filter").fill("ação +%");
        await page.locator(".object-table [data-object-detail]").first().click();
        const drawer = page.locator("#object-drawer");
        await expect(drawer.locator("#object-name")).toBeVisible();
        const initial = await drawer.boundingBox();
        for (const path of ["/s3/object-metadata?", "/s3/versions?", "/s3/preview?"]) {
          await drawer.locator(`a[href^="${path}"]`).first().click();
          await expect(drawer.locator("#object-inspector-view")).toBeVisible();
          expect((await drawer.boundingBox()).width).toBe(initial.width);
          expect(
            await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
          ).toBe(true);
          await expect(drawer.locator("h1")).toHaveCSS("font-size", "20px");
          expect(new URL(page.url()).pathname).toBe("/s3/bucket");
          await drawer.locator(".back-navigation a").click();
          await expect(drawer.locator("#object-name")).toBeVisible();
          await expect(drawer.locator("#object-name")).toHaveCSS("font-size", "20px");
        }
        expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
        await page.screenshot({ path: info.outputPath(`inspector-${width}.png`), fullPage: true });
        await page.keyboard.press("Escape");
        await expect(drawer).not.toBeVisible();
        await expect(page.locator("#object-filter")).toHaveValue("ação +%");
        await expect(page.locator(".object-table [data-object-detail]").first()).toBeFocused();
      }
    });
  }

test("Back, Forward, reload and canonical return retain the listing", async ({ page }, info) => {
  const { bucket } = fixtures.get(info.testId);
  await page.goto(listing(bucket));
  await page.locator("#object-filter").fill("ação +%");
  await page.locator(".object-table [data-object-detail]").first().click();
  const canonicalHref = await page
    .locator(".object-table [data-object-detail]")
    .first()
    .getAttribute("href");
  await page.locator('#object-drawer a[href^="/s3/object-metadata?"]').click();
  await expect(page.locator("#object-inspector-view")).toBeVisible();
  await page.goBack();
  await expect(page.locator("#object-drawer #object-name")).toBeVisible();
  await page.goForward();
  await expect(page.locator("#object-inspector-view")).toBeVisible();
  await page.reload();
  await expect(page.locator("#object-inspector-view")).toBeVisible();
  await page.locator("#object-drawer .back-navigation a").click();
  await expect(page.locator("[data-drawer-page]")).toHaveCount(0);
  await page.goto(canonicalHref);
  await expect(page.locator(".object-detail-panel")).toBeVisible();
  await page.goBack();
  await expect(page.locator("#object-drawer #object-name")).toBeVisible();
  await page.goForward();
  await expect(page.locator(".object-detail-panel")).toBeVisible();
  await page.locator(".back-navigation a").click();
  expect(new URL(page.url()).searchParams.get("prefix")).toBe("assets/2026/");
  await expect(page.locator("#object-filter")).toHaveValue("ação +%");
});

test("canonical return retains the opaque cursor", async ({ page, request }, info) => {
  const fixture = fixtures.get(info.testId);
  for (let i = 0; i < 51; i++) {
    const name = `pages/row-${String(i).padStart(2, "0")}.txt`;
    fixture.keys.push(name);
    expect(
      (
        await request.put(`${endpoint}/${fixture.bucket}/${encodeURIComponent(name)}`, {
          data: "cursor",
          headers: { "Content-Type": "text/plain" },
        })
      ).status(),
    ).toBe(200);
  }
  await page.goto(`/s3/bucket?bucket=${fixture.bucket}&prefix=pages%2F&lang=en&theme=light`);
  await page.locator("#next-page").click();
  const token = new URL(page.url()).searchParams.get("token");
  expect(token).toBeTruthy();
  await page.locator("#object-filter").fill("row-50");
  await page.locator(".object-table [data-object-detail]").first().click();
  await page.goto(
    await page.locator(".object-table [data-object-detail]").first().getAttribute("href"),
  );
  await expect(page.locator(".object-detail-panel")).toBeVisible();
  await page.locator(".back-navigation a").click();
  expect(new URL(page.url()).searchParams.get("token")).toBe(token);
  await expect(page.locator("#object-filter")).toHaveValue("row-50");
});

test("preferences keep the share target and require explicit draft discard", async ({
  page,
}, info) => {
  const { bucket } = fixtures.get(info.testId);
  await page.goto(object(bucket).replace("/s3/object?", "/s3/share?"));
  await page.locator(".preferences-trigger").click();
  await page.locator('.theme-controls a[href*="theme=light"]').click();
  await expect(page.locator('input[name="bucket"]')).toHaveValue(bucket);
  await expect(page.locator('input[name="key"]')).toHaveValue(key);
  await page.locator(".back-navigation a").click();
  await expect(page.locator(".object-detail-panel")).toBeVisible();
  await page.goto(object(bucket).replace("/s3/object?", "/s3/transfer?"));
  await page.locator("#transfer-key").fill("unsent-draft.txt");
  await page.locator(".preferences-trigger").click();
  await page.locator('.theme-controls a[href*="theme=light"]').click();
  await expect(page.locator("#navigation-guard")).toBeVisible();
  await page.locator("[data-keep-editing]").click();
  await expect(page.locator("#transfer-key")).toHaveValue("unsent-draft.txt");
  await expect(page.locator("#transfer-key")).toBeFocused();
  await page.locator(".preferences-trigger").click();
  await page.locator('.theme-controls a[href*="theme=light"]').click();
  await page.locator("[data-discard-changes]").click();
  await expect(page.locator("#transfer-key")).toHaveValue(key);
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
});

test("untrusted inspector hashes never invoke mutation or external routes", async ({
  page,
}, info) => {
  const { bucket } = fixtures.get(info.testId);
  const forbidden = [];
  page.on("request", (request) => {
    const url = new URL(request.url());
    if (url.pathname.startsWith("/s3/delete") || url.hostname === "example.invalid")
      forbidden.push(url.href);
  });
  for (const path of [
    "https://example.invalid/",
    `/s3/delete-object?bucket=${bucket}&key=unsafe`,
    "/s3/object?bucket=other-bucket&key=unsafe",
  ]) {
    await page.goto(listing(bucket) + "#" + new URLSearchParams({ inspect: path }));
    await expect(page.locator("#object-drawer")).not.toBeVisible();
  }
  expect(forbidden).toEqual([]);
});

test("unsupported text preview is not offered and native returns remain useful", async ({
  browser,
  baseURL,
}, info) => {
  const { bucket } = fixtures.get(info.testId);
  const context = await browser.newContext({ javaScriptEnabled: false, baseURL });
  const page = await context.newPage();
  try {
    await page.goto(object(bucket));
    await page.locator('a[href^="/s3/object-metadata?"]').click();
    await page.locator(".back-navigation a").click();
    await expect(page.locator(".object-detail-panel")).toBeVisible();
    await page.locator('a[href^="/s3/versions?"]').click();
    await page.locator(".back-navigation a").click();
    await expect(page.locator(".object-detail-panel")).toBeVisible();
    const url = new URL(object(bucket), baseURL);
    url.searchParams.set("key", other);
    await page.goto(url.href);
    await expect(page.locator('a[href^="/s3/preview?"]')).toHaveCount(0);
    await expect(page.locator('a[href^="/s3/download?"]')).toBeVisible();
  } finally {
    await context.close();
  }
});

test("bucket versions restore the originating folder and preferences retain markers", async ({
  page,
}, info) => {
  const { bucket } = fixtures.get(info.testId);
  await page.goto(listing(bucket));
  await page.locator("#object-filter").fill("ação");
  await page.locator('.s3-resource-toolbar a[href^="/s3/versions?"]').click();
  await page.locator(".back-navigation a").click();
  expect(new URL(page.url()).searchParams.get("prefix")).toBe("assets/2026/");
  await expect(page.locator("#object-filter")).toHaveValue("ação");
  await page.goto(
    object(bucket).replace("/s3/object?", "/s3/versions?") +
      "&keyMarker=" +
      encodeURIComponent(key) +
      "&versionMarker=null",
  );
  const href = new URL(
    await page.locator('.theme-controls a[href*="theme=light"]').getAttribute("href"),
    page.url(),
  );
  expect(href.searchParams.get("keyMarker")).toBe(key);
  expect(href.searchParams.get("versionMarker")).toBe("null");
  await page.goto("/operations?page=1&lang=pt-BR&theme=dark");
  const journal = new URL(
    await page.locator('.theme-controls a[href*="theme=light"]').getAttribute("href"),
    page.url(),
  );
  expect(journal.searchParams.get("page")).toBe("1");
});

test("unmarked child fragments are rejected and explicit retry recovers", async ({
  page,
}, info) => {
  const { bucket } = fixtures.get(info.testId);
  await page.goto(listing(bucket));
  await page.locator(".object-table [data-object-detail]").filter({ hasText: key }).click();
  await expect(page.locator("#object-drawer #object-name")).toBeVisible();
  await page.route("**/s3/object-metadata?**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/html",
      body: '<section id="object-inspector-view"><p>untrusted child</p></section>',
    }),
  );
  await page.locator('#object-drawer a[href^="/s3/object-metadata?"]').click();
  await expect(page.locator("#drawer-status")).not.toBeEmpty();
  await expect(page.locator("#object-drawer-content")).not.toContainText("untrusted child");
  await page.unroute("**/s3/object-metadata?**");
  await page.locator("[data-drawer-retry]").click();
  await expect(page.locator("#object-inspector-view")).toBeVisible();
});
