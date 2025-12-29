package org.doothy.untitled.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import org.doothy.untitled.Untitled;

public class ModScreenHandlers {
    public static final MenuType<ManaFurnaceMenu> MANA_FURNACE_MENU = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "mana_furnace_menu"),
            new ExtendedScreenHandlerType<>(
                    ManaFurnaceMenu::new,
                    BlockPos.STREAM_CODEC
            )
    );

    public static void initialize() {}
}