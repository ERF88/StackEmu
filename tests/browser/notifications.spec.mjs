import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulator = process.env.STACKEMU_BROWSER_EMULATOR || "http://127.0.0.1:4566";
const endpoint = new URL(emulator);
if (
  endpoint.protocol !== "http:" ||
  endpoint.port !== "4566" ||
  !["localhost", "127.0.0.1", "ministack", "host.docker.internal"].includes(endpoint.hostname)
)
  throw new Error("Notification tests require local MiniStack");

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    test(`confirmed notifications retain results and keyboard focus: ${language}, ${theme}`, async ({
      page,
      request,
    }, testInfo) => {
      const bucket = `stackemu-notice-${randomUUID().replaceAll("-", "")}`;
      const errors = [];
      page.on("pageerror", (error) => errors.push(error.message));
      page.on("console", (message) => {
        if (message.type() === "error" && /Content Security Policy|Refused to/.test(message.text()))
          errors.push(message.text());
      });
      const completed = language === "en" ? "Operation completed" : "Operação concluída";
      const notice = page.locator("[data-operation-notice]");
      const message = page.locator("[data-notice-message]");
      try {
        await page.goto(`/s3/write?action=bucket&lang=${language}&theme=${theme}`);
        await expect(notice).toBeHidden();
        await page.locator("#mutation-bucket").fill(bucket);
        await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
        await page.locator('button[type="submit"]').click();
        await expect(notice).toBeVisible();
        await expect(message).toHaveText(completed);
        await expect(message).toHaveAttribute("role", "status");
        await expect(message).toHaveAttribute("aria-live", "polite");
        const heading = page.locator("h1[data-operation-success]");
        await expect(heading).not.toBeFocused();
        const originalTarget = await page.locator("#content").innerText();
        for (const width of [390, 1440]) {
          await page.setViewportSize({ width, height: 900 });
          expect(
            await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
          ).toBe(true);
          const result = await new AxeBuilder({ page })
            .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
            .analyze();
          expect(result.violations).toEqual([]);
          await page.screenshot({
            path: testInfo.outputPath(`notice-${language}-${theme}-${width}.png`),
            fullPage: true,
          });
        }
        // No timer exists: repeated lifecycle events must not reannounce or replace content.
        await page.evaluate(() =>
          window.dispatchEvent(new window.PageTransitionEvent("pageshow", { persisted: true })),
        );
        await expect(notice).toBeVisible();
        expect(await page.locator("#content").innerText()).toBe(originalTarget);
        await page.locator("[data-notice-dismiss]").focus();
        await page.keyboard.press("Enter");
        await expect(notice).toBeHidden();
        await expect(heading).toBeFocused();
        await expect(page.locator("#content")).toContainText(bucket);
        await page.locator(".action-help summary").click();
        await expect(page.locator(".command-preview")).toBeVisible();
        await page.evaluate(() =>
          window.dispatchEvent(new window.PageTransitionEvent("pageshow", { persisted: true })),
        );
        await expect(notice).toBeHidden();

        await page.goto(`/s3/write?action=folder&bucket=${bucket}&lang=${language}&theme=${theme}`);
        await page.locator("#mutation-key").fill("notice/");
        await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
        await page.locator('button[type="submit"]').click();
        await expect(message).toHaveText(completed);
        await expect(message).toHaveAttribute("aria-live", "polite");

        await page.goto(
          `/s3/write?action=upload&bucket=${bucket}&prefix=notice%2F&lang=${language}&theme=${theme}`,
        );
        await page.locator("#mutation-file").setInputFiles({
          name: "original.txt",
          mimeType: "text/plain",
          buffer: Buffer.from("notice bytes"),
        });
        await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
        await page.locator('button[type="submit"]').click();
        await expect(message).toHaveText(completed);
        await expect(message).toHaveAttribute("aria-live", "off");
        await expect(message).not.toHaveAttribute("role", "status");
        await expect(heading).toBeFocused();
        expect((await request.get(`${emulator}/${bucket}/notice/original.txt`)).status()).toBe(200);
        expect(errors).toEqual([]);
      } finally {
        for (const key of ["notice/original.txt", "notice/"])
          expect(
            (await request.delete(`${emulator}/${bucket}/${encodeURIComponent(key)}`)).status(),
          ).toBe(204);
        expect((await request.delete(`${emulator}/${bucket}`)).status()).toBe(204);
      }
    });
  }
}

test("native success without JavaScript retains authoritative result without toast", async ({
  browser,
  baseURL,
  request,
}) => {
  const bucket = `stackemu-notice-${randomUUID().replaceAll("-", "")}`;
  const context = await browser.newContext({ javaScriptEnabled: false, baseURL });
  try {
    const page = await context.newPage();
    await page.goto("/s3/write?action=bucket&lang=en");
    await page.locator("#mutation-bucket").fill(bucket);
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    await page.locator('button[type="submit"]').click();
    await expect(page.getByRole("heading", { name: "Operation completed" })).toBeVisible();
    await expect(page.locator("#content")).toContainText(bucket);
    await expect(page.locator("[data-operation-notice]")).toBeHidden();
    await page.locator(".action-help summary").click();
    await expect(page.locator(".command-preview")).toBeVisible();
  } finally {
    await context.close();
    expect((await request.delete(`${emulator}/${bucket}`)).status()).toBe(204);
  }
});
