package org.doothy.untitled.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface ClientOnlyEffect {
    void applyClient(EffectContext context);
}
