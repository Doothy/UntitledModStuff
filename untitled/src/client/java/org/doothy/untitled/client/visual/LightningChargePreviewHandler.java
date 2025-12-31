package org.doothy.untitled.client.visual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.doothy.untitled.combat.RaycastTargeting;
import org.doothy.untitled.items.LightningStick;

public final class LightningChargePreviewHandler {

    private LightningChargePreviewHandler() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientLevel level = client.level;
            Player player = client.player;

            if (level == null || player == null) {
                LightningTargetPreview.clear();
                return;
            }

            ItemStack using = player.getUseItem();
            if (!(using.getItem() instanceof LightningStick)) {
                LightningTargetPreview.clear();
                return;
            }

            // Only while actively charging
            if (!player.isUsingItem()) {
                LightningTargetPreview.clear();
                return;
            }

            HitResult hit = RaycastTargeting.raycast(player, LightningStick.TARGET_REACH);
            if (hit == null) {
                LightningTargetPreview.clear();
                return;
            }

            LightningTargetPreview.show(hit.getLocation());
        });
    }
}