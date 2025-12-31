package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ModAttachments;

/**
 * Mana cost policy backed by the player's {@code MANA} attachment.
 */
public class AttachmentManaCostPolicy implements ManaCostPolicy {

    private final long manaCost;

    /**
     * @param manaCost fixed mana cost to charge on activation
     */
    public AttachmentManaCostPolicy(long manaCost) {
        this.manaCost = manaCost;
    }

    /**
     * Checks whether the attached mana storage contains at least the fixed cost.
     *
     * @param player player to check
     * @return true if the player has enough mana
     */
    @Override
    public boolean hasMana(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        return mana != null && mana.getMana() >= manaCost;
    }

    /**
     * Extracts the fixed cost from the attached mana storage if present.
     *
     * @param player player from whom to consume
     */
    @Override
    public void consume(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        if (mana == null) return;

        mana.extractMana(manaCost, ManaTransaction.EXECUTE);
    }
}
