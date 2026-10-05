package minecanon;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

/** Reads and writes the minecanon-state.json bridge plus the desktop launcher's
 *  launcher.properties, so the in-game UI and the desktop launcher share one selection. */
public final class CanonState {
    public static final String[] IDS = {"first-breath", "nocturne-west", "chakra-nations", "nen-new-world", "curse-modernity", "dark-continent"};
    public static final String[] NAMES = {"First Breath", "Nocturne West", "Chakra Nations", "Nen New World", "Curse Modernity", "Dark Continent"};
    public static final String[] LORE = {
        "Breath, bending and the first disciplines of the Canon Field.",
        "Bloodlines, sacred relics and the moonlit western kingdoms.",
        "Seals, summons and the rise of the clan-state civilizations.",
        "Aura, contracts and the age of licensed hunters.",
        "Domains, binding vows and the hidden urban world.",
        "Forbidden archaeology beyond the boundaries of known canon."};

    public final Path gameDir;
    public String ageId = "", ageName = "", lore = "", library = "";
    public int ageIndex = -1;

    private CanonState(Path gameDir) { this.gameDir = gameDir; }

    public static CanonState load(Path gameDir) {
        CanonState s = new CanonState(gameDir);
        Path file = gameDir.resolve("minecanon-state.json");
        if (Files.isRegularFile(file)) {
            try {
                JsonObject o = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                s.ageId = text(o, "age"); s.ageName = text(o, "ageName"); s.lore = text(o, "lore"); s.library = text(o, "library");
            } catch (Exception ignored) {}
        }
        // Detect the running age from the game directory when possible.
        Path instances = launcherHome().resolve("instances");
        try {
            if (Files.isDirectory(instances)) {
                for (int i = 0; i < IDS.length; i++) {
                    if (Files.isSameFile(instances.resolve(IDS[i]), gameDir)) { s.ageIndex = i; if (s.ageId.isEmpty()) { s.ageId = IDS[i]; s.ageName = NAMES[i]; s.lore = LORE[i]; } break; }
                }
            }
        } catch (IOException ignored) {}
        if (s.ageIndex < 0) for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(s.ageId)) { s.ageIndex = i; break; }
        if (s.library.isEmpty()) {
            for (Path probe = gameDir; probe != null; probe = probe.getParent()) {
                Path candidate = probe.resolve("mods-1.20.1-base");
                if (Files.isDirectory(candidate)) { s.library = candidate.toString(); break; }
            }
        }
        return s;
    }

    public static Path launcherHome() { return Path.of(System.getProperty("user.home"), "MineCanon"); }

    /** Mod library path: bridge file first, then fallbacks. */
    public Path libraryPath() { return library.isEmpty() ? null : Path.of(library); }

    /** The desktop launcher's selected age (launcher.properties), -1 when unreadable. */
    public static int launcherSelected() {
        Properties p = readProperties(launcherHome().resolve("launcher.properties"));
        try { return Math.max(0, Math.min(IDS.length - 1, Integer.parseInt(p.getProperty("selected", "0")))); }
        catch (Exception e) { return -1; }
    }

    /** Updates the desktop launcher's selection and the state bridge. Safe to call in game; the desktop app reads it on next refresh. */
    public static void setAge(int index) throws IOException {
        Path home = launcherHome();
        Files.createDirectories(home);
        Path props = home.resolve("launcher.properties");
        Properties p = readProperties(props);
        p.setProperty("selected", Integer.toString(index));
        try (OutputStream out = Files.newOutputStream(props)) { p.store(out, "Mine Canon - local launcher settings"); }
    }

    /** Records the outcome of an in-game sort back into the state bridge. */
    public void recordSort(int copied, int skipped, int present, int failed) throws IOException {
        Path file = gameDir.resolve("minecanon-state.json");
        JsonObject o = new JsonObject();
        if (ageIndex >= 0) { o.addProperty("age", IDS[ageIndex]); o.addProperty("ageName", NAMES[ageIndex]); o.addProperty("lore", LORE[ageIndex]); }
        else { o.addProperty("age", ageId); o.addProperty("ageName", ageName); }
        o.addProperty("schema", 1);
        if (!library.isEmpty()) o.addProperty("library", library);
        o.addProperty("lastSortCopied", copied); o.addProperty("lastSortSkipped", skipped);
        o.addProperty("lastSortPresent", present); o.addProperty("lastSortFailed", failed);
        o.addProperty("lastSort", Instant.now().toString());
        Files.writeString(file, o.toString(), StandardCharsets.UTF_8);
    }

    static Properties readProperties(Path file) {
        Properties p = new Properties();
        if (Files.isRegularFile(file)) { try (InputStream in = Files.newInputStream(file)) { p.load(in); } catch (IOException ignored) {} }
        return p;
    }

    static String text(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }
}
