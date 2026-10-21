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
  throw new Error("Motion fixtures may only target the local emulator");
}
const bucket = `stackemu-motion-${randomUUID().replaceAll("-", "")}`;
const key = "samples/ação +% original.txt";
let created = false;
test.beforeAll(async ({ request }) => {
  expect((await request.put(`${emulatorURL}/${bucket}`)).status()).toBe(200);
  created = true;
  expect(
    (
      await request.put(`${emulatorURL}/${bucket}/${encodeURIComponent(key)}`, {
        data: "original motion fixture",
        headers: { "Content-Type": "text/plain" },
      })
    ).status(),
  ).toBe(200);
});
test.afterAll(async ({ request }) => {
  if (!created) return;
  // Remove only this suite's exact UUID-owned fixture.
  expect(
    (await request.delete(`${emulatorURL}/${bucket}/${encodeURIComponent(key)}`)).status(),
  ).toBe(204);
  expect((await request.delete(`${emulatorURL}/${bucket}`)).status()).toBe(204);
});

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    test(`HIG entry, native navigation and settled clipboard: ${language}, ${theme}`, async ({
      page,
    }, testInfo) => {
      const errors = [];
      page.on("pageerror", (error) => errors.push(error.message));
      page.on("console", (message) => {
        if (message.type() === "error" && /Content Security Policy|Refused to/.test(message.text()))
          errors.push(message.text());
      });
      await page.emulateMedia({ reducedMotion: "no-preference" });
      await page.addInitScript(() => {
        // Test-only instrumentation, not a production View Transition dependency.
        window.addEventListener("pagereveal", async (event) => {
          window.motionReveal = {
            active: Boolean(event.viewTransition),
            finished: false,
            durations: [],
          };
          if (event.viewTransition) {
            try {
              await event.viewTransition.ready;
              window.motionReveal.durations = document
                .getAnimations()
                .map((animation) => animation.effect.getTiming().duration);
              await event.viewTransition.finished;
            } catch {
              window.motionReveal.skipped = true;
            }
          }
          window.motionReveal.finished = true;
        });
        Object.defineProperty(window.navigator, "clipboard", {
          value: {
            writeText: (value) => {
              window.motionCopiedValue = value;
              return new Promise((resolve, reject) => {
                window.motionCopyResolve = resolve;
                window.motionCopyReject = reject;
              });
            },
          },
        });
      });
      await page.goto(`/?lang=${language}&theme=${theme}&density=compact`);
      await expect(page.locator(".summary-card").first()).toHaveCSS("transition-duration", "0.2s");
      const entry = await page.evaluate(async () => {
        const card = document.createElement("section");
        card.className = "summary-card";
        card.textContent = "Test-only entry fixture";
        document.querySelector("main").append(card);
        await new Promise((resolve) =>
          window.requestAnimationFrame(() => window.requestAnimationFrame(resolve)),
        );
        const animations = card.getAnimations();
        const durations = animations.map((animation) => animation.effect.getTiming().duration);
        const opacity = window.getComputedStyle(card).opacity;
        await Promise.all(animations.map((animation) => animation.finished));
        const finalTranslate = window.getComputedStyle(card).translate;
        card.remove();
        return { durations, opacity, finalTranslate };
      });
      expect(entry.durations).toEqual([200]);
      expect(entry.opacity).toBe("1");
      expect(entry.finalTranslate).toBe("none");
      await page.locator(".service-card a").click();
      await expect(page).toHaveURL(/\/s3\?/);
      await expect.poll(() => page.evaluate(() => window.motionReveal?.finished)).toBe(true);
      const navigation = await page.evaluate(() => window.motionReveal);
      expect(navigation.active).toBe(true);
      expect(navigation.skipped).not.toBe(true);
      expect(navigation.durations.length).toBeGreaterThan(0);
      expect(navigation.durations.every((duration) => duration > 0 && duration < 300)).toBe(true);
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      await page.locator(".table-scroll").focus();
      await expect(page.locator(".table-scroll")).toBeFocused();

      await page.goto(
        `/s3/bucket?bucket=${bucket}&prefix=samples%2F&lang=${language}&theme=${theme}&density=compact`,
      );
      const link = page.getByRole("link", { name: key, exact: true });
      await link.click();
      const panel = page.locator(".clipboard-panel");
      const status = panel.locator(".copy-status");
      const copy = page.getByRole("button", {
        name: language === "en" ? "Copy object key" : "Copiar chave do objeto",
        exact: true,
      });
      await copy.click();
      await expect(panel).toHaveAttribute("data-copy-state", "pending");
      await expect(status).toBeEmpty();
      await expect(copy).toBeDisabled();
      expect(await page.evaluate(() => window.motionCopiedValue)).toBe(key);
      await page.evaluate(() => window.motionCopyResolve());
      await expect(panel).toHaveAttribute("data-copy-state", "success");
      await expect(status).toHaveText(
        language === "en" ? "Copied to clipboard." : "Copiado para o clipboard.",
      );
      await expect(copy).toBeEnabled();
      await expect(status).toHaveCSS("transition-duration", "0.1s");
      await expect(status).toHaveCSS("border-inline-start-style", "solid");
      await expect(status).toHaveCSS("border-inline-start-width", "3px");
      await expect
        .poll(() =>
          status.evaluate((element) => window.getComputedStyle(element).borderInlineStartColor),
        )
        .not.toBe("rgba(0, 0, 0, 0)");
      await copy.click();
      await expect(panel).toHaveAttribute("data-copy-state", "pending");
      await expect(status).toBeEmpty();
      await page.evaluate(() => window.motionCopyReject(new Error("test-only denial")));
      await expect(panel).toHaveAttribute("data-copy-state", "failure");
      await expect(status).not.toBeEmpty();
      const field = page.locator("#drawer-copy-key");
      await expect(field).toBeFocused();
      expect(
        await field.evaluate((element) =>
          element.value.slice(element.selectionStart, element.selectionEnd),
        ),
      ).toBe(key);
      await page.emulateMedia({ reducedMotion: "reduce" });
      await expect(status).toHaveCSS("transition-duration", "0s");
      await page.emulateMedia({ reducedMotion: "no-preference" });
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
          path: testInfo.outputPath(`clipboard-${width}.png`),
          fullPage: true,
        });
      }
      await page.keyboard.press("Escape");
      await expect(link).toBeFocused();
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.goto(`/?lang=${language}&theme=${theme}`);
      await expect(page.locator(".summary-card").first()).toHaveCSS("transition-duration", "0s");
      await page.locator(".service-card a").click();
      await expect(page).toHaveURL(/\/s3\?/);
      expect(await page.evaluate(() => window.motionReveal?.active)).toBe(false);
      expect(await page.evaluate(() => document.getAnimations().length)).toBe(0);
      expect(errors).toEqual([]);
    });
  }
}

for (const theme of ["dark", "light"]) {
  test(`HIG native no-JS navigation and motion preferences: ${theme}`, async ({ browser }) => {
    const reducedMotion = theme === "dark" ? "reduce" : "no-preference";
    const context = await browser.newContext({ javaScriptEnabled: false, reducedMotion });
    const page = await context.newPage();
    try {
      await page.goto(`http://127.0.0.1:8080/?lang=pt-BR&theme=${theme}&density=compact`);
      await expect(page.locator(".summary-card").first()).toBeVisible();
      await expect(page.locator(".summary-card").first()).toHaveCSS(
        "transition-duration",
        reducedMotion === "reduce" ? "0s" : "0.2s",
      );
      await page.locator(".service-card a").click();
      await expect(page).toHaveURL(/\/s3\?/);
      await expect(page.locator("html")).toHaveAttribute("data-density", "compact");
      await page.locator(".table-scroll").focus();
      await expect(page.locator(".table-scroll")).toBeFocused();
      await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    } finally {
      await context.close();
    }
  });
}
