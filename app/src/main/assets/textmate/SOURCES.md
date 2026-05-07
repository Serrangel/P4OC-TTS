# TextMate grammar sources

Pinned upstream sources for grammars shipped under `app/src/main/assets/textmate/`.
All sources are MIT-licensed and re-distributable.

_Last refreshed: 2026-05-07_

To regenerate: `cd scripts/textmate && npm install && node fetch_grammars.mjs`.
The script downloads each grammar at the pinned commit, performs a deterministic
plist→JSON conversion when needed (currently only `env`), and rewrites
`languages.json` plus this file. Grammar rules are vendored unmodified.

| Language | Scope | Source | Pinned ref | License |
| --- | --- | --- | --- | --- |
| env | `source.env` | [mikestead/vscode-dotenv](https://github.com/mikestead/vscode-dotenv) | `ad506a66ede7d6215cb1f2a16c169eadd414916e` | MIT |
| json | `source.json` | [microsoft/vscode (extensions/json)](https://github.com/microsoft/vscode) | `384ff7382de624fb94dbaf6da11977bba1ecd427` | MIT |
| kotlin | `source.kotlin` | [fwcd/vscode-kotlin](https://github.com/fwcd/vscode-kotlin) | `4a7c1538754828c1d22a8bee8ff3400045b4352a` | MIT |
| markdown | `text.html.markdown` | [microsoft/vscode (extensions/markdown-basics)](https://github.com/microsoft/vscode) | `384ff7382de624fb94dbaf6da11977bba1ecd427` | MIT |
| yaml | `source.yaml` | [microsoft/vscode (extensions/yaml)](https://github.com/microsoft/vscode) | `384ff7382de624fb94dbaf6da11977bba1ecd427` | MIT |
| toml | `source.toml` | [tamasfe/taplo (editors/vscode)](https://github.com/tamasfe/taplo) | `ab68333d17afab9319d0516b311a71bde828f900` | MIT |
| xml | `text.xml` | [microsoft/vscode (extensions/xml)](https://github.com/microsoft/vscode) | `384ff7382de624fb94dbaf6da11977bba1ecd427` | MIT |
| shell | `source.shell` | [microsoft/vscode (extensions/shellscript)](https://github.com/microsoft/vscode) | `384ff7382de624fb94dbaf6da11977bba1ecd427` | MIT |
| typescript | `source.ts` | [microsoft/vscode (extensions/typescript-basics)](https://github.com/microsoft/vscode) | `384ff7382de624fb94dbaf6da11977bba1ecd427` | MIT |
| python | `source.python` | [microsoft/vscode (extensions/python, vendored from MagicStack/MagicPython)](https://github.com/microsoft/vscode) | `384ff7382de624fb94dbaf6da11977bba1ecd427` | MIT |

## APK size impact

Measured on the debug variant (`./gradlew :app:assembleDebug`) on 2026-05-07:

| | Bytes | MiB |
| --- | ---: | ---: |
| Baseline `app-debug.apk` (pre-bundle) | 28 690 839 | 27.36 |
| Post-bundle `app-debug.apk` | 29 636 151 | 28.26 |
| **Delta** | **+945 312** | **+0.90** |

This is the debug-APK delta attributable to the curated 10-grammar bundle
(`app/src/main/assets/textmate/`). Release APKs compress slightly better but
the order of magnitude is the same; re-measure after any upstream refresh.

Per-grammar uncompressed JSON sizes (largest first):

| Grammar | Uncompressed |
| --- | ---: |
| typescript | ~260 KB |
| python | ~120 KB |
| markdown | ~92 KB |
| shell | ~72 KB |
| kotlin | ~20 KB |
| toml | ~16 KB |
| xml | ~16 KB |
| json | ~12 KB |
| yaml | ~8 KB |
| env | ~8 KB |
