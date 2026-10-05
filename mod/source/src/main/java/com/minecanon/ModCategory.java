package com.minecanon;

import java.util.Locale;
import java.util.Set;

/** Canon categories used to group the mod list. Order is the group order. */
public enum ModCategory {
    CORE("Core", 0xFFA4A7AD),
    LIBRARY("Library", 0xFF6FA8DC),
    OPTIMIZATION("Optimization", 0xFFA8CF8A),
    INTERFACE("Interface", 0xFFC9A0FF),
    CONTENT("Content", 0xFFF57C2C),
    CANON("Canon", 0xFFFFD27A);

    private final String label;
    private final int color;

    ModCategory(String label, int color) { this.label = label; this.color = color; }

    public String label() { return label; }
    public int color() { return color; }

    private static final Set<String> LIB_IDS = Set.of("architectury", "cloth_config", "geckolib", "kotlinforforge", "balm",
            "bookshelf", "playeranimator", "creativecore", "supermartijn642corelib", "curios", "collective", "citadel",
            "moonlight", "resourcefullib", "patchouli", "fusion", "puzzleslib", "forgeconfigapiport", "configured_lib");
    private static final Set<String> OPT_IDS = Set.of("embeddium", "rubidium", "oculus", "ferritecore", "modernfix", "canary",
            "starlight", "entityculling", "immediatelyfast", "smoothboot", "memoryleakfix", "saturn", "dynamic_fps",
            "sodiumdynamiclights", "noisium", "badoptimizations", "cull_less_leaves", "lazydfu", "clumps", "fastsuite");
    private static final Set<String> UI_IDS = Set.of("jei", "roughlyenoughitems", "journeymap", "xaerominimap", "xaeroworldmap",
            "jade", "waila", "appleskin", "mousetweaks", "controlling", "configured", "catalogue", "inventoryhud", "emi",
            "jeresources", "betterf3", "invtweaks", "inventoryprofilesnext", "craftingtweaks", "itemzoom");

    /** Classifies a mod from its id, name and how many other mods depend on it. */
    public static ModCategory classify(String id, String displayName, int dependents) {
        String i = id.toLowerCase(Locale.ROOT);
        String n = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        if (i.equals("minecraft") || i.equals("forge")) return CORE;
        if (i.startsWith("minecanon") || i.startsWith("canon")) return CANON;
        if (LIB_IDS.contains(i) || i.endsWith("lib") || i.endsWith("_lib") || i.endsWith("-lib") || i.endsWith("api")
                || n.endsWith(" lib") || n.endsWith(" library") || n.endsWith(" api") || n.contains("core lib")) return LIBRARY;
        if (OPT_IDS.contains(i) || n.contains("optimiz") || n.contains("performance")) return OPTIMIZATION;
        if (UI_IDS.contains(i) || n.contains("minimap") || n.contains("tooltip") || n.contains("inventory")) return INTERFACE;
        if (dependents >= 2 && (i.contains("core") || i.contains("common"))) return LIBRARY;
        return CONTENT;
    }
}
