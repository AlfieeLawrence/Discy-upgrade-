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
import net.discyupgrade.core.block.*;
import net.discyupgrade.core.floor.FloorGroups;
import net.discyupgrade.core.floor.FloorGroupIndex;
import net.discyupgrade.core.floor.FloorPattern;
import net.discyupgrade.core.item.TuningWrenchItem;
import net.discyupgrade.core.screen.LightControllerMenu;
import net.discyupgrade.core.screen.LightControllerMenus;
import net.discyupgrade.core.util.ModIdentifier;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class DiscyUpgradeNetworking {
    private static final AtomicInteger SYNC_SEQ = new AtomicInteger();

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
    public static final ResourceLocation TOGGLE_PARTY = ModIdentifier.of("toggle_party");
    public static final ResourceLocation TOGGLE_STROBE = ModIdentifier.of("toggle_strobe");
    public static final ResourceLocation SET_PATTERN = ModIdentifier.of("set_pattern");
    public static final ResourceLocation SET_PATTERN_SPEED = ModIdentifier.of("set_pattern_speed");
    public static final ResourceLocation TOGGLE_PATTERN = ModIdentifier.of("toggle_pattern");
    public static final ResourceLocation TOGGLE_SYNC_DISCO = ModIdentifier.of("toggle_sync_disco");
    public static final ResourceLocation TOGGLE_SYNC_JUKEBOX = ModIdentifier.of("toggle_sync_jukebox");
    public static final ResourceLocation SAVE_PRESET = ModIdentifier.of("save_preset");
    public static final ResourceLocation APPLY_PRESET = ModIdentifier.of("apply_preset");
    public static final ResourceLocation COPY_GROUP = ModIdentifier.of("copy_group");
    public static final ResourceLocation REFRESH_CONTROLLER = ModIdentifier.of("refresh_controller");
    public static final ResourceLocation SYNC_MENU = ModIdentifier.of("sync_menu");

    private DiscyUpgradeNetworking() {}

    public static void init() {
        registerC2S(SET_TILE_COLOR, (p, b) -> handleSetTileColor(p, b.readBlockPos(), b.readUUID(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
        registerC2S(APPLY_ALL_COLOR, (p, b) -> handleApplyAllColor(p, b.readBlockPos(), b.readUUID(), b.readVarInt()));
        registerC2S(SELECT_FLOOR_GROUP, (p, b) -> {
            var c = getController(p, b.readBlockPos());
            if (c != null) { c.setSelectedFloorGroup(b.readUUID()); syncMenu(p, c); }
        });
        registerC2S(RENAME_FLOOR_GROUP, (p, b) -> {
            UUID groupId = b.readUUID();
            FloorGroupIndex.setGroupName(groupId, b.readUtf(32));
            if (p.containerMenu instanceof LightControllerMenu menu) {
                BlockEntity be = p.level().getBlockEntity(menu.getControllerPos());
                if (be instanceof LightControllerBlockEntity controller) syncMenu(p, controller);
            }
        });
        registerC2S(UNLINK_FLOOR_GROUP, (p, b) -> {
            var c = getController(p, b.readBlockPos());
            if (c != null) { c.unlinkFloorGroup(b.readUUID()); syncMenu(p, c); }
        });
        registerC2S(SET_WRENCH_GROUP, (p, b) -> TuningWrenchItem.setWrenchGroupFromController(p, b.readUUID()));
        registerC2S(TOGGLE_DISCO_SPIN, (p, b) -> {
            var c = getController(p, b.readBlockPos());
            if (c != null) { c.setDiscoSpinEnabled(b.readBoolean()); syncMenu(p, c); }
        });
        registerC2S(TOGGLE_ALL_DISCOS, (p, b) -> {
            var c = getController(p, b.readBlockPos());
            if (c != null) { c.toggleAllDiscos(); syncMenu(p, c); }
        });
        registerC2S(TOGGLE_DISCO, (p, b) -> {
            BlockPos cPos = b.readBlockPos(); BlockPos target = b.readBlockPos();
            toggleDisco(p, cPos, target); syncMenu(p, getController(p, cPos));
        });
        registerC2S(TOGGLE_LASER, (p, b) -> {
            BlockPos cPos = b.readBlockPos(); BlockPos target = b.readBlockPos();
            toggleLaser(p, cPos, target); syncMenu(p, getController(p, cPos));
        });
        registerC2S(TOGGLE_PARTY, (p, b) -> {
            BlockPos cPos = b.readBlockPos(); BlockPos target = b.readBlockPos();
            toggleParty(p, cPos, target); syncMenu(p, getController(p, cPos));
        });
        registerC2S(TOGGLE_STROBE, (p, b) -> {
            BlockPos cPos = b.readBlockPos(); BlockPos target = b.readBlockPos();
            toggleStrobe(p, cPos, target); syncMenu(p, getController(p, cPos));
        });
        registerC2S(SET_PATTERN, (p, b) -> {
            BlockPos cPos = b.readBlockPos(); UUID g = b.readUUID();
            setPattern(p, cPos, g, FloorPattern.fromId(b.readVarInt())); syncMenu(p, getController(p, cPos));
        });
        registerC2S(SET_PATTERN_SPEED, (p, b) -> {
            BlockPos cPos = b.readBlockPos(); UUID g = b.readUUID();
            setSpeed(p, cPos, g, b.readVarInt()); syncMenu(p, getController(p, cPos));
        });
        registerC2S(TOGGLE_PATTERN, (p, b) -> {
            BlockPos cPos = b.readBlockPos(); UUID g = b.readUUID();
            togglePlaying(p, cPos, g, b.readBoolean()); syncMenu(p, getController(p, cPos));
        });
        registerC2S(TOGGLE_SYNC_DISCO, (p, b) -> {
            BlockPos cPos = b.readBlockPos(); UUID g = b.readUUID();
            toggleSyncDisco(p, cPos, g, b.readBoolean()); syncMenu(p, getController(p, cPos));
        });
        registerC2S(TOGGLE_SYNC_JUKEBOX, (p, b) -> {
            BlockPos cPos = b.readBlockPos(); UUID g = b.readUUID();
            toggleSyncJukebox(p, cPos, g, b.readBoolean()); syncMenu(p, getController(p, cPos));
        });
        registerC2S(SAVE_PRESET, (p, b) -> {
            var c = getController(p, b.readBlockPos());
            if (c != null) { c.savePreset(b.readUtf(24), b.readUUID()); syncMenu(p, c); }
        });
        registerC2S(APPLY_PRESET, (p, b) -> {
            var c = getController(p, b.readBlockPos());
            if (c != null) { c.applyPreset(b.readUtf(24), b.readUUID()); syncMenu(p, c); }
        });
        registerC2S(COPY_GROUP, (p, b) -> {
            var c = getController(p, b.readBlockPos());
            if (c != null) { c.copyGroupColors(b.readUUID(), b.readUUID()); syncMenu(p, c); }
        });
        registerC2S(REFRESH_CONTROLLER, (p, b) -> {
            var c = getController(p, b.readBlockPos());
            if (c != null) {
                c.linkTouchingFloors();
                c.pruneBrokenLinks();
                for (UUID id : c.getLinkedFloorGroups()) {
                    if (p.level() instanceof ServerLevel sl) FloorGroupIndex.rebuildFromLevel(sl, id);
                }
                syncMenu(p, c);
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_MENU, (buf, ctx) -> {
            ctx.queue(() -> {
                Player player = ctx.getPlayer();
                if (player.containerMenu instanceof LightControllerMenu menu) {
                    menu.applySync(buf);
                }
            });
        });
    }

    private interface PacketHandler { void handle(ServerPlayer player, FriendlyByteBuf buf); }

    private static void registerC2S(ResourceLocation id, PacketHandler handler) {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, id, (buf, ctx) -> {
            Player player = ctx.getPlayer();
            if (player instanceof ServerPlayer sp) ctx.queue(() -> handler.handle(sp, buf));
        });
    }

    public static void syncOpenMenu(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (sp.containerMenu instanceof LightControllerMenu menu) {
            BlockEntity be = sp.level().getBlockEntity(menu.getControllerPos());
            if (be instanceof LightControllerBlockEntity controller) syncMenu(sp, controller);
        }
    }

    public static void syncMenu(ServerPlayer player, LightControllerBlockEntity controller) {
        if (controller == null || player == null) return;
        if (!(player.containerMenu instanceof LightControllerMenu menu)) return;
        if (!menu.getControllerPos().equals(controller.getBlockPos())) return;

        controller.pruneBrokenLinks();
        var floors = LightControllerMenu.buildFloorViews(controller.getLevel(), controller);
        var discos = lightViews(controller, controller.getLinkedDiscoBalls(), true);
        var lasers = lightViews(controller, controller.getLinkedLasers(), false);
        var parties = lightViewsParty(controller);
        var strobes = lightViewsStrobe(controller);
        var jukeboxes = LightControllerMenu.buildJukeboxViews(controller.getLevel(), controller);
        var presets = new ArrayList<>(controller.getPresets().keySet());
        NetworkManager.sendToPlayer(player, SYNC_MENU, buildSyncBuf(controller, floors, discos, lasers, parties, strobes, jukeboxes, presets));
    }

    private static FriendlyByteBuf buildSyncBuf(LightControllerBlockEntity controller,
                                                 java.util.List<LightControllerMenu.FloorGroupView> floors,
                                                 java.util.List<LightControllerMenu.LightView> discos,
                                                 java.util.List<LightControllerMenu.LightView> lasers,
                                                 java.util.List<LightControllerMenu.LightView> parties,
                                                 java.util.List<LightControllerMenu.LightView> strobes,
                                                 java.util.List<LightControllerMenu.LightView> jukeboxes,
                                                 java.util.List<String> presets) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        LightControllerMenu.writeOpeningData(buf, controller.getBlockPos(), floors, discos, lasers, parties, strobes,
                jukeboxes, presets, controller.getSelectedFloorGroup(), controller.isDiscoSpinEnabled(),
                controller.isRedstonePowered(), SYNC_SEQ.incrementAndGet());
        return buf;
    }

    private static LightControllerBlockEntity getController(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64 * 64) return null;
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof LightControllerBlockEntity c ? c : null;
    }

    private static boolean hasGroup(LightControllerBlockEntity c, UUID groupId) {
        return c.getLinkedFloorGroups().contains(groupId);
    }

    private static java.util.List<LightControllerMenu.LightView> lightViews(LightControllerBlockEntity controller,
                                                                              java.util.List<BlockPos> positions, boolean disco) {
        java.util.List<LightControllerMenu.LightView> list = new ArrayList<>();
        for (var pos : positions) {
            BlockEntity be = controller.getLevel().getBlockEntity(pos);
            boolean active = disco
                    ? be instanceof DiscoBallBlockEntity ball && ball.isActive()
                    : be instanceof LaserEmitterBlockEntity laser && laser.isPowered();
            list.add(new LightControllerMenu.LightView(pos, active));
        }
        return list;
    }

    private static java.util.List<LightControllerMenu.LightView> lightViewsParty(LightControllerBlockEntity controller) {
        java.util.List<LightControllerMenu.LightView> list = new ArrayList<>();
        for (var pos : controller.getLinkedPartyLights()) {
            BlockEntity be = controller.getLevel().getBlockEntity(pos);
            list.add(new LightControllerMenu.LightView(pos, be instanceof PartyLightBlockEntity p && p.isPowered()));
        }
        return list;
    }

    private static java.util.List<LightControllerMenu.LightView> lightViewsStrobe(LightControllerBlockEntity controller) {
        java.util.List<LightControllerMenu.LightView> list = new ArrayList<>();
        for (var pos : controller.getLinkedStrobes()) {
            BlockEntity be = controller.getLevel().getBlockEntity(pos);
            list.add(new LightControllerMenu.LightView(pos, be instanceof StrobeLightBlockEntity s && s.isPowered()));
        }
        return list;
    }

    private static void handleSetTileColor(ServerPlayer player, BlockPos controllerPos, UUID groupId, int relX, int relZ, int color) {
        var c = getController(player, controllerPos);
        if (c == null || !hasGroup(c, groupId)) return;
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
        var c = getController(player, controllerPos);
        if (c == null || !hasGroup(c, groupId)) return;
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
        syncMenu(player, c);
    }

    private static void toggleDisco(ServerPlayer player, BlockPos controllerPos, BlockPos discoPos) {
        var c = getController(player, controllerPos);
        if (c == null || !c.getLinkedDiscoBalls().contains(discoPos)) return;
        BlockEntity be = player.level().getBlockEntity(discoPos);
        if (be instanceof DiscoBallBlockEntity ball) {
            ball.toggleActive();
            player.level().sendBlockUpdated(discoPos, ball.getBlockState(), ball.getBlockState(), 3);
        }
    }

    private static void toggleLaser(ServerPlayer player, BlockPos controllerPos, BlockPos laserPos) {
        var c = getController(player, controllerPos);
        if (c == null || !c.getLinkedLasers().contains(laserPos)) return;
        BlockEntity be = player.level().getBlockEntity(laserPos);
        if (be instanceof LaserEmitterBlockEntity laser) {
            laser.setPowered(!laser.isPowered());
            player.level().sendBlockUpdated(laserPos, laser.getBlockState(), laser.getBlockState(), 3);
        }
    }

    private static void toggleParty(ServerPlayer player, BlockPos controllerPos, BlockPos pos) {
        var c = getController(player, controllerPos);
        if (c == null || !c.getLinkedPartyLights().contains(pos)) return;
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof PartyLightBlockEntity light) {
            light.toggle();
            player.level().sendBlockUpdated(pos, light.getBlockState(), light.getBlockState(), 3);
        }
    }

    private static void toggleStrobe(ServerPlayer player, BlockPos controllerPos, BlockPos pos) {
        var c = getController(player, controllerPos);
        if (c == null || !c.getLinkedStrobes().contains(pos)) return;
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof StrobeLightBlockEntity strobe) {
            strobe.setPowered(!strobe.isPowered());
            player.level().sendBlockUpdated(pos, strobe.getBlockState(), strobe.getBlockState(), 3);
        }
    }

    private static void setPattern(ServerPlayer player, BlockPos controllerPos, UUID groupId, FloorPattern pattern) {
        var c = getController(player, controllerPos);
        if (c == null || !hasGroup(c, groupId)) return;
        var s = c.getGroupSettings(groupId);
        c.setGroupSettings(groupId, new LightControllerBlockEntity.GroupAnimSettings(pattern, s.speed(), s.playing(), s.syncDisco(), s.syncJukebox()));
    }

    private static void setSpeed(ServerPlayer player, BlockPos controllerPos, UUID groupId, int speed) {
        var c = getController(player, controllerPos);
        if (c == null || !hasGroup(c, groupId)) return;
        var s = c.getGroupSettings(groupId);
        c.setGroupSettings(groupId, new LightControllerBlockEntity.GroupAnimSettings(s.pattern(), Math.max(1, Math.min(10, speed)), s.playing(), s.syncDisco(), s.syncJukebox()));
    }

    private static void togglePlaying(ServerPlayer player, BlockPos controllerPos, UUID groupId, boolean playing) {
        var c = getController(player, controllerPos);
        if (c == null || !hasGroup(c, groupId)) return;
        var s = c.getGroupSettings(groupId);
        c.setGroupSettings(groupId, new LightControllerBlockEntity.GroupAnimSettings(s.pattern(), s.speed(), playing, s.syncDisco(), s.syncJukebox()));
    }

    private static void toggleSyncDisco(ServerPlayer player, BlockPos controllerPos, UUID groupId, boolean sync) {
        var c = getController(player, controllerPos);
        if (c == null || !hasGroup(c, groupId)) return;
        var s = c.getGroupSettings(groupId);
        c.setGroupSettings(groupId, new LightControllerBlockEntity.GroupAnimSettings(s.pattern(), s.speed(), s.playing(), sync, s.syncJukebox()));
    }

    private static void toggleSyncJukebox(ServerPlayer player, BlockPos controllerPos, UUID groupId, boolean sync) {
        var c = getController(player, controllerPos);
        if (c == null || !hasGroup(c, groupId)) return;
        var s = c.getGroupSettings(groupId);
        c.setGroupSettings(groupId, new LightControllerBlockEntity.GroupAnimSettings(s.pattern(), s.speed(), s.playing(), s.syncDisco(), sync));
    }

    public static void sendSetTileColor(BlockPos c, UUID g, int x, int z, int color) {
        var b = new FriendlyByteBuf(Unpooled.buffer());
        b.writeBlockPos(c); b.writeUUID(g); b.writeVarInt(x); b.writeVarInt(z); b.writeVarInt(color);
        NetworkManager.sendToServer(SET_TILE_COLOR, b);
    }

    public static void sendApplyAllColor(BlockPos c, UUID g, int color) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUUID(g); b.writeVarInt(color);
        NetworkManager.sendToServer(APPLY_ALL_COLOR, b);
    }

    public static void sendSelectFloorGroup(BlockPos c, UUID g) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUUID(g);
        NetworkManager.sendToServer(SELECT_FLOOR_GROUP, b);
    }

    public static void sendRenameFloorGroup(UUID g, String name) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeUUID(g); b.writeUtf(name, 32);
        NetworkManager.sendToServer(RENAME_FLOOR_GROUP, b);
    }

    public static void sendUnlinkFloorGroup(BlockPos c, UUID g) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUUID(g);
        NetworkManager.sendToServer(UNLINK_FLOOR_GROUP, b);
    }

    public static void sendSetWrenchGroup(UUID g) { NetworkManager.sendToServer(SET_WRENCH_GROUP, writeUuid(g)); }

    public static void sendToggleDiscoSpin(BlockPos c, boolean on) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeBoolean(on);
        NetworkManager.sendToServer(TOGGLE_DISCO_SPIN, b);
    }

    public static void sendToggleAllDiscos(BlockPos c) { NetworkManager.sendToServer(TOGGLE_ALL_DISCOS, writePos(c)); }

    public static void sendToggleDisco(BlockPos c, BlockPos disco) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeBlockPos(disco);
        NetworkManager.sendToServer(TOGGLE_DISCO, b);
    }

    public static void sendToggleLaser(BlockPos c, BlockPos laser) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeBlockPos(laser);
        NetworkManager.sendToServer(TOGGLE_LASER, b);
    }

    public static void sendToggleParty(BlockPos c, BlockPos pos) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeBlockPos(pos);
        NetworkManager.sendToServer(TOGGLE_PARTY, b);
    }

    public static void sendToggleStrobe(BlockPos c, BlockPos pos) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeBlockPos(pos);
        NetworkManager.sendToServer(TOGGLE_STROBE, b);
    }

    public static void sendSetPattern(BlockPos c, UUID g, FloorPattern pattern) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUUID(g); b.writeVarInt(pattern.id());
        NetworkManager.sendToServer(SET_PATTERN, b);
    }

    public static void sendSetPatternSpeed(BlockPos c, UUID g, int speed) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUUID(g); b.writeVarInt(speed);
        NetworkManager.sendToServer(SET_PATTERN_SPEED, b);
    }

    public static void sendTogglePattern(BlockPos c, UUID g, boolean playing) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUUID(g); b.writeBoolean(playing);
        NetworkManager.sendToServer(TOGGLE_PATTERN, b);
    }

    public static void sendToggleSyncDisco(BlockPos c, UUID g, boolean sync) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUUID(g); b.writeBoolean(sync);
        NetworkManager.sendToServer(TOGGLE_SYNC_DISCO, b);
    }

    public static void sendToggleSyncJukebox(BlockPos c, UUID g, boolean sync) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUUID(g); b.writeBoolean(sync);
        NetworkManager.sendToServer(TOGGLE_SYNC_JUKEBOX, b);
    }

    public static void sendSavePreset(BlockPos c, String name, UUID g) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUtf(name, 24); b.writeUUID(g);
        NetworkManager.sendToServer(SAVE_PRESET, b);
    }

    public static void sendApplyPreset(BlockPos c, String name, UUID g) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUtf(name, 24); b.writeUUID(g);
        NetworkManager.sendToServer(APPLY_PRESET, b);
    }

    public static void sendCopyGroup(BlockPos c, UUID from, UUID to) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(c); b.writeUUID(from); b.writeUUID(to);
        NetworkManager.sendToServer(COPY_GROUP, b);
    }

    public static void sendRefresh(BlockPos c) { NetworkManager.sendToServer(REFRESH_CONTROLLER, writePos(c)); }

    private static FriendlyByteBuf writePos(BlockPos pos) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeBlockPos(pos); return b;
    }

    private static FriendlyByteBuf writeUuid(UUID id) {
        var b = new FriendlyByteBuf(Unpooled.buffer()); b.writeUUID(id); return b;
    }
}
