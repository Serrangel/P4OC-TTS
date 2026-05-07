#!/usr/bin/env node
/*
 * One-shot grammar fetch + (if needed) plist→JSON conversion.
 *
 * Run from repo root:
 *   node scripts/textmate/fetch_grammars.mjs
 *
 * Writes to app/src/main/assets/textmate/<lang>/<lang>.tmLanguage.json
 * and emits app/src/main/assets/textmate/SOURCES.md with pinned commit SHAs,
 * source URLs, license, retrieval date.
 *
 * Pure download + (rare) plist→JSON conversion. We DO NOT modify grammar
 * content. All sources here are MIT-licensed.
 */

import { mkdir, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import * as plist from "plist";

const ROOT = new URL("../../app/src/main/assets/textmate/", import.meta.url).pathname;

// --- Grammar manifest -------------------------------------------------------
// Each entry pins a specific upstream commit SHA. Refresh by editing here.
// Pinned commit SHAs (resolved from tags / branch HEADs at retrieval time).
const VSCODE_SHA = "384ff7382de624fb94dbaf6da11977bba1ecd427"; // tag 1.94.2
const FWCD_KOTLIN_SHA = "4a7c1538754828c1d22a8bee8ff3400045b4352a"; // main
const TAPLO_SHA = "ab68333d17afab9319d0516b311a71bde828f900"; // tag 0.9.3
const DOTENV_SHA = "ad506a66ede7d6215cb1f2a16c169eadd414916e"; // master HEAD

// shorthand for vscode raw URL
const vsc = (path) =>
  `https://raw.githubusercontent.com/microsoft/vscode/${VSCODE_SHA}/extensions/${path}`;

const GRAMMARS = [
  {
    id: "env",
    scope: "source.env",
    out: "env/env.tmLanguage.json",
    // Upstream ships a plist `.tmLanguage`; we convert at fetch time.
    url: `https://raw.githubusercontent.com/mikestead/vscode-dotenv/${DOTENV_SHA}/syntaxes/env.tmLanguage`,
    upstream: "https://github.com/mikestead/vscode-dotenv",
    sha: DOTENV_SHA,
    license: "MIT",
    project: "mikestead/vscode-dotenv",
  },
  {
    id: "json",
    scope: "source.json",
    out: "json/json.tmLanguage.json",
    url: vsc("json/syntaxes/JSON.tmLanguage.json"),
    upstream: "https://github.com/microsoft/vscode",
    sha: VSCODE_SHA,
    license: "MIT",
    project: "microsoft/vscode (extensions/json)",
  },
  {
    id: "kotlin",
    scope: "source.kotlin",
    out: "kotlin/kotlin.tmLanguage.json",
    url: `https://raw.githubusercontent.com/fwcd/vscode-kotlin/${FWCD_KOTLIN_SHA}/syntaxes/kotlin.tmLanguage.json`,
    upstream: "https://github.com/fwcd/vscode-kotlin",
    sha: FWCD_KOTLIN_SHA,
    license: "MIT",
    project: "fwcd/vscode-kotlin",
  },
  {
    id: "markdown",
    scope: "text.html.markdown",
    out: "markdown/markdown.tmLanguage.json",
    url: vsc("markdown-basics/syntaxes/markdown.tmLanguage.json"),
    upstream: "https://github.com/microsoft/vscode",
    sha: VSCODE_SHA,
    license: "MIT",
    project: "microsoft/vscode (extensions/markdown-basics)",
  },
  {
    id: "yaml",
    scope: "source.yaml",
    out: "yaml/yaml.tmLanguage.json",
    url: vsc("yaml/syntaxes/yaml.tmLanguage.json"),
    upstream: "https://github.com/microsoft/vscode",
    sha: VSCODE_SHA,
    license: "MIT",
    project: "microsoft/vscode (extensions/yaml)",
  },
  {
    id: "toml",
    scope: "source.toml",
    out: "toml/toml.tmLanguage.json",
    url: `https://raw.githubusercontent.com/tamasfe/taplo/${TAPLO_SHA}/editors/vscode/toml.tmLanguage.json`,
    upstream: "https://github.com/tamasfe/taplo",
    sha: TAPLO_SHA,
    license: "MIT",
    project: "tamasfe/taplo (editors/vscode)",
  },
  {
    id: "xml",
    scope: "text.xml",
    out: "xml/xml.tmLanguage.json",
    url: vsc("xml/syntaxes/xml.tmLanguage.json"),
    upstream: "https://github.com/microsoft/vscode",
    sha: VSCODE_SHA,
    license: "MIT",
    project: "microsoft/vscode (extensions/xml)",
  },
  {
    id: "shell",
    scope: "source.shell",
    out: "shell/shell.tmLanguage.json",
    url: vsc("shellscript/syntaxes/shell-unix-bash.tmLanguage.json"),
    upstream: "https://github.com/microsoft/vscode",
    sha: VSCODE_SHA,
    license: "MIT",
    project: "microsoft/vscode (extensions/shellscript)",
  },
  {
    id: "typescript",
    scope: "source.ts",
    out: "typescript/typescript.tmLanguage.json",
    url: vsc("typescript-basics/syntaxes/TypeScript.tmLanguage.json"),
    upstream: "https://github.com/microsoft/vscode",
    sha: VSCODE_SHA,
    license: "MIT",
    project: "microsoft/vscode (extensions/typescript-basics)",
  },
  {
    id: "python",
    scope: "source.python",
    out: "python/python.tmLanguage.json",
    url: vsc("python/syntaxes/MagicPython.tmLanguage.json"),
    upstream: "https://github.com/microsoft/vscode",
    sha: VSCODE_SHA,
    license: "MIT",
    project: "microsoft/vscode (extensions/python, vendored from MagicStack/MagicPython)",
  },
];

async function fetchText(url) {
  const r = await fetch(url);
  if (!r.ok) throw new Error(`HTTP ${r.status} ${url}`);
  return r.text();
}

function looksLikePlist(s) {
  return s.trimStart().startsWith("<?xml") || s.trimStart().startsWith("<!DOCTYPE plist");
}

async function main() {
  const ymd = new Date().toISOString().slice(0, 10);
  const sourcesLines = [
    "# TextMate grammar sources",
    "",
    "Pinned upstream sources for grammars shipped under `app/src/main/assets/textmate/`.",
    "All sources are MIT-licensed and re-distributable.",
    "",
    `_Last refreshed: ${ymd}_`,
    "",
    "| Language | Scope | Source | Pinned ref | License |",
    "| --- | --- | --- | --- | --- |",
  ];

  for (const g of GRAMMARS) {
    process.stdout.write(`fetch ${g.id} ... `);
    let body = await fetchText(g.url);
    if (looksLikePlist(body)) {
      const obj = plist.parse(body);
      body = JSON.stringify(obj, null, 2) + "\n";
    } else {
      // Minify-pretty: re-emit through JSON.parse to normalise.
      const obj = JSON.parse(body);
      body = JSON.stringify(obj, null, 2) + "\n";
    }
    const target = join(ROOT, g.out);
    await mkdir(dirname(target), { recursive: true });
    await writeFile(target, body);
    sourcesLines.push(
      `| ${g.id} | \`${g.scope}\` | [${g.project}](${g.upstream}) | \`${g.sha}\` | ${g.license} |`,
    );
    process.stdout.write("ok\n");
  }

  // languages.json — config consumed by Sora's LanguageDefinitionReader.
  // Sora expects: { "grammarDefinition": [ { name, scopeName, grammar, ... } ] }
  // where `grammar` is the asset path resolved via FileProviderRegistry.
  const languagesJson = {
    grammarDefinition: GRAMMARS.map((g) => ({
      name: g.id,
      scopeName: g.scope,
      grammar: `textmate/${g.out}`,
    })),
  };
  await writeFile(
    join(ROOT, "languages.json"),
    JSON.stringify(languagesJson, null, 2) + "\n",
  );

  await writeFile(join(ROOT, "SOURCES.md"), sourcesLines.join("\n") + "\n");
  console.log("done.");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
