package net.discyupgrade.forge;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DiscyUpgradeForgeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue WIRELESS_LINK_RANGE;
    public static final ForgeConfigSpec.IntValue REMOTE_OPEN_RANGE;
    public static final ForgeConfigSpec.IntValue MAX_LINKED_FLOORS;
    public static final ForgeConfigSpec.IntValue MAX_TILES_PER_GROUP;
    public static final ForgeConfigSpec.IntValue ANIMATION_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue ANIMATION_TICK_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue REDSTONE_TRIGGERS_PATTERNS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("discyupgrade");
        WIRELESS_LINK_RANGE = builder.comment("Max blocks for wireless wrench floor linking")
                .defineInRange("wirelessLinkRange", 64, 8, 256);
        REMOTE_OPEN_RANGE = builder.comment("Max blocks to open controller GUI with light remote")
                .defineInRange("remoteOpenRange", 32, 4, 128);
        MAX_LINKED_FLOORS = builder.comment("Max floor groups per light controller")
                .defineInRange("maxLinkedFloorsPerController", 8, 1, 32);
        MAX_TILES_PER_GROUP = builder.comment("Max tiles in one floor group")
                .defineInRange("maxTilesPerGroup", 256, 16, 1024);
        ANIMATION_MAX_DISTANCE = builder.comment("Skip floor animation when no player is within this range")
                .defineInRange("animationMaxDistance", 64, 16, 256);
        ANIMATION_TICK_INTERVAL = builder.comment("Run floor animation every N server ticks (1 = every tick)")
                .defineInRange("animationTickInterval", 1, 1, 10);
        REDSTONE_TRIGGERS_PATTERNS = builder.comment("Redstone signal to controller forces patterns to play")
                .define("redstoneTriggersPatterns", true);
        builder.pop();
        SPEC = builder.build();
    }

    private DiscyUpgradeForgeConfig() {}
}
