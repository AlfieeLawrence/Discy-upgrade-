package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class PartyLightBlock extends BaseEntityBlock {
    public PartyLightBlock(BlockBehaviour.Properties props) { super(props); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PartyLightBlockEntity(pos, state); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PartyLightBlockEntity light) {
                light.toggle();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
