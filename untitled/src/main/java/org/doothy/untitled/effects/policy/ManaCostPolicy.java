package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;

public interface ManaCostPolicy {

    /**
     * Checks whether the player has enough mana.
     */
    boolean hasMana(Player player);

    /**
     * Consumes mana. Assumes hasMana() was checked.
     */
    void consume(Player player);
}
