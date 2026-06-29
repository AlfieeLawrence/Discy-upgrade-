package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.discyupgrade.core.registry.BlockEntityRegistry;
import org.jetbrains.annotations.Nullable;

public class StrobeLightBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public StrobeLightBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StrobeLightBlockEntity(pos, state); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, BlockEntityRegistry.STROBE_LIGHT.get(),
                (lvl, p, s, be) -> be.tick());
    }
}
