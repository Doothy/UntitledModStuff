package org.doothy.untitled.items;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.network.ManaPayload;

public class ManaBatteryItem extends Item {

    private final int maxCapacity;
    private final int transferRate; // Mana per tick

    public ManaBatteryItem(Properties properties, int maxCapacity, int transferRate) {
        super(properties.stacksTo(1)); // Batteries shouldn't stack
        this.maxCapacity = maxCapacity;
        this.transferRate = transferRate;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        int currentStored = stack.getOrDefault(Untitled.STORED_MANA, 0);

        if (currentStored >= getMaxCapacity()) {
            return InteractionResult.FAIL;
        }

        player.startUsingItem(usedHand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(livingEntity instanceof ServerPlayer player)) return;

        // 1. Get Item Storage State
        int currentStored = stack.getOrDefault(Untitled.STORED_MANA, 0);
        if (currentStored >= maxCapacity) {
            player.stopUsingItem();
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 2.0f);
            return;
        }

        // 2. Get Player Mana State
        ManaAttachment mana = player.getAttached(ModAttachments.MANA);
        if (mana == null || mana.getMana() <= 0) {
            player.stopUsingItem(); // Stop if player is empty
            return;
        }

        // 3. Calculate Transfer
        // We can transfer the Rate, but not more than the player has, and not more than fits in the battery
        int spaceInBattery = maxCapacity - currentStored;
        int amountToTransfer = Math.min(transferRate, Math.min(mana.getMana(), spaceInBattery));

        if (amountToTransfer > 0) {
            // Update Player
            mana.setMana(mana.getMana() - amountToTransfer);
            player.setAttached(ModAttachments.MANA, mana);

            // SYNC PLAYER MANA TO CLIENT (Important!)
            ServerPlayNetworking.send(player, new ManaPayload(mana.getMana(), mana.getMaxMana()));

            // Update Item
            stack.set(Untitled.STORED_MANA, currentStored + amountToTransfer);

            // Optional: Play a quiet pitch-rising sound based on fill level
            if (remainingUseDuration % 5 == 0) {
                float pitch = 0.5f + ((float) currentStored / maxCapacity);
                level.playSound(null, player.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.4f, pitch);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // Allow holding for a long time
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW; // Looks like they are charging something
    }

    // --- VISUALS ---

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getOrDefault(Untitled.STORED_MANA, 0) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int stored = stack.getOrDefault(Untitled.STORED_MANA, 0);
        // Return value between 0 and 13 (pixel width of item)
        return Math.round(13.0f * stored / maxCapacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FFFF; // Cyan color for Mana
    }

    // In ManaBatteryItem.java
    public int getMaxCapacity() {
        return this.maxCapacity;
    }
}