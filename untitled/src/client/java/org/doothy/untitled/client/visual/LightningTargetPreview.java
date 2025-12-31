package org.doothy.untitled.client.visual;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Manages the visual preview of the lightning target.
 * <p>
 * This class handles showing a marker at the target position, which is refreshed
 * every tick while charging. The marker consists of electric spark particles
 * arranged in a circle.
 */
public final class LightningTargetPreview {

    private static Vec3 currentPos;
    private static int ttl;

    private LightningTargetPreview() {}

    /**
     * Shows the target preview at the specified position.
     *
     * @param pos the position to show the preview at
     */
    public static void show(Vec3 pos) {
        currentPos = pos;
        ttl = 2; // refreshed every tick while charging
    }

    /**
     * Clears the current target preview.
     */
    public static void clear() {
        currentPos = null;
        ttl = 0;
    }

    /**
     * Ticks the target preview, spawning particles if active.
     *
     * @param level the client level to spawn particles in
     */
    public static void tick(ClientLevel level) {
        if (currentPos == null) return;
        if (--ttl <= 0) {
            currentPos = null;
            return;
        }

        spawnMarker(level, currentPos);
    }

    private static void spawnMarker(ClientLevel level, Vec3 pos) {
        Vec3 p = pos.add(0, 0.25, 0);

        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2 / 12) * i;
            level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    p.x + Math.cos(angle) * 0.45,
                    p.y,
                    p.z + Math.sin(angle) * 0.45,
                    0, 0.02, 0
            );
        }
    }
}
