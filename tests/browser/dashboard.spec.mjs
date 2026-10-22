import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { readFile } from "node:fs/promises";

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    for (const density of ["comfortable", "compact"]) {
      test(`service catalog and S3 refresh: ${language}, ${theme}, ${density}`, async ({
        page,
      }, testInfo) => {
        const query = `lang=${language}&theme=${theme}${density === "compact" ? "&density=compact" : ""}`;
        await page.emulateMedia({ reducedMotion: "reduce" });
        await page.goto(`/?${query}`);
        await expect(page.getByRole("heading", { level: 1 })).toHaveText(
          language === "en" ? "Home" : "Início",
        );
        await expect(page.locator(".primary-navigation a")).toHaveCount(2);
        await expect(
          page.locator(".sidebar a[href^='/components'], .sidebar a[href^='/connection']"),
        ).toHaveCount(0);
        await expect(page.locator(".service-card")).toHaveCount(1);
        await expect(page.locator(".service-catalog")).toHaveCSS("border-top-width", "0px");
        await expect(page.locator(".service-card")).toHaveCSS("border-top-width", "1px");
        await expect(
          page.locator("#bucket-panel, [data-refresh-panel], [data-bucket-count]"),
        ).toHaveCount(0);
        await expect(page.locator(".service-status-link")).toHaveCount(0);
        for (const width of [390, 1440]) {
          await page.setViewportSize({ width, height: 1000 });
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
          await page.screenshot({ path: testInfo.outputPath(`home-${width}.png`), fullPage: true });
        }
        await page.locator(".service-card a").focus();
        await page.keyboard.press("Enter");
        await expect(page).toHaveURL((url) => url.pathname === "/s3" && url.search === `?${query}`);
        await expect(page.locator("html")).toHaveAttribute("data-density", density);
        await expect(page.locator(".primary-navigation [aria-current=page]")).toContainText("S3");
        await expect(page.locator(".service-connection .status")).toHaveCount(0);
        const trigger = page.locator("[data-refresh-panel]");
        await expect(trigger).toHaveText(language === "en" ? "Refresh list" : "Atualizar lista");
        let release;
        const gate = new Promise((resolve) => {
          release = resolve;
        });
        let requests = 0;
        await page.route("**/buckets?*", async (route) => {
          requests++;
          const response = await route.fetch();
          await gate;
          await route.fulfill({ response });
        });
        try {
          await trigger.focus();
          await page.keyboard.press("Enter");
          await expect(page.locator("#bucket-panel")).toHaveAttribute("aria-busy", "true");
          await expect(page.locator("[data-bucket-count]")).toBeHidden();
          await trigger.click();
          release();
          await expect(page.locator("#bucket-panel")).toHaveAttribute("aria-busy", "false");
          await expect(page.locator("[data-bucket-count]")).toBeVisible();
          await expect(trigger).toBeFocused();
          expect(requests).toBe(1);
        } finally {
          release();
          await page.unroute("**/buckets?*");
        }
        for (const failure of ["http", "network"]) {
          const previous = await page.locator("#bucket-panel").innerHTML();
          await page.route("**/buckets?*", (route) =>
            failure === "http"
              ? route.fulfill({
                  status: 503,
                  contentType: "text/plain",
                  body: "Controlled test failure",
                })
              : route.abort("failed"),
          );
          await trigger.click();
          await expect(page.locator("#loading")).toContainText(
            language === "en" ? "Refresh failed." : "Falha ao atualizar.",
          );
          expect(await page.locator("#bucket-panel").innerHTML()).toBe(previous);
          await page.unroute("**/buckets?*");
        }
        await trigger.click();
        await expect(page.locator("#loading")).toBeEmpty();
        for (const width of [390, 1440]) {
          await page.setViewportSize({ width, height: 1000 });
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
          await page.screenshot({ path: testInfo.outputPath(`s3-${width}.png`), fullPage: true });
        }
        await page.locator(".preferences-trigger").click();
        await page.locator('.preferences a[href^="/connection?"]').click();
        await expect(page).toHaveURL((url) => url.pathname === "/connection");
        await expect(page.locator(".connection-facts")).toBeVisible();
        await expect(page.locator("#bucket-list")).toHaveCount(0);
        await page.locator(".primary-navigation a").first().click();
        await expect(page.locator(".service-status-link")).toHaveCount(0);
      });
    }

    test(`unavailable S3 presentation: ${language}, ${theme}`, async ({ page }, testInfo) => {
      // Actual unavailable Qute fixture emitted by Java verification, not a live outage claim.
      const html = await readFile(
        new URL(
          `../../work/browser-fixtures/unavailable-${language}-${theme}.html`,
          import.meta.url,
        ),
        "utf8",
      );
      const policy = (await page.request.get("/components")).headers()["content-security-policy"];
      await page.route("**/*", async (route) =>
        route.request().isNavigationRequest()
          ? route.fulfill({
              status: 200,
              contentType: "text/html; charset=utf-8",
              headers: { "Content-Security-Policy": policy },
              body: html,
            })
          : route.fulfill({ response: await route.fetch() }),
      );
      await page.setViewportSize({ width: 390, height: 1000 });
      await page.goto(`/s3?lang=${language}&theme=${theme}`);
      await expect(page.locator("[data-bucket-count]")).toHaveCount(0);
      await expect(page.locator(".service-connection .error")).toBeVisible();
      await expect(page.locator("#bucket-list > a")).toHaveAttribute(
        "href",
        `/s3?lang=${language}&theme=${theme}`,
      );
      expect(
        (
          await new AxeBuilder({ page })
            .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
            .analyze()
        ).violations,
      ).toEqual([]);
      await page.screenshot({ path: testInfo.outputPath("unavailable.png"), fullPage: true });
    });
  }

  test(`native service navigation without JS: ${language}`, async ({ browser, baseURL }) => {
    const context = await browser.newContext({
      baseURL,
      javaScriptEnabled: false,
      reducedMotion: "reduce",
    });
    try {
      const page = await context.newPage();
      await page.goto(`/?lang=${language}&theme=light&density=compact`);
      await page.locator(".service-card a").click();
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      await page.locator("[data-refresh-panel]").click();
      await expect(page.locator("#bucket-panel")).toHaveAttribute("aria-busy", "false");
      await page.locator(".preferences-trigger").click();
      await page.locator('.preferences a[href^="/connection?"]').click();
      await expect(page.locator(".connection-facts")).toBeVisible();
      await page.locator(".primary-navigation a").first().click();
      await expect(page.locator(".service-status-link")).toHaveCount(0);
    } finally {
      await context.close();
    }
  });
}
