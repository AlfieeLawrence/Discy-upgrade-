package net.discyupgrade.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.*;
import net.discyupgrade.core.compat.AccessoryCompat;
import net.discyupgrade.core.config.DiscyUpgradeConfig;
import net.discyupgrade.core.floor.FloorGroupIndex;
import net.discyupgrade.core.network.DiscyUpgradeNetworking;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class TuningWrenchItem extends Item {
    public static final String TAG_GROUP = "TargetGroup";
    public static final String TAG_LIGHT_POS = "TargetLightPos";

    public TuningWrenchItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (player == null || level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        boolean sneak = player.isShiftKeyDown();

        if (be instanceof DanceFloorTileBlockEntity) {
            if (sneak) {
                if (level instanceof ServerLevel sl) {
                    FloorGroupIndex.disconnectTile(sl, pos);
                    player.displayClientMessage(Component.translatable(
                            "message.discyupgrade.wrench_disconnected_floor"), true);
                }
            } else if (hasGroup(stack)) {
                UUID target = getGroup(stack);
                FloorGroupIndex.assignTileToGroup((ServerLevel) level, pos, target);
                player.displayClientMessage(Component.translatable(
                        "message.discyupgrade.wrench_assigned_floor", shortId(target)), true);
            } else {
                UUID captured = FloorGroupIndex.getTileGroup(level, pos);
                if (captured != null) {
                    setGroup(stack, captured);
                    player.displayClientMessage(Component.translatable(
                            "message.discyupgrade.wrench_captured_floor", shortId(captured)), true);
                }
            }
            DiscyUpgradeNetworking.syncOpenMenu(player);
            return InteractionResult.CONSUME;
        }

        if (be instanceof LightControllerBlockEntity controller) {
            ItemStack off = player.getOffhandItem();
            if (!sneak && off.getItem() instanceof LightRemoteItem) {
                LightRemoteItem.link(off, controller.getBlockPos());
                player.displayClientMessage(Component.translatable("message.discyupgrade.remote_linked"), true);
                return InteractionResult.CONSUME;
            }
            if (sneak && hasGroup(stack)) {
                controller.unlinkFloorGroup(getGroup(stack));
                player.displayClientMessage(Component.translatable(
                        "message.discyupgrade.wrench_unlinked_floor", shortId(getGroup(stack))), true);
            } else if (!sneak && hasLightPos(stack)) {
                linkCapturedLight(controller, level, getLightPos(stack), player);
                clearLightPos(stack);
            } else if (hasGroup(stack)) {
                if (!withinWirelessRange(player, pos)) return InteractionResult.FAIL;
                if (controller.getLinkedFloorGroups().size() >= DiscyUpgradeConfig.maxLinkedFloorsPerController) {
                    player.displayClientMessage(Component.translatable("message.discyupgrade.max_floors"), true);
                    return InteractionResult.FAIL;
                }
                controller.linkFloorGroup(getGroup(stack));
                player.displayClientMessage(Component.translatable(
                        "message.discyupgrade.wrench_linked_floor_wireless", shortId(getGroup(stack))), true);
            }
            DiscyUpgradeNetworking.syncOpenMenu(player);
            return InteractionResult.CONSUME;
        }

        if (sneak) {
            if (be instanceof DiscoBallBlockEntity || be instanceof LaserEmitterBlockEntity
                    || be instanceof PartyLightBlockEntity || be instanceof StrobeLightBlockEntity
                    || level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.JUKEBOX)) {
                setLightPos(stack, pos);
                player.displayClientMessage(Component.translatable(
                        level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.JUKEBOX)
                                ? "message.discyupgrade.wrench_captured_jukebox"
                                : "message.discyupgrade.wrench_captured_light"), true);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    private static void linkCapturedLight(LightControllerBlockEntity controller, Level level,
                                          BlockPos lightPos, Player player) {
        BlockEntity light = level.getBlockEntity(lightPos);
        if (light instanceof DiscoBallBlockEntity) {
            controller.linkDiscoBall(lightPos);
            player.displayClientMessage(Component.translatable("message.discyupgrade.wrench_linked_disco"), true);
        } else if (light instanceof LaserEmitterBlockEntity) {
            controller.linkLaser(lightPos);
            player.displayClientMessage(Component.translatable("message.discyupgrade.wrench_linked_laser"), true);
        } else if (light instanceof PartyLightBlockEntity) {
            controller.linkPartyLight(lightPos);
            player.displayClientMessage(Component.translatable("message.discyupgrade.wrench_linked_party"), true);
        } else if (light instanceof StrobeLightBlockEntity) {
            controller.linkStrobe(lightPos);
            player.displayClientMessage(Component.translatable("message.discyupgrade.wrench_linked_strobe"), true);
        } else if (level.getBlockState(lightPos).is(net.minecraft.world.level.block.Blocks.JUKEBOX)) {
            controller.linkJukebox(lightPos);
            player.displayClientMessage(Component.translatable("message.discyupgrade.wrench_linked_jukebox"), true);
        }
    }

    private static boolean withinWirelessRange(Player player, BlockPos target) {
        double range = DiscyUpgradeConfig.wirelessLinkRange;
        return player.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) <= range * range;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasGroup(stack)) {
            UUID id = getGroup(stack);
            tooltip.add(Component.translatable("tooltip.discyupgrade.wrench_group",
                    FloorGroupIndex.getGroupName(id), shortId(id)));
        }
        if (hasLightPos(stack)) {
            BlockPos p = getLightPos(stack);
            tooltip.add(Component.translatable("tooltip.discyupgrade.wrench_light", p.getX(), p.getY(), p.getZ()));
        }
        if (!hasGroup(stack) && !hasLightPos(stack)) {
            tooltip.add(Component.translatable("tooltip.discyupgrade.wrench_empty"));
        }
    }

    public static boolean hasGroup(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID(TAG_GROUP);
    }

    public static UUID getGroup(ItemStack stack) { return stack.getTag().getUUID(TAG_GROUP); }

    public static void setGroup(ItemStack stack, UUID groupId) {
        stack.getOrCreateTag().putUUID(TAG_GROUP, groupId);
    }

    public static boolean hasLightPos(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_LIGHT_POS);
    }

    public static BlockPos getLightPos(ItemStack stack) {
        return BlockPos.of(stack.getTag().getLong(TAG_LIGHT_POS));
    }

    public static void setLightPos(ItemStack stack, BlockPos pos) {
        stack.getOrCreateTag().putLong(TAG_LIGHT_POS, pos.asLong());
    }

    public static void clearLightPos(ItemStack stack) {
        if (stack.hasTag()) stack.getTag().remove(TAG_LIGHT_POS);
    }

    public static void setWrenchGroupFromController(ServerPlayer player, UUID groupId) {
        AccessoryCompat.findAnywhere(player, net.discyupgrade.core.registry.ItemRegistry.TUNING_WRENCH.get())
                .ifPresent(stack -> setGroup(stack, groupId));
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof TuningWrenchItem) setGroup(stack, groupId);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof TuningWrenchItem) setGroup(stack, groupId);
        }
    }

    private static String shortId(UUID id) {
        return id == null ? "?" : id.toString().substring(0, 8);
    }
}
