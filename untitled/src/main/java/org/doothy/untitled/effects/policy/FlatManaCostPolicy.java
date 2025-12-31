package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;

/**
 * Mana cost policy that uses vanilla experience levels as a stand-in resource.
 */
public class FlatManaCostPolicy implements ManaCostPolicy {

    private final int manaCost;

    /**
     * @param manaCost number of experience levels to consume
     */
    public FlatManaCostPolicy(int manaCost) {
        this.manaCost = manaCost;
    }

    @Override
    /**
     * Checks if the player has at least the required XP levels.
     */
    public boolean hasMana(Player player) {
        return player.experienceLevel >= manaCost;
    }

    @Override
    /**
     * Removes the required XP levels from the player.
     */
    public void consume(Player player) {
        player.giveExperienceLevels(-manaCost);
    }
}
