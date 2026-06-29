package net.discyupgrade.forge;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.block.DanceFloorTileBlockEntity;
import net.discyupgrade.core.client.DiscyUpgradeClient;
import net.discyupgrade.core.client.render.DiscoBallRenderer;
import net.discyupgrade.core.client.render.LaserEmitterRenderer;
import net.discyupgrade.core.registry.BlockEntityRegistry;
import net.discyupgrade.core.registry.BlockRegistry;

@Mod.EventBusSubscriber(modid = DiscyUpgrade.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DiscyUpgradeForgeClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            DiscyUpgradeClient.init();

            ColorHandlerRegistry.registerBlockColors((state, level, pos, tintIndex) -> {
                if (level != null && pos != null) {
                    var be = level.getBlockEntity(pos);
                    if (be instanceof DanceFloorTileBlockEntity tile) {
                        return tile.getColor();
                    }
                }
                return 0x2A2A2A;
            }, BlockRegistry.DANCE_FLOOR_TILE.get());

            ColorHandlerRegistry.registerBlockColors((state, level, pos, tintIndex) -> {
                if (level != null && pos != null) {
                    var be = level.getBlockEntity(pos);
                    if (be instanceof net.discyupgrade.core.block.PartyLightBlockEntity light && light.isPowered()) {
                        return 0xFF000000 | light.getColor();
                    }
                }
                return 0xFF333333;
            }, BlockRegistry.PARTY_LIGHT.get());

            BlockEntityRendererRegistry.register(BlockEntityRegistry.DISCO_BALL.get(), DiscoBallRenderer::new);
            BlockEntityRendererRegistry.register(BlockEntityRegistry.LASER_EMITTER.get(), LaserEmitterRenderer::new);
        });
    }
}
