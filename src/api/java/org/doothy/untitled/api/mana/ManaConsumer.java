package org.doothy.untitled.api.mana;

/**
 * Represents an object that consumes Mana.
 */
public interface ManaConsumer {
    /**
     * Retrieves the mana storage used for input.
     *
     * @return the input {@link ManaStorage}.
     */
    ManaStorage getManaInput();
}
