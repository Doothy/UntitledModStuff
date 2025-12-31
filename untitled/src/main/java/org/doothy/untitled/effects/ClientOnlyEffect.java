package org.doothy.untitled.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Represents an effect that is only applied on the client side.
 */
@Environment(EnvType.CLIENT)
public interface ClientOnlyEffect {
    /**
     * Applies the effect on the client side.
     *
     * @param context the context in which the effect is applied
     */
    void applyClient(EffectContext context);
}
