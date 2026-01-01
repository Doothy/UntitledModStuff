package org.doothy.untitled.mana.network;

import net.minecraft.server.level.ServerLevel;
import org.doothy.untitled.api.mana.ManaConsumer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ManaConsumerRegistry {

    private static final Set<ManaConsumerNode> CONSUMERS = new HashSet<>();

    private ManaConsumerRegistry() {}

    public static void register(ServerLevel level, ManaConsumer consumer, int x, int y, int z) {
        CONSUMERS.add(new ManaConsumerNode(
                consumer,
                level,
                new net.minecraft.core.BlockPos(x, y, z)
        ));
    }

    public static void unregister(ManaConsumer consumer) {
        CONSUMERS.removeIf(node -> node.consumer() == consumer);
    }

    public static Set<ManaConsumerNode> getConsumers(ServerLevel level) {
        Set<ManaConsumerNode> result = new HashSet<>();
        for (ManaConsumerNode node : CONSUMERS) {
            if (node.level() == level) {
                result.add(node);
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
