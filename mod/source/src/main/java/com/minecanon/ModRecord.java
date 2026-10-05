package com.minecanon;

import java.util.ArrayList;
import java.util.List;

/** One loaded mod, with its dependency edges resolved. */
public final class ModRecord {
    public final String id, name, version, description;
    public final long sizeBytes;
    public final String fileName;
    public final List<String> requires = new ArrayList<>();   // mandatory dependencies present in the loaded set
    public final List<String> optional = new ArrayList<>();
    public final List<String> dependents = new ArrayList<>();
    public ModCategory category = ModCategory.CONTENT;
    public int planIndex = -1;

    public ModRecord(String id, String name, String version, String description, long sizeBytes, String fileName) {
        this.id = id;
        this.name = name == null || name.isBlank() ? id : name;
        this.version = version;
        this.description = description == null ? "" : description.strip();
        this.sizeBytes = sizeBytes;
        this.fileName = fileName;
    }
}
