package org.doothy.untitled.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.doothy.untitled.items.LightningSoundHelper;
import org.doothy.untitled.sound.ModSounds;

/**
 * Client-side implementation of the LightningSoundHelper.
 * Handles playing the thunder sound and applying visual recoil.
 */
public class ClientLightningSoundHelper implements LightningSoundHelper {

    private SoundInstance activeThunderSound;

    @Override
    public void start() {
        // Prevent double-start in the same windup
        if (activeThunderSound != null) return;

        activeThunderSound = new SimpleSoundInstance(
                ModSounds.THUNDER.location(),
                SoundSource.PLAYERS,
                2.0f,
                1.0f,
                SoundInstance.createUnseededRandom(),
                false, // one-shot
                0,
                SoundInstance.Attenuation.NONE,
                0.0, 0.0, 0.0,
                true
        );

        Minecraft.getInstance().getSoundManager().play(activeThunderSound);
    }

    @Override
    public void stop() {
        if (activeThunderSound != null) {
            Minecraft.getInstance().getSoundManager().stop(activeThunderSound);
            activeThunderSound = null;
        }
    }

    @Override
    public void applyRecoil() { net.minecraft.client.Minecraft mc =
            net.minecraft.client.Minecraft.getInstance(); if (mc.player != null) {
        // 1. Camera Kick (Subtle)
        mc.player.setXRot(mc.player.getXRot() - 5.0f);
        // 2. Physical Shove (Recoil)
        // This pushes the player backward based on where they are looking
        net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
        mc.player.addDeltaMovement(new net.minecraft.world.phys.Vec3(
                look.x * -0.2, 0.05, look.z * -0.2 ));
        }
    }
}
