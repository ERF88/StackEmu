import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";

const emulatorURL = "http://127.0.0.1:4566";
const ownedFixtures = new Map();

test.afterEach(async ({ playwright }, testInfo) => {
  const bucket = ownedFixtures.get(testInfo.testId);
  if (!bucket) return;
  const cleanup = await playwright.request.newContext();
  try {
    // Only this test's exact, empty bucket; never discover other resources.
    expect((await cleanup.delete(`${emulatorURL}/${bucket}`)).status()).toBe(204);
  } finally {
    ownedFixtures.delete(testInfo.testId);
    await cleanup.dispose();
  }
});

for (const javaScriptEnabled of [true, false]) {
  test.describe(`HIG explorer, JavaScript=${javaScriptEnabled}`, () => {
    test.use({ javaScriptEnabled });
    for (const language of ["en", "pt-BR"]) {
      for (const theme of ["light", "dark"]) {
        test(`empty prefix, guidance and resource error: ${language}, ${theme}`, async ({
          page,
          request,
        }, testInfo) => {
          const bucket = `stackemu-hig-${randomUUID().replaceAll("-", "")}`;
          expect((await request.put(`${emulatorURL}/${bucket}`)).status()).toBe(200);
          ownedFixtures.set(testInfo.testId, bucket);
          await page.emulateMedia({ reducedMotion: "reduce" });
          for (const density of ["comfortable", "compact"]) {
            const preferences = `lang=${language}&theme=${theme}&density=${density}`;
            await page.goto(`/s3/bucket?bucket=${bucket}&prefix=empty%2F&${preferences}`);
            await expect(page.locator(".object-list-panel .resource-state h3")).toHaveText(
              language === "en"
                ? "No objects or prefixes found here."
                : "Nenhum objeto ou prefixo encontrado aqui.",
            );
            await expect(page.locator(".object-table")).toHaveCount(0);
            const upload = new URL(
              await page.locator(".resource-state a").getAttribute("href"),
              page.url(),
            );
            expect(upload.pathname).toBe("/s3/write");
            expect(upload.searchParams.get("action")).toBe("upload");
            expect(upload.searchParams.get("bucket")).toBe(bucket);
            expect(upload.searchParams.get("prefix")).toBe("empty/");
            expect(upload.searchParams.get("lang")).toBe(language);
            expect(upload.searchParams.get("theme")).toBe(theme);
            if (density === "compact") expect(upload.searchParams.get("density")).toBe(density);
            const help = page.locator(".explorer-help");
            await expect(help).not.toHaveAttribute("open");
            await help.locator("summary").focus();
            await page.keyboard.press("Enter");
            await expect(help).toHaveAttribute("open", "");
            await expect(help.locator("p")).toHaveCount(2);
            await expect(help.locator("p").first()).toBeVisible();
            await page.keyboard.press("Enter");
            await expect(help).not.toHaveAttribute("open");
            for (const width of [390, 1440]) {
              await page.setViewportSize({ width, height: 1000 });
              expect(
                await page.evaluate(
                  () => document.documentElement.scrollWidth <= window.innerWidth,
                ),
              ).toBe(true);
              const bounds = await page.locator(".resource-state a").boundingBox();
              expect(bounds.height).toBeGreaterThanOrEqual(44);
              await page.screenshot({
                path: testInfo.outputPath(`empty-${density}-${width}.png`),
                fullPage: true,
              });
            }
            if (javaScriptEnabled)
              expect(
                (
                  await new AxeBuilder({ page })
                    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
                    .analyze()
                ).violations,
              ).toEqual([]);
          }
          const response = await page.goto(
            `/s3/object?bucket=${bucket}&key=absent.txt&lang=${language}&theme=${theme}&density=compact`,
          );
          expect(response.status()).toBe(404);
          await expect(page.locator(".resource-state[role=alert]")).toBeVisible();
          const back = page.locator('.resource-state a[href^="/s3?"]');
          await expect(back).toHaveAttribute(
            "href",
            `/s3?lang=${language}&theme=${theme}&density=compact`,
          );
          await back.focus();
          await expect(back).toHaveCSS("outline-style", "solid");
          await page.keyboard.press("Enter");
          await expect(page).toHaveURL(/\/s3\?/);
        });
      }
    }
  });
}
