package org.doothy.untitled.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.doothy.untitled.Untitled;

import java.util.function.Function;

/**
 * Registry class for all custom blocks in the mod.
 */
public class ModBlocks {

    /**
     * The Mana Furnace block.
     */
    public static final Block MANA_FURNACE = register("mana_furnace", key ->
            new ManaFurnaceBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.5f)
            ));

    /**
     * Registers a block and its corresponding item.
     *
     * @param name    The registry name of the block.
     * @param factory A function to create the block instance.
     * @return The registered block.
     */
    private static Block register(String name, Function<ResourceKey<Block>, Block> factory) {
        // 1. Create IDs and Keys for both Block and Item
        Identifier id = Identifier.fromNamespaceAndPath(Untitled.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);

        // 2. Create the Block (using the blockKey)
        Block block = factory.apply(blockKey);

        // 3. Register the Block
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        // 4. Register the BlockItem (using the itemKey)
        // FIX: calling .setId(itemKey) on the properties is now required!
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey)));

        return block;
    }

    /**
     * Initializes the blocks and adds them to creative tabs.
     */
    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(MANA_FURNACE);
        });
    }
}
