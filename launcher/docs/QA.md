# Verification and design review

41 automated checks passed on Java 17.0.20 in isolated temporary directories.

## Functional coverage

- Missing/wrong Forge metadata is rejected before profile writes.
- Exact pre-edit backups, unrelated profile/settings preservation, correct Forge pin, memory allocation and separate instance folders.
- Idempotent profile updates; Standard vs Microsoft Store ambiguity and explicit selection.
- Malformed profile JSON fails without modifying the file.
- Selected-age persistence, JAR-only mods listing, original installer hash.
- All five source hashes, scoped inflation arithmetic, amber Nocturne exception, unverified formal claims.
- Rank boundaries, native age buttons, graph-node selection, BST spinner, all six navigation screens.
- Narrative vs operational log tags; minimum-window layout dimensions and launch-button placement.

## Native visual review

This is a Swing desktop JAR. Browser/IAB/Playwright are inapplicable. Screenshots are produced by the same actual Swing component tree used by the application, using its built-in `--render` path. No HTML mockup stands in for the shipped interface.

Compared the revised image-generation concept to the native Home render using image inspection at 1536×1024. Also inspected Home at 1120×820 and all five other pages at full size. Final visual review found no clipping of the primary content or launch actions at these sizes. Files outside the package used for inspection:

- Revised concept: `qa/revised-concept.png`
- Final native rendering: `output/Mine-Canon-Launcher-Preview.png`

| Comparison point | Evidence and disposition |
|---|---|
| Layout | Left identity rail, main image/age/classification column, right lineage/verification/log column and launch band retained. |
| Typography | Serif display headings and restrained sans-serif controls retained. Native system font metrics differ from the image concept. |
| Palette | Obsidian #0c1115, panel #12191d, ivory #f6f1e6, orange #f57c2c and muted slate used consistently. |
| Artwork | Separate generated voxel shrine asset and three era assets used beneath real interactive controls; no screenshot UI. Nocturne intentionally uses western gothic architecture. |
| Graph | Native clickable nodes and curved edges preserve the branching structure. Decorative elemental pictures replaced by restrained text/color identities. |
| Classification | Real keyboard-accessible category/BST controls. One Hunter/Monster/Hybrid selector instead of the concept's redundant two category fields. Source-accurate outlier interval 620–660 replaces the concept's shorthand 660. |
| Status semantics | Actual measurements, explicit setup-required state, Nocturne exception, and NOT VERIFIED theorem status; no invented green formal certificate. |
| Copy | Branding, main headline, slogan, age names, navigation and main CTAs match. Intentional additions explain profile handoff, draft classification, assessment and verification scope. |
| Responsive desktop | Minimum-window graph, classification and launch band inspected. This desktop launcher does not claim a mobile interface. |

Corrections made during review: text antialiasing; control font scale; dark spinner editor; duplicated thumbnail artwork replaced with three scenes; wrapped-note layout fixed; verification rows constrained to viewport width; minimum-size classification spacing; narrow action-label clipping; asynchronous launcher lookup to keep the UI responsive.

The implementation was compared directly with the revised design and preserves its visual hierarchy and main interactions, with the listed native-control and evidence-driven adaptations. It is not asserted to be pixel-identical to generative concept art.

## Not executed here

Windows Start-menu discovery / explorer handoff, official-account sign-in, the Forge installer dependency downloads, live Minecraft launch, file-chooser interaction on Windows, and third-party mod compatibility. This environment has no Windows desktop or the user's installed Minecraft. Headless rendering and fixture tests do not establish that those external workflows succeed on the target PC.

The delivered GUI is a companion that prepares instances and hands off to the official launcher. It is not a direct authenticated Minecraft launch implementation, a theorem prover, a modpack or a playable world.
