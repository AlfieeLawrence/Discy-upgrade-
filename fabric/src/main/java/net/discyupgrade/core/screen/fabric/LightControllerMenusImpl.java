package net.discyupgrade.core.screen.fabric;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.*;
import net.discyupgrade.core.screen.LightControllerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LightControllerMenusImpl {
    public static void open(ServerPlayer player, LightControllerBlockEntity controller) {
        controller.pruneBrokenLinks();
        var floors = LightControllerMenu.buildFloorViews(controller.getLevel(), controller);
        var discos = lightViews(controller, controller.getLinkedDiscoBalls(), true);
        var lasers = lightViews(controller, controller.getLinkedLasers(), false);
        var parties = lightViewsParty(controller);
        var strobes = lightViewsStrobe(controller);
        var presets = new ArrayList<>(controller.getPresets().keySet());
        UUID selected = controller.getSelectedFloorGroup();
        boolean spin = controller.isDiscoSpinEnabled();

        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
                LightControllerMenu.writeOpeningData(buf, controller.getBlockPos(), floors, discos, lasers,
                        parties, strobes, presets, selected, spin);
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("block.discyupgrade.light_controller");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new LightControllerMenu(syncId, inv, controller.getBlockPos(), floors, discos, lasers,
                        parties, strobes, presets, selected, spin);
            }
        });
    }

    private static List<LightControllerMenu.LightView> lightViews(LightControllerBlockEntity controller,
                                                                   List<net.minecraft.core.BlockPos> positions,
                                                                   boolean disco) {
        List<LightControllerMenu.LightView> list = new ArrayList<>();
        for (var pos : positions) {
            BlockEntity be = controller.getLevel().getBlockEntity(pos);
            boolean active = disco
                    ? be instanceof DiscoBallBlockEntity ball && ball.isActive()
                    : be instanceof LaserEmitterBlockEntity laser && laser.isPowered();
            list.add(new LightControllerMenu.LightView(pos, active));
        }
        return list;
    }

    private static List<LightControllerMenu.LightView> lightViewsParty(LightControllerBlockEntity controller) {
        List<LightControllerMenu.LightView> list = new ArrayList<>();
        for (var pos : controller.getLinkedPartyLights()) {
            BlockEntity be = controller.getLevel().getBlockEntity(pos);
            list.add(new LightControllerMenu.LightView(pos, be instanceof PartyLightBlockEntity p && p.isPowered()));
        }
        return list;
    }

    private static List<LightControllerMenu.LightView> lightViewsStrobe(LightControllerBlockEntity controller) {
        List<LightControllerMenu.LightView> list = new ArrayList<>();
        for (var pos : controller.getLinkedStrobes()) {
            BlockEntity be = controller.getLevel().getBlockEntity(pos);
            list.add(new LightControllerMenu.LightView(pos, be instanceof StrobeLightBlockEntity s && s.isPowered()));
        }
        return list;
    }
}
