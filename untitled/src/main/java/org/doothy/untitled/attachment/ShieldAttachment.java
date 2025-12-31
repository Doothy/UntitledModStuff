package org.doothy.untitled.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class ShieldAttachment {

    public static final Codec<ShieldAttachment> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("ticks")
                            .orElse(0)
                            .forGetter(ShieldAttachment::ticks)
            ).apply(instance, ShieldAttachment::new));

    private int ticks;

    public ShieldAttachment(int ticks) {
        this.ticks = Math.max(0, ticks);
    }

    public int ticks() {
        return ticks;
    }

    public void setTicks(int ticks) {
        this.ticks = Math.max(0, ticks);
    }

    public void decrement() {
        if (ticks > 0) ticks--;
    }
}
