package net.discyupgrade.core.item;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class DiscoGuideItem extends Item {
    public DiscoGuideItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        var tag = book.getOrCreateTag();
        tag.putString("title", Component.translatable("item.discyupgrade.disco_guide").getString());
        tag.putString("author", "Disco Lights");
        ListTag pages = new ListTag();
        for (int i = 1; i <= 5; i++) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.translatable("book.discyupgrade.page" + i))));
        }
        tag.put("pages", pages);

        if (!player.getInventory().add(book)) player.drop(book, false);
        return InteractionResultHolder.success(stack);
    }
}
