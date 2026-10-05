# MineCanon — VS Code extension

Toolkit for the [Mine Canon World](../README.md) repository: canon-age switching,
mod-library audit and auto-sort, launcher/mod build integration, and Forge
1.20.1 snippets. Pure JavaScript, **zero npm dependencies** — nothing to
install, nothing to compile.

## What it adds

- **Status bar age** — shows the desktop launcher's selected canon age
  (`~/MineCanon/launcher.properties`, the same `selected` key the in-game UI
  reads and writes). Click to switch; the launcher's "Prepare profile" then
  targets `Mine Canon | <Age>`.
- **MineCanon Ages tree view** (Explorer panel) — the six ages with their
  instance mods and per-jar verdicts, plus the mod library.
- **Scanner twin** — the same COMPATIBLE / WRONG_LOADER / WRONG_VERSION /
  UNKNOWN verdicts as the launcher and the in-game mod, read straight from
  each JAR's `META-INF/mods.toml`, `fabric.mod.json` or `mcmod.info`.

## Commands (Command Palette → "MineCanon")

| Command | What it does |
| --- | --- |
| Select Age | Quick-pick an age; updates the launcher selection |
| Auto-sort Mods into Age | Dry-run scan, confirm, then copy every compatible library mod into the age's `instances/<age>/mods` (originals are never modified) |
| Scan Mod Library / Scan Game Mods Folder | Verdict list for every JAR |
| Show Canon State Bridge | Opens `minecanon-state.json` from the game dir |
| Reveal Age / Library / Game Mods Folder | Opens the folders in Explorer |
| Build MineCanon Launcher | Runs `launcher/Build-MineCanon.ps1` in a terminal |
| Build minecanon-ui Mod | Gradle 8.8 + JDK 17, build dir staged in `%TEMP%` (outside OneDrive) |
| Install UI Mod into Launcher | Copies the freshly built `minecanon-ui-1.0.0.jar` into `launcher/bin/MineCanon/app/` |

## Settings

- `minecanon.launcherHome` — default `~/MineCanon`
- `minecanon.gameDir` — default `%APPDATA%\.minecraft`
- `minecanon.libraryDir` — default the repo's `mods-1.20.1-base`
- `minecanon.jdk17` — default the VS Code Java extension pack's JDK 17

## Snippets

- Java: `mcmod` (Forge entry class + O keybind), `mcscreen` (in-game screen),
  `mckeybind`
- TOML: `modstoml` (full mods.toml), `mcdep` (dependency block)
- JSON: `mclang` (lang keys), `mcstate` (state bridge skeleton)

## Development

```text
npm test          # headless self-test: scanner parity, sort, properties
npx @vscode/vsce package   # build minecanon-vscode-1.0.0.vsix
code --install-extension minecanon-vscode-1.0.0.vsix
```

The heavy logic lives in `lib/` (pure Node, no `vscode` import), so the
self-test runs outside the editor and checks the real library and the live
game mods folder.
