package org.doothy.untitled.mana.network;

/**
 * Static holder for the server-wide mana network context.
 *
 * <p>The context defines network-level constraints (e.g., per-tick transfer caps)
 * and is read by the {@link ManaNetworkTicker} on each tick. It is expected to be
 * configured during mod initialization or world load.</p>
 */
public final class ManaNetworkConfig {

    /**
     * Active context used by the ticker. If {@code null}, ticking is a no-op.
     */
    public static ManaNetworkContext CONTEXT;

    private ManaNetworkConfig() {}
}
