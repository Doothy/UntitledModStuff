package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SimpleItemCooldownPolicy implements EffectCooldownPolicy {

    private final int cooldownTicks;

    public SimpleItemCooldownPolicy(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public boolean canActivate(Player player, ItemStack stack) {
        return !player.getCooldowns().isOnCooldown(stack);
    }

    @Override
    public void applyCooldown(Player player, ItemStack stack) {
        player.getCooldowns().addCooldown(stack, cooldownTicks);
    }
}
