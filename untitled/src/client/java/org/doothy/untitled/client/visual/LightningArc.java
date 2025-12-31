package org.doothy.untitled.client.visual;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Utility for spawning a high-arched lightning link between two points using a quadratic Bezier curve.
 */
public final class LightningArc {

    private LightningArc() {}

    /**
     * Spawns a jittered Bezier arc between start and end.
     *
     * @param level             client level to spawn particles in
     * @param start             starting point of the arc (e.g., target position)
     * @param end               ending point of the arc (e.g., source/sky point)
     * @param chaos             max random offset per axis applied to interior points (e.g., 0.15)
     * @param densityPerBlock  particles per block of distance (e.g., 10.0)
     * @param maxPoints        safety upper bound on total points (e.g., 250)
     */
    public static void spawnBezierArc(
            ClientLevel level,
            Vec3 start,
            Vec3 end,
            double chaos,
            double densityPerBlock,
            int maxPoints
    ) {
        double distance = start.distanceTo(end);

        // Degenerate case: spawn a tiny burst at start and return
        if (distance < 0.05) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, start.x, start.y, start.z, 0, 0, 0);
            level.addParticle(ParticleTypes.END_ROD, start.x, start.y, start.z, 0, 0, 0);
            return;
        }

        // Control point: midpoint raised upward
        Vec3 midPoint = start.add(end).scale(0.5);
        double arcHeight = Math.min(distance * 0.6, 8.0);
        Vec3 controlPoint = midPoint.add(0, arcHeight, 0);

        // Particle density
        int particleCount = (int) Math.ceil(distance * densityPerBlock);
        particleCount = Math.max(2, Math.min(maxPoints, particleCount));

        RandomSource random = level.random;

        for (int i = 0; i <= particleCount; i++) {
            double t = (double) i / particleCount;

            // Quadratic Bezier: P = (1-t)^2*A + 2(1-t)t*C + t^2*B
            double aCoeff = Math.pow(1 - t, 2);
            double cCoeff = 2 * (1 - t) * t;
            double bCoeff = Math.pow(t, 2);

            double x = (aCoeff * start.x) + (cCoeff * controlPoint.x) + (bCoeff * end.x);
            double y = (aCoeff * start.y) + (cCoeff * controlPoint.y) + (bCoeff * end.y);
            double z = (aCoeff * start.z) + (cCoeff * controlPoint.z) + (bCoeff * end.z);

            // Apply jitter to interior points only
            if (i > 0 && i < particleCount) {
                double jX = (random.nextDouble() - 0.5) * chaos;
                double jY = (random.nextDouble() - 0.5) * chaos;
                double jZ = (random.nextDouble() - 0.5) * chaos;
                x += jX; y += jY; z += jZ;
            }

            // Fizzle core
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
            // After-image every 2nd step
            if ((i & 1) == 0) {
                level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
            }
        }
    }
}
