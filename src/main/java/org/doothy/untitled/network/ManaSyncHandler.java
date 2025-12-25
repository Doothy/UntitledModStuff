package org.doothy.untitled.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.doothy.untitled.attachment.ModAttachments;

public class ManaSyncHandler {
    public static void sync(ServerPlayer player) {
        var mana = player.getAttached(ModAttachments.MANA);
        if (mana != null) {
            // ServerPlayNetworking automatically handles the "FabricCustomPayload" wrapping
            ServerPlayNetworking.send(player, new ManaPayload(mana.getMana(), mana.getMaxMana()));
        }
    }
}
