import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID, createHash } from "node:crypto";
import { deflateSync } from "node:zlib";

const emulator = process.env.STACKEMU_BROWSER_EMULATOR || "http://127.0.0.1:4566";
const endpoint = new URL(emulator);
if (
  endpoint.protocol !== "http:" ||
  endpoint.port !== "4566" ||
  !["127.0.0.1", "localhost", "ministack", "host.docker.internal"].includes(endpoint.hostname)
)
  throw new Error("Image fixtures target local MiniStack only");
const key = "folder/ação +%<img src=x>.PNG";
const hash = (bytes) => createHash("sha256").update(bytes).digest("hex");

test.beforeAll(async ({ browser }) => {
  process.stdout.write(`Preview browser version: ${browser.version()}\n`);
});

function chunk(type, data) {
  const name = Buffer.from(type);
  const body = Buffer.concat([name, data]);
  let crc = 0xffffffff;
  for (const value of body) {
    crc ^= value;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0);
  }
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length);
  const checksum = Buffer.alloc(4);
  checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0);
  return Buffer.concat([length, body, checksum]);
}

function png() {
  const width = 960,
    height = 480;
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width);
  header.writeUInt32BE(height, 4);
  header[8] = 8;
  header[9] = 2;
  const pixels = Buffer.alloc((width * 3 + 1) * height);
  for (let y = 0; y < height; y++)
    for (let x = 0; x < width; x++) {
      const offset = y * (width * 3 + 1) + 1 + x * 3;
      pixels[offset] = Math.floor((x * 255) / width);
      pixels[offset + 1] = Math.floor((y * 255) / height);
      pixels[offset + 2] = 140;
    }
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk("IHDR", header),
    chunk("tEXt", Buffer.from("Comment\0source-private-marker")),
    chunk("IDAT", deflateSync(pixels)),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

async function fixture(request, run) {
  const bucket = `stackemu-image-${randomUUID().replaceAll("-", "")}`;
  const keys = new Set();
  expect((await request.put(`${emulator}/${bucket}`)).status()).toBe(200);
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

for (const language of ["en", "pt-BR"])
  for (const theme of ["dark", "light"]) {
    test(`safe image preview and original attachment: ${language}, ${theme}`, async ({
      page,
      request,
    }, testInfo) => {
      await fixture(request, async (bucket, put) => {
        const original = png();
        await put(key, original, "image/png");
        const errors = [];
        let contentRequests = 0;
        page.on("request", (request) => {
          if (new URL(request.url()).pathname === "/s3/preview/image/content") contentRequests++;
        });
        page.on("pageerror", (error) => errors.push(error.message));
        page.on("console", (message) => {
          if (/Content Security Policy|Refused to/.test(message.text()))
            errors.push(message.text());
        });
        await page.goto(
          `/s3/bucket?bucket=${bucket}&prefix=folder%2F&lang=${language}&theme=${theme}`,
        );
        await page.getByRole("link", { name: key, exact: true }).click();
        const drawerPreview = page.locator('.object-panel-actions a[href^="/s3/preview/image?"]');
        await expect(drawerPreview).toBeVisible();
        const drawerURL = new URL(await drawerPreview.getAttribute("href"), page.url());
        expect(drawerURL.searchParams.get("lang")).toBe(language);
        expect(drawerURL.searchParams.get("theme")).toBe(theme);
        expect(contentRequests).toBe(0);
        await page.locator("[data-drawer-close]").click();
        await page.goto(
          `/s3/object?bucket=${bucket}&key=${encodeURIComponent(key)}&lang=${language}&theme=${theme}`,
        );
        expect(contentRequests).toBe(0);
        const link = page.getByRole("link", {
          name: language === "en" ? "Image preview" : "Prévia de imagem",
          exact: true,
        });
        await link.focus();
        await page.keyboard.press("Enter");
        const image = page.locator("[data-image-preview]");
        await expect.poll(() => image.evaluate((node) => node.naturalWidth)).toBe(960);
        await expect(page.locator("[data-image-status]")).toContainText(
          language === "en" ? "Re-encoded image loaded" : "Imagem reencodada carregada",
        );
        await expect(page.locator("h2.object-key")).toHaveText(key);
        expect(contentRequests).toBe(1);
        const src = await image.getAttribute("src");
        const sanitized = await request.get(src);
        expect(sanitized.status()).toBe(200);
        expect(sanitized.headers()["content-type"]).toBe("image/png");
        expect(sanitized.headers()["cache-control"]).toBe("no-store");
        expect((await sanitized.body()).includes(Buffer.from("source-private-marker"))).toBe(false);
        expect(sanitized.headers()["content-security-policy"]).not.toContain("data:");
        for (const width of [390, 1440]) {
          await page.setViewportSize({ width, height: 900 });
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
            path: testInfo.outputPath(`image-${language}-${theme}-${width}.png`),
            fullPage: true,
          });
        }
        const download = page.getByRole("link", {
          name: language === "en" ? "Download as attachment" : "Baixar como anexo",
          exact: true,
        });
        await download.focus();
        await expect(download).toBeFocused();
        const response = await request.get(await download.getAttribute("href"));
        expect(response.headers()["content-disposition"]).toContain("attachment");
        expect(hash(await response.body())).toBe(hash(original));
        expect(new URL(page.url()).searchParams.get("lang")).toBe(language);
        expect(new URL(page.url()).searchParams.get("theme")).toBe(theme);
        expect(errors).toEqual([]);
      });
    });
  }

test("refused and missing images retain native fallback and explicit content status", async ({
  page,
  request,
}) => {
  await fixture(request, async (bucket, put) => {
    await put("bad.png", Buffer.from("<svg onload='alert(1)'>SECRET</svg>"), "image/png");
    for (const [name, status] of [
      ["bad.png", 422],
      ["missing.png", 404],
    ]) {
      await page.goto(`/s3/preview/image?bucket=${bucket}&key=${name}&lang=pt-BR`);
      await expect(page.locator("[data-image-status]")).toContainText("indisponível ou recusada");
      await expect(page.locator("[data-image-preview]")).toBeHidden();
      expect(await page.locator("#content").textContent()).not.toContain("SECRET");
      const content = await request.get(
        `/s3/preview/image/content?bucket=${bucket}&key=${name}&lang=pt-BR`,
      );
      expect(content.status()).toBe(status);
      expect(content.headers()["cache-control"]).toBe("no-store");
      await expect(
        page.getByRole("link", { name: "Baixar como anexo", exact: true }),
      ).toBeVisible();
    }
  });
});

test("native image preview works without JavaScript", async ({ browser, request, baseURL }) => {
  await fixture(request, async (bucket, put) => {
    await put(key, png(), "image/png");
    const context = await browser.newContext({ javaScriptEnabled: false, baseURL });
    try {
      const page = await context.newPage();
      await page.goto(
        `/s3/preview/image?bucket=${bucket}&key=${encodeURIComponent(key)}&lang=pt-BR&theme=light`,
      );
      await expect
        .poll(() => page.locator("[data-image-preview]").evaluate((node) => node.naturalWidth))
        .toBe(960);
      await expect(page.locator("[data-image-status]")).toContainText("requisição separada");
      await expect(
        page.getByRole("link", { name: "Baixar como anexo", exact: true }),
      ).toBeVisible();
      await page.goto(`/s3/preview/image?bucket=${bucket}&key=missing.png&lang=pt-BR`);
      await expect(page.locator("[data-image-status]")).toContainText("Se não puder ser exibida");
      await expect(
        page.getByRole("link", { name: "Baixar como anexo", exact: true }),
      ).toBeVisible();
    } finally {
      await context.close();
    }
  });
});

test("loading status is not success while the image request is held", async ({ page, request }) => {
  await fixture(request, async (bucket, put) => {
    await put("held.png", png(), "image/png");
    let release;
    const gate = new Promise((resolve) => {
      release = resolve;
    });
    // Network-gating fixture, not a real slow-decoder or performance measurement.
    await page.route("**/s3/preview/image/content?**", async (route) => {
      await gate;
      await route.continue();
    });
    try {
      await page.goto(`/s3/preview/image?bucket=${bucket}&key=held.png&lang=en`, {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator("[data-image-status]")).toHaveText("Loading and validating image…");
      release();
      await expect(page.locator("[data-image-status]")).toContainText("Re-encoded image loaded");
    } finally {
      release();
    }
  });
});
