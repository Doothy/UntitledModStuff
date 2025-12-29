package org.doothy.untitled.client;

import net.fabricmc.api.ClientModInitializer;
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
import org.doothy.untitled.items.LightningSoundHelper;
import org.doothy.untitled.items.ManaBatteryItem;
import org.doothy.untitled.network.ManaPayload;
import org.doothy.untitled.screen.ModScreenHandlers;

public class UntitledClient implements ClientModInitializer {

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
            if (!ClientManaCache.isValid()) return;

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
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientManaCache.reset());
    }
}
