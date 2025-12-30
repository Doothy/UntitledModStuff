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

    private static void spawnLightning(Vec3 hitPos, float charge) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        RandomSource random = level.random;

        int segments = 10 + (int)(charge * 15);
        double height = 12 + charge * 10;

        Vec3 start = hitPos.add(
                random.nextGaussian() * 0.3,
                height,
                random.nextGaussian() * 0.3
        );

        Vec3 prev = start;

        for (int i = 0; i < segments; i++) {
            float t = (float)i / segments;

            Vec3 next = start.lerp(hitPos, t).add(
                    random.nextGaussian() * 0.2,
                    random.nextGaussian() * 0.2,
                    random.nextGaussian() * 0.2
            );

            spawnArc(level, prev, next);
            prev = next;
        }
    }

    private static void spawnArc(ClientLevel level, Vec3 a, Vec3 b) {
        Vec3 delta = b.subtract(a);
        int steps = 4;

        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.add(delta.scale(i / (double)steps));
            level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    p.x, p.y, p.z,
                    0, 0, 0
            );
        }
    }
}
