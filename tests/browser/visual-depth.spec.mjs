import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    for (const width of [390, 1440]) {
      test(`HIG neutral surfaces: ${language}, ${theme}, ${width}`, async ({ page }, testInfo) => {
        await page.setViewportSize({ width, height: 1000 });
        await page.goto(`/?lang=${language}&theme=${theme}`);
        await expect(page.locator("body")).toHaveCSS("background-image", "none");
        await expect(page.locator(".service-card .button.primary")).toHaveCSS(
          "border-top-left-radius",
          "999px",
        );
        await page.screenshot({ path: testInfo.outputPath("dashboard.png"), fullPage: true });
        await page.goto(`/components?lang=${language}&theme=${theme}`);
        await expect(page.locator(".resource-panel .table-scroll")).toHaveCSS(
          "border-top-width",
          "0px",
        );
        await page.locator(".table-scroll").focus();
        await expect(page.locator(".table-scroll")).toBeFocused();
        await expect(page.locator(".table-scroll")).toHaveCSS("outline-style", "solid");
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        await page.screenshot({ path: testInfo.outputPath("components.png"), fullPage: true });
        for (const path of ["/", "/components"]) {
          await page.goto(`${path}?lang=${language}&theme=${theme}`);
          const result = await new AxeBuilder({ page })
            .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"])
            .analyze();
          expect(result.violations).toEqual([]);
        }
        await page.emulateMedia({ contrast: "more", reducedMotion: "reduce" });
        await expect(page.locator("body")).toHaveCSS("background-image", "none");
        await page.emulateMedia({ contrast: "no-preference", forcedColors: "active" });
        await expect(page.locator("body")).toHaveCSS("background-image", "none");
        await expect(page.locator(".button.primary").first()).toHaveCSS("box-shadow", "none");
      });
    }
  }
}

for (const theme of ["dark", "light"]) {
  test(`HIG neutral native fallback: ${theme}`, async ({ browser }) => {
    const context = await browser.newContext({ javaScriptEnabled: false });
    const page = await context.newPage();
    try {
      await page.goto(`http://127.0.0.1:8080/components?lang=pt-BR&theme=${theme}`);
      await expect(page.locator(".table-scroll")).toBeVisible();
      await page.locator(".table-scroll").focus();
      await expect(page.locator(".table-scroll")).toBeFocused();
      await expect(page.locator("body")).toHaveCSS("background-image", "none");
      await expect(page.locator(".button.primary").first()).toBeEnabled();
    } finally {
      await context.close();
    }
  });
}
