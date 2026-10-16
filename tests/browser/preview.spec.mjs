import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID, createHash } from "node:crypto";

const emulator = process.env.STACKEMU_BROWSER_EMULATOR || "http://127.0.0.1:4566";
const endpoint = new URL(emulator);
if (
  endpoint.protocol !== "http:" ||
  endpoint.port !== "4566" ||
  !["localhost", "127.0.0.1", "ministack", "host.docker.internal"].includes(endpoint.hostname)
)
  throw new Error("Preview fixtures target local MiniStack only");
const key = "folder/ação +%<img src=x>.TXT";
const body =
  '</code></pre><img src=x onerror="window.previewAttack=1">\n<svg onload="window.previewAttack=2"></svg>\nhttps://example.invalid/\n' +
  "long-".repeat(400);
const hash = (bytes) => createHash("sha256").update(bytes).digest("hex");
async function fixture(request, run) {
  const bucket = `stackemu-preview-${randomUUID().replaceAll("-", "")}`;
  expect((await request.put(`${emulator}/${bucket}`)).status()).toBe(200);
  const keys = new Set();
  const put = async (name, bytes, type) => {
    keys.add(name);
    expect(
      (
        await request.put(`${emulator}/${bucket}/${encodeURIComponent(name)}`, {
          data: bytes,
          headers: { "Content-Type": type },
        })
      ).status(),
    ).toBe(200);
  };
  try {
    await run(bucket, put);
  } finally {
    for (const name of keys)
      expect(
        (await request.delete(`${emulator}/${bucket}/${encodeURIComponent(name)}`)).status(),
      ).toBe(204);
    expect((await request.delete(`${emulator}/${bucket}`)).status()).toBe(204);
  }
}

for (const language of ["en", "pt-BR"]) {
  for (const theme of ["dark", "light"]) {
    test(`native escaped text preview and original attachment: ${language}, ${theme}`, async ({
      page,
      request,
    }, testInfo) => {
      await fixture(request, async (bucket, put) => {
        await put(key, Buffer.from(body), "text/plain; charset=utf-8");
        const errors = [];
        page.on("pageerror", (error) => errors.push(error.message));
        page.on("console", (message) => {
          if (
            message.type() === "error" &&
            /Content Security Policy|Refused to/.test(message.text())
          )
            errors.push(message.text());
        });
        await page.goto(
          `/s3/object?bucket=${bucket}&key=${encodeURIComponent(key)}&lang=${language}&theme=${theme}`,
        );
        const link = page.getByRole("link", {
          name: language === "en" ? "Text preview" : "Prévia de texto",
          exact: true,
        });
        for (const width of [390, 1440]) {
          await page.setViewportSize({ width, height: 900 });
          await page.screenshot({
            path: testInfo.outputPath(`object-link-${language}-${theme}-${width}.png`),
            fullPage: true,
          });
        }
        await link.focus();
        await page.keyboard.press("Enter");
        const region = page.locator(".text-preview");
        await expect(region).toHaveText(body);
        expect(await region.locator("img,svg,script,iframe,object,embed,a").count()).toBe(0);
        expect(await page.evaluate(() => window.previewAttack)).toBeUndefined();
        await expect(page.locator("h2.object-key")).toHaveText(key);
        expect(new URL(page.url()).searchParams.get("lang")).toBe(language);
        expect(new URL(page.url()).searchParams.get("theme")).toBe(theme);
        await region.focus();
        await expect(region).toBeFocused();
        for (const width of [390, 1440]) {
          await page.setViewportSize({ width, height: 900 });
          expect(
            await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
          ).toBe(true);
          const result = await new AxeBuilder({ page })
            .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
            .analyze();
          expect(result.violations).toEqual([]);
          await page.screenshot({
            path: testInfo.outputPath(`preview-${language}-${theme}-${width}.png`),
            fullPage: true,
          });
        }
        const download = await request.get(
          `/s3/download?bucket=${bucket}&key=${encodeURIComponent(key)}`,
        );
        expect(download.status()).toBe(200);
        expect(download.headers()["content-disposition"]).toContain("attachment");
        expect(download.headers()["content-type"]).toContain("text/plain");
        expect(hash(await download.body())).toBe(hash(Buffer.from(body)));
        await page
          .getByRole("link", {
            name: language === "en" ? "Back to object details" : "Voltar aos detalhes do objeto",
            exact: true,
          })
          .click();
        await expect(page).toHaveURL(/\/s3\/object\?/);
        expect(errors).toEqual([]);
      });
    });
  }
}

test("refused formats, oversized, malformed and missing objects never render a partial body", async ({
  page,
  request,
}) => {
  await fixture(request, async (bucket, put) => {
    for (const [name, bytes, type, reason] of [
      [
        "active.html",
        Buffer.from("SECRET <script>window.previewAttack=1</script>"),
        "text/html",
        "format",
      ],
      ["active.svg", Buffer.from("SECRET <svg/>"), "image/svg+xml", "format"],
      ["large.txt", Buffer.alloc(65537, "x"), "text/plain", "exceeds 64 KiB"],
      ["bad.txt", Buffer.from([0xc3, 0x28]), "text/plain", "invalid UTF-8"],
    ]) {
      await put(name, bytes, type);
      const response = await page.goto(`/s3/preview?bucket=${bucket}&key=${name}&lang=en`);
      expect(response.status()).toBe(200);
      expect(response.headers()["cache-control"]).toBe("no-store");
      await expect(page.locator(".text-preview")).toHaveCount(0);
      await expect(page.locator("#content")).toContainText(reason);
      await expect(page.locator("#content")).not.toContainText("SECRET");
      expect(await page.evaluate(() => window.previewAttack)).toBeUndefined();
      await expect(
        page.getByRole("link", { name: "Download as attachment", exact: true }),
      ).toBeVisible();
    }
    const response = await page.goto(`/s3/preview?bucket=${bucket}&key=missing.txt&lang=en`);
    expect(response.status()).toBe(404);
    expect(response.headers()["cache-control"]).toBe("no-store");
    await expect(page.locator(".text-preview")).toHaveCount(0);
  });
});

test("JSON verbatim preview remains available without JavaScript", async ({
  browser,
  baseURL,
  request,
}) => {
  await fixture(request, async (bucket, put) => {
    const json = '{"name":"ação","markup":"<script>not executed</script>"}';
    await put("data.json", Buffer.from(json), "application/json");
    const context = await browser.newContext({ javaScriptEnabled: false, baseURL });
    try {
      const page = await context.newPage();
      await page.goto(`/s3/object?bucket=${bucket}&key=data.json&lang=pt-BR`);
      await page.getByRole("link", { name: "Prévia de texto", exact: true }).click();
      await expect(page.locator(".text-preview")).toHaveText(json);
      await expect(
        page.getByRole("link", { name: "Baixar como anexo", exact: true }),
      ).toBeVisible();
    } finally {
      await context.close();
    }
  });
});
