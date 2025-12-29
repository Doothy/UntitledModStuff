package org.doothy.untitled.mana;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.doothy.untitled.api.mana.ManaConsumer;
import org.doothy.untitled.api.mana.ManaProducer;
import org.doothy.untitled.api.mana.ManaStorage;

import java.util.Optional;

public final class ManaLookup {

    private ManaLookup() {}

    public static Optional<ManaStorage> find(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return Optional.empty();

        if (be instanceof ManaConsumer consumer) {
            return Optional.ofNullable(consumer.getManaInput());
        }

        if (be instanceof ManaProducer producer) {
            return Optional.ofNullable(producer.getManaOutput());
        }

        return Optional.empty();
    }
}
