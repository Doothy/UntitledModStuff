package org.doothy.untitled.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.doothy.untitled.Untitled;

public record ShieldPayload(int ticks) implements CustomPacketPayload {

    public static final Type<ShieldPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "shield"));

    public static final StreamCodec<FriendlyByteBuf, ShieldPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeVarInt(payload.ticks),
                    buf -> new ShieldPayload(buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
