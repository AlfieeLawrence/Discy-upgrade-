package net.discyupgrade.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.discyupgrade.DiscyUpgrade;

@Mod(DiscyUpgrade.MOD_ID)
public class DiscyUpgradeForge {
    public DiscyUpgradeForge() {
        EventBuses.registerModEventBus(DiscyUpgrade.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        DiscyUpgrade.init();
    }
}
