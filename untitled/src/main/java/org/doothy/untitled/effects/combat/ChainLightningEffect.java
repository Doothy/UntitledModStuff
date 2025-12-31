package org.doothy.untitled.effects.combat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.effects.EffectContext;
import org.doothy.untitled.effects.ItemEffect;
import org.doothy.untitled.network.payload.ChainLightningVisualPayload;

import java.util.HashSet;
import java.util.Set;

public class ChainLightningEffect implements ItemEffect {

    @Override
    public void apply(EffectContext ctx) {
        ServerLevel level = ctx.level();
        Player caster = ctx.player();
        Vec3 origin = ctx.hitPosition();
        float charge = ctx.charge();

        if (!(caster instanceof ServerPlayer)) return;

        double radius = 6.0 + charge * 4.0;
        float damage = 4.0f + charge * 4.0f;
        int maxChains = 3 + (int)(charge * 3);

        // ── Find initial target near hit position ──
        AABB initialArea = new AABB(
                origin.x - radius, origin.y - radius, origin.z - radius,
                origin.x + radius, origin.y + radius, origin.z + radius
        );

        LivingEntity current = level.getEntitiesOfClass(
                LivingEntity.class,
                initialArea,
                e -> e.isAlive() && e != caster
        ).stream().findFirst().orElse(null);

        if (current == null) return;

        Set<LivingEntity> hit = new HashSet<>();
        hit.add(current);

        // ── Chain ──
        for (int i = 0; i < maxChains; i++) {

            AABB chainArea = current.getBoundingBox().inflate(radius);

            LivingEntity next = level.getEntitiesOfClass(
                    LivingEntity.class,
                    chainArea,
                    e -> e.isAlive() && e != caster && !hit.contains(e)
            ).stream().findFirst().orElse(null);

            if (next == null) break;

            // Damage
            next.hurt(level.damageSources().lightningBolt(), damage);

            // 🔹 SEND VISUAL PACKET (per hop)
            ChainLightningVisualPayload payload =
                    new ChainLightningVisualPayload(current.getId(), next.getId());

            for (ServerPlayer watcher : level.players()) {
                if (watcher.distanceToSqr(current) < 64 * 64) {
                    ServerPlayNetworking.send(watcher, payload);
                }
            }

            hit.add(next);
            current = next;
        }
    }
}
