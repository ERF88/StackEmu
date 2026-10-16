import { readFile, writeFile, mkdir, copyFile, readdir } from "node:fs/promises";
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { gzipSync } from "node:zlib";
import assert from "node:assert/strict";
import console from "node:console";
import { fileURLToPath } from "node:url";

// Outputs only ignored work/ artifacts; never overwrite production resources.
const directory = new URL("../../work/ux0/", import.meta.url);
await mkdir(directory, { recursive: true });
const hash = (bytes) => createHash("sha256").update(bytes).digest("hex");
const root = new URL("../../", import.meta.url);
const cli = new URL("node_modules/@tailwindcss/cli/dist/index.mjs", import.meta.url);
const input = new URL("input.css", import.meta.url);
const compile = (name) => {
  const output = new URL(name, directory);
  execFileSync(
    process.execPath,
    [fileURLToPath(cli), "-i", fileURLToPath(input), "-o", fileURLToPath(output), "--minify"],
    {
      cwd: root,
      stdio: "inherit",
    },
  );
  return output;
};
const utilities = await readFile(compile("utilities.css"));
assert.equal(hash(utilities), hash(await readFile(compile("utilities-repeat.css"))));
const source = utilities.toString();
assert.match(source, /\.gap-4/);
assert.match(source, /hover\\:bg-panel/);
assert.match(source, /dark\\:underline/);
assert.doesNotMatch(source, /\.p-37(?:[\s{,])/);
assert.doesNotMatch(source, /box-sizing:border-box/); // No Tailwind Preflight.
const current = await readFile(new URL("src/main/resources/META-INF/resources/app.css", root));
const pico = await readFile(
  new URL("node_modules/@picocss/pico/css/pico.min.css", import.meta.url),
);
const assets = {
  current,
  tailwind: Buffer.concat([current, Buffer.from("\n"), utilities]),
  pico: Buffer.concat([current, Buffer.from("\n"), pico]),
};
const report = {
  scope: "additive prototype, not production performance",
  assets: {},
  licenses: {},
};
for (const [candidate, bytes] of Object.entries(assets)) {
  const fingerprint = hash(bytes);
  report.assets[candidate] = {
    bytes: bytes.length,
    gzipBytes: gzipSync(bytes).length,
    sha256: fingerprint,
  };
  await writeFile(new URL(`${candidate}.css`, directory), bytes);
  for (const language of ["en", "pt-BR"]) {
    for (const theme of ["dark", "light", "auto"]) {
      const name = `${candidate}-${language}-${theme}.html`;
      const fixture = await readFile(new URL(`work/ux0-fixtures/${name}`, root), "utf8");
      await writeFile(new URL(name, directory), fixture.replaceAll("CSS_FINGERPRINT", fingerprint));
    }
  }
}
for (const name of ["tailwindcss", "@tailwindcss/cli", "@picocss/pico"]) {
  const location = new URL(`node_modules/${name}/`, import.meta.url);
  const metadata = JSON.parse(await readFile(new URL("package.json", location), "utf8"));
  const license = (await readdir(location)).find((file) =>
    /^licen[sc]e(?:\.md|\.txt)?$/i.test(file),
  );
  assert.ok(license, `Missing license: ${name}`);
  const bytes = await readFile(new URL(license, location));
  assert.match(bytes.toString(), /MIT License|Permission is hereby granted/);
  const target = `license-${name.replaceAll("/", "-").replaceAll("@", "")}.txt`;
  await copyFile(new URL(license, location), new URL(target, directory));
  report.licenses[name] = {
    version: metadata.version,
    license: metadata.license,
    sha256: hash(bytes),
    file: target,
  };
}
await writeFile(new URL("build-report.json", directory), `${JSON.stringify(report, null, 2)}\n`);
console.log(JSON.stringify(report, null, 2));
