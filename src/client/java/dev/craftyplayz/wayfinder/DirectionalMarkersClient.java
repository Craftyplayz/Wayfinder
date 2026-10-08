package dev.craftyplayz.wayfinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public final class DirectionalMarkersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MarkerManager manager = new MarkerManager(
                FabricLoader.getInstance().getConfigDir().resolve("wayfinder.json"));
        KeyBindings.initialize();
        MarkerCameraController.initialize(manager);
        MarkerHudRenderer hud = new MarkerHudRenderer(manager);
        LevelRenderEvents.END_EXTRACTION.register(hud::captureCamera);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("wayfinder", "markers"), hud::extractRenderState);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.MANAGE.consumeClick()) {
                if (client.player != null && client.level != null && client.screen == null) {
                    client.setScreen(new MarkerManagementScreen(null, manager));
                }
            }
        });
    }
}
