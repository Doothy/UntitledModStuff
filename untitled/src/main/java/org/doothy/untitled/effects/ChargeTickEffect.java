package org.doothy.untitled.effects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Represents an effect that is applied periodically while an item is being charged.
 */
public interface ChargeTickEffect {
    /**
     * Called on each tick while the item is being charged.
     *
     * @param level        the level where the charging is happening
     * @param user         the entity charging the item
     * @param stack        the item stack being charged
     * @param elapsedTicks the number of ticks since charging started
     */
    void onChargeTick(Level level, LivingEntity user, ItemStack stack, int elapsedTicks);
}
