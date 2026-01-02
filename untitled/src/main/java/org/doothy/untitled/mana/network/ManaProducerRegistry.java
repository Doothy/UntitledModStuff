package org.doothy.untitled.mana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.doothy.untitled.api.mana.ManaProducer;

import java.util.*;

public final class ManaProducerRegistry {
    private static final Map<ServerLevel, Set<ManaProducerNode>> BY_LEVEL = new HashMap<>();
    private static final Map<ManaProducer, ManaProducerNode> BY_PRODUCER = new IdentityHashMap<>();

    private ManaProducerRegistry() {}

    public static void register(
            ServerLevel level,
            ManaProducer producer,
            BlockPos pos
    ) {
        if (BY_PRODUCER.containsKey(producer)) {
            return; // idempotent
        }

        ManaProducerNode node = new ManaProducerNode(producer, level, pos);

        BY_PRODUCER.put(producer, node);
        BY_LEVEL
                .computeIfAbsent(level, l -> new HashSet<>())
                .add(node);
    }

    public static void unregister(ManaProducer producer) {
        ManaProducerNode node = BY_PRODUCER.remove(producer);
        if (node == null) {
            return;
        }

        Set<ManaProducerNode> set = BY_LEVEL.get(node.level());
        if (set != null) {
            set.remove(node);
            if (set.isEmpty()) {
                BY_LEVEL.remove(node.level());
            }
        }
    }

    public static Collection<ManaProducerNode> getProducers(ServerLevel level) {
        Set<ManaProducerNode> set = BY_LEVEL.get(level);
        return set != null
                ? Collections.unmodifiableSet(set)
                : List.of();
    }
}

