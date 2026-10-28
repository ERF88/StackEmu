import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import {
  createOwned,
  removeOwned,
  readyDialog,
  stableOperation,
  emulator,
} from "./operation-fixtures.mjs";

const owned = new Map();
test.beforeEach(async ({ request }, info) => {
  owned.set(info.testId, await createOwned(request, ["folder/", "folder/ação +%.txt"]));
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});
for (const lang of ["en", "pt-BR"])
  for (const theme of ["light", "dark"]) {
    test(`minimal reviewed cleanup: ${lang}, ${theme}`, async ({ page, request }, info) => {
      const fixture = owned.get(info.testId),
        query = `lang=${lang}&theme=${theme}&density=compact`;
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.goto(`/s3?${query}`);
      const trigger = page
        .locator(".bucket-table tbody tr")
        .filter({ hasText: fixture.bucket })
        .locator('a[href^="/s3/delete-bucket?"]');
      await trigger.click();
      const dialog = page.locator("#action-dialog");
      await expect(dialog.locator("[data-action-review]")).toBeVisible({ timeout: 15000 });
      await dialog.locator("[data-action-review]").click();
      await readyDialog(page);
      await expect(dialog.locator("[data-action-close]")).toBeFocused();
      await expect(dialog.locator("details,summary,input[type=checkbox]")).toHaveCount(0);
      await expect(dialog.locator(".object-key")).toContainText(fixture.bucket);
      for (const width of [390, 1366, 1440]) {
        await page.setViewportSize({ width, height: 768 });
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        await page.screenshot({ path: info.outputPath(`cleanup-${width}.png`), fullPage: true });
      }
      expect(
        (
          await new AxeBuilder({ page })
            .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
            .analyze()
        ).violations,
      ).toEqual([]);
      await page.keyboard.press("Escape");
      await expect(trigger).toBeFocused();
      expect((await request.get(`${emulator}/${fixture.bucket}`)).status()).toBe(200);
      await trigger.click();
      await dialog.locator("[data-action-review]").click();
      const { id } = await readyDialog(page);
      await dialog.locator('input[name="bucketName"]').fill("wrong");
      await dialog.locator('button[type="submit"]').click();
      await expect(dialog.locator('[aria-invalid="true"]')).toBeFocused();
      await dialog.locator('input[name="bucketName"]').fill(fixture.bucket);
      await dialog.locator('button[type="submit"]').click();
      await expect(dialog).not.toBeVisible();
      expect((await stableOperation(request, id)).state).toBe("SUCCEEDED");
      await expect(
        page.locator(".bucket-table tbody tr").filter({ hasText: fixture.bucket }),
      ).toHaveCount(0, { timeout: 15000 });
      await expect(page.locator("[data-operation-notice]")).toBeVisible();
      expect(await (await request.get(`${emulator}/${fixture.neighbor}/guard.txt`)).text()).toBe(
        "guard",
      );
    });
  }
