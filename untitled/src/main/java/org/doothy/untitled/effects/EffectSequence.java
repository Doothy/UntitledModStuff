package org.doothy.untitled.effects;

import java.util.List;

/**
 * Composite effect that applies a sequence of effects in order.
 */
public class EffectSequence implements ItemEffect {

    private final List<ItemEffect> effects;

    /**
     * Creates a new sequence from the provided effects.
     *
     * @param effects the effects to apply in order
     */
    public EffectSequence(ItemEffect... effects) {
        this.effects = List.of(effects);
    }

    /**
     * Applies all effects in the sequence using the same context.
     *
     * @param context the context in which the effects are applied
     */
    @Override
    public void apply(EffectContext context) {
        for (ItemEffect effect : effects) {
            effect.apply(context);
        }
    }
}
