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

/**
 * Applies a short-lived protective aura when the user charges an ability.
 * While active, the shield pushes nearby entities away and deals minor periodic damage.
 */
public class ShieldDuringChargeEffect implements ChargeTickEffect {

    private final int durationTicks;
    private final double radius;

    /**
     * Creates a new shield effect that persists for a fixed duration and radius.
     *
     * @param durationTicks total active ticks for the shield after being applied
     * @param radius        horizontal radius used to affect nearby entities
     */
    public ShieldDuringChargeEffect(int durationTicks, double radius) {
        this.durationTicks = durationTicks;
        this.radius = radius;
    }

    /**
     * On each charge tick, attempts to activate the shield once per use. The shield state
     * is stored on a player attachment and mirrored to the client via a payload.
     *
     * @param level    current level (server-side required)
     * @param user     living entity charging the item
     * @param stack    item stack being used
     * @param elapsed  ticks elapsed since use started
     */
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
        // Notify client about shield activation with its remaining duration
        ServerPlayNetworking.send(
                player,
                new ShieldPayload(durationTicks)
        );
    }

    /**
     * Per-tick maintenance for an active shield: decrements duration, plays a sound when
     * it ends, syncs termination to client, and applies knockback/damage to nearby entities.
     *
     * @param level  server level containing the player
     * @param player player that currently has a shield attachment
     */
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
            // Notify client that the shield has ended
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
            // Compute a horizontal push vector away from the player and apply gentle lift
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
