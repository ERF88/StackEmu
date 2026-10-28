import { test, expect } from "@playwright/test";
import { createHash } from "node:crypto";
import { createOwned, removeOwned } from "./operation-fixtures.mjs";
const owned = new Map();
test.beforeEach(async ({ request }, info) => {
  owned.set(info.testId, await createOwned(request, ["sample.txt"]));
});
test.afterEach(async ({ playwright }, info) => {
  await removeOwned(playwright, owned.get(info.testId));
  owned.delete(info.testId);
});

for (const lang of ["en", "pt-BR"])
  for (const theme of ["light", "dark"]) {
    test(`first reveal is initialized and Download is a neutral icon: ${lang}, ${theme}`, async ({
      page,
    }, info) => {
      const fixture = owned.get(info.testId);
      await page.addInitScript(() => {
        window.revealStates = [];
        const reveal = window.HTMLDialogElement.prototype.showModal;
        window.HTMLDialogElement.prototype.showModal = function () {
          if (this.id === "action-dialog" || this.id === "object-drawer")
            window.revealStates.push({
              id: this.id,
              busy: this.querySelector(
                "#action-dialog-content, #object-drawer-content",
              )?.getAttribute("aria-busy"),
              status: this.querySelector("[data-action-status], #drawer-status")?.textContent || "",
              file: Boolean(this.querySelector('input[type="file"]')),
              hintHidden: this.querySelector("[data-upload-drop-hint]")?.hidden,
            });
          return reveal.call(this);
        };
      });
      const response = await page.goto(
        `/s3/bucket?bucket=${fixture.bucket}&lang=${lang}&theme=${theme}`,
      );
      expect(response.headers()["cache-control"]).toBe("no-store");
      await page.locator('.page-actions a[href*="action=upload"]').click();
      await expect(page.locator('#action-dialog input[type="file"]')).toBeVisible();
      const first = await page.evaluate(() => window.revealStates[0]);
      expect(first).toMatchObject({
        id: "action-dialog",
        busy: "false",
        status: "",
        file: true,
        hintHidden: false,
      });
      await page.locator("[data-action-close]").click();
      await page.getByRole("link", { name: "sample.txt", exact: true }).click();
      const download = page.locator('#object-drawer a[href^="/s3/download?"]');
      await expect(download).toBeVisible();
      await page.mouse.move(0, 0);
      await expect(download).toHaveClass("button icon-action");
      await expect(download).toHaveCSS("background-color", "rgba(0, 0, 0, 0)");
      await expect(download).not.toBeFocused();
      await expect(download).toHaveCSS("box-shadow", "none");
      expect(await page.evaluate(() => window.revealStates.at(-1).busy)).toBe("false");
      await page.locator('#object-drawer a[href^="/s3/transfer?"]').first().click();
      await expect(page.locator("#transfer-bucket")).toBeVisible();
      await page.locator("[data-action-close]").click();
      await page.mouse.move(0, 0);
      await expect(download).not.toBeFocused();
      await expect(download).toHaveCSS("background-color", "rgba(0, 0, 0, 0)");
      const canonical = await page.request.get(
        `/s3/object?bucket=${fixture.bucket}&key=sample.txt&lang=${lang}&theme=${theme}`,
      );
      expect(canonical.headers()["cache-control"]).toBe("no-store");
    });
  }

test("HTML is not stored while asset fingerprints match the actual immutable bytes", async ({
  page,
}) => {
  const response = await page.goto("/?lang=en&theme=auto");
  expect(response.headers()["cache-control"]).toBe("no-store");
  for (const selector of ['script[src^="/app.js?"]', 'link[href^="/app.css?"]']) {
    const element = page.locator(selector);
    const href = await element.getAttribute(selector.startsWith("script") ? "src" : "href");
    const asset = await page.request.get(href);
    expect(asset.status()).toBe(200);
    expect(asset.headers()["cache-control"]).toContain("immutable");
    expect(new URL(href, page.url()).searchParams.get("v")).toBe(
      createHash("sha256")
        .update(await asset.body())
        .digest("hex"),
    );
  }
});

for (const theme of ["light", "dark"]) {
  test(`table Download is not highlighted by another row control: ${theme}`, async ({
    page,
  }, info) => {
    const fixture = owned.get(info.testId);
    await page.goto(`/s3/bucket?bucket=${fixture.bucket}&lang=pt-BR&theme=${theme}`);
    const row = page.locator(".object-table tbody tr").filter({ hasText: "sample.txt" });
    const download = row.locator('a[href^="/s3/download?"]');
    const details = row.locator("a[data-object-detail][aria-label]");
    for (const control of [
      row.locator("th a"),
      details,
      row.locator('a[href^="/s3/delete-object?"]'),
    ]) {
      await control.focus();
      await expect(download).not.toBeFocused();
      await expect(download).toHaveCSS("border-top-color", "rgba(0, 0, 0, 0)");
      await expect(download).toHaveCSS("box-shadow", "none");
      await expect(download).toHaveCSS("text-decoration-line", "none");
    }
    await download.focus();
    await page.keyboard.press("Tab");
    await page.keyboard.press("Shift+Tab");
    await expect(download).toBeFocused();
    await expect(download).toHaveCSS("outline-style", "solid");
    await details.focus();
    await expect(download).not.toBeFocused();
    await expect(download).toHaveCSS("box-shadow", "none");
    await expect(download).toHaveCSS("border-top-color", "rgba(0, 0, 0, 0)");
    await page.screenshot({ path: info.outputPath(`row-focus-${theme}.png`) });
  });
}
