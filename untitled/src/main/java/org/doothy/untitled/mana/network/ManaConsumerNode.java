package org.doothy.untitled.mana.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.doothy.untitled.api.mana.ManaConsumer;

public record ManaConsumerNode(
        ManaConsumer consumer,
        Level level,
        BlockPos pos
) {}