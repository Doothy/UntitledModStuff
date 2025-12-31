package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;

/**
 * Strategy for checking and consuming a resource required to activate effects.
 */
public interface ManaCostPolicy {

    /**
     * Checks whether the player has sufficient resource to activate.
     *
     * @param player player to check
     * @return true if enough resource is available
     */
    boolean hasMana(Player player);

    /**
     * Consumes the necessary resource. Callers should first check {@link #hasMana(Player)}.
     *
     * @param player player from whom to consume
     */
    void consume(Player player);
}
