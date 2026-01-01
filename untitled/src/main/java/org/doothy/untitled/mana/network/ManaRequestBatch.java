package org.doothy.untitled.mana.network;

import org.doothy.untitled.api.mana.ManaConsumer;

import java.util.ArrayList;
import java.util.List;

public final class ManaRequestBatch {

    public static final class Request {
        public final ManaConsumer consumer;
        public final int requested;

        public Request(ManaConsumer consumer, int requested) {
            this.consumer = consumer;
            this.requested = requested;
        }
    }

    private final List<Request> requests = new ArrayList<>();

    public void add(ManaConsumer consumer, int requested) {
        if (requested > 0) {
            requests.add(new Request(consumer, requested));
        }
    }

    public List<Request> requests() {
        return requests;
    }

    public int totalRequested() {
        return requests.stream().mapToInt(r -> r.requested).sum();
    }
}

