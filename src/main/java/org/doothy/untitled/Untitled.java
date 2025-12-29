package org.doothy.untitled;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.block.ModBlocks;
import org.doothy.untitled.block.entity.ModBlockEntities;
import org.doothy.untitled.effect.ManaRegenEffect;
import org.doothy.untitled.items.LightningStick;
import org.doothy.untitled.items.ManaPotionItem;
import org.doothy.untitled.items.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import com.mojang.serialization.Codec;
import org.doothy.untitled.network.ManaPayload;
import org.doothy.untitled.screen.ModScreenHandlers;
import org.doothy.untitled.sound.ModSounds;

/**
 * The main entry point for the Untitled Mod.
 * Handles initialization of mod content, networking, and server-side logic.
 */
public class Untitled implements ModInitializer {

    /** The Mod ID used for resource identification. */
    public static final String MOD_ID = "untitled";

    /**
     * Registry holder for the Mana Regeneration effect.
     */
    public static final Holder<MobEffect> MANA_REGEN = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(MOD_ID, "mana_regen"),
            new ManaRegenEffect(MobEffectCategory.BENEFICIAL, 0x00AAFF)
    );

    /**
     * Data component to track if an item was on cooldown.
     */
    public static final DataComponentType<Boolean> WAS_ON_COOLDOWN = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "was_on_cooldown"),
            DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build()
    );

    /**
     * Data component to store mana amount in items.
     * Syncs to client for tooltip display.
     */
    public static final DataComponentType<Integer> STORED_MANA = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "stored_mana"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT) // Saves to disk
                    .networkSynchronized(ByteBufCodecs.VAR_INT) // Syncs to Client for Tooltips
                    .build()
    );

    /**
     * Initializes the mod.
     * Registers items, blocks, sounds, networking, and event handlers.
     */
    @Override
    public void onInitialize() {
        // Initialize Mod Content
        ModSounds.initialize();
        ModItems.initialize();
        LightningStick.initialize();
        ModAttachments.initialize();
        ManaPotionItem.initialize();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModScreenHandlers.initialize();

        // Networking Registration (S2C = Server to Client)
        PayloadTypeRegistry.playS2C().register(ManaPayload.TYPE, ManaPayload.CODEC);

        // SYNC ON JOIN
        // Forces the server to tell the client what its mana is the moment the world loads.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ManaAttachment mana = player.getAttachedOrCreate(ModAttachments.MANA, () -> new ManaAttachment(100, 100));
            ServerPlayNetworking.send(player, new ManaPayload(mana.getMana(), mana.getMaxMana()));
        });

        // MANA REGENERATION SYSTEM
        // This runs on the server side every tick.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ManaAttachment mana = player.getAttached(ModAttachments.MANA);
                if (mana == null || mana.getMana() >= mana.getMaxMana()) continue;

                int amount = player.hasEffect(MANA_REGEN) ? 5 : 1;
                mana.setMana(Math.min(mana.getMaxMana(), mana.getMana() + amount));

                ServerPlayNetworking.send(
                        player,
                        new ManaPayload(mana.getMana(), mana.getMaxMana())
                );
            }
        });
    }
}