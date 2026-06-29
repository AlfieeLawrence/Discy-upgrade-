package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.discyupgrade.core.registry.BlockEntityRegistry;
import net.discyupgrade.core.registry.SoundEventRegistry;

public class StrobeLightBlockEntity extends BlockEntity {
    private boolean powered;
    private int tick;

    public StrobeLightBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.STROBE_LIGHT.get(), pos, state);
    }

    public boolean isPowered() { return powered; }
    public void setPowered(boolean powered) { this.powered = powered; setChanged(); }
    public int getTick() { return tick; }

    public void tick() {
        if (level == null || level.isClientSide) return;
        tick++;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof StrobeLightBlock)) return;

        if (!powered) {
            if (state.getValue(StrobeLightBlock.LIT)) {
                level.setBlock(worldPosition, state.setValue(StrobeLightBlock.LIT, false), 3);
            }
            return;
        }

        boolean lit = (tick / 3) % 2 == 0;
        if (state.getValue(StrobeLightBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(StrobeLightBlock.LIT, lit), 3);
            if (lit) {
                level.playSound(null, worldPosition, SoundEventRegistry.STROBE_CLICK.get(),
                        SoundSource.BLOCKS, 0.35f, 1.2f);
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        powered = tag.getBoolean("Powered");
        tick = tag.getInt("Tick");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Powered", powered);
        tag.putInt("Tick", tick);
    }

    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
