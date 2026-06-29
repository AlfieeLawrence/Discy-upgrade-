package net.discyupgrade.core.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.discyupgrade.core.floor.DanceFloorNetwork;
import net.discyupgrade.core.registry.ModMenuRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DanceFloorControllerMenu extends AbstractContainerMenu {
    public record TileView(int relX, int relZ, int color) {}

    private final BlockPos controllerPos;
    private final UUID networkId;
    private final List<TileView> tiles;

    public DanceFloorControllerMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, buf.readBlockPos(), buf.readUUID(), readTiles(buf));
    }

    public DanceFloorControllerMenu(int containerId, Inventory playerInv, BlockPos controllerPos,
                                    UUID networkId, List<TileView> tiles) {
        super(ModMenuRegistry.DANCE_FLOOR_CONTROLLER.get(), containerId);
        this.controllerPos = controllerPos;
        this.networkId = networkId;
        this.tiles = tiles == null ? new ArrayList<>() : new ArrayList<>(tiles);
    }

    private static List<TileView> readTiles(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<TileView> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new TileView(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return list;
    }

    public static void writeTiles(FriendlyByteBuf buf, List<TileView> tiles) {
        buf.writeVarInt(tiles.size());
        for (TileView tile : tiles) {
            buf.writeVarInt(tile.relX());
            buf.writeVarInt(tile.relZ());
            buf.writeVarInt(tile.color());
        }
    }

    public static List<TileView> fromNetwork(net.minecraft.world.level.Level level, UUID networkId) {
        List<TileView> views = new ArrayList<>();
        for (DanceFloorNetwork.TileEntry entry : DanceFloorNetwork.buildLayout(level, networkId)) {
            views.add(new TileView(entry.relX(), entry.relZ(), entry.color()));
        }
        return views;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public List<TileView> getTiles() {
        return tiles;
    }

    public void updateTileColor(int relX, int relZ, int color) {
        for (int i = 0; i < tiles.size(); i++) {
            TileView tile = tiles.get(i);
            if (tile.relX() == relX && tile.relZ() == relZ) {
                tiles.set(i, new TileView(relX, relZ, color & 0xFFFFFF));
                return;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                controllerPos.getX() + 0.5,
                controllerPos.getY() + 0.5,
                controllerPos.getZ() + 0.5) <= 64.0;
    }
}
