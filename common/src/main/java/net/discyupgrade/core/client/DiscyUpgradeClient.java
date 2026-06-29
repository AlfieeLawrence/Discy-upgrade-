package net.discyupgrade.core.client;

import dev.architectury.registry.menu.MenuRegistry;
import net.discyupgrade.core.client.screen.DanceFloorControllerScreen;
import net.discyupgrade.core.registry.BlockRegistry;
import net.discyupgrade.core.registry.ModMenuRegistry;

public class DiscyUpgradeClient {
    public static void init() {
        MenuRegistry.registerScreenFactory(
                ModMenuRegistry.DANCE_FLOOR_CONTROLLER.get(), DanceFloorControllerScreen::new);
        BlockRegistry.registerBlockColors();
    }
}
