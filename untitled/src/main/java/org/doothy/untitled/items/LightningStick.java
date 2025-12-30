package org.doothy.untitled.items;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.combat.RaycastTargeting;
import org.doothy.untitled.effects.ChargeTickEffect;
import org.doothy.untitled.effects.EffectContext;
import org.doothy.untitled.effects.combat.ShieldDuringChargeEffect;
import org.doothy.untitled.effects.registry.ItemEffects;
import org.doothy.untitled.effects.policy.AttachmentManaCostPolicy;
import org.doothy.untitled.effects.policy.SimpleItemCooldownPolicy;
import org.doothy.untitled.network.payload.LightningVisualPayload;

public class LightningStick extends AbstractMagicItem {

    private static final int MAX_USE_TICKS = 72000;
    private static final int REQUIRED_CHARGE = 20;
    private static final int SHIELD_DURATION_TICKS = 100;
    private static final double TARGET_REACH = 25.0D;

    private static final double SHIELD_RADIUS = 1.5;

    private static final ChargeTickEffect SHIELD_EFFECT =
            new ShieldDuringChargeEffect(100, 1.5);

    public LightningStick(Properties properties) {
        super(
                properties,
                new SimpleItemCooldownPolicy(30),
                new AttachmentManaCostPolicy(20)
        );
    }

    /* ───────────────────────── START USING ───────────────────────── */

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            if (!manaPolicy.hasMana(player)) return InteractionResult.FAIL;
            if (!cooldownPolicy.canActivate(player, stack)) return InteractionResult.FAIL;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /* ───────────────────────── CHARGING SHIELD ───────────────────────── */

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int elapsed = getUseDuration(stack, user) - remaining;
        SHIELD_EFFECT.onChargeTick(level, user, stack, elapsed);
    }

    /* ───────────────────────── RELEASE ───────────────────────── */

    @Override
    public boolean releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int timeLeft
    ) {
        if (!(entity instanceof Player player)) return false;
        if (!(level instanceof ServerLevel serverLevel)) return false;

        int elapsed = getUseDuration(stack, player) - timeLeft;
        if (elapsed < REQUIRED_CHARGE) return false;

        float charge = Math.min(1.0f, elapsed / 40.0f);
        Vec3 hitPos = RaycastTargeting.raycastPosition(player, TARGET_REACH);
        if (hitPos == null) return false;

        if (!manaPolicy.hasMana(player)) return false;
        if (!cooldownPolicy.canActivate(player, stack)) return false;

        manaPolicy.consume(player);
        cooldownPolicy.applyCooldown(player, stack);

        // 🔥 THIS is now the only effect on release
        EffectContext ctx = new EffectContext(serverLevel, player, hitPos, charge);

        ItemEffects.LIGHTNING_STRIKE.apply(ctx);
        ItemEffects.LIGHTNING_SHOCKWAVE.apply(ctx);
        ItemEffects.CHAIN_LIGHTNING.apply(ctx);
        ItemEffects.LIGHTNING_SOUND.apply(ctx);


        // Small recoil (kept from old behavior)
        Vec3 look = player.getLookAngle();
        player.push(-look.x * 0.5, 0.1, -look.z * 0.5);
        player.hurtMarked = true;

        ServerPlayNetworking.send(
                (ServerPlayer) player,
                new LightningVisualPayload(hitPos, charge)
        );


        return true;
    }

    /* ───────────────────────── USE PROPERTIES ───────────────────────── */

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return MAX_USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }
}
