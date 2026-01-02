package org.doothy.untitled.mana.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.doothy.untitled.api.mana.ManaConsumer;
import org.doothy.untitled.api.mana.ManaProducer;
import org.doothy.untitled.api.mana.ManaStorage;

import java.util.*;

public final class ManaNetworkTicker {

    private ManaNetworkTicker() {}

    /* ───────────────────────── Registration ───────────────────────── */

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ManaNetworkTicker::tick);
    }

    /* ───────────────────────── Tick Entry ───────────────────────── */

    private static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
    }

    /* ───────────────────────── Per-Level Tick ───────────────────────── */

    private static void tickLevel(ServerLevel level) {
        ManaNetworkContext context = ManaNetworkConfig.CONTEXT;
        if (context == null) return;

        Set<ManaConsumerNode> consumers =
                ManaConsumerRegistry.getConsumers(level);

        if (consumers.isEmpty()) return;

        ManaRequestBatch batch = new ManaRequestBatch();

        for (ManaConsumerNode node : consumers) {
            int request = computeRequest(node.consumer());
            if (request > 0) {
                batch.add(node.consumer(), request);
            }
        }

        if (batch.requests().isEmpty()) return;

        Collection<ManaProducerNode> producerNodes =
                ManaProducerRegistry.getProducers(level);

        if (producerNodes.isEmpty()) return;

        List<ManaProducer> producers = new ArrayList<>();
        for (ManaProducerNode node : producerNodes) {
            producers.add(node.producer());
        }

        int available = simulateAvailable(
                producers,
                context.maxTransferPerTick
        );

        if (available <= 0) return;

        ManaAllocationResult allocation =
                allocateFairly(batch, available);

        int remaining = available;
        for (ManaProducer producer : producers) {
            if (remaining <= 0) break;

            int extracted = producer.extract(
                    Math.min(remaining, context.maxTransferPerTick)
            );
            remaining -= extracted;
        }

        for (var consumer : allocation.consumers()) {
            int amount = allocation.getAllocation(consumer);
            if (amount > 0) {
                consumer.acceptMana(amount);
            }
        }
    }

    /* ───────────────────────── Helpers ───────────────────────── */

    private static int computeRequest(ManaConsumer consumer) {
        int perTick = consumer.getRequestedManaPerTick();
        if (perTick <= 0) return 0;

        if (!consumer.hasBuffer()) {
            return perTick;
        }

        ManaStorage buffer = consumer.getBuffer();
        long space = buffer.getMaxMana() - buffer.getMana();
        if (space <= 0) return 0;

        return (int) Math.min(space, perTick);
    }

    private static int simulateAvailable(
            List<ManaProducer> producers,
            int maxTransferPerTick
    ) {
        int total = 0;
        for (ManaProducer producer : producers) {
            total += producer.simulateExtract(maxTransferPerTick);
        }
        return total;
    }

    private static ManaAllocationResult allocateFairly(
            ManaRequestBatch batch,
            int available
    ) {
        ManaAllocationResult result = new ManaAllocationResult();

        int totalRequested = batch.totalRequested();
        if (totalRequested <= 0) return result;

        int distributed = 0;

        for (ManaRequestBatch.Request req : batch.requests()) {
            int share = (available * req.requested) / totalRequested;
            if (share > 0) {
                result.allocate(req.consumer, share);
                distributed += share;
            }
        }

        int remainder = available - distributed;
        if (remainder > 0) {
            for (ManaRequestBatch.Request req : batch.requests()) {
                if (remainder <= 0) break;
                result.allocate(
                        req.consumer,
                        result.getAllocation(req.consumer) + 1
                );
                remainder--;
            }
        }

        return result;
    }
}
