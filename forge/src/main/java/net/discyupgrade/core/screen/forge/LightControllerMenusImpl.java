package net.discyupgrade.core.screen.forge;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.DiscoBallBlockEntity;
import net.discyupgrade.core.block.LaserEmitterBlockEntity;
import net.discyupgrade.core.block.LightControllerBlockEntity;
import net.discyupgrade.core.screen.LightControllerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LightControllerMenusImpl {
    public static void open(ServerPlayer player, LightControllerBlockEntity controller) {
        controller.pruneBrokenLinks();
        List<LightControllerMenu.FloorGroupView> floors =
                LightControllerMenu.buildFloorViews(controller.getLevel(), controller.getLinkedFloorGroups());
        List<LightControllerMenu.LightView> discos = new ArrayList<>();
        for (var pos : controller.getLinkedDiscoBalls()) {
            BlockEntity be = controller.getLevel().getBlockEntity(pos);
            boolean active = be instanceof DiscoBallBlockEntity ball && ball.isActive();
            discos.add(new LightControllerMenu.LightView(pos, active));
        }
        List<LightControllerMenu.LightView> lasers = new ArrayList<>();
        for (var pos : controller.getLinkedLasers()) {
            BlockEntity be = controller.getLevel().getBlockEntity(pos);
            boolean active = be instanceof LaserEmitterBlockEntity laser && laser.isPowered();
            lasers.add(new LightControllerMenu.LightView(pos, active));
        }
        UUID selected = controller.getSelectedFloorGroup();
        boolean spin = controller.isDiscoSpinEnabled();

        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
                LightControllerMenu.writeOpeningData(buf, controller.getBlockPos(), floors, discos, lasers, selected, spin);
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("block.discyupgrade.light_controller");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new LightControllerMenu(syncId, inv, controller.getBlockPos(), floors, discos, lasers, selected, spin);
            }
        });
    }
}
