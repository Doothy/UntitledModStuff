package org.doothy.untitled.mana.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record BlockManaStorageComponent(int mana, int capacity) {

    public static final Codec<BlockManaStorageComponent> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("mana")
                            .orElse(0)
                            .forGetter(BlockManaStorageComponent::mana),
                    Codec.INT.fieldOf("capacity")
                            .forGetter(BlockManaStorageComponent::capacity)
            ).apply(instance, BlockManaStorageComponent::new));

    public BlockManaStorageComponent {
        mana = Math.max(0, Math.min(mana, capacity));
    }

    public BlockManaStorageComponent withMana(int newMana) {
        return new BlockManaStorageComponent(newMana, capacity);
    }

    public int space() {
        return capacity - mana;
    }
}
