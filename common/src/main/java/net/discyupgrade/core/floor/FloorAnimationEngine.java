package net.discyupgrade.core.floor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.DanceFloorTileBlockEntity;
import net.discyupgrade.core.block.DiscoBallBlockEntity;
import net.discyupgrade.core.block.LightControllerBlockEntity;

import java.util.*;

public final class FloorAnimationEngine {
    private static final Map<Long, ControllerAnimState> ACTIVE = new HashMap<>();

    private FloorAnimationEngine() {}

    public static void registerController(LightControllerBlockEntity controller) {
        if (controller.getLevel() == null || controller.getLevel().isClientSide) return;
        ACTIVE.put(controller.getBlockPos().asLong(), new ControllerAnimState(controller));
    }

    public static void unregisterController(BlockPos pos) {
        ACTIVE.remove(pos.asLong());
    }

    public static void tick(ServerLevel level) {
        ACTIVE.entrySet().removeIf(e -> {
            BlockEntity be = level.getBlockEntity(BlockPos.of(e.getKey()));
            return !(be instanceof LightControllerBlockEntity controller) || controller.isRemoved();
        });

        for (ControllerAnimState state : ACTIVE.values()) {
            state.tick(level);
        }
    }

    private static final class ControllerAnimState {
        private final BlockPos pos;
        private int globalTick;

        ControllerAnimState(LightControllerBlockEntity controller) {
            this.pos = controller.getBlockPos().immutable();
        }

        void tick(ServerLevel level) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof LightControllerBlockEntity controller)) return;

            globalTick++;
            boolean anyPlaying = false;

            for (UUID groupId : controller.getLinkedFloorGroups()) {
                LightControllerBlockEntity.GroupAnimSettings settings = controller.getGroupSettings(groupId);
                if (settings == null || settings.pattern() == FloorPattern.STATIC) continue;

                boolean playing = settings.playing();
                if (settings.syncDisco()) {
                    playing = isAnyDiscoActive(level, controller);
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
                int tick = settings.syncDisco() ? globalTick + (int) getMaxDiscoSpin(level, controller) : globalTick;

                for (FloorGroupIndex.TileEntry entry : FloorGroupIndex.buildLayout(level, groupId)) {
                    BlockEntity tileBe = level.getBlockEntity(entry.pos());
                    if (tileBe instanceof DanceFloorTileBlockEntity tile) {
                        int animated = settings.pattern().computeColor(tile.getBaseColor(), entry.relX(), entry.relZ(), tick, speed);
                        if (tile.getColor() != animated) {
                            tile.setDisplayColor(animated);
                            level.sendBlockUpdated(entry.pos(), tile.getBlockState(), tile.getBlockState(), 3);
                        }
                    }
                }
            }

            if (!anyPlaying) globalTick++;
            else if (globalTick % 20 == 0) {
                level.playSound(null, pos, net.discyupgrade.core.registry.SoundEventRegistry.FLOOR_TICK.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.15f, 1.0f);
            }
        }

        private static void restoreBaseColors(Level level, UUID groupId) {
            for (FloorGroupIndex.TileEntry entry : FloorGroupIndex.buildLayout(level, groupId)) {
                BlockEntity tileBe = level.getBlockEntity(entry.pos());
                if (tileBe instanceof DanceFloorTileBlockEntity tile && tile.getColor() != tile.getBaseColor()) {
                    tile.setDisplayColor(tile.getBaseColor());
                    level.sendBlockUpdated(entry.pos(), tile.getBlockState(), tile.getBlockState(), 3);
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
