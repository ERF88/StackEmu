import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";

export function parseColor(value) {
  if (/^#[\da-f]{3}$/i.test(value))
    value = `#${value
      .slice(1)
      .split("")
      .map((channel) => channel.repeat(2))
      .join("")}`;
  if (/^#[\da-f]{6}$/i.test(value)) {
    return value
      .slice(1)
      .match(/../g)
      .map((channel) => Number.parseInt(channel, 16) / 255);
  }
  const match = /^oklch\(([\d.]+)(%)? ([\d.]+) ([\d.]+)(?:deg)?\)$/.exec(value);
  if (!match) throw new Error(`Unsupported color: ${value}`);
  const lightness = Number(match[1]) / (match[2] ? 100 : 1);
  const chroma = Number(match[3]);
  const hue = Number(match[4]);
  return fromOklab([
    lightness,
    chroma * Math.cos((hue * Math.PI) / 180),
    chroma * Math.sin((hue * Math.PI) / 180),
  ]);
}

function fromOklab([lightness, a, b]) {
  const l = (lightness + 0.3963377774 * a + 0.2158037573 * b) ** 3;
  const m = (lightness - 0.1055613458 * a - 0.0638541728 * b) ** 3;
  const s = (lightness - 0.0894841775 * a - 1.291485548 * b) ** 3;
  return [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  ].map((channel) => {
    if (channel < -0.0005 || channel > 1.0005)
      throw new Error("Out-of-sRGB color needs a gamut review");
    const bounded = Math.min(1, Math.max(0, channel));
    return bounded <= 0.0031308 ? bounded * 12.92 : 1.055 * bounded ** (1 / 2.4) - 0.055;
  });
}

function luminance(rgb) {
  const linear = rgb.map((channel) =>
    channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4,
  );
  return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2];
}

export function contrast(first, second) {
  const values = [luminance(first), luminance(second)].sort((a, b) => b - a);
  return (values[0] + 0.05) / (values[1] + 0.05);
}

export function readPalettes(css) {
  const palettes = { fallback: {}, oklch: {} };
  for (const [, theme, role, value] of css.matchAll(
    /--palette-(dark|light)-([a-z0-9-]+):\s*([^;]+);/g,
  )) {
    const mode = value.startsWith("oklch(") ? "oklch" : "fallback";
    palettes[mode][theme] ??= {};
    palettes[mode][theme][role] = value.trim();
  }
  return palettes;
}

export function checkPalettes(css) {
  const rows = [];
  const palettes = readPalettes(css);
  const surfaces = ["canvas", "surface-1", "surface-2", "surface-3"];
  for (const [mode, themes] of Object.entries(palettes)) {
    for (const theme of ["dark", "light"]) {
      const values = themes[theme];
      if (!values) throw new Error(`Missing ${mode}/${theme} palette`);
      const color = (role) => parseColor(values[role] ?? "missing");
      for (const role of [
        "text",
        "text-muted",
        "primary",
        "success",
        "danger",
        "warning",
        "info",
        "border",
        "focus",
      ]) {
        const ratio = Math.min(...surfaces.map((surface) => contrast(color(role), color(surface))));
        rows.push({
          mode,
          theme,
          pair: `${role} / surfaces (minimum)`,
          ratio,
          required: ["border", "focus"].includes(role) ? 3 : 4.5,
        });
      }
      rows.push({
        mode,
        theme,
        pair: "on-primary / primary",
        ratio: contrast(color("on-primary"), color("primary")),
        required: 4.5,
      });
      {
        // sRGB interpolation keeps hover in gamut; match the actual CSS mix.
        const primary = color("primary");
        const text = color("text");
        const hover = primary.map((channel, index) => channel * 0.85 + text[index] * 0.15);
        rows.push({
          mode,
          theme,
          pair: "on-primary / hover",
          ratio: contrast(color("on-primary"), hover),
          required: 4.5,
        });
      }
    }
  }
  return rows;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const source = await readFile(
      new URL("../src/main/resources/META-INF/resources/app.css", import.meta.url),
      "utf8",
    );
    const rows = checkPalettes(source);
    process.stdout.write(
      "| Palette | Theme | Pair | Ratio | Required |\n| --- | --- | --- | --- | --- |\n",
    );
    for (const row of rows)
      process.stdout.write(
        `| ${row.mode} | ${row.theme} | ${row.pair} | ${row.ratio.toFixed(2)} | ${row.required} |\n`,
      );
    if (rows.some((row) => row.ratio < row.required)) process.exitCode = 1;
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  }
}
