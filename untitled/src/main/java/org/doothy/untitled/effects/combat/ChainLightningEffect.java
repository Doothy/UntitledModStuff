package org.doothy.untitled.effects.combat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.effects.EffectContext;
import org.doothy.untitled.effects.ItemEffect;
import org.doothy.untitled.network.payload.ChainLightningVisualPayload;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Jumps lightning damage from the initial impact to nearby living entities,
 * up to a maximum number of chained targets based on charge.
 */
public class ChainLightningEffect implements ItemEffect {

    @Override
    /**
     * Applies sustained, proximity-based chain lightning with strict no-duplicate hits.
     * Target selection is nearest-first within a fixed jump range from the current arc end.
     * Sends a visual payload for each hop.
     */
    public void apply(EffectContext ctx) {
        if (!(ctx.level() instanceof ServerLevel level)) return;
        Player caster = ctx.player();
        if (!(caster instanceof ServerPlayer)) return;
        Vec3 startPos = ctx.hitPosition();
        if (startPos == null) return;

        // Legacy-aligned constants
        final int maxJumps = 12;
        final double jumpRange = 12.0;
        final float baseDamage = 10.0f;
        final float decayRate = 0.85f;

        Vec3 currentSource = startPos;
        LivingEntity previousEntityForVisuals = caster; // visuals: first hop links from caster -> first target
        Set<Integer> struckIds = new HashSet<>();

        final double maxDistSqr = jumpRange * jumpRange;

        for (int i = 0; i < maxJumps; i++) {
            final Vec3 finalSource = currentSource;

            // Find nearest valid target around currentSource within jumpRange
            AABB search = new AABB(
                    currentSource.x - jumpRange, currentSource.y - jumpRange, currentSource.z - jumpRange,
                    currentSource.x + jumpRange, currentSource.y + jumpRange, currentSource.z + jumpRange
            );

            LivingEntity target = level.getEntitiesOfClass(
                    LivingEntity.class,
                    search,
                    e -> e.isAlive()
                            && e != caster
                            && !struckIds.contains(e.getId())
                            && e.distanceToSqr(finalSource) <= maxDistSqr
            ).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(finalSource)))
                    .orElse(null);

            if (target == null) break;

            // Compute decayed damage for this hop
            float currentDamage = (float) (baseDamage * Math.pow(decayRate, i));

            // Apply effects
            target.hurt(level.damageSources().lightningBolt(), currentDamage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 4));
            target.setRemainingFireTicks(40);

            // Visuals: send a hop packet (client draws arc from target -> source per handler)
            ChainLightningVisualPayload payload = new ChainLightningVisualPayload(
                    previousEntityForVisuals.getId(),
                    target.getId()
            );
            for (ServerPlayer watcher : level.players()) {
                // Reasonable view distance for the hop effect
                if (watcher.distanceToSqr(previousEntityForVisuals) < 64 * 64) {
                    ServerPlayNetworking.send(watcher, payload);
                }
            }

            // Update state for next hop
            struckIds.add(target.getId());
            previousEntityForVisuals = target;
            currentSource = target.position().add(0, target.getBbHeight() * 0.5, 0);
        }
    }
}
