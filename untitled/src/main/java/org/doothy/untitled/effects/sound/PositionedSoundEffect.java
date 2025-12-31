package org.doothy.untitled.effects.sound;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a sound effect that can be played at a specific position in the world.
 */
public class PositionedSoundEffect {

    private final SoundEvent sound;
    private final SoundSource source;
    private final float volume;
    private final float pitch;

    /**
     * Creates a new positioned sound effect.
     *
     * @param sound  the sound event to play
     * @param source the sound source category
     * @param volume the volume of the sound
     * @param pitch  the pitch of the sound
     */
    public PositionedSoundEffect(
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch
    ) {
        this.sound = sound;
        this.source = source;
        this.volume = volume;
        this.pitch = pitch;
    }

    /**
     * Plays the sound effect at the specified position in the given level.
     *
     * @param level the server level to play the sound in
     * @param pos   the position to play the sound at
     */
    public void play(ServerLevel level, Vec3 pos) {
        level.playSound(
                null,
                pos.x,
                pos.y,
                pos.z,
                sound,
                source,
                volume,
                pitch
        );
    }
}
