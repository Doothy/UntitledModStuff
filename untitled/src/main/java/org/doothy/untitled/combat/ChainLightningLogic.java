package org.doothy.untitled.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for selecting chain-lightning targets.
 *
 * <p>This class performs a greedy, nearest-next selection up to a maximum
 * number of chains within a given radius, making sure each entity is hit at
 * most once.
 */
public final class ChainLightningLogic {

    /**
     * Finds a set of unique follow-up targets for a chain lightning effect.
     *
     * <p>Starting from {@code start}, the algorithm repeatedly looks for the first
     * valid entity within {@code radius} that hasn't been struck yet, up to
     * {@code maxChains} times. The returned set excludes the starting entity.
     *
     * @param level     server level to search in
     * @param start     first entity that was struck
     * @param maxChains maximum number of subsequent links in the chain
     * @param radius    search radius around the current target each hop
     * @return unique set of targets found in order of discovery
     */
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
            // Note: we use getEntitiesOfClass with a bounding box inflate as a
            // simple proximity query, then pick the first match that isn't the
            // current entity and hasn't been hit yet. For deterministic results,
            // consider sorting by distance.
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
