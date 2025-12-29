package org.doothy.untitled.block.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.block.ModBlocks;

/**
 * Registry class for all custom block entities in the mod.
 */
public class ModBlockEntities {
    /**
     * The BlockEntityType for the Mana Furnace.
     */
    public static final BlockEntityType<ManaFurnaceBlockEntity> MANA_FURNACE_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "mana_furnace_be"),
            FabricBlockEntityTypeBuilder.create(ManaFurnaceBlockEntity::new, ModBlocks.MANA_FURNACE).build()
    );

    /**
     * Initializes the block entities.
     */
    public static void initialize() {}
}