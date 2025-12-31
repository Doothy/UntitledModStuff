package org.doothy.untitled.client.visual;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.doothy.untitled.network.payload.ChainLightningVisualPayload;

public final class ChainLightningVisualHandler {

    private ChainLightningVisualHandler() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                ChainLightningVisualPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        ClientLevel level = context.client().level;
                        if (level == null) return;

                        Entity from = level.getEntity(payload.fromId());
                        Entity to = level.getEntity(payload.toId());
                        if (from == null || to == null) return;

                        // Per request: use the target entity position as the arc's start
                        // and link it back to the source. Use a high-arched Bezier with jitter.
                        var start = to.position().add(0, to.getBbHeight() * 0.5, 0);
                        var end = from.position().add(0, from.getBbHeight() * 0.5, 0);
                        LightningArc.spawnBezierArc(level, start, end, 0.15, 10.0, 250);
                    });
                }
        );
    }
}
