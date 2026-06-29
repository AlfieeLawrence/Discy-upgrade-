# Disco Lights

A standalone Minecraft 1.20.1 mod (Forge + Fabric via Architectury) — **not** part of Discy. Adds disco lighting, programmable dance floors, and a central **Light Controller**.

## Blocks & items

| Item | Purpose |
|------|---------|
| **Dance Floor Tile** | Colored floor segment; adjacent tiles auto-join the same group |
| **Floor Link Plate** | Invisible-style bridge; floor groups link through it |
| **Light Controller** | Controls all linked floors, disco balls, lasers, party & strobe lights |
| **Tuning Wrench** | Assign, disconnect, capture, and wireless-link floor groups and lights |
| **Light Remote** | Open a linked controller GUI from a distance |
| **Disco Ball** | Spinning ball with colored particles and laser beams |
| **Laser Emitter** | Redstone-powered colored laser beam |
| **Party Light** | Tinted ceiling light, toggled by hand |
| **Strobe Light** | Flashing white strobe when powered |

## Dance floor groups

- Tiles placed **next to each other** merge into one floor group automatically.
- **Floor Link Plates** let groups connect through a gap (e.g. under walls).
- Use the **Tuning Wrench** to:
  - **Sneak + use** a tile → disconnect it from its group
  - **Use** a tile while the wrench holds a target group → assign that tile to the group
  - **Use** a tile (empty wrench) → capture its group ID for wireless linking
- Name groups from the Light Controller **Groups** tab.

## Light Controller linking

**Physical (touch):** Place the controller beside dance floor tiles or link plates — it auto-links touching floor groups.

**Wireless:** Capture a floor group on the wrench, then use the wrench on the Light Controller (range configurable).

**Multiple floors:** Link several named floor groups to one controller. Switch between them in the **Floors** tab.

**Lights:** Sneak+use a disco ball, laser, party light, or strobe with the wrench to capture it, then use the wrench on the controller.

**Remote:** Hold a Light Remote in your offhand and use the wrench on a controller to bind it.

## Controller GUI

- **Floors** — pick a linked floor, paint tiles (DDS-style color wheel), set animation pattern/speed, play/pause, sync to disco
- **Lights** — toggle linked disco balls, lasers, party lights, and strobes
- **Groups** — rename groups, save/load color presets, copy layouts between groups

### Floor patterns

Static, Rainbow Chase, Pulse, Wave, and Sparkle — each with adjustable speed. Enable **Sync** to drive animations from linked disco ball spin.

## Configuration (Forge)

`config/discyupgrade-common.toml`:

- `wirelessLinkRange` — max blocks for wireless wrench linking (default 64)
- `remoteOpenRange` — max blocks for light remote (default 32)
- `maxLinkedFloorsPerController` — floor groups per controller (default 8)
- `maxTilesPerGroup` — tiles per floor group (default 256)

## Build

```bash
./gradlew :forge:build
./gradlew :fabric:build
```

Outputs:

- `forge/build/libs/discyupgrade-forge-1.0.0.jar`
- `fabric/build/libs/discyupgrade-fabric-1.0.0.jar`

## Requirements

- Minecraft 1.20.1
- Forge 47+ **or** Fabric Loader + Fabric API
- Architectury API

MIT License
