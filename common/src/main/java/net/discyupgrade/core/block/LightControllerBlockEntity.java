package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.discyupgrade.core.registry.BlockEntityRegistry;

import java.util.*;

public class LightControllerBlockEntity extends BlockEntity {
    private final List<UUID> linkedFloorGroups = new ArrayList<>();
    private final List<BlockPos> linkedDiscoBalls = new ArrayList<>();
    private final List<BlockPos> linkedLasers = new ArrayList<>();
    private UUID selectedFloorGroup;
    private boolean discoSpinEnabled = true;

    public LightControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.LIGHT_CONTROLLER.get(), pos, state);
    }

    public List<UUID> getLinkedFloorGroups() {
        return Collections.unmodifiableList(linkedFloorGroups);
    }

    public List<BlockPos> getLinkedDiscoBalls() {
        return Collections.unmodifiableList(linkedDiscoBalls);
    }

    public List<BlockPos> getLinkedLasers() {
        return Collections.unmodifiableList(linkedLasers);
    }

    public UUID getSelectedFloorGroup() {
        return selectedFloorGroup;
    }

    public void setSelectedFloorGroup(UUID selectedFloorGroup) {
        this.selectedFloorGroup = selectedFloorGroup;
        setChanged();
    }

    public boolean isDiscoSpinEnabled() {
        return discoSpinEnabled;
    }

    public void setDiscoSpinEnabled(boolean discoSpinEnabled) {
        this.discoSpinEnabled = discoSpinEnabled;
        setChanged();
        applyDiscoSpin();
    }

    public void linkFloorGroup(UUID groupId) {
        if (groupId == null || linkedFloorGroups.contains(groupId)) return;
        linkedFloorGroups.add(groupId);
        if (selectedFloorGroup == null) selectedFloorGroup = groupId;
        setChanged();
    }

    public void unlinkFloorGroup(UUID groupId) {
        if (linkedFloorGroups.remove(groupId)) {
            if (groupId.equals(selectedFloorGroup)) {
                selectedFloorGroup = linkedFloorGroups.isEmpty() ? null : linkedFloorGroups.get(0);
            }
            setChanged();
        }
    }

    public void linkDiscoBall(BlockPos pos) {
        if (pos != null && !linkedDiscoBalls.contains(pos.immutable())) {
            linkedDiscoBalls.add(pos.immutable());
            setChanged();
        }
    }

    public void unlinkDiscoBall(BlockPos pos) {
        if (linkedDiscoBalls.remove(pos)) setChanged();
    }

    public void linkLaser(BlockPos pos) {
        if (pos != null && !linkedLasers.contains(pos.immutable())) {
            linkedLasers.add(pos.immutable());
            setChanged();
        }
    }

    public void unlinkLaser(BlockPos pos) {
        if (linkedLasers.remove(pos)) setChanged();
    }

    public void linkTouchingFloors() {
        if (level == null) return;
        for (UUID id : net.discyupgrade.core.floor.FloorGroups.findTouchingGroups(level, worldPosition)) {
            linkFloorGroup(id);
        }
    }

    public void pruneBrokenLinks() {
        if (level == null) return;
        linkedDiscoBalls.removeIf(pos -> !(level.getBlockEntity(pos) instanceof DiscoBallBlockEntity));
        linkedLasers.removeIf(pos -> !(level.getBlockEntity(pos) instanceof LaserEmitterBlockEntity));
        linkedFloorGroups.removeIf(id -> net.discyupgrade.core.floor.FloorGroupIndex.getMembers(id).isEmpty());
        if (selectedFloorGroup != null && !linkedFloorGroups.contains(selectedFloorGroup)) {
            selectedFloorGroup = linkedFloorGroups.isEmpty() ? null : linkedFloorGroups.get(0);
        }
        setChanged();
    }

    public void toggleAllDiscos() {
        if (level == null) return;
        for (BlockPos pos : linkedDiscoBalls) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DiscoBallBlockEntity ball) {
                ball.toggleActive();
                level.sendBlockUpdated(pos, ball.getBlockState(), ball.getBlockState(), 3);
            }
        }
    }

    public void applyDiscoSpin() {
        if (level == null) return;
        for (BlockPos pos : linkedDiscoBalls) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DiscoBallBlockEntity ball) {
                if (!discoSpinEnabled && ball.isActive()) {
                    ball.setActive(false);
                    level.sendBlockUpdated(pos, ball.getBlockState(), ball.getBlockState(), 3);
                }
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedFloorGroups.clear();
        ListTag floors = tag.getList("FloorGroups", Tag.TAG_STRING);
        for (Tag t : floors) linkedFloorGroups.add(UUID.fromString(t.getAsString()));
        linkedDiscoBalls.clear();
        ListTag discos = tag.getList("DiscoBalls", Tag.TAG_LONG);
        for (Tag t : discos) linkedDiscoBalls.add(BlockPos.of(((net.minecraft.nbt.LongTag) t).getAsLong()));
        linkedLasers.clear();
        ListTag lasers = tag.getList("Lasers", Tag.TAG_LONG);
        for (Tag t : lasers) linkedLasers.add(BlockPos.of(((net.minecraft.nbt.LongTag) t).getAsLong()));
        selectedFloorGroup = tag.hasUUID("SelectedFloor") ? tag.getUUID("SelectedFloor") : null;
        discoSpinEnabled = !tag.contains("DiscoSpin") || tag.getBoolean("DiscoSpin");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag floors = new ListTag();
        for (UUID id : linkedFloorGroups) floors.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("FloorGroups", floors);
        ListTag discos = new ListTag();
        for (BlockPos p : linkedDiscoBalls) discos.add(net.minecraft.nbt.LongTag.valueOf(p.asLong()));
        tag.put("DiscoBalls", discos);
        ListTag lasers = new ListTag();
        for (BlockPos p : linkedLasers) lasers.add(net.minecraft.nbt.LongTag.valueOf(p.asLong()));
        tag.put("Lasers", lasers);
        if (selectedFloorGroup != null) tag.putUUID("SelectedFloor", selectedFloorGroup);
        tag.putBoolean("DiscoSpin", discoSpinEnabled);
    }
}
