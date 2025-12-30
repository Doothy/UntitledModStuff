package org.doothy.untitled.api.mana;

/**
 * Utility class for transferring Mana between storages.
 */
public final class ManaTransfer {

    /**
     * Transfers mana from one storage to another.
     * <p>
     * This method first simulates the extraction and insertion to determine the maximum possible transfer amount,
     * and then executes the transfer.
     *
     * @param from      the source {@link ManaStorage}.
     * @param to        the destination {@link ManaStorage}.
     * @param maxAmount the maximum amount of mana to transfer.
     * @return the actual amount of mana transferred.
     */
    public static long transfer(
            ManaStorage from,
            ManaStorage to,
            long maxAmount
    ) {
        // Simulate extraction from source
        long extracted = from.extractMana(maxAmount, ManaTransaction.SIMULATE);
        if (extracted <= 0) return 0;

        // Simulate insertion into destination
        long accepted = to.insertMana(extracted, ManaTransaction.SIMULATE);
        if (accepted <= 0) return 0;

        // Determine the actual amount to transfer
        long amount = Math.min(extracted, accepted);

        // Execute the transfer
        from.extractMana(amount, ManaTransaction.EXECUTE);
        to.insertMana(amount, ManaTransaction.EXECUTE);
        return amount;
    }

    private ManaTransfer() {}
}
