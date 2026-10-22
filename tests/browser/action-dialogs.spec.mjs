import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulator = "http://127.0.0.1:4566";
const owned = new Map();
const keys = ["target.txt", "neighbor.txt", "created/", "original.txt"];

test.beforeEach(async ({ request }, info) => {
  const bucket = `stackemu-dialog-${randomUUID().replaceAll("-", "")}`;
  expect((await request.put(`${emulator}/${bucket}`)).status()).toBe(200);
  owned.set(info.testId, bucket);
  for (const key of keys.slice(0, 2)) {
    expect(
      (
        await request.put(`${emulator}/${bucket}/${key}`, {
          data: "dialog fixture",
          headers: { "Content-Type": "text/plain" },
        })
      ).status(),
    ).toBe(200);
  }
});

test.afterEach(async ({ playwright }, info) => {
  const bucket = owned.get(info.testId);
  if (!bucket) return;
  const cleanup = await playwright.request.newContext();
  try {
    for (const key of keys) {
      expect([204, 404]).toContain(
        (await cleanup.delete(`${emulator}/${bucket}/${encodeURIComponent(key)}`)).status(),
      );
    }
    expect((await cleanup.delete(`${emulator}/${bucket}`)).status()).toBe(204);
  } finally {
    owned.delete(info.testId);
    await cleanup.dispose();
  }
});

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["light", "dark"]) {
    test(`interactive actions: ${language}, ${theme}`, async ({ page, request }, info) => {
      const bucket = owned.get(info.testId);
      const query = `lang=${language}&theme=${theme}&density=compact`;
      const listing = `/s3/bucket?bucket=${bucket}&prefix=&${query}`;
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.goto(listing);
      const dialog = page.locator("#action-dialog");
      const close = dialog.locator("[data-action-close]");
      const folder = page.locator('.page-actions a[href*="action=folder"]');
      await folder.click();
      await expect(dialog).toBeVisible();
      await expect(page.locator("#mutation-key")).toBeFocused();
      await expect(dialog.getByRole("checkbox")).toHaveCount(0);
      await page.keyboard.press("Escape");
      await expect(folder).toBeFocused();
      expect((await request.get(`${emulator}/${bucket}/created%2F`)).status()).toBe(404);
      await folder.click();
      await dialog.locator("#mutation-key").fill("created/");
      await dialog.locator('button[type="submit"]').click();
      await expect(dialog.locator("[data-operation-success]")).toHaveCount(1);
      await expect(page).toHaveURL(new RegExp(`/s3/bucket\\?bucket=${bucket}`));
      await close.click();
      await expect(dialog).not.toBeVisible();
      expect((await request.get(`${emulator}/${bucket}/created%2F`)).status()).toBe(200);

      await page.locator('.page-actions a[href*="action=upload"]').click();
      await dialog.locator('input[type="file"]').setInputFiles({
        name: "original.txt",
        mimeType: "text/plain",
        buffer: Buffer.from("original bytes\n"),
      });
      await dialog.locator('button[type="submit"]').click();
      await expect(dialog.locator("[data-operation-success]")).toHaveCount(1);
      expect(await (await request.get(`${emulator}/${bucket}/original.txt`)).text()).toBe(
        "original bytes\n",
      );
      await close.click();
      await expect(dialog).not.toBeVisible();

      const row = page.locator(".object-table tbody tr").filter({ hasText: "target.txt" });
      const deletion = row.locator('a[href^="/s3/delete-object?"]');
      await deletion.click();
      await expect(dialog.locator('input[name="ticket"]')).toHaveValue(/^[A-Za-z0-9_-]{43}$/);
      await expect(close).toBeFocused();
      await expect(dialog.locator(".object-key")).toHaveText("target.txt");
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
        await page.screenshot({
          path: info.outputPath(`confirmation-${width}.png`),
          fullPage: true,
        });
      }
      await page.keyboard.press("Escape");
      await expect(deletion).toBeFocused();
      expect((await request.get(`${emulator}/${bucket}/target.txt`)).status()).toBe(200);
      await deletion.click();
      await expect(dialog.locator('input[name="ticket"]')).toHaveCount(1);
      const response = page.waitForResponse(
        (r) => r.request().method() === "POST" && r.url().includes("/s3/delete-object"),
      );
      await dialog.locator('button[type="submit"]').click();
      expect((await response).status()).toBe(200);
      await expect(dialog.locator("[data-action-completed]")).toHaveCount(1);
      expect((await request.get(`${emulator}/${bucket}/target.txt`)).status()).toBe(404);
      expect((await request.get(`${emulator}/${bucket}/neighbor.txt`)).status()).toBe(200);
      await close.click();
      await expect(page).toHaveURL(new RegExp(`/s3/bucket\\?bucket=${bucket}.*density=compact`));
      await expect(
        page.locator(".object-table tbody tr").filter({ hasText: "target.txt" }),
      ).toHaveCount(0);
    });
  }
}

test("creation validation, stale reads and unmarked mutation failure", async ({ page }, info) => {
  const bucket = owned.get(info.testId);
  await page.goto("/s3?lang=en&theme=light");
  const dialog = page.locator("#action-dialog");
  const create = page.locator('.page-actions a[href*="action=bucket"]');
  await create.click();
  await dialog.locator("#mutation-bucket").fill("INVALID");
  await dialog.locator('button[type="submit"]').click();
  await expect(dialog.locator("#mutation-bucket")).toHaveAttribute("aria-invalid", "true");
  await expect(dialog.locator("#mutation-bucket")).toBeFocused();
  await expect(dialog.locator('button[type="submit"]')).toBeEnabled();
  await page.keyboard.press("Escape");
  let release;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  await page.route("**/s3/write?*", async (route) => {
    const response = await route.fetch();
    await gate;
    await route.fulfill({ response });
  });
  await create.click();
  await page.keyboard.press("Escape");
  release();
  await expect(dialog).not.toBeVisible();
  await page.unroute("**/s3/write?*");
  await create.click();
  await dialog.locator("#mutation-bucket").fill(`${bucket}-not-created`);
  let posts = 0;
  await page.route("**/s3/write/bucket", (route) => {
    posts++;
    return route.fulfill({
      status: 502,
      contentType: "text/html",
      body: "<h1>Untrusted response</h1>",
    });
  });
  await dialog.locator('button[type="submit"]').click();
  await expect(dialog.locator("[data-action-status]")).toContainText("could not be confirmed");
  await expect(dialog.locator('button[type="submit"]')).toBeDisabled();
  await expect(dialog).not.toContainText("Untrusted response");
  await dialog.locator("form").evaluate((form) => form.requestSubmit());
  expect(posts).toBe(1);
  await expect(dialog.locator("[data-action-close]")).toBeEnabled();
});
