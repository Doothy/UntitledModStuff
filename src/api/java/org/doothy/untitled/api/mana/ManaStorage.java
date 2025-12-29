package org.doothy.untitled.api.mana;

/**
 * Represents a storage for Mana.
 * <p>
 * Implementations of this interface define how mana is stored, retrieved, and modified.
 */
public interface ManaStorage {
    /**
     * Returns the current amount of mana stored.
     *
     * @return the current mana amount.
     */
    long getMana();

    /**
     * Returns the maximum amount of mana that can be stored.
     *
     * @return the maximum mana capacity.
     */
    long getMaxMana();

    /**
     * Inserts mana into the storage.
     *
     * @param amount the amount of mana to insert.
     * @param tx     the transaction mode (SIMULATE or EXECUTE).
     * @return the actual amount of mana inserted.
     */
    long insertMana(long amount, ManaTransaction tx);

    /**
     * Extracts mana from the storage.
     *
     * @param amount the amount of mana to extract.
     * @param tx     the transaction mode (SIMULATE or EXECUTE).
     * @return the actual amount of mana extracted.
     */
    long extractMana(long amount, ManaTransaction tx);

    /**
     * Checks if the storage is empty.
     *
     * @return true if the storage contains no mana, false otherwise.
     */
    default boolean isEmpty() {
        return getMana() <= 0;
    }

    /**
     * Checks if the storage is full.
     *
     * @return true if the storage is at maximum capacity, false otherwise.
     */
    default boolean isFull() {
        return getMana() >= getMaxMana();
    }
}
