package com.minecanon;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/** Mine Canon - opens the canon hub on launch and sorts the mod list by category and dependency order. */
@Mod(MineCanon.MODID)
public class MineCanon {
    public static final String MODID = "minecanon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MineCanon() {
        // Client-side quality-of-life mod: never block a connection to a server that lacks it.
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (remote, isServer) -> true));
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CanonConfig.SPEC);
        LOGGER.info("[Mine Canon] The Canon Field stirs (age {}).", CanonAge.current().label());
    }
}
