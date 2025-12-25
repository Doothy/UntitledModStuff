package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.network.ManaPayload;
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

        if (wasOnCooldown && !isOnCooldown) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5f, 2.0f);
        }

        if (!isOnCooldown) {
            if (level.random.nextFloat() < 0.15f) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        1, 0.2, 0.2, 0.2, 0.02);
            }
        }

        if (wasOnCooldown != isOnCooldown) {
            stack.set(WAS_ON_COOLDOWN, isOnCooldown);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !stack.getOrDefault(WAS_ON_COOLDOWN, false);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level world, @NotNull Player user, @NotNull InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // --- MANA SYSTEM INTEGRATION ---
        // 1. Get the player's Mana Attachment
        ManaAttachment mana = user.getAttachedOrCreate(ModAttachments.MANA, () -> new ManaAttachment(100, 100));

        // 2. Define cost (e.g., 20 mana per bolt)
        int manaCost = 20;

        // 3. Check if player has enough mana
        if (mana.getMana() < manaCost) {
            user.displayClientMessage(net.minecraft.network.chat.Component.literal("Not enough Mana!")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        HitResult hit = user.pick(20.0D, 0.0F, false);

        if (hit.getType() != HitResult.Type.MISS) {
            // 4. Consume Mana and update the Attachment
            mana.setMana(mana.getMana() - manaCost);
            user.setAttached(ModAttachments.MANA, mana);

            // 5. Sync the updated mana to the client
            ServerPlayNetworking.send((ServerPlayer) user, new ManaPayload(mana.getMana(), mana.getMaxMana()));

            // 6. Spawn Lightning
            BlockPos strikePos = BlockPos.containing(hit.getLocation());
            EntityType.LIGHTNING_BOLT.spawn((ServerLevel) world, strikePos, EntitySpawnReason.TRIGGERED);

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
            // Removed .durability() since we are using Mana now
            .component(DataComponents.LORE, new ItemLore(java.util.List.of(
                    net.minecraft.network.chat.Component.literal("Right-click to summon lightning!")
                            .withStyle(net.minecraft.ChatFormatting.GOLD),
                    net.minecraft.network.chat.Component.literal("Cost: 20 Mana")
                            .withStyle(net.minecraft.ChatFormatting.AQUA),
                    net.minecraft.network.chat.Component.literal("Cooldown: 1 second")
                            .withStyle(net.minecraft.ChatFormatting.GRAY)
            )))
    );

    public static void initialize(){
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register((itemGroup) -> itemGroup.accept(LightningStick.LIGHTNING_STICK));
    }
}