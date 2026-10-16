import { test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { checkPalettes, contrast, parseColor } from "./check-contrast.mjs";

test("WCAG reference ratios and neutral OKLCH conversion", () => {
  assert.equal(contrast(parseColor("#000000"), parseColor("#ffffff")), 21);
  assert.equal(contrast(parseColor("#ffffff"), parseColor("#ffffff")), 1);
  assert.ok(Math.abs(contrast(parseColor("#777777"), parseColor("#ffffff")) - 4.478) < 0.001);
  assert.ok(Math.abs(parseColor("oklch(0.5 0 0)")[0] - 0.388573) < 0.000001);
  assert.throws(() => parseColor("red"));
  assert.throws(() => parseColor("oklch(0.5 2 270)"));
});

test("missing palettes fail closed and deliberately low contrast is detected", async () => {
  assert.throws(() => checkPalettes(""));
  const css = await readFile(
    new URL("../src/main/resources/META-INF/resources/app.css", import.meta.url),
    "utf8",
  );
  const rows = checkPalettes(css);
  assert.equal(rows.length, 44);
  assert.ok(rows.every((row) => row.ratio >= row.required));
  const broken = css.replace(/(--palette-dark-text:) #[\da-f]+;/, "$1 #0f172a;");
  assert.ok(checkPalettes(broken).some((row) => row.ratio < row.required));
});
