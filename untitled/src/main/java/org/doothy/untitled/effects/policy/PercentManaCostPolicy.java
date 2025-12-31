package org.doothy.untitled.effects.policy;

import net.minecraft.world.entity.player.Player;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ModAttachments;

public class PercentManaCostPolicy implements ManaCostPolicy {

    private final double percent; // 0.0 – 1.0

    public PercentManaCostPolicy(double percent) {
        this.percent = percent;
    }

    @Override
    public boolean hasMana(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        if (mana == null) return false;

        long cost = (long) (mana.getMaxMana() * percent);
        return mana.getMana() >= cost;
    }

    @Override
    public void consume(Player player) {
        ManaStorage mana = player.getAttached(ModAttachments.MANA);
        if (mana == null) return;

        long cost = (long) (mana.getMaxMana() * percent);
        mana.extractMana(cost, ManaTransaction.EXECUTE);
    }
}
