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

    /**
     * Returns true if the stack is not on cooldown for the player.
     *
     * @param player the player attempting to activate the item
     * @param stack the item stack being used
     * @return true if the item is not on cooldown, false otherwise
     */
    @Override
    public boolean canActivate(Player player, ItemStack stack) {
        return !player.getCooldowns().isOnCooldown(stack);
    }

    /**
     * Adds a cooldown entry for the given stack.
     *
     * @param player the player using the item
     * @param stack the item stack to apply cooldown to
     */
    @Override
    public void applyCooldown(Player player, ItemStack stack) {
        player.getCooldowns().addCooldown(stack, cooldownTicks);
    }
}
