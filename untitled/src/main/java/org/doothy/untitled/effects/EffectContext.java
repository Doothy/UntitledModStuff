package org.doothy.untitled.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Context object containing all necessary information for applying an effect.
 *
 * @param level       the server level where the effect occurs
 * @param player      the player initiating the effect
 * @param hitPosition the position where the effect is applied (e.g., impact point)
 * @param charge      normalized charge value [0.0, 1.0] indicating effect intensity
 */
public record EffectContext(
        ServerLevel level,
        Player player,
        Vec3 hitPosition,
        float charge // 0.0 → 1.0
) {}
