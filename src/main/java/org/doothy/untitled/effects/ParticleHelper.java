package org.doothy.untitled.effects;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Helper class for spawning particle effects.
 */
public class ParticleHelper {

    /**
     * Spawns sparks at the target point so the player knows where the bolt will land.
     *
     * @param level    The server level.
     * @param position The target position.
     */
    public static void spawnTargetingSparks(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                position.x, position.y + 0.1, position.z,
                3, 0.2, 0.1, 0.2, 0.05);
    }

    /**
     * Renders the Repel Zone Ring for the shield.
     *
     * @param level     The server level.
     * @param player    The player using the shield.
     * @param ticksUsed The number of ticks the item has been used.
     */
    public static void spawnShieldRing(ServerLevel level, Player player, int ticksUsed) {
        double radius = 2.0;
        double particleCount = 10;
        double increment = 2 * Math.PI / particleCount;
        double offset = (ticksUsed * 0.1); // Spin animation

        for (int i = 0; i < particleCount; i++) {
            double angle = (i * increment) + offset;
            double px = player.getX() + (Math.cos(angle) * radius);
            double pz = player.getZ() + (Math.sin(angle) * radius);

            // Count 0 sends particle to specific coords without velocity spread
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, player.getY(), pz, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Spawns particles on an entity hit by the shield.
     *
     * @param level  The server level.
     * @param target The entity hit.
     */
    public static void spawnShieldHit(ServerLevel level, Entity target) {
        level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + 1, target.getZ(), 3, 0.1, 0.1, 0.1, 0.05);
    }

    /**
     * Visual "Smoke puff" indicating shield failure.
     *
     * @param level  The server level.
     * @param player The player whose shield failed.
     */
    public static void spawnShieldFizzle(ServerLevel level, Player player) {
        for (int i = 0; i < 20; i++) {
            double angle = Math.random() * Math.PI * 2;
            double r = 1.5;
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX() + Math.cos(angle) * r,
                    player.getY() + 0.5,
                    player.getZ() + Math.sin(angle) * r,
                    1, 0, 0, 0, 0.05);
        }
    }

    /**
     * Renders an ARCED line of particles between two points for chain lightning.
     * Uses a Quadratic Bezier Curve to create a "jump" effect.
     * Layers END_ROD particles to create a lingering after-image.
     *
     * @param level The server level.
     * @param start The starting position.
     * @param end   The ending position.
     */
    public static void spawnChainLightningLink(ServerLevel level, Vec3 start, Vec3 end) {
        double distance = start.distanceTo(end);

        // 1. Calculate the Control Point (The peak of the arc)
        Vec3 midPoint = start.add(end).scale(0.5);
        // Height factor 0.6, might fiddle around with this number
        double arcHeight = Math.min(distance * 0.6, 8.0);
        Vec3 controlPoint = midPoint.add(0, arcHeight, 0);

        // 2. Determine density
        // 10 particles per block distance ensures a solid looking beam
        int particleCount = (int) (distance * 10);

        java.util.Random random = new java.util.Random();

        // 3. Interpolate along the curve
        for (int i = 0; i <= particleCount; i++) {
            double t = (double) i / particleCount;

            // --- THE BEZIER MATH ---
            // Formula: P = (1-t)^2 * A + 2(1-t)t * C + t^2 * B
            double aCoeff = Math.pow(1 - t, 2);
            double cCoeff = 2 * (1 - t) * t;
            double bCoeff = Math.pow(t, 2);

            double x = (aCoeff * start.x) + (cCoeff * controlPoint.x) + (bCoeff * end.x);
            double y = (aCoeff * start.y) + (cCoeff * controlPoint.y) + (bCoeff * end.y);
            double z = (aCoeff * start.z) + (cCoeff * controlPoint.z) + (bCoeff * end.z);

            // 4. Add Electric Jitter
            // We apply jitter to BOTH particles so they line up
            if (i > 0 && i < particleCount) {
                double chaos = 0.15;
                double jX = (random.nextDouble() - 0.5) * chaos;
                double jY = (random.nextDouble() - 0.5) * chaos;
                double jZ = (random.nextDouble() - 0.5) * chaos;
                x += jX; y += jY; z += jZ;
            }

            // 5. Spawn Particles

            // A. The Fizzle (instant)
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0, 0, 0);

            // B. The After-Image (Persists ~1 second)
            // We only spawn this every 2nd step to keep the bolt from looking too "thick"
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.END_ROD,
                        x, y, z,
                        1, 0, 0, 0, 0); // Speed 0 keeps it locked in place
            }
        }
    }

    /**
     * Creates a zig-zagging vertical line of flash particles from the sky to the ground.
     *
     * @param level The server level.
     * @param pos   The ground position where the bolt strikes.
     */
    public static void spawnDramaticBolt(ServerLevel level, Vec3 pos) {
        double curX = pos.x;
        double curZ = pos.z;
        java.util.Random random = new java.util.Random();
        ColorParticleOption flashWhite = ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF);

        // Sky-to-ground vertical effect
        for (double y = pos.y; y < pos.y + 55; y += 0.6) {
            curX += (random.nextDouble() - 0.5) * 0.6;
            curZ += (random.nextDouble() - 0.5) * 0.6;
            level.sendParticles(flashWhite, curX, y, curZ, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Creates an expanding ring of particles at the target position on impact.
     * Includes a Blast, a Sonic Boom, and a horizontal Pressure Wave.
     *
     * @param level The server level.
     * @param pos   The center of the shockwave.
     */
    public static void spawnShockwave(ServerLevel level, Vec3 pos) {
        // 1. The Core Blast (Explosion)
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0);

        // 2. The Vertical Sonic Distortion
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                pos.x, pos.y + 1.2, pos.z,
                1, 0, 0, 0, 0);

        // 3. The "Pressure Wave" (Expanding Ground Circle)
        int particleCount = 40;
        double speed = 0.6;

        for (int i = 0; i < particleCount; i++) {
            double angle = (Math.PI * 2 * i) / particleCount;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            // LAYER A: The "Dust" Wave
            level.sendParticles(ParticleTypes.CLOUD,
                    pos.x, pos.y + 0.1, pos.z,
                    0, dx, 0, dz, speed);

            // LAYER B: The "Electric" Edge
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y + 0.1, pos.z,
                    0, dx, 0, dz, speed * 1.2);
        }
    }
}