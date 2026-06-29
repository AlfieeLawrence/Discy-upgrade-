package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.discyupgrade.core.floor.FloorGroupIndex;
import net.discyupgrade.core.registry.BlockEntityRegistry;

import java.util.UUID;

public class DanceFloorTileBlockEntity extends BlockEntity {
    private static final int DEFAULT_COLOR = 0x2A2A2A;

    private UUID groupId;
    private int baseColor = DEFAULT_COLOR;
    private int color = DEFAULT_COLOR;

    public DanceFloorTileBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DANCE_FLOOR_TILE.get(), pos, state);
    }

    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }

    public int getBaseColor() { return baseColor; }
    public int getColor() { return color; }

    public void setColor(int color) {
        this.baseColor = color & 0xFFFFFF;
        this.color = this.baseColor;
        setChanged();
    }

    public void setDisplayColor(int color) {
        this.color = color & 0xFFFFFF;
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("GroupId")) groupId = tag.getUUID("GroupId");
        else if (tag.hasUUID("NetworkId")) groupId = tag.getUUID("NetworkId");
        else groupId = null;
        baseColor = tag.contains("BaseColor") ? tag.getInt("BaseColor") : tag.getInt("Color");
        color = tag.contains("Color") ? tag.getInt("Color") : baseColor;
        if (!tag.contains("BaseColor") && !tag.contains("Color")) {
            baseColor = color = DEFAULT_COLOR;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (groupId != null) tag.putUUID("GroupId", groupId);
        tag.putInt("BaseColor", baseColor);
        tag.putInt("Color", color);
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

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (level != null && !level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && groupId != null) {
            FloorGroupIndex.register(serverLevel, worldPosition, groupId);
        }
    }
}
