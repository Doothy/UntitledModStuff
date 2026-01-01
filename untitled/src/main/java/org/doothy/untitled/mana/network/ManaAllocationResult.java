package org.doothy.untitled.mana.network;

import org.doothy.untitled.api.mana.ManaConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ManaAllocationResult {

    private final Map<ManaConsumer, Integer> allocations = new HashMap<>();

    public void allocate(ManaConsumer consumer, int amount) {
        allocations.put(consumer, amount);
    }

    public int getAllocation(ManaConsumer consumer) {
        return allocations.getOrDefault(consumer, 0);
    }

    public Set<ManaConsumer> consumers() {
        return allocations.keySet();
    }
}

