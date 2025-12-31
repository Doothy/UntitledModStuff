package org.doothy.untitled.client;

public final class ClientShieldCache {

    private static boolean active;
    private static int visualTicks;

    private ClientShieldCache() {}

    public static void start() {
        active = true;
        visualTicks = 0;
    }

    public static void stop() {
        active = false;
    }

    public static void tickVisual() {
        if (active) visualTicks++;
    }

    public static boolean active() {
        return active;
    }

    public static int visualTicks() {
        return visualTicks;
    }

    public static void reset() {
        active = false;
        visualTicks = 0;
    }
}
