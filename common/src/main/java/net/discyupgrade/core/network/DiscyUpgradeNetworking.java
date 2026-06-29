package net.discyupgrade.core.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.DanceFloorControllerBlockEntity;
import net.discyupgrade.core.block.DanceFloorTileBlockEntity;
import net.discyupgrade.core.floor.DanceFloorNetwork;
import net.discyupgrade.core.floor.DanceFloorNetworkIndex;
import net.discyupgrade.core.screen.DanceFloorControllerMenu;
import net.discyupgrade.core.util.ModIdentifier;

import java.util.UUID;

public final class DiscyUpgradeNetworking {
    public static final ResourceLocation SET_TILE_COLOR = ModIdentifier.of("set_tile_color");
    public static final ResourceLocation APPLY_ALL_COLOR = ModIdentifier.of("apply_all_color");
    public static final ResourceLocation REFRESH_CONTROLLER = ModIdentifier.of("refresh_controller");

    private DiscyUpgradeNetworking() {}

    public static void init() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SET_TILE_COLOR, (buf, context) -> {
            BlockPos controllerPos = buf.readBlockPos();
            int relX = buf.readVarInt();
            int relZ = buf.readVarInt();
            int color = buf.readVarInt();
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleSetTileColor(serverPlayer, controllerPos, relX, relZ, color));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, APPLY_ALL_COLOR, (buf, context) -> {
            BlockPos controllerPos = buf.readBlockPos();
            int color = buf.readVarInt();
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleApplyAllColor(serverPlayer, controllerPos, color));
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REFRESH_CONTROLLER, (buf, context) -> {
            BlockPos controllerPos = buf.readBlockPos();
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer serverPlayer) {
                context.queue(() -> handleRefresh(serverPlayer, controllerPos));
            }
        });
    }

    private static DanceFloorControllerBlockEntity getController(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64 * 64) {
            return null;
        }
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof DanceFloorControllerBlockEntity controller ? controller : null;
    }

    private static void handleSetTileColor(ServerPlayer player, BlockPos controllerPos, int relX, int relZ, int color) {
        DanceFloorControllerBlockEntity controller = getController(player, controllerPos);
        if (controller == null) return;

        UUID networkId = controller.getLinkedNetworkId();
        if (networkId == null) return;

        BlockPos tilePos = DanceFloorNetwork.findTileAt(player.level(), networkId, relX, relZ);
        if (tilePos == null) return;

        BlockEntity be = player.level().getBlockEntity(tilePos);
        if (be instanceof DanceFloorTileBlockEntity tile) {
            tile.setColor(color & 0xFFFFFF);
            player.level().sendBlockUpdated(tilePos, tile.getBlockState(), tile.getBlockState(), 3);
            if (player.containerMenu instanceof DanceFloorControllerMenu menu
                    && menu.getControllerPos().equals(controllerPos)) {
                menu.updateTileColor(relX, relZ, color);
            }
        }
    }

    private static void handleApplyAllColor(ServerPlayer player, BlockPos controllerPos, int color) {
        DanceFloorControllerBlockEntity controller = getController(player, controllerPos);
        if (controller == null) return;

        UUID networkId = controller.getLinkedNetworkId();
        if (networkId == null) return;

        for (var entry : DanceFloorNetwork.buildLayout(player.level(), networkId)) {
            BlockEntity be = player.level().getBlockEntity(entry.pos());
            if (be instanceof DanceFloorTileBlockEntity tile) {
                tile.setColor(color & 0xFFFFFF);
                player.level().sendBlockUpdated(entry.pos(), tile.getBlockState(), tile.getBlockState(), 3);
            }
        }

        if (player.containerMenu instanceof DanceFloorControllerMenu menu
                && menu.getControllerPos().equals(controllerPos)) {
            for (int i = 0; i < menu.getTiles().size(); i++) {
                var t = menu.getTiles().get(i);
                menu.updateTileColor(t.relX(), t.relZ(), color);
            }
        }
    }

    private static void handleRefresh(ServerPlayer player, BlockPos controllerPos) {
        DanceFloorControllerBlockEntity controller = getController(player, controllerPos);
        if (controller == null) return;
        UUID networkId = controller.getLinkedNetworkId();
        if (networkId == null) {
            networkId = DanceFloorNetwork.findAdjacentNetwork(player.level(), controllerPos);
            if (networkId != null) controller.setLinkedNetworkId(networkId);
        }
        if (networkId != null && player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            DanceFloorNetworkIndex.rebuildFromLevel(sl, networkId);
        }
    }

    public static void sendSetTileColor(BlockPos controllerPos, int relX, int relZ, int color) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        buf.writeVarInt(relX);
        buf.writeVarInt(relZ);
        buf.writeVarInt(color);
        NetworkManager.sendToServer(SET_TILE_COLOR, buf);
    }

    public static void sendApplyAllColor(BlockPos controllerPos, int color) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        buf.writeVarInt(color);
        NetworkManager.sendToServer(APPLY_ALL_COLOR, buf);
    }

    public static void sendRefresh(BlockPos controllerPos) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        NetworkManager.sendToServer(REFRESH_CONTROLLER, buf);
    }
}
