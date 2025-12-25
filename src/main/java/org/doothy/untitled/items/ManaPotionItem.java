package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.network.ManaPayload;
import org.jspecify.annotations.NonNull;

import java.util.function.BiFunction;

public class ManaPotionItem extends Item {
    private final boolean isRegen;

    public ManaPotionItem(Properties properties, boolean isRegen) {
        super(properties);
        this.isRegen = isRegen;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public @NonNull ItemStack finishUsingItem(@NonNull ItemStack stack, @NonNull Level level, @NonNull LivingEntity user) {
        if (!level.isClientSide() && user instanceof ServerPlayer player) {
            ManaAttachment mana = player.getAttached(ModAttachments.MANA);

            // Play the sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

            if (mana != null) {
                // Logic for instant vs regen
                if (!isRegen) {
                    mana.setMana(Math.min(mana.getMaxMana(), mana.getMana() + 50));
                } else {
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(Untitled.MANA_REGEN, 600, 0));
                }

                // Sync
                player.setAttached(ModAttachments.MANA, mana);
                ServerPlayNetworking.send(player, new ManaPayload(mana.getMana(), mana.getMaxMana()));

                // Handle the Stack and the Empty Bottle
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);

                    ItemStack emptyBottle = new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                    if (stack.isEmpty()) {
                        return emptyBottle; // Stack is gone, just return the bottle
                    } else {
                        // Stack still has potions, put the bottle in the inventory
                        if (!player.getInventory().add(emptyBottle)) {
                            player.drop(emptyBottle, false); // Inventory full? Drop it on the ground
                        }
                    }
                }
            }
        }
        return stack;
    }

    @Override
    public @NonNull ItemUseAnimation getUseAnimation(@NonNull ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(@NonNull ItemStack stack, @NonNull LivingEntity entity) {
        return 32;
    }

    // Register requires a BiFunction due to the additional boolean parameter
    public static Item register(String name, BiFunction<Properties, Boolean, Item> itemFactory, Item.Properties settings, boolean isRegen) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Untitled.MOD_ID, name));

        // Pass both settings AND the isRegen boolean to the factory
        Item item = itemFactory.apply(settings.setId(itemKey), isRegen);

        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    public static final Item INSTANT_MANA_POTION = register("instant_mana_potion",
            ManaPotionItem::new, new Item.Properties().stacksTo(16), false);

    public static final Item REGEN_MANA_POTION = register("regen_mana_potion",
            ManaPotionItem::new, new Item.Properties().stacksTo(16), true);

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS)
                .register((itemGroup) -> itemGroup.accept(ManaPotionItem.INSTANT_MANA_POTION));
    }
}