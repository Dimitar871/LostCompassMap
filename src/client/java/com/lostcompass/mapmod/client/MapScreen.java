package com.lostcompass.mapmod.client;

import com.lostcompass.mapmod.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class MapScreen extends Screen {
    private final TileManager tileManager;
    private final MarkerManager markerManager;
    private final ModConfig config;

    private double viewCenterX;
    private double viewCenterZ;
    private int currentZoom;

    private boolean isDragging = false;
    private double dragStartX;
    private double dragStartY;
    private double dragStartViewX;
    private double dragStartViewZ;

    public MapScreen(TileManager tileManager, ModConfig config) {
        super(Text.literal("LostCompass Map"));
        this.tileManager = tileManager;
        this.markerManager = LostCompassMapClient.getMarkerManager();
        this.config = config;
        this.currentZoom = config.getDefaultZoom();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            this.viewCenterX = client.player.getX();
            this.viewCenterZ = client.player.getZ();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF1a1a1a);

        renderMap(context);
        renderMarkers(context);
        renderUI(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderMap(DrawContext context) {
        int tileSize = config.getTileSize();
        int blocksPerTile = config.getBlocksPerTile(currentZoom);
        double pixelsPerBlock = (double) tileSize / blocksPerTile;
        int serverMaxZoom = config.getServerMaxZoom();
        int serverMinZoom = config.getServerMinZoom();

        // Clamp to server's actual tile range
        int serverZoom = Math.max(serverMinZoom, Math.min(currentZoom, serverMaxZoom));
        int serverBlocksPerTile = config.getBlocksPerTile(serverZoom);
        int displayTileSize = tileSize;

        if (currentZoom > serverMaxZoom) {
            // Extra zoom IN: scale up tiles (2x at zoom 4, 4x at zoom 5)
            int scaleFactor = (int) Math.pow(2, currentZoom - serverMaxZoom);
            displayTileSize = tileSize * scaleFactor;
        } else if (currentZoom < serverMinZoom) {
            // Extra zoom OUT: scale down tiles (1/2 at zoom -1, 1/4 at zoom -2)
            int scaleFactor = (int) Math.pow(2, serverMinZoom - currentZoom);
            displayTileSize = tileSize / scaleFactor;
        }

        int preloadRadius = currentZoom < serverMinZoom ? 5 : 3;
        tileManager.preloadTilesAround(viewCenterX, viewCenterZ, serverZoom, preloadRadius);

        TileManager.TileKey centerTile = tileManager.worldToTile(viewCenterX, viewCenterZ, serverZoom);

        double tileOffsetX = (viewCenterX - centerTile.x() * serverBlocksPerTile) * pixelsPerBlock;
        double tileOffsetY = (viewCenterZ - centerTile.y() * serverBlocksPerTile) * pixelsPerBlock;

        int tilesNeededX = (int) Math.ceil((double) width / displayTileSize) + 2;
        int tilesNeededY = (int) Math.ceil((double) height / displayTileSize) + 2;
        int halfTilesX = tilesNeededX / 2;
        int halfTilesY = tilesNeededY / 2;

        for (int dx = -halfTilesX; dx <= halfTilesX; dx++) {
            for (int dy = -halfTilesY; dy <= halfTilesY; dy++) {
                TileManager.TileKey tileKey = new TileManager.TileKey(
                        serverZoom,
                        centerTile.x() + dx,
                        centerTile.y() + dy
                );

                TileManager.CachedTile tile = tileManager.getTile(tileKey);

                int tileScreenX = (int) (width / 2.0 - tileOffsetX + dx * displayTileSize);
                int tileScreenY = (int) (height / 2.0 - tileOffsetY + dy * displayTileSize);

                if (tileScreenX + displayTileSize < 0 || tileScreenX > width ||
                        tileScreenY + displayTileSize < 0 || tileScreenY > height) {
                    continue;
                }

                if (tile != null && tile.isValid()) {
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, tile.getTextureId(),
                            tileScreenX, tileScreenY,
                            0.0f, 0.0f,
                            displayTileSize, displayTileSize,
                            tileSize, tileSize,
                            tileSize, tileSize);
                } else {
                    context.fill(tileScreenX, tileScreenY,
                            tileScreenX + displayTileSize, tileScreenY + displayTileSize, 0xFF2a2a2a);
                    context.fill(tileScreenX, tileScreenY, tileScreenX + displayTileSize, tileScreenY + 1, 0xFF3a3a3a);
                    context.fill(tileScreenX, tileScreenY + displayTileSize - 1, tileScreenX + displayTileSize, tileScreenY + displayTileSize, 0xFF3a3a3a);
                    context.fill(tileScreenX, tileScreenY, tileScreenX + 1, tileScreenY + displayTileSize, 0xFF3a3a3a);
                    context.fill(tileScreenX + displayTileSize - 1, tileScreenY, tileScreenX + displayTileSize, tileScreenY + displayTileSize, 0xFF3a3a3a);
                }
            }
        }
    }

    private void renderMarkers(DrawContext context) {
        int blocksPerTile = config.getBlocksPerTile(currentZoom);
        double pixelsPerBlock = (double) config.getTileSize() / blocksPerTile;

        // Draw town markers (hide when zoomed out to reduce clutter)
        if (currentZoom >= 0) for (MarkerManager.TownMarker town : markerManager.getTowns()) {
            int townScreenX = (int) (width / 2.0 + (town.x() - viewCenterX) * pixelsPerBlock);
            int townScreenY = (int) (height / 2.0 + (town.z() - viewCenterZ) * pixelsPerBlock);

            if (townScreenX >= 0 && townScreenX <= width && townScreenY >= 0 && townScreenY <= height) {
                int color = town.type().contains("capital") ? 0xFFFFD700 : 0xFF00FF00;
                int size = town.type().contains("capital") ? 5 : 4;

                // Draw marker
                context.fill(townScreenX - size, townScreenY - size,
                        townScreenX + size, townScreenY + size, color);
                context.fill(townScreenX - size + 1, townScreenY - size + 1,
                        townScreenX + size - 1, townScreenY + size - 1, 0xFF000000 | (color & 0x00FFFFFF));

                // Draw town name
                String name = town.name();
                if (name.length() > 12) name = name.substring(0, 12) + "..";
                context.drawText(textRenderer, name, townScreenX - textRenderer.getWidth(name) / 2,
                        townScreenY + size + 2, 0xFFFFFFFF, true);
            }
        }

        // Draw other players (blue) - filter out local player
        MinecraftClient client = MinecraftClient.getInstance();
        String localPlayerName = client.player != null ? client.player.getName().getString() : "";
        String localPlayerUuid = client.player != null ? client.player.getUuidAsString() : "";

        for (MarkerManager.PlayerMarker player : markerManager.getPlayers()) {
            // Skip the local player (check both name and UUID)
            if (player.name().equals(localPlayerName) ||
                (!player.uuid().isEmpty() && player.uuid().equals(localPlayerUuid))) {
                continue;
            }

            int pScreenX = (int) (width / 2.0 + (player.x() - viewCenterX) * pixelsPerBlock);
            int pScreenY = (int) (height / 2.0 + (player.z() - viewCenterZ) * pixelsPerBlock);

            if (pScreenX >= 0 && pScreenX <= width && pScreenY >= 0 && pScreenY <= height) {
                drawPlayerArrow(context, pScreenX, pScreenY, player.yaw(), 5, 0xFF00BFFF);

                // Draw player name
                context.drawText(textRenderer, player.name(), pScreenX - textRenderer.getWidth(player.name()) / 2,
                        pScreenY + 10, 0xFF00BFFF, true);
            }
        }

        // Draw local player marker (red, on top)
        if (client.player != null) {
            double playerX = client.player.getX();
            double playerZ = client.player.getZ();

            int playerScreenX = (int) (width / 2.0 + (playerX - viewCenterX) * pixelsPerBlock);
            int playerScreenY = (int) (height / 2.0 + (playerZ - viewCenterZ) * pixelsPerBlock);

            drawPlayerArrow(context, playerScreenX, playerScreenY, client.player.getYaw(), 6, 0xFFFF0000);
        }
    }

    private void renderUI(DrawContext context, int mouseX, int mouseY) {
        int padding = 10;
        int boxWidth = 220;
        int boxHeight = 75;

        context.fill(padding, padding, padding + boxWidth, padding + boxHeight, 0xAA000000);

        MinecraftClient client = MinecraftClient.getInstance();

        String title = "LostCompass Map";
        context.drawText(textRenderer, title, padding + 5, padding + 5, 0xFFFFFFFF, true);

        String viewCoords = String.format("View: X: %d Z: %d", (int) viewCenterX, (int) viewCenterZ);
        context.drawText(textRenderer, viewCoords, padding + 5, padding + 18, 0xFFCCCCCC, false);

        String zoomText = String.format("Zoom: %d (%d to %d)", currentZoom, config.getMinZoom(), config.getMaxZoom());
        context.drawText(textRenderer, zoomText, padding + 5, padding + 30, 0xFFCCCCCC, false);

        String stats = String.format("Players: %d | Towns: %d",
                markerManager.getPlayers().size(), markerManager.getTowns().size());
        context.drawText(textRenderer, stats, padding + 5, padding + 42, 0xFF88FF88, false);

        String controls = "[Scroll] Zoom  [Drag] Pan  [R] Reset";
        context.drawText(textRenderer, controls, padding + 5, padding + 58, 0xFF888888, false);

        if (client.player != null) {
            double playerX = client.player.getX();
            double playerZ = client.player.getZ();
            String playerCoords = String.format("You: X: %d Z: %d", (int) playerX, (int) playerZ);
            context.drawText(textRenderer, playerCoords,
                    width - textRenderer.getWidth(playerCoords) - padding,
                    padding + 5, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (click.button() == 0) {
            isDragging = true;
            dragStartX = click.x();
            dragStartY = click.y();
            dragStartViewX = viewCenterX;
            dragStartViewZ = viewCenterZ;
            return true;
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (isDragging && click.button() == 0) {
            int blocksPerTile = config.getBlocksPerTile(currentZoom);
            double pixelsPerBlock = (double) config.getTileSize() / blocksPerTile;

            double dragDeltaX = click.x() - dragStartX;
            double dragDeltaY = click.y() - dragStartY;

            viewCenterX = dragStartViewX - dragDeltaX / pixelsPerBlock;
            viewCenterZ = dragStartViewZ - dragDeltaY / pixelsPerBlock;
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            zoomIn();
        } else if (verticalAmount < 0) {
            zoomOut();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();

        if (keyCode == GLFW.GLFW_KEY_R) {
            centerOnPlayer();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_EQUAL || keyCode == GLFW.GLFW_KEY_KP_ADD) {
            zoomIn();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            zoomOut();
            return true;
        }

        int panSpeed = config.getBlocksPerTile(currentZoom) / 4;
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) {
            viewCenterZ -= panSpeed;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
            viewCenterZ += panSpeed;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            viewCenterX -= panSpeed;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            viewCenterX += panSpeed;
            return true;
        }

        return super.keyPressed(keyInput);
    }

    private void zoomIn() {
        if (currentZoom < config.getMaxZoom()) {
            currentZoom++;
        }
    }

    private void zoomOut() {
        if (currentZoom > config.getMinZoom()) {
            currentZoom--;
        }
    }

    private void centerOnPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            viewCenterX = client.player.getX();
            viewCenterZ = client.player.getZ();
        }
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

    @Override
    public boolean shouldPause() {
        return false;
    }
}
