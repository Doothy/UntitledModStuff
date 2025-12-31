package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Cooldown policy that uses the built-in player item cooldowns for a fixed duration.
 */
public class SimpleItemCooldownPolicy implements EffectCooldownPolicy {

    private final int cooldownTicks;

    /**
     * @param cooldownTicks number of ticks to apply as cooldown after activation
     */
    public SimpleItemCooldownPolicy(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    /**
     * Returns true if the stack is not on cooldown for the player.
     */
    public boolean canActivate(Player player, ItemStack stack) {
        return !player.getCooldowns().isOnCooldown(stack);
    }

    @Override
    /**
     * Adds a cooldown entry for the given stack.
     */
    public void applyCooldown(Player player, ItemStack stack) {
        player.getCooldowns().addCooldown(stack, cooldownTicks);
    }
}
