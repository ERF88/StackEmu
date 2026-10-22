import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulatorURL = "http://127.0.0.1:4566";
const key = "samples/ação +% original.txt";
const guard = "samples/neighbor.txt";
const ownedFixtures = new Map();

test.afterEach(async ({ playwright }, testInfo) => {
  const bucket = ownedFixtures.get(testInfo.testId);
  if (!bucket) return;
  // A fresh, hook-owned context still works after a timed-out test context is disposed.
  const cleanup = await playwright.request.newContext();
  try {
    for (const name of [key, guard]) {
      expect(
        (await cleanup.delete(`${emulatorURL}/${bucket}/${encodeURIComponent(name)}`)).status(),
      ).toBe(204);
    }
    expect((await cleanup.delete(`${emulatorURL}/${bucket}`)).status()).toBe(204);
  } finally {
    ownedFixtures.delete(testInfo.testId);
    await cleanup.dispose();
  }
});

for (const javaScriptEnabled of [true, false]) {
  test.describe(`single-object deletion, JavaScript=${javaScriptEnabled}`, () => {
    test.use({ javaScriptEnabled });
    for (const language of ["en", "pt-BR"]) {
      for (const theme of ["light", "dark"]) {
        test(`exact target, native confirmation and return: ${language}, ${theme}`, async ({
          page,
          request,
        }, testInfo) => {
          const bucket = `stackemu-browser-delete-${randomUUID().replaceAll("-", "")}`;
          const objectURL = `${emulatorURL}/${bucket}/${encodeURIComponent(key)}`;
          const guardURL = `${emulatorURL}/${bucket}/${encodeURIComponent(guard)}`;
          expect((await request.put(`${emulatorURL}/${bucket}`)).status()).toBe(200);
          ownedFixtures.set(testInfo.testId, bucket);
          for (const url of [objectURL, guardURL]) {
            expect(
              (
                await request.put(url, {
                  data: "owned deletion fixture",
                  headers: { "Content-Type": "text/plain" },
                })
              ).status(),
            ).toBe(200);
          }
          const preferences = `lang=${language}&theme=${theme}&density=compact`;
          const listing = `/s3/bucket?bucket=${bucket}&prefix=samples%2F&${preferences}`;
          await page.emulateMedia({ reducedMotion: "reduce" });
          await page.goto(listing);
          const row = page.locator("tbody tr").filter({ hasText: key });
          await expect(row.locator("summary")).toHaveCount(0);
          await row.locator('a[href^="/s3/delete-object?"]').click();
          if (javaScriptEnabled) {
            await expect(page.locator("#action-dialog")).toBeVisible();
            await expect(page.locator("[data-action-close]")).toBeFocused();
            await expect(page).toHaveURL(/\/s3\/bucket\?/);
          } else await expect(page).toHaveURL(/\/s3\/delete-object\?/);
          const review = javaScriptEnabled
            ? page.locator("#action-dialog")
            : page.locator("#action-workspace");
          await expect(review.locator(".object-key")).toHaveText(key);
          await expect(page.locator('input[name="density"]')).toHaveValue("compact");
          await expect(page.locator('input[name="lang"]')).toHaveValue(language);
          await expect(page.locator('input[name="theme"]')).toHaveValue(theme);
          await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
          await expect(page.getByRole("checkbox")).toHaveCount(0);
          const deletionButton = review.locator("form .button.danger");
          await expect(deletionButton).toHaveCSS("border-top-left-radius", "999px");
          await expect(deletionButton).toHaveCSS("box-shadow", "none");
          const dangerColor = await deletionButton.evaluate(
            (element) => window.getComputedStyle(element).borderTopColor,
          );
          await expect(deletionButton).toHaveCSS("color", dangerColor);
          const neutralSurface = await review
            .locator(".resource-panel")
            .evaluate((element) => window.getComputedStyle(element).backgroundColor);
          await expect(deletionButton).toHaveCSS("background-color", neutralSurface);
          expect((await request.get(objectURL)).status()).toBe(200);
          // Read-only cancel must not mutate either exact target or neighbor.
          if (javaScriptEnabled) await page.locator("[data-action-close]").click();
          else await page.locator('.back-navigation a[href^="/s3/bucket?"]').click();
          expect((await request.get(objectURL)).status()).toBe(200);
          await page.goto(
            `/s3/object?bucket=${bucket}&key=${encodeURIComponent(key)}&${preferences}`,
          );
          await page.locator('.object-actions a[href^="/s3/delete-object?"]').click();
          for (const width of [390, 1440]) {
            await page.setViewportSize({ width, height: 1000 });
            expect(
              await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
            ).toBe(true);
            // Axe needs browser scripts/timers. Native fallback keeps all other gates.
            if (javaScriptEnabled)
              expect(
                (
                  await new AxeBuilder({ page })
                    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
                    .analyze()
                ).violations,
              ).toEqual([]);
            await page.screenshot({
              path: testInfo.outputPath(`delete-review-${width}.png`),
              fullPage: true,
            });
          }
          await deletionButton.focus();
          await page.keyboard.press("Shift+Tab");
          await page.keyboard.press("Tab");
          await expect(page.locator('form button[type="submit"]')).toBeFocused();
          await expect(deletionButton).toHaveCSS("outline-style", "solid");
          await expect(deletionButton).toHaveCSS("outline-width", "3px");
          const response = page.waitForResponse(
            (r) => r.request().method() === "POST" && r.url().includes("/s3/delete-object"),
          );
          await page.keyboard.press("Enter");
          expect((await response).status()).toBe(200);
          await expect(page.locator('form[action="/s3/delete-object"]')).toHaveCount(0);
          expect((await request.get(objectURL)).status()).toBe(404);
          expect((await request.get(guardURL)).status()).toBe(200);
          if (javaScriptEnabled) await page.locator("[data-action-close]").click();
          else await page.locator('.back-navigation a[href^="/s3/bucket?"]').click();
          const returned = new URL(page.url());
          expect(returned.searchParams.get("bucket")).toBe(bucket);
          expect(returned.searchParams.get("prefix")).toBe("samples/");
          expect(returned.searchParams.get("lang")).toBe(language);
          expect(returned.searchParams.get("theme")).toBe(theme);
          expect(returned.searchParams.get("density")).toBe("compact");
          await expect(page.locator("tbody tr").filter({ hasText: key })).toHaveCount(0);
          await expect(page.locator("tbody tr").filter({ hasText: guard })).toHaveCount(1);
        });
      }
    }
  });
}
