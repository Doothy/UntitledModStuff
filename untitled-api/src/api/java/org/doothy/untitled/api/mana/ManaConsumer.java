package org.doothy.untitled.api.mana;

public interface ManaConsumer {

    /**
     * Maximum mana this consumer wants per tick.
     */
    int getRequestedManaPerTick();

    /**
     * Called once per tick by the mana system.
     */
    void acceptMana(int amount);

    /**
     * Whether this consumer has a local buffer.
     */
    default boolean hasBuffer() {
        return true;
    }

    /**
     * Local buffer for network-delivered mana.
     * Must NOT be network-visible.
     */
    ManaStorage getBuffer();
}
