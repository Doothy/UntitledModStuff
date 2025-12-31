package org.doothy.untitled.items;

import net.minecraft.world.item.Item;
import org.doothy.untitled.effects.policy.EffectCooldownPolicy;
import org.doothy.untitled.effects.policy.ManaCostPolicy;

/**
 * Base class for magic items that require mana and enforce an activation cooldown.
 * Subclasses access the configured policies via the protected fields.
 */
public abstract class AbstractMagicItem extends Item {

    protected final EffectCooldownPolicy cooldownPolicy;
    protected final ManaCostPolicy manaPolicy;

    /**
     * Constructs a magic item with the provided cooldown and mana policies.
     *
     * @param properties       vanilla item properties
     * @param cooldownPolicy   policy controlling activation cooldowns
     * @param manaPolicy       policy controlling mana availability/consumption
     */
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
