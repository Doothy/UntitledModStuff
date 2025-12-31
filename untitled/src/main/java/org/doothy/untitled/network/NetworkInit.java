package org.doothy.untitled.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.doothy.untitled.network.payload.ChainLightningVisualPayload;
import org.doothy.untitled.network.payload.LightningVisualPayload;
import org.doothy.untitled.network.payload.ManaPayload;
import org.doothy.untitled.network.payload.ShieldPayload;

/**
 * Registers play channel payload types used by the mod for server-to-client packets.
 */
public final class NetworkInit {

    private NetworkInit() {}

    /**
     * Registers all S2C payload codecs with Fabric's play registry.
     */
    public static void init() {
        PayloadTypeRegistry.playS2C().register(
                ManaPayload.TYPE,
                ManaPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                ChainLightningVisualPayload.TYPE,
                ChainLightningVisualPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                LightningVisualPayload.TYPE,
                LightningVisualPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                ShieldPayload.TYPE,
                ShieldPayload.CODEC
        );
    }
}
