import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID, createHash } from "node:crypto";

const emulatorURL = process.env.STACKEMU_BROWSER_EMULATOR || "http://127.0.0.1:4566";
const endpoint = new URL(emulatorURL);
if (
  endpoint.protocol !== "http:" ||
  endpoint.port !== "4566" ||
  !["localhost", "127.0.0.1", "ministack", "host.docker.internal"].includes(endpoint.hostname)
) {
  throw new Error("Upload fixtures target local MiniStack only");
}
const filename = `ação +% ${"long-name-".repeat(12)}.TXT`;
const payload = "StackEmu original bytes — upload\n";
const hash = (value) => createHash("sha256").update(value).digest("hex");

const browserErrors = new WeakMap();
test.beforeEach(async ({ page }) => {
  const errors = [];
  browserErrors.set(page, errors);
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error" && /Content Security Policy|Refused to/.test(message.text()))
      errors.push(message.text());
  });
});
test.afterEach(async ({ page }) => {
  expect(browserErrors.get(page)).toEqual([]);
});

async function fixture(request, run) {
  const bucket = `stackemu-upload-${randomUUID().replaceAll("-", "")}`;
  expect((await request.put(`${emulatorURL}/${bucket}`)).status()).toBe(200);
  const keys = new Set();
  try {
    await run(bucket, keys);
  } finally {
    for (const key of keys)
      expect(
        (await request.delete(`${emulatorURL}/${bucket}/${encodeURIComponent(key)}`)).status(),
      ).toBe(204);
    expect((await request.delete(`${emulatorURL}/${bucket}`)).status()).toBe(204);
  }
}

async function transfer(page, names, text = false) {
  return page.evaluateHandle(
    ({ names, text, payload }) => {
      const transfer = new window.DataTransfer();
      if (text) transfer.items.add("not a file", "text/plain");
      else
        for (const name of names)
          transfer.items.add(new window.File([payload], name, { type: "text/plain" }));
      return transfer;
    },
    { names, text, payload },
  );
}

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    test(`single-file drop, processing, duplicate guard and original download: ${language}, ${theme}`, async ({
      page,
      request,
    }, testInfo) => {
      await fixture(request, async (bucket, keys) => {
        const key = `ux3/${filename}`;
        keys.add(key);
        await page.goto(
          `/s3/write?action=upload&bucket=${bucket}&prefix=ux3%2F&lang=${language}&theme=${theme}`,
        );
        const zone = page.locator("[data-upload-drop]");
        const input = page.locator("#mutation-file");
        await input.setInputFiles({
          name: "previous.txt",
          mimeType: "text/plain",
          buffer: Buffer.from("previous"),
        });
        const pickerPromise = page.waitForEvent("filechooser");
        await input.focus();
        await page.keyboard.press("Enter");
        const chooser = await pickerPromise;
        await chooser.setFiles({
          name: "previous.txt",
          mimeType: "text/plain",
          buffer: Buffer.from("previous"),
        });
        for (const kind of ["multiple", "directory", "text"]) {
          if (kind === "directory") {
            await zone.evaluate((element) => {
              // Simulate the browser directory-entry API, not a real OS folder drag.
              const prototype = window.DataTransferItem.prototype;
              const original = prototype.webkitGetAsEntry;
              prototype.webkitGetAsEntry = () => ({ isDirectory: true });
              try {
                const data = new window.DataTransfer();
                data.items.add(new window.File(["fixture"], "folder"));
                element.dispatchEvent(
                  new window.DragEvent("drop", {
                    bubbles: true,
                    cancelable: true,
                    dataTransfer: data,
                  }),
                );
              } finally {
                prototype.webkitGetAsEntry = original;
              }
            });
          } else {
            const data = await transfer(
              page,
              kind === "multiple" ? ["one.txt", "two.txt"] : [],
              kind === "text",
            );
            await zone.dispatchEvent("drop", { dataTransfer: data });
          }
          expect(await input.evaluate((element) => element.files[0].name)).toBe("previous.txt");
          await expect(page.locator("[data-drop-status]")).toContainText(
            language === "en" ? "not supported" : "não são suportados",
          );
        }
        let requests = 0;
        const holdToken = randomUUID().replaceAll("-", "");
        const countUpload = (request) => {
          if (request.method() === "POST" && request.url().endsWith("/s3/write/upload")) requests++;
        };
        page.on("request", countUpload);
        await page.evaluate((token) => {
          document.addEventListener(
            "htmx:configRequest",
            (event) => {
              event.detail.headers["X-StackEmu-Test-Hold"] = token;
            },
            { once: true },
          );
        }, holdToken);
        const release = () => request.post(`/__stackemu_test/upload-release?token=${holdToken}`);
        try {
          const selected = await transfer(page, [filename]);
          await zone.dispatchEvent("dragover", { dataTransfer: selected });
          await expect(zone).toHaveAttribute("data-dragging", "true");
          await zone.dispatchEvent("drop", { dataTransfer: selected });
          expect(await input.evaluate((element) => element.files[0].name)).toBe(filename);
          expect(requests).toBe(0);
          await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
          await page.locator('button[type="submit"]').click();
          await expect
            .poll(async () => {
              const status = await request.get(`/__stackemu_test/upload-status?token=${holdToken}`);
              return (await status.json()).held;
            })
            .toBe(true);
          await expect(page.locator("[data-upload-status]")).toContainText(
            language === "en" ? "not yet confirmed" : "ainda não foi confirmado",
            { timeout: 5000 },
          );
          await expect(page.locator("progress")).toHaveJSProperty("value", 100);
          await expect(page.locator("[data-operation-notice]")).toBeHidden();
          await expect(input).toBeDisabled();
          await expect(page.locator('button[type="submit"]')).toBeDisabled();
          await expect(page.locator('button[type="submit"]')).toBeDisabled();
          await page.locator("form").dispatchEvent("submit");
          await zone.dispatchEvent("drop", {
            dataTransfer: await transfer(page, ["different.txt"]),
          });
          expect(await input.evaluate((element) => element.files[0].name)).toBe(filename);
          await page.evaluate(() =>
            document.querySelector("form").dispatchEvent(
              new window.CustomEvent("htmx:xhr:progress", {
                bubbles: true,
                detail: { loaded: 1, total: 100, lengthComputable: true },
              }),
            ),
          );
          await expect(page.locator("progress")).toHaveJSProperty("value", 100);
          for (const width of [390, 1440]) {
            await page.setViewportSize({ width, height: 900 });
            expect(
              await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
            ).toBe(true);
            const results = await new AxeBuilder({ page })
              .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
              .analyze();
            expect(results.violations).toEqual([]);
            await page.screenshot({
              path: testInfo.outputPath(`upload-${language}-${theme}-${width}.png`),
              fullPage: true,
            });
          }
          expect(requests).toBe(1);
          await release();
          await expect(
            page.getByRole("heading", {
              name: language === "en" ? "Operation completed" : "Operação concluída",
            }),
          ).toBeFocused();
          const download = await request.get(
            `/s3/download?bucket=${bucket}&key=${encodeURIComponent(key)}`,
          );
          expect(download.status()).toBe(200);
          expect(hash(await download.body())).toBe(hash(Buffer.from(payload)));
          expect(download.headers()["content-type"]).toContain("text/plain");
          expect(download.headers()["content-disposition"]).toContain(".TXT");
        } finally {
          await release();
          page.off("request", countUpload);
        }
      });
    });
  }
}

test("server validation reselects the file and explicit retry succeeds without overwrite", async ({
  page,
  request,
}) => {
  await fixture(request, async (bucket, keys) => {
    const key = "ux3/size.txt";
    keys.add(key);
    await page.goto(`/s3/write?action=upload&bucket=${bucket}&prefix=ux3%2F&lang=en`);
    await page.locator("#mutation-file").setInputFiles({
      name: "size.txt",
      mimeType: "text/plain",
      buffer: Buffer.alloc(10485761, "x"),
    });
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    const failed = page.waitForResponse((response) => response.url().endsWith("/s3/write/upload"));
    await page.locator('button[type="submit"]').click();
    expect((await failed).status()).toBe(422);
    await expect(page.locator("#file-error")).toBeVisible();
    await expect(page.locator("[data-operation-notice]")).toBeHidden();
    await expect(page.locator("#mutation-file")).toBeFocused();
    expect(await page.locator("#mutation-file").evaluate((element) => element.files.length)).toBe(
      0,
    );
    await expect(page.locator("[data-upload-drop]")).toBeVisible();
    await page
      .locator("#mutation-file")
      .setInputFiles({ name: "size.txt", mimeType: "text/plain", buffer: Buffer.from(payload) });
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    await page.locator('button[type="submit"]').click();
    await expect(page.getByRole("heading", { name: "Operation completed" })).toBeVisible();
    await page.reload();
    await page.locator("#mutation-file").setInputFiles({
      name: "size.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("must not overwrite"),
    });
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    const conflict = page.waitForResponse((response) =>
      response.url().endsWith("/s3/write/upload"),
    );
    await page.locator('button[type="submit"]').click();
    expect((await conflict).status()).toBe(409);
    await expect(page.locator("[data-operation-notice]")).toBeHidden();
    await expect(page.getByRole("alert").first()).toContainText("already exists");
    expect(
      await (
        await request.get(`/s3/download?bucket=${bucket}&key=${encodeURIComponent(key)}`)
      ).text(),
    ).toBe(payload);
  });
});

test("unmarked, network, size and shortened-timeout failures never retry automatically", async ({
  page,
  request,
}) => {
  await fixture(request, async (bucket, keys) => {
    keys.add("ux3/failure.txt");
    for (const failure of ["unmarked", "network", "size", "timeout"]) {
      await page.goto(`/s3/write?action=upload&bucket=${bucket}&prefix=ux3%2F&lang=en`);
      let requests = 0;
      let release;
      const gate = new Promise((resolve) => {
        release = resolve;
      });
      if (failure === "timeout")
        await page.evaluate(() => {
          document.addEventListener(
            "htmx:configRequest",
            (event) => {
              window.observedUploadTimeout = event.detail.timeout;
              event.detail.timeout = 40; // Shortened transport fault fixture, not a real two-minute wait.
            },
            { once: true },
          );
        });
      await page.route("**/s3/write/upload", async (route) => {
        requests++;
        if (failure === "timeout") {
          await gate;
          try {
            await route.abort();
          } catch {
            /* Timed out already. */
          }
        } else if (failure === "network") await route.abort("failed");
        else
          await route.fulfill({
            status: failure === "size" ? 413 : 200,
            contentType: "text/html",
            body: "<p>untrusted unmarked response</p>",
          });
      });
      try {
        await page.locator("#mutation-file").setInputFiles({
          name: "failure.txt",
          mimeType: "text/plain",
          buffer: Buffer.from(payload),
        });
        await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
        await page.locator('button[type="submit"]').click();
        await expect(page.locator("[data-upload-status]")).toContainText(
          failure === "size" ? "rejected" : "could not be confirmed",
        );
        await expect(page.locator("form")).toHaveAttribute("aria-busy", "false");
        await expect(page.locator("[data-operation-notice]")).toBeHidden();
        await expect(page.locator(".mutation-form")).toHaveAttribute("aria-busy", "false");
        await expect(page.locator('button[type="submit"]')).toBeEnabled();
        expect(requests).toBe(1);
        await expect(page.locator("#mutation-workspace")).not.toContainText(
          "untrusted unmarked response",
        );
        if (failure === "timeout")
          expect(await page.evaluate(() => window.observedUploadTimeout)).toBe(120000);
      } finally {
        release();
        await page.unroute("**/s3/write/upload");
      }
    }
  });
});

test("synthetic progress edge cases stay indeterminate or monotonic and ignore response events", async ({
  page,
  request,
}) => {
  await fixture(request, async (bucket, keys) => {
    keys.add("ux3/edges.txt");
    await page.goto(`/s3/write?action=upload&bucket=${bucket}&prefix=ux3%2F&lang=en`);
    await page.evaluate(() => {
      document.addEventListener(
        "htmx:beforeSend",
        (event) => {
          const progress = document.querySelector("progress");
          const samples = [];
          const record = () =>
            samples.push({ determinate: progress.hasAttribute("value"), value: progress.value });
          const send = (loaded, total, lengthComputable) => {
            event.detail.xhr.upload.dispatchEvent(
              new window.ProgressEvent("progress", { loaded, total, lengthComputable }),
            );
            record();
          };
          send(0, 0, false);
          send(50, 100, true);
          send(25, 100, true);
          send(1, 0, true);
          send(60, 100, true);
          document.querySelector("form").dispatchEvent(
            new window.CustomEvent("htmx:xhr:progress", {
              bubbles: true,
              detail: { loaded: 1, total: 100, lengthComputable: true },
            }),
          );
          record();
          window.uploadProgressEdges = samples;
        },
        { once: true },
      );
    });
    await page.route("**/s3/write/upload", (route) => route.abort("failed"));
    await page
      .locator("#mutation-file")
      .setInputFiles({ name: "edges.txt", mimeType: "text/plain", buffer: Buffer.from(payload) });
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    await page.locator('button[type="submit"]').click();
    await expect(page.locator("[data-upload-status]")).toContainText("could not be confirmed");
    expect(await page.evaluate(() => window.uploadProgressEdges)).toEqual([
      { determinate: false, value: 0 },
      { determinate: true, value: 50 },
      { determinate: true, value: 50 },
      { determinate: false, value: 0 },
      { determinate: true, value: 60 },
      { determinate: true, value: 60 },
    ]);
  });
});

test("real upload CSRF rejection never produces a success workspace", async ({ page, request }) => {
  await fixture(request, async (bucket, keys) => {
    keys.add("ux3/csrf.txt");
    await page.goto(`/s3/write?action=upload&bucket=${bucket}&prefix=ux3%2F&lang=en`);
    await page
      .locator("#mutation-file")
      .setInputFiles({ name: "csrf.txt", mimeType: "text/plain", buffer: Buffer.from(payload) });
    await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
    await page.locator('input[name="csrf-token"]').evaluate((element) => {
      element.value = "invalid-fixture";
    });
    const rejected = page.waitForResponse((response) =>
      response.url().endsWith("/s3/write/upload"),
    );
    await page.locator('button[type="submit"]').click();
    expect((await rejected).status()).toBe(400);
    await expect(page.locator("[data-operation-notice]")).toBeHidden();
    await expect(page.locator("[data-upload-status]")).toContainText("could not be confirmed");
    await expect(page.locator(".mutation-form")).toHaveAttribute("aria-busy", "false");
    expect(
      (
        await request.head(`${emulatorURL}/${bucket}/${encodeURIComponent("ux3/csrf.txt")}`)
      ).status(),
    ).toBe(404);
  });
});

test("native upload without JavaScript keeps original bytes and server confirmation", async ({
  browser,
  baseURL,
  request,
}) => {
  await fixture(request, async (bucket, keys) => {
    keys.add(`ux3/${filename}`);
    const context = await browser.newContext({ javaScriptEnabled: false, baseURL });
    try {
      const page = await context.newPage();
      await page.goto(`/s3/write?action=upload&bucket=${bucket}&prefix=ux3%2F&lang=en`);
      await expect(page.locator("[data-upload-drop]")).toBeVisible();
      await expect(page.locator("[data-upload-drop-hint]")).toBeHidden();
      await expect(page.locator("[data-upload-progress]")).toBeHidden();
      await page
        .locator("#mutation-file")
        .setInputFiles({ name: filename, mimeType: "text/plain", buffer: Buffer.from(payload) });
      await expect(page.locator('input[name="confirm"]')).toHaveValue("yes");
      await page.getByRole("button", { name: "Upload new object", exact: true }).click();
      await expect(page.getByRole("heading", { name: "Operation completed" })).toBeVisible();
      expect(
        hash(
          await (
            await request.get(
              `/s3/download?bucket=${bucket}&key=${encodeURIComponent(`ux3/${filename}`)}`,
            )
          ).body(),
        ),
      ).toBe(hash(Buffer.from(payload)));
    } finally {
      await context.close();
    }
  });
});
