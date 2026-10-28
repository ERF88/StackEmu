import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulator = "http://127.0.0.1:4566";
const fixtures = new Map();
const prefix = "examples/2026/october +% ação/";
const filename = "report +% <x>.txt";
const original = "original detail bytes\n";

test.beforeEach(async ({ request }, info) => {
  const bucket = `stackemu-detail-${randomUUID().replaceAll("-", "")}`;
  const keys = [prefix + filename, prefix + "line\r\nend.txt"];
  expect((await request.put(`${emulator}/${bucket}`)).status()).toBe(200);
  fixtures.set(info.testId, { bucket, keys });
  for (const key of keys)
    expect(
      (
        await request.put(`${emulator}/${bucket}/${encodeURIComponent(key)}`, {
          data: original,
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

for (const javaScriptEnabled of [true, false]) {
  test.describe(`focused details, JavaScript=${javaScriptEnabled}`, () => {
    test.use({ javaScriptEnabled });
    for (const lang of ["en", "pt-BR"]) {
      for (const theme of ["light", "dark"]) {
        test(`relative folders and shared details: ${lang}, ${theme}`, async ({
          page,
          request,
        }, info) => {
          const { bucket, keys } = fixtures.get(info.testId);
          if (javaScriptEnabled)
            await page.addInitScript(() => {
              Object.defineProperty(window.navigator, "clipboard", {
                value: {
                  writeText: async (value) => {
                    window.exactCopied = value;
                  },
                },
              });
            });
          await page.emulateMedia({ reducedMotion: "reduce" });
          await page.setViewportSize({ width: 1366, height: 768 });
          await page.goto(
            `/s3/bucket?bucket=${bucket}&prefix=examples%2F&lang=${lang}&theme=${theme}&density=compact`,
          );
          for (const [label, target] of [
            ["2026/", "examples/2026/"],
            ["october +% ação/", prefix],
          ]) {
            const folder = page.locator(".object-table tbody th a");
            await expect(folder).toHaveText(label);
            const url = new URL(await folder.getAttribute("href"), page.url());
            expect(url.searchParams.get("prefix")).toBe(target);
            expect(url.searchParams.get("lang")).toBe(lang);
            expect(url.searchParams.get("theme")).toBe(theme);
            expect(url.searchParams.get("density")).toBe("compact");
            await folder.focus();
            await page.keyboard.press("Enter");
          }
          await expect(page.locator(".breadcrumbs li")).toHaveText([
            "S3",
            bucket,
            "examples/",
            "2026/",
            "october +% ação/",
          ]);
          await expect(page.locator('.breadcrumbs [aria-current="page"]')).toHaveText(
            "october +% ação/",
          );
          const link = page.getByRole("link", { name: keys[0], exact: true });
          await link.click();
          const panel = page.locator(".object-panel");
          await expect(panel.locator("#object-name")).toHaveText(filename);
          await expect(
            panel.getByLabel(lang === "en" ? "Object key" : "Chave do objeto", { exact: true }),
          ).toHaveValue(keys[0]);
          await expect(panel.locator("textarea[data-copy-param]")).toHaveCount(2);
          await expect(panel.locator('a[href^="/s3/object-metadata?"]')).toHaveCount(1);
          await expect(panel.locator("details, summary")).toHaveCount(0);
          const copy = panel.getByRole("button", {
            name: lang === "en" ? "Copy object key" : "Copiar chave do objeto",
            exact: true,
          });
          if (javaScriptEnabled) {
            await copy.click();
            expect(await page.evaluate(() => window.exactCopied)).toBe(keys[0]);
            await expect(panel.locator(".copy-status")).not.toBeEmpty();
          } else {
            for (const button of await panel.locator("[data-copy-field]").all())
              await expect(button).toBeHidden();
          }
          const download = await request.get(
            await panel.locator('a[href^="/s3/download?"]').getAttribute("href"),
          );
          expect(download.status()).toBe(200);
          expect(await download.text()).toBe(original);
          for (const width of [1366, 390, 1440]) {
            await page.setViewportSize({ width, height: 768 });
            if (javaScriptEnabled) {
              expect(
                (await page.locator(".drawer-heading").boundingBox()).height,
              ).toBeLessThanOrEqual(64);
              await expect(page.locator("[data-drawer-close]")).toHaveAccessibleName(
                lang === "en" ? "Close details" : "Fechar detalhes",
              );
              const title = await page.locator("#drawer-title").boundingBox();
              const close = page.locator("[data-drawer-close]");
              const closeBounds = await close.boundingBox();
              expect(
                Math.abs(title.y + title.height / 2 - closeBounds.y - closeBounds.height / 2),
              ).toBeLessThanOrEqual(1);
              await close.focus();
              await page.keyboard.press("Tab");
              await page.keyboard.press("Shift+Tab");
              await expect(close).toBeFocused();
              await expect(close.locator(".action-tooltip")).toBeVisible();
              expect(
                (await close.locator(".action-tooltip").boundingBox()).y,
              ).toBeGreaterThanOrEqual(0);
            }
            expect(
              await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
            ).toBe(true);
            expect(
              await panel.evaluate((element) => element.scrollWidth <= element.clientWidth),
            ).toBe(true);
            if (javaScriptEnabled)
              expect(
                (
                  await new AxeBuilder({ page })
                    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
                    .analyze()
                ).violations,
              ).toEqual([]);
            await page.screenshot({
              path: info.outputPath(`details-${width}.png`),
              fullPage: true,
            });
          }
          if (javaScriptEnabled) {
            await page.goto(
              await page.locator("[data-object-detail]").first().getAttribute("href"),
            );
            await expect(page.locator("h1")).toHaveText(filename);
            await page
              .getByRole("button", {
                name: lang === "en" ? "Copy object key" : "Copiar chave do objeto",
                exact: true,
              })
              .click();
            expect(await page.evaluate(() => window.exactCopied)).toBe(keys[0]);
          }
          const back = page.locator(".back-navigation a");
          const target = new URL(await back.getAttribute("href"), page.url());
          expect(target.searchParams.get("prefix")).toBe(prefix);
          expect(target.searchParams.get("density")).toBe("compact");
          await back.click();
          await expect(page.locator(".object-table")).toBeVisible();
          await page.locator(".breadcrumbs a").filter({ hasText: "2026/" }).click();
          await expect(page.locator(".object-table tbody th a")).toHaveText("october +% ação/");
        });
      }
    }
  });
}

test("direct details preserve control characters for clipboard and denial fallback", async ({
  page,
}, info) => {
  const { bucket, keys } = fixtures.get(info.testId);
  await page.addInitScript(() => {
    Object.defineProperty(window.navigator, "clipboard", {
      value: {
        writeText: async (value) => {
          window.exactCopied = value;
          if (window.denyCopy) throw new Error("fixture denial");
        },
      },
    });
  });
  await page.goto(`/s3/object?bucket=${bucket}&key=${encodeURIComponent(keys[1])}&lang=en`);
  const field = page.getByLabel("Object key", { exact: true });
  await expect(field).toHaveValue(encodeURIComponent(keys[1]));
  await expect(page.locator("[data-encoded-note]")).toBeVisible();
  const copy = page.getByRole("button", { name: "Copy object key", exact: true });
  await copy.click();
  expect(await page.evaluate(() => window.exactCopied)).toBe(keys[1]);
  await page.evaluate(() => {
    window.denyCopy = true;
  });
  await copy.click();
  await expect(page.locator(".copy-status")).toContainText("unavailable or denied");
  await expect(field).toBeFocused();
  expect(
    await field.evaluate((element) =>
      element.value.slice(element.selectionStart, element.selectionEnd),
    ),
  ).toBe(encodeURIComponent(keys[1]));
});
