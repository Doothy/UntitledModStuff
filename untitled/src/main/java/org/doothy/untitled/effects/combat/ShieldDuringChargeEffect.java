package org.doothy.untitled.effects.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.attachment.ShieldAttachment;
import org.doothy.untitled.effects.ChargeTickEffect;
import org.doothy.untitled.network.payload.ShieldPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ShieldDuringChargeEffect implements ChargeTickEffect {

    private final int durationTicks;
    private final double radius;

    public ShieldDuringChargeEffect(int durationTicks, double radius) {
        this.durationTicks = durationTicks;
        this.radius = radius;
    }

    @Override
    public void onChargeTick(Level level, LivingEntity user, ItemStack stack, int elapsed) {
        if (!(level instanceof ServerLevel)) return;
        if (!(user instanceof ServerPlayer player)) return;

        ShieldAttachment shield =
                player.getAttachedOrCreate(ModAttachments.LIGHTNING_SHIELD);

        if (shield.ticks() > 0) return;
        if (stack.getOrDefault(Untitled.SHIELD_USED_THIS_USE, false)) return;

        stack.set(Untitled.SHIELD_USED_THIS_USE, true);

        shield.setTicks(durationTicks);

        // 🔁 SYNC TO CLIENT (start)
        ServerPlayNetworking.send(
                player,
                new ShieldPayload(durationTicks)
        );
    }

    public void tick(ServerLevel level, ServerPlayer player) {
        ShieldAttachment shield =
                player.getAttachedOrCreate(ModAttachments.LIGHTNING_SHIELD);

        int ticks = shield.ticks();
        if (ticks <= 0) return;

        ticks--;
        shield.setTicks(ticks);

        if (ticks == 0) {
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f
            );

            // 🔁 SYNC TO CLIENT (stop)
            ServerPlayNetworking.send(
                    player,
                    new ShieldPayload(0)
            );
            return;
        }
        AABB area = player.getBoundingBox().inflate(radius);

        for (LivingEntity enemy : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e != player && e.isAlive()
        )) {
            Vec3 push = enemy.position()
                    .subtract(player.position())
                    .normalize()
                    .multiply(0.3, 0, 0.3);

            enemy.push(push.x, 0.2, push.z);

            if (ticks % 10 == 0) {
                enemy.hurt(level.damageSources().lightningBolt(), 1.0f);
            }
        }
    }
}
