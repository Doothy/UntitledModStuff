package org.doothy.untitled.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.doothy.untitled.network.payload.ChainLightningVisualPayload;
import org.doothy.untitled.network.payload.LightningVisualPayload;
import org.doothy.untitled.network.payload.ManaPayload;
import org.doothy.untitled.network.payload.ShieldPayload;

public final class NetworkInit {

    private NetworkInit() {}

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
                ShieldPayload.STREAM_CODEC
        );
    }
}
