package org.doothy.untitled.api.mana;

public final class ManaTransfer {

    public static long transfer(
            ManaStorage from,
            ManaStorage to,
            long maxAmount
    ) {
        long extracted = from.extractMana(maxAmount, ManaTransaction.SIMULATE);
        if (extracted <= 0) return 0;

        long accepted = to.insertMana(extracted, ManaTransaction.SIMULATE);
        if (accepted <= 0) return 0;

        long amount = Math.min(extracted, accepted);
        from.extractMana(amount, ManaTransaction.EXECUTE);
        to.insertMana(amount, ManaTransaction.EXECUTE);
        return amount;
    }

    private ManaTransfer() {}
}
