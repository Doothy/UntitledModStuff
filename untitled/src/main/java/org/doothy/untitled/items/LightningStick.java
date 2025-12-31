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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.attachment.ShieldAttachment;
import org.doothy.untitled.combat.RaycastTargeting;
import org.doothy.untitled.effects.ChargeTickEffect;
import org.doothy.untitled.effects.EffectContext;
import org.doothy.untitled.effects.combat.ShieldDuringChargeEffect;
import org.doothy.untitled.effects.policy.AttachmentManaCostPolicy;
import org.doothy.untitled.effects.policy.SimpleItemCooldownPolicy;
import org.doothy.untitled.effects.registry.ItemEffects;
import org.doothy.untitled.network.payload.LightningVisualPayload;
import org.doothy.untitled.network.payload.ShieldPayload;

/**
 * Magic item that fires a lightning strike after a short charge-up.
 * While charging, a temporary shield effect can be applied to the player.
 *
 * Behavior overview:
 * - Start using to begin charging and apply a short-lived shield aura.
 * - Releasing after the minimum charge consumes mana, applies cooldown, and executes lightning effects.
 * - Client visuals and slight recoil are applied on successful release.
 */
public class LightningStick extends AbstractMagicItem {

    private static final int MAX_USE_TICKS = 72000;
    private static final int REQUIRED_CHARGE = 20;
    private static final int SHIELD_DURATION_TICKS = 100;

    public static final double TARGET_REACH = 25.0D;
    private static final double SHIELD_RADIUS = 1.5;

    private static final ChargeTickEffect SHIELD_EFFECT =
            new ShieldDuringChargeEffect(SHIELD_DURATION_TICKS, SHIELD_RADIUS);

    public LightningStick(Properties properties) {
        super(
                properties,
                new SimpleItemCooldownPolicy(30),
                new AttachmentManaCostPolicy(20)
        );
    }

    @Override
    /**
     * Starts using the item, initiating the charge phase if mana and cooldown permit.
     *
     * @param level  world the use occurs in
     * @param player player attempting to use the item
     * @param hand   hand holding the item
     * @return interaction result indicating whether the use is consumed/failed
     */
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Ensure a fresh state for this use attempt even if a previous charge was cancelled
        stack.remove(Untitled.SHIELD_USED_THIS_USE);

        if (!level.isClientSide()) {
            if (!manaPolicy.hasMana(player)) return InteractionResult.FAIL;
            if (!cooldownPolicy.canActivate(player, stack)) return InteractionResult.FAIL;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    /**
     * Called each tick while the item is being used (charged). Applies the shield effect
     * for a limited duration around the user while charging progresses.
     *
     * @param level     current level
     * @param user      entity using the item
     * @param stack     item stack used
     * @param remaining remaining ticks before max use duration
     */
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int elapsed = getUseDuration(stack, user) - remaining;
        SHIELD_EFFECT.onChargeTick(level, user, stack, elapsed);
    }

    @Override
    /**
     * Releases the item after charging. If the minimum charge threshold is met, consumes mana
     * and applies cooldown, then executes the lightning effects at the targeted location.
     *
     * Complex part: shield lifecycle and target acquisition
     * - Any active charge shield is explicitly stopped when use ends.
     * - Target is determined via a reach-limited raycast from the player's view.
     *
     * @param stack    the item stack
     * @param level    the level in which the action occurs
     * @param entity   the entity releasing the use
     * @param timeLeft ticks remaining from max duration when released
     * @return true if an action occurred on release, false otherwise
     */
    public boolean releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int timeLeft
    ) {
        stack.remove(Untitled.SHIELD_USED_THIS_USE);

        if (!(entity instanceof Player player)) return false;
        if (!(level instanceof ServerLevel serverLevel)) return false;

        int elapsed = getUseDuration(stack, player) - timeLeft;
        // Explicitly terminate any charge-time shield when use ends
        ShieldAttachment shield =
                player.getAttachedOrCreate(ModAttachments.LIGHTNING_SHIELD);

        if (shield.ticks() > 0) {
            shield.setTicks(0);

            ServerPlayNetworking.send(
                    (ServerPlayer) player,
                    new ShieldPayload(0)
            );
        }
        // Not enough charge accumulated: abort without effects
        if (elapsed < REQUIRED_CHARGE) {
            return false;
        }

        // Compute normalized charge [0..1] and acquire a target via reach-limited raycast
        float charge = Math.min(1.0f, elapsed / 40.0f);
        HitResult hit = RaycastTargeting.raycast(player, TARGET_REACH);
        if (hit == null) return false;

        Vec3 hitPos = hit.getLocation();

        if (!manaPolicy.hasMana(player)) return false;
        if (!cooldownPolicy.canActivate(player, stack)) return false;

        manaPolicy.consume(player);
        cooldownPolicy.applyCooldown(player, stack);
        // Execute the lightning effect suite at the target location
        EffectContext ctx = new EffectContext(serverLevel, player, hitPos, charge);

        ItemEffects.LIGHTNING_STRIKE.apply(ctx);
        ItemEffects.LIGHTNING_SHOCKWAVE.apply(ctx);
        ItemEffects.CHAIN_LIGHTNING.apply(ctx);
        ItemEffects.LIGHTNING_SOUND.apply(ctx);
        // Apply mild recoil to the player for tactile feedback
        Vec3 look = player.getLookAngle();
        player.push(-look.x * 0.5, 0.1, -look.z * 0.5);
        player.hurtMarked = true;
        // Send client visuals payload
        ServerPlayNetworking.send(
                (ServerPlayer) player,
                new LightningVisualPayload(hitPos, charge)
        );

        return true;
    }

    @Override
    /**
     * Returns the maximum use (charge) duration in ticks.
     *
     * @param stack stack being used
     * @param user  entity using the item
     * @return maximum use duration
     */
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return MAX_USE_TICKS;
    }

    @Override
    /**
     * Animation to display while using (charging) this item.
     *
     * @param stack the item stack
     * @return the use animation
     */
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    /**
     * Delegates right-click on block to the standard use behavior.
     *
     * @param context the use-on context provided by Minecraft
     * @return interaction result of starting to use the item
     */
    public InteractionResult useOn(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand());
    }
}
