import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulatorURL = process.env.STACKEMU_BROWSER_EMULATOR || "http://127.0.0.1:4566";
const emulator = new URL(emulatorURL);
if (
  emulator.protocol !== "http:" ||
  emulator.port !== "4566" ||
  !["127.0.0.1", "localhost", "ministack", "host.docker.internal"].includes(emulator.hostname)
) {
  throw new Error("Operational shell fixtures target only the local emulator");
}
const bucket = `stackemu-actions-${randomUUID().replaceAll("-", "")}`;
const key = "samples/ação +% file.txt";
let created = false;
test.beforeAll(async ({ request }) => {
  expect((await request.put(`${emulatorURL}/${bucket}`)).status()).toBe(200);
  created = true;
  expect(
    (
      await request.put(`${emulatorURL}/${bucket}/${encodeURIComponent(key)}`, {
        data: "Operational shell preview fixture",
        headers: { "Content-Type": "text/plain" },
      })
    ).status(),
  ).toBe(200);
});
test.afterAll(async ({ request }) => {
  if (!created) return;
  expect(
    (await request.delete(`${emulatorURL}/${bucket}/${encodeURIComponent(key)}`)).status(),
  ).toBe(204);
  expect((await request.delete(`${emulatorURL}/${bucket}`)).status()).toBe(204);
});

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    test(`quiet shell and contextual actions: ${language}, ${theme}`, async ({
      page,
    }, testInfo) => {
      const query = `lang=${language}&theme=${theme}&density=compact`;
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.goto(`/?${query}`);
      const preferences = page.locator(".preferences-panel");
      const preferencesTrigger = page.locator(".preferences-trigger");
      await expect(preferences).toBeHidden();
      await expect(page.locator(".language-controls")).toBeHidden();
      await expect(page.locator(".preferences .density-controls")).toBeHidden();
      await expect(page.locator(".density-controls")).toHaveCount(0);
      await expect(
        page.locator(".sidebar .local-notice, .brand small, .workspace-label .eyebrow"),
      ).toHaveCount(0);
      await expect(page.locator("footer")).toHaveCount(0);
      for (const width of [390, 1440]) {
        await page.setViewportSize({ width, height: 1000 });
        if (width === 390) {
          // Do not distribute free viewport height into the mobile navigation row.
          expect((await page.locator(".sidebar").boundingBox()).height).toBeLessThanOrEqual(180);
        }
        const headingBefore = await page.locator("h1").boundingBox();
        await preferencesTrigger.focus();
        await page.keyboard.press("Space");
        await expect(preferences).toBeVisible();
        expect(await page.locator("h1").boundingBox()).toEqual(headingBefore);
        await page.keyboard.press("Tab");
        await expect(page.locator(".preferences-close")).toBeFocused();
        await expect(page.locator(".language-controls")).toBeVisible();
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        expect(
          (
            await new AxeBuilder({ page })
              .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
              .analyze()
          ).violations,
        ).toEqual([]);
        await page.screenshot({
          path: testInfo.outputPath(`preferences-${width}.png`),
          fullPage: true,
        });
        await page.keyboard.press("Escape");
        await expect(preferences).toBeHidden();
        await expect(preferencesTrigger).toBeFocused();
        await preferencesTrigger.click();
        await page.locator(".preferences-close").click();
        await expect(preferences).toBeHidden();
        await preferencesTrigger.click();
        // The mobile popover legitimately overlays the title. Use visible sidebar padding.
        await page.locator(".sidebar").click({ position: { x: 4, y: 4 } });
        await expect(preferences).toBeHidden();
        await page.screenshot({ path: testInfo.outputPath(`home-${width}.png`), fullPage: true });
      }
      await page.goto(`/s3?${query}`);
      const row = page.locator("#bucket-list tbody tr").filter({ hasText: bucket });
      const actions = row.locator(".resource-action-links");
      await expect(row.locator("summary")).toHaveCount(0);
      await expect(actions.locator("a")).toHaveCount(3);
      for (const action of await actions.locator("a").all()) {
        await expect(action).toBeVisible();
        await expect(action.locator("svg[aria-hidden=true]")).toHaveCount(1);
        expect((await action.boundingBox()).height).toBeGreaterThanOrEqual(48);
      }
      await page.screenshot({ path: testInfo.outputPath("bucket-actions.png"), fullPage: true });
      const upload = actions.locator('a[href*="action=upload"]');
      await expect(upload).toHaveAttribute(
        "href",
        `/s3/write?action=upload&bucket=${bucket}&${query}`,
      );
      await upload.click();
      await expect(page.locator("#mutation-bucket")).toHaveValue(bucket);
      await expect(page.locator('input[name="density"]')).toHaveValue("compact");
      await expect(page.locator('button[type="submit"] [data-submit-label]')).toBeVisible();
      await expect(page.locator('button[type="submit"] svg')).toHaveCount(1);
      await expect(page.locator(".back-navigation a")).toHaveAttribute(
        "href",
        `/s3/bucket?bucket=${bucket}&prefix=&${query}`,
      );
      await page.goto(`/s3/bucket?bucket=${bucket}&prefix=samples%2F&${query}`);
      await expect(page.locator(".object-list-panel .density-controls")).toHaveCount(0);
      const objectRow = page.locator(".object-table tbody tr").filter({ hasText: key });
      await expect(objectRow.locator(".table-action")).toBeVisible();
      await expect(objectRow.locator("summary")).toHaveCount(0);
      for (const width of [390, 1440]) {
        await page.setViewportSize({ width, height: 1000 });
        await objectRow.locator(".table-action").focus();
        const sizeHeading = page.locator(".object-table thead th").nth(1);
        const modifiedHeading = page.locator(".object-table thead th").nth(2);
        await expect(sizeHeading).toHaveCSS("overflow-wrap", "normal");
        expect((await sizeHeading.boundingBox()).width).toBeGreaterThanOrEqual(112);
        expect((await modifiedHeading.boundingBox()).width).toBeGreaterThanOrEqual(176);
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        expect(
          (
            await new AxeBuilder({ page })
              .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
              .analyze()
          ).violations,
        ).toEqual([]);
        await page.screenshot({
          path: testInfo.outputPath(`object-actions-${width}.png`),
          fullPage: true,
        });
        const directAction = objectRow.locator(".table-action");
        await directAction.scrollIntoViewIfNeeded();
        const actionBounds = await directAction.boundingBox();
        expect(actionBounds.x).toBeGreaterThanOrEqual(0);
        expect(actionBounds.x + actionBounds.width).toBeLessThanOrEqual(width);
        await page.screenshot({
          path: testInfo.outputPath(`object-actions-visible-${width}.png`),
          fullPage: true,
        });
      }
      const detail = objectRow.locator('.resource-action-links a[href^="/s3/object?"]');
      await expect(detail).toHaveAttribute(
        "href",
        `/s3/object?bucket=${bucket}&key=${encodeURIComponent(key)}&${query}`,
      );
      await detail.click();
      await expect(page.locator("#object-drawer")).toBeVisible();
      for (const button of await page.locator("#object-drawer .button:visible").all()) {
        await expect(button.locator("svg[aria-hidden=true]")).toHaveCount(1);
        expect((await button.boundingBox()).height).toBeGreaterThanOrEqual(48);
      }
      await page.locator('#object-drawer a[href^="/s3/preview?"]').click();
      await expect(page.locator("pre")).toContainText("Operational shell preview fixture");
      await expect(page.locator(".preferences-panel")).toBeHidden();
      await expect(page.locator(".back-navigation")).toBeVisible();
      await page.locator(".back-navigation a").click();
      await expect(page.locator(".object-key")).toHaveText(key);
      await page.locator(".back-navigation a").click();
      await expect(page).toHaveURL(new RegExp(`/s3/bucket\\?bucket=${bucket}.*density=compact`));
    });
  }
  test(`native preferences and actions without JS: ${language}`, async ({ browser, baseURL }) => {
    const context = await browser.newContext({ baseURL, javaScriptEnabled: false });
    try {
      const page = await context.newPage();
      await page.goto(`/?lang=${language}&theme=light&density=compact`);
      await page.locator(".preferences-trigger").click();
      await page
        .getByRole("link", { name: language === "en" ? "Português" : "English", exact: true })
        .click();
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      await expect(page.locator(".preferences-panel")).toBeHidden();
      await page.locator(".service-card a").click();
      const row = page.locator("#bucket-list tbody tr").filter({ hasText: bucket });
      await row.locator('a[href*="action=folder"]').click();
      await expect(page.locator("#mutation-bucket")).toHaveValue(bucket);
      await expect(page.locator('input[name="density"]')).toHaveValue("compact");
    } finally {
      await context.close();
    }
  });
}
