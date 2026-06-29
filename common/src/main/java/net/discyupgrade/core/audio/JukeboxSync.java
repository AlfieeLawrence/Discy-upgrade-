package net.discyupgrade.core.audio;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class JukeboxSync {
    private JukeboxSync() {}

    public static boolean isPlaying(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX)) return false;
        return state.hasProperty(JukeboxBlock.HAS_RECORD) && state.getValue(JukeboxBlock.HAS_RECORD);
    }

    public static int speedFromJukebox(Level level, BlockPos pos) {
        if (!isPlaying(level, pos)) return 5;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof JukeboxBlockEntity jukebox)) return 5;
        int hash = Math.abs(jukebox.getBlockPos().hashCode() % 8);
        return Math.max(1, Math.min(10, 3 + hash));
    }

    public static boolean anyJukeboxPlaying(Level level, Iterable<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (isPlaying(level, pos)) return true;
        }
        return false;
    }
}
