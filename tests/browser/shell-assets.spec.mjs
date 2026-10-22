import { test, expect } from "@playwright/test";
import { createHash } from "node:crypto";

for (const language of ["en", "pt-BR"]) {
  test(`localized shell, titles and fingerprinted assets: ${language}`, async ({
    page,
    request,
  }, testInfo) => {
    const labels =
      language === "en"
        ? [
            "Home",
            "Connection",
            "Component gallery",
            "Create bucket",
            "Create folder marker",
            "Upload new object",
          ]
        : [
            "Início",
            "Conexão",
            "Galeria de componentes",
            "Criar bucket",
            "Criar marcador de pasta",
            "Enviar novo objeto",
          ];
    const paths = [
      "/",
      "/connection",
      "/components",
      "/s3/write?action=bucket",
      "/s3/write?action=folder&bucket=example",
      "/s3/write?action=upload&bucket=example",
    ];
    for (const theme of ["dark", "light"]) {
      for (const [index, path] of paths.entries()) {
        await page.goto(`${path}${path.includes("?") ? "&" : "?"}lang=${language}&theme=${theme}`);
        await expect(page).toHaveTitle(`${labels[index]} · StackEmu`);
        for (const controls of [".theme-controls", ".language-controls"]) {
          await expect(page.locator(`${controls} a[aria-current=true]`)).toHaveCount(1);
        }
        await expect(page.locator("html")).toHaveAttribute("data-density", "comfortable");
        await expect(page.locator(".preferences .density-controls")).toBeHidden();
        await expect(page.locator(".preferences-panel")).toBeHidden();
        await expect(page.locator(".language-controls")).toBeHidden();
        await expect(page.locator(".primary-navigation svg")).toHaveCount(2);
        for (const icon of await page.locator(".primary-navigation svg").all()) {
          await expect(icon).toHaveAttribute("aria-hidden", "true");
        }
        expect(await page.locator("[style], script:not([src])").count()).toBe(0);
      }
      await page.setViewportSize({ width: 1440, height: 1000 });
      await page.goto(`/?lang=${language}&theme=${theme}`);
      await page.screenshot({
        path: testInfo.outputPath(`e2-shell-${language}-${theme}-1440.png`),
        fullPage: true,
      });
      await page.setViewportSize({ width: 390, height: 844 });
      await page.screenshot({
        path: testInfo.outputPath(`e2-shell-${language}-${theme}-390.png`),
        fullPage: true,
      });
    }
    const stylesheet = await page.locator('link[rel="stylesheet"]').getAttribute("href");
    const favicon = await page.locator('link[rel="icon"]').getAttribute("href");
    const script = await page.locator('script[src^="/app.js?v="]').getAttribute("src");
    for (const href of [stylesheet, favicon, script]) {
      const response = await request.get(href);
      expect(response.status()).toBe(200);
      expect(href.split("?v=")[1]).toBe(
        createHash("sha256")
          .update(await response.body())
          .digest("hex"),
      );
    }
    await page.goto(`/connection?lang=${language}&theme=auto`);
    const themeLabel = language === "en" ? "Dark theme" : "Tema escuro";
    await page.locator(".preferences-trigger").click();
    await page.getByRole("link", { name: themeLabel, exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`/connection\\?lang=${language}&theme=dark$`));
    await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  });
}
