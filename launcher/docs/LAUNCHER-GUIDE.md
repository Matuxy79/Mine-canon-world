# Mine Canon — John Matukutire

A custom native Java desktop companion launcher for **Minecraft Java 1.20.1 + Forge 47.4.26**, with an obsidian-and-ember interface and your Canon Hunters & Monsters dossiers integrated.

## Start on Windows

1. Build once with **Build-MineCanon.cmd** in the repository root (needs a JDK 17+ with jpackage), then double-click **MineCanon.lnk** in the root. The packaged `MineCanon.exe` carries its own Java runtime.
2. Minecraft 1.20.1 itself still needs Java 17+; the official launcher provides it.
3. In the official Minecraft launcher, install and run **vanilla Java 1.20.1** once, then close the game and launcher.
4. In Mine Canon **Settings**, confirm your Minecraft folder (normally `%APPDATA%\.minecraft`). Choose the profile-file variant if both Standard and Microsoft Store files exist.
5. Click **Set up Forge**. In the bundled installer, choose **Install client** and the same Minecraft folder. This step needs internet access for Forge/Minecraft dependencies.
6. Close the official launcher. Choose an age in **Worlds**, then click **Prepare profile**.
7. Click **Open Minecraft**. In the official launcher, select **Mine Canon | First Breath** (or your chosen age), sign in, and press **Play**.

This launcher hands off to the official launcher; it does not directly authenticate or boot the game. It does not implement sign-in or request passwords or access tokens. Microsoft Store handoff uses Windows Start-app discovery. If automatic discovery fails, select a launcher executable in Settings or open Minecraft manually.

## Working features

- Home: generated voxel world art, three distinct age thumbnails, John Matukutire branding.
- Worlds: six separate instance profiles and folders; chronological age labels, draft ceilings and outlier bands.
- Classification: Hunter / Monster / Hybrid identity selector; editable BST and dynamic rank/band assessment. This is a design explorer, not a Minecraft entity editor.
- Power lineage: a clickable graph. Select Breath/Bending, Relics/Resonance, Chakra, Nen or Cursed Energy to choose the corresponding age. Edges are a **proposed synthesis**, not a definitive phylogeny; the graph is not draggable yet.
- Verification: uploaded Forge JAR SHA-256; hashes for all five bundled source dossiers; Java version; local Forge version metadata; scoped finite-era inflation arithmetic; honest exception/unverified states.
- Boot log: timestamped **LORE** narrative and **LOCAL** operational events, separated explicitly. Handoff is logged as requested, never falsely as a confirmed game start.
- Mods: list local JARs, open the selected instance's mods folder, refresh the list.
- Settings: Minecraft folder, launcher executable, Standard/Store profile file, 2–16 GB memory. Preparing a profile applies the chosen memory.

No maps, creatures, gameplay mods, server or battle simulation are bundled. The landscape is original generated concept artwork, not a screenshot of an included playable world. John Matukutire is personal launcher branding; the Minecraft player identity remains your official account.

## Data and safeguards

Data lives under `~/MineCanon` (on Windows, normally `C:\Users\YOUR_NAME\MineCanon`):

- `instances/<age>/mods`, `saves`, and `resourcepacks`
- `launcher.properties`
- `profile-backups/`: exact pre-edit profile JSON snapshots
- `launcher-session.log`: append-only session events
- `forge-setup.log`: output from the installer subprocess

Existing profiles and unrelated JSON settings are preserved. Profile writing refuses malformed JSON, checks for changes during the write, and replaces the file atomically where supported. Close the official launcher before preparing profiles because it can rewrite its configuration independently. Selected-instance directories do not modify existing save folders.

To restore: close both launchers; copy the desired file from `profile-backups` back over the corresponding `launcher_profiles.json` or `launcher_profiles_microsoft_store.json` in your Minecraft folder.

The original Forge JAR is included **unchanged**. The build places it next to `MineCanon.jar` in `launcher\bin\MineCanon\app\`; the custom launcher uses the Gson JSON library already inside that JAR. Forge's installer opens only when you select the setup action. No automatic downloads or installer execution happen on startup.

Installer SHA-256:
`138961bc2a5f085ced0b5db2cd72c6caad20b25e22bcdccd031b4fc9182e8186`

A matching hash establishes equality to the supplied file, not publisher authenticity. The source-document hash manifest is an integrity snapshot, not a digital signature. The Forge metadata check does not prove every dependency was downloaded or that a given modpack is compatible.

## Dossier mapping and honest formal status

The `dossier/` folder contains unchanged copies of your five source documents. Filenames are normalized for packaging.

| Dossier | Implemented surface |
|---|---|
| Age Architecture | Ordered age selection, evolution framing, six-dimensional stat-budget vocabulary |
| Game lore | Draft rank ladder and world identities |
| Game lore 2 | Narrative initialization, lost-skill and lineage themes |
| Phylogenetic tree of power systems | Proposed branching power-dialect graph |
| Canon Field Mathematics — Formal Guarantees | Verification surface, Σ / Φ / Hₜ vocabulary, explicitly scoped arithmetic checks |

The source drafts contain different age numbering and balance tables. This version uses **Formal Guarantees §1.2** for the displayed era ceilings/outlier bands, and **Game lore** for the rank ladder. It does not silently reconcile every draft.

Rank boundaries are lower-inclusive, upper-exclusive: Initiate [280,350), Hunter [350,430), Veteran [430,500), Elite [500,570), Master [570,640), Calamity [640,720), Disaster [720,790), Anomaly [790,∞). Identity category and rank are independent. BST alone cannot certify squad legality or combat balance.

- **Finite-era ratio check:** recomputes the five finite §1.2 rows; maximum 660/540 = 1.2222… ≤ 1.23. This validates those table numbers only.
- **Margin check:** amber EXCEPTION because Nocturne has +70–110, which does not meet a universal +80–120 rule.
- **Dark Continent:** open-ended 880+ band, excluded from finite upper-bound certification.
- **Topology, algebra and statistical guarantees:** NOT VERIFIED. No proof assistant, battle-model verifier, persistent-homology computation, or conformal calibration engine is implemented.

The original mathematical dossier is preserved as a design source, not endorsed wholesale as proved mathematics.

## Build and verification

Source is in `src/`. Rebuild with a Java 17+ JDK that includes jpackage:

```text
Build-MineCanon.cmd
```

The build (`launcher\Build-MineCanon.ps1`) uses only the JDK tools and the Forge JAR in `mods-1.20.1-base`; no Maven, Gradle or npm downloads.

The build runs `SelfTest` automatically before packaging (skip with `-SkipTests`). The test uses isolated temporary directories. Full QA scope and visual adaptations are in `QA.md`.

Visual checks use the actual Swing component tree rendered headlessly at 1536×1024 and 1120×820. This Linux environment cannot exercise the Windows shell, your Microsoft account, native file choosers, or a real Minecraft/Forge installation; those need a local smoke test. A browser test is not applicable to this desktop JAR.

Official reference: https://docs.minecraftforge.net/en/1.20.1/gettingstarted/

## Four possible next additions

1. Draggable, zoomable lineage canvas with editable relationships.
2. Curated modpack manifest with dependency and version validation.
3. Per-age save backups and restore snapshots.
4. A matching in-game Forge title-screen mod and world boot sequence.

Personal fan-project launcher; not an official Mojang, Microsoft or Forge product. Third-party code stays in the original Forge distribution with its included notices. Generated landscape assets and custom source are included for your project.
