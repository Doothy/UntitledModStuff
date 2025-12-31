package org.doothy.untitled.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.doothy.untitled.Untitled;

public record ChainLightningVisualPayload(
        int fromId,
        int toId
) implements CustomPacketPayload {

    public static final Type<ChainLightningVisualPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(
                            Untitled.MOD_ID, "chain_lightning"
                    )
            );

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
