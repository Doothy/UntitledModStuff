package org.doothy.untitled.client;

/**
 * Client-side cache for the player's mana.
 * Server is authoritative; this is render-only state.
 */
public final class ClientManaCache {

    private static int mana = 0;
    private static int capacity = 0;

    private ClientManaCache() {}

    public static void set(int mana, int capacity) {
        ClientManaCache.mana = mana;
        ClientManaCache.capacity = capacity;
    }

    public static int getMana() {
        return mana;
    }

    public static int getCapacity() {
        return capacity;
    }

    public static boolean isValid() {
        return capacity > 0;
    }

    public static float getFillRatio() {
        return capacity <= 0 ? 0f : (float) mana / capacity;
    }

    public static void reset() {
        mana = 0;
        capacity = 0;
    }
}
