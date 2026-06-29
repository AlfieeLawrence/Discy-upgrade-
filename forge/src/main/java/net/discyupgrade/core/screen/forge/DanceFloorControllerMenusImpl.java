package net.discyupgrade.core.screen.forge;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.discyupgrade.core.block.DanceFloorControllerBlockEntity;
import net.discyupgrade.core.screen.DanceFloorControllerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DanceFloorControllerMenusImpl {
    public static void open(ServerPlayer player, DanceFloorControllerBlockEntity controller) {
        UUID networkId = controller.getLinkedNetworkId();
        if (networkId == null) return;

        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
                buf.writeBlockPos(controller.getBlockPos());
                buf.writeUUID(networkId);
                DanceFloorControllerMenu.writeTiles(buf,
                        DanceFloorControllerMenu.fromNetwork(controller.getLevel(), networkId));
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("block.discyupgrade.dance_floor_controller");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new DanceFloorControllerMenu(syncId, inv, controller.getBlockPos(), networkId,
                        DanceFloorControllerMenu.fromNetwork(controller.getLevel(), networkId));
            }
        });
    }
}
