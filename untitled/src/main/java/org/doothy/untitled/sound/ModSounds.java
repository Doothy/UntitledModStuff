package org.doothy.untitled.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.doothy.untitled.Untitled;

/**
 * Registry class for all custom sound events in the mod.
 */
public class ModSounds {

    // These Identifiers must match the names used in your sounds.json
    /** Thunder sound event. */
    public static final SoundEvent THUNDER = registerSound("thunder");
    /** Thunder hit sound event. */
    public static final SoundEvent THUNDER_HIT = registerSound("thunder_hit");

    /** Avada Kedavra sound event. */
    public static final SoundEvent AVADAKEDAVRA = registerSound("avadakedavra");

    /**
     * Registers a sound event.
     *
     * @param name The registry name of the sound.
     * @return The registered sound event.
     */
    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Untitled.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /**
     * Initializes the sound events.
     */
    public static void initialize() {
        // This just ensures the static fields are loaded and registered
    }
}
