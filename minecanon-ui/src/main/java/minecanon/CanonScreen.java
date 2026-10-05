package minecanon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The in-game MineCanon UI: canon age, live mod audit, and per-age auto-sort.
 *  Open with the O key. File changes apply the next time the game starts. */
public class CanonScreen extends Screen {
    private static final int BG = 0xF50C1115, PANEL = 0xF512191D, TEXT = 0xFFF6F1E6, MUTED = 0xFFA4A7AD, ACCENT = 0xFFF57C2C, GREEN = 0xFFA8CF8A;
    private CanonState state;
    private final List<ModScanner.ModInfo> instanceMods = new ArrayList<>();
    private final List<ModScanner.ModInfo> libraryMods = new ArrayList<>();
    private String message = "";
    private int scroll;

    public CanonScreen() {
        super(Component.literal("Mine Canon"));
    }

    @Override
    protected void init() {
        Path gameDir = this.minecraft.gameDirectory.toPath();
        this.state = CanonState.load(gameDir);
        instanceMods.clear(); instanceMods.addAll(ModScanner.scanFolder(gameDir.resolve("mods")));
        libraryMods.clear(); libraryMods.addAll(ModScanner.scanFolder(state.libraryPath()));
        long compatible = libraryMods.stream().filter(m -> m.status() == ModScanner.Status.COMPATIBLE).count();
        int w = this.width, y = this.height - 66, bw = Math.min(300, (w - 60) / 3), x = 20;
        this.addRenderableWidget(Button.builder(Component.literal("Sort mods for " + ageLabel() + " (next launch)"), b -> sortCurrent()).bounds(x, y, bw, 20).build());
        x += bw + 10;
        this.addRenderableWidget(Button.builder(Component.literal("Sort every other age"), b -> sortOtherAges()).bounds(x, y, bw, 20).build());
        x += bw + 10;
        this.addRenderableWidget(Button.builder(Component.literal("Launcher age: " + CanonState.NAMES[Math.max(0, CanonState.launcherSelected())] + " (switch)"), b -> switchLauncherAge()).bounds(x, y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(w - 90, y + 26, 70, 20).build());
        if (!libraryMods.isEmpty())
            message = "Library: " + compatible + " of " + libraryMods.size() + " mods are compatible with MC 1.20.1 / Forge 47.4.26.";
    }

    private String ageLabel() { return state.ageIndex >= 0 ? CanonState.NAMES[state.ageIndex] : (state.ageName.isEmpty() ? "this world" : state.ageName); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, BG);
        super.render(g, mouseX, mouseY, partialTick);
        int y = 20;
        g.drawString(this.font, "MINE CANON", 22, y, ACCENT); y += 16;
        g.drawString(this.font, "Age: " + ageLabel() + "   |   MC 1.20.1 / Forge 47.4.26", 22, y, TEXT); y += 14;
        if (!state.lore.isEmpty()) { g.drawString(this.font, state.lore, 22, y, MUTED); y += 14; }
        if (!message.isEmpty()) { g.drawString(this.font, message, 22, y, ACCENT); y += 14; }
        y += 6;
        g.drawString(this.font, "THIS INSTANCE (" + instanceMods.size() + " mods)", 22, y, GREEN); y += 14;
        int listTop = y, listBottom = this.height - 96, line = 0;
        for (ModScanner.ModInfo m : instanceMods) {
            int ly = listTop + line * 24 - scroll;
            if (ly >= listTop - 24 && ly < listBottom) {
                g.fill(20, ly - 2, this.width - 20, ly + 20, PANEL);
                g.drawString(this.font, trim(m.name() + " " + m.version(), this.width / Math.max(1, this.font.width("M")) - 22), 24, ly + 2, statusColor(m));
                g.drawString(this.font, m.loader() + (m.mcRange().isEmpty() ? "" : " " + m.mcRange()) + " - " + m.detail(), 24, ly + 12, MUTED);
            }
            line++;
        }
        if (instanceMods.isEmpty()) g.drawString(this.font, "No mods installed in this instance yet.", 24, listTop, MUTED);
    }

    private int statusColor(ModScanner.ModInfo m) {
        return switch (m.status()) { case COMPATIBLE -> GREEN; case UNKNOWN -> MUTED; default -> ACCENT; };
    }

    private String trim(String s, int maxChars) { return s.length() <= maxChars ? s : s.substring(0, Math.max(1, maxChars - 1)) + "..."; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, instanceMods.size() * 24 - Math.max(24, this.height - 96 - 134));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) delta * 24));
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void sortCurrent() {
        try {
            Path mods = this.minecraft.gameDirectory.toPath().resolve("mods");
            Files.createDirectories(mods);
            int[] r = sortInto(mods);
            state.recordSort(r[0], r[1], r[2], r[3]);
            message = "Copied " + r[0] + " mod(s) into this age. Skipped " + r[1] + " incompatible, " + r[2] + " present, " + r[3] + " failed. Restart Minecraft to load them.";
            instanceMods.clear(); instanceMods.addAll(ModScanner.scanFolder(mods));
        } catch (Exception e) {
            message = "Sort failed: " + e.getMessage() + " (close the game and use the desktop launcher).";
        }
    }

    private void sortOtherAges() {
        try {
            Path instances = CanonState.launcherHome().resolve("instances");
            int totalCopied = 0, ages = 0;
            for (int i = 0; i < CanonState.IDS.length; i++) {
                if (state.ageIndex >= 0 && i == state.ageIndex) continue;
                Path mods = instances.resolve(CanonState.IDS[i]).resolve("mods");
                if (!Files.isDirectory(instances.resolve(CanonState.IDS[i]))) continue;
                int[] r = sortInto(mods);
                totalCopied += r[0]; ages++;
            }
            message = "Sorted " + ages + " other age(s); copied " + totalCopied + " mod(s). Changes apply when those worlds next start.";
        } catch (Exception e) {
            message = "Could not sort other ages: " + e.getMessage();
        }
    }

    private int[] sortInto(Path mods) {
        int copied = 0, skipped = 0, present = 0, failed = 0;
        for (ModScanner.ModInfo m : libraryMods) {
            if (m.status() != ModScanner.Status.COMPATIBLE) { skipped++; continue; }
            Path target = mods.resolve(m.file().getFileName());
            if (Files.exists(target)) { present++; continue; }
            try { Files.copy(m.file(), target); copied++; } catch (Exception e) { failed++; }
        }
        return new int[]{copied, skipped, present, failed};
    }

    private void switchLauncherAge() {
        int current = CanonState.launcherSelected();
        if (current < 0) current = 0;
        int next = (current + 1) % CanonState.IDS.length;
        try {
            CanonState.setAge(next);
            message = "Desktop launcher age set to " + CanonState.NAMES[next] + ". Re-prepare profiles from the desktop launcher when the game is closed.";
            clearWidgets(); init();
        } catch (Exception e) {
            message = "Could not update the launcher selection: " + e.getMessage();
        }
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
