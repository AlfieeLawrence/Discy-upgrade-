package net.discyupgrade.core.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.discyupgrade.core.compat.AccessoryCompat;
import net.discyupgrade.core.registry.ItemRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DiscoGogglesItem extends Item {
    public DiscoGogglesItem(Properties props) { super(props); }

    public static void tickPlayer(Player player) {
        if (player.level().isClientSide || !isWearing(player)) return;
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));
    }

    private static boolean isWearing(Player player) {
        Item goggles = ItemRegistry.DISCO_GOGGLES.get();
        if (player.getInventory().armor.get(3).is(goggles)) return true;
        return AccessoryCompat.findAccessory(player, goggles).isPresent();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.discyupgrade.disco_goggles"));
    }
}
