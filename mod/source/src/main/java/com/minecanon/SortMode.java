package com.minecanon;

/** Ways to order the mod list. */
public enum SortMode {
    LOAD_PLAN("Load plan"),
    NAME("Name"),
    CATEGORY("Category"),
    DEPENDENTS("Most depended on"),
    SIZE("File size");

    private final String label;

    SortMode(String label) { this.label = label; }

    public String label() { return label; }
}
