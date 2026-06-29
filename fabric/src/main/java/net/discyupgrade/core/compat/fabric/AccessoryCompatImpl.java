package net.discyupgrade.core.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;

public class AccessoryCompatImpl {
  private AccessoryCompatImpl() {}

  public static Optional<ItemStack> findAccessory(Player player, Item item) {
    if (!FabricLoader.getInstance().isModLoaded("trinkets")) return Optional.empty();
    try {
      Class<?> api = Class.forName("dev.emi.trinkets.api.TrinketsApi");
      Method getInv = api.getMethod("getTrinketComponent", net.minecraft.world.entity.LivingEntity.class);
      Object optional = getInv.invoke(null, player);
      Method ifPresent = optional.getClass().getMethod("ifPresent", java.util.function.Consumer.class);
      final ItemStack[] found = new ItemStack[1];
      ifPresent.invoke(optional, (java.util.function.Consumer<Object>) component -> {
        try {
          Method getEquipped = component.getClass().getMethod("getEquipped", Item.class);
          java.util.List<?> stacks = (java.util.List<?>) getEquipped.invoke(component, item);
          if (!stacks.isEmpty()) {
            Object pair = stacks.get(0);
            Method b = pair.getClass().getMethod("getB");
            found[0] = (ItemStack) b.invoke(pair);
          }
        } catch (ReflectiveOperationException ignored) {}
      });
      return found[0] == null ? Optional.empty() : Optional.of(found[0]);
    } catch (ReflectiveOperationException e) {
      return Optional.empty();
    }
  }
}
