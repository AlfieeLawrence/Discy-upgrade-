package net.discyupgrade.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.discyupgrade.DiscyUpgrade;

public final class TabRegistry {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(DiscyUpgrade.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> DISCO_TAB = TABS.register("disco",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.discyupgrade.disco"))
                    .icon(() -> new ItemStack(ItemRegistry.DISCO_BALL.get()))
                    .displayItems((params, output) -> {
                        output.accept(ItemRegistry.DANCE_FLOOR_TILE.get());
                        output.accept(ItemRegistry.LIGHT_CONTROLLER.get());
                        output.accept(ItemRegistry.TUNING_WRENCH.get());
                        output.accept(ItemRegistry.DISCO_BALL.get());
                        output.accept(ItemRegistry.LASER_EMITTER.get());
                    })
                    .build());

    public static void init() {
        TABS.register();
    }
}
