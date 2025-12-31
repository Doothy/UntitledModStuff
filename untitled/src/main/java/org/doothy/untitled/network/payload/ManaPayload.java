package org.doothy.untitled.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.doothy.untitled.Untitled;
import org.jspecify.annotations.NonNull;

/**
 * Server-to-client payload carrying the player's current mana and capacity for HUD updates.
 *
 * @param mana     current mana amount
 * @param capacity maximum mana capacity
 */
public record ManaPayload(int mana, int capacity) implements CustomPacketPayload {

    public static final Type<ManaPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "mana"));

    /** Stream codec using VAR_INT for compact HUD-scale values. */
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
