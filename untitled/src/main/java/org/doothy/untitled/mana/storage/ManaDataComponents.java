package org.doothy.untitled.mana.storage;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

import static org.doothy.untitled.Untitled.MOD_ID;

public final class ManaDataComponents {

    public static final DataComponentType<BlockManaStorageComponent> BLOCK_MANA_STORAGE =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(MOD_ID, "block_mana_storage"),
                    DataComponentType.<BlockManaStorageComponent>builder()
                            .persistent(BlockManaStorageComponent.CODEC)
                            .networkSynchronized(
                                    ByteBufCodecs.fromCodec(BlockManaStorageComponent.CODEC)
                            )
                            .build()
            );

    private ManaDataComponents() {}
}
