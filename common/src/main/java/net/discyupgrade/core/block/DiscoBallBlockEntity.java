package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.discyupgrade.core.registry.BlockEntityRegistry;

public class DiscoBallBlockEntity extends BlockEntity {
    private boolean active = true;
    private float spin;

    public DiscoBallBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DISCO_BALL.get(), pos, state);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        setChanged();
    }

    public void toggleActive() {
        setActive(!active);
    }

    public float getSpin() {
        return spin;
    }

    public void tickSpin() {
        if (active) {
            spin = (spin + 4f) % 360f;
            if (level != null && !level.isClientSide && tick % 40 == 0) {
                level.playSound(null, worldPosition, net.discyupgrade.core.registry.SoundEventRegistry.DISCO_BALL_SPIN.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.25f, 1.0f + (spin / 360f));
            }
        }
        tick++;
    }

    private int tick;

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        active = !tag.contains("Active") || tag.getBoolean("Active");
        spin = tag.getFloat("Spin");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Active", active);
        tag.putFloat("Spin", spin);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
