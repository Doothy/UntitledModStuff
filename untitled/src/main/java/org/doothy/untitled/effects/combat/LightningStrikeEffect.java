package org.doothy.untitled.effects.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.effects.EffectContext;
import org.doothy.untitled.effects.ItemEffect;

/**
 * Deals a burst of lightning damage and applies fire to entities near the impact point.
 */
public class LightningStrikeEffect implements ItemEffect {

    private static final float DAMAGE = 8.0f;
    private static final double RADIUS = 3.0;

    @Override
    /**
     * Applies area damage around the hit position using the lightning damage source
     * and ignites affected entities for a short duration.
     *
     * @param ctx effect context containing level and impact position
     */
    public void apply(EffectContext ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;

        Vec3 pos = ctx.hitPosition();
        if (pos == null) return;

        AABB area = new AABB(
                pos.x - RADIUS, pos.y - RADIUS, pos.z - RADIUS,
                pos.x + RADIUS, pos.y + RADIUS, pos.z + RADIUS
        );

        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                LivingEntity::isAlive
        )) {
            entity.setRemainingFireTicks(80);
            entity.hurt(level.damageSources().lightningBolt(), DAMAGE);
        }
    }
}
