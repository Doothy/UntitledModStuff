package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.doothy.untitled.Untitled;
import org.doothy.untitled.attachment.ManaAttachment;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.effects.AbilityHelper;
import org.doothy.untitled.effects.ParticleHelper;
import org.doothy.untitled.network.ManaPayload;
import org.doothy.untitled.sound.ModSounds;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

import static org.doothy.untitled.Untitled.WAS_ON_COOLDOWN;

/**
 * A magical item that summons lightning and shields the user.
 * <p>
 * Core Mechanics:
 * <ul>
 *     <li><b>Charging:</b> Right-click initiates a windup. Requires 20 Mana.</li>
 *     <li><b>Targeting:</b> Uses raycasting to find the exact block the player is looking at.</li>
 *     <li><b>Shielding:</b> Passive 1.5-block radius "repel" zone while charging (Max 5s).</li>
 *     <li><b>Strike:</b> On release, summons a dramatic vertical bolt and triggers a Chain Lightning event.</li>
 *     <li><b>Chain Lightning:</b> Jumps between 12 entities with a 15% damage decay per jump.</li>
 * </ul>
 */
public class LightningStick extends Item {

    public static final int MAX_WINDUP_TICKS = 72000;
    public static final int REQUIRED_WINDUP = 20;
    public static final int SHIELD_DURATION_TICKS = 100; // 5 Seconds
    public static final double TARGET_REACH = 25.0D;

    public LightningStick(Properties settings) {
        super(settings);
    }

    /**
     * Handles real-time visual feedback and the defensive shield.
     * Includes logic to fizzle the shield after 5 seconds.
     */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;

        boolean isOnCooldown = player.getCooldowns().isOnCooldown(stack);
        boolean wasOnCooldown = stack.getOrDefault(WAS_ON_COOLDOWN, false);

        if (player.isUsingItem() && player.getUseItem() == stack) {

            // --- 1. CALCULATE DURATION ---
            int maxUse = getUseDuration(stack, player);
            int remaining = player.getUseItemRemainingTicks();
            int ticksUsed = maxUse - remaining;

            // --- 2. FULL CHARGE EFFECTS (UPDATED) ---
            if (ticksUsed >= REQUIRED_WINDUP) {
                // MATH: Calculate "Right Hand" position relative to camera
                Vec3 look = player.getLookAngle();
                Vec3 up = new Vec3(0, 1, 0);
                // Cross product gives us a "Right" vector (perpendicular to look)
                Vec3 right = look.cross(up).normalize();

                // Offset: 0.6 forward, 0.4 to the right, 0.2 down
                Vec3 handPos = player.getEyePosition()
                        .add(look.scale(0.6))
                        .add(right.scale(0.4))
                        .add(0, -0.2, 0);

                // Add jitter
                double jitter = 0.05;
                double jX = (player.getRandom().nextDouble() - 0.5) * jitter;
                double jY = (player.getRandom().nextDouble() - 0.5) * jitter;
                double jZ = (player.getRandom().nextDouble() - 0.5) * jitter;

                // EFFECT: Custom Colored Energy
                if (ticksUsed % 4 == 0) {

                    float r = 1.0f; // R
                    float g = 0.9f; // G
                    float b = 0.0f; // B
                    float size = 1.0f;
                    int color = ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);

                    var coloredDust = new net.minecraft.core.particles.DustParticleOptions(color, size);

                    level.sendParticles(coloredDust,
                            handPos.x + jX, handPos.y + jY, handPos.z + jZ,
                            1, 0, 0, 0, 0.01);

                    // Optional: Keep the small flame for contrast
                    level.sendParticles(ParticleTypes.SMALL_FLAME,
                            handPos.x + jX, handPos.y + jY, handPos.z + jZ,
                            1, 0, 0, 0, 0.005);
                }
            }

            // --- 3. TARGETING ---
            HitResult hit = player.pick(TARGET_REACH, 0.0F, false);
            if (hit.getType() != HitResult.Type.MISS) {
                ParticleHelper.spawnTargetingSparks(level, hit.getLocation());
            }

            /*
             * DEFENSIVE SHIELD (TIMED)
             */
            if (ticksUsed < SHIELD_DURATION_TICKS) {
                ParticleHelper.spawnShieldRing(level, player, ticksUsed);

                AABB shieldArea = player.getBoundingBox().inflate(1.5);
                level.getEntitiesOfClass(LivingEntity.class, shieldArea, e -> e != player && e.isAlive()).forEach(enemy -> {
                    Vec3 pushDir = enemy.position().subtract(player.position()).normalize().multiply(0.3, 0, 0.3);
                    enemy.push(pushDir.x, 0.2, pushDir.z);
                    if(ticksUsed % 10 == 0) enemy.hurt(level.damageSources().lightningBolt(), 1.0f);
                    ParticleHelper.spawnShieldHit(level, enemy);
                });

            } else if (ticksUsed == SHIELD_DURATION_TICKS) {
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 2.0f);
                ParticleHelper.spawnShieldFizzle(level, player);
            }
        }

        if (wasOnCooldown != isOnCooldown) {
            stack.set(WAS_ON_COOLDOWN, isOnCooldown);
        }
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level world, @NotNull Player user, @NotNull InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;

        ManaAttachment mana = user.getAttachedOrCreate(ModAttachments.MANA, () -> new ManaAttachment(100, 100));
        if (mana.getMana() < 20) return InteractionResult.FAIL;

        if (world.isClientSide()) {
            LightningSoundHelper.Holder.INSTANCE.start();
        }

        user.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof Player player)) return false;
        int elapsed = getUseDuration(stack, user) - remainingUseTicks;

        if (world.isClientSide()) {
            LightningSoundHelper.Holder.INSTANCE.stop();
        }

        if (elapsed >= REQUIRED_WINDUP && world instanceof ServerLevel serverLevel) {
            HitResult hit = player.pick(TARGET_REACH, 0.0F, false);

            if (hit.getType() != HitResult.Type.MISS) {
                Vec3 pos = hit.getLocation();

                ManaAttachment mana = player.getAttachedOrCreate(ModAttachments.MANA, () -> new ManaAttachment(100, 100));
                mana.setMana(mana.getMana() - 20);
                player.setAttached(ModAttachments.MANA, mana);
                ServerPlayNetworking.send((ServerPlayer) player, new ManaPayload(mana.getMana(), mana.getMaxMana()));

                AbilityHelper.applyThunderClap(serverLevel, player, pos);
                ParticleHelper.spawnDramaticBolt(serverLevel, pos);
                ParticleHelper.spawnShockwave(serverLevel, pos);

                world.playSound(null, BlockPos.containing(pos), ModSounds.THUNDER_HIT, SoundSource.WEATHER, 10.0f, 1.0f);

                AbilityHelper.performSustainedChain(serverLevel, player, pos);

                Vec3 look = player.getLookAngle();
                player.push(-look.x * 0.5, 0.1, -look.z * 0.5);
                player.hurtMarked = true;

                player.getCooldowns().addCooldown(stack, 30);
            }
            return true;
        }
        return false;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) { return MAX_WINDUP_TICKS; }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }

    @Override
    public boolean isFoil(ItemStack stack) { return !stack.getOrDefault(WAS_ON_COOLDOWN, false); }

    public static Item register(String name, Function<Item.Properties, Item> itemFactory, Item.Properties settings){
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Untitled.MOD_ID, name));
        Item item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }

    public static final Item LIGHTNING_STICK = register("lightning_stick", LightningStick::new, new Item.Properties()
            .component(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("Sustained Cataclysm").withStyle(net.minecraft.ChatFormatting.GOLD),
                    Component.literal("Shields user & chains targets").withStyle(net.minecraft.ChatFormatting.YELLOW),
                    Component.literal("Cost: 20 Mana").withStyle(net.minecraft.ChatFormatting.AQUA)
            )))
    );

    public static void initialize(){
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register((itemGroup) -> itemGroup.accept(LightningStick.LIGHTNING_STICK));
    }
}