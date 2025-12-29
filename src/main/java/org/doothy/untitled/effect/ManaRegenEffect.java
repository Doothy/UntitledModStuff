package org.doothy.untitled.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A status effect that regenerates mana over time.
 * The actual regeneration logic is handled in the server tick event in {@link org.doothy.untitled.Untitled}.
 */
public class ManaRegenEffect extends MobEffect {
    public ManaRegenEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
