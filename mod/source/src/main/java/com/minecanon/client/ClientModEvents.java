package com.minecanon.client;

import com.minecanon.MineCanon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Auto-open on the first title screen, plus a key to open the hub any time. */
public final class ClientModEvents {
    private ClientModEvents() {}

    @Mod.EventBusSubscriber(modid = MineCanon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(Keys.OPEN_HUB);
        }
    }

    @Mod.EventBusSubscriber(modid = MineCanon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        private static boolean autoShown = false;

        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (autoShown || !com.minecanon.CanonConfig.AUTO_OPEN.get()) return;
            if (event.getNewScreen() instanceof TitleScreen title) {
                autoShown = true;
                event.setNewScreen(new CanonHubScreen(title));
                MineCanon.LOGGER.info("[Mine Canon] Auto-opened the hub on the title screen.");
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            while (Keys.OPEN_HUB.consumeClick()) {
                if (!(mc.screen instanceof CanonHubScreen)) mc.setScreen(new CanonHubScreen(mc.screen));
            }
        }
    }
}
