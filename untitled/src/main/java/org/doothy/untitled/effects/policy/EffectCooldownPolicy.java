package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Strategy for checking and applying item activation cooldowns.
 */
public interface EffectCooldownPolicy {
    /**
     * Checks whether the given item can be activated by the player (i.e., not on cooldown).
     *
     * @param player player attempting activation
     * @param stack  item stack being activated
     * @return true if activation is allowed
     */
    boolean canActivate(Player player, ItemStack stack);

    /**
     * Applies the cooldown to the given item for the player.
     *
     * @param player player to apply cooldown for
     * @param stack  item stack to put on cooldown
     */
    void applyCooldown(Player player, ItemStack stack);
}
