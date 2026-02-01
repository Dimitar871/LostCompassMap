package com.lostcompass.mapmod.client;

import com.lostcompass.mapmod.LostCompassMapMod;
import com.lostcompass.mapmod.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class LostCompassMapClient implements ClientModInitializer {
    private static TileManager tileManager;
    private static MapRenderer mapRenderer;
    private static MarkerManager markerManager;
    private static ModConfig config;

    @Override
    public void onInitializeClient() {
        LostCompassMapMod.LOGGER.info("Initializing LostCompass Map Client");

        config = new ModConfig();
        tileManager = new TileManager(config);
        markerManager = new MarkerManager(config);
        mapRenderer = new MapRenderer(tileManager, markerManager, config);

        KeyBindings.register();
        HudRenderCallback.EVENT.register(mapRenderer::render);

        // Set max brightness
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options != null) {
                client.options.getGamma().setValue(5.0);
            }
        });

        LostCompassMapMod.LOGGER.info("LostCompass Map Client initialized");
    }

    public static TileManager getTileManager() {
        return tileManager;
    }

    public static MapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public static MarkerManager getMarkerManager() {
        return markerManager;
    }

    public static ModConfig getConfig() {
        return config;
    }
}
