import { test, expect } from "@playwright/test";
// Signed bearer URLs must not be captured in traces or automatic screenshots.
test.use({ trace: "off", screenshot: "off" });
import {
  createOwned,
  removeOwned,
  readyDialog,
  stableOperation,
  emulator,
} from "./operation-fixtures.mjs";
const owned = new Map();
test.beforeEach(async ({ request }, info) => {
  owned.set(info.testId, await createOwned(request, ["original.txt", "existing.txt"]));
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});
async function openObject(page, bucket) {
  await page.goto(`/s3/object?bucket=${bucket}&key=original.txt&lang=en&theme=light`);
}
test("copy conflict can be corrected, then move retains bytes", async ({ page, request }, info) => {
  const fixture = owned.get(info.testId);
  await request.put(`${emulator}/${fixture.bucket}/existing.txt`, { data: "old destination" });
  await openObject(page, fixture.bucket);
  await page.getByRole("link", { name: "Copy object", exact: true }).click();
  const dialog = page.locator("#action-dialog");
  await dialog.locator("#transfer-key").fill("existing.txt");
  await dialog.locator('button[type="submit"]').click();
  await expect(dialog.getByRole("link", { name: "Change destination", exact: true })).toBeVisible({
    timeout: 15000,
  });
  await dialog.getByRole("link", { name: "Change destination", exact: true }).click();
  await expect(dialog.locator("#transfer-key")).toHaveValue("existing.txt");
  await dialog.locator("#transfer-conflict").selectOption("replace");
  await dialog.locator('button[type="submit"]').click();
  let ready = await readyDialog(page);
  await ready.dialog.locator('button[type="submit"]').click();
  await expect(dialog).not.toBeVisible();
  expect((await stableOperation(request, ready.id)).state).toBe("SUCCEEDED");
  expect(await (await request.get(`${emulator}/${fixture.bucket}/existing.txt`)).text()).toBe(
    "original bytes",
  );
  await page.goto(`/operations/${ready.id}?lang=en&theme=light`);
  await expect(page.getByRole("heading", { name: "Copy object", exact: true })).toBeVisible();
  await expect(page.locator("main").getByRole("status")).toContainText("Completed");
  await expect(page.locator('.primary-navigation a[href^="/operations"]')).toHaveCount(0);
  await page.screenshot({ path: info.outputPath("completed-operation.png"), fullPage: true });
  await page.goto("/operations?lang=en&theme=light");
  await expect(page.locator("tbody tr").filter({ hasText: fixture.bucket }).first()).toContainText(
    "Copy object",
  );
  await page.screenshot({ path: info.outputPath("operations-list.png"), fullPage: true });
  await openObject(page, fixture.bucket);
  await page.getByRole("link", { name: "Move object", exact: true }).click();
  await dialog.locator("#transfer-key").fill("moved.txt");
  fixture.keys.add("moved.txt");
  await dialog.locator('button[type="submit"]').click();
  ready = await readyDialog(page);
  await ready.dialog.locator('button[type="submit"]').click();
  expect((await stableOperation(request, ready.id)).state).toBe("SUCCEEDED");
  await expect(dialog).not.toBeVisible();
  expect((await request.get(`${emulator}/${fixture.bucket}/original.txt`)).status()).toBe(404);
  expect(await (await request.get(`${emulator}/${fixture.bucket}/moved.txt`)).text()).toBe(
    "original bytes",
  );
});
test("upload conflict offers a new name and explicit replacement", async ({
  page,
  request,
}, info) => {
  const fixture = owned.get(info.testId),
    dialog = page.locator("#action-dialog");
  async function conflict() {
    await page.goto(`/s3/bucket?bucket=${fixture.bucket}&prefix=&lang=en&theme=light`);
    await page.locator('.page-actions a[href*="action=upload"]').click();
    await dialog.locator('input[type="file"]').setInputFiles({
      name: "original.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("new upload bytes"),
    });
    await dialog.locator('button[type="submit"]').click();
    await expect(dialog.locator('input[name="replacementTicket"]')).toHaveValue(
      /^[A-Za-z0-9_-]{43}$/,
    );
  }
  await conflict();
  fixture.keys.add("renamed.txt");
  await dialog.locator("#upload-filename").fill("renamed.txt");
  await dialog.locator('input[type="file"]').setInputFiles({
    name: "original.txt",
    mimeType: "text/plain",
    buffer: Buffer.from("new upload bytes"),
  });
  await dialog.locator('button[type="submit"]').click();
  await expect(dialog).not.toBeVisible();
  expect(await (await request.get(`${emulator}/${fixture.bucket}/renamed.txt`)).text()).toBe(
    "new upload bytes",
  );
  expect(await (await request.get(`${emulator}/${fixture.bucket}/original.txt`)).text()).toBe(
    "original bytes",
  );
  await conflict();
  await dialog.locator("#upload-decision").selectOption("replace");
  await dialog.locator('input[type="file"]').setInputFiles({
    name: "original.txt",
    mimeType: "text/plain",
    buffer: Buffer.from("replacement bytes"),
  });
  await dialog.locator('button[type="submit"]').click();
  await expect(dialog).not.toBeVisible();
  expect(await (await request.get(`${emulator}/${fixture.bucket}/original.txt`)).text()).toBe(
    "replacement bytes",
  );
});
test.describe("signed downloads", () => {
  test("explicit generation, selectable URL and real clipboard result", async ({
    page,
    request,
  }, info) => {
    const fixture = owned.get(info.testId);
    await page.context().grantPermissions(["clipboard-read", "clipboard-write"]);
    await openObject(page, fixture.bucket);
    await page.getByRole("link", { name: "Share download", exact: true }).click();
    const dialog = page.locator("#action-dialog");
    await expect(dialog.locator("#share-minutes")).toHaveValue("15");
    await dialog.locator('button[type="submit"]').click();
    const url = await dialog.locator("#signed-download").inputValue();
    expect(url.includes("X-Amz-Signature=") && url.includes("X-Amz-Expires=900")).toBe(true);
    await dialog.locator("[data-copy-field]").click();
    await expect(dialog.locator(".copy-status")).toContainText("Copied");
    expect((await page.evaluate(() => window.navigator.clipboard.readText())) === url).toBe(true);
    const response = await request.get(url);
    expect(response.status()).toBe(200);
    expect(await response.text()).toBe("original bytes");
    expect(response.headers()["content-disposition"].includes("original.txt")).toBe(true);
    await expect(dialog.locator("details,summary")).toHaveCount(0);
  });
});
