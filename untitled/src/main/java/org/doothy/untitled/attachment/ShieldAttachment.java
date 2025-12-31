package org.doothy.untitled.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Attachment storing remaining duration of a temporary shield effect.
 */
public final class ShieldAttachment {

    /** Codec for serializing the remaining tick count. */
    public static final Codec<ShieldAttachment> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("ticks")
                            .orElse(0)
                            .forGetter(ShieldAttachment::ticks)
            ).apply(instance, ShieldAttachment::new));

    private int ticks;

    /**
     * Creates a new shield attachment with the given remaining ticks.
     * Negative values are clamped to zero.
     *
     * @param ticks remaining shield duration in ticks
     */
    public ShieldAttachment(int ticks) {
        this.ticks = Math.max(0, ticks);
    }

    /**
     * @return remaining shield duration in ticks
     */
    public int ticks() {
        return ticks;
    }

    /**
     * Sets remaining shield ticks; negative values are clamped to zero.
     *
     * @param ticks new remaining duration
     */
    public void setTicks(int ticks) {
        this.ticks = Math.max(0, ticks);
    }

    /**
     * Decrements remaining ticks by one if positive.
     */
    public void decrement() {
        if (ticks > 0) ticks--;
    }
}
