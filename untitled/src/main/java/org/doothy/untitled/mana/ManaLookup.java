package org.doothy.untitled.mana;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.doothy.untitled.api.mana.ManaConsumer;
import org.doothy.untitled.api.mana.ManaProducer;
import org.doothy.untitled.api.mana.ManaStorage;

import java.util.Optional;

/**
 * Utility class for looking up Mana capabilities in the world.
 */
public final class ManaLookup {

    private ManaLookup() {}

    /**
     * Attempts to find a {@link ManaStorage} at the specified position in the level.
     * <p>
     * This method checks if the block entity at the given position implements {@link ManaConsumer} or {@link ManaProducer}.
     *
     * @param level the level to search in.
     * @param pos   the position to check.
     * @return an {@link Optional} containing the found {@link ManaStorage}, or empty if none found.
     */
    public static Optional<ManaStorage> find(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return Optional.empty();

        if (be instanceof ManaProducer producer) {
            return Optional.ofNullable(producer.getManaOutput());
        }

        return Optional.empty();
    }
}
