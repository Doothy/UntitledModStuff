package org.doothy.untitled.api.mana;

/**
 * Represents an object that produces Mana.
 */
public interface ManaProducer {
    int simulateExtract(int amount);
    int extract(int amount);
    /**
     * Retrieves the mana storage used for output.
     *
     * @return the output {@link ManaStorage}.
     */
    ManaStorage getManaOutput();
}
