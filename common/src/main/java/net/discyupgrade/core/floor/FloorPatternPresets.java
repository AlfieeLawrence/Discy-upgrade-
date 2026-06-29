package net.discyupgrade.core.floor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import java.util.*;

/** Saved color layouts keyed by preset name. */
public final class FloorPatternPresets {
    public record PresetEntry(String name, Map<Long, Integer> tileColors) {}

    private FloorPatternPresets() {}

    public static Map<Long, Integer> snapshotGroup(Level level, UUID groupId) {
        Map<Long, Integer> map = new HashMap<>();
        for (FloorGroupIndex.TileEntry entry : FloorGroupIndex.buildLayout(level, groupId)) {
            map.put(pack(entry.relX(), entry.relZ()), entry.color());
        }
        return map;
    }

    public static void applyPreset(Level level, UUID groupId, Map<Long, Integer> colors) {
        for (FloorGroupIndex.TileEntry entry : FloorGroupIndex.buildLayout(level, groupId)) {
            Integer color = colors.get(pack(entry.relX(), entry.relZ()));
            if (color == null) continue;
            var be = level.getBlockEntity(entry.pos());
            if (be instanceof net.discyupgrade.core.block.DanceFloorTileBlockEntity tile) {
                tile.setColor(color);
                level.sendBlockUpdated(entry.pos(), tile.getBlockState(), tile.getBlockState(), 3);
            }
        }
    }

    public static ListTag writePresets(Map<String, Map<Long, Integer>> presets) {
        ListTag list = new ListTag();
        for (var e : presets.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Name", e.getKey());
            ListTag tiles = new ListTag();
            for (var c : e.getValue().entrySet()) {
                CompoundTag t = new CompoundTag();
                t.putLong("Key", c.getKey());
                t.putInt("Color", c.getValue());
                tiles.add(t);
            }
            tag.put("Tiles", tiles);
            list.add(tag);
        }
        return list;
    }

    public static Map<String, Map<Long, Integer>> readPresets(ListTag list) {
        Map<String, Map<Long, Integer>> presets = new LinkedHashMap<>();
        for (Tag raw : list) {
            CompoundTag tag = (CompoundTag) raw;
            String name = tag.getString("Name");
            Map<Long, Integer> colors = new HashMap<>();
            ListTag tiles = tag.getList("Tiles", Tag.TAG_COMPOUND);
            for (Tag tRaw : tiles) {
                CompoundTag t = (CompoundTag) tRaw;
                colors.put(t.getLong("Key"), t.getInt("Color"));
            }
            presets.put(name, colors);
        }
        return presets;
    }

    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
