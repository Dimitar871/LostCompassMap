# LostCompass Map Mod

A Fabric mod that adds a minimap and full-screen map for the LostCompass server, pulling tiles from [map.lostcompass.world](https://map.lostcompass.world/).

## Requirements

- Minecraft **1.21.10** (Java Edition)
- [Fabric Loader](https://fabricmc.net/use/installer/) (0.16.10+)
- [Fabric API](https://modrinth.com/mod/fabric-api) (0.138.4+1.21.10)

## Installation

1. Install Fabric Loader using the [Fabric installer](https://fabricmc.net/use/installer/)
2. Download **Fabric API** from [Modrinth](https://modrinth.com/mod/fabric-api) and place it in your `.minecraft/mods/` folder
3. Place `lostcompass-map-1.0.0.jar` in your `.minecraft/mods/` folder
4. Launch Minecraft with the Fabric profile

## Controls

| Key | Action |
|-----|--------|
| **M** | Open/close the full-screen map |
| **Scroll wheel** | Zoom in/out (on minimap and full-screen map) |

## Features

- **Minimap** in the top-right corner showing your position, nearby towns, and other players
- **Full-screen map** with pan (click and drag) and zoom (scroll wheel)
- **Zoom levels 0-5**: zoom 0 is the most zoomed out, zoom 5 is the most zoomed in
- **Town markers** with names (hidden when zoomed out to reduce clutter)
- **Player markers** showing other online players
- **Current town display** below the minimap when within 50 blocks of a town center
