package org.doothy.untitled.effects.sound;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class PositionedSoundEffect {

    private final SoundEvent sound;
    private final SoundSource source;
    private final float volume;
    private final float pitch;

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
