package net.discyupgrade.core.util;

import net.minecraft.resources.ResourceLocation;
import net.discyupgrade.DiscyUpgrade;

public class ModIdentifier extends ResourceLocation {
    public ModIdentifier(String path) {
        super(DiscyUpgrade.MOD_ID, path);
    }

    public static ResourceLocation of(String path) {
        return new ModIdentifier(path);
    }
}
