package minecanon;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Entry point: registers the O keybind that opens the in-game MineCanon UI. */
@Mod(MineCanonMod.ID)
public class MineCanonMod {
    public static final String ID = "minecanon";
    public static KeyMapping openUi;

    public MineCanonMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventBusSubscriber(modid = ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            MineCanonMod.openUi = new KeyMapping("key.minecanon.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.minecanon");
            event.register(MineCanonMod.openUi);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (openUi != null && mc.screen == null && mc.player != null) {
            while (openUi.consumeClick()) {
                mc.setScreen(new CanonScreen());
            }
        }
    }
}
