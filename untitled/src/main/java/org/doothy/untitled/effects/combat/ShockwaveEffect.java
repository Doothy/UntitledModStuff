package org.doothy.untitled.effects.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.effects.EffectContext;
import org.doothy.untitled.effects.ItemEffect;

public class ShockwaveEffect implements ItemEffect {

    @Override
    public void apply(EffectContext ctx) {
        ServerLevel level = ctx.level();
        Vec3 pos = ctx.hitPosition();

        double radius = 4.0 + ctx.charge() * 4.0;
        float damage = 4.0f + ctx.charge() * 6.0f;

        AABB area = new AABB(
                pos.x - radius, pos.y - 1, pos.z - radius,
                pos.x + radius, pos.y + 3, pos.z + radius
        );

        for (LivingEntity e : level.getEntitiesOfClass(
                LivingEntity.class, area, LivingEntity::isAlive)) {

            Vec3 push = e.position().subtract(pos).normalize().scale(0.6 + ctx.charge());
            e.push(push.x, 0.4, push.z);
            e.setRemainingFireTicks(60);
            e.hurt(level.damageSources().lightningBolt(), damage);
        }
    }
}
