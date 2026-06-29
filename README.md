# Discy Upgrade

A Minecraft 1.20.1 Forge mod that adds disco lighting and a programmable dance floor, inspired by the **Disc Design Studio (DDS)** color tools from [Discy](https://github.com/AlfieeLawrence/Discy-A-custom-Disc-mod-for-Minecraft-).

## Features

### Dance floor tiles
- Place individual **Dance Floor Tiles** next to each other to form a linked network.
- Each tile stores its own RGB color and syncs across clients.

### Dance floor controller
- Place a **Dance Floor Controller** beside your floor and right-click to open the GUI.
- The controller shows every connected tile in its real-world layout (top-down grid).
- Pick a tile, then use the DDS-style color wheel, brightness slider, quick palette, and hex field to change it.
- **Apply to tile** updates one square; **Apply to all** paints the whole floor.

### Disco ball
- Hang-style disco ball block that spins when active.
- Toggle with right-click or redstone power.
- Emits colored sparkle particles while active.

### Laser emitters
- Directional blocks that shoot a colored laser beam when powered by redstone.
- Beams stop at the first solid block they hit.

## Building

```bash
./gradlew :forge:build
```

The remapped JAR is at `forge/build/libs/discyupgrade-forge-1.0.0.jar`.

## Usage tips

1. Craft dance floor tiles and lay out your pattern on one Y level.
2. Place the controller adjacent to any tile in the floor.
3. Open the controller, click tiles in the layout preview, and paint colors.
4. Add a disco ball and laser emitters, then power the lasers with redstone for a full disco setup.

## Requirements

- Minecraft 1.20.1
- Forge 47+
- Architectury API

## License

MIT
