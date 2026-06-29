package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.discyupgrade.core.floor.DanceFloorNetworkIndex;
import net.discyupgrade.core.registry.BlockEntityRegistry;
import org.jetbrains.annotations.Nullable;

public class DanceFloorTileBlock extends BaseEntityBlock {

    public DanceFloorTileBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DanceFloorTileBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            DanceFloorNetworkIndex.onTilePlaced(serverLevel, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DanceFloorTileBlockEntity tile) {
                DanceFloorNetworkIndex.onTileRemoved(serverLevel, pos, tile.getNetworkId());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DanceFloorTileBlockEntity tile && player.isShiftKeyDown()) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.discyupgrade.floor_network",
                            tile.getNetworkId() == null ? "?" : tile.getNetworkId().toString().substring(0, 8)),
                    true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
