package org.doothy.untitled;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import org.doothy.untitled.items.LightningStick;
import org.doothy.untitled.items.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentType;
import com.mojang.serialization.Codec;

public class Untitled implements ModInitializer {

    public static final String MOD_ID = "untitled";
    public static final DataComponentType<Boolean> WAS_ON_COOLDOWN = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Untitled.MOD_ID, "was_on_cooldown"),
            DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build()
    );

    @Override
    public void onInitialize() {
        ModItems.initialize();
        LightningStick.initialize();
    }
}
