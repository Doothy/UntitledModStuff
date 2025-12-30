package org.doothy.untitled.client.visual;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.items.LightningStick;

public final class LightningShieldVisual {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level == null) return;
        if (!player.isUsingItem()) return;
        if (!(player.getUseItem().getItem() instanceof LightningStick)) return;

        ItemStack stack = player.getUseItem();

        int used =
                stack.getItem().getUseDuration(stack, player)
                        - player.getUseItemRemainingTicks();

        double radius = 1.5 + Math.sin(used * 0.2) * 0.2;

        for (int i = 0; i < 6; i++) {
            double angle = (used * 0.3) + (Math.PI * 2 * i / 6);

            Vec3 pos = player.position().add(
                    Math.cos(angle) * radius,
                    0.8,
                    Math.sin(angle) * radius
            );

            level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z,
                    0, 0.01, 0
            );
        }
    }
}
