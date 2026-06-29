package net.discyupgrade.core.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.DanceFloorTileBlockEntity;
import net.discyupgrade.core.block.DiscoBallBlockEntity;
import net.discyupgrade.core.block.LaserEmitterBlockEntity;
import net.discyupgrade.core.block.LightControllerBlockEntity;
import net.discyupgrade.core.floor.FloorGroups;
import net.discyupgrade.core.floor.FloorGroupIndex;
import net.discyupgrade.core.item.TuningWrenchItem;
import net.discyupgrade.core.screen.LightControllerMenu;
import net.discyupgrade.core.util.ModIdentifier;

import java.util.UUID;

public final class DiscyUpgradeNetworking {
    public static final ResourceLocation SET_TILE_COLOR = ModIdentifier.of("set_tile_color");
    public static final ResourceLocation APPLY_ALL_COLOR = ModIdentifier.of("apply_all_color");
    public static final ResourceLocation SELECT_FLOOR_GROUP = ModIdentifier.of("select_floor_group");
    public static final ResourceLocation RENAME_FLOOR_GROUP = ModIdentifier.of("rename_floor_group");
    public static final ResourceLocation UNLINK_FLOOR_GROUP = ModIdentifier.of("unlink_floor_group");
    public static final ResourceLocation SET_WRENCH_GROUP = ModIdentifier.of("set_wrench_group");
    public static final ResourceLocation TOGGLE_DISCO_SPIN = ModIdentifier.of("toggle_disco_spin");
    public static final ResourceLocation TOGGLE_ALL_DISCOS = ModIdentifier.of("toggle_all_discos");
    public static final ResourceLocation TOGGLE_DISCO = ModIdentifier.of("toggle_disco");
    public static final ResourceLocation TOGGLE_LASER = ModIdentifier.of("toggle_laser");
    public static final ResourceLocation REFRESH_CONTROLLER = ModIdentifier.of("refresh_controller");

    private DiscyUpgradeNetworking() {}

    public static void init() {
        register(SET_TILE_COLOR, (player, buf) -> {
            BlockPos pos = buf.readBlockPos();
            UUID groupId = buf.readUUID();
            int relX = buf.readVarInt();
            int relZ = buf.readVarInt();
            int color = buf.readVarInt();
            handleSetTileColor(player, pos, groupId, relX, relZ, color);
        });
        register(APPLY_ALL_COLOR, (player, buf) -> {
            handleApplyAllColor(player, buf.readBlockPos(), buf.readUUID(), buf.readVarInt());
        });
        register(SELECT_FLOOR_GROUP, (player, buf) -> {
            LightControllerBlockEntity c = getController(player, buf.readBlockPos());
            if (c != null) c.setSelectedFloorGroup(buf.readUUID());
        });
        register(RENAME_FLOOR_GROUP, (player, buf) -> {
            UUID groupId = buf.readUUID();
            String name = buf.readUtf(32);
            FloorGroupIndex.setGroupName(groupId, name);
        });
        register(UNLINK_FLOOR_GROUP, (player, buf) -> {
            LightControllerBlockEntity c = getController(player, buf.readBlockPos());
            if (c != null) c.unlinkFloorGroup(buf.readUUID());
        });
        register(SET_WRENCH_GROUP, (player, buf) -> {
            TuningWrenchItem.setWrenchGroupFromController(player, buf.readUUID());
        });
        register(TOGGLE_DISCO_SPIN, (player, buf) -> {
            LightControllerBlockEntity c = getController(player, buf.readBlockPos());
            if (c != null) c.setDiscoSpinEnabled(buf.readBoolean());
        });
        register(TOGGLE_ALL_DISCOS, (player, buf) -> {
            LightControllerBlockEntity c = getController(player, buf.readBlockPos());
            if (c != null) c.toggleAllDiscos();
        });
        register(TOGGLE_DISCO, (player, buf) -> {
            LightControllerBlockEntity c = getController(player, buf.readBlockPos());
            BlockPos discoPos = buf.readBlockPos();
            if (c == null || !c.getLinkedDiscoBalls().contains(discoPos)) return;
            BlockEntity be = player.level().getBlockEntity(discoPos);
            if (be instanceof DiscoBallBlockEntity ball) {
                ball.toggleActive();
                player.level().sendBlockUpdated(discoPos, ball.getBlockState(), ball.getBlockState(), 3);
            }
        });
        register(TOGGLE_LASER, (player, buf) -> {
            LightControllerBlockEntity c = getController(player, buf.readBlockPos());
            BlockPos laserPos = buf.readBlockPos();
            if (c == null || !c.getLinkedLasers().contains(laserPos)) return;
            BlockEntity be = player.level().getBlockEntity(laserPos);
            if (be instanceof LaserEmitterBlockEntity laser) {
                laser.setPowered(!laser.isPowered());
                player.level().sendBlockUpdated(laserPos, laser.getBlockState(), laser.getBlockState(), 3);
            }
        });
        register(REFRESH_CONTROLLER, (player, buf) -> {
            LightControllerBlockEntity c = getController(player, buf.readBlockPos());
            if (c != null) {
                c.linkTouchingFloors();
                c.pruneBrokenLinks();
                for (UUID id : c.getLinkedFloorGroups()) {
                    if (player.level() instanceof ServerLevel sl) FloorGroupIndex.rebuildFromLevel(sl, id);
                }
            }
        });
    }

    private interface PacketHandler {
        void handle(ServerPlayer player, FriendlyByteBuf buf);
    }

    private static void register(ResourceLocation id, PacketHandler handler) {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, id, (buf, context) -> {
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer sp) {
                context.queue(() -> handler.handle(sp, buf));
            }
        });
    }

    private static LightControllerBlockEntity getController(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64 * 64) return null;
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof LightControllerBlockEntity c ? c : null;
    }

    private static boolean controllerHasGroup(LightControllerBlockEntity c, UUID groupId) {
        return c.getLinkedFloorGroups().contains(groupId);
    }

    private static void handleSetTileColor(ServerPlayer player, BlockPos controllerPos, UUID groupId,
                                             int relX, int relZ, int color) {
        LightControllerBlockEntity c = getController(player, controllerPos);
        if (c == null || !controllerHasGroup(c, groupId)) return;
        BlockPos tilePos = FloorGroups.findTileAt(player.level(), groupId, relX, relZ);
        if (tilePos == null) return;
        BlockEntity be = player.level().getBlockEntity(tilePos);
        if (be instanceof DanceFloorTileBlockEntity tile) {
            tile.setColor(color & 0xFFFFFF);
            player.level().sendBlockUpdated(tilePos, tile.getBlockState(), tile.getBlockState(), 3);
            if (player.containerMenu instanceof LightControllerMenu menu && menu.getControllerPos().equals(controllerPos)) {
                menu.updateTileColor(groupId, relX, relZ, color);
            }
        }
    }

    private static void handleApplyAllColor(ServerPlayer player, BlockPos controllerPos, UUID groupId, int color) {
        LightControllerBlockEntity c = getController(player, controllerPos);
        if (c == null || !controllerHasGroup(c, groupId)) return;
        for (var entry : FloorGroups.buildLayout(player.level(), groupId)) {
            BlockEntity be = player.level().getBlockEntity(entry.pos());
            if (be instanceof DanceFloorTileBlockEntity tile) {
                tile.setColor(color & 0xFFFFFF);
                player.level().sendBlockUpdated(entry.pos(), tile.getBlockState(), tile.getBlockState(), 3);
            }
        }
        if (player.containerMenu instanceof LightControllerMenu menu && menu.getControllerPos().equals(controllerPos)) {
            for (var t : menu.getFloorGroups()) {
                if (!t.id().equals(groupId)) continue;
                for (var tile : t.tiles()) menu.updateTileColor(groupId, tile.relX(), tile.relZ(), color);
            }
        }
    }

    public static void sendSetTileColor(BlockPos controllerPos, UUID groupId, int relX, int relZ, int color) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        buf.writeUUID(groupId);
        buf.writeVarInt(relX);
        buf.writeVarInt(relZ);
        buf.writeVarInt(color);
        NetworkManager.sendToServer(SET_TILE_COLOR, buf);
    }

    public static void sendApplyAllColor(BlockPos controllerPos, UUID groupId, int color) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        buf.writeUUID(groupId);
        buf.writeVarInt(color);
        NetworkManager.sendToServer(APPLY_ALL_COLOR, buf);
    }

    public static void sendSelectFloorGroup(BlockPos controllerPos, UUID groupId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        buf.writeUUID(groupId);
        NetworkManager.sendToServer(SELECT_FLOOR_GROUP, buf);
    }

    public static void sendRenameFloorGroup(UUID groupId, String name) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(groupId);
        buf.writeUtf(name, 32);
        NetworkManager.sendToServer(RENAME_FLOOR_GROUP, buf);
    }

    public static void sendUnlinkFloorGroup(BlockPos controllerPos, UUID groupId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        buf.writeUUID(groupId);
        NetworkManager.sendToServer(UNLINK_FLOOR_GROUP, buf);
    }

    public static void sendSetWrenchGroup(UUID groupId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(groupId);
        NetworkManager.sendToServer(SET_WRENCH_GROUP, buf);
    }

    public static void sendToggleDiscoSpin(BlockPos controllerPos, boolean enabled) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        buf.writeBoolean(enabled);
        NetworkManager.sendToServer(TOGGLE_DISCO_SPIN, buf);
    }

    public static void sendToggleAllDiscos(BlockPos controllerPos) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        NetworkManager.sendToServer(TOGGLE_ALL_DISCOS, buf);
    }

    public static void sendToggleDisco(BlockPos controllerPos, BlockPos discoPos) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        buf.writeBlockPos(discoPos);
        NetworkManager.sendToServer(TOGGLE_DISCO, buf);
    }

    public static void sendToggleLaser(BlockPos controllerPos, BlockPos laserPos) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        buf.writeBlockPos(laserPos);
        NetworkManager.sendToServer(TOGGLE_LASER, buf);
    }

    public static void sendRefresh(BlockPos controllerPos) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(controllerPos);
        NetworkManager.sendToServer(REFRESH_CONTROLLER, buf);
    }
}
