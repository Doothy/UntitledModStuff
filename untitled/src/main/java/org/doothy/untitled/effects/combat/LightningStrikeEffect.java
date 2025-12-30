package org.doothy.untitled.effects.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.effects.EffectContext;
import org.doothy.untitled.effects.ItemEffect;

public class LightningStrikeEffect implements ItemEffect {

    private static final float DAMAGE = 8.0f;
    private static final double RADIUS = 3.0;

    @Override
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
            entity.hurt(level.damageSources().lightningBolt(), DAMAGE);
        }
    }
}
