package net.discyupgrade.core.client;

import dev.architectury.registry.menu.MenuRegistry;
import net.discyupgrade.core.client.screen.LightControllerScreen;
import net.discyupgrade.core.registry.ModMenuRegistry;

public class DiscyUpgradeClient {
    public static void init() {
        MenuRegistry.registerScreenFactory(
                ModMenuRegistry.LIGHT_CONTROLLER.get(), LightControllerScreen::new);
    }
}
