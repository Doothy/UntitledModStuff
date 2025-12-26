package org.doothy.untitled.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.items.LightningSoundHelper;
import org.doothy.untitled.network.ManaPayload;
import org.joml.Vector3f;

public class UntitledClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("DEBUG: UntitledClient Initialized!");

        LightningSoundHelper.Holder.INSTANCE = new ClientLightningSoundHelper();

        ClientPlayNetworking.registerGlobalReceiver(ManaPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.setAttached(ModAttachments.MANA,
                            new ManaAttachment(payload.current(), payload.max()));
                }
            });
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.options.hideGui) return;

            ManaAttachment mana = client.player.getAttached(ModAttachments.MANA);
            if (mana == null) return;

            // Position and Size
            int x = 10;
            int y = 10;
            int width = 100;
            int height = 8;

            // Calculate fill
            float ratio = (float) mana.getMana() / mana.getMaxMana();
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
            String manaText = mana.getMana() + " / " + mana.getMaxMana();
            guiGraphics.drawString(client.font, manaText, x + width + 5, y, 0xFFFFFFFF, true);
        });
    }
}