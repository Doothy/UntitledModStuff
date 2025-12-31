package org.doothy.untitled.client.visual;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.network.payload.LightningVisualPayload;

/**
 * Handles the client-side visual effects for lightning strikes.
 * <p>
 * This class registers the network receiver for {@link LightningVisualPayload}
 * and spawns the lightning bolt and impact shockwave particles when the packet is received.
 */
public final class LightningVisualHandler {

    /**
     * Registers the global receiver for lightning visual payloads.
     */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                LightningVisualPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() ->
                            spawnLightning(payload.pos(), payload.charge())
                    );
                }
        );

    }

    private static void spawnLightning(Vec3 hitPos, float charge) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        // Logic from ParticleHelper.spawnDramaticBolt
        double curX = hitPos.x;
        double curZ = hitPos.z;
        java.util.Random random = new java.util.Random();
        ColorParticleOption flashWhite = ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF);

        // sky-to-ground vertical effect with mild horizontal wander
        for (double y = hitPos.y; y < hitPos.y + 55; y += 0.6) {
            curX += (random.nextDouble() - 0.5) * 0.6;
            curZ += (random.nextDouble() - 0.5) * 0.6;
            level.addParticle(flashWhite, curX, y, curZ, 0, 0, 0);
        }

        // Impact visuals — exactly once
        spawnShockwave(level, hitPos);
    }

    private static void spawnShockwave(ClientLevel level, Vec3 pos) {
        // core blast at impact
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z,
                0, 0, 0);

        // vertical sonic distortion above the center
        level.addParticle(ParticleTypes.SONIC_BOOM,
                pos.x, pos.y + 1.2, pos.z,
                0, 0, 0);

        // expanding ground ring composed of two particle layers
        int particleCount = 40;
        double speed = 0.6;

        for (int i = 0; i < particleCount; i++) {
            double angle = (Math.PI * 2 * i) / particleCount;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            // layer A: dust wave
            level.addParticle(ParticleTypes.CLOUD,
                    pos.x, pos.y + 0.1, pos.z,
                    dx * speed, 0, dz * speed);

            // layer B: electric edge
            level.addParticle(ParticleTypes.END_ROD,
                    pos.x, pos.y + 0.1, pos.z,
                    dx * speed * 1.2, 0, dz * speed * 1.2);
        }
    }
}
