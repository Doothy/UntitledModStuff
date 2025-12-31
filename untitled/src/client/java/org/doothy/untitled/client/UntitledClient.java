package org.doothy.untitled.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.client.screen.ManaFurnaceScreen;
import org.doothy.untitled.client.sound.LightningChargeAudioHandler;
import org.doothy.untitled.client.visual.*;
import org.doothy.untitled.items.ManaBatteryItem;
import org.doothy.untitled.network.payload.ManaPayload;
import org.doothy.untitled.network.payload.ShieldPayload;
import org.doothy.untitled.screen.ModScreenHandlers;
import net.minecraft.util.Mth;

/**
 * Client-side entry point that wires client networking listeners, HUD rendering,
 * screens, and simple client-side caches.
 */
public class UntitledClient implements ClientModInitializer {

    // Charge sound/preview are handled by dedicated visual handlers
    @Override
    /**
     * Initializes client listeners and UI components:
     * - Network receivers update client caches and trigger visuals
     * - Tooltip and HUD callbacks render mana information
     * - Screen registration and per-tick tasks for previews and sounds
     */
    public void onInitializeClient() {
        // Register client-side visual/audio handlers

        ClientPlayNetworking.registerGlobalReceiver(
                ManaPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    ClientManaCache.set(payload.mana(), payload.capacity());
                })
        );
        LightningVisualHandler.register();
        LightningShieldVisual.register();
        ChainLightningVisualHandler.register();
        LightningChargePreviewHandler.register();
        LightningChargeAudioHandler.register();

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.getItem() instanceof ManaBatteryItem battery) {
                int stored = stack.getOrDefault(Untitled.STORED_MANA, 0);
                int max = battery.getMaxCapacity();

                lines.add(
                        Component.literal("Stored Mana: ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(
                                        Component.literal(stored + " / " + max)
                                                .withStyle(ChatFormatting.AQUA)
                                )
                );
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.options.hideGui) return;
            if (!ClientManaCache.isValid()) {
                return;
            }

            int mana = ClientManaCache.getMana();
            int maxMana = ClientManaCache.getCapacity();
            float ratio = Mth.clamp(ClientManaCache.getFillRatio(), 0f, 1f);

            int x = 10;
            int y = 10;
            int width = 100;
            int height = 8;

            int progress = (int) (width * ratio);

            guiGraphics.fill(x, y, x + width, y + height, 0xFF121212);
            guiGraphics.fill(x, y, x + progress, y + height, 0xFF00AAFF);
            guiGraphics.fill(x, y, x + progress, y + 2, 0x44FFFFFF);
            guiGraphics.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xFFAAAAAA);

            guiGraphics.drawString(
                    client.font,
                    mana + " / " + maxMana,
                    x + width + 5,
                    y,
                    0xFFFFFFFF,
                    true
            );
        });

        MenuScreens.register(
                ModScreenHandlers.MANA_FURNACE_MENU,
                ManaFurnaceScreen::new
        );

        // Single disconnect handler: reset all client caches/visuals
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientManaCache.reset();
            ClientShieldCache.reset();
            LightningTargetPreview.clear();
        });

        // Charge audio is handled in LightningChargeAudioHandler

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                LightningTargetPreview.tick(client.level);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(
                ShieldPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (payload.ticks() > 0) {
                        ClientShieldCache.start();
                    } else {
                        ClientShieldCache.stop();
                    }
                })
        );

    }
}
