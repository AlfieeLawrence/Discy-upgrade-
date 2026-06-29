package net.discyupgrade.core.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.discyupgrade.DiscyUpgrade;
import net.discyupgrade.core.util.ModIdentifier;

public final class SoundEventRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(DiscyUpgrade.MOD_ID, Registries.SOUND_EVENT);
    public static final Registrar<SoundEvent> SOUND_REGISTRAR = SOUNDS.getRegistrar();

    public static final RegistrySupplier<SoundEvent> DISCO_BALL_SPIN = register("disco_ball_spin");
    public static final RegistrySupplier<SoundEvent> FLOOR_TICK = register("floor_tick");
    public static final RegistrySupplier<SoundEvent> LASER_HUM = register("laser_hum");
    public static final RegistrySupplier<SoundEvent> STROBE_CLICK = register("strobe_click");

    private static RegistrySupplier<SoundEvent> register(String path) {
        return SOUND_REGISTRAR.register(new ModIdentifier(path),
                () -> SoundEvent.createVariableRangeEvent(new ModIdentifier(path)));
    }

    public static void init() {
        SOUNDS.register();
    }
}
