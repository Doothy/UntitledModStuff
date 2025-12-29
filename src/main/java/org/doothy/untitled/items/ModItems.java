package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.doothy.untitled.Untitled;

import java.util.function.Function;

public class ModItems {
    public static <GenericItem extends Item> GenericItem register(String name,
                                                                  Function<Item.Properties, GenericItem> itemFactory,
                                                                  Item.Properties settings){
        // Create resource key for the item
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Untitled.MOD_ID, name));

        // Create the item instance
        GenericItem item = itemFactory.apply(settings.setId(itemKey));

        // Register the item
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static final Item SUSPICIOUS_SUBSTANCE = register("suspicious_substance", Item::new, new Item.Properties());

    // BATTERIES
    // Tier 1: Stores 500 Mana, Drains 1 per tick
    public static final Item WEAK_MANA_BATTERY = register("weak_mana_battery",
            p -> new ManaBatteryItem(p, 500, 1),
            new Item.Properties());

    // Tier 2: Stores 5000 Mana, Drains 5 per tick
    public static final Item DENSE_MANA_BATTERY = register("dense_mana_battery",
            p -> new ManaBatteryItem(p, 5000, 5),
            new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));

    public static void initialize(){
        // Get the event for modifying entries in the (for example) ingredients group
        // Register an event handler that adds the item to the corresponding item group
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register((itemGroup) -> itemGroup.accept(ModItems.SUSPICIOUS_SUBSTANCE));

        // Add the suspicious substance to the composting registry with a 30% chance of increasing the composter's level.
        CompostingChanceRegistry.INSTANCE.add(ModItems.SUSPICIOUS_SUBSTANCE, 0.3f);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register((itemGroup) -> {
            itemGroup.accept(WEAK_MANA_BATTERY);
            itemGroup.accept(DENSE_MANA_BATTERY);
        });

        // Add an item to Fuels Registry, a second has 20 ticks, so we declare time in
        // amount of seconds * 20 ticks
        FuelRegistryEvents.BUILD.register((builder, context) -> {
            builder.add(ModItems.SUSPICIOUS_SUBSTANCE, 30 * 20);
        });
    }
}
