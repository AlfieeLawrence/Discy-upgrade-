package net.discyupgrade.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.config.DiscyUpgradeConfig;
import net.discyupgrade.core.floor.FloorAnimationEngine;
import net.minecraft.server.level.ServerLevel;

public class DiscyUpgradeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DiscyUpgradeConfig.wirelessLinkRange = 64;
        DiscyUpgradeConfig.remoteOpenRange = 32;
        DiscyUpgradeConfig.maxLinkedFloorsPerController = 8;
        DiscyUpgradeConfig.maxTilesPerGroup = 256;
        DiscyUpgrade.init();

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world instanceof ServerLevel) {
                FloorAnimationEngine.tick((ServerLevel) world);
            }
        });
    }
}
