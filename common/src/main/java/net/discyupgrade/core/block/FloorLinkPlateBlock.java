package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.discyupgrade.core.registry.BlockRegistry;

public class FloorLinkPlateBlock extends Block {
    public FloorLinkPlateBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(1.5f)
                .lightLevel(state -> 2));
    }

    public static boolean isLinkPlate(BlockState state) {
        return state.is(BlockRegistry.FLOOR_LINK_PLATE.get());
    }
}
