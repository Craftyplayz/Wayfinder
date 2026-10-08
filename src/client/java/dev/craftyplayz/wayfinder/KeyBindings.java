package dev.craftyplayz.wayfinder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("wayfinder", "markers"));

    public static final KeyMapping SHOW = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.wayfinder.show", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
    public static final KeyMapping MANAGE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.wayfinder.manage", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY));

    private KeyBindings() {
    }

    public static void initialize() {
        // Loading this class registers both bindings once.
    }
}
