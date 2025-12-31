package org.doothy.untitled.effects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ChargeTickEffect {
    void onChargeTick(Level level, LivingEntity user, ItemStack stack, int elapsedTicks);
}
