package org.doothy.untitled.effects.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.effects.EffectContext;
import org.doothy.untitled.effects.ItemEffect;

import java.util.HashSet;
import java.util.Set;

public class ChainLightningEffect implements ItemEffect {

    @Override
    public void apply(EffectContext ctx) {
        ServerLevel level = ctx.level();
        Vec3 origin = ctx.hitPosition();

        int jumps = 2 + (int)(ctx.charge() * 4);
        double range = 6.0;

        Vec3 current = origin;
        Set<LivingEntity> hit = new HashSet<>();

        for (int i = 0; i < jumps; i++) {
            LivingEntity next = level.getNearestEntity(
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            AABB.ofSize(current, range, range, range),
                            e -> e.isAlive() && !hit.contains(e)
                    ),
                    null,
                    null,
                    current.x, current.y, current.z
            );

            if (next == null) break;

            hit.add(next);
            next.hurt(level.damageSources().lightningBolt(), 5.0f);
            current = next.position();
        }
    }
}
