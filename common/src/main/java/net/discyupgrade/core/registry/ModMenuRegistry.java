package net.discyupgrade.core.registry;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.screen.LightControllerMenu;

public final class ModMenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(DiscyUpgrade.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<LightControllerMenu>> LIGHT_CONTROLLER =
            MENUS.register("light_controller", () -> MenuRegistry.ofExtended(LightControllerMenu::new));

    public static void init() {
        MENUS.register();
    }
}
