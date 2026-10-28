import { test, expect } from "@playwright/test";
import {
  createOwned,
  removeOwned,
  readyDialog,
  canonicalReview,
  stableOperation,
  emulator,
} from "./operation-fixtures.mjs";
const owned = new Map(),
  key = "folder/ação +%.txt";
test.beforeEach(async ({ request }, info) => {
  owned.set(info.testId, await createOwned(request, [key]));
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});
for (const javaScriptEnabled of [true, false])
  test.describe(`object deletion, JavaScript=${javaScriptEnabled}`, () => {
    test.use({ javaScriptEnabled });
    for (const lang of ["en", "pt-BR"])
      for (const theme of ["light", "dark"])
        test(`exact target and contextual return: ${lang}, ${theme}`, async ({
          page,
          request,
        }, info) => {
          const fixture = owned.get(info.testId),
            query = `lang=${lang}&theme=${theme}&density=compact`,
            href = `/s3/delete-object?bucket=${fixture.bucket}&key=${encodeURIComponent(key)}`;
          let review, id;
          if (javaScriptEnabled) {
            await page.goto(`/s3/bucket?bucket=${fixture.bucket}&prefix=folder%2F&${query}`);
            await page
              .locator(".object-table tbody tr")
              .filter({ hasText: key })
              .locator('a[href^="/s3/delete-object?"]')
              .click();
            const ready = await readyDialog(page);
            review = ready.dialog;
            id = ready.id;
            await expect(review.locator("[data-action-close]")).toBeFocused();
          } else {
            const ready = await canonicalReview(page, request, href, query);
            review = ready.review;
            id = ready.id;
          }
          await expect(review.locator(".object-key")).toContainText(key);
          await expect(
            review.locator("details,summary,input[type=checkbox],input[name=bucketName]"),
          ).toHaveCount(0);
          expect(
            (
              await request.get(`${emulator}/${fixture.bucket}/${encodeURIComponent(key)}`)
            ).status(),
          ).toBe(200);
          for (const width of [390, 1366, 1440]) {
            await page.setViewportSize({ width, height: 768 });
            expect(
              await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
            ).toBe(true);
          }
          await review.locator('button[type="submit"]').click();
          expect((await stableOperation(request, id)).state).toBe("SUCCEEDED");
          if (javaScriptEnabled) {
            await expect(page.locator("#action-dialog")).not.toBeVisible();
            await expect(
              page.locator(".object-table tbody tr").filter({ hasText: key }),
            ).toHaveCount(0, { timeout: 15000 });
          } else {
            await page.locator(".back-navigation a").click();
            await expect(page).toHaveURL(/prefix=folder%2F/);
          }
          const url = new URL(page.url());
          expect(url.searchParams.get("lang")).toBe(lang);
          expect(url.searchParams.get("theme")).toBe(theme);
          expect(url.searchParams.get("density")).toBe("compact");
          expect(
            await (await request.get(`${emulator}/${fixture.neighbor}/guard.txt`)).text(),
          ).toBe("guard");
        });
  });
