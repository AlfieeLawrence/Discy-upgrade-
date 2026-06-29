package net.discyupgrade.core.screen;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.discyupgrade.core.block.DanceFloorControllerBlockEntity;

public final class DanceFloorControllerMenus {
    private DanceFloorControllerMenus() {}

    @ExpectPlatform
    public static void open(ServerPlayer player, DanceFloorControllerBlockEntity controller) {
        throw new AssertionError();
    }
}
