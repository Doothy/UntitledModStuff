package org.doothy.untitled.mana.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.doothy.untitled.api.mana.ManaConsumer;
import org.doothy.untitled.api.mana.ManaProducer;
import org.doothy.untitled.api.mana.ManaStorage;

import java.util.*;

/**
 * Server-side orchestrator that, every tick, collects mana requests from consumers,
 * simulates supply from producers, and distributes available mana fairly across
 * all active consumers within each level.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Registered once via {@link #register()} (hooks into END_SERVER_TICK).</li>
 *   <li>On each tick, iterates levels and performs a per-level allocation pass.</li>
 *   <li>Extraction from producers happens after simulation to avoid overdrawing.</li>
 * </ol>
 */
public final class ManaNetworkTicker {

    private ManaNetworkTicker() {}

    /* ───────────────────────── Registration ───────────────────────── */

    /**
     * Wires this ticker into Fabric's END_SERVER_TICK event.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ManaNetworkTicker::tick);
    }

    /* ───────────────────────── Tick Entry ───────────────────────── */

    /**
     * Entry point invoked once per server tick.
     *
     * @param server the running server instance
     */
    private static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
    }

    /* ───────────────────────── Per-Level Tick ───────────────────────── */

    /**
     * Performs the complete request/allocate/extract/accept cycle for a single level.
     *
     * @param level the level to process
     */
    private static void tickLevel(ServerLevel level) {
        ManaNetworkContext context = ManaNetworkConfig.CONTEXT;
        if (context == null) return;

        Set<ManaConsumerNode> consumers =
                ManaConsumerRegistry.getConsumers(level);

        if (consumers.isEmpty()) return;

        // Gather all consumer requests into a batch for this level
        ManaRequestBatch batch = new ManaRequestBatch();

        for (ManaConsumerNode node : consumers) {
            int request = computeRequest(node.consumer());
            if (request > 0) {
                batch.add(node.consumer(), request);
            }
        }

        if (batch.requests().isEmpty()) return;

        // Snapshot producers for this level
        Collection<ManaProducerNode> producerNodes =
                ManaProducerRegistry.getProducers(level);

        if (producerNodes.isEmpty()) return;

        List<ManaProducer> producers = new ArrayList<>();
        for (ManaProducerNode node : producerNodes) {
            producers.add(node.producer());
        }

        // Simulate without extracting first to determine total available budget
        int available = simulateAvailable(
                producers,
                context.maxTransferPerTick
        );

        if (available <= 0) return;

        // Allocate the budget proportionally across consumers
        ManaAllocationResult allocation =
                allocateFairly(batch, available);

        // Now execute actual extraction up to the allocated budget
        int remaining = available;
        for (ManaProducer producer : producers) {
            if (remaining <= 0) break;

            int extracted = producer.extract(
                    Math.min(remaining, context.maxTransferPerTick)
            );
            remaining -= extracted;
        }

        // Deliver allocated amounts to consumers
        for (var consumer : allocation.consumers()) {
            int amount = allocation.getAllocation(consumer);
            if (amount > 0) {
                consumer.acceptMana(amount);
            }
        }
    }

    /* ───────────────────────── Helpers ───────────────────────── */

    /**
     * Computes the effective request for a consumer, respecting per-tick limits and
     * optional internal buffer capacity to avoid overfilling.
     */
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

    /**
     * Sums the simulated extract of all producers without mutating their state.
     *
     * @param producers           the list of producers in the level
     * @param maxTransferPerTick  per-producer clamp during simulation
     * @return total mana that can be drawn this tick
     */
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

    /**
     * Distributes available mana proportionally to each consumer's request.
     * Remainders (due to integer division) are handed out one-by-one in request order.
     *
     * @param batch     the aggregated requests
     * @param available total available budget
     * @return allocation map
     */
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
            // Deterministic remainder distribution: first-come first-serve
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
