package org.doothy.untitled.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.doothy.untitled.Untitled;
import org.jspecify.annotations.NonNull;


public record ManaPayload(int current, int max) implements CustomPacketPayload {

    public static final Type<ManaPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "mana_sync"));

    // Modern 1.21.11 StreamCodec implementation
    public static final StreamCodec<RegistryFriendlyByteBuf, ManaPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ManaPayload::current,
            ByteBufCodecs.VAR_INT, ManaPayload::max,
            ManaPayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


