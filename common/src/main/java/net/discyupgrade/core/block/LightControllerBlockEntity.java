package net.discyupgrade.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.discyupgrade.core.floor.FloorAnimationEngine;
import net.discyupgrade.core.floor.FloorPattern;
import net.discyupgrade.core.floor.FloorPatternPresets;
import net.discyupgrade.core.registry.BlockEntityRegistry;

import java.util.*;

public class LightControllerBlockEntity extends BlockEntity {
    public record GroupAnimSettings(FloorPattern pattern, int speed, boolean playing, boolean syncDisco, boolean syncJukebox) {
        public static GroupAnimSettings defaults() {
            return new GroupAnimSettings(FloorPattern.STATIC, 5, false, false, false);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Pattern", pattern.id());
            tag.putInt("Speed", speed);
            tag.putBoolean("Playing", playing);
            tag.putBoolean("SyncDisco", syncDisco);
            tag.putBoolean("SyncJukebox", syncJukebox);
            return tag;
        }

        public static GroupAnimSettings fromTag(CompoundTag tag) {
            return new GroupAnimSettings(
                    FloorPattern.fromId(tag.getInt("Pattern")),
                    tag.getInt("Speed"),
                    tag.getBoolean("Playing"),
                    tag.getBoolean("SyncDisco"),
                    tag.contains("SyncJukebox") && tag.getBoolean("SyncJukebox"));
        }
    }

    private final List<UUID> linkedFloorGroups = new ArrayList<>();
    private final List<BlockPos> linkedDiscoBalls = new ArrayList<>();
    private final List<BlockPos> linkedLasers = new ArrayList<>();
    private final List<BlockPos> linkedPartyLights = new ArrayList<>();
    private final List<BlockPos> linkedStrobes = new ArrayList<>();
    private final List<BlockPos> linkedJukeboxes = new ArrayList<>();
    private final Map<UUID, GroupAnimSettings> groupSettings = new HashMap<>();
    private final Map<String, Map<Long, Integer>> presets = new LinkedHashMap<>();

    private UUID selectedFloorGroup;
    private boolean discoSpinEnabled = true;
    private boolean redstonePowered;

    public LightControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.LIGHT_CONTROLLER.get(), pos, state);
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (level != null && !level.isClientSide) {
            FloorAnimationEngine.registerController(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            FloorAnimationEngine.unregisterController(level, worldPosition);
        }
        super.setRemoved();
    }

    public List<UUID> getLinkedFloorGroups() { return Collections.unmodifiableList(linkedFloorGroups); }
    public List<BlockPos> getLinkedDiscoBalls() { return Collections.unmodifiableList(linkedDiscoBalls); }
    public List<BlockPos> getLinkedLasers() { return Collections.unmodifiableList(linkedLasers); }
    public List<BlockPos> getLinkedPartyLights() { return Collections.unmodifiableList(linkedPartyLights); }
    public List<BlockPos> getLinkedStrobes() { return Collections.unmodifiableList(linkedStrobes); }
    public List<BlockPos> getLinkedJukeboxes() { return Collections.unmodifiableList(linkedJukeboxes); }
    public Map<String, Map<Long, Integer>> getPresets() { return Collections.unmodifiableMap(presets); }

    public UUID getSelectedFloorGroup() { return selectedFloorGroup; }
    public void setSelectedFloorGroup(UUID selectedFloorGroup) { this.selectedFloorGroup = selectedFloorGroup; setChanged(); }

    public boolean isDiscoSpinEnabled() { return discoSpinEnabled; }
    public void setDiscoSpinEnabled(boolean discoSpinEnabled) {
        this.discoSpinEnabled = discoSpinEnabled;
        setChanged();
        applyDiscoSpin();
    }

    public boolean isRedstonePowered() { return redstonePowered; }
    public void setRedstonePowered(boolean redstonePowered) {
        if (this.redstonePowered == redstonePowered) return;
        this.redstonePowered = redstonePowered;
        setChanged();
    }

    public GroupAnimSettings getGroupSettings(UUID groupId) {
        return groupSettings.getOrDefault(groupId, GroupAnimSettings.defaults());
    }

    public void setGroupSettings(UUID groupId, GroupAnimSettings settings) {
        groupSettings.put(groupId, settings);
        setChanged();
    }

    public void linkFloorGroup(UUID groupId) {
        if (groupId == null || linkedFloorGroups.contains(groupId)) return;
        linkedFloorGroups.add(groupId);
        groupSettings.putIfAbsent(groupId, GroupAnimSettings.defaults());
        if (selectedFloorGroup == null) selectedFloorGroup = groupId;
        setChanged();
    }

    public void unlinkFloorGroup(UUID groupId) {
        if (linkedFloorGroups.remove(groupId)) {
            groupSettings.remove(groupId);
            if (groupId.equals(selectedFloorGroup)) {
                selectedFloorGroup = linkedFloorGroups.isEmpty() ? null : linkedFloorGroups.get(0);
            }
            setChanged();
        }
    }

    public void linkDiscoBall(BlockPos pos) { linkPos(linkedDiscoBalls, pos); }
    public void unlinkDiscoBall(BlockPos pos) { linkedDiscoBalls.remove(pos); setChanged(); }
    public void linkLaser(BlockPos pos) { linkPos(linkedLasers, pos); }
    public void unlinkLaser(BlockPos pos) { linkedLasers.remove(pos); setChanged(); }
    public void linkPartyLight(BlockPos pos) { linkPos(linkedPartyLights, pos); }
    public void linkStrobe(BlockPos pos) { linkPos(linkedStrobes, pos); }
    public void linkJukebox(BlockPos pos) { linkPos(linkedJukeboxes, pos); }

    private void linkPos(List<BlockPos> list, BlockPos pos) {
        if (pos != null && !list.contains(pos.immutable())) {
            list.add(pos.immutable());
            setChanged();
        }
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
        linkedPartyLights.removeIf(pos -> !(level.getBlockEntity(pos) instanceof PartyLightBlockEntity));
        linkedStrobes.removeIf(pos -> !(level.getBlockEntity(pos) instanceof StrobeLightBlockEntity));
        linkedJukeboxes.removeIf(pos -> !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.JUKEBOX));
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
            if (be instanceof DiscoBallBlockEntity ball && !discoSpinEnabled && ball.isActive()) {
                ball.setActive(false);
                level.sendBlockUpdated(pos, ball.getBlockState(), ball.getBlockState(), 3);
            }
        }
    }

    public void savePreset(String name, UUID groupId) {
        if (level == null || name == null || name.isBlank()) return;
        presets.put(name.trim(), FloorPatternPresets.snapshotGroup(level, groupId));
        setChanged();
    }

    public void applyPreset(String name, UUID groupId) {
        if (level == null) return;
        Map<Long, Integer> colors = presets.get(name);
        if (colors != null) FloorPatternPresets.applyPreset(level, groupId, colors);
    }

    public void copyGroupColors(UUID from, UUID to) {
        if (level == null) return;
        Map<Long, Integer> snap = FloorPatternPresets.snapshotGroup(level, from);
        FloorPatternPresets.applyPreset(level, to, snap);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        linkedFloorGroups.clear();
        for (Tag t : tag.getList("FloorGroups", Tag.TAG_STRING)) {
            linkedFloorGroups.add(UUID.fromString(t.getAsString()));
        }
        linkedDiscoBalls.clear();
        readPosList(tag, "DiscoBalls", linkedDiscoBalls);
        linkedLasers.clear();
        readPosList(tag, "Lasers", linkedLasers);
        linkedPartyLights.clear();
        readPosList(tag, "PartyLights", linkedPartyLights);
        linkedStrobes.clear();
        readPosList(tag, "Strobes", linkedStrobes);
        linkedJukeboxes.clear();
        readPosList(tag, "Jukeboxes", linkedJukeboxes);
        groupSettings.clear();
        CompoundTag anim = tag.getCompound("GroupAnim");
        for (String key : anim.getAllKeys()) {
            groupSettings.put(UUID.fromString(key), GroupAnimSettings.fromTag(anim.getCompound(key)));
        }
        presets.clear();
        presets.putAll(FloorPatternPresets.readPresets(tag.getList("Presets", Tag.TAG_COMPOUND)));
        selectedFloorGroup = tag.hasUUID("SelectedFloor") ? tag.getUUID("SelectedFloor") : null;
        discoSpinEnabled = !tag.contains("DiscoSpin") || tag.getBoolean("DiscoSpin");
        redstonePowered = tag.getBoolean("Redstone");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag floors = new ListTag();
        for (UUID id : linkedFloorGroups) floors.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("FloorGroups", floors);
        tag.put("DiscoBalls", posList(linkedDiscoBalls));
        tag.put("Lasers", posList(linkedLasers));
        tag.put("PartyLights", posList(linkedPartyLights));
        tag.put("Strobes", posList(linkedStrobes));
        tag.put("Jukeboxes", posList(linkedJukeboxes));
        CompoundTag anim = new CompoundTag();
        for (var e : groupSettings.entrySet()) anim.put(e.getKey().toString(), e.getValue().toTag());
        tag.put("GroupAnim", anim);
        tag.put("Presets", FloorPatternPresets.writePresets(presets));
        if (selectedFloorGroup != null) tag.putUUID("SelectedFloor", selectedFloorGroup);
        tag.putBoolean("DiscoSpin", discoSpinEnabled);
        tag.putBoolean("Redstone", redstonePowered);
    }

    private static void readPosList(CompoundTag tag, String key, List<BlockPos> out) {
        for (Tag t : tag.getList(key, Tag.TAG_LONG)) {
            out.add(BlockPos.of(((net.minecraft.nbt.LongTag) t).getAsLong()));
        }
    }

    private static ListTag posList(List<BlockPos> positions) {
        ListTag list = new ListTag();
        for (BlockPos p : positions) list.add(net.minecraft.nbt.LongTag.valueOf(p.asLong()));
        return list;
    }
}
