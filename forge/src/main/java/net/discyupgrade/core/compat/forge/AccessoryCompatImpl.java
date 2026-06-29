package net.discyupgrade.core.compat.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

public class AccessoryCompatImpl {
  private AccessoryCompatImpl() {}

  public static Optional<ItemStack> findAccessory(Player player, Item item) {
    if (!ModList.get().isLoaded("curios")) return Optional.empty();
    try {
      Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
      Method getInv = api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
      Object optional = getInv.invoke(null, player);
      Method ifPresent = optional.getClass().getMethod("ifPresent", java.util.function.Consumer.class);
      final ItemStack[] found = new ItemStack[1];
      ifPresent.invoke(optional, (java.util.function.Consumer<Object>) handler -> {
        try {
          Method find = handler.getClass().getMethod("findFirstCurio", Item.class);
          Object stackOpt = find.invoke(handler, item);
          Method map = stackOpt.getClass().getMethod("map", java.util.function.Function.class);
          map.invoke(stackOpt, (java.util.function.Function<Object, Object>) slot -> {
            try {
              Method stack = slot.getClass().getMethod("stack");
              found[0] = (ItemStack) stack.invoke(slot);
            } catch (ReflectiveOperationException ignored) {}
            return null;
          });
        } catch (ReflectiveOperationException ignored) {}
      });
      return found[0] == null ? Optional.empty() : Optional.of(found[0]);
    } catch (ReflectiveOperationException e) {
      return Optional.empty();
    }
  }
}
