package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ModAttachments;

public class AttachmentManaCostPolicy implements ManaCostPolicy {

    private final long manaCost;

    public AttachmentManaCostPolicy(long manaCost) {
        this.manaCost = manaCost;
    }

    @Override
    public boolean hasMana(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        return mana != null && mana.getMana() >= manaCost;
    }

    @Override
    public void consume(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        if (mana == null) return;

        mana.extractMana(manaCost, ManaTransaction.EXECUTE);
    }
}
