package com.lostcompass.mapmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    private static KeyBinding openMapKey;
    private static KeyBinding toggleMinimapKey;
    private static KeyBinding zoomInKey;
    private static KeyBinding zoomOutKey;

    public static void register() {
        openMapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lostcompassmap.open_map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                Category.MISC
        ));

        toggleMinimapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lostcompassmap.toggle_minimap",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                Category.MISC
        ));

        zoomInKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lostcompassmap.zoom_in",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                Category.MISC
        ));

        zoomOutKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lostcompassmap.zoom_out",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(KeyBindings::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        while (openMapKey.wasPressed()) {
            openFullMap(client);
        }

        while (toggleMinimapKey.wasPressed()) {
            toggleMinimap();
        }

        while (zoomInKey.wasPressed()) {
            zoomIn();
        }

        while (zoomOutKey.wasPressed()) {
            zoomOut();
        }
    }

    private static void openFullMap(MinecraftClient client) {
        TileManager tileManager = LostCompassMapClient.getTileManager();
        if (tileManager != null) {
            client.setScreen(new MapScreen(tileManager, LostCompassMapClient.getConfig()));
        }
    }

    private static void toggleMinimap() {
        MapRenderer renderer = LostCompassMapClient.getMapRenderer();
        if (renderer != null) {
            renderer.toggleMinimap();
        }
    }

    private static void zoomIn() {
        MapRenderer renderer = LostCompassMapClient.getMapRenderer();
        if (renderer != null) {
            renderer.zoomIn();
        }
    }

    private static void zoomOut() {
        MapRenderer renderer = LostCompassMapClient.getMapRenderer();
        if (renderer != null) {
            renderer.zoomOut();
        }
    }
}
