package com.lostcompass.mapmod.client;

import com.lostcompass.mapmod.LostCompassMapMod;
import com.lostcompass.mapmod.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TileManager {
    private final ModConfig config;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Map<TileKey, CachedTile> tileCache;
    private final Map<TileKey, CompletableFuture<CachedTile>> pendingFetches;
    private int textureIdCounter = 0;

    public TileManager(ModConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.executor = Executors.newFixedThreadPool(4);
        this.pendingFetches = new ConcurrentHashMap<>();

        int cacheSize = config.getTileCacheSize();
        this.tileCache = new LinkedHashMap<>(cacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<TileKey, CachedTile> eldest) {
                if (size() > cacheSize) {
                    eldest.getValue().destroy();
                    return true;
                }
                return false;
            }
        };
    }

    public record TileKey(int zoom, int x, int y) {}

    public static class CachedTile {
        private final Identifier textureId;
        private final NativeImageBackedTexture texture;
        private boolean destroyed = false;

        public CachedTile(Identifier textureId, NativeImageBackedTexture texture) {
            this.textureId = textureId;
            this.texture = texture;
        }

        public Identifier getTextureId() {
            return textureId;
        }

        public boolean isValid() {
            return !destroyed && texture != null;
        }

        public void destroy() {
            if (!destroyed) {
                destroyed = true;
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().getTextureManager().destroyTexture(textureId);
                });
            }
        }
    }

    public TileKey worldToTile(double worldX, double worldZ, int zoom) {
        int blocksPerTile = config.getBlocksPerTile(zoom);
        int tileX = (int) Math.floor(worldX / blocksPerTile);
        int tileY = (int) Math.floor(worldZ / blocksPerTile);
        return new TileKey(zoom, tileX, tileY);
    }

    public double[] tileToWorld(int tileX, int tileY, int zoom) {
        int blocksPerTile = config.getBlocksPerTile(zoom);
        return new double[]{tileX * blocksPerTile, tileY * blocksPerTile};
    }

    public CachedTile getTile(TileKey key) {
        synchronized (tileCache) {
            CachedTile cached = tileCache.get(key);
            if (cached != null && cached.isValid()) {
                return cached;
            }
        }
        return null;
    }

    public void requestTile(TileKey key) {
        synchronized (tileCache) {
            if (tileCache.containsKey(key)) {
                return;
            }
        }

        if (pendingFetches.containsKey(key)) {
            return;
        }

        CompletableFuture<CachedTile> future = CompletableFuture.supplyAsync(() -> {
            try {
                return fetchTile(key);
            } catch (Exception e) {
                LostCompassMapMod.LOGGER.warn("Failed to fetch tile {}: {}", key, e.getMessage());
                return null;
            }
        }, executor);

        pendingFetches.put(key, future);

        future.thenAccept(tile -> {
            pendingFetches.remove(key);
            if (tile != null) {
                MinecraftClient.getInstance().execute(() -> {
                    synchronized (tileCache) {
                        tileCache.put(key, tile);
                    }
                });
            }
        });
    }

    private CachedTile fetchTile(TileKey key) throws Exception {
        String url = config.getTileUrl(key.zoom(), key.x(), key.y());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode());
        }

        try (InputStream is = response.body()) {
            NativeImage image = NativeImage.read(is);

            final NativeImage finalImage = image;
            final int id = textureIdCounter++;
            final Identifier textureId = Identifier.of(LostCompassMapMod.MOD_ID,
                    "tile_" + key.zoom() + "_" + key.x() + "_" + key.y() + "_" + id);

            CompletableFuture<CachedTile> result = new CompletableFuture<>();

            MinecraftClient.getInstance().execute(() -> {
                try {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(
                            () -> textureId.toString(), finalImage);
                    MinecraftClient.getInstance().getTextureManager()
                            .registerTexture(textureId, texture);
                    result.complete(new CachedTile(textureId, texture));
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });

            return result.get();
        }
    }

    public void preloadTilesAround(double worldX, double worldZ, int zoom, int radius) {
        TileKey center = worldToTile(worldX, worldZ, zoom);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                TileKey key = new TileKey(zoom, center.x() + dx, center.y() + dy);
                if (getTile(key) == null) {
                    requestTile(key);
                }
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
        synchronized (tileCache) {
            for (CachedTile tile : tileCache.values()) {
                tile.destroy();
            }
            tileCache.clear();
        }
    }

    public ModConfig getConfig() {
        return config;
    }
}
