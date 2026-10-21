import { test, expect } from "@playwright/test";

for (const language of ["en", "pt-BR"]) {
  test(`short desktop sidebar preserves keyboard reachability: ${language}`, async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 320 });
    await page.goto(`/?lang=${language}&theme=dark`);
    const links = page.locator(".sidebar a");
    await links.first().focus();
    for (let index = 0; index < (await links.count()); index++) {
      const link = links.nth(index);
      await expect(link).toBeFocused();
      const bounds = await link.boundingBox();
      // Keep the 3px outline plus 4px offset inside the scroll viewport.
      expect(bounds.y).toBeGreaterThanOrEqual(7);
      expect(bounds.y + bounds.height).toBeLessThanOrEqual(313);
      await page.keyboard.press("Tab");
    }
    // Fewer links may fit without scrolling; keyboard visibility remains the contract.
    await expect(page.locator(".sidebar a").last()).toBeVisible();
  });
}

for (const scheme of ["dark", "light"]) {
  test(`system theme and explicit overrides: ${scheme}`, async ({ page }, testInfo) => {
    await page.emulateMedia({ colorScheme: scheme });
    await page.goto("/?lang=pt-BR");
    await expect(page.locator("html")).toHaveAttribute("data-theme", "auto");
    const schemeValue = () =>
      page.evaluate(() => window.getComputedStyle(document.documentElement).colorScheme);
    expect(await schemeValue()).toBe(scheme);
    for (const width of [360, 390, 900, 901, 1440, 1920]) {
      await page.setViewportSize({ width, height: 1000 });
      await expect(page.locator(".primary-navigation")).toHaveCSS(
        "display",
        width <= 900 ? "flex" : "grid",
      );
      await expect(page.locator(".app-shell")).toHaveCSS("display", "grid");
      await expect(page.locator(".sidebar")).toHaveCSS(
        "position",
        width <= 900 ? "static" : "sticky",
      );
      await expect(page.locator(".sidebar")).toHaveCSS(
        "align-self",
        width <= 900 ? "stretch" : "start",
      );
      if (width > 900) {
        await expect(page.locator(".sidebar")).toHaveCSS("width", "256px");
      }
      await expect
        .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
        .toBe(true);
      if (width === 390 || width === 1440)
        await page.screenshot({
          path: testInfo.outputPath(`e1-auto-${scheme}-${width}.png`),
          fullPage: true,
        });
    }
    for (const theme of ["dark", "light"]) {
      await page.goto(`/?lang=pt-BR&theme=${theme}`);
      expect(await schemeValue()).toBe(theme);
      await expect(page.locator("html")).toHaveAttribute("data-theme", theme);
    }
    await page.goto("/connection?lang=pt-BR&theme=auto");
    expect(await schemeValue()).toBe(scheme);
    await page.locator(".preferences-trigger").click();
    await page.getByRole("link", { name: "English", exact: true }).click();
    await expect(page).toHaveURL(/lang=en&theme=auto/);
  });
}

test("contrast, motion and forced-colors preferences", async ({ page }) => {
  await page.emulateMedia({ contrast: "more", reducedMotion: "reduce" });
  await page.goto("/?theme=light");
  const tokens = await page.evaluate(() => {
    const css = window.getComputedStyle(document.documentElement);
    return ["--color-border", "--color-text", "--motion-fast"].map((key) =>
      css.getPropertyValue(key).trim(),
    );
  });
  expect(tokens[0]).toBe(tokens[1]);
  // The production CSS minifier may normalize equivalent zero time units.
  expect(tokens[2]).toMatch(/^0(?:ms|s)$/);
  await page.emulateMedia({ forcedColors: "active" });
  await page.reload();
  await expect(page.locator(".primary-navigation [aria-current=page]")).toBeVisible();
  await expect(page.locator(".primary-navigation [aria-current=page]")).toHaveCSS(
    "border-top-width",
    "1px",
  );
  await page.locator(".primary-navigation [aria-current=page]").focus();
  await expect(page.locator(".primary-navigation [aria-current=page]")).toHaveCSS(
    "outline-style",
    "solid",
  );
});
