package net.discyupgrade.fabric;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.discyupgrade.core.block.DanceFloorTileBlockEntity;
import net.discyupgrade.core.block.PartyLightBlockEntity;
import net.discyupgrade.core.client.DiscyUpgradeClient;
import net.discyupgrade.core.client.render.DiscoBallRenderer;
import net.discyupgrade.core.client.render.LaserEmitterRenderer;
import net.discyupgrade.core.registry.BlockEntityRegistry;
import net.discyupgrade.core.registry.BlockRegistry;

public class DiscyUpgradeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
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
                if (be instanceof PartyLightBlockEntity light && light.isPowered()) {
                    return 0xFF000000 | light.getColor();
                }
            }
            return 0xFF333333;
        }, BlockRegistry.PARTY_LIGHT.get());

        BlockEntityRendererRegistry.register(BlockEntityRegistry.DISCO_BALL.get(), DiscoBallRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntityRegistry.LASER_EMITTER.get(), LaserEmitterRenderer::new);
    }
}
