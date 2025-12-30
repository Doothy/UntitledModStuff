package org.doothy.untitled.effects.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.effects.ClientOnlyEffect;
import org.doothy.untitled.effects.EffectContext;

@Environment(EnvType.CLIENT)
public class LightningArcParticleEffect implements ClientOnlyEffect {

    private final Entity from;
    private final Entity to;
    private final int points;

    public LightningArcParticleEffect(Entity from, Entity to, int points) {
        this.from = from;
        this.to = to;
        this.points = points;
    }

    @Override
    public void applyClient(EffectContext context) {
        Vec3 start = from.position().add(0, from.getBbHeight() * 0.5, 0);
        Vec3 end = to.position().add(0, to.getBbHeight() * 0.5, 0);

        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            Vec3 pos = start.lerp(end, t);

            context.level().addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z,
                    0, 0, 0
            );
        }
    }
}
