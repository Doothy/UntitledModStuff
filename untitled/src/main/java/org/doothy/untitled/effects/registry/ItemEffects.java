package org.doothy.untitled.effects.registry;

import net.minecraft.sounds.SoundSource;
import org.doothy.untitled.effects.*;
import org.doothy.untitled.effects.combat.ChainLightningEffect;
import org.doothy.untitled.effects.combat.LightningStrikeEffect;
import org.doothy.untitled.effects.combat.ShockwaveEffect;
import org.doothy.untitled.effects.sound.PositionedSoundEffect;
import org.doothy.untitled.sound.ModSounds;

/**
 * Central registry of item effects used by magic items.
 */
public final class ItemEffects {

    /**
     * Effect that summons a lightning strike at the target location.
     */
    public static final ItemEffect LIGHTNING_STRIKE =
            new LightningStrikeEffect();

    /**
     * Effect that creates a shockwave, knocking back and damaging entities.
     */
    public static final ItemEffect LIGHTNING_SHOCKWAVE =
            new ShockwaveEffect();

    /**
     * Effect that chains lightning between multiple targets.
     */
    public static final ItemEffect CHAIN_LIGHTNING =
            new ChainLightningEffect();

    /**
     * Effect that plays a thunder sound at the target location.
     */
    public static final ItemEffect LIGHTNING_SOUND = ctx ->
            new PositionedSoundEffect(
                    ModSounds.THUNDER_HIT,
                    SoundSource.PLAYERS,
                    4.0f,
                    1.0f
            ).play(ctx.level(), ctx.hitPosition());

    private ItemEffects() {}
}
