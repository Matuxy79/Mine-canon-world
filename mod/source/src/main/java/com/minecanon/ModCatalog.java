package com.minecanon;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Builds sorted views of the loaded mod list. Pure logic lives in {@link #sort}; {@link #collect()} reads Forge. */
public final class ModCatalog {
    private ModCatalog() {}

    /** Reads every loaded mod from Forge and resolves dependency edges and categories. */
    public static List<ModRecord> collect() {
        List<ModRecord> out = new ArrayList<>();
        Map<String, ModRecord> byId = new HashMap<>();
        Map<String, List<IModInfo.ModVersion>> deps = new HashMap<>();
        for (IModInfo info : ModList.get().getMods()) {
            long size = 0;
            String file = "";
            IModFileInfo fi = info.getOwningFile();
            if (fi != null && fi.getFile() != null && fi.getFile().getFilePath() != null) {
                Path p = fi.getFile().getFilePath();
                file = p.getFileName() == null ? p.toString() : p.getFileName().toString();
                try { if (Files.isRegularFile(p)) size = Files.size(p); } catch (IOException ignored) { }
            }
            ModRecord r = new ModRecord(info.getModId(), info.getDisplayName(), String.valueOf(info.getVersion()), info.getDescription(), size, file);
            out.add(r);
            byId.put(r.id, r);
            deps.put(r.id, new ArrayList<>(info.getDependencies()));
        }
        for (ModRecord r : out) {
            for (IModInfo.ModVersion d : deps.get(r.id)) {
                ModRecord target = byId.get(d.getModId());
                if (target == null || target == r) continue;
                if (d.isMandatory()) { r.requires.add(target.id); target.dependents.add(r.id); }
                else r.optional.add(target.id);
            }
        }
        for (ModRecord r : out) r.category = ModCategory.classify(r.id, r.name, r.dependents.size());
        plan(out);
        return out;
    }

    /** Dependency-first order (Kahn), ties broken by category then name. Cycles are appended at the end. Sets planIndex. */
    public static void plan(List<ModRecord> mods) {
        Map<String, ModRecord> byId = new HashMap<>();
        for (ModRecord m : mods) byId.put(m.id, m);
        Map<String, Integer> indeg = new HashMap<>();
        for (ModRecord m : mods) indeg.put(m.id, (int) m.requires.stream().filter(byId::containsKey).distinct().count());
        Comparator<ModRecord> tie = Comparator.comparingInt((ModRecord m) -> m.category.ordinal())
                .thenComparing(m -> m.name.toLowerCase(Locale.ROOT)).thenComparing(m -> m.id);
        PriorityQueue<ModRecord> ready = new PriorityQueue<>(tie);
        for (ModRecord m : mods) if (indeg.get(m.id) == 0) ready.add(m);
        int i = 0;
        Set<String> done = new HashSet<>();
        while (!ready.isEmpty()) {
            ModRecord m = ready.poll();
            m.planIndex = i++;
            done.add(m.id);
            for (String child : new LinkedHashSet<>(m.dependents)) {
                ModRecord c = byId.get(child);
                if (c == null || done.contains(c.id)) continue;
                int d = indeg.merge(c.id, -1, Integer::sum);
                if (d == 0) ready.add(c);
            }
        }
        List<ModRecord> rest = new ArrayList<>();
        for (ModRecord m : mods) if (m.planIndex < 0) rest.add(m);
        rest.sort(tie);
        for (ModRecord m : rest) m.planIndex = i++;
    }

    /** Returns a new list ordered by the requested mode. */
    public static List<ModRecord> sort(Collection<ModRecord> mods, SortMode mode) {
        List<ModRecord> l = new ArrayList<>(mods);
        Comparator<ModRecord> byName = Comparator.comparing((ModRecord m) -> m.name.toLowerCase(Locale.ROOT)).thenComparing(m -> m.id);
        switch (mode) {
            case NAME -> l.sort(byName);
            case CATEGORY -> l.sort(Comparator.comparingInt((ModRecord m) -> m.category.ordinal()).thenComparing(byName));
            case DEPENDENTS -> l.sort(Comparator.comparingInt((ModRecord m) -> -m.dependents.size()).thenComparing(byName));
            case SIZE -> l.sort(Comparator.comparingLong((ModRecord m) -> -m.sizeBytes).thenComparing(byName));
            case LOAD_PLAN -> l.sort(Comparator.comparingInt((ModRecord m) -> m.planIndex));
        }
        return l;
    }

    public static String humanSize(long b) {
        if (b <= 0) return "-";
        if (b < 1024 * 1024) return String.format("%.0f KB", b / 1024.0);
        return String.format("%.1f MB", b / 1048576.0);
    }
}
