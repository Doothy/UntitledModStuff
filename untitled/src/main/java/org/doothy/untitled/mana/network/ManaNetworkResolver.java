package org.doothy.untitled.mana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.doothy.untitled.api.mana.ManaProducer;

import java.util.ArrayList;
import java.util.List;

public final class ManaNetworkResolver {

    public static List<ManaProducer> resolveProducers(
            Level level,
            BlockPos origin,
            int maxDistance
    ) {
        List<ManaProducer> producers = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-maxDistance, -maxDistance, -maxDistance),
                origin.offset(maxDistance, maxDistance, maxDistance)
        )) {
            if (!level.isLoaded(pos)) continue;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ManaProducer producer) {
                producers.add(producer);
            }
        }

        return producers;
    }
}
