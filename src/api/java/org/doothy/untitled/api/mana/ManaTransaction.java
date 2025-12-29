package org.doothy.untitled.api.mana;

/**
 * Defines the mode of a mana transaction.
 */
public enum ManaTransaction {
    /**
     * Simulate the transaction without modifying the state.
     */
    SIMULATE,
    /**
     * Execute the transaction and modify the state.
     */
    EXECUTE
}
