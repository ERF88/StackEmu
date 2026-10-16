import { mkdir, readFile, copyFile } from "node:fs/promises";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import assert from "node:assert/strict";

const root = new URL("../", import.meta.url);
const output = new URL("target/generated-resources/META-INF/resources/", root);
await mkdir(output, { recursive: true });
execFileSync(
  process.execPath,
  [
    fileURLToPath(new URL("node_modules/@tailwindcss/cli/dist/index.mjs", root)),
    "-i",
    fileURLToPath(new URL("src/main/frontend/input.css", root)),
    "-o",
    fileURLToPath(new URL("app.css", output)),
    "--minify",
  ],
  { cwd: root, stdio: "inherit" },
);
const css = await readFile(new URL("app.css", output), "utf8");
assert.match(css, /\.grid\s*\{/);
assert.match(css, /--palette-dark-canvas/);
assert.doesNotMatch(css, /@import|@source|@theme/);
const licenses = new URL("target/generated-resources/META-INF/licenses/", root);
await mkdir(licenses, { recursive: true });
await copyFile(
  new URL("node_modules/tailwindcss/LICENSE", root),
  new URL("tailwindcss-LICENSE.txt", licenses),
);
