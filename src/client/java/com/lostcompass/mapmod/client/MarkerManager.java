package com.lostcompass.mapmod.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lostcompass.mapmod.LostCompassMapMod;
import com.lostcompass.mapmod.config.ModConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MarkerManager {
    private final ModConfig config;
    private final HttpClient httpClient;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    private final List<PlayerMarker> players = new CopyOnWriteArrayList<>();
    private final List<TownMarker> towns = new CopyOnWriteArrayList<>();

    public record PlayerMarker(String name, String uuid, double x, double y, double z, float yaw) {}
    public record TownMarker(String name, String type, double x, double z, int residents) {}

    public MarkerManager(ModConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(this::fetchPlayers, 0, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::fetchTowns, 0, 60, TimeUnit.SECONDS);
    }

    private void fetchPlayers() {
        try {
            String url = config.getMapServerUrl() + "/tiles/players.json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                parsePlayersJson(response.body());
            }
        } catch (Exception e) {
            LostCompassMapMod.LOGGER.debug("Failed to fetch players: {}", e.getMessage());
        }
    }

    private void parsePlayersJson(String json) {
        try {
            JsonObject root = gson.fromJson(json, JsonObject.class);
            JsonArray playersArray = root.getAsJsonArray("players");

            List<PlayerMarker> newPlayers = new ArrayList<>();
            if (playersArray != null) {
                for (JsonElement element : playersArray) {
                    JsonObject player = element.getAsJsonObject();
                    String name = player.get("name").getAsString();
                    String uuid = player.has("uuid") ? player.get("uuid").getAsString() : "";
                    double x = player.get("x").getAsDouble();
                    double y = player.get("y").getAsDouble();
                    double z = player.get("z").getAsDouble();
                    float yaw = player.has("yaw") ? player.get("yaw").getAsFloat() : 0;

                    newPlayers.add(new PlayerMarker(name, uuid, x, y, z, yaw));
                }
            }

            players.clear();
            players.addAll(newPlayers);
        } catch (Exception e) {
            LostCompassMapMod.LOGGER.debug("Failed to parse players JSON: {}", e.getMessage());
        }
    }

    private void fetchTowns() {
        try {
            String url = config.getMapServerUrl() + "/tiles/minecraft_overworld/markers.json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                parseTownsJson(response.body());
            }
        } catch (Exception e) {
            LostCompassMapMod.LOGGER.debug("Failed to fetch towns: {}", e.getMessage());
        }
    }

    private void parseTownsJson(String json) {
        try {
            JsonArray root = gson.fromJson(json, JsonArray.class);

            List<TownMarker> newTowns = new ArrayList<>();
            for (JsonElement groupElement : root) {
                JsonObject group = groupElement.getAsJsonObject();
                if (!group.has("markers")) continue;

                JsonArray markers = group.getAsJsonArray("markers");
                for (JsonElement markerElement : markers) {
                    JsonObject marker = markerElement.getAsJsonObject();

                    String type = marker.has("type") ? marker.get("type").getAsString() : "icon";
                    if (!"icon".equals(type)) continue;

                    JsonObject point = marker.getAsJsonObject("point");
                    if (point == null) continue;

                    double x = point.get("x").getAsDouble();
                    double z = point.get("z").getAsDouble();

                    String tooltip = marker.has("tooltip") ? marker.get("tooltip").getAsString() : "";
                    String name = extractTownName(tooltip);
                    int residents = extractResidentCount(tooltip);
                    String iconType = marker.has("icon") ? marker.get("icon").getAsString() : "town_icon";

                    if (!name.isEmpty()) {
                        newTowns.add(new TownMarker(name, iconType, x, z, residents));
                    }
                }
            }

            towns.clear();
            towns.addAll(newTowns);
            LostCompassMapMod.LOGGER.debug("Loaded {} towns", towns.size());
        } catch (Exception e) {
            LostCompassMapMod.LOGGER.debug("Failed to parse towns JSON: {}", e.getMessage());
        }
    }

    private String extractTownName(String tooltip) {
        if (tooltip.contains("<b>")) {
            int start = tooltip.indexOf("<b>") + 3;
            int end = tooltip.indexOf("</b>", start);
            if (end > start) {
                return tooltip.substring(start, end).trim();
            }
        }
        return tooltip.split("<br>")[0].replaceAll("<[^>]*>", "").trim();
    }

    private int extractResidentCount(String tooltip) {
        try {
            if (tooltip.contains("Жители:")) {
                int start = tooltip.indexOf("Жители:") + 7;
                String rest = tooltip.substring(start);
                String[] parts = rest.split("<br>")[0].split(",");
                return parts.length;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public List<PlayerMarker> getPlayers() {
        return players;
    }

    public List<TownMarker> getTowns() {
        return towns;
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
