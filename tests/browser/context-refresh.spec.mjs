import { test, expect } from "@playwright/test";
import { createOwned, removeOwned, readyDialog } from "./operation-fixtures.mjs";
const owned = new Map();
test.beforeEach(async ({ request }, info) => {
  owned.set(info.testId, await createOwned(request, ["sample.txt"]));
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});

test("context refresh retains filter/focus and rejects an unmarked response", async ({
  page,
}, info) => {
  const fixture = owned.get(info.testId);
  await page.goto(`/s3/bucket?bucket=${fixture.bucket}&lang=en&theme=light`);
  await page.locator("#object-filter").fill("sample");
  let documents = 0;
  page.on("request", (request) => {
    if (request.isNavigationRequest() && request.resourceType() === "document") documents++;
  });
  const refresh = page.locator("[data-refresh-objects]");
  await refresh.focus();
  await page.keyboard.press("Enter");
  await expect(page.locator("#object-list-panel")).toHaveAttribute("aria-busy", "false");
  await expect(page.locator("#object-filter")).toHaveValue("sample");
  await expect(refresh).toBeFocused();
  expect(documents).toBe(0);
  await page.route("**/s3/bucket?*", async (route) => {
    const response = await route.fetch();
    const headers = { ...response.headers() };
    delete headers["x-stackemu-fragment"];
    await route.fulfill({ response, headers });
  });
  await refresh.click();
  await expect(page.locator("#object-loading")).toContainText("Refresh failed.");
  await expect(page.locator("#object-filter")).toHaveValue("sample");
  await expect(page.locator(".object-table tbody tr:visible")).toHaveCount(1);
  expect(documents).toBe(0);
});

test("creation and durable deletion update the listing and notice without document navigation", async ({
  page,
}, info) => {
  const fixture = owned.get(info.testId);
  fixture.keys.add("made/");
  await page.goto(`/s3/bucket?bucket=${fixture.bucket}&lang=pt-BR&theme=dark`);
  await page.locator("#object-filter").fill("sample");
  let documents = 0;
  page.on("request", (request) => {
    if (request.isNavigationRequest() && request.resourceType() === "document") documents++;
  });
  await page.locator('.page-actions a[href*="action=folder"]').click();
  await page.locator("#mutation-key").fill("made/");
  await page.locator('#action-dialog button[type="submit"]').click();
  await expect(page.locator("#action-dialog")).not.toBeVisible();
  await expect(page.locator("[data-operation-notice]")).toBeVisible();
  await expect(page.locator("#object-list-panel")).toHaveAttribute("aria-busy", "false");
  await expect(page.locator("#object-filter")).toHaveValue("sample");
  await page.getByRole("link", { name: "Excluir objeto: sample.txt", exact: true }).click();
  const ready = await readyDialog(page);
  await ready.dialog.locator('button[type="submit"]').click();
  await expect(page.locator("#action-dialog")).not.toBeVisible();
  await expect(page.locator("[data-notice-message]")).toHaveText("Operação concluída.", {
    timeout: 15000,
  });
  await expect(page.locator(".object-table a[data-object-detail]")).toHaveCount(0);
  await expect(page.locator("#object-filter")).toHaveValue("sample");
  expect(documents).toBe(0);
  for (const width of [390, 1366, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    const bounds = await page.locator("[data-operation-notice]").boundingBox();
    expect(bounds.x).toBeGreaterThanOrEqual(0);
    expect(bounds.x + bounds.width).toBeLessThanOrEqual(width);
    expect(bounds.y + bounds.height).toBeLessThanOrEqual(900);
  }
  await page.locator("[data-notice-dismiss]").click();
  await expect(page.locator("[data-operation-notice]")).not.toBeVisible();
});
