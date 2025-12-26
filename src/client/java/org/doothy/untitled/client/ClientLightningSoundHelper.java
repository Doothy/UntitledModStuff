package org.doothy.untitled.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.doothy.untitled.items.LightningSoundHelper;
import org.doothy.untitled.sound.ModSounds;

public class ClientLightningSoundHelper implements LightningSoundHelper {
    private SoundInstance activeThunderSound;

    @Override
    public void start() {
        stop();
        // Using a volume of 2.0f makes it much more prominent in the mix
        activeThunderSound = new SimpleSoundInstance(
                ModSounds.THUNDER.location(),
                SoundSource.PLAYERS,
                2.0f, // Volume (Loudness)
                1.0f, // Pitch
                SoundInstance.createUnseededRandom(),
                false, // Looping
                0,
                SoundInstance.Attenuation.NONE, // Ignore distance
                0.0, 0.0, 0.0,
                true // Relative (stays with the player)
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
    public void applyRecoil() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            // 1. Camera Kick (Subtle)
            mc.player.setXRot(mc.player.getXRot() - 5.0f);

            // 2. Physical Shove (Recoil)
            // This pushes the player backward based on where they are looking
            net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
            mc.player.addDeltaMovement(new net.minecraft.world.phys.Vec3(
                    look.x * -0.2,
                    0.05,
                    look.z * -0.2
            ));
        }
    }
}