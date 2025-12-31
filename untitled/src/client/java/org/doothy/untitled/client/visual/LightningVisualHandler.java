package org.doothy.untitled.client.visual;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.network.payload.LightningVisualPayload;

public final class LightningVisualHandler {

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

    private static void spawnStrikeMarker(ClientLevel level, Vec3 pos) {
        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2 / 12) * i;
            level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.x + Math.cos(angle) * 0.4,
                    pos.y + 0.05,
                    pos.z + Math.sin(angle) * 0.4,
                    0, 0.02, 0
            );
        }
    }


    private static void spawnLightning(Vec3 hitPos, float charge) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        RandomSource random = level.random;
        double height = 12 + charge * 10; // keep a dramatic skyward endpoint

        // Per request: make the arc start at the target position and reach upward into the sky
        Vec3 start = hitPos;
        Vec3 end = hitPos.add(
                random.nextGaussian() * 0.3,
                height,
                random.nextGaussian() * 0.3
        );

        LightningArc.spawnBezierArc(level, start, end, 0.15, 10.0, 250);

        // Impact visuals — exactly once
        spawnStrikeMarker(level, hitPos);
        spawnShockwave(level, hitPos);
    }

    private static void spawnShockwave(ClientLevel level, Vec3 center) {
        int rings = 3;

        for (int r = 1; r <= rings; r++) {
            double radius = r * 0.6;

            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                level.addParticle(
                        ParticleTypes.CRIT,
                        center.x + Math.cos(angle) * radius,
                        center.y + 0.1,
                        center.z + Math.sin(angle) * radius,
                        Math.cos(angle) * 0.05,
                        0.02,
                        Math.sin(angle) * 0.05
                );
            }
        }
    }

    // straight-segment helper removed in favor of Bezier-based LightningArc
}
