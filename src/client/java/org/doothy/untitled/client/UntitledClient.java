package org.doothy.untitled.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.network.ManaPayload;
import org.joml.Vector3f;

public class UntitledClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("DEBUG: UntitledClient Initialized!");

        // 1. Receiver logic remains the same
        ClientPlayNetworking.registerGlobalReceiver(ManaPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.setAttached(ModAttachments.MANA,
                            new ManaAttachment(payload.current(), payload.max()));
                }
            });
        });

        // 2. Updated HUD Renderer for 1.21.1
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.options.hideGui) return;

            ManaAttachment mana = client.player.getAttached(ModAttachments.MANA);

            int x = 20;
            int y = 20;

            String text = (mana == null) ? "Mana: Loading..." : "Mana: " + mana.getMana() + "/" + mana.getMaxMana();

            // Full opacity Cyan (0xFF for Alpha)
            int textColor = 0xFF55FFFF;
            int boxColor = 0x88000000;

            int textWidth = client.font.width(text);

            // 1. Draw the background box
            guiGraphics.fill(x - 5, y - 5, x + textWidth + 5, y + 15, boxColor);

            // 2. Use push/popMatrix to move the text "forward"
            guiGraphics.pose().pushMatrix();
            // Translating Z by 100 puts it clearly in front of the box
            guiGraphics.pose().translate(0.0f, 0.0f);

            // Using the drawString method that takes a String and a boolean for shadow
            guiGraphics.drawString(
                    client.font,
                    text,
                    x,
                    y,
                    textColor,
                    true
            );

            guiGraphics.pose().popMatrix();
        });
    }
}