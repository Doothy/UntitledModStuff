package org.doothy.untitled.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

public final class ChainLightningLogic {

    public static Set<LivingEntity> findTargets(
            ServerLevel level,
            LivingEntity start,
            int maxChains,
            double radius
    ) {
        Set<LivingEntity> hit = new HashSet<>();
        LivingEntity current = start;

        for (int i = 0; i < maxChains; i++) {
            LivingEntity finalCurrent = current;
            LivingEntity next = level.getEntitiesOfClass(
                    LivingEntity.class,
                    current.getBoundingBox().inflate(radius),
                    e -> e != finalCurrent && !hit.contains(e)
            ).stream().findFirst().orElse(null);

            if (next == null) break;

            hit.add(next);
            current = next;
        }

        return hit;
    }

    private ChainLightningLogic() {}
}
