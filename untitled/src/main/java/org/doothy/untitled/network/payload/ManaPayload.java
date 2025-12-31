package org.doothy.untitled.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.doothy.untitled.Untitled;
import org.jspecify.annotations.NonNull;

/**
 * Server → Client payload for syncing mana state.
 * <p>
 * This payload is PURE DATA.
 * It does not reference attachments, storage implementations, or gameplay logic.
 *
 * @param mana     Current mana amount
 * @param capacity Maximum mana capacity
 */
public record ManaPayload(int mana, int capacity) implements CustomPacketPayload {

    /** Packet identifier */
    public static final Type<ManaPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "mana"));

    /**
     * Stream codec (MC 1.21.11+).
     * <p>
     * Uses VAR_INT because:
     * - HUD-scale values
     * - Compact over the wire
     * - Internal storage may still use long
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ManaPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ManaPayload::mana,
                    ByteBufCodecs.VAR_INT, ManaPayload::capacity,
                    ManaPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
