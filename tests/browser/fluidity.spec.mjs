import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["light", "dark"]) {
    test(`cohesive shell and press feedback: ${language}, ${theme}`, async ({ page }, testInfo) => {
      await page.emulateMedia({ reducedMotion: "no-preference" });
      for (const width of [390, 1440]) {
        await page.setViewportSize({ width, height: 1000 });
        await page.goto(`/?lang=${language}&theme=${theme}`);
        await expect(page.locator("body")).toHaveCSS("background-image", "none");
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        if (width === 390) {
          const brand = await page.locator(".brand").boundingBox();
          const navigation = await page.locator(".primary-navigation").boundingBox();
          const sidebar = await page.locator(".sidebar").boundingBox();
          expect(sidebar.height).toBeLessThanOrEqual(100);
          expect(Math.abs(brand.y - navigation.y)).toBeLessThanOrEqual(8);
          expect((await page.locator("h1").boundingBox()).y).toBeLessThan(230);
        }
        await page.screenshot({ path: testInfo.outputPath(`home-${width}.png`), fullPage: true });
        expect(
          (
            await new AxeBuilder({ page })
              .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
              .analyze()
          ).violations,
        ).toEqual([]);
      }
      await page.goto(`/components?lang=${language}&theme=${theme}`);
      const button = page.locator("#gallery-primary");
      await button.scrollIntoViewIfNeeded();
      const bounds = await button.boundingBox();
      expect(bounds.height).toBeGreaterThanOrEqual(48);
      await page.mouse.move(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
      await page.mouse.down();
      try {
        await expect
          .poll(() =>
            button.evaluate((element) => {
              const transform = window.getComputedStyle(element).transform;
              return transform === "none" ? 1 : new window.DOMMatrix(transform).a;
            }),
          )
          .toBeCloseTo(0.98, 3);
        expect((await button.boundingBox()).height).toBeGreaterThanOrEqual(44);
      } finally {
        await page.mouse.up();
      }
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.mouse.down();
      try {
        await expect(button).toHaveCSS("transform", "none");
        await expect(button).toHaveCSS("transition-duration", "0s");
      } finally {
        await page.mouse.up();
      }
      await page.keyboard.press("Tab");
      await page.keyboard.press("Shift+Tab");
      await expect(button).toBeFocused();
      await expect(button).toHaveCSS("outline-style", "solid");
    });
  }
}
