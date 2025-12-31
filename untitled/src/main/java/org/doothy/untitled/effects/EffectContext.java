package org.doothy.untitled.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public record EffectContext(
        ServerLevel level,
        Player player,
        Vec3 hitPosition,
        float charge // 0.0 → 1.0
) {}
