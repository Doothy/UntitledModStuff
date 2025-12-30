package org.doothy.untitled.effects.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.effects.ChargeTickEffect;

public class ShieldDuringChargeEffect implements ChargeTickEffect {

    private final int durationTicks;
    private final double radius;

    public ShieldDuringChargeEffect(int durationTicks, double radius) {
        this.durationTicks = durationTicks;
        this.radius = radius;
    }

    @Override
    public void onChargeTick(
            Level level,
            LivingEntity user,
            ItemStack stack,
            int elapsed
    ) {
        if (!(user instanceof Player player)) return;
        if (!(level instanceof ServerLevel server)) return;
        if (elapsed > durationTicks) return;

        AABB area = player.getBoundingBox().inflate(radius);

        for (LivingEntity enemy : server.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e != player && e.isAlive()
        )) {
            Vec3 push = enemy.position()
                    .subtract(player.position())
                    .normalize()
                    .multiply(0.3, 0, 0.3);

            enemy.push(push.x, 0.2, push.z);

            if (elapsed % 10 == 0) {
                enemy.hurt(server.damageSources().lightningBolt(), 1.0f);
            }
        }
    }
}
