package net.unfamily.iskautils.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    
    public static final String KEY_CATEGORY = "key.category.iska_utils";
    
    public static final KeyMapping PORTABLE_DISLOCATOR_KEY = new KeyMapping(
            "key.iska_utils.portable_dislocator",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            KEY_CATEGORY
    );
}