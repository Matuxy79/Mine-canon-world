# Mine Canon — John Matukutire

Native Java desktop companion launcher, Minecraft 1.20.1 / Forge 47.4.26.
Design: obsidian #0c1115, panel #12191d, ivory #f6f1e6, muted #a4a7ad,
ember #f57c2c, border #293239, sage #a8cf8a. Serif display (Georgia / Serif),
sans-serif controls. 238/1536 sidebar ratio, 20px content gutters, full-width
cinematic voxel shrine hero, three era selections, bottom launch band.

Home copy follows generated concept. Required functional additions: a profile
preparation action in Worlds, installation state in footer, help text on setup,
and filesystem settings. Worlds are separate local instance folders, not
pre-installed maps or mods. No accounts, passwords, or access tokens are read.

Native UI intentional adaptations: scalable Swing layout; system font fallback;
dedicated three-scene artwork for era thumbnails; platform-standard file choosers and
dialogs. Native Java rendering replaces browser verification because this is
an executable desktop JAR, not a website. Headless rendering exercises the same
Swing tree used by the real window; Windows shell handoff needs local testing.

User-directed revision: add Classification, power lineage graph, Verification,
and Boot log. The revised full-screen concept is the final design reference.
Native implementation uses standard editable combo/spinner controls, source-
accurate outlier intervals, explicit mathematical review states, and real logs.
