package com.minecanon.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Client key bindings (loaded on the client only). */
public final class Keys {
    public static final KeyMapping OPEN_HUB = new KeyMapping("key.minecanon.open", KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.minecanon");

    private Keys() {}
}
