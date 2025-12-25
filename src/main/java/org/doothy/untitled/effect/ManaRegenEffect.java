package org.doothy.untitled.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ManaRegenEffect extends MobEffect {
    // Logic is handled in the ServerTick inside Untitled.java
    public ManaRegenEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}