import { chromium, request } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { readFile, writeFile } from "node:fs/promises";
import assert from "node:assert/strict";
import console from "node:console";

assert.equal(process.platform, "linux", "Use the documented Playwright Linux container");

const baseURL = process.env.STACKEMU_BROWSER_URL || "http://127.0.0.1:8080";
const endpoint = new URL(baseURL);
assert.ok(["localhost", "127.0.0.1", "host.docker.internal", "gui"].includes(endpoint.hostname));
assert.equal(endpoint.protocol, "http:");
assert.equal(endpoint.port, "8080");
const directory = new URL("../../work/ux0/", import.meta.url);
const api = await request.newContext({ baseURL });
const response = await api.get("/components");
assert.equal(response.status(), 200);
const policy = response.headers()["content-security-policy"];
assert.ok(policy && !policy.includes("unsafe-inline") && !policy.includes("unsafe-eval"));
const browser = await chromium.launch();
const results = [];
try {
  for (const candidate of ["current", "tailwind", "pico"]) {
    for (const language of ["en", "pt-BR"]) {
      for (const mode of ["dark", "light", "auto-dark", "auto-light"]) {
        const theme = mode.startsWith("auto") ? "auto" : mode;
        const scheme = mode.endsWith("dark") ? "dark" : "light";
        const context = await browser.newContext({
          locale: "en-US",
          colorScheme: scheme,
          reducedMotion: "reduce",
        });
        const page = await context.newPage();
        const errors = [];
        page.on("pageerror", (error) => errors.push(error.message));
        page.on("console", (message) => {
          if (message.type() === "error") errors.push(message.text());
        });
        const html = await readFile(
          new URL(`${candidate}-${language}-${theme}.html`, directory),
          "utf8",
        );
        await page.route("**/*", async (route) => {
          const url = new URL(route.request().url());
          assert.equal(url.origin, endpoint.origin);
          if (route.request().isNavigationRequest()) {
            await route.fulfill({
              contentType: "text/html; charset=utf-8",
              headers: { "Content-Security-Policy": policy },
              body: html,
            });
          } else if (url.pathname === `/ux0/${candidate}.css`) {
            await route.fulfill({
              contentType: "text/css",
              body: await readFile(new URL(`${candidate}.css`, directory)),
            });
          } else {
            await route.fulfill({ status: 404, body: "Unexpected spike resource" });
          }
        });
        await page.goto(`${baseURL}/ux0/prototype`);
        assert.equal(await page.locator("[style], script").count(), 0);
        const measurements = [];
        for (const width of [360, 390, 1440, 1920]) {
          await page.setViewportSize({ width, height: 1000 });
          const values = await page.evaluate(() => {
            const doc = globalThis.document;
            const css = globalThis.getComputedStyle(doc.documentElement);
            const probe = globalThis.getComputedStyle(doc.querySelector(".utility-probe"));
            return {
              overflow: doc.documentElement.scrollWidth > globalThis.innerWidth,
              scheme: css.colorScheme,
              display: probe.display,
              gap: probe.gap,
            };
          });
          measurements.push({ width, ...values });
          if (candidate !== "pico") {
            assert.equal(values.overflow, false);
            assert.equal(values.scheme, scheme);
          }
          if (candidate === "tailwind") {
            assert.equal(values.display, "grid");
            assert.equal(values.gap, "16px");
            const decoration = await page
              .locator(".literal-probe")
              .evaluate((element) => globalThis.getComputedStyle(element).textDecorationLine);
            assert.equal(decoration.includes("underline"), scheme === "dark");
          }
          if (width === 390 || width === 1440)
            await page.screenshot({
              path: new URL(`${candidate}-${language}-${mode}-${width}.png`, directory).pathname,
              fullPage: true,
            });
        }
        await page.locator("#primary").focus();
        await page.keyboard.press("Tab");
        assert.equal(
          await page
            .locator(".table-scroll")
            .evaluate((element) => element === globalThis.document.activeElement),
          true,
        );
        // The labelled table scroll region is between actions and form in tab order.
        await page.locator("#example-check").focus();
        await page.keyboard.press("Space");
        assert.equal(await page.locator("#example-check").isChecked(), true);
        await page.locator("summary").focus();
        await page.keyboard.press("Enter");
        assert.equal(await page.locator("details").getAttribute("open"), "");
        const audit = await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze();
        if (candidate !== "pico") {
          assert.equal(audit.violations.length, 0, JSON.stringify(audit.violations));
          assert.deepEqual(errors, []);
        }
        const timing = await page.evaluate(
          () => globalThis.performance.getEntriesByType("navigation")[0].domContentLoadedEventEnd,
        );
        results.push({
          candidate,
          language,
          mode,
          measurements,
          violations: audit.violations.map(({ id, impact }) => ({ id, impact })),
          errors,
          interceptedDOMContentLoadedMs: timing,
        });
        await context.close();
        console.log(`PASS: ${candidate} ${language} ${mode}`);
      }
    }
  }
} finally {
  await browser.close();
  await api.dispose();
  await writeFile(
    new URL("browser-report.json", directory),
    `${JSON.stringify(results, null, 2)}\n`,
  );
}
assert.equal(results.length, 24);
console.log("PASS: 24 prototype cases; Pico findings are diagnostics, not acceptance");
