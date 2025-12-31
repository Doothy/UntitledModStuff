package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface EffectCooldownPolicy {
    boolean canActivate(Player player, ItemStack stack);
    void applyCooldown(Player player, ItemStack stack);
}
