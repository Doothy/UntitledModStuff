package org.doothy.untitled.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.network.payload.ManaPayload;

/**
 * Helper class for syncing mana data to the client.
 * This class is SERVER-ONLY and API-compliant.
 */
public final class ManaSyncHandler {

    private ManaSyncHandler() {}

    /**
     * Syncs the player's current mana to their client.
     *
     * @param player The player to sync.
     */
    public static void sync(ServerPlayer player) {
        ManaStorage mana = player.getAttachedOrCreate(ModAttachments.MANA);

        if (mana == null) return;

        ServerPlayNetworking.send(
                player,
                new ManaPayload(
                        (int) mana.getMana(),
                        (int) mana.getMaxMana()
                )
        );
    }
}
