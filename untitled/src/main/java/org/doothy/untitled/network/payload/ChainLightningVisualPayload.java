package org.doothy.untitled.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.doothy.untitled.Untitled;

/**
 * Server-to-client payload instructing the client to render a lightning arc
 * between two entities identified by their IDs.
 *
 * @param fromId source entity ID
 * @param toId   target entity ID
 */
public record ChainLightningVisualPayload(
        int fromId,
        int toId
) implements CustomPacketPayload {

    /** Packet type identifier for Fabric networking. */
    public static final Type<ChainLightningVisualPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(
                            Untitled.MOD_ID, "chain_lightning"
                    )
            );

    /** Stream codec encoding the two entity IDs as VAR_INT values. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainLightningVisualPayload> CODEC =
            StreamCodec.of(
                    ChainLightningVisualPayload::write,
                    ChainLightningVisualPayload::read
            );

    private static void write(
            RegistryFriendlyByteBuf buf,
            ChainLightningVisualPayload payload
    ) {
        buf.writeVarInt(payload.fromId);
        buf.writeVarInt(payload.toId);
    }

    private static ChainLightningVisualPayload read(
            RegistryFriendlyByteBuf buf
    ) {
        return new ChainLightningVisualPayload(
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
