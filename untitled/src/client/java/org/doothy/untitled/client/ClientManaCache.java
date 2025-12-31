package org.doothy.untitled.client;

/**
 * Client-side cache of mana values mirrored from the server for HUD rendering.
 * The server remains authoritative; this cache is cleared on disconnect.
 */
public final class ClientManaCache {

    private static int mana = 0;
    private static int capacity = 0;

    private ClientManaCache() {}

    /**
     * Updates the cached mana values.
     *
     * @param mana      current mana amount
     * @param capacity  maximum mana capacity
     */
    public static void set(int mana, int capacity) {
        ClientManaCache.mana = mana;
        ClientManaCache.capacity = capacity;
    }

    /**
     * @return cached mana amount
     */
    public static int getMana() {
        return mana;
    }

    /**
     * @return cached maximum mana capacity
     */
    public static int getCapacity() {
        return capacity;
    }

    /**
     * @return true when a valid capacity is present
     */
    public static boolean isValid() {
        return capacity > 0;
    }

    /**
     * @return ratio in [0..1] representing current fill level; 0 if invalid
     */
    public static float getFillRatio() {
        return capacity <= 0 ? 0f : (float) mana / capacity;
    }

    /**
     * Resets the cache to an invalid, empty state.
     */
    public static void reset() {
        mana = 0;
        capacity = 0;
    }
}
