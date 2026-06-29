package net.discyupgrade.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.discyupgrade.core.block.DanceFloorTileBlockEntity;
import net.discyupgrade.core.block.DiscoBallBlockEntity;
import net.discyupgrade.core.block.LaserEmitterBlockEntity;
import net.discyupgrade.core.block.LightControllerBlockEntity;
import net.discyupgrade.core.floor.FloorGroupIndex;

import java.util.UUID;

public class TuningWrenchItem extends Item {
    public static final String TAG_GROUP = "TargetGroup";
    public static final String TAG_LIGHT_POS = "TargetLightPos";

    public TuningWrenchItem(Properties props) {
        super(props);
    }

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
            return InteractionResult.CONSUME;
        }

        if (be instanceof LightControllerBlockEntity controller) {
            if (!sneak && hasLightPos(stack)) {
                BlockPos lightPos = getLightPos(stack);
                if (level.getBlockEntity(lightPos) instanceof DiscoBallBlockEntity) {
                    controller.linkDiscoBall(lightPos);
                    player.displayClientMessage(Component.translatable(
                            "message.discyupgrade.wrench_linked_disco"), true);
                } else if (level.getBlockEntity(lightPos) instanceof LaserEmitterBlockEntity) {
                    controller.linkLaser(lightPos);
                    player.displayClientMessage(Component.translatable(
                            "message.discyupgrade.wrench_linked_laser"), true);
                }
                clearLightPos(stack);
            } else if (hasGroup(stack)) {
                controller.linkFloorGroup(getGroup(stack));
                player.displayClientMessage(Component.translatable(
                        "message.discyupgrade.wrench_linked_floor_wireless", shortId(getGroup(stack))), true);
            }
            return InteractionResult.CONSUME;
        }

        if (sneak && (be instanceof DiscoBallBlockEntity || be instanceof LaserEmitterBlockEntity)) {
            setLightPos(stack, pos);
            player.displayClientMessage(Component.translatable("message.discyupgrade.wrench_captured_light"), true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    public static boolean hasGroup(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID(TAG_GROUP);
    }

    public static UUID getGroup(ItemStack stack) {
        return stack.getTag().getUUID(TAG_GROUP);
    }

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
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof TuningWrenchItem) {
                setGroup(stack, groupId);
                return;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof TuningWrenchItem) {
                setGroup(stack, groupId);
                return;
            }
        }
    }

    private static String shortId(UUID id) {
        return id == null ? "?" : id.toString().substring(0, 8);
    }
}
