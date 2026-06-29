package net.discyupgrade.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.util.ModIdentifier;

public final class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(DiscyUpgrade.MOD_ID, Registries.ITEM);
    public static final Registrar<Item> ITEM_REGISTRAR = ITEMS.getRegistrar();

    public static final RegistrySupplier<Item> DANCE_FLOOR_TILE = registerBlockItem(BlockRegistry.DANCE_FLOOR_TILE);
    public static final RegistrySupplier<Item> DANCE_FLOOR_CONTROLLER = registerBlockItem(BlockRegistry.DANCE_FLOOR_CONTROLLER);
    public static final RegistrySupplier<Item> DISCO_BALL = registerBlockItem(BlockRegistry.DISCO_BALL);
    public static final RegistrySupplier<Item> LASER_EMITTER = registerBlockItem(BlockRegistry.LASER_EMITTER);

    private static RegistrySupplier<Item> registerBlockItem(RegistrySupplier<Block> block) {
        return ITEM_REGISTRAR.register(block.getId(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void init() {
        ITEMS.register();
    }
}
