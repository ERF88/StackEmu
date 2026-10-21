import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulatorURL = process.env.STACKEMU_BROWSER_EMULATOR || "http://127.0.0.1:4566";
const emulator = new URL(emulatorURL);
if (
  emulator.protocol !== "http:" ||
  emulator.port !== "4566" ||
  !["127.0.0.1", "localhost", "ministack", "host.docker.internal"].includes(emulator.hostname)
) {
  throw new Error("Drawer fixtures may only target the local emulator");
}
const bucket = `stackemu-drawer-${randomUUID().replaceAll("-", "")}`;
const key = "samples/ação +% <img src=x onerror=alert(1)>.txt";
const otherKey = "samples/second.json";
const controlKey = "samples/line\r\nend.txt";
const bucketPath = `/s3/bucket?bucket=${bucket}&prefix=samples%2F`;
let created = false;

const browserErrors = new WeakMap();
test.beforeEach(async ({ page }) => {
  const errors = [];
  browserErrors.set(page, errors);
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error" && /Content Security Policy|Refused to/.test(message.text())) {
      errors.push(message.text());
    }
  });
});
test.afterEach(async ({ page }) => {
  expect(browserErrors.get(page)).toEqual([]);
});

test.beforeAll(async ({ request }) => {
  expect((await request.put(`${emulatorURL}/${bucket}`)).status()).toBe(200);
  created = true;
  for (const ownedKey of [key, otherKey, controlKey]) {
    expect(
      (
        await request.put(`${emulatorURL}/${bucket}/${encodeURIComponent(ownedKey)}`, {
          data: "drawer fixture",
          headers: { "Content-Type": "text/plain" },
        })
      ).status(),
    ).toBe(200);
  }
});
test.afterAll(async ({ request }) => {
  if (!created) return;
  // Exact UUID-owned fixtures only, never discover or delete unrelated keys.
  for (const ownedKey of [key, otherKey, controlKey]) {
    expect(
      (await request.delete(`${emulatorURL}/${bucket}/${encodeURIComponent(ownedKey)}`)).status(),
    ).toBe(204);
  }
  expect((await request.delete(`${emulatorURL}/${bucket}`)).status()).toBe(204);
});

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    test(`drawer metadata, keyboard and clipboard contracts: ${language}, ${theme}`, async ({
      page,
    }, testInfo) => {
      // Simulate Clipboard API settlement, not OS clipboard or real permissions.
      await page.addInitScript(() => {
        Object.defineProperty(window, "isSecureContext", { value: true });
        Object.defineProperty(window.navigator, "clipboard", {
          value: {
            writeText: async (value) => {
              window.drawerCopiedValue = value;
            },
          },
        });
      });
      await page.goto(`${bucketPath}&lang=${language}&theme=${theme}`);
      const link = page.getByRole("link", { name: key, exact: true });
      const originalURL = page.url();
      await link.focus();
      await page.keyboard.press("Enter");
      const drawer = page.getByRole("dialog");
      await expect(drawer).toBeVisible();
      await expect(drawer.locator(".object-key")).toHaveText(key);
      const actions = drawer.locator(".object-panel-actions");
      await expect(actions.locator('a[href^="/s3/download?"]')).toBeVisible();
      await expect(actions.locator('a[href^="/s3/preview?"]')).toBeVisible();
      await expect(actions.locator('a[href^="/s3/delete-object?"]')).toBeVisible();
      for (const action of await actions.locator(".button").all())
        expect((await action.boundingBox()).height).toBeGreaterThanOrEqual(48);
      expect(
        await actions.evaluate((element) =>
          Boolean(
            element.compareDocumentPosition(document.querySelector(".response-policy")) &
              window.Node.DOCUMENT_POSITION_FOLLOWING,
          ),
        ),
      ).toBe(true);
      await expect(drawer.locator("img, script, iframe")).toHaveCount(0);
      await expect(page.locator("[data-drawer-close]")).toBeFocused();
      await page.keyboard.press("Shift+Tab");
      expect(await drawer.evaluate((element) => element.contains(document.activeElement))).toBe(
        true,
      );
      await page.keyboard.press("Tab");
      await expect(page.locator("[data-drawer-close]")).toBeFocused();
      await expect(drawer.locator("[data-drawer-page]")).toHaveAttribute("href", /\/s3\/object\?/);
      expect(page.url()).toBe(originalURL);
      expect(await page.evaluate(() => window.htmx.config.allowScriptTags)).toBe(false);
      const copyKey = drawer.getByRole("button", {
        name: language === "en" ? "Copy object key" : "Copiar chave do objeto",
        exact: true,
      });
      await copyKey.click();
      await expect(drawer.locator(".copy-status")).toHaveText(
        language === "en" ? "Copied to clipboard." : "Copiado para o clipboard.",
      );
      expect(await page.evaluate(() => window.drawerCopiedValue)).toBe(key);
      await drawer
        .getByRole("button", {
          name: language === "en" ? "Copy bucket name" : "Copiar nome do bucket",
          exact: true,
        })
        .click();
      expect(await page.evaluate(() => window.drawerCopiedValue)).toBe(bucket);
      for (const width of [1440, 390]) {
        await page.setViewportSize({ width, height: width === 390 ? 844 : 1000 });
        const bounds = await drawer.boundingBox();
        expect(bounds.x).toBeGreaterThanOrEqual(0);
        expect(bounds.x + bounds.width).toBeLessThanOrEqual(width + 1);
        if (width === 390) expect(bounds.width).toBe(width);
        expect(await drawer.evaluate((element) => element.scrollWidth <= element.clientWidth)).toBe(
          true,
        );
        const results = await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze();
        expect(results.violations).toEqual([]);
        await page.locator("[data-drawer-close]").focus();
        await drawer.screenshot({
          path: testInfo.outputPath(`drawer-${language}-${theme}-${width}.png`),
        });
        await page.keyboard.press("Tab");
        expect(await drawer.evaluate((element) => element.contains(document.activeElement))).toBe(
          true,
        );
      }
      await page.keyboard.press("Escape");
      await expect(drawer).not.toBeVisible();
      await expect(link).toBeFocused();
    });
  }
}

test("clipboard denial retains selected exact text and no false success", async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(window, "isSecureContext", { value: true });
    Object.defineProperty(window.navigator, "clipboard", {
      value: {
        writeText: async () => {
          throw new Error("denied fixture");
        },
      },
    });
  });
  await page.goto(`${bucketPath}&lang=en`);
  await page.getByRole("link", { name: key, exact: true }).click();
  await page.getByRole("button", { name: "Copy object key", exact: true }).click();
  await expect(page.locator(".copy-status")).toContainText("unavailable or denied");
  const field = page.getByLabel("Object key", { exact: true });
  await expect(field).toBeFocused();
  expect(
    await field.evaluate((element) =>
      element.value.slice(element.selectionStart, element.selectionEnd),
    ),
  ).toBe(key);
});

test("control-character keys keep exact API values and labelled encoded fallback", async ({
  page,
}) => {
  await page.addInitScript(() => {
    Object.defineProperty(window, "isSecureContext", { value: true });
    Object.defineProperty(window.navigator, "clipboard", {
      value: {
        writeText: async (value) => {
          window.drawerCopiedValue = value;
        },
      },
    });
  });
  await page.goto(`${bucketPath}&lang=en`);
  await page.locator('.object-name a[data-object-detail][href*="line%0D%0Aend"]').click();
  await expect(page.getByLabel("Object key", { exact: true })).toHaveValue(
    encodeURIComponent(controlKey),
  );
  await expect(page.locator("[data-encoded-note]")).toBeVisible();
  await page.getByRole("button", { name: "Copy object key", exact: true }).click();
  expect(await page.evaluate(() => window.drawerCopiedValue)).toBe(controlKey);
});

test("native secure-context Clipboard API copies exact key and bucket", async ({
  page,
  context,
}) => {
  await page.goto(`${bucketPath}&lang=en`);
  test.skip(
    !(await page.evaluate(() => window.isSecureContext)),
    "Real clipboard needs a trusted loopback origin; use run-browser-local.mjs in Docker",
  );
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  await page.getByRole("link", { name: key, exact: true }).click();
  await page.getByRole("button", { name: "Copy object key", exact: true }).click();
  await expect(page.locator(".copy-status")).toHaveText("Copied to clipboard.");
  expect(await page.evaluate(() => window.navigator.clipboard.readText())).toBe(key);
  await page.getByRole("button", { name: "Copy bucket name", exact: true }).click();
  expect(await page.evaluate(() => window.navigator.clipboard.readText())).toBe(bucket);
});

test("absent Clipboard API preserves manual copying", async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(window.navigator, "clipboard", { value: undefined });
  });
  await page.goto(`${bucketPath}&lang=en`);
  await page.getByRole("link", { name: key, exact: true }).click();
  await page.getByRole("button", { name: "Copy object key", exact: true }).click();
  await expect(page.locator(".copy-status")).toContainText("unavailable or denied");
  await expect(page.getByLabel("Object key", { exact: true })).toHaveValue(key);
});

test("missing objects retain error state without old metadata", async ({ page }) => {
  await page.goto(`${bucketPath}&lang=en`);
  await page.getByRole("link", { name: otherKey, exact: true }).evaluate((element) => {
    const endpoint = new URL(element.href);
    endpoint.searchParams.set("key", "absent-drawer-fixture.txt");
    element.href = endpoint.href;
  });
  const response = page.waitForResponse((value) => value.url().includes("/s3/object-panel?"));
  await page.getByRole("link", { name: otherKey, exact: true }).click();
  expect((await response).status()).toBe(404);
  await expect(page.locator("#drawer-status")).toContainText("Could not load");
  await expect(page.locator(".drawer-feedback")).toBeVisible();
  await expect(page.locator("#object-drawer-content")).toBeEmpty();
  await expect(page.getByRole("button", { name: "Retry details", exact: true })).toBeVisible();
});

test("ten-second browser timeout is visible and does not automatically retry", async ({ page }) => {
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  let requests = 0;
  await page.route("**/s3/object-panel?**", async (route) => {
    requests++;
    await held;
    try {
      await route.abort();
    } catch {
      /* The browser already timed out this held request. */
    }
  });
  await page.goto(`${bucketPath}&lang=en`);
  await page.getByRole("link", { name: key, exact: true }).click();
  await expect(page.locator("#drawer-status")).toContainText("Loading");
  try {
    await expect(page.locator("#drawer-status")).toContainText("Could not load", {
      timeout: 15000,
    });
    await expect(page.locator("#object-drawer-content")).toBeEmpty();
    expect(requests).toBe(1);
  } finally {
    release();
  }
});

test("closed requests cannot overwrite a reopened drawer with another selection", async ({
  page,
}) => {
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  let firstSeen;
  const seen = new Promise((resolve) => {
    firstSeen = resolve;
  });
  await page.route("**/s3/object-panel?**", async (route) => {
    if (new URL(route.request().url()).searchParams.get("key") !== key) return route.continue();
    const response = await route.fetch();
    firstSeen();
    await held;
    try {
      await route.fulfill({ response });
    } catch {
      /* The obsolete XHR was deliberately aborted. */
    }
  });
  await page.goto(`${bucketPath}&lang=en`);
  await page.getByRole("link", { name: key, exact: true }).click();
  await seen;
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).not.toBeVisible();
  await page.getByRole("link", { name: otherKey, exact: true }).click();
  await expect(page.locator("#object-drawer .object-key")).toHaveText(otherKey);
  // Independently exercise the stale-identity guard, not only XHR abortion.
  expect(
    await page.evaluate(() => {
      const stale = new window.CustomEvent("htmx:beforeSwap", {
        bubbles: true,
        cancelable: true,
        detail: {
          target: document.getElementById("object-drawer-content"),
          xhr: { status: 200, getResponseHeader: () => "object-details" },
        },
      });
      document.dispatchEvent(stale);
      return stale.defaultPrevented;
    }),
  ).toBe(true);
  release();
  await expect(page.locator("#object-drawer .object-key")).toHaveText(otherKey);
  await page.keyboard.press("Escape");
});

test("network failure and invalid fragment require explicit retry", async ({ page }) => {
  await page.route("**/s3/object-panel?**", (route) => route.abort("failed"));
  await page.goto(`${bucketPath}&lang=en`);
  await page.getByRole("link", { name: key, exact: true }).click();
  await expect(page.locator("#drawer-status")).toContainText("Could not load");
  await expect(page.locator("#object-drawer-content")).toBeEmpty();
  await page.unroute("**/s3/object-panel?**");
  await page.route("**/s3/object-panel?**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "text/html",
      body: "<p>unmarked response must not swap</p>",
    }),
  );
  await page.getByRole("button", { name: "Retry details", exact: true }).click();
  await expect(page.locator("#drawer-status")).toContainText("Could not load");
  await expect(page.locator("#object-drawer-content")).toBeEmpty();
  await page.unroute("**/s3/object-panel?**");
  await page.getByRole("button", { name: "Retry details", exact: true }).click();
  await expect(page.locator("#object-drawer .object-key")).toHaveText(key);
});

test("native fallback and normal Back Forward preserve canonical detail navigation", async ({
  page,
  browser,
  baseURL,
}) => {
  await page.goto("/connection?lang=en");
  await page.goto(`${bucketPath}&lang=en`);
  await page.getByRole("link", { name: key, exact: true }).click();
  await expect(page.locator("#object-drawer .object-key")).toHaveText(key);
  const detailHref = await page.locator("[data-drawer-page]").getAttribute("href");
  await page.goBack();
  await expect(page).toHaveURL(/\/connection\?/);
  await page.goForward();
  await expect(page.locator("#object-drawer")).not.toBeVisible();
  const context = await browser.newContext({ javaScriptEnabled: false, baseURL });
  try {
    const nativePage = await context.newPage();
    await nativePage.goto(`${bucketPath}&lang=en`);
    await nativePage.getByRole("link", { name: key, exact: true }).click();
    await expect(nativePage).toHaveURL(/\/s3\/object\?/);
    await expect(nativePage.locator(".object-key")).toHaveText(key);
    await nativePage.goto(detailHref);
    await expect(nativePage.locator(".object-key")).toHaveText(key);
  } finally {
    await context.close();
  }
});
