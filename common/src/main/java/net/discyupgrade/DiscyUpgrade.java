package net.discyupgrade;

import net.discyupgrade.core.network.DiscyUpgradeNetworking;
import net.discyupgrade.core.registry.BlockEntityRegistry;
import net.discyupgrade.core.registry.BlockRegistry;
import net.discyupgrade.core.registry.ItemRegistry;
import net.discyupgrade.core.registry.ModMenuRegistry;
import net.discyupgrade.core.registry.TabRegistry;

public class DiscyUpgrade {
    public static final String MOD_ID = "discyupgrade";

    public static void init() {
        BlockRegistry.init();
        ItemRegistry.init();
        BlockEntityRegistry.init();
        ModMenuRegistry.init();
        TabRegistry.init();
        DiscyUpgradeNetworking.init();
    }
}
