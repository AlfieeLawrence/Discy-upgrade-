# Disco Lights

A standalone Minecraft 1.20.1 Forge mod — **not** part of Discy. Adds disco lighting, programmable dance floors, and a central **Light Controller**.

## Blocks & items

| Item | Purpose |
|------|---------|
| **Dance Floor Tile** | Colored floor segment; adjacent tiles auto-join the same group |
| **Light Controller** | Controls all linked floors, disco balls, and lasers |
| **Tuning Wrench** | Assign, disconnect, capture, and wireless-link floor groups and lights |
| **Disco Ball** | Spinning ball with colored particles |
| **Laser Emitter** | Redstone-powered colored laser beam |

## Dance floor groups

- Tiles placed **next to each other** merge into one floor group automatically.
- Use the **Tuning Wrench** to:
  - **Sneak + use** a tile → disconnect it from its group (even if still touching other tiles)
  - **Use** a tile while the wrench holds a target group → assign that tile to the group (works across distance)
  - **Use** a tile (empty wrench) → capture its group ID for wireless linking
- Name groups from the Light Controller **Groups** tab.

## Light Controller linking

**Physical (touch):** Place the controller on a block beside dance floor tiles — it auto-links any touching floor groups.

**Wireless:** Capture a floor group on the wrench (use a tile), then use the wrench on the Light Controller.

**Multiple floors:** Link as many floor groups as you want to one controller. Switch between them in the **Floors** tab.

**Lights:** Sneak+use a disco ball or laser with the wrench to capture it, then use the wrench on the controller.

## Controller GUI tabs

- **Floors** — pick a linked floor, view layout, paint tiles (DDS-style color wheel)
- **Lights** — see linked disco balls & lasers; toggle spin and power
- **Groups** — manage names and wrench instructions

## Build

```bash
./gradlew :forge:build
```

Output: `forge/build/libs/discyupgrade-forge-1.0.0.jar`

## Requirements

- Minecraft 1.20.1, Forge 47+, Architectury API

MIT License
