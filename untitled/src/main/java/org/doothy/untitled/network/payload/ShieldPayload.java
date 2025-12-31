package org.doothy.untitled.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.doothy.untitled.Untitled;

/**
 * Server-to-client payload toggling shield visuals with the remaining duration in ticks.
 * A value of 0 indicates the shield visuals should stop.
 *
 * @param ticks remaining shield duration in ticks; 0 to stop
 */
public record ShieldPayload(int ticks) implements CustomPacketPayload {

    public static final Type<ShieldPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "shield"));

    /** Stream codec encoding the remaining ticks as a VAR_INT. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeVarInt(payload.ticks),
                    buf -> new ShieldPayload(buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
