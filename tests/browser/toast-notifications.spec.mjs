import { test, expect } from "@playwright/test";

async function setup(page, lang = "en", theme = "light") {
  await page.clock.install({ time: new Date("2026-10-27T12:00:00Z") });
  await page.goto(`/?lang=${lang}&theme=${theme}`);
  await page.clock.pauseAt(new Date("2026-10-27T12:01:00Z"));
  await page.mouse.move(0, 0);
}
async function publish(page, message = "Completed", pending = false) {
  await page.evaluate(
    ({ message, pending }) =>
      document.dispatchEvent(
        new window.CustomEvent("stackemu:notice", {
          detail: { message, pending, operation: "", time: Date.now() },
        }),
      ),
    { message, pending },
  );
  await page.clock.runFor(32);
}

for (const lang of ["en", "pt-BR"])
  for (const theme of ["light", "dark"]) {
    test(`compact success expires without moving focus: ${lang}, ${theme}`, async ({
      page,
    }, info) => {
      await setup(page, lang, theme);
      const source = page.locator(".service-card a");
      await source.focus();
      await publish(page, lang === "en" ? "Operation completed." : "Operação concluída.");
      const notice = page.locator("[data-operation-notice]");
      for (const width of [390, 1366, 1440]) {
        await page.setViewportSize({ width, height: 900 });
        const box = await notice.boundingBox();
        expect(box.width).toBeLessThanOrEqual(384);
        expect(box.height).toBeLessThanOrEqual(90);
        expect(box.x).toBeGreaterThanOrEqual(0);
        expect(box.x + box.width).toBeLessThanOrEqual(width);
        const close = await notice.locator("[data-notice-dismiss]").boundingBox();
        expect(close.width).toBeGreaterThanOrEqual(44);
        expect(close.height).toBeGreaterThanOrEqual(44);
        await page.screenshot({ path: info.outputPath(`toast-${width}.png`) });
      }
      await page.clock.runFor(4900);
      await expect(notice).toBeVisible();
      await page.clock.runFor(100);
      await expect(notice).not.toBeVisible();
      await expect(source).toBeFocused();
    });
  }

test("hover and keyboard focus pause the remaining visible lifetime", async ({ page }) => {
  await setup(page);
  await publish(page);
  const notice = page.locator("[data-operation-notice]");
  await notice.hover();
  await page.clock.runFor(10000);
  await expect(notice).toBeVisible();
  await notice.locator("[data-notice-dismiss]").focus();
  await page.mouse.move(0, 0);
  await page.clock.runFor(10000);
  await expect(notice).toBeVisible();
  await page.locator(".service-card a").focus();
  await page.clock.runFor(5100);
  await expect(notice).not.toBeVisible();
});

test("replacement cancels the old deadline and pending or warning notices persist", async ({
  page,
}) => {
  await setup(page);
  const notice = page.locator("[data-operation-notice]");
  await publish(page, "First result");
  await page.clock.runFor(4000);
  await publish(page, "Second result");
  await page.clock.runFor(2000);
  await expect(notice).toBeVisible();
  await expect(page.locator("[data-notice-message]")).toHaveText("Second result");
  await page.clock.runFor(3100);
  await expect(notice).not.toBeVisible();
  await publish(page, "Working", true);
  await page.clock.runFor(10000);
  await expect(notice).toBeVisible();
  await page.evaluate(() => {
    const notice = document.querySelector("[data-operation-notice]");
    notice.dataset.noticeTone = "warning";
    notice.querySelector("[data-notice-message]").textContent = "Partial result";
    notice.dispatchEvent(new window.Event("stackemu:notice-updated"));
  });
  await page.clock.runFor(10000);
  await expect(notice).toBeVisible();
  await notice.locator("[data-notice-dismiss]").click();
  await expect(notice).not.toBeVisible();
  await expect(page.locator("main h1")).toBeFocused();
});

test("hidden document pauses success expiry", async ({ page }) => {
  await setup(page);
  await publish(page);
  await page.clock.runFor(1000);
  await page.evaluate(() => {
    Object.defineProperty(document, "hidden", { configurable: true, value: true });
    document.dispatchEvent(new window.Event("visibilitychange"));
  });
  await page.clock.runFor(10000);
  await expect(page.locator("[data-operation-notice]")).toBeVisible();
  await page.evaluate(() => {
    delete document.hidden;
    document.dispatchEvent(new window.Event("visibilitychange"));
  });
  await page.clock.runFor(4100);
  await expect(page.locator("[data-operation-notice]")).not.toBeVisible();
});

test("confirmed operation completion starts the success lifetime", async ({ page }) => {
  await setup(page);
  await publish(page, "Working", true);
  await page.clock.runFor(10000);
  await expect(page.locator("[data-operation-notice]")).toBeVisible();
  await page.evaluate(() => {
    const notice = document.querySelector("[data-operation-notice]");
    notice.dataset.noticeTone = "success";
    notice.querySelector("[data-notice-message]").textContent = "Completed";
    notice.dispatchEvent(new window.Event("stackemu:notice-updated"));
  });
  await page.clock.runFor(4900);
  await expect(page.locator("[data-operation-notice]")).toBeVisible();
  await page.clock.runFor(200);
  await expect(page.locator("[data-operation-notice]")).not.toBeVisible();
});

test("a terminal update cannot be overwritten by an older deferred announcement", async ({
  page,
}) => {
  await setup(page);
  await page.evaluate(() => {
    document.dispatchEvent(
      new window.CustomEvent("stackemu:notice", {
        detail: {
          message: "Working",
          pending: true,
          operation: "",
          time: Date.now(),
        },
      }),
    );
    const notice = document.querySelector("[data-operation-notice]");
    notice.dataset.noticeTone = "warning";
    notice.querySelector("[data-notice-message]").textContent = "Partial result";
    notice.dispatchEvent(new window.Event("stackemu:notice-updated"));
  });
  await page.clock.runFor(10000);
  await expect(page.locator("[data-notice-message]")).toHaveText("Partial result");
  await expect(page.locator("[data-operation-notice]")).toBeVisible();
});
