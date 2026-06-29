package net.discyupgrade.core.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.DanceFloorTileBlockEntity;

import java.util.*;

/**
 * Server-side index of dance floor tile networks.
 */
public final class DanceFloorNetworkIndex {
    private static final Map<UUID, Set<BlockPos>> NETWORKS = new HashMap<>();

    private DanceFloorNetworkIndex() {}

    public static void register(ServerLevel level, BlockPos pos, UUID networkId) {
        NETWORKS.computeIfAbsent(networkId, id -> new HashSet<>()).add(pos.immutable());
    }

    public static void unregister(BlockPos pos, UUID networkId) {
        if (networkId == null) return;
        Set<BlockPos> members = NETWORKS.get(networkId);
        if (members != null) {
            members.remove(pos);
            if (members.isEmpty()) NETWORKS.remove(networkId);
        }
    }

    public static void changeNetwork(BlockPos pos, UUID oldId, UUID newId) {
        unregister(pos, oldId);
        if (newId != null) {
            NETWORKS.computeIfAbsent(newId, id -> new HashSet<>()).add(pos.immutable());
        }
    }

    public static List<BlockPos> getMembers(UUID networkId) {
        Set<BlockPos> members = NETWORKS.get(networkId);
        return members == null ? List.of() : new ArrayList<>(members);
    }

    public static void rebuildFromLevel(ServerLevel level, UUID networkId) {
        Set<BlockPos> members = NETWORKS.get(networkId);
        if (members == null) return;
        members.removeIf(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            return !(be instanceof DanceFloorTileBlockEntity tile) || !networkId.equals(tile.getNetworkId());
        });
        if (members.isEmpty()) NETWORKS.remove(networkId);
    }

    public static void onTilePlaced(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DanceFloorTileBlockEntity tile)) return;

        Set<UUID> neighborNetworks = new HashSet<>();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockEntity nbe = level.getBlockEntity(pos.relative(dir));
            if (nbe instanceof DanceFloorTileBlockEntity neighborTile) {
                UUID id = neighborTile.getNetworkId();
                if (id != null) neighborNetworks.add(id);
            }
        }

        if (neighborNetworks.isEmpty()) {
            UUID newId = UUID.randomUUID();
            tile.setNetworkId(newId);
            register(level, pos, newId);
            tile.setChanged();
            return;
        }

        UUID merged = neighborNetworks.iterator().next();
        for (UUID other : neighborNetworks) {
            if (!other.equals(merged)) {
                mergeNetworks(other, merged);
            }
        }

        tile.setNetworkId(merged);
        register(level, pos, merged);
        tile.setChanged();
        syncNetwork(level, merged);
    }

    public static void onTileRemoved(ServerLevel level, BlockPos removedPos, UUID networkId) {
        unregister(removedPos, networkId);
        if (networkId == null) return;

        List<BlockPos> remaining = getMembers(networkId);
        if (remaining.isEmpty()) return;

        if (!isConnected(level, remaining)) {
            splitDisconnected(level, remaining);
        } else {
            syncNetwork(level, networkId);
        }
    }

    private static void mergeNetworks(UUID from, UUID into) {
        Set<BlockPos> fromMembers = NETWORKS.remove(from);
        if (fromMembers == null) return;
        NETWORKS.computeIfAbsent(into, id -> new HashSet<>()).addAll(fromMembers);
    }

    private static void splitDisconnected(ServerLevel level, List<BlockPos> tiles) {
        Set<BlockPos> unassigned = new HashSet<>(tiles);
        NETWORKS.values().forEach(set -> set.removeAll(unassigned));

        while (!unassigned.isEmpty()) {
            BlockPos start = unassigned.iterator().next();
            Set<BlockPos> component = floodFill(start, unassigned);
            UUID newId = UUID.randomUUID();
            Set<BlockPos> registered = NETWORKS.computeIfAbsent(newId, id -> new HashSet<>());
            for (BlockPos pos : component) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof DanceFloorTileBlockEntity tile) {
                    tile.setNetworkId(newId);
                    tile.setChanged();
                    registered.add(pos.immutable());
                }
                unassigned.remove(pos);
            }
            syncNetwork(level, newId);
        }
    }

    private static boolean isConnected(ServerLevel level, List<BlockPos> tiles) {
        if (tiles.size() <= 1) return true;
        Set<BlockPos> set = new HashSet<>(tiles);
        return floodFill(tiles.get(0), set).size() == set.size();
    }

    private static Set<BlockPos> floodFill(BlockPos start, Set<BlockPos> allowed) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(dir);
                if (allowed.contains(next) && visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    public static List<DanceFloorNetwork.TileEntry> buildLayout(Level level, UUID networkId) {
        List<BlockPos> tiles = getMembers(networkId);
        if (tiles.isEmpty()) return List.of();

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (BlockPos pos : tiles) {
            minX = Math.min(minX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
        }

        List<DanceFloorNetwork.TileEntry> entries = new ArrayList<>();
        for (BlockPos pos : tiles) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DanceFloorTileBlockEntity tile) {
                entries.add(new DanceFloorNetwork.TileEntry(pos, pos.getX() - minX, pos.getZ() - minZ, tile.getColor()));
            }
        }
        entries.sort(Comparator.comparingInt(DanceFloorNetwork.TileEntry::relX)
                .thenComparingInt(DanceFloorNetwork.TileEntry::relZ));
        return entries;
    }

    public static BlockPos findTileAt(Level level, UUID networkId, int relX, int relZ) {
        for (DanceFloorNetwork.TileEntry entry : buildLayout(level, networkId)) {
            if (entry.relX() == relX && entry.relZ() == relZ) {
                return entry.pos();
            }
        }
        return null;
    }

    public static void syncNetwork(ServerLevel level, UUID networkId) {
        for (BlockPos pos : getMembers(networkId)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DanceFloorTileBlockEntity tile) {
                level.sendBlockUpdated(pos, tile.getBlockState(), tile.getBlockState(), 3);
            }
        }
    }

    public static UUID findAdjacentNetwork(Level level, BlockPos controllerPos) {
        for (Direction dir : Direction.values()) {
            BlockPos check = controllerPos.relative(dir);
            BlockEntity be = level.getBlockEntity(check);
            if (be instanceof DanceFloorTileBlockEntity tile) {
                return tile.getNetworkId();
            }
        }
        return null;
    }
}
