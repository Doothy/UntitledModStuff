package org.doothy.untitled.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;

import java.util.Optional;

/**
 * Default {@link org.doothy.untitled.api.mana.ManaStorage} implementation used by attachments.
 * Implementation detail of the mod (not part of the public API).
 */
public final class ManaAttachment implements ManaStorage {

    /** Default capacity for new players (no saved data). */
    public static final long DEFAULT_CAPACITY = 100;

    /**
     * Codec for serializing mana and capacity; supports legacy key "max_mana" in saves.
     */
    public static final Codec<ManaAttachment> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.fieldOf("mana")
                            .orElse(0L)
                            .forGetter(ManaAttachment::getMana),
                    Codec.LONG.optionalFieldOf("capacity")
                            .forGetter(att -> Optional.of(att.getMaxMana())),
                    Codec.LONG.optionalFieldOf("max_mana")
                            .forGetter(att -> Optional.of(att.getMaxMana()))
            ).apply(instance, (mana, capacityOpt, legacyOpt) -> {
                long capacity = capacityOpt
                        .orElseGet(() -> legacyOpt.orElse(DEFAULT_CAPACITY));
                return new ManaAttachment(mana, capacity);
            }));

    private long mana;
    private final long capacity;

    /**
     * Full constructor used by the codec; clamps negative values and enforces nonzero capacity.
     */
    public ManaAttachment(long mana, long capacity) {
        this.capacity = capacity > 0 ? capacity : DEFAULT_CAPACITY;
        this.mana = Math.min(Math.max(0, mana), this.capacity);
    }

    /** Convenience constructor defaulting mana to 0. */
    public ManaAttachment(long capacity) {
        this(0, capacity);
    }

    @Override
    public long getMana() {
        return mana;
    }

    @Override
    public long getMaxMana() {
        return capacity;
    }

    @Override
    public long insertMana(long amount, ManaTransaction tx) {
        if (amount <= 0) return 0;

        long accepted = Math.min(amount, capacity - mana);
        if (tx == ManaTransaction.EXECUTE && accepted > 0) {
            mana += accepted;
        }
        return accepted;
    }

    @Override
    public long extractMana(long amount, ManaTransaction tx) {
        if (amount <= 0) return 0;

        long extracted = Math.min(amount, mana);
        if (tx == ManaTransaction.EXECUTE && extracted > 0) {
            mana -= extracted;
        }
        return extracted;
    }
}
