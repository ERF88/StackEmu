import { test, expect } from "@playwright/test";
import { createOwned, removeOwned, emulator } from "./operation-fixtures.mjs";
const owned = new Map();
test.beforeEach(async ({ request }, info) => {
  owned.set(info.testId, await createOwned(request, ["sample.txt"]));
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});

test("deletion opens a checked review without an empty loading dialog or mutation", async ({
  page,
  request,
}, info) => {
  const fixture = owned.get(info.testId);
  await page.goto(`/s3/bucket?bucket=${fixture.bucket}&lang=en&theme=light`);
  let release,
    posts = 0;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  page.on("request", (req) => {
    if (req.method() === "POST") posts++;
  });
  await page.route("**/s3/delete-object?*", async (route) => {
    const response = await route.fetch();
    await gate;
    await route.fulfill({ response });
  });
  const action = page.getByRole("link", { name: "Delete object: sample.txt", exact: true });
  try {
    await action.click();
    await expect(action).toHaveAttribute("data-opening-visible", "");
    await expect(page.locator("#action-dialog")).not.toBeVisible();
    release();
    await expect(page.locator("#action-dialog #action-workspace")).toBeVisible();
    await expect(page.locator("#action-dialog")).toContainText("sample.txt");
    expect(posts).toBe(0);
    expect(await (await request.get(`${emulator}/${fixture.bucket}/sample.txt`)).text()).toBe(
      "original bytes",
    );
  } finally {
    release();
  }
});

test("inspector opens complete and keeps current content during a slow child read", async ({
  page,
}, info) => {
  const fixture = owned.get(info.testId);
  await page.goto(`/s3/bucket?bucket=${fixture.bucket}&lang=pt-BR&theme=dark`);
  let release;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  await page.route("**/s3/object-panel?*", async (route) => {
    const response = await route.fetch();
    await gate;
    await route.fulfill({ response });
  });
  const link = page.getByRole("link", { name: "sample.txt", exact: true });
  try {
    await link.click();
    await expect(link).toHaveAttribute("data-opening-visible", "");
    await expect(page.locator("#object-drawer")).not.toBeVisible();
    release();
    await expect(page.locator("#object-drawer #object-name")).toHaveText("sample.txt");
  } finally {
    release();
  }
  let releaseChild;
  const childGate = new Promise((resolve) => {
    releaseChild = resolve;
  });
  await page.route("**/s3/object-metadata?*", async (route) => {
    const response = await route.fetch();
    await childGate;
    await route.fulfill({ response });
  });
  try {
    const child = page.locator('#object-drawer a[href^="/s3/object-metadata?"]');
    await child.click();
    await expect(child).toHaveAttribute("data-opening-visible", "");
    await expect(page.locator("#object-drawer #object-name")).toHaveText("sample.txt");
    await expect(page.locator("#object-drawer-content")).toHaveAttribute("aria-busy", "true");
    releaseChild();
    await expect(page.locator("#object-inspector-view h1")).toHaveText("Detalhes técnicos");
    await expect(page.locator("[data-drawer-close]")).toBeFocused();
  } finally {
    releaseChild();
  }
});

test("slow clipboard uses the same feedback without a modal", async ({ page }, info) => {
  const fixture = owned.get(info.testId);
  await page.addInitScript(() => {
    Object.defineProperty(window.navigator, "clipboard", {
      value: {
        writeText: () =>
          new Promise((resolve) => {
            window.releaseCopy = resolve;
          }),
      },
    });
  });
  await page.goto(`/s3/bucket?bucket=${fixture.bucket}&lang=en&theme=light`);
  await page.getByRole("link", { name: "sample.txt", exact: true }).click();
  const copy = page.getByRole("button", { name: "Copy object key", exact: true });
  await copy.click();
  await expect(copy).toHaveAttribute("data-opening-visible", "");
  await expect(copy).toBeDisabled();
  await expect(page.locator("#action-dialog")).not.toBeVisible();
  await page.evaluate(() => window.releaseCopy());
  await expect(copy).toBeEnabled();
  await expect(copy).not.toHaveAttribute("aria-busy");
  await expect(page.locator(".copy-status")).toHaveText("Copied to clipboard.");
});
