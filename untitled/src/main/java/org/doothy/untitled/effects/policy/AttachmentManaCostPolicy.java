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

    @Override
    /**
     * Checks whether the attached mana storage contains at least the fixed cost.
     */
    public boolean hasMana(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        return mana != null && mana.getMana() >= manaCost;
    }

    @Override
    /**
     * Extracts the fixed cost from the attached mana storage if present.
     */
    public void consume(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        if (mana == null) return;

        mana.extractMana(manaCost, ManaTransaction.EXECUTE);
    }
}
