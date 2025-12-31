package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;

public class FlatManaCostPolicy implements ManaCostPolicy {

    private final int manaCost;

    public FlatManaCostPolicy(int manaCost) {
        this.manaCost = manaCost;
    }

    @Override
    public boolean hasMana(Player player) {
        return player.experienceLevel >= manaCost;
    }

    @Override
    public void consume(Player player) {
        player.giveExperienceLevels(-manaCost);
    }
}
