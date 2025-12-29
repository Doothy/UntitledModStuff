package org.doothy.untitled.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ManaAttachment {
    // This Codec is for saving to the player's .dat file (NBT)
    public static final Codec<ManaAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("mana").forGetter(ManaAttachment::getMana),
            Codec.INT.fieldOf("max_mana").forGetter(ManaAttachment::getMaxMana)
    ).apply(instance, ManaAttachment::new));

    private int mana;
    private int maxMana;

    public ManaAttachment(int mana, int maxMana) {
        this.mana = mana;
        this.maxMana = maxMana;
    }

    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }

    public void setMana(int amount) {
        this.mana = Math.clamp(amount, 0, maxMana);
    }
}
