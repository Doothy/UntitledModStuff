package org.doothy.untitled.items;

import net.minecraft.world.item.Item;
import org.doothy.untitled.effects.policy.EffectCooldownPolicy;
import org.doothy.untitled.effects.policy.ManaCostPolicy;

public abstract class AbstractMagicItem extends Item {

    protected final EffectCooldownPolicy cooldownPolicy;
    protected final ManaCostPolicy manaPolicy;

    protected AbstractMagicItem(
            Properties properties,
            EffectCooldownPolicy cooldownPolicy,
            ManaCostPolicy manaPolicy
    ) {
        super(properties);
        this.cooldownPolicy = cooldownPolicy;
        this.manaPolicy = manaPolicy;
    }
}
