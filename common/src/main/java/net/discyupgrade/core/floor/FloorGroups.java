package net.discyupgrade.core.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public final class FloorGroups {
    private FloorGroups() {}

    public record TileEntry(BlockPos pos, int relX, int relZ, int color) {}

    public static List<TileEntry> buildLayout(Level level, UUID groupId) {
        return FloorGroupIndex.buildLayout(level, groupId).stream()
                .map(e -> new TileEntry(e.pos(), e.relX(), e.relZ(), e.color()))
                .toList();
    }

    public static BlockPos findTileAt(Level level, UUID groupId, int relX, int relZ) {
        return FloorGroupIndex.findTileAt(level, groupId, relX, relZ);
    }

    public static List<UUID> findTouchingGroups(Level level, BlockPos controllerPos) {
        return FloorGroupIndex.findTouchingGroups(level, controllerPos);
    }

    public static String getGroupName(UUID groupId) {
        return FloorGroupIndex.getGroupName(groupId);
    }
}
