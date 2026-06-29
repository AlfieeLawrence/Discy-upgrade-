package net.discyupgrade.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.block.*;
import net.discyupgrade.core.util.ModIdentifier;

public final class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(DiscyUpgrade.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    public static final Registrar<BlockEntityType<?>> BLOCK_ENTITY_REGISTRAR = BLOCK_ENTITY_TYPES.getRegistrar();

    public static final RegistrySupplier<BlockEntityType<DanceFloorTileBlockEntity>> DANCE_FLOOR_TILE =
            register("dance_floor_tile", () -> BlockEntityType.Builder
                    .of(DanceFloorTileBlockEntity::new, BlockRegistry.DANCE_FLOOR_TILE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<LightControllerBlockEntity>> LIGHT_CONTROLLER =
            register("light_controller", () -> BlockEntityType.Builder
                    .of(LightControllerBlockEntity::new, BlockRegistry.LIGHT_CONTROLLER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<DiscoBallBlockEntity>> DISCO_BALL =
            register("disco_ball", () -> BlockEntityType.Builder
                    .of(DiscoBallBlockEntity::new, BlockRegistry.DISCO_BALL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<LaserEmitterBlockEntity>> LASER_EMITTER =
            register("laser_emitter", () -> BlockEntityType.Builder
                    .of(LaserEmitterBlockEntity::new, BlockRegistry.LASER_EMITTER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<PartyLightBlockEntity>> PARTY_LIGHT =
            register("party_light", () -> BlockEntityType.Builder
                    .of(PartyLightBlockEntity::new, BlockRegistry.PARTY_LIGHT.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<StrobeLightBlockEntity>> STROBE_LIGHT =
            register("strobe_light", () -> BlockEntityType.Builder
                    .of(StrobeLightBlockEntity::new, BlockRegistry.STROBE_LIGHT.get()).build(null));

    private static <T extends BlockEntityType<?>> RegistrySupplier<T> register(String path, java.util.function.Supplier<T> type) {
        return BLOCK_ENTITY_REGISTRAR.register(new ModIdentifier(path), type);
    }

    public static void init() { BLOCK_ENTITY_TYPES.register(); }
}
