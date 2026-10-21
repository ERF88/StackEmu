import { test, expect } from "@playwright/test";

for (const theme of ["dark", "light"]) {
  test(`operational workspace layout: ${theme}`, async ({ page }, testInfo) => {
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.goto(`/s3?lang=pt-BR&theme=${theme}`);
    await expect(page.locator(".sidebar")).toBeVisible();
    await expect(page.locator(".primary-navigation [aria-current=page]")).toContainText("S3");
    await expect(page.locator(".page-actions .primary")).toBeVisible();
    const listedBuckets = await page.locator("#bucket-list tbody tr").count();
    await expect(page.locator("[data-bucket-count]")).toHaveAttribute(
      "data-bucket-count",
      String(listedBuckets),
    );
    await expect(page.locator(".service-connection .status")).toBeVisible();
    await page.locator("[hx-get]").click();
    await expect(page.locator("[data-bucket-count]")).toHaveAttribute(
      "data-bucket-count",
      String(await page.locator("#bucket-list tbody tr").count()),
    );
    const sidebar = await page.locator(".sidebar").boundingBox();
    const content = await page.locator("main").boundingBox();
    expect(content.x).toBeGreaterThanOrEqual(sidebar.width);
    await page.screenshot({ path: testInfo.outputPath(`workspace-${theme}.png`), fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
      .toBe(true);
    await expect(page.getByRole("heading", { name: "S3", exact: true })).toBeVisible();
    await page.screenshot({
      path: testInfo.outputPath(`workspace-mobile-${theme}.png`),
      fullPage: true,
    });
  });
}
