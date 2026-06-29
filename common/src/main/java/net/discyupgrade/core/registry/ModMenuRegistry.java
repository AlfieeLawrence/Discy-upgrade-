package net.discyupgrade.core.registry;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.screen.DanceFloorControllerMenu;

public final class ModMenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(DiscyUpgrade.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<DanceFloorControllerMenu>> DANCE_FLOOR_CONTROLLER =
            MENUS.register("dance_floor_controller",
                    () -> MenuRegistry.ofExtended(DanceFloorControllerMenu::new));

    public static void init() {
        MENUS.register();
    }
}
