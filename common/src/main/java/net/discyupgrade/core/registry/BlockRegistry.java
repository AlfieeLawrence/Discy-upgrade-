package net.discyupgrade.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.block.*;
import net.discyupgrade.core.item.TuningWrenchItem;
import net.discyupgrade.core.util.ModIdentifier;

import java.util.function.Supplier;

public final class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(DiscyUpgrade.MOD_ID, Registries.BLOCK);
    public static final Registrar<Block> BLOCK_REGISTRAR = BLOCKS.getRegistrar();

    public static final RegistrySupplier<Block> DANCE_FLOOR_TILE = registerBlock("dance_floor_tile",
            () -> new DanceFloorTileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.8f)
                    .lightLevel(state -> 3)
                    .noOcclusion()));

    public static final RegistrySupplier<Block> LIGHT_CONTROLLER = registerBlock("light_controller",
            () -> new LightControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)
                    .noOcclusion()));

    public static final RegistrySupplier<Block> DISCO_BALL = registerBlock("disco_ball",
            () -> new DiscoBallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(1.0f)
                    .lightLevel(state -> 12)
                    .noOcclusion()));

    public static final RegistrySupplier<Block> LASER_EMITTER = registerBlock("laser_emitter",
            () -> new LaserEmitterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.5f)
                    .noOcclusion()));

    private static <T extends Block> RegistrySupplier<T> registerBlock(String name, Supplier<T> block) {
        return BLOCK_REGISTRAR.register(new ModIdentifier(name), block);
    }

    public static void init() {
        BLOCKS.register();
    }

    public static void registerBlockColors() {}
}
