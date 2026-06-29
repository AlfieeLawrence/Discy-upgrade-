package net.discyupgrade.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.config.DiscyUpgradeConfig;
import net.discyupgrade.core.config.DiscyUpgradeConfigLoader;
import net.discyupgrade.core.floor.FloorAnimationEngine;
import net.discyupgrade.core.item.DiscoGogglesItem;
import net.minecraft.server.level.ServerLevel;

import java.nio.file.Path;

public class DiscyUpgradeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Path config = FabricLoader.getInstance().getConfigDir().resolve("discyupgrade.json");
        DiscyUpgradeConfigLoader.loadFromJson(config);
        DiscyUpgrade.init();

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world instanceof ServerLevel) {
                FloorAnimationEngine.tick((ServerLevel) world);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getPlayerList().getPlayers().forEach(DiscoGogglesItem::tickPlayer);
        });
    }
}
