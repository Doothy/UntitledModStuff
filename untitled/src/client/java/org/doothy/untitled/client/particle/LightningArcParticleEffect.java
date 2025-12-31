package org.doothy.untitled.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class LightningArcParticleEffect {

    private final Entity from;
    private final Entity to;
    private final int points;

    public LightningArcParticleEffect(Entity from, Entity to, int points) {
        this.from = from;
        this.to = to;
        this.points = points;
    }

    public void spawn(ClientLevel level) {
        Vec3 start = from.position().add(0, from.getBbHeight() * 0.5, 0);
        Vec3 end = to.position().add(0, to.getBbHeight() * 0.5, 0);

        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            Vec3 pos = start.lerp(end, t);

            level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z,
                    0, 0.01, 0
            );
        }
    }
}
