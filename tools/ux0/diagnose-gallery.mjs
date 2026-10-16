import { chromium, request } from "@playwright/test";
import { readFile, writeFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import console from "node:console";

const baseURL = process.env.STACKEMU_BROWSER_URL || "http://127.0.0.1:8080";
const endpoint = new URL(baseURL);
if (
  !["localhost", "127.0.0.1", "host.docker.internal"].includes(endpoint.hostname) ||
  endpoint.protocol !== "http:" ||
  endpoint.port !== "8080"
)
  throw new Error("Local GUI only");
const api = await request.newContext({ baseURL });
const policy = (await api.get("/components")).headers()["content-security-policy"];
const browser = await chromium.launch();
try {
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1000 },
    locale: "en-US",
    timezoneId: "UTC",
    deviceScaleFactor: 1,
    colorScheme: "dark",
    reducedMotion: "reduce",
  });
  const html = await readFile(
    new URL("../../work/browser-fixtures/gallery-pt-BR-dark.html", import.meta.url),
    "utf8",
  );
  await page.route("**/*", async (route) =>
    route.request().isNavigationRequest()
      ? route.fulfill({
          contentType: "text/html; charset=utf-8",
          headers: { "Content-Security-Policy": policy },
          body: html,
        })
      : route.fulfill({ response: await route.fetch() }),
  );
  await page.goto(`${baseURL}/components?lang=pt-BR&theme=dark`);
  for (const mode of ["page", "shell"]) {
    for (let index = 0; index < 5; index++) {
      const before = await page.locator(".app-shell").boundingBox();
      const bytes =
        mode === "page"
          ? await page.screenshot({ fullPage: true, animations: "disabled", caret: "hide" })
          : await page.locator(".app-shell").screenshot({ animations: "disabled", caret: "hide" });
      const after = await page.locator(".app-shell").boundingBox();
      const result = {
        mode,
        index,
        before,
        after,
        width: bytes.readUInt32BE(16),
        height: bytes.readUInt32BE(20),
        sha256: createHash("sha256").update(bytes).digest("hex"),
      };
      console.log(JSON.stringify(result));
      await writeFile(
        new URL(`../../work/ux0/diagnostic-${mode}-${index}.png`, import.meta.url),
        bytes,
      );
    }
  }
} finally {
  await browser.close();
  await api.dispose();
}
