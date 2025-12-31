package org.doothy.untitled.client;

/**
 * Client-side cache for temporary shield visuals/state. Used to drive rendering
 * and simple timers based on server notifications. Server is authoritative.
 */
public final class ClientShieldCache {

    private static boolean active;
    private static int visualTicks;

    private ClientShieldCache() {}

    /**
     * Activates the shield visuals and resets the local visual tick counter.
     */
    public static void start() {
        active = true;
        visualTicks = 0;
    }

    /**
     * Deactivates the shield visuals.
     */
    public static void stop() {
        active = false;
    }

    /**
     * Increments the local visual timer when active; intended to be called per-client-tick.
     */
    public static void tickVisual() {
        if (active) visualTicks++;
    }

    /**
     * @return true if the shield visuals are currently active
     */
    public static boolean active() {
        return active;
    }

    /**
     * @return number of client ticks since visuals were activated
     */
    public static int visualTicks() {
        return visualTicks;
    }

    /**
     * Resets the cache to a non-active state and clears timers.
     */
    public static void reset() {
        active = false;
        visualTicks = 0;
    }
}
