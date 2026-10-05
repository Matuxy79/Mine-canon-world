package com.minecanon.client;

import com.minecanon.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** The Mine Canon hub: age banner, sortable mod list with categories and dependency details. */
public class CanonHubScreen extends Screen {
    private static final int OBSIDIAN = 0xFF0C1115, PANEL = 0xFF12191D, BORDER = 0xFF293239, IVORY = 0xFFF6F1E6,
            MUTED = 0xFFA4A7AD, EMBER = 0xFFF57C2C, SAGE = 0xFFA8CF8A;

    private final Screen parent;
    private final List<ModRecord> all = ModCatalog.collect();
    private final CanonAge age = CanonAge.current();
    private SortMode sort = CanonConfig.DEFAULT_SORT.get();
    private ModCategory filter = null;                // null = all
    private String query = "";
    private ModListWidget list;
    private EditBox search;
    private Button autoButton, exportButton;
    private String status = "";
    private int leftW;

    public CanonHubScreen(Screen parent) {
        super(Component.literal("Mine Canon"));
        this.parent = parent;
    }

    private boolean compact() { return this.height < 330; }
    private int headerH() { return compact() ? 46 : 88; }
    private int ctrlY() { return headerH() + 4; }
    private int listTop() { return ctrlY() + 26; }
    private int bottomY() { return this.height - 26; }
    private int listBottom() { return bottomY() - 20; }

    @Override
    protected void init() {
        int m = 16;
        leftW = (int) (this.width * 0.56);
        int total = this.width - 2 * m - 8;
        int sw = (int) (total * 0.30), cw = (int) (total * 0.35), kw = total - sw - cw;
        int y = ctrlY();
        search = new EditBox(this.font, m, y, sw, 20, Component.literal("Search"));
        search.setHint(Component.literal("Search mods...").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(query);
        search.setResponder(s -> { query = s; refilter(); });
        addRenderableWidget(search);
        addRenderableWidget(CycleButton.<SortMode>builder(s -> Component.literal(s.label()))
                .withValues(SortMode.values()).withInitialValue(sort)
                .create(m + sw + 4, y, cw, 20, Component.literal("Sort"), (b, v) -> { sort = v; refilter(); }));
        List<Object> cats = new ArrayList<>();
        cats.add("All");
        cats.addAll(Arrays.asList(ModCategory.values()));
        addRenderableWidget(CycleButton.<Object>builder(o -> Component.literal(o instanceof ModCategory c ? c.label() : "All"))
                .withValues(cats).withInitialValue(filter == null ? "All" : filter)
                .create(m + sw + cw + 8, y, kw, 20, Component.literal("Show"),
                        (b, v) -> { filter = v instanceof ModCategory c ? c : null; refilter(); }));
        // list
        list = new ModListWidget(this.minecraft, leftW - m, this.height, listTop(), listBottom(), 22);
        list.setLeftPos(m);
        addWidget(list);
        refilter();
        // bottom bar
        int by = bottomY();
        autoButton = addRenderableWidget(Button.builder(autoLabel(), b -> {
            CanonConfig.AUTO_OPEN.set(!CanonConfig.AUTO_OPEN.get());
            CanonConfig.AUTO_OPEN.save();
            b.setMessage(autoLabel());
        }).bounds(m, by, 104, 20).build());
        exportButton = addRenderableWidget(Button.builder(Component.literal("Export plan"), b -> exportPlan())
                .bounds(m + 108, by, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("ENTER THE WORLD"), b -> onClose())
                .bounds(this.width - m - 150, by, 150, 20).build());
    }

    private Component autoLabel() {
        return Component.literal("Auto-open: " + (CanonConfig.AUTO_OPEN.get() ? "ON" : "OFF"));
    }

    private void refilter() {
        if (list == null) return;
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<ModRecord> sel = new ArrayList<>();
        for (ModRecord r : all) {
            if (filter != null && r.category != filter) continue;
            if (!q.isEmpty() && !(r.name.toLowerCase(Locale.ROOT).contains(q) || r.id.contains(q))) continue;
            sel.add(r);
        }
        ModRecord keep = list.getSelected() == null ? null : list.getSelected().rec;
        list.setEntries(ModCatalog.sort(sel, sort), keep);
    }

    private void exportPlan() {
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("minecanon");
            Files.createDirectories(dir);
            Path f = dir.resolve("load-plan.txt");
            StringBuilder sb = new StringBuilder("# Mine Canon load plan - " + age.label() + "\n# dependencies first\n");
            for (ModRecord r : ModCatalog.sort(all, SortMode.LOAD_PLAN))
                sb.append(String.format("%03d  [%-12s] %s (%s) v%s  requires: %s%n", r.planIndex + 1, r.category.label(), r.name, r.id, r.version,
                        r.requires.isEmpty() ? "-" : String.join(", ", r.requires)));
            Files.writeString(f, sb.toString());
            status = "Wrote " + f.getFileName() + " (" + all.size() + " mods)";
        } catch (IOException e) {
            status = "Export failed: " + e.getMessage();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------ rendering
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int m = 16;
        boolean c = compact();
        g.fill(0, 0, this.width, this.height, OBSIDIAN);
        g.fillGradient(0, 0, this.width, headerH(), 0xFF1B2128, OBSIDIAN);
        g.fill(0, 0, this.width, 2, EMBER);
        // header
        float ts = c ? 2f : 2.5f;
        g.pose().pushPose();
        g.pose().translate(m, c ? 8 : 14, 0);
        g.pose().scale(ts, ts, 1f);
        g.drawString(this.font, "MINE CANON", 0, 0, IVORY, false);
        g.pose().popPose();
        if (!c) g.drawString(this.font, "Your world. Your canon.", m + 2, 44, MUTED, false);
        String ver = "Minecraft 1.20.1 \u00B7 Forge " + forgeVersion() + " \u00B7 " + all.size() + " mods";
        g.drawString(this.font, ver, this.width - m - this.font.width(ver), c ? 8 : 18, SAGE, false);
        String ageLine = age.label();
        g.drawString(this.font, ageLine, this.width - m - this.font.width(ageLine), c ? 20 : 32, EMBER, false);
        String ceil = (age.ceiling() == 0 ? "Era ceiling: undefined" : "Era ceiling: " + age.ceiling() + " BST") + (c ? "" : "");
        g.drawString(this.font, ceil, this.width - m - this.font.width(ceil), c ? 32 : 46, MUTED, false);
        if (!c) g.drawString(this.font, age.dialect(), this.width - m - this.font.width(age.dialect()), 60, MUTED, false);

        // list frame
        int lt = listTop(), lb = listBottom();
        g.fill(m, lt, leftW, lb, PANEL);
        g.renderOutline(m, lt, leftW - m, lb - lt, BORDER);
        list.render(g, mx, my, pt);

        // details panel
        int dx = leftW + 8, dw = this.width - dx - m, dy = lt, dh = lb - lt;
        g.fill(dx, dy, dx + dw, dy + dh, PANEL);
        g.renderOutline(dx, dy, dw, dh, BORDER);
        g.enableScissor(dx + 1, dy + 1, dx + dw - 1, dy + dh - 1);
        renderDetails(g, dx + 8, dy + 8, dw - 16, dy + dh - 4);
        g.disableScissor();

        super.render(g, mx, my, pt);
        String cnt = list.children().size() + " shown \u00B7 " + sort.label();
        g.drawString(this.font, status.isEmpty() ? cnt : status, m, lb + 5, status.isEmpty() ? MUTED : SAGE, false);
    }

    private String forgeVersion() {
        return ModList.get().getModContainerById("forge").map(c -> c.getModInfo().getVersion().toString()).orElse("?");
    }

    private void renderDetails(GuiGraphics g, int x, int y, int w, int maxY) {
        ModListWidget.Row sel = list.getSelected();
        if (sel == null) {
            g.drawString(this.font, "Select a mod", x, y, IVORY, false);
            y += 16;
            for (FormattedCharSequence s : this.font.split(Component.literal(
                    "LOAD PLAN lists every dependency before the mods that need it. Click a row for requirements and dependents."), w)) {
                if (y > maxY - 10) break;
                g.drawString(this.font, s, x, y, MUTED, false);
                y += 11;
            }
            y += 6;
            Map<ModCategory, Integer> counts = new EnumMap<>(ModCategory.class);
            for (ModRecord r : all) counts.merge(r.category, 1, Integer::sum);
            for (ModCategory c : ModCategory.values()) {
                if (y > maxY - 10) break;
                int n = counts.getOrDefault(c, 0);
                g.fill(x, y + 1, x + 6, y + 9, c.color());
                g.drawString(this.font, c.label() + "  " + n, x + 12, y, IVORY, false);
                int bar = all.isEmpty() ? 0 : (int) ((w - 110) * (n / (double) all.size()));
                g.fill(x + 100, y + 2, x + 100 + Math.max(1, bar), y + 8, c.color());
                y += 14;
            }
            return;
        }
        ModRecord r = sel.rec;
        g.drawString(this.font, r.name, x, y, IVORY, false);
        y += 12;
        g.drawString(this.font, r.id + " · v" + r.version, x, y, MUTED, false);
        y += 14;
        g.fill(x, y, x + 6, y + 8, r.category.color());
        g.drawString(this.font, r.category.label() + "  ·  plan #" + (r.planIndex + 1) + "  ·  " + ModCatalog.humanSize(r.sizeBytes), x + 12, y, r.category.color(), false);
        y += 16;
        y = block(g, "Requires", r.requires, x, y, w, maxY);
        y = block(g, "Optional", r.optional, x, y, w, maxY);
        y = block(g, "Needed by", r.dependents, x, y, w, maxY);
        if (!r.fileName.isEmpty()) {
            g.drawString(this.font, "File: " + this.font.plainSubstrByWidth(r.fileName, w - 30), x, y, MUTED, false);
            y += 14;
        }
        for (FormattedCharSequence s : this.font.split(Component.literal(r.description), w)) {
            if (y > maxY - 10) break;
            g.drawString(this.font, s, x, y, 0xFFC9CCD1, false);
            y += 11;
        }
    }

    private int block(GuiGraphics g, String title, List<String> ids, int x, int y, int w, int maxY) {
        if (ids.isEmpty() || y > maxY - 10) return y;
        g.drawString(this.font, title + " (" + ids.size() + ")", x, y, EMBER, false);
        y += 11;
        for (FormattedCharSequence s : this.font.split(Component.literal(String.join(", ", ids)), w)) {
            if (y > maxY - 10) break;
            g.drawString(this.font, s, x, y, IVORY, false);
            y += 11;
        }
        return y + 5;
    }

    // ------------------------------------------------------------------ list widget
    private class ModListWidget extends ObjectSelectionList<ModListWidget.Row> {
        ModListWidget(Minecraft mc, int width, int height, int y0, int y1, int itemHeight) {
            super(mc, width, height, y0, y1, itemHeight);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        void setEntries(List<ModRecord> recs, ModRecord keep) {
            clearEntries();
            Row again = null;
            for (ModRecord r : recs) {
                Row row = new Row(r);
                addEntry(row);
                if (r == keep) again = row;
            }
            setSelected(again);
            setScrollAmount(0);
        }

        @Override
        public int getRowWidth() { return this.width - 16; }

        @Override
        protected int getScrollbarPosition() { return this.x0 + this.width - 6; }

        class Row extends ObjectSelectionList.Entry<Row> {
            final ModRecord rec;

            Row(ModRecord rec) { this.rec = rec; }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mx, int my, boolean hover, float pt) {
                if (hover || getSelected() == this) g.fill(left, top, left + width, top + height - 2, hover ? 0xFF1B242A : 0xFF2A1E14);
                g.fill(left, top, left + 3, top + height - 2, rec.category.color());
                g.drawString(font, font.plainSubstrByWidth(rec.name, width - 18 - font.width(rec.category.label())), left + 9, top + 2, IVORY, false);
                g.drawString(font, "v" + font.plainSubstrByWidth(rec.version, 60), left + 9, top + 12, MUTED, false);
                String tag = rec.category.label();
                g.drawString(font, tag, left + width - font.width(tag) - 6, top + 2, rec.category.color(), false);
                String right = "#" + (rec.planIndex + 1) + (rec.dependents.isEmpty() ? "" : "  ←" + rec.dependents.size());
                g.drawString(font, right, left + width - font.width(right) - 6, top + 12, MUTED, false);
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                setSelected(this);
                return true;
            }

            @Override
            public Component getNarration() { return Component.literal(rec.name); }
        }
    }
}
