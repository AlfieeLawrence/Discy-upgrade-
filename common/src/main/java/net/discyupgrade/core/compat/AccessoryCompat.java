package net.discyupgrade.core.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class AccessoryCompat {
    private AccessoryCompat() {}

    @ExpectPlatform
    public static Optional<ItemStack> findAccessory(Player player, Item item) {
        throw new AssertionError();
    }

    public static Optional<ItemStack> findAnywhere(Player player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) return Optional.of(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() == item) return Optional.of(stack);
        }
        return findAccessory(player, item);
    }
}
