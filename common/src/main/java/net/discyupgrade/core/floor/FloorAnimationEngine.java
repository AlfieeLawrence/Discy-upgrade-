package net.discyupgrade.core.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.audio.JukeboxSync;
import net.discyupgrade.core.block.DanceFloorTileBlockEntity;
import net.discyupgrade.core.block.DiscoBallBlockEntity;
import net.discyupgrade.core.block.LightControllerBlockEntity;
import net.discyupgrade.core.config.DiscyUpgradeConfig;

import java.util.*;

public final class FloorAnimationEngine {
    private static final Map<String, Map<Long, ControllerAnimState>> ACTIVE = new HashMap<>();

    private FloorAnimationEngine() {}

    private static String dimKey(Level level) {
        return level.dimension().location().toString();
    }

    public static void registerController(LightControllerBlockEntity controller) {
        if (controller.getLevel() == null || controller.getLevel().isClientSide) return;
        ACTIVE.computeIfAbsent(dimKey(controller.getLevel()), k -> new HashMap<>())
                .put(controller.getBlockPos().asLong(), new ControllerAnimState(controller));
    }

    public static void unregisterController(Level level, BlockPos pos) {
        Map<Long, ControllerAnimState> map = ACTIVE.get(dimKey(level));
        if (map != null) map.remove(pos.asLong());
    }

    public static void tick(ServerLevel level) {
        Map<Long, ControllerAnimState> map = ACTIVE.computeIfAbsent(dimKey(level), k -> new HashMap<>());
        map.entrySet().removeIf(e -> {
            BlockEntity be = level.getBlockEntity(BlockPos.of(e.getKey()));
            return !(be instanceof LightControllerBlockEntity controller) || controller.isRemoved();
        });

        int interval = Math.max(1, DiscyUpgradeConfig.animationTickInterval);
        for (ControllerAnimState state : map.values()) {
            state.tick(level, interval);
        }
    }

    private static boolean hasNearbyPlayer(ServerLevel level, BlockPos pos, int range) {
        double rangeSq = (double) range * range;
        for (Player player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    private static final class ControllerAnimState {
        private final BlockPos pos;
        private int globalTick;

        ControllerAnimState(LightControllerBlockEntity controller) {
            this.pos = controller.getBlockPos().immutable();
        }

        void tick(ServerLevel level, int interval) {
            if (!level.hasChunkAt(pos)) return;
            if (!hasNearbyPlayer(level, pos, DiscyUpgradeConfig.animationMaxDistance)) return;
            if (globalTick++ % interval != 0) return;

            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof LightControllerBlockEntity controller)) return;

            boolean anyPlaying = false;
            boolean redstonePlay = DiscyUpgradeConfig.redstoneTriggersPatterns && controller.isRedstonePowered();

            for (UUID groupId : controller.getLinkedFloorGroups()) {
                LightControllerBlockEntity.GroupAnimSettings settings = controller.getGroupSettings(groupId);
                if (settings == null || settings.pattern() == FloorPattern.STATIC) continue;

                boolean playing = settings.playing() || redstonePlay;
                if (settings.syncDisco()) {
                    playing = isAnyDiscoActive(level, controller);
                }
                if (settings.syncJukebox()) {
                    playing = JukeboxSync.anyJukeboxPlaying(level, controller.getLinkedJukeboxes());
                }
                if (!playing) {
                    restoreBaseColors(level, groupId);
                    continue;
                }

                anyPlaying = true;
                int speed = settings.speed();
                if (settings.syncDisco()) {
                    speed = Math.max(1, Math.min(10, (int) (getMaxDiscoSpin(level, controller) / 10f)));
                }
                if (settings.syncJukebox() && !controller.getLinkedJukeboxes().isEmpty()) {
                    speed = JukeboxSync.speedFromJukebox(level, controller.getLinkedJukeboxes().get(0));
                }
                int tick = globalTick;
                if (settings.syncDisco()) {
                    tick += (int) getMaxDiscoSpin(level, controller);
                }

                for (FloorGroupIndex.TileEntry entry : FloorGroupIndex.buildLayout(level, groupId)) {
                    if (!level.hasChunkAt(entry.pos())) continue;
                    BlockEntity tileBe = level.getBlockEntity(entry.pos());
                    if (tileBe instanceof DanceFloorTileBlockEntity tile) {
                        int animated = settings.pattern().computeColor(tile.getBaseColor(), entry.relX(), entry.relZ(), tick, speed);
                        if (tile.getColor() != animated) {
                            tile.setDisplayColor(animated);
                            level.sendBlockUpdated(entry.pos(), tile.getBlockState(), tile.getBlockState(), 2);
                        }
                    }
                }
            }

            if (anyPlaying && globalTick % 20 == 0) {
                level.playSound(null, pos, net.discyupgrade.core.registry.SoundEventRegistry.FLOOR_TICK.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.15f, 1.0f);
            }
        }

        private static void restoreBaseColors(Level level, UUID groupId) {
            for (FloorGroupIndex.TileEntry entry : FloorGroupIndex.buildLayout(level, groupId)) {
                BlockEntity tileBe = level.getBlockEntity(entry.pos());
                if (tileBe instanceof DanceFloorTileBlockEntity tile && tile.getColor() != tile.getBaseColor()) {
                    tile.setDisplayColor(tile.getBaseColor());
                    level.sendBlockUpdated(entry.pos(), tile.getBlockState(), tile.getBlockState(), 2);
                }
            }
        }

        private static boolean isAnyDiscoActive(Level level, LightControllerBlockEntity controller) {
            for (BlockPos discoPos : controller.getLinkedDiscoBalls()) {
                BlockEntity dbe = level.getBlockEntity(discoPos);
                if (dbe instanceof DiscoBallBlockEntity ball && ball.isActive()) return true;
            }
            return false;
        }

        private static float getMaxDiscoSpin(Level level, LightControllerBlockEntity controller) {
            float max = 0;
            for (BlockPos discoPos : controller.getLinkedDiscoBalls()) {
                BlockEntity dbe = level.getBlockEntity(discoPos);
                if (dbe instanceof DiscoBallBlockEntity ball) max = Math.max(max, ball.getSpin());
            }
            return max;
        }
    }
}
