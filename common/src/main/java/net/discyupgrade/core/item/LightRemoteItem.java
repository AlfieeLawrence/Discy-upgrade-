package net.discyupgrade.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.LightControllerBlockEntity;
import net.discyupgrade.core.config.DiscyUpgradeConfig;
import net.discyupgrade.core.screen.LightControllerMenus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LightRemoteItem extends Item {
    public static final String TAG_CONTROLLER = "ControllerPos";

    public LightRemoteItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!hasController(stack) || level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        BlockPos pos = getController(stack);
        if (player instanceof ServerPlayer sp) {
            double range = DiscyUpgradeConfig.remoteOpenRange;
            if (sp.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > range * range) {
                sp.displayClientMessage(Component.translatable("message.discyupgrade.remote_too_far"), true);
                return InteractionResultHolder.fail(stack);
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LightControllerBlockEntity controller) {
                LightControllerMenus.open(sp, controller);
            } else {
                sp.displayClientMessage(Component.translatable("message.discyupgrade.remote_invalid"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasController(stack)) {
            BlockPos pos = getController(stack);
            tooltip.add(Component.translatable("tooltip.discyupgrade.remote_linked", pos.getX(), pos.getY(), pos.getZ()));
        } else {
            tooltip.add(Component.translatable("tooltip.discyupgrade.remote_unlinked"));
        }
    }

    public static boolean hasController(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_CONTROLLER);
    }

    public static BlockPos getController(ItemStack stack) {
        return BlockPos.of(stack.getTag().getLong(TAG_CONTROLLER));
    }

    public static void link(ItemStack stack, BlockPos controllerPos) {
        stack.getOrCreateTag().putLong(TAG_CONTROLLER, controllerPos.asLong());
    }
}
