import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { dirname, extname, isAbsolute, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

// Repository Markdown subset: inline/image links, reference definitions, ATX headings
// and explicit HTML ids. No external HTTP checks, full Markdown parser or prose review.
function withoutFences(text) {
  let fence = "";
  return text
    .split(/\r?\n/)
    .map((line) => {
      const marker = line.match(/^ {0,3}(`{3,}|~{3,})/);
      if (marker) {
        if (!fence) fence = marker[1];
        else if (marker[1][0] === fence[0] && marker[1].length >= fence.length) fence = "";
        return "";
      }
      return fence ? "" : line;
    })
    .join("\n");
}

export function linkTargets(text) {
  const prose = withoutFences(text).replace(/`+[^`\n]*`+/g, "");
  return [
    ...prose.matchAll(/!?\[[^\]\n]*\]\(\s*(?:<([^>]+)>|([^\s)]+))(?:\s+["'][^"']*["'])?\s*\)/g),
    ...prose.matchAll(/^ {0,3}\[[^\]]+\]:\s*(?:<([^>]+)>|(\S+))/gm),
  ].map((match) => match[1] ?? match[2]);
}

function headingIds(text) {
  const prose = withoutFences(text);
  const ids = new Set([...prose.matchAll(/\bid=["']([^"']+)["']/g)].map((match) => match[1]));
  for (const match of prose.matchAll(/^ {0,3}#{1,6}\s+(.+?)\s*$/gm)) {
    const base = match[1]
      .replace(/\s+#+$/, "")
      .replace(/<[^>]*>/g, "")
      .toLowerCase()
      .replace(/[^\p{L}\p{M}\p{N}_\-\s]/gu, "")
      .trim()
      .replace(/\s/g, "-");
    let slug = base;
    let suffix = 0;
    while (ids.has(slug)) slug = `${base}-${++suffix}`;
    ids.add(slug);
  }
  return ids;
}

const slash = (path) => path.replaceAll("\\", "/");
function markdownFiles(root) {
  const documents = readdirSync(root, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith(".md"))
    .map((entry) => entry.name);
  function visit(directory) {
    if (!existsSync(directory)) return;
    for (const entry of readdirSync(directory, { withFileTypes: true })) {
      const path = resolve(directory, entry.name);
      if (entry.isDirectory()) visit(path);
      else if (entry.isFile() && entry.name.endsWith(".md"))
        documents.push(slash(relative(root, path)));
    }
  }
  visit(resolve(root, "docs"));
  visit(resolve(root, ".github"));
  if (existsSync(resolve(root, "tools/ux0/README.md"))) documents.push("tools/ux0/README.md");
  return documents.sort();
}

export function inspectDocumentation(root) {
  root = resolve(root);
  const documents = markdownFiles(root);
  const errors = [];
  let checkedLinks = 0,
    externalLinks = 0,
    artifactLinks = 0;
  function locate(source, target) {
    if (/^[a-z][a-z\d+.-]*:/i.test(target) || target.startsWith("//")) return null;
    const path = target.split(/[?#]/)[0];
    return resolve(
      path.startsWith("/") ? root : dirname(resolve(root, source)),
      path ? decodeURIComponent(path.replace(/^\//, "")) : resolve(root, source),
    );
  }
  for (const source of documents) {
    for (const target of linkTargets(readFileSync(resolve(root, source), "utf8"))) {
      let destination;
      try {
        destination = locate(source, target);
      } catch {
        errors.push(`${source}: malformed link ${target}`);
        continue;
      }
      if (!destination) {
        externalLinks++;
        continue;
      }
      const path = slash(relative(root, destination));
      if (path.startsWith("../") || isAbsolute(path)) {
        errors.push(`${source}: target outside repository ${target}`);
        continue;
      }
      // Historical references to ignored run artifacts are intentionally not portable evidence.
      if (/^(?:work|target|outputs)\//.test(path)) {
        artifactLinks++;
        continue;
      }
      checkedLinks++;
      if (!existsSync(destination)) {
        errors.push(`${source}: missing target ${target}`);
        continue;
      }
      const hash = target.indexOf("#");
      if (
        hash >= 0 &&
        hash < target.length - 1 &&
        statSync(destination).isFile() &&
        extname(destination) === ".md"
      ) {
        let anchor;
        try {
          anchor = decodeURIComponent(target.slice(hash + 1));
        } catch {
          errors.push(`${source}: malformed anchor ${target}`);
          continue;
        }
        if (!headingIds(readFileSync(destination, "utf8")).has(anchor))
          errors.push(`${source}: missing anchor ${target}`);
      }
    }
  }
  const navigation = "docs/README.md";
  const indexed = new Map();
  const groups = new Set([
    "Use / Usar",
    "Contribute / Contribuir",
    "Decisions / Decisões",
    "Evidence / Evidências",
  ]);
  const seenGroups = new Set();
  if (!existsSync(resolve(root, navigation)))
    errors.push("Missing docs/README.md navigation index");
  else {
    let group = "";
    for (const line of withoutFences(readFileSync(resolve(root, navigation), "utf8")).split("\n")) {
      if (line.startsWith("## ")) {
        group = line.slice(3);
        if (!groups.has(group)) errors.push(`Index: unexpected group ${group}`);
        else seenGroups.add(group);
      }
      const entry = line.match(/^- \[[^\]]+\]\(([^)]+)\)$/);
      if (!entry) continue;
      if (!groups.has(group)) errors.push(`Index entry outside the four groups: ${entry[1]}`);
      const target = locate(navigation, entry[1]);
      if (!target || extname(target) !== ".md") continue;
      const path = slash(relative(root, target));
      indexed.set(path, (indexed.get(path) ?? 0) + 1);
    }
    for (const group of groups)
      if (!seenGroups.has(group)) errors.push(`Index: missing group ${group}`);
    for (const path of documents.filter((path) => path !== navigation)) {
      const count = indexed.get(path) ?? 0;
      if (count !== 1) errors.push(`Index: ${path} appears ${count} times, expected exactly once`);
    }
    for (const path of indexed.keys())
      if (!documents.includes(path)) errors.push(`Index: unknown document ${path}`);
  }
  return { documents: documents.length, checkedLinks, externalLinks, artifactLinks, errors };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const result = inspectDocumentation(fileURLToPath(new URL("../", import.meta.url)));
  process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  if (result.errors.length) process.exitCode = 1;
}
