package org.doothy.untitled.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Helper class for complex ability logic such as knockback and chain lightning.
 */
public class AbilityHelper {

    /**
     * Projects entities away from the impact point based on their distance.
     * <p>
     * The knockback strength decays linearly from the center.
     *
     * @param level  The server level.
     * @param source The player who initiated the effect.
     * @param pos    The center of the impact.
     */
    public static void applyThunderClap(ServerLevel level, Player source, Vec3 pos) {
        double radius = 6.0;
        AABB area = new AABB(pos.x - radius, pos.y - 2, pos.z - radius, pos.x + radius, pos.y + 4, pos.z + radius);

        level.getEntitiesOfClass(LivingEntity.class, area, e -> e != source && e.isAlive()).forEach(target -> {
            Vec3 dir = target.position().subtract(pos).normalize();
            double dist = target.position().distanceTo(pos);
            // Linear strength decay: 1.2 at center, 0 at the edge of 6 blocks.
            double strength = 1.2 * (1.0 - (dist / radius));

            target.push(dir.x * strength, 0.4, dir.z * strength);
            target.hurtMarked = true;
        });
    }

    /**
     * Iteratively finds the closest target that hasn't been struck yet.
     * Capable of bouncing up to 'maxJumps' times.
     *
     * @param level    The server level.
     * @param source   The player who initiated the chain.
     * @param startPos The starting position of the chain.
     */
    public static void performSustainedChain(ServerLevel level, Player source, Vec3 startPos) {
        Vec3 currentSource = startPos;
        List<Entity> struckEntities = new ArrayList<>();

        // CONFIGURATION
        int maxJumps = 12; // Will bounce to up to 12 unique targets
        double jumpRange = 12.0;
        float baseDamage = 10.0f;
        float decayRate = 0.85f;

        // Add the source to struckEntities so the lightning doesn't bounce back to the player
        struckEntities.add(source);

        for (int i = 0; i < maxJumps; i++) {
            Vec3 finalSource = currentSource;
            float currentDamage = baseDamage * (float) Math.pow(decayRate, i);

            // Define the search box around the current source
            AABB searchBox = new AABB(
                    finalSource.x - jumpRange, finalSource.y - jumpRange, finalSource.z - jumpRange,
                    finalSource.x + jumpRange, finalSource.y + jumpRange, finalSource.z + jumpRange
            );

            // Find closest living entity within range that hasn't been hit yet
            LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, searchBox, e ->
                            e.isAlive() &&
                                    e.distanceToSqr(finalSource) <= (jumpRange * jumpRange) &&
                                    !struckEntities.contains(e) // Ensure uniqueness
                    )
                    .stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(finalSource))) // Find closest
                    .orElse(null);

            // If no valid target is found, the chain ends immediately
            if (target == null) break;

            // --- APPLICATION ---

            // Set fire before damage (ensures cooked drops if entity dies)
            target.setRemainingFireTicks(60);
            target.hurt(level.damageSources().lightningBolt(), currentDamage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 4));

            // Mark entity as struck so it can't be targeted again
            struckEntities.add(target);

            // --- VISUALS ---
            // Calculate the visual center of the bounding box (Chest height)
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);

            ParticleHelper.spawnChainLightningLink(level, finalSource, targetCenter);

            // Pitch increases slightly per jump for audio feedback
            level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.8f, 1.2f + (i * 0.1f));

            // Move the source point to the CENTER of the new target for the next iteration
            currentSource = targetCenter;
        }
    }
}
