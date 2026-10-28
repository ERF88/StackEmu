import { test, expect } from "@playwright/test";
import { createOwned, removeOwned } from "./operation-fixtures.mjs";
const owned = new Map();
test.beforeEach(async ({ request }, info) => {
  owned.set(info.testId, await createOwned(request, ["sample.txt"]));
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});

for (const lang of ["en", "pt-BR"]) {
  test(`upload opens once with its complete form: ${lang}`, async ({ page }, info) => {
    const fixture = owned.get(info.testId);
    await page.goto(
      `/s3/bucket?bucket=${fixture.bucket}&lang=${lang}&theme=${lang === "en" ? "light" : "dark"}`,
    );
    let release;
    let requests = 0;
    const gate = new Promise((resolve) => {
      release = resolve;
    });
    await page.route("**/s3/write?*", async (route) => {
      requests++;
      const response = await route.fetch();
      await gate;
      await route.fulfill({ response });
    });
    const trigger = page.locator('.page-actions a[href*="action=upload"]');
    const dialog = page.locator("#action-dialog");
    try {
      await trigger.click();
      await expect(trigger).toHaveAttribute("aria-busy", "true");
      await expect(trigger).toHaveAttribute("data-opening-visible", "");
      await expect(dialog).not.toBeVisible();
      await trigger.click();
      expect(requests).toBe(1);
      release();
      await expect(dialog).toBeVisible();
      await expect(dialog.locator('input[type="file"]')).toHaveCount(1);
      await expect(dialog.locator('input[type="file"]')).toBeVisible();
      await expect(dialog.locator("[data-action-status]")).toBeEmpty();
      await expect(trigger).not.toHaveAttribute("aria-busy");
      await page.keyboard.press("Escape");
      await expect(trigger).toBeFocused();
    } finally {
      release();
    }
  });
}

test("pending form read can be cancelled and unmarked failure stays explicit", async ({
  page,
}, info) => {
  const fixture = owned.get(info.testId);
  await page.goto(`/s3/bucket?bucket=${fixture.bucket}&lang=en&theme=light`);
  let release, finished;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  const settled = new Promise((resolve) => {
    finished = resolve;
  });
  await page.route("**/s3/write?*", async (route) => {
    const response = await route.fetch();
    await gate;
    try {
      await route.fulfill({ response });
    } catch {
      /* The explicit Escape aborted this read. */
    } finally {
      finished();
    }
  });
  const trigger = page.locator('.page-actions a[href*="action=upload"]');
  const dialog = page.locator("#action-dialog");
  await trigger.click();
  await expect(trigger).toHaveAttribute("aria-busy", "true");
  await page.keyboard.press("Escape");
  release();
  await settled;
  await expect(dialog).not.toBeVisible();
  await expect(trigger).toBeFocused();
  await expect(trigger).not.toHaveAttribute("aria-busy");
  await page.unroute("**/s3/write?*");
  await page.route("**/s3/write?*", (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/html",
      body: '<div id="action-workspace"><p>Unmarked fixture</p></div>',
    }),
  );
  await trigger.click();
  await expect(dialog).toBeVisible();
  await expect(dialog.locator("[data-action-status]")).not.toBeEmpty();
  await expect(dialog).not.toContainText("Unmarked fixture");
  await expect(dialog.locator("form")).toHaveCount(0);
  await expect(dialog.locator("[data-action-close]")).toBeEnabled();
});
