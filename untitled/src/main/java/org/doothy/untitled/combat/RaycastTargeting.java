package org.doothy.untitled.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RaycastTargeting {

    private RaycastTargeting() {}

    public static Vec3 raycastPosition(Player player, double reach) {
        HitResult hit = player.pick(reach, 0.0F, false);
        return hit.getLocation();
    }
}
