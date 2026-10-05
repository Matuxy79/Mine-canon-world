package com.minecanon;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client config: config/minecanon-client.toml */
public final class CanonConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue AUTO_OPEN = B
            .comment("Open the Mine Canon hub the first time the title screen appears in each game session.")
            .define("autoOpen", true);

    public static final ForgeConfigSpec.EnumValue<SortMode> DEFAULT_SORT = B
            .comment("How the hub sorts the mod list when it opens: LOAD_PLAN, NAME, CATEGORY, DEPENDENTS, SIZE.")
            .defineEnum("defaultSort", SortMode.LOAD_PLAN);

    public static final ForgeConfigSpec SPEC = B.build();

    private CanonConfig() {}
}
