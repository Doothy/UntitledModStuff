package org.doothy.untitled.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Utility for performing player-centric raycasts with a configurable reach.
 */
public final class RaycastTargeting {

    private RaycastTargeting() {}

    /**
     * Raycasts from the player's eyes up to the given reach and returns the first hit.
     * Returns {@code null} when there is no collision within reach.
     *
     * @param player player to raycast from
     * @param reach  maximum distance in blocks
     * @return hit result or {@code null} if nothing was hit
     */
    public static HitResult raycast(Player player, double reach) {
        HitResult hit = player.pick(reach, 0.0F, true);

        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }

        return hit;
    }
}
