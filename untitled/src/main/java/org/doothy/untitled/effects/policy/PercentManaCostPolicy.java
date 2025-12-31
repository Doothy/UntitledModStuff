package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ModAttachments;

/**
 * Mana cost policy that charges a percentage of the player's maximum mana.
 */
public class PercentManaCostPolicy implements ManaCostPolicy {

    private final double percent;

    /**
     * @param percent fraction in [0.0, 1.0] of max mana to charge
     */
    public PercentManaCostPolicy(double percent) {
        this.percent = percent;
    }

    /**
     * Checks whether the attached mana storage has at least the computed percent cost.
     *
     * @param player player to check
     * @return true if the player has enough mana
     */
    @Override
    public boolean hasMana(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        if (mana == null) return false;

        long cost = (long) (mana.getMaxMana() * percent);
        return mana.getMana() >= cost;
    }

    /**
     * Consumes the computed percentage of max mana from the attached storage.
     *
     * @param player player from whom to consume
     */
    @Override
    public void consume(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        if (mana == null) return;

        long cost = (long) (mana.getMaxMana() * percent);
        mana.extractMana(cost, ManaTransaction.EXECUTE);
    }
}
