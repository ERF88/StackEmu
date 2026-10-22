import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    test(`progressive request feedback: ${language}, ${theme}`, async ({ page }, testInfo) => {
      const loading = language === "en" ? "Loading…" : "Carregando…";
      const failed = language === "en" ? "Refresh failed." : "Falha ao atualizar.";
      for (const path of ["/s3"]) {
        await page.goto(`${path}?lang=${language}&theme=${theme}`);
        let release;
        let requests = 0;
        const gate = new Promise((resolve) => {
          release = resolve;
        });
        await page.route("**/buckets?*", async (route) => {
          requests++;
          const response = await route.fetch();
          await gate;
          await route.fulfill({ response });
        });
        const trigger = page.locator("[data-refresh-panel]");
        try {
          await trigger.focus();
          await page.keyboard.press("Enter");
          await expect(page.locator("#bucket-panel")).toHaveAttribute("aria-busy", "true");
          await expect(page.locator("#loading")).toHaveText(loading);
          await trigger.click();
          release();
          await expect(page.locator("#bucket-panel")).toHaveAttribute("aria-busy", "false");
          await expect(page.locator("#loading")).toBeEmpty();
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
                  body: "Controlled browser-test failure",
                })
              : route.abort("failed"),
          );
          await trigger.click();
          await expect(page.locator("#loading")).toContainText(failed);
          await expect(page.locator("#bucket-panel")).toHaveAttribute("aria-busy", "false");
          expect(await page.locator("#bucket-panel").innerHTML()).toBe(previous);
          await page.unroute("**/buckets?*");
        }
        await trigger.click();
        await expect(page.locator("#loading")).toBeEmpty();
        await expect(page.locator("#bucket-panel")).toHaveAttribute("aria-busy", "false");
      }

      for (const action of ["bucket", "folder", "upload"]) {
        await page.goto(
          `/s3/write?action=${action}&bucket=example&lang=${language}&theme=${theme}`,
        );
        await page.setViewportSize({ width: 390, height: 844 });
        expect(
          await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
        ).toBe(true);
        const results = await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"])
          .analyze();
        expect(results.violations).toEqual([]);
        if (action === "upload") {
          await expect(page.locator("#mutation-file")).toHaveAttribute(
            "aria-describedby",
            "upload-policy file-help",
          );
        }
      }

      await page.goto(`/s3/write?action=bucket&lang=${language}&theme=${theme}`);
      await page.locator("#mutation-bucket").fill("INVALID");
      await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
      // Test-only listener runs after the application's listener, keeping the transient UI
      // observable without stalling a native navigation. A fresh real POST is verified below.
      await page.evaluate(() => {
        document.addEventListener("submit", (event) => event.preventDefault(), { once: true });
      });
      await page.locator('.mutation-form button[type="submit"]').click();
      await expect(page.locator(".mutation-form")).toHaveAttribute("aria-busy", "true");
      await expect(page.locator('.mutation-form button[type="submit"]')).toBeDisabled();
      await expect(page.locator("#submission-status")).not.toBeEmpty();
      await page.screenshot({
        path: testInfo.outputPath(`e5-submitting-${language}-${theme}.png`),
        fullPage: true,
      });
      await page.reload();
      await page.locator("#mutation-bucket").fill("INVALID");
      await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
      const posted = page.waitForRequest(
        (request) => request.method() === "POST" && request.url().endsWith("/s3/write/bucket"),
      );
      await page.locator('.mutation-form button[type="submit"]').click();
      const payload = new URLSearchParams((await posted).postData());
      expect(payload.get("bucket")).toBe("INVALID");
      expect(payload.get("confirm")).toBe("yes");
      expect(payload.get("csrf-token")).toBeTruthy();
      await expect(page.locator("#mutation-bucket")).toHaveAttribute("aria-invalid", "true");
      await expect(page.locator("#mutation-bucket")).toHaveAttribute(
        "aria-describedby",
        "bucket-error",
      );
      await expect(page.locator(".mutation-form")).toHaveAttribute("aria-busy", "false");
      await expect(page.locator('.mutation-form button[type="submit"]')).toBeEnabled();
      await page.screenshot({
        path: testInfo.outputPath(`e5-validation-${language}-${theme}.png`),
        fullPage: true,
      });
      await page.goBack();
      await expect(page.locator(".mutation-form")).toHaveAttribute("aria-busy", "false");
      await expect(page.locator('.mutation-form button[type="submit"]')).toBeEnabled();
    });
  }
}

test("native validation remains usable without JavaScript", async ({ browser, baseURL }) => {
  const context = await browser.newContext({ baseURL, javaScriptEnabled: false });
  try {
    const page = await context.newPage();
    await page.goto("/s3/write?action=bucket&lang=en");
    await page.locator("#mutation-bucket").fill("INVALID");
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    await page.getByRole("button", { name: "Create bucket", exact: true }).click();
    await expect(page.locator("#bucket-error")).toBeVisible();
    await expect(page.locator("#mutation-bucket")).toHaveAttribute("aria-invalid", "true");
  } finally {
    await context.close();
  }
});
