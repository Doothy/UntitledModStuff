package org.doothy.untitled.mana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.doothy.untitled.api.mana.ManaProducer;

public final class ManaProducerNode {

    private final ManaProducer producer;
    private final ServerLevel level;
    private final BlockPos pos;

    public ManaProducerNode(
            ManaProducer producer,
            ServerLevel level,
            BlockPos pos
    ) {
        this.producer = producer;
        this.level = level;
        this.pos = pos;
    }

    public ManaProducer producer() {
        return producer;
    }

    public ServerLevel level() {
        return level;
    }

    public BlockPos pos() {
        return pos;
    }
}
