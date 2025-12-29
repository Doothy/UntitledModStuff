package org.doothy.untitled.api.mana;

public interface ManaStorage {
    long getMana();
    long getMaxMana();

    long insertMana(long amount, ManaTransaction tx);
    long extractMana(long amount, ManaTransaction tx);

    default boolean isEmpty() {
        return getMana() <= 0;
    }

    default boolean isFull() {
        return getMana() >= getMaxMana();
    }
}
