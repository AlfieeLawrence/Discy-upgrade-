package net.discyupgrade.core.screen;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.discyupgrade.core.block.LightControllerBlockEntity;

public final class LightControllerMenus {
    private LightControllerMenus() {}

    @ExpectPlatform
    public static void open(ServerPlayer player, LightControllerBlockEntity controller) {
        throw new AssertionError();
    }
}
