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
import net.minecraft.world.entity.player.Player;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.attachment.ShieldAttachment;
import org.doothy.untitled.client.screen.ManaFurnaceScreen;
//import org.doothy.untitled.client.visual.LightningShieldVisual;
//import org.doothy.untitled.client.visual.LightningVisualHandler;
import org.doothy.untitled.client.visual.*;
import org.doothy.untitled.items.LightningSoundHelper;
import org.doothy.untitled.items.LightningStick;
import org.doothy.untitled.items.ManaBatteryItem;
import org.doothy.untitled.network.payload.ManaPayload;
import org.doothy.untitled.network.payload.ShieldPayload;
import org.doothy.untitled.screen.ModScreenHandlers;

/**
 * The client-side entry point for the mod.
 * Handles client initialization, rendering, and input.
 */
public class UntitledClient implements ClientModInitializer {

    private static boolean wasCharging = false;

    private static int chargeTicks = 0;
    @Override
    public void onInitializeClient() {

        LightningSoundHelper.Holder.INSTANCE = new ClientLightningSoundHelper();

        // ───────────────────────── NETWORKING ─────────────────────────

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

        // ───────────────────────── TOOLTIP ─────────────────────────

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

        // ───────────────────────── HUD ─────────────────────────

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.options.hideGui) return;
            if (!ClientManaCache.isValid()) {
                return;
            }

            int mana = ClientManaCache.getMana();
            int maxMana = ClientManaCache.getCapacity();
            float ratio = ClientManaCache.getFillRatio();

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

        // ───────────────────────── SCREENS ─────────────────────────

        MenuScreens.register(
                ModScreenHandlers.MANA_FURNACE_MENU,
                ManaFurnaceScreen::new
        );

        // ───────────────────────── RESET CACHE ─────────────────────────

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientManaCache.reset());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            Player player = client.player;

            boolean charging =
                    player.isUsingItem() &&
                            player.getUseItem().getItem() instanceof LightningStick;

            // ─── charge started ───
            if (charging && !wasCharging) {
                chargeTicks = 0;
                LightningSoundHelper.Holder.INSTANCE.start();
            }

            // ─── charge ongoing ───
            if (charging) {
                chargeTicks++;
            }

            // ─── charge cancelled before completion ───
            if (!charging && wasCharging) {
                LightningSoundHelper.Holder.INSTANCE.stop();
                chargeTicks = 0;
            }

            wasCharging = charging;
        });

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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientManaCache.reset();
            ClientShieldCache.reset();
        });

    }
}
