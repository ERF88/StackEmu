import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulator = "http://127.0.0.1:4566";
const owned = new Map();
const filename = "ação +%.TXT";
const key = `picker/${filename}`;
const payload = "single picker original bytes\n";

test.beforeEach(async ({ request }, info) => {
  const bucket = `stackemu-picker-${randomUUID().replaceAll("-", "")}`;
  expect((await request.put(`${emulator}/${bucket}`)).status()).toBe(200);
  owned.set(info.testId, bucket);
});

test.afterEach(async ({ request }, info) => {
  const bucket = owned.get(info.testId);
  if (!bucket) return;
  expect((await request.delete(`${emulator}/${bucket}/${encodeURIComponent(key)}`)).status()).toBe(
    204,
  );
  expect((await request.delete(`${emulator}/${bucket}`)).status()).toBe(204);
  owned.delete(info.testId);
});

for (const javaScriptEnabled of [true, false]) {
  test.describe(`single file picker, JavaScript=${javaScriptEnabled}`, () => {
    test.use({ javaScriptEnabled });
    for (const lang of ["en", "pt-BR"]) {
      test(`one chooser and original upload: ${lang}`, async ({ page, request }, info) => {
        const bucket = owned.get(info.testId);
        await page.goto(
          `/s3/write?action=upload&bucket=${bucket}&prefix=picker%2F&lang=${lang}&theme=${lang === "en" ? "light" : "dark"}`,
        );
        const input = page.locator('input[type="file"]');
        await expect(input).toHaveCount(1);
        await expect(input).toBeVisible();
        await expect(page.locator("[data-upload-picker]")).toHaveCount(0);
        await expect(page.locator("[data-upload-drop] input[type=file]")).toHaveCount(1);
        if (javaScriptEnabled) await expect(page.locator("[data-upload-drop-hint]")).toBeVisible();
        else await expect(page.locator("[data-upload-drop-hint]")).toBeHidden();
        let chooserCount = 0;
        let posts = 0;
        page.on("filechooser", () => {
          chooserCount += 1;
        });
        page.on("request", (request) => {
          if (request.method() === "POST" && new URL(request.url()).pathname === "/s3/write/upload")
            posts += 1;
        });
        const chooserPromise = page.waitForEvent("filechooser");
        await input.focus();
        await page.keyboard.press("Space");
        const chooser = await chooserPromise;
        await chooser.setFiles({
          name: filename,
          mimeType: "text/plain",
          buffer: Buffer.from(payload),
        });
        expect(chooserCount).toBe(1);
        expect(posts).toBe(0);
        if (javaScriptEnabled) {
          await page.locator("[data-upload-drop]").evaluate(
            (zone, content) => {
              const transfer = new window.DataTransfer();
              transfer.items.add(
                new window.File([content.payload], content.filename, { type: "text/plain" }),
              );
              zone.dispatchEvent(
                new window.DragEvent("drop", {
                  dataTransfer: transfer,
                  bubbles: true,
                  cancelable: true,
                }),
              );
            },
            { payload, filename },
          );
          await expect(input).toHaveValue(/ação \+%\.TXT$/);
        }
        for (const width of [390, 1366]) {
          await page.setViewportSize({ width, height: 900 });
          expect(
            await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
          ).toBe(true);
          if (javaScriptEnabled)
            expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
          await page.screenshot({ path: info.outputPath(`picker-${width}.png`), fullPage: true });
        }
        await page.locator('button[type="submit"]').click();
        await expect
          .poll(async () =>
            (await request.get(`${emulator}/${bucket}/${encodeURIComponent(key)}`)).status(),
          )
          .toBe(200);
        const result = await request.get(`${emulator}/${bucket}/${encodeURIComponent(key)}`);
        expect(await result.text()).toBe(payload);
        expect(result.headers()["content-type"]).toContain("text/plain");
        expect(posts).toBe(1);
      });
    }
  });
}
