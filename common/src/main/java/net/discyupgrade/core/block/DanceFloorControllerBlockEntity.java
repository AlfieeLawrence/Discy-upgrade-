package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.discyupgrade.core.registry.BlockEntityRegistry;

import java.util.UUID;

public class DanceFloorControllerBlockEntity extends BlockEntity {
    private UUID linkedNetworkId;

    public DanceFloorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DANCE_FLOOR_CONTROLLER.get(), pos, state);
    }

    public UUID getLinkedNetworkId() {
        return linkedNetworkId;
    }

    public void setLinkedNetworkId(UUID linkedNetworkId) {
        this.linkedNetworkId = linkedNetworkId;
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedNetworkId = tag.hasUUID("LinkedNetworkId") ? tag.getUUID("LinkedNetworkId") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (linkedNetworkId != null) {
            tag.putUUID("LinkedNetworkId", linkedNetworkId);
        }
    }
}
