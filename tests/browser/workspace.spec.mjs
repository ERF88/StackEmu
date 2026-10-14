import { test, expect } from "@playwright/test";

for (const theme of ["dark", "light"]) {
  test(`operational workspace layout: ${theme}`, async ({ page }, testInfo) => {
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.goto(`/?lang=pt-BR&theme=${theme}`);
    await expect(page.locator(".sidebar")).toBeVisible();
    await expect(page.locator(".primary-navigation [aria-current=page]")).toHaveText(
      "◫Visão geral",
    );
    await expect(page.locator(".page-actions .primary")).toBeVisible();
    const sidebar = await page.locator(".sidebar").boundingBox();
    const content = await page.locator("main").boundingBox();
    expect(content.x).toBeGreaterThanOrEqual(sidebar.width);
    await page.screenshot({ path: testInfo.outputPath(`workspace-${theme}.png`), fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
      .toBe(true);
    await expect(page.getByRole("heading", { name: "Visão geral", exact: true })).toBeVisible();
    await page.screenshot({
      path: testInfo.outputPath(`workspace-mobile-${theme}.png`),
      fullPage: true,
    });
  });
}
