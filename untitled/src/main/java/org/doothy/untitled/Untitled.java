package org.doothy.untitled;

import com.google.common.graph.Network;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.block.ModBlocks;
import org.doothy.untitled.block.entity.ModBlockEntities;
import org.doothy.untitled.effect.ManaRegenEffect;
import org.doothy.untitled.effects.combat.ShieldDuringChargeEffect;
import org.doothy.untitled.items.ManaPotionItem;
import org.doothy.untitled.items.ModItems;
import org.doothy.untitled.network.NetworkInit;
import org.doothy.untitled.network.payload.ManaPayload;
import org.doothy.untitled.network.ManaSyncHandler;
import org.doothy.untitled.network.payload.LightningVisualPayload;
import org.doothy.untitled.screen.ModScreenHandlers;
import org.doothy.untitled.sound.ModSounds;

/**
 * The main entry point for the mod.
 * Handles initialization of content, networking, and event listeners.
 */
public class Untitled implements ModInitializer {

    public static final String MOD_ID = "untitled";

    // ───────────────────────── EFFECTS ─────────────────────────

    public static final Holder<MobEffect> MANA_REGEN = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(MOD_ID, "mana_regen"),
            new ManaRegenEffect(MobEffectCategory.BENEFICIAL, 0x00AAFF)
    );
    private static final ShieldDuringChargeEffect SHIELD_EFFECT =
            new ShieldDuringChargeEffect(100, 1.5); // 5s, radius 1.5

    // ───────────────────────── DATA COMPONENTS ─────────────────────────

    public static final DataComponentType<Boolean> WAS_ON_COOLDOWN = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "was_on_cooldown"),
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .build()
    );

    public static final DataComponentType<Boolean> SHIELD_USED_THIS_USE =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(MOD_ID, "shield_used_this_use"),
                    DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .build()
            );

    public static final DataComponentType<Integer> STORED_MANA = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "stored_mana"),
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()
    );

    // ───────────────────────── INITIALIZATION ─────────────────────────

    @Override
    public void onInitialize() {

        // Content
        ModSounds.initialize();
        ModItems.initialize();
        ModAttachments.initialize();
        ManaPotionItem.initialize();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModScreenHandlers.initialize();
        NetworkInit.init();
        // ───────────────────────── SYNC (SAFE TIMING) ─────────────────────────

        // Delay sync by 1 server task so attachments exist
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                ServerPlayer player = handler.player;
                ManaStorage mana = player.getAttachedOrCreate(ModAttachments.MANA);
                ManaSyncHandler.sync(player);
            });
        });

        // Sync on respawn / dimension copy
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            ManaSyncHandler.sync(newPlayer);
        });

        // ───────────────────────── MANA REGEN + SYNC ─────────────────────────

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {

                ManaStorage mana = player.getAttachedOrCreate(ModAttachments.MANA);
                if (mana == null || mana.isFull()) continue;

                long amount = player.hasEffect(MANA_REGEN) ? 5 : 1;
                mana.insertMana(amount, ManaTransaction.EXECUTE);

                ServerPlayNetworking.send(
                        player,
                        new ManaPayload(
                                (int) mana.getMana(),
                                (int) mana.getMaxMana()
                        )
                );
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                for (Player player : level.players()) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        SHIELD_EFFECT.tick(level, serverPlayer);
                    }
                }
            }
        });

    }
}
