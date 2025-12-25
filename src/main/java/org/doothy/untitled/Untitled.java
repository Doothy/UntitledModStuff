package org.doothy.untitled;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.items.LightningStick;
import org.doothy.untitled.items.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import com.mojang.serialization.Codec;
import org.doothy.untitled.network.ManaPayload;

public class Untitled implements ModInitializer {

    public static final String MOD_ID = "untitled";

    // 1. Data Component Registration
    public static final DataComponentType<Boolean> WAS_ON_COOLDOWN = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "was_on_cooldown"),
            DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build()
    );

    @Override
    public void onInitialize() {
        // 2. Initialize Mod Content
        ModItems.initialize();
        LightningStick.initialize();
        ModAttachments.initialize();

        // 3. Networking Registration (S2C = Server to Client)
        PayloadTypeRegistry.playS2C().register(ManaPayload.TYPE, ManaPayload.CODEC);

        // 4. SYNC ON JOIN
        // Forces the server to tell the client what its mana is the moment the world loads.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ManaAttachment mana = player.getAttachedOrCreate(ModAttachments.MANA, () -> new ManaAttachment(100, 100));
            ServerPlayNetworking.send(player, new ManaPayload(mana.getMana(), mana.getMaxMana()));
        });

        // 5. MANA REGENERATION SYSTEM
        // This runs on the server side every tick.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Regeneration happens once every 20 ticks (1 second)
            if (server.getTickCount() % 20 == 0) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    ManaAttachment mana = player.getAttached(ModAttachments.MANA);

                    // Only regen if player has mana data and isn't already full
                    if (mana != null && mana.getMana() < mana.getMaxMana()) {
                        int newMana = Math.min(mana.getMaxMana(), mana.getMana() + 1);
                        mana.setMana(newMana);

                        // Critical: Flag the attachment as changed on the server
                        player.setAttached(ModAttachments.MANA, mana);

                        // Sync the new value to the client HUD immediately
                        ServerPlayNetworking.send(player, new ManaPayload(mana.getMana(), mana.getMaxMana()));
                    }
                }
            }
        });
    }
}