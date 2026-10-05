package com.minecanon;

/**
 * The age the launcher selected. The Mine Canon launcher puts -Dminecanon.age=N into the profile's
 * JVM arguments; without the launcher the mod falls back to Age 1.
 */
public record CanonAge(String tag, String name, String dialect, int ceiling) {
    private static final CanonAge[] ALL = {
            new CanonAge("0", "Source / Pre-Canon", "Raw Will · Canon Field", 0),
            new CanonAge("1", "First Breath", "Breath · Bending", 540),
            new CanonAge("2", "Nocturne West", "Steel · Blood · Holy · Moon", 570),
            new CanonAge("3", "Chakra Nations", "Chakra · Seals · Summons", 620),
            new CanonAge("4", "Nen New World", "Nen · Aura · Contracts", 660),
            new CanonAge("4+", "Curse Modernity", "Cursed Energy · Domains · Vows", 700),
            new CanonAge("5", "Dark Continent", "Forbidden · Lost Human Skill", 800),
    };

    public static CanonAge current() {
        String t = System.getProperty("minecanon.age", "1").trim();
        for (CanonAge a : ALL) if (a.tag.equalsIgnoreCase(t)) return a;
        return ALL[1];
    }

    public String label() { return "Age " + tag + " · " + name; }
}
