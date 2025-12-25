package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.doothy.untitled.Untitled;
import org.jetbrains.annotations.NotNull;
import java.util.function.Function;

import static org.doothy.untitled.Untitled.WAS_ON_COOLDOWN;

public class LightningStick extends Item {

    public LightningStick(Properties settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;

        boolean isOnCooldown = player.getCooldowns().isOnCooldown(stack);
        boolean wasOnCooldown = stack.getOrDefault(WAS_ON_COOLDOWN, false);

        // 1. Play sound when ready
        if (wasOnCooldown && !isOnCooldown) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5f, 2.0f);
        }

        // 2. Add Sparkle effect only when the Blaze Rod is visible (NOT on cooldown)
        if (!isOnCooldown) {
            // Only spawn particles occasionally so it's not too laggy
            if (level.random.nextFloat() < 0.15f) {
                // Spawn an electric spark slightly above the player
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        1, 0.2, 0.2, 0.2, 0.02);
            }
        }

        // 3. Keep the component in sync with the cooldown tracker
        if (wasOnCooldown != isOnCooldown) {
            stack.set(WAS_ON_COOLDOWN, isOnCooldown);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // The stick glows when NOT on cooldown
        return !stack.getOrDefault(WAS_ON_COOLDOWN, false);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level world, @NotNull Player user, @NotNull InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        // Prevents using the item if the player is on cooldown
        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        HitResult hit = user.pick(20.0D, 0.0F, false);

        if (hit.getType() != HitResult.Type.MISS) {
            BlockPos strikePos = BlockPos.containing(hit.getLocation());
            EntityType.LIGHTNING_BOLT.spawn((ServerLevel) world, strikePos, EntitySpawnReason.TRIGGERED);

            // Durability loss
            stack.hurtAndBreak(1, (ServerLevel) world, (ServerPlayer) user, (item) -> {
                user.onEquippedItemBroken(item, user.getEquipmentSlotForItem(stack));
            });

            // Apply global player cooldown
            user.getCooldowns().addCooldown(stack, 20);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public static Item register(String name,
                                Function<Item.Properties, Item> itemFactory,
                                Item.Properties settings){
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Untitled.MOD_ID, name));

        Item item = itemFactory.apply(settings.setId(itemKey));

        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static final Item LIGHTNING_STICK = register("lightning_stick", LightningStick::new, new Item.Properties()
            .durability(64)
            // Correct 1.21 way to add Lore via Properties
            .component(DataComponents.LORE, new ItemLore(java.util.List.of(
                    net.minecraft.network.chat.Component.literal("Right-click to summon lightning!")
                            .withStyle(net.minecraft.ChatFormatting.GOLD),
                    net.minecraft.network.chat.Component.literal("Cooldown: 1 second")
                            .withStyle(net.minecraft.ChatFormatting.GRAY)
            )))
    );

    public static void initialize(){
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register((itemGroup) -> itemGroup.accept(LightningStick.LIGHTNING_STICK));
    }

}
