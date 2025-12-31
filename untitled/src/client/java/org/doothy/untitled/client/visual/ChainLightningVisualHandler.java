package org.doothy.untitled.client.visual;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.doothy.untitled.client.particle.LightningArcParticleEffect;
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

                        new LightningArcParticleEffect(from, to, 12)
                                .spawn(level);
                    });
                }
        );
    }
}
