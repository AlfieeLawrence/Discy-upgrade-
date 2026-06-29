package net.discyupgrade.core.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.discyupgrade.core.floor.FloorGroups;
import net.discyupgrade.core.floor.FloorGroupIndex;
import net.discyupgrade.core.registry.ModMenuRegistry;

import java.util.*;

public class LightControllerMenu extends AbstractContainerMenu {
    public record TileView(int relX, int relZ, int color) {}
    public record FloorGroupView(UUID id, String name, List<TileView> tiles) {}
    public record LightView(BlockPos pos, boolean active) {}

    private final BlockPos controllerPos;
    private final List<FloorGroupView> floorGroups;
    private final List<LightView> discoBalls;
    private final List<LightView> lasers;
    private UUID selectedFloorGroup;
    private boolean discoSpinEnabled;

    public LightControllerMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, buf.readBlockPos(), readFloorGroups(buf), readLights(buf),
                readLights(buf), buf.readBoolean() ? buf.readUUID() : null, buf.readBoolean());
    }

    public LightControllerMenu(int containerId, Inventory playerInv, BlockPos controllerPos,
                               List<FloorGroupView> floorGroups, List<LightView> discoBalls,
                               List<LightView> lasers, UUID selectedFloorGroup, boolean discoSpinEnabled) {
        super(ModMenuRegistry.LIGHT_CONTROLLER.get(), containerId);
        this.controllerPos = controllerPos;
        this.floorGroups = floorGroups == null ? new ArrayList<>() : new ArrayList<>(floorGroups);
        this.discoBalls = discoBalls == null ? new ArrayList<>() : new ArrayList<>(discoBalls);
        this.lasers = lasers == null ? new ArrayList<>() : new ArrayList<>(lasers);
        this.selectedFloorGroup = selectedFloorGroup;
        this.discoSpinEnabled = discoSpinEnabled;
    }

    public static void writeOpeningData(FriendlyByteBuf buf, BlockPos pos, List<FloorGroupView> floors,
                                        List<LightView> discos, List<LightView> lasers,
                                        UUID selected, boolean discoSpin) {
        buf.writeBlockPos(pos);
        writeFloorGroups(buf, floors);
        writeLights(buf, discos);
        writeLights(buf, lasers);
        if (selected != null) {
            buf.writeBoolean(true);
            buf.writeUUID(selected);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeBoolean(discoSpin);
    }

    private static List<FloorGroupView> readFloorGroups(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<FloorGroupView> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf();
            list.add(new FloorGroupView(id, name, readTiles(buf)));
        }
        return list;
    }

    private static void writeFloorGroups(FriendlyByteBuf buf, List<FloorGroupView> groups) {
        buf.writeVarInt(groups.size());
        for (FloorGroupView g : groups) {
            buf.writeUUID(g.id());
            buf.writeUtf(g.name());
            writeTiles(buf, g.tiles());
        }
    }

    private static List<TileView> readTiles(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<TileView> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new TileView(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return list;
    }

    private static void writeTiles(FriendlyByteBuf buf, List<TileView> tiles) {
        buf.writeVarInt(tiles.size());
        for (TileView t : tiles) {
            buf.writeVarInt(t.relX());
            buf.writeVarInt(t.relZ());
            buf.writeVarInt(t.color());
        }
    }

    private static List<LightView> readLights(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<LightView> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new LightView(buf.readBlockPos(), buf.readBoolean()));
        }
        return list;
    }

    private static void writeLights(FriendlyByteBuf buf, List<LightView> lights) {
        buf.writeVarInt(lights.size());
        for (LightView l : lights) {
            buf.writeBlockPos(l.pos());
            buf.writeBoolean(l.active());
        }
    }

    public static List<FloorGroupView> buildFloorViews(net.minecraft.world.level.Level level,
                                                       List<UUID> groupIds) {
        List<FloorGroupView> views = new ArrayList<>();
        for (UUID id : groupIds) {
            List<TileView> tiles = new ArrayList<>();
            for (FloorGroups.TileEntry e : FloorGroups.buildLayout(level, id)) {
                tiles.add(new TileView(e.relX(), e.relZ(), e.color()));
            }
            views.add(new FloorGroupView(id, FloorGroupIndex.getGroupName(id), tiles));
        }
        return views;
    }

    public BlockPos getControllerPos() { return controllerPos; }
    public List<FloorGroupView> getFloorGroups() { return floorGroups; }
    public List<LightView> getDiscoBalls() { return discoBalls; }
    public List<LightView> getLasers() { return lasers; }
    public UUID getSelectedFloorGroup() { return selectedFloorGroup; }
    public boolean isDiscoSpinEnabled() { return discoSpinEnabled; }

    public void setSelectedFloorGroup(UUID id) { this.selectedFloorGroup = id; }

    public Optional<FloorGroupView> getSelectedFloorView() {
        for (FloorGroupView g : floorGroups) {
            if (g.id().equals(selectedFloorGroup)) return Optional.of(g);
        }
        return floorGroups.isEmpty() ? Optional.empty() : Optional.of(floorGroups.get(0));
    }

    public void updateTileColor(UUID groupId, int relX, int relZ, int color) {
        for (int i = 0; i < floorGroups.size(); i++) {
            FloorGroupView g = floorGroups.get(i);
            if (!g.id().equals(groupId)) continue;
            List<TileView> tiles = new ArrayList<>(g.tiles());
            for (int j = 0; j < tiles.size(); j++) {
                TileView t = tiles.get(j);
                if (t.relX() == relX && t.relZ() == relZ) {
                    tiles.set(j, new TileView(relX, relZ, color & 0xFFFFFF));
                    floorGroups.set(i, new FloorGroupView(g.id(), g.name(), tiles));
                    return;
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(controllerPos.getX() + 0.5, controllerPos.getY() + 0.5, controllerPos.getZ() + 0.5) <= 64;
    }
}
