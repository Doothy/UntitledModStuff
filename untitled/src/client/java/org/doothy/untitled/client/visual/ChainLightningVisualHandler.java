package org.doothy.untitled.client.visual;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.doothy.untitled.network.payload.ChainLightningVisualPayload;

/**
 * Handles the client-side visual effects for chain lightning.
 * <p>
 * This class registers the network receiver for {@link ChainLightningVisualPayload}
 * and spawns the lightning arc particles when the packet is received.
 */
public final class ChainLightningVisualHandler {

    private ChainLightningVisualHandler() {}

    /**
     * Registers the global receiver for chain lightning visual payloads.
     */
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

                        // prevent the arc from connecting to the player (caster).
                        // The chain should appear to start from the first target.
                        if (from instanceof Player) return;

                        // use the target entity position as the arc's start
                        // and link it back to the source. Use a high-arched Bezier with jitter.
                        var start = to.position().add(0, to.getBbHeight() * 0.5, 0);
                        var end = from.position().add(0, from.getBbHeight() * 0.5, 0);
                        LightningArc.spawnBezierArc(level, start, end, 0.15, 10.0, 250);
                    });
                }
        );
    }
}
