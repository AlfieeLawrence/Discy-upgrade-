package net.discyupgrade.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.item.LightRemoteItem;
import net.discyupgrade.core.item.TuningWrenchItem;

public final class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(DiscyUpgrade.MOD_ID, Registries.ITEM);
    public static final Registrar<Item> ITEM_REGISTRAR = ITEMS.getRegistrar();

    public static final RegistrySupplier<Item> DANCE_FLOOR_TILE = registerBlockItem(BlockRegistry.DANCE_FLOOR_TILE);
    public static final RegistrySupplier<Item> FLOOR_LINK_PLATE = registerBlockItem(BlockRegistry.FLOOR_LINK_PLATE);
    public static final RegistrySupplier<Item> LIGHT_CONTROLLER = registerBlockItem(BlockRegistry.LIGHT_CONTROLLER);
    public static final RegistrySupplier<Item> DISCO_BALL = registerBlockItem(BlockRegistry.DISCO_BALL);
    public static final RegistrySupplier<Item> LASER_EMITTER = registerBlockItem(BlockRegistry.LASER_EMITTER);
    public static final RegistrySupplier<Item> PARTY_LIGHT = registerBlockItem(BlockRegistry.PARTY_LIGHT);
    public static final RegistrySupplier<Item> STROBE_LIGHT = registerBlockItem(BlockRegistry.STROBE_LIGHT);
    public static final RegistrySupplier<Item> TUNING_WRENCH = ITEMS.register("tuning_wrench",
            () -> new TuningWrenchItem(new Item.Properties().stacksTo(1)));
    public static final RegistrySupplier<Item> LIGHT_REMOTE = ITEMS.register("light_remote",
            () -> new LightRemoteItem(new Item.Properties().stacksTo(1)));

    private static RegistrySupplier<Item> registerBlockItem(RegistrySupplier<Block> block) {
        return ITEM_REGISTRAR.register(block.getId(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void init() { ITEMS.register(); }
}
