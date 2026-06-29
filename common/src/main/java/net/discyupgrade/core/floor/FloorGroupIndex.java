package net.discyupgrade.core.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.DanceFloorTileBlockEntity;

import java.util.*;

/**
 * Named floor groups — tiles join when adjacent, or via tuning wrench.
 */
public final class FloorGroupIndex {
    public record FloorGroup(UUID id, String name, Set<BlockPos> tiles) {}

    public record TileEntry(BlockPos pos, int relX, int relZ, int color) {}

    private static final Map<UUID, FloorGroup> GROUPS = new HashMap<>();
    private static int unnamedCounter = 1;

    private FloorGroupIndex() {}

    public static UUID createGroup(String name) {
        UUID id = UUID.randomUUID();
        String resolved = name == null || name.isBlank()
                ? Component.translatable("floor.discyupgrade.unnamed", unnamedCounter++).getString()
                : name;
        GROUPS.put(id, new FloorGroup(id, resolved, new HashSet<>()));
        return id;
    }

    public static String getGroupName(UUID groupId) {
        FloorGroup g = GROUPS.get(groupId);
        return g == null ? "?" : g.name();
    }

    public static void setGroupName(UUID groupId, String name) {
        if (groupId == null || name == null || name.isBlank()) return;
        FloorGroup g = GROUPS.get(groupId);
        if (g == null) return;
        GROUPS.put(groupId, new FloorGroup(groupId, name.trim(), g.tiles()));
    }

    public static Set<UUID> allGroupIds() {
        return Collections.unmodifiableSet(GROUPS.keySet());
    }

    public static void register(ServerLevel level, BlockPos pos, UUID groupId) {
        if (groupId == null) return;
        FloorGroup g = GROUPS.get(groupId);
        if (g == null) {
            GROUPS.put(groupId, new FloorGroup(groupId,
                    Component.translatable("floor.discyupgrade.unnamed", unnamedCounter++).getString(),
                    new HashSet<>(Set.of(pos.immutable()))));
        } else {
            g.tiles().add(pos.immutable());
        }
    }

    public static void unregister(BlockPos pos, UUID groupId) {
        if (groupId == null) return;
        FloorGroup g = GROUPS.get(groupId);
        if (g != null) {
            g.tiles().remove(pos);
            if (g.tiles().isEmpty()) GROUPS.remove(groupId);
        }
    }

    public static List<BlockPos> getMembers(UUID groupId) {
        FloorGroup g = GROUPS.get(groupId);
        return g == null ? List.of() : new ArrayList<>(g.tiles());
    }

    public static UUID getTileGroup(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DanceFloorTileBlockEntity tile) {
            return tile.getGroupId();
        }
        return null;
    }

    public static void onTilePlaced(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DanceFloorTileBlockEntity tile)) return;

        Set<UUID> neighborGroups = new HashSet<>();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockEntity nbe = level.getBlockEntity(pos.relative(dir));
            if (nbe instanceof DanceFloorTileBlockEntity neighbor) {
                UUID id = neighbor.getGroupId();
                if (id != null) neighborGroups.add(id);
            }
        }

        if (neighborGroups.isEmpty()) {
            UUID newId = createGroup(null);
            applyGroup(level, tile, pos, newId);
            return;
        }

        UUID merged = neighborGroups.iterator().next();
        for (UUID other : neighborGroups) {
            if (!other.equals(merged)) mergeGroups(other, merged);
        }
        applyGroup(level, tile, pos, merged);
    }

    public static UUID disconnectTile(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DanceFloorTileBlockEntity tile)) return null;

        UUID old = tile.getGroupId();
        unregister(pos, old);

        UUID solo = createGroup(Component.translatable("floor.discyupgrade.isolated").getString());
        applyGroup(level, tile, pos, solo);
        splitDisconnectedClusters(level, old);
        return solo;
    }

    public static void assignTileToGroup(ServerLevel level, BlockPos pos, UUID groupId) {
        if (groupId == null || !GROUPS.containsKey(groupId)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof DanceFloorTileBlockEntity tile)) return;

        UUID old = tile.getGroupId();
        if (groupId.equals(old)) return;

        unregister(pos, old);
        applyGroup(level, tile, pos, groupId);
        if (old != null) splitDisconnectedClusters(level, old);
        syncGroup(level, groupId);
    }

    public static void onTileRemoved(ServerLevel level, BlockPos removedPos, UUID groupId) {
        unregister(removedPos, groupId);
        if (groupId == null) return;
        splitDisconnectedClusters(level, groupId);
    }

    private static void applyGroup(ServerLevel level, DanceFloorTileBlockEntity tile, BlockPos pos, UUID groupId) {
        tile.setGroupId(groupId);
        register(level, pos, groupId);
        tile.setChanged();
        syncGroup(level, groupId);
    }

    private static void mergeGroups(UUID from, UUID into) {
        FloorGroup fromGroup = GROUPS.remove(from);
        if (fromGroup == null) return;
        FloorGroup intoGroup = GROUPS.computeIfAbsent(into,
                id -> new FloorGroup(id, Component.translatable("floor.discyupgrade.unnamed", unnamedCounter++).getString(), new HashSet<>()));
        intoGroup.tiles().addAll(fromGroup.tiles());
    }

    private static void splitDisconnectedClusters(ServerLevel level, UUID groupId) {
        List<BlockPos> remaining = getMembers(groupId);
        if (remaining.isEmpty()) return;

        Set<BlockPos> set = new HashSet<>(remaining);
        Set<BlockPos> visited = floodFill(remaining.get(0), set);
        if (visited.size() == set.size()) {
            syncGroup(level, groupId);
            return;
        }

        GROUPS.remove(groupId);
        Set<BlockPos> unassigned = new HashSet<>(set);
        while (!unassigned.isEmpty()) {
            BlockPos start = unassigned.iterator().next();
            Set<BlockPos> component = floodFill(start, unassigned);
            UUID newId = createGroup(null);
            Set<BlockPos> registered = GROUPS.computeIfAbsent(newId,
                    id -> new FloorGroup(id, Component.translatable("floor.discyupgrade.unnamed", unnamedCounter++).getString(), new HashSet<>())).tiles();
            for (BlockPos p : component) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof DanceFloorTileBlockEntity tile) {
                    tile.setGroupId(newId);
                    tile.setChanged();
                    registered.add(p.immutable());
                }
                unassigned.remove(p);
            }
            syncGroup(level, newId);
        }
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
                if (allowed.contains(next) && visited.add(next)) queue.add(next);
            }
        }
        return visited;
    }

    public static List<TileEntry> buildLayout(Level level, UUID groupId) {
        List<BlockPos> tiles = getMembers(groupId);
        if (tiles.isEmpty()) return List.of();

        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (BlockPos p : tiles) {
            minX = Math.min(minX, p.getX());
            minZ = Math.min(minZ, p.getZ());
        }

        List<TileEntry> entries = new ArrayList<>();
        for (BlockPos p : tiles) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof DanceFloorTileBlockEntity tile) {
                entries.add(new TileEntry(p, p.getX() - minX, p.getZ() - minZ, tile.getColor()));
            }
        }
        entries.sort(Comparator.comparingInt(TileEntry::relX).thenComparingInt(TileEntry::relZ));
        return entries;
    }

    public static BlockPos findTileAt(Level level, UUID groupId, int relX, int relZ) {
        for (TileEntry e : buildLayout(level, groupId)) {
            if (e.relX() == relX && e.relZ() == relZ) return e.pos();
        }
        return null;
    }

    public static void syncGroup(ServerLevel level, UUID groupId) {
        for (BlockPos pos : getMembers(groupId)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DanceFloorTileBlockEntity tile) {
                level.sendBlockUpdated(pos, tile.getBlockState(), tile.getBlockState(), 3);
            }
        }
    }

    public static void rebuildFromLevel(ServerLevel level, UUID groupId) {
        FloorGroup g = GROUPS.get(groupId);
        if (g == null) return;
        g.tiles().removeIf(pos -> {
            BlockEntity be = level.getBlockEntity(pos);
            return !(be instanceof DanceFloorTileBlockEntity tile) || !groupId.equals(tile.getGroupId());
        });
        if (g.tiles().isEmpty()) GROUPS.remove(groupId);
    }

    /** Groups touching the controller (direct, via link plate, or support block). */
    public static List<UUID> findTouchingGroups(Level level, BlockPos controllerPos) {
        Set<UUID> found = new LinkedHashSet<>();
        collectNearbyGroups(level, controllerPos, found, 0);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = controllerPos.relative(dir);
            if (net.discyupgrade.core.block.FloorLinkPlateBlock.isLinkPlate(level.getBlockState(neighbor))) {
                collectNearbyGroups(level, neighbor, found, 0);
            }
        }
        return new ArrayList<>(found);
    }

    private static void collectNearbyGroups(Level level, BlockPos origin, Set<UUID> found, int depth) {
        if (depth > 2) return;
        for (Direction dir : Direction.values()) {
            BlockPos check = origin.relative(dir);
            UUID id = getTileGroup(level, check);
            if (id != null) found.add(id);
            if (net.discyupgrade.core.block.FloorLinkPlateBlock.isLinkPlate(level.getBlockState(check))) {
                collectNearbyGroups(level, check, found, depth + 1);
            }
        }
    }
}
