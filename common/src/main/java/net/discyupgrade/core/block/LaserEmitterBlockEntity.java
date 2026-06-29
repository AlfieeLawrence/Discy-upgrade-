package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.discyupgrade.core.registry.BlockEntityRegistry;

public class LaserEmitterBlockEntity extends BlockEntity {
    private static final int[] LASER_COLORS = {
            0xFF0000, 0x00FF00, 0x0000FF, 0xFF00FF, 0x00FFFF, 0xFFFF00
    };

    private boolean powered;
    private int colorIndex;

    public LaserEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.LASER_EMITTER.get(), pos, state);
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
        setChanged();
    }

    public int getLaserColor() {
        return LASER_COLORS[Math.floorMod(colorIndex, LASER_COLORS.length)];
    }

    public void cycleColor() {
        colorIndex++;
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        powered = tag.getBoolean("Powered");
        colorIndex = tag.getInt("ColorIndex");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Powered", powered);
        tag.putInt("ColorIndex", colorIndex);
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
