package net.discyupgrade.core.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Facade for dance floor network operations. */
public final class DanceFloorNetwork {
    private DanceFloorNetwork() {}

    public record TileEntry(BlockPos pos, int relX, int relZ, int color) {}

    public static UUID findAdjacentNetwork(Level level, BlockPos controllerPos) {
        return DanceFloorNetworkIndex.findAdjacentNetwork(level, controllerPos);
    }

    public static java.util.List<TileEntry> buildLayout(Level level, UUID networkId) {
        return DanceFloorNetworkIndex.buildLayout(level, networkId);
    }

    public static BlockPos findTileAt(Level level, UUID networkId, int relX, int relZ) {
        return DanceFloorNetworkIndex.findTileAt(level, networkId, relX, relZ);
    }
}
