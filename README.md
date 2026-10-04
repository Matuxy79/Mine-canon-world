# Mine Canon World

A fan-made, open-source crossover world and lore game being built for Minecraft
1.20.1, bringing together settings, characters, and stories from multiple shows.

## Cross-developer modification permission

John Matukutire explicitly authorizes cross-developer collaboration on Mine Canon
World, including modifying its Minecraft 1.20.1 mods, world files, configurations,
JAR files, and other project files, to the extent that he owns those materials or
has the right to authorize their modification. This permission is intended to
support development of this fan-made crossover world and lore game.

This project-level authorization does not override third-party licenses or grant
rights that John Matukutire does not hold. Each third-party Minecraft mod, JAR,
library, resource pack, and other asset remains subject to its own license and
its rights holder's permissions. Before modifying or redistributing those
materials, check the applicable license and obtain any additional permission
required. The presence of a file in this local folder is not evidence of
permission to modify or redistribute it.

## Project contents

- `saves/`: Minecraft world saves, including `minecraft world`.
- `mods-1.20.1-base/`: locally collected mod binaries and related files.
- `resourcepacks/`: local resource packs.

The target game version is Minecraft 1.20.1. Files in the local mod folder are
not a verified compatible modpack; check each mod's game version, loader, and
dependencies before use.

## MineCanon desktop launcher

**MineCanon** is the John Matukutire desktop GUI for Mine Canon World. Its
orange-accented interface uses the supplied canon landscape artwork, with Home,
Worlds, Mods, and Settings pages. The original design references remain in
`minecanon launcher`; the GUI loads `launcher\assets\canon-landscape.png`.

For a native Windows app, double-click `launcher\Install-MineCanon.cmd` once.
It compiles the app, bundles a minimal Java runtime, creates
`launcher\dist\MineCanon\MineCanon.exe`, and installs a **MineCanon** shortcut on
your Windows desktop. Building requires a **JDK 17+ with jpackage**; the bundled
app needs no separately installed Java and opens without a console window.
No administrator access, downloads, or system-wide file associations are needed.
Close MineCanon before rerunning the installer to update its app code.

Keep the packaged app inside this repository: artwork, saves, mods, and
preferences are resolved relative to it, even when started from another working
directory. If you move the repository, rerun the installer to recreate the
desktop shortcut. Generated build/runtime folders are ignored by Git. To remove
the desktop integration, delete the MineCanon shortcut and `launcher\dist\MineCanon`;
your worlds and mods are unaffected.

`launcher\MineCanon.cmd` opens the packaged app when available and otherwise
falls back to source mode.

Double-click `launcher\Launch-Mine-Canon-World.cmd` on Windows to open the custom
Mine Canon World hub. It requires **a Java JDK 17 or newer** on `PATH` or through
`JAVA_HOME`, and runs directly from source without downloading dependencies or
requiring a build. Use the script rather than opening a JAR through Windows'
"Open with" dialog. Keep the `launcher` folder inside this repository.

Both entry points use the same GUI and launcher settings:

- **Open Minecraft** opens the official Minecraft Launcher. It detects standard
  desktop and Microsoft Store installations; select a custom launcher executable
  in Settings if necessary. Authentication stays in the official launcher.
- **Open Forge installer** starts the bundled 1.20.1 / 47.4.26 installer using
  Java explicitly, bypassing JAR file associations. You confirm the action and
  choose installation options in Forge's own window. If no window appears, read
  `launcher\forge-installer.log`. Git LFS pointer files must be downloaded before
  they can run as JARs.
- **World, mod, and resource-pack shortcuts** open the local folders. The mod
  count includes nested copies and installers, not just usable unique mods.

This is a **launcher hub, not a game runtime or modpack installer**. It does not
automatically select a profile, copy saves, install mods, modify third-party
JARs, or bypass Minecraft ownership. Run vanilla Java 1.20.1 once before installing
Forge. Select the appropriate profile in Minecraft Launcher and check its game
directory. Back up and copy the world into that directory's `saves` folder while
Minecraft is closed, and install only compatible mods. Do not copy this entire
mixed-loader collection into an active `mods` folder.

Local launcher preferences and installer logs are ignored by Git. To run the
small dependency-free checks, compile the two Java source files into a temporary
directory with `javac --release 17 -d <temp-dir> launcher\MineCanonLauncher.java
launcher\MineCanonLauncherTest.java`, then run `java -Djava.awt.headless=true
-cp <temp-dir> MineCanonLauncherTest`.

## Contributing

Back up the world and close Minecraft before editing or copying save files.
Keep changes focused on the shared world, lore, and game experience. For changes
to third-party mods, document their source, version, license, and any additional
permission relied on, and preserve required attribution and notices.

Git ignores local logs, player-specific progress, preferences, and incomplete
downloads. JAR binaries are ignored by default except in `mods-1.20.1-base/`,
whose mod JARs are tracked via Git LFS. Ignoring a file does not delete it from
the local installation. Do not force-add other third-party binaries without
first confirming redistribution rights and checking the hosting service's
file-size limits.

## Licensing and fan-project status

Original contributions to Mine Canon World (world content, lore, configurations,
documentation, and other original material authored for this project) are licensed
under the Creative Commons Attribution-ShareAlike 4.0 International License
(CC BY-SA 4.0). See the `LICENSE` file for the full license text.

The authorization above and this license apply only to original project
contributions. They are not a blanket license for third-party materials; each
third-party mod, JAR, library, resource pack, and other asset remains subject to
its own license.

Minecraft and the shows referenced by this project belong to their respective
rights holders. Mine Canon World is an unofficial fan project and is not
affiliated with or endorsed by Mojang, Microsoft, or those rights holders.
