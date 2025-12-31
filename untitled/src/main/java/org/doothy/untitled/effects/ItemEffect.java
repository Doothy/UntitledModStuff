package org.doothy.untitled.effects;

/**
 * Represents an effect that can be applied by an item.
 */
public interface ItemEffect {
    /**
     * Applies the effect using the provided context.
     *
     * @param context the context in which the effect is applied
     */
    void apply(EffectContext context);
}
