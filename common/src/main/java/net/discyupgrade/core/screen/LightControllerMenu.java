package net.discyupgrade.core.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.discyupgrade.core.block.LightControllerBlockEntity;
import net.discyupgrade.core.floor.FloorGroupIndex;
import net.discyupgrade.core.floor.FloorGroups;
import net.discyupgrade.core.floor.FloorPattern;
import net.discyupgrade.core.registry.ModMenuRegistry;

import java.util.*;

public class LightControllerMenu extends AbstractContainerMenu {
    public record TileView(int relX, int relZ, int color) {}
    public record GroupAnimView(FloorPattern pattern, int speed, boolean playing, boolean syncDisco) {}
    public record FloorGroupView(UUID id, String name, List<TileView> tiles, GroupAnimView anim) {}
    public record LightView(BlockPos pos, boolean active) {}

    private final BlockPos controllerPos;
    private final List<FloorGroupView> floorGroups;
    private final List<LightView> discoBalls;
    private final List<LightView> lasers;
    private final List<LightView> partyLights;
    private final List<LightView> strobes;
    private final List<String> presetNames;
    private UUID selectedFloorGroup;
    private boolean discoSpinEnabled;

    public LightControllerMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, buf.readBlockPos(), readFloorGroups(buf), readLights(buf), readLights(buf),
                readLights(buf), readLights(buf), readPresetNames(buf),
                buf.readBoolean() ? buf.readUUID() : null, buf.readBoolean());
    }

    public LightControllerMenu(int containerId, Inventory playerInv, BlockPos controllerPos,
                               List<FloorGroupView> floorGroups, List<LightView> discoBalls, List<LightView> lasers,
                               List<LightView> partyLights, List<LightView> strobes, List<String> presetNames,
                               UUID selectedFloorGroup, boolean discoSpinEnabled) {
        super(ModMenuRegistry.LIGHT_CONTROLLER.get(), containerId);
        this.controllerPos = controllerPos;
        this.floorGroups = floorGroups == null ? new ArrayList<>() : new ArrayList<>(floorGroups);
        this.discoBalls = discoBalls == null ? new ArrayList<>() : new ArrayList<>(discoBalls);
        this.lasers = lasers == null ? new ArrayList<>() : new ArrayList<>(lasers);
        this.partyLights = partyLights == null ? new ArrayList<>() : new ArrayList<>(partyLights);
        this.strobes = strobes == null ? new ArrayList<>() : new ArrayList<>(strobes);
        this.presetNames = presetNames == null ? new ArrayList<>() : new ArrayList<>(presetNames);
        this.selectedFloorGroup = selectedFloorGroup;
        this.discoSpinEnabled = discoSpinEnabled;
    }

    public static void writeOpeningData(FriendlyByteBuf buf, BlockPos pos, List<FloorGroupView> floors,
                                        List<LightView> discos, List<LightView> lasers, List<LightView> parties,
                                        List<LightView> strobes, List<String> presets, UUID selected, boolean discoSpin) {
        buf.writeBlockPos(pos);
        writeFloorGroups(buf, floors);
        writeLights(buf, discos);
        writeLights(buf, lasers);
        writeLights(buf, parties);
        writeLights(buf, strobes);
        buf.writeVarInt(presets.size());
        for (String p : presets) buf.writeUtf(p);
        if (selected != null) { buf.writeBoolean(true); buf.writeUUID(selected); } else buf.writeBoolean(false);
        buf.writeBoolean(discoSpin);
    }

    private static List<FloorGroupView> readFloorGroups(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<FloorGroupView> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf();
            List<TileView> tiles = readTiles(buf);
            GroupAnimView anim = new GroupAnimView(FloorPattern.fromId(buf.readVarInt()), buf.readVarInt(),
                    buf.readBoolean(), buf.readBoolean());
            list.add(new FloorGroupView(id, name, tiles, anim));
        }
        return list;
    }

    private static void writeFloorGroups(FriendlyByteBuf buf, List<FloorGroupView> groups) {
        buf.writeVarInt(groups.size());
        for (FloorGroupView g : groups) {
            buf.writeUUID(g.id());
            buf.writeUtf(g.name());
            writeTiles(buf, g.tiles());
            buf.writeVarInt(g.anim().pattern().id());
            buf.writeVarInt(g.anim().speed());
            buf.writeBoolean(g.anim().playing());
            buf.writeBoolean(g.anim().syncDisco());
        }
    }

    private static List<TileView> readTiles(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<TileView> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(new TileView(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        return list;
    }

    private static void writeTiles(FriendlyByteBuf buf, List<TileView> tiles) {
        buf.writeVarInt(tiles.size());
        for (TileView t : tiles) { buf.writeVarInt(t.relX()); buf.writeVarInt(t.relZ()); buf.writeVarInt(t.color()); }
    }

    private static List<LightView> readLights(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<LightView> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(new LightView(buf.readBlockPos(), buf.readBoolean()));
        return list;
    }

    private static void writeLights(FriendlyByteBuf buf, List<LightView> lights) {
        buf.writeVarInt(lights.size());
        for (LightView l : lights) { buf.writeBlockPos(l.pos()); buf.writeBoolean(l.active()); }
    }

    private static List<String> readPresetNames(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) list.add(buf.readUtf());
        return list;
    }

    public static List<FloorGroupView> buildFloorViews(net.minecraft.world.level.Level level, LightControllerBlockEntity controller) {
        List<FloorGroupView> views = new ArrayList<>();
        for (UUID id : controller.getLinkedFloorGroups()) {
            List<TileView> tiles = new ArrayList<>();
            for (FloorGroups.TileEntry e : FloorGroups.buildLayout(level, id)) {
                tiles.add(new TileView(e.relX(), e.relZ(), e.color()));
            }
            var anim = controller.getGroupSettings(id);
            views.add(new FloorGroupView(id, FloorGroupIndex.getGroupName(id), tiles,
                    new GroupAnimView(anim.pattern(), anim.speed(), anim.playing(), anim.syncDisco())));
        }
        return views;
    }

    public BlockPos getControllerPos() { return controllerPos; }
    public List<FloorGroupView> getFloorGroups() { return floorGroups; }
    public List<LightView> getDiscoBalls() { return discoBalls; }
    public List<LightView> getLasers() { return lasers; }
    public List<LightView> getPartyLights() { return partyLights; }
    public List<LightView> getStrobes() { return strobes; }
    public List<String> getPresetNames() { return presetNames; }
    public UUID getSelectedFloorGroup() { return selectedFloorGroup; }
    public boolean isDiscoSpinEnabled() { return discoSpinEnabled; }

    public void updateTileColor(UUID groupId, int relX, int relZ, int color) {
        for (int i = 0; i < floorGroups.size(); i++) {
            FloorGroupView g = floorGroups.get(i);
            if (!g.id().equals(groupId)) continue;
            List<TileView> tiles = new ArrayList<>(g.tiles());
            for (int j = 0; j < tiles.size(); j++) {
                TileView t = tiles.get(j);
                if (t.relX() == relX && t.relZ() == relZ) {
                    tiles.set(j, new TileView(relX, relZ, color & 0xFFFFFF));
                    floorGroups.set(i, new FloorGroupView(g.id(), g.name(), tiles, g.anim()));
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
