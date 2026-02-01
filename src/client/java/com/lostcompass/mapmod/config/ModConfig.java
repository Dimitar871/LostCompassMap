package com.lostcompass.mapmod.config;

public class ModConfig {
    private String mapServerUrl = "https://map.lostcompass.world";
    private String tileUrlPattern = "/tiles/minecraft_overworld/{z}/{x}_{y}.png";
    private int tileSize = 512;
    private int maxZoom = 3;      // Max server-side zoom (actual tiles)
    private int extraZoom = 2;    // Extra zoom IN levels via client-side scaling (4-5)
    private int minZoom = -2;     // Extra zoom OUT levels via client-side scaling
    private int serverMinZoom = 0; // Server's actual minimum zoom
    private int defaultZoom = 1;
    private int minimapSize = 100;
    private float minimapOpacity = 0.9f;
    private int minimapMargin = 10;
    private int tileCacheSize = 50;

    public String getMapServerUrl() {
        return mapServerUrl;
    }

    public void setMapServerUrl(String mapServerUrl) {
        this.mapServerUrl = mapServerUrl;
    }

    public String getTileUrlPattern() {
        return tileUrlPattern;
    }

    public String getTileUrl(int zoom, int x, int y) {
        return mapServerUrl + tileUrlPattern
                .replace("{z}", String.valueOf(zoom))
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y));
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getMaxZoom() {
        return maxZoom + extraZoom;  // Total max zoom including extra levels
    }

    public int getServerMaxZoom() {
        return maxZoom;  // Max zoom level with actual server tiles
    }

    public void setMaxZoom(int maxZoom) {
        this.maxZoom = maxZoom;
    }

    public int getMinZoom() {
        return minZoom;
    }

    public int getServerMinZoom() {
        return serverMinZoom;  // Min zoom level with actual server tiles
    }

    public void setMinZoom(int minZoom) {
        this.minZoom = minZoom;
    }

    public int getDefaultZoom() {
        return defaultZoom;
    }

    public void setDefaultZoom(int defaultZoom) {
        this.defaultZoom = defaultZoom;
    }

    public int getMinimapSize() {
        return minimapSize;
    }

    public void setMinimapSize(int minimapSize) {
        this.minimapSize = minimapSize;
    }

    public float getMinimapOpacity() {
        return minimapOpacity;
    }

    public void setMinimapOpacity(float minimapOpacity) {
        this.minimapOpacity = minimapOpacity;
    }

    public int getMinimapMargin() {
        return minimapMargin;
    }

    public int getTileCacheSize() {
        return tileCacheSize;
    }

    public int getBlocksPerTile(int zoom) {
        // Clamp zoom to server range for base calculation
        int clampedZoom = Math.max(serverMinZoom, Math.min(zoom, maxZoom));
        int baseBlocks = tileSize * (int) Math.pow(2, maxZoom - clampedZoom);

        // For extra zoom IN (above maxZoom): halve blocks per tile
        if (zoom > maxZoom) {
            baseBlocks = baseBlocks / (int) Math.pow(2, zoom - maxZoom);
        }
        // For extra zoom OUT (below serverMinZoom): double blocks per tile
        if (zoom < serverMinZoom) {
            baseBlocks = baseBlocks * (int) Math.pow(2, serverMinZoom - zoom);
        }
        return Math.max(1, baseBlocks);
    }
}
