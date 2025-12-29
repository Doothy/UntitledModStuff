package org.doothy.untitled.client;

public final class ClientManaCache {

    private ClientManaCache() {} // No instantiation

    public static int mana = 0;
    public static int maxMana = 0;

    public static void set(int mana, int maxMana) {
        ClientManaCache.mana = mana;
        ClientManaCache.maxMana = maxMana;
    }

    public static boolean isValid() {
        return maxMana > 0;
    }
}
