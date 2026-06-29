package net.discyupgrade.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.config.DiscyUpgradeConfig;
import net.discyupgrade.core.floor.FloorAnimationEngine;
import net.minecraft.server.level.ServerLevel;

@Mod(DiscyUpgrade.MOD_ID)
public class DiscyUpgradeForge {
    public DiscyUpgradeForge() {
        EventBuses.registerModEventBus(DiscyUpgrade.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DiscyUpgradeForgeConfig.SPEC);
        applyConfig();
        DiscyUpgrade.init();
        MinecraftForge.EVENT_BUS.addListener(this::onLevelTick);
    }

    private static void applyConfig() {
        DiscyUpgradeConfig.wirelessLinkRange = DiscyUpgradeForgeConfig.WIRELESS_LINK_RANGE.get();
        DiscyUpgradeConfig.remoteOpenRange = DiscyUpgradeForgeConfig.REMOTE_OPEN_RANGE.get();
        DiscyUpgradeConfig.maxLinkedFloorsPerController = DiscyUpgradeForgeConfig.MAX_LINKED_FLOORS.get();
        DiscyUpgradeConfig.maxTilesPerGroup = DiscyUpgradeForgeConfig.MAX_TILES_PER_GROUP.get();
    }

    private void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        if (event.level instanceof ServerLevel sl) {
            FloorAnimationEngine.tick(sl);
        }
    }
}
