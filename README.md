# Disco Lights

A standalone Minecraft 1.20.1 mod (Forge + Fabric via Architectury) — **not** part of Discy. Adds disco lighting, programmable dance floors, and a central **Light Controller**.

## Blocks & items

| Item | Purpose |
|------|---------|
| **Dance Floor Tile** | Colored floor segment; adjacent tiles auto-join the same group |
| **Floor Link Plate** | Bridge floor groups through gaps |
| **Light Controller** | Controls all linked floors, disco balls, lasers, party & strobe lights |
| **Tuning Wrench** | Assign, disconnect, capture, and wireless-link floor groups and lights |
| **Light Remote** | Open a linked controller GUI from a distance |
| **Disco Ball** | Spinning ball with colored particles and laser beams |
| **Laser Emitter** | Redstone-powered colored laser beam |
| **Party Light** | Tinted ceiling light, toggled by hand |
| **Strobe Light** | Flashing white strobe when powered |
| **Disco Guide** | Written book with setup instructions |
| **Disco Goggles** | Night vision when worn (head or Curios/Trinkets slot) |

## Dance floor groups

- Tiles placed **next to each other** merge into one floor group automatically.
- **Floor Link Plates** let groups connect through a gap.
- Use the **Tuning Wrench** to assign, disconnect, capture, and wireless-link groups.
- Name groups from the Light Controller **Groups** tab.

## Light Controller linking

**Physical (touch):** Place the controller beside dance floor tiles or link plates.

**Wireless:** Capture a floor group on the wrench, then use the wrench on the controller.

**Lights & jukeboxes:** Sneak+use to capture, then use on the controller to link.

**Remote:** Hold a Light Remote in offhand and use the wrench on a controller to bind.

## Controller GUI

- **Floors** — paint tiles, set patterns (Rainbow Chase, Pulse, Wave, Sparkle), speed, play/pause
- **Sync** — tie patterns to disco ball spin or linked jukebox playback
- **Lights** — toggle disco balls, lasers, party lights, strobes; view jukebox status
- **Groups** — rename, save/load presets, copy layouts

**Redstone:** Power the controller to force all patterns to play.

## Configuration

**Forge:** `config/discyupgrade-common.toml`

**Fabric:** `config/discyupgrade.json`

Options: wireless range, remote range, max floors/tiles, animation distance, tick interval, redstone trigger.

## Accessories (optional)

- **Curios** (Forge) — wrench, remote, and goggles work from accessory slots
- **Trinkets** (Fabric) — same via Trinkets slots

## Build

```bash
./gradlew :forge:build
./gradlew :fabric:build
```

## Requirements

- Minecraft 1.20.1
- Forge 47+ **or** Fabric Loader + Fabric API
- Architectury API

MIT License
