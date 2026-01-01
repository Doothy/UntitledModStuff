package org.doothy.untitled.mana.network;

public final class ManaNetworkContext {

    public final int maxTransferPerTick;
    public final int maxDistance;
    public final int maxConsumersPerTick;

    public ManaNetworkContext(
            int maxTransferPerTick,
            int maxDistance,
            int maxConsumersPerTick
    ) {
        this.maxTransferPerTick = maxTransferPerTick;
        this.maxDistance = maxDistance;
        this.maxConsumersPerTick = maxConsumersPerTick;
    }
}
