package org.doothy.untitled.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.Untitled;

/**
 * Server-to-client payload instructing the client to render a lightning impact visual
 * at the given world position with a normalized charge factor.
 *
 * @param pos    world position of the visual
 * @param charge normalized charge in [0..1]
 */
public record LightningVisualPayload(Vec3 pos, float charge)
        implements CustomPacketPayload {

    /** Packet type identifier for Fabric networking. */
    public static final CustomPacketPayload.Type<LightningVisualPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(
                            Untitled.MOD_ID, "lightning_visual"
                    )
            );

    /** Stream codec encoding the hit position (x,y,z) followed by the charge float. */
    public static final StreamCodec<RegistryFriendlyByteBuf, LightningVisualPayload> CODEC =
            StreamCodec.of(
                    LightningVisualPayload::write,
                    LightningVisualPayload::read
            );

    private static void write(
            RegistryFriendlyByteBuf buf,
            LightningVisualPayload payload
    ) {
        buf.writeDouble(payload.pos.x);
        buf.writeDouble(payload.pos.y);
        buf.writeDouble(payload.pos.z);
        buf.writeFloat(payload.charge);
    }

    private static LightningVisualPayload read(
            RegistryFriendlyByteBuf buf
    ) {
        return new LightningVisualPayload(
                new Vec3(
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble()
                ),
                buf.readFloat()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
