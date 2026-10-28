import { test, expect } from "@playwright/test";
import { createOwned, removeOwned, canonicalReview, emulator } from "./operation-fixtures.mjs";
const owned = new Map(),
  key = "folder/ação +%.txt";
test.beforeEach(async ({ request }, info) => {
  owned.set(info.testId, await createOwned(request, [key]));
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});
for (const lang of ["en", "pt-BR"])
  for (const theme of ["light", "dark"])
    test(`minimal action copy: ${lang}, ${theme}`, async ({ page, request }, info) => {
      const fixture = owned.get(info.testId),
        query = `lang=${lang}&theme=${theme}`;
      const { review } = await canonicalReview(
        page,
        request,
        `/s3/delete-object?bucket=${fixture.bucket}&key=${encodeURIComponent(key)}`,
        query,
      );
      await expect(review.locator("h1")).toHaveCount(1);
      await expect(review.locator("h2,details,summary,input[type=checkbox]")).toHaveCount(0);
      expect((await review.locator("#operation-consequence").innerText()).length).toBeLessThan(160);
      await page.goto(`/s3/write?action=upload&bucket=${fixture.bucket}&${query}`);
      await expect(page.locator("#action-workspace details")).toHaveCount(0);
      for (const width of [390, 1366, 1440]) {
        await page.setViewportSize({ width, height: 768 });
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
      }
      expect(
        await (
          await request.get(`${emulator}/${fixture.bucket}/${encodeURIComponent(key)}`)
        ).text(),
      ).toBe("original bytes");
    });
