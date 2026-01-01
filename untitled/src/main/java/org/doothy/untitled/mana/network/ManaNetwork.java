package org.doothy.untitled.mana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.doothy.untitled.api.mana.ManaProducer;

import java.util.List;

public final class ManaNetwork {

    private final ManaNetworkContext context;

    public ManaNetwork(ManaNetworkContext context) {
        this.context = context;
    }

    public ManaAllocationResult resolve(
            Level level,
            BlockPos origin,
            ManaRequestBatch batch
    ) {
        ManaAllocationResult result = new ManaAllocationResult();

        List<ManaProducer> producers =
                ManaNetworkResolver.resolveProducers(level, origin, context.maxDistance);

        int available = producers.stream()
                .mapToInt(p -> p.simulateExtract(context.maxTransferPerTick))
                .sum();

        int totalRequested = batch.totalRequested();
        if (available <= 0 || totalRequested <= 0) return result;

        for (var req : batch.requests()) {
            int share = Math.min(
                    req.requested,
                    (available * req.requested) / totalRequested
            );
            result.allocate(req.consumer, share);
        }

        // Commit phase
        int remaining = available;
        for (ManaProducer producer : producers) {
            if (remaining <= 0) break;
            int extracted = producer.extract(remaining);
            remaining -= extracted;
        }

        return result;
    }
}

