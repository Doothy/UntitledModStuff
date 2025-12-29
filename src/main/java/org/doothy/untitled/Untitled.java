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
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import com.mojang.serialization.Codec;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.block.ModBlocks;
import org.doothy.untitled.block.entity.ModBlockEntities;
import org.doothy.untitled.effect.ManaRegenEffect;
import org.doothy.untitled.items.LightningStick;
import org.doothy.untitled.items.ManaPotionItem;
import org.doothy.untitled.items.ModItems;
import org.doothy.untitled.network.ManaPayload;
import org.doothy.untitled.screen.ModScreenHandlers;
import org.doothy.untitled.sound.ModSounds;

/**
 * The main entry point for the Untitled Mod.
 */
public class Untitled implements ModInitializer {

    public static final String MOD_ID = "untitled";

    // ───────────────────────── EFFECTS ─────────────────────────

    public static final Holder<MobEffect> MANA_REGEN = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath(MOD_ID, "mana_regen"),
            new ManaRegenEffect(MobEffectCategory.BENEFICIAL, 0x00AAFF)
    );

    // ───────────────────────── DATA COMPONENTS ─────────────────────────

    public static final DataComponentType<Boolean> WAS_ON_COOLDOWN = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "was_on_cooldown"),
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
        LightningStick.initialize();
        ModAttachments.initialize();
        ManaPotionItem.initialize();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModScreenHandlers.initialize();

        // Networking
        PayloadTypeRegistry.playS2C().register(ManaPayload.TYPE, ManaPayload.CODEC);

        // ───────────────────────── SYNC ON JOIN ─────────────────────────
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {

                ManaAttachment mana = player.getAttached(ModAttachments.MANA);
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

        // ───────────────────────── MANA REGEN SYSTEM ─────────────────────────
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {

                ManaStorage mana = player.getAttached(ModAttachments.MANA);
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
    }
}
