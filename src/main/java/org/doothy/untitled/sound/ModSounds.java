package org.doothy.untitled.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.doothy.untitled.Untitled;

public class ModSounds {

    // These Identifiers must match the names used in your sounds.json
    public static final SoundEvent THUNDER = registerSound("thunder");
    public static final SoundEvent THUNDER_HIT = registerSound("thunder_hit");

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Untitled.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() {
        // This just ensures the static fields are loaded and registered
    }
}
