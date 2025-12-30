package org.doothy.untitled.items;

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
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.network.ManaSyncHandler;

/**
 * An item that stores Mana and can be charged by the player.
 */
public class ManaBatteryItem extends Item {

    private final int maxCapacity;
    private final int transferRate; // Mana per tick

    public ManaBatteryItem(Properties properties, int maxCapacity, int transferRate) {
        super(properties.stacksTo(1));
        this.maxCapacity = maxCapacity;
        this.transferRate = transferRate;
    }

    // ───────────────────────── USE START ─────────────────────────

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        int stored = stack.getOrDefault(Untitled.STORED_MANA, 0);
        if (stored >= maxCapacity) {
            return InteractionResult.PASS;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    // ───────────────────────── CHARGING ─────────────────────────

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;

        int stored = stack.getOrDefault(Untitled.STORED_MANA, 0);
        if (stored >= maxCapacity) {
            player.stopUsingItem();
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(),
                    SoundSource.PLAYERS,
                    1.0f,
                    2.0f
            );
            return;
        }

        ManaStorage mana = player.getAttachedOrCreate(ModAttachments.MANA);
        if (mana == null) {
            player.stopUsingItem();
            return;
        }

        int spaceInBattery = maxCapacity - stored;
        int maxTransfer = Math.min(transferRate, spaceInBattery);

        // Simulate extraction first
        long available = mana.extractMana(maxTransfer, ManaTransaction.SIMULATE);
        if (available <= 0) {
            player.stopUsingItem();
            return;
        }

        // Execute transfer
        mana.extractMana(available, ManaTransaction.EXECUTE);
        stack.set(Untitled.STORED_MANA, stored + (int) available);

        ManaSyncHandler.sync(player);

        // Feedback sound
        if (remainingUseDuration % 5 == 0) {
            float pitch = 0.5f + ((float) stored / maxCapacity);
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.SCULK_CLICKING,
                    SoundSource.PLAYERS,
                    0.4f,
                    pitch
            );
        }
    }

    // ───────────────────────── ITEM BEHAVIOR ─────────────────────────

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    // ───────────────────────── VISUALS ─────────────────────────

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getOrDefault(Untitled.STORED_MANA, 0) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int stored = stack.getOrDefault(Untitled.STORED_MANA, 0);
        return Math.round(13.0f * stored / maxCapacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FFFF;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }
}
