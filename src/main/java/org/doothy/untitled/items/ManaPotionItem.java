package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.network.ManaSyncHandler;
import org.jspecify.annotations.NonNull;

import java.util.function.BiFunction;

/**
 * A potion that restores Mana or applies a Mana Regen effect.
 */
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
    public @NonNull ItemStack finishUsingItem(
            @NonNull ItemStack stack,
            @NonNull Level level,
            @NonNull LivingEntity user
    ) {
        if (!level.isClientSide() && user instanceof ServerPlayer player) {

            ManaStorage mana = player.getAttachedOrCreate(ModAttachments.MANA);
            if (mana == null) return stack;

            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f
            );

            if (!isRegen) {
                mana.insertMana(50, ManaTransaction.EXECUTE);
            } else {
                player.addEffect(
                        new net.minecraft.world.effect.MobEffectInstance(
                                Untitled.MANA_REGEN,
                                600,
                                0
                        )
                );
            }

            // Sync once, centrally
            ManaSyncHandler.sync(player);

            // Handle bottle
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);

                ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) {
                    return emptyBottle;
                } else if (!player.getInventory().add(emptyBottle)) {
                    player.drop(emptyBottle, false);
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

    // ───────────────────────── REGISTRATION ─────────────────────────

    public static Item register(
            String name,
            BiFunction<Properties, Boolean, Item> factory,
            Properties settings,
            boolean isRegen
    ) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(Untitled.MOD_ID, name)
        );

        Item item = factory.apply(settings.setId(key), isRegen);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static final Item INSTANT_MANA_POTION =
            register("instant_mana_potion", ManaPotionItem::new,
                    new Item.Properties().stacksTo(16), false);

    public static final Item REGEN_MANA_POTION =
            register("regen_mana_potion", ManaPotionItem::new,
                    new Item.Properties().stacksTo(16), true);

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS)
                .register(entries -> entries.accept(INSTANT_MANA_POTION));
    }
}
