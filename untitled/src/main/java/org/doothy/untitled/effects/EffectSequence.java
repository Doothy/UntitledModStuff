package org.doothy.untitled.effects;

import java.util.List;

public class EffectSequence implements ItemEffect {

    private final List<ItemEffect> effects;

    public EffectSequence(ItemEffect... effects) {
        this.effects = List.of(effects);
    }

    @Override
    public void apply(EffectContext context) {
        for (ItemEffect effect : effects) {
            effect.apply(context);
        }
    }
}
