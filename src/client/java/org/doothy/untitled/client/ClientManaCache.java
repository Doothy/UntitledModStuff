package org.doothy.untitled.client;

/**
 * A simple cache for storing the player's mana on the client side.
 * Used for rendering the HUD.
 */
public final class ClientManaCache {

    private ClientManaCache() {} // No instantiation

    /** Current mana amount. */
    public static int mana = 0;
    /** Maximum mana amount. */
    public static int maxMana = 0;

    /**
     * Updates the cache with new values.
     *
     * @param mana    The current mana.
     * @param maxMana The maximum mana.
     */
    public static void set(int mana, int maxMana) {
        ClientManaCache.mana = mana;
        ClientManaCache.maxMana = maxMana;
    }

    /**
     * Checks if the cache contains valid data (maxMana > 0).
     *
     * @return True if valid, false otherwise.
     */
    public static boolean isValid() {
        return maxMana > 0;
    }
}