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
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.client.screen.ManaFurnaceScreen;
import org.doothy.untitled.items.LightningSoundHelper;
import org.doothy.untitled.items.ManaBatteryItem;
import org.doothy.untitled.network.ManaPayload;
import org.doothy.untitled.screen.ModScreenHandlers;
import org.joml.Vector3f;

/**
 * The main client-side entry point for the Untitled Mod.
 * Handles client initialization, networking receivers, HUD rendering, and screen registration.
 */
public class UntitledClient implements ClientModInitializer {

    /**
     * Initializes the client-side mod content.
     */
    @Override
    public void onInitializeClient() {
        System.out.println("DEBUG: UntitledClient Initialized!");

        // Initialize client-side sound helper
        LightningSoundHelper.Holder.INSTANCE = new ClientLightningSoundHelper();

        // Register Mana Sync Receiver
        ClientPlayNetworking.registerGlobalReceiver(ManaPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientManaCache.set(payload.current(), payload.max());
            });
        });

        // Register Tooltip Callback for Mana Batteries
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            // Check if the item is one of our batteries
            if (stack.getItem() instanceof ManaBatteryItem battery) {
                // Read the synced data
                int stored = stack.getOrDefault(Untitled.STORED_MANA, 0);
                int max = battery.getMaxCapacity(); // You'll need a getter for maxCapacity in ManaBatteryItem

                lines.add(Component.literal("Stored Mana: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(stored + " / " + max)
                                .withStyle(ChatFormatting.AQUA)));
            }
        });

        // Register HUD Render Callback for Mana Bar
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.options.hideGui) return;
            if (!ClientManaCache.isValid()) return;

            int mana = ClientManaCache.mana;
            int maxMana = ClientManaCache.maxMana;
            float ratio = maxMana > 0 ? (float) mana / maxMana : 0;

            // Position and Size
            int x = 10;
            int y = 10;
            int width = 100;
            int height = 8;

            // Calculate fill

            int progress = (int) (width * ratio);

            // 1. Draw Background (Darker blue-black)
            guiGraphics.fill(x, y, x + width, y + height, 0xFF121212);

            // 2. Draw Mana Bar (Vibrant Blue)
            guiGraphics.fill(x, y, x + progress, y + height, 0xFF00AAFF);

            // 3. Optional: Draw a subtle highlight on the bar
            guiGraphics.fill(x, y, x + progress, y + 2, 0x44FFFFFF);

            // 4. Border (White or Light Gray)
            guiGraphics.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xFFAAAAAA);

            // 5. Mana Text (Centered or Inline)
            String manaText = mana + " / " + maxMana;
            guiGraphics.drawString(client.font, manaText, x + width + 5, y, 0xFFFFFFFF, true);
        });

        // Register Screen Handlers
        MenuScreens.register(ModScreenHandlers.MANA_FURNACE_MENU, ManaFurnaceScreen::new);

        // Reset Mana Cache on Disconnect/Join
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientManaCache.set(0, 0);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientManaCache.set(0, 0);
        });
    }
}