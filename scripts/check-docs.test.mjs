import assert from "node:assert/strict";
import { after, test } from "node:test";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { basename, dirname, join, resolve } from "node:path";
import { tmpdir } from "node:os";
import { inspectDocumentation, linkTargets } from "./check-docs.mjs";

function fixture() {
  const parent = resolve(tmpdir());
  const root = mkdtempSync(join(parent, "stackemu-doc-check-"));
  after(() => {
    if (dirname(root) !== parent || !basename(root).startsWith("stackemu-doc-check-"))
      throw new Error("Refusing cleanup outside owned temporary fixture");
    rmSync(root, { recursive: true, force: true });
  });
  mkdirSync(join(root, "docs"));
  writeFileSync(join(root, "README.md"), "# Entry\n\n[Guide](docs/guide.md#guide--guia)\n");
  writeFileSync(join(root, "docs/guide.md"), "# Guide / Guia\n");
  writeFileSync(
    join(root, "docs/README.md"),
    "# Index\n\n## Use / Usar\n\n- [Entry](../README.md)\n- [Guide](guide.md)\n\n## Contribute / Contribuir\n\n## Decisions / Decisões\n\n## Evidence / Evidências\n",
  );
  return root;
}

test("Markdown subset covers images and references while ignoring code examples", () => {
  assert.deepEqual(
    linkTargets(
      "![Image](a.png)\n[Link](<folder/a.md>)\n[id]: b.md\n`[Code](no.md)`\n```md\n[Example](missing.md)\n```",
    ),
    ["a.png", "folder/a.md", "b.md"],
  );
});

test("valid links, headings and exact-once grouped index pass", () => {
  const result = inspectDocumentation(fixture());
  assert.equal(result.documents, 3);
  assert.deepEqual(result.errors, []);
});

test("missing files, anchors and duplicate index entries fail visibly", () => {
  const root = fixture();
  writeFileSync(
    join(root, "README.md"),
    "# Entry\n[Missing](missing.md)\n[Bad anchor](docs/guide.md#absent)\n",
  );
  const index = join(root, "docs/README.md");
  writeFileSync(index, `${readFileSync(index, "utf8")}\n- [Duplicate](../README.md)\n`);
  const errors = inspectDocumentation(root).errors.join("\n");
  assert.match(errors, /missing target missing.md/);
  assert.match(errors, /missing anchor/);
  assert.match(errors, /README.md appears 2 times/);
});

test("missing index coverage fails and external/artifact links are reported separately", () => {
  const root = fixture();
  writeFileSync(
    join(root, "docs/unlisted.md"),
    "# Unlisted\n[Web](https://example.invalid/)\n[Report](../work/report.json)\n",
  );
  const result = inspectDocumentation(root);
  assert.equal(result.externalLinks, 1);
  assert.equal(result.artifactLinks, 1);
  assert.match(result.errors.join("\n"), /unlisted.md appears 0 times/);
});

test("GitHub contribution templates also require index coverage", () => {
  const root = fixture();
  mkdirSync(join(root, ".github"));
  writeFileSync(join(root, ".github/pull_request_template.md"), "# Pull request\n");
  assert.match(
    inspectDocumentation(root).errors.join("\n"),
    /\.github\/pull_request_template.md appears 0 times/,
  );
});
