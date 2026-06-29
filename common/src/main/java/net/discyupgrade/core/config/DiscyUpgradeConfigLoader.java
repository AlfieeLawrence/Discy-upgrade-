package net.discyupgrade.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Shared JSON config loader used by Fabric (Forge uses ForgeConfigSpec). */
public final class DiscyUpgradeConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DiscyUpgradeConfigLoader() {}

    public static void loadFromJson(Path path) {
        try {
            if (!Files.exists(path)) {
                saveDefaults(path);
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            apply(root);
        } catch (Exception ignored) {
            // keep defaults
        }
    }

    public static void saveDefaults(Path path) {
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("wirelessLinkRange", DiscyUpgradeConfig.wirelessLinkRange);
            root.addProperty("remoteOpenRange", DiscyUpgradeConfig.remoteOpenRange);
            root.addProperty("maxLinkedFloorsPerController", DiscyUpgradeConfig.maxLinkedFloorsPerController);
            root.addProperty("maxTilesPerGroup", DiscyUpgradeConfig.maxTilesPerGroup);
            root.addProperty("animationMaxDistance", DiscyUpgradeConfig.animationMaxDistance);
            root.addProperty("animationTickInterval", DiscyUpgradeConfig.animationTickInterval);
            root.addProperty("redstoneTriggersPatterns", DiscyUpgradeConfig.redstoneTriggersPatterns);
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException ignored) {}
    }

    private static void apply(JsonObject root) {
        if (root.has("wirelessLinkRange")) DiscyUpgradeConfig.wirelessLinkRange = root.get("wirelessLinkRange").getAsInt();
        if (root.has("remoteOpenRange")) DiscyUpgradeConfig.remoteOpenRange = root.get("remoteOpenRange").getAsInt();
        if (root.has("maxLinkedFloorsPerController")) DiscyUpgradeConfig.maxLinkedFloorsPerController = root.get("maxLinkedFloorsPerController").getAsInt();
        if (root.has("maxTilesPerGroup")) DiscyUpgradeConfig.maxTilesPerGroup = root.get("maxTilesPerGroup").getAsInt();
        if (root.has("animationMaxDistance")) DiscyUpgradeConfig.animationMaxDistance = root.get("animationMaxDistance").getAsInt();
        if (root.has("animationTickInterval")) DiscyUpgradeConfig.animationTickInterval = root.get("animationTickInterval").getAsInt();
        if (root.has("redstoneTriggersPatterns")) DiscyUpgradeConfig.redstoneTriggersPatterns = root.get("redstoneTriggersPatterns").getAsBoolean();
    }
}
