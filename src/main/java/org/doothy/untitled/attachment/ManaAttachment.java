package org.doothy.untitled.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Data class representing the mana attachment on a player.
 * Stores current mana and maximum mana.
 */
public class ManaAttachment {
    /**
     * Codec for serializing and deserializing the attachment to/from NBT.
     */
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

    /**
     * Sets the current mana amount, clamped between 0 and maxMana.
     *
     * @param amount The new mana amount.
     */
    public void setMana(int amount) {
        this.mana = Math.clamp(amount, 0, maxMana);
    }
}