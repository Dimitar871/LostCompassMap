package com.lostcompass.mapmod.client;

import com.lostcompass.mapmod.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class MapRenderer {
    private final TileManager tileManager;
    private final MarkerManager markerManager;
    private final ModConfig config;
    private boolean minimapEnabled = true;
    private int currentZoom;

    public MapRenderer(TileManager tileManager, MarkerManager markerManager, ModConfig config) {
        this.tileManager = tileManager;
        this.markerManager = markerManager;
        this.config = config;
        this.currentZoom = config.getDefaultZoom();
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!minimapEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        if (client.currentScreen != null) {
            return;
        }

        renderMinimap(context, client.player);
    }

    private void renderMinimap(DrawContext context, PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();

        int mapSize = config.getMinimapSize();
        int margin = config.getMinimapMargin();
        int mapX = screenWidth - mapSize - margin;
        int mapY = margin;

        double playerX = player.getX();
        double playerZ = player.getZ();

        context.fill(mapX - 2, mapY - 2, mapX + mapSize + 2, mapY + mapSize + 2, 0xFF000000);
        context.fill(mapX, mapY, mapX + mapSize, mapY + mapSize, 0xFF333333);

        tileManager.preloadTilesAround(playerX, playerZ, currentZoom, 2);

        int tileSize = config.getTileSize();
        int blocksPerTile = config.getBlocksPerTile(currentZoom);
        double pixelsPerBlock = (double) tileSize / blocksPerTile;

        TileManager.TileKey centerTile = tileManager.worldToTile(playerX, playerZ, currentZoom);

        double tileOffsetX = (playerX - centerTile.x() * blocksPerTile) * pixelsPerBlock;
        double tileOffsetY = (playerZ - centerTile.y() * blocksPerTile) * pixelsPerBlock;

        int tilesNeeded = (int) Math.ceil((double) mapSize / tileSize) + 2;
        int halfTiles = tilesNeeded / 2;

        for (int dx = -halfTiles; dx <= halfTiles; dx++) {
            for (int dy = -halfTiles; dy <= halfTiles; dy++) {
                TileManager.TileKey tileKey = new TileManager.TileKey(
                        currentZoom,
                        centerTile.x() + dx,
                        centerTile.y() + dy
                );

                TileManager.CachedTile tile = tileManager.getTile(tileKey);

                int tileScreenX = (int) (mapX + mapSize / 2.0 - tileOffsetX + dx * tileSize);
                int tileScreenY = (int) (mapY + mapSize / 2.0 - tileOffsetY + dy * tileSize);

                if (tileScreenX + tileSize < mapX || tileScreenX > mapX + mapSize ||
                        tileScreenY + tileSize < mapY || tileScreenY > mapY + mapSize) {
                    continue;
                }

                if (tile != null && tile.isValid()) {
                    int drawX = tileScreenX;
                    int drawY = tileScreenY;
                    int drawWidth = tileSize;
                    int drawHeight = tileSize;
                    int u = 0;
                    int v = 0;

                    if (drawX < mapX) {
                        int clip = mapX - drawX;
                        u = clip;
                        drawWidth -= clip;
                        drawX = mapX;
                    }
                    if (drawY < mapY) {
                        int clip = mapY - drawY;
                        v = clip;
                        drawHeight -= clip;
                        drawY = mapY;
                    }
                    if (drawX + drawWidth > mapX + mapSize) {
                        int clip = (drawX + drawWidth) - (mapX + mapSize);
                        drawWidth -= clip;
                    }
                    if (drawY + drawHeight > mapY + mapSize) {
                        int clip = (drawY + drawHeight) - (mapY + mapSize);
                        drawHeight -= clip;
                    }

                    if (drawWidth > 0 && drawHeight > 0) {
                        context.drawTexture(RenderPipelines.GUI_TEXTURED, tile.getTextureId(),
                                drawX, drawY, u, v,
                                drawWidth, drawHeight,
                                tileSize, tileSize);
                    }
                }
            }
        }

        // Draw town markers (use same scale as tiles)
        for (MarkerManager.TownMarker town : markerManager.getTowns()) {
            int townScreenX = (int) (mapX + mapSize / 2.0 + (town.x() - playerX) * pixelsPerBlock);
            int townScreenY = (int) (mapY + mapSize / 2.0 + (town.z() - playerZ) * pixelsPerBlock);

            if (townScreenX >= mapX && townScreenX <= mapX + mapSize &&
                    townScreenY >= mapY && townScreenY <= mapY + mapSize) {
                int color = town.type().contains("capital") ? 0xFFFFD700 : 0xFF00FF00;
                int size = town.type().contains("capital") ? 3 : 2;
                context.fill(townScreenX - size, townScreenY - size,
                        townScreenX + size, townScreenY + size, color);
            }
        }

        // Draw other players (use same scale as tiles) - filter out local player
        String localPlayerName = player.getName().getString();
        String localPlayerUuid = player.getUuidAsString();

        for (MarkerManager.PlayerMarker otherPlayer : markerManager.getPlayers()) {
            // Skip the local player
            if (otherPlayer.name().equals(localPlayerName) ||
                (!otherPlayer.uuid().isEmpty() && otherPlayer.uuid().equals(localPlayerUuid))) {
                continue;
            }

            int pScreenX = (int) (mapX + mapSize / 2.0 + (otherPlayer.x() - playerX) * pixelsPerBlock);
            int pScreenY = (int) (mapY + mapSize / 2.0 + (otherPlayer.z() - playerZ) * pixelsPerBlock);

            if (pScreenX >= mapX && pScreenX <= mapX + mapSize &&
                    pScreenY >= mapY && pScreenY <= mapY + mapSize) {
                drawPlayerArrow(context, pScreenX, pScreenY, otherPlayer.yaw(), 3, 0xFF00BFFF);
            }
        }

        // Draw local player marker (on top)
        int playerMarkerX = mapX + mapSize / 2;
        int playerMarkerY = mapY + mapSize / 2;
        drawPlayerArrow(context, playerMarkerX, playerMarkerY, player.getYaw(), 4, 0xFFFF0000);

        // Border
        context.fill(mapX - 2, mapY - 2, mapX + mapSize + 2, mapY - 1, 0xFF555555);
        context.fill(mapX - 2, mapY + mapSize + 1, mapX + mapSize + 2, mapY + mapSize + 2, 0xFF555555);
        context.fill(mapX - 2, mapY - 1, mapX - 1, mapY + mapSize + 1, 0xFF555555);
        context.fill(mapX + mapSize + 1, mapY - 1, mapX + mapSize + 2, mapY + mapSize + 1, 0xFF555555);

        String coords = String.format("X: %d Z: %d", (int) playerX, (int) playerZ);
        context.drawText(MinecraftClient.getInstance().textRenderer, coords,
                mapX + 2, mapY + mapSize + 4, 0xFFFFFFFF, true);

        // Find and display current town (if within 50 blocks of town center)
        MarkerManager.TownMarker nearestTown = null;
        double nearestDistance = 50; // Max distance to be considered "in" a town
        for (MarkerManager.TownMarker town : markerManager.getTowns()) {
            double dx = town.x() - playerX;
            double dz = town.z() - playerZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTown = town;
            }
        }

        if (nearestTown != null) {
            String townText = nearestTown.name();
            if (nearestTown.type().contains("capital")) {
                townText = "\u2605 " + townText; // Star for capital
            }
            int textWidth = client.textRenderer.getWidth(townText);
            context.drawText(client.textRenderer, townText,
                    mapX + (mapSize - textWidth) / 2, mapY + mapSize + 14, 0xFF00FF00, true);
        }
    }

    public void toggleMinimap() {
        minimapEnabled = !minimapEnabled;
    }

    public boolean isMinimapEnabled() {
        return minimapEnabled;
    }

    public void setMinimapEnabled(boolean enabled) {
        minimapEnabled = enabled;
    }

    public int getCurrentZoom() {
        return currentZoom;
    }

    public void setCurrentZoom(int zoom) {
        this.currentZoom = Math.max(0, Math.min(zoom, config.getMaxZoom()));
    }

    public void zoomIn() {
        setCurrentZoom(currentZoom + 1);
    }

    public void zoomOut() {
        setCurrentZoom(currentZoom - 1);
    }

    private static final Identifier ARROW_TEXTURE = Identifier.of("lostcompassmap", "textures/arrow.png");

    private void drawPlayerArrow(DrawContext context, int cx, int cy, float yaw, int size, int color) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(cx, cy);
        matrices.rotate((float) Math.toRadians(yaw));
        int half = size;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE,
                -half, -half, 0, 0,
                half * 2, half * 2,
                16, 16,
                16, 16,
                color);
        matrices.popMatrix();
    }
}
