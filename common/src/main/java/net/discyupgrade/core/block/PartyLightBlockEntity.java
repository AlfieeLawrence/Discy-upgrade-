package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.discyupgrade.core.registry.BlockEntityRegistry;

public class PartyLightBlockEntity extends BlockEntity {
    private int color = 0xFF4488;
    private boolean powered = true;

    public PartyLightBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.PARTY_LIGHT.get(), pos, state);
    }

    public int getColor() { return color; }
    public boolean isPowered() { return powered; }

    public void setColor(int color) { this.color = color & 0xFFFFFF; setChanged(); }
    public void setPowered(boolean powered) { this.powered = powered; setChanged(); }
    public void toggle() { setPowered(!powered); }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        color = tag.getInt("Color");
        powered = !tag.contains("Powered") || tag.getBoolean("Powered");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Color", color);
        tag.putBoolean("Powered", powered);
    }

    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = new CompoundTag(); saveAdditional(t); return t; }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
