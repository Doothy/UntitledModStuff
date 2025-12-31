package org.doothy.untitled.client.sound;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.doothy.untitled.items.LightningStick;
import org.doothy.untitled.sound.ModSounds;

/**
 * Client-side handler that plays/stops Lightning Stick charge-related audio,
 * mirroring how visuals are handled.
 */
public final class LightningChargeAudioHandler {

    private static boolean wasCharging = false;
    private static SoundInstance activeChargeSound;

    private LightningChargeAudioHandler() {}

    /**
     * Registers the client tick listener for lightning charge audio.
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                stop();
                wasCharging = false;
                return;
            }

            boolean charging = client.player.isUsingItem()
                    && client.player.getUseItem().getItem() instanceof LightningStick;

            if (charging && !wasCharging) {
                start();
            } else if (!charging && wasCharging) {
                stop();
            }

            wasCharging = charging;
        });
    }

    private static void start() {
        if (activeChargeSound != null) return;

        activeChargeSound = new SimpleSoundInstance(
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

        Minecraft.getInstance().getSoundManager().play(activeChargeSound);
    }

    private static void stop() {
        if (activeChargeSound != null) {
            Minecraft.getInstance().getSoundManager().stop(activeChargeSound);
            activeChargeSound = null;
        }
    }
}
