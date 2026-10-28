import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

for (const lang of ["en", "pt-BR"]) {
  for (const theme of ["light", "dark"]) {
    test(`precision console: ${lang}, ${theme}`, async ({ page }, info) => {
      const external = [];
      page.on("request", (request) => {
        if (new URL(request.url()).origin !== "http://127.0.0.1:8080") external.push(request.url());
      });
      await page.emulateMedia({ reducedMotion: "reduce" });
      for (const width of [390, 1366, 1440]) {
        await page.setViewportSize({ width, height: 900 });
        const response = await page.goto(`/s3?lang=${lang}&theme=${theme}`);
        expect(response.headers()["content-security-policy"]).toContain("font-src 'self'");
        await page.evaluate(() => document.fonts.ready);
        expect(await page.evaluate(() => document.fonts.check('400 16px "IBM Plex Sans"'))).toBe(
          true,
        );
        expect(await page.evaluate(() => document.fonts.check('600 32px "Space Grotesk"'))).toBe(
          true,
        );
        await expect(page.locator('.primary-navigation a[href^="/operations"]')).toHaveCount(0);
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
        await page.screenshot({ path: info.outputPath(`console-${width}.png`), fullPage: true });
      }
      expect(external).toEqual([]);
    });
  }
}
