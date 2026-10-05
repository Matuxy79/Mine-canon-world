package minecanon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** In-game twin of the desktop ModScanner: reads JAR metadata without loading mod code. */
public final class ModScanner {
    public static final String TARGET_MC = "1.20.1";

    public enum Status { COMPATIBLE, WRONG_LOADER, WRONG_VERSION, UNKNOWN }

    public record ModInfo(Path file, String modId, String name, String version, String loader, String mcRange, Status status, String detail) {}

    private ModScanner() {}

    public static List<ModInfo> scanFolder(Path dir) {
        List<ModInfo> out = new ArrayList<>();
        if (dir == null || !Files.isDirectory(dir)) return out;
        try (var s = Files.list(dir)) {
            for (Path f : s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase())).toList()) {
                out.add(scan(f));
            }
        } catch (IOException ignored) {}
        return out;
    }

    public static ModInfo scan(Path jar) {
        String modId = "", name = "", version = "", loader = "unknown", mcRange = "";
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry toml = zip.getEntry("META-INF/mods.toml");
            if (toml != null) {
                loader = "forge";
                String[] p = parseModsToml(new String(zip.getInputStream(toml).readAllBytes(), StandardCharsets.UTF_8));
                modId = p[0]; version = p[1]; name = p[2]; mcRange = p[3];
            } else {
                ZipEntry fabric = zip.getEntry("fabric.mod.json");
                if (fabric != null) {
                    loader = "fabric";
                    JsonObject o = JsonParser.parseString(new String(zip.getInputStream(fabric).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                    modId = text(o, "id"); version = text(o, "version"); name = text(o, "name");
                    if (name.isEmpty()) name = modId;
                    if (o.has("depends") && o.get("depends").isJsonObject()) {
                        JsonElement mc = o.getAsJsonObject("depends").get("minecraft");
                        if (mc != null && mc.isJsonPrimitive()) mcRange = mc.getAsString();
                        else if (mc != null && mc.isJsonArray() && !mc.getAsJsonArray().isEmpty()) mcRange = mc.getAsJsonArray().get(0).getAsString();
                    }
                } else {
                    ZipEntry legacy = zip.getEntry("mcmod.info");
                    if (legacy != null) {
                        loader = "forge";
                        JsonArray a = JsonParser.parseString(new String(zip.getInputStream(legacy).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonArray();
                        if (!a.isEmpty() && a.get(0).isJsonObject()) {
                            JsonObject o = a.get(0).getAsJsonObject();
                            modId = text(o, "modid"); name = text(o, "name"); version = text(o, "version"); mcRange = text(o, "mcversion");
                        }
                    }
                }
            }
        } catch (Exception e) {
            return new ModInfo(jar, "", "", "", "unknown", "", Status.UNKNOWN, "could not read JAR");
        }
        if (name.isEmpty()) name = jar.getFileName().toString();
        Status status; String detail;
        if (loader.equals("fabric")) { status = Status.WRONG_LOADER; detail = "Fabric mod; this instance runs Forge."; }
        else if (!loader.equals("forge")) { status = Status.UNKNOWN; detail = "No mod metadata (plugin or unknown format)."; }
        else if (mcRange.isEmpty()) { status = Status.COMPATIBLE; detail = "Forge mod; no MC range declared."; }
        else if (versionMatches(mcRange, TARGET_MC)) { status = Status.COMPATIBLE; detail = "Forge mod for MC " + TARGET_MC + "."; }
        else { status = Status.WRONG_VERSION; detail = "Declares " + mcRange + ", not " + TARGET_MC + "."; }
        return new ModInfo(jar, modId, name, version, loader, mcRange, status, detail);
    }

    static String[] parseModsToml(String toml) {
        String modId = "", version = "", name = "", range = "";
        boolean inMods = false, captureRange = false, depForMc = false;
        for (String raw : toml.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("[[")) {
                String sec = line.substring(2, line.lastIndexOf("]]")).trim();
                inMods = sec.equals("mods"); captureRange = sec.startsWith("dependencies.");
                if (captureRange) depForMc = false;
                continue;
            }
            if (line.startsWith("[")) { inMods = false; captureRange = false; continue; }
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String key = line.substring(0, eq).trim(), value = line.substring(eq + 1).trim();
            if (value.length() >= 2 && value.charAt(0) == '"') value = value.substring(1, value.indexOf('"', 1));
            if (captureRange) {
                if (key.equals("modId")) depForMc = value.equals("minecraft");
                else if (key.equals("versionRange") && depForMc && range.isEmpty()) range = value;
            } else if (inMods) {
                if (key.equals("modId") && modId.isEmpty()) modId = value;
                else if (key.equals("version") && version.isEmpty()) version = value;
                else if (key.equals("displayName") && name.isEmpty()) name = value;
            }
        }
        return new String[]{modId, version, name, range};
    }

    static String text(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    public static boolean versionMatches(String range, String target) {
        range = range.trim();
        if (range.isEmpty()) return true;
        for (String clause : range.split("\\s+")) if (matchesClause(clause, target)) return true;
        return false;
    }

    static boolean matchesClause(String clause, String target) {
        if (!clause.contains(",")) {
            String v = clause;
            if ((v.startsWith("[") || v.startsWith("(")) && v.length() > 2) v = v.substring(1, v.length() - 1);
            return cmp(v, target) == 0;
        }
        boolean lowerInclusive = clause.startsWith("["), upperInclusive = clause.endsWith("]");
        String inner = clause.substring(1, clause.length() - 1);
        String[] parts = inner.split(",", 2);
        String lower = parts[0].trim(), upper = parts.length > 1 ? parts[1].trim() : "";
        if (!lower.isEmpty() && ((lowerInclusive && cmp(lower, target) > 0) || (!lowerInclusive && cmp(lower, target) >= 0))) return false;
        if (!upper.isEmpty() && ((upperInclusive && cmp(upper, target) < 0) || (!upperInclusive && cmp(upper, target) <= 0))) return false;
        return true;
    }

    static int cmp(String a, String b) {
        String[] x = a.split("[-._]"), y = b.split("[-._]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            String p = i < x.length ? x[i] : "0", q = i < y.length ? y[i] : "0";
            int c;
            try { c = Integer.compare(Integer.parseInt(p), Integer.parseInt(q)); }
            catch (NumberFormatException e) { c = p.compareTo(q); }
            if (c != 0) return c;
        }
        return 0;
    }
}
