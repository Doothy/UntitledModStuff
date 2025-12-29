package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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
import org.doothy.untitled.api.mana.ManaStorage;
import org.doothy.untitled.api.mana.ManaTransaction;
import org.doothy.untitled.attachment.ModAttachments;
import org.doothy.untitled.effects.AbilityHelper;
import org.doothy.untitled.effects.ParticleHelper;
import org.doothy.untitled.network.ManaSyncHandler;
import org.doothy.untitled.sound.ModSounds;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

import static org.doothy.untitled.Untitled.WAS_ON_COOLDOWN;

/**
 * A powerful staff that channels lightning.
 * <p>
 * Features:
 * <ul>
 *     <li>Shields the user while charging.</li>
 *     <li>Unleashes a thunderclap and chain lightning on release.</li>
 *     <li>Consumes Mana.</li>
 * </ul>
 */
public class LightningStick extends Item {

    public static final int MAX_WINDUP_TICKS = 72000;
    public static final int REQUIRED_WINDUP = 20;
    public static final int SHIELD_DURATION_TICKS = 100;
    public static final double TARGET_REACH = 25.0D;
    private static final int MANA_COST = 20;

    public LightningStick(Properties settings) {
        super(settings);
    }

    // ───────────────────────── INVENTORY TICK ─────────────────────────

    @Override
    public void inventoryTick(
            ItemStack stack,
            ServerLevel level,
            Entity entity,
            EquipmentSlot slot
    ) {
        if (!(entity instanceof Player player)) return;
        boolean isOnCooldown = player.getCooldowns().isOnCooldown(stack);
        boolean wasOnCooldown = stack.getOrDefault(WAS_ON_COOLDOWN, false);

        if (player.isUsingItem() && player.getUseItem() == stack) {
            int ticksUsed =
                    getUseDuration(stack, player) -
                            player.getUseItemRemainingTicks();

            if (ticksUsed >= REQUIRED_WINDUP && ticksUsed % 4 == 0) {
                Vec3 look = player.getLookAngle();
                Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

                Vec3 handPos = player.getEyePosition()
                        .add(look.scale(0.6))
                        .add(right.scale(0.4))
                        .add(0, -0.2, 0);

                double jitter = 0.05;
                level.sendParticles(
                        ParticleTypes.SMALL_FLAME,
                        handPos.x,
                        handPos.y,
                        handPos.z,
                        1,
                        jitter,
                        jitter,
                        jitter,
                        0.01
                );
            }

            HitResult hit = player.pick(TARGET_REACH, 0.0F, false);
            if (hit.getType() != HitResult.Type.MISS) {
                ParticleHelper.spawnTargetingSparks(level, hit.getLocation());
            }

            if (ticksUsed < SHIELD_DURATION_TICKS) {
                ParticleHelper.spawnShieldRing(level, player, ticksUsed);

                AABB area = player.getBoundingBox().inflate(1.5);
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        e -> e != player && e.isAlive()
                ).forEach(enemy -> {
                    Vec3 push = enemy.position()
                            .subtract(player.position())
                            .normalize()
                            .multiply(0.3, 0, 0.3);
                    enemy.push(push.x, 0.2, push.z);
                    if (ticksUsed % 10 == 0) {
                        enemy.hurt(level.damageSources().lightningBolt(), 1.0f);
                    }
                    ParticleHelper.spawnShieldHit(level, enemy);
                });
            } else if (ticksUsed == SHIELD_DURATION_TICKS) {
                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS,
                        1f,
                        1f
                );
                level.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS,
                        1f,
                        2f
                );
                ParticleHelper.spawnShieldFizzle(level, player);
            }
        }

        if (wasOnCooldown != isOnCooldown) {
            stack.set(WAS_ON_COOLDOWN, isOnCooldown);
        }
    }

    // ───────────────────────── USE START ─────────────────────────

    @Override
    public @NotNull InteractionResult use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;

        ManaStorage mana = player.getAttachedOrCreate(ModAttachments.MANA);
        if (mana.extractMana(MANA_COST, ManaTransaction.SIMULATE) < MANA_COST) {
            return InteractionResult.FAIL;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    // ───────────────────────── RELEASE ─────────────────────────

    @Override
    public boolean releaseUsing(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int remainingUseTicks
    ) {
        if (!(entity instanceof ServerPlayer player)) return false;

        int elapsed = getUseDuration(stack, entity) - remainingUseTicks;
        if (elapsed < REQUIRED_WINDUP || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        HitResult hit = player.pick(TARGET_REACH, 0.0F, false);
        if (hit.getType() == HitResult.Type.MISS) return false;

        ManaStorage mana = player.getAttachedOrCreate(ModAttachments.MANA);
        if (mana.extractMana(MANA_COST, ManaTransaction.EXECUTE) < MANA_COST) {
            return false;
        }

        ManaSyncHandler.sync(player);

        Vec3 pos = hit.getLocation();

        AbilityHelper.applyThunderClap(serverLevel, player, pos);
        ParticleHelper.spawnDramaticBolt(serverLevel, pos);
        ParticleHelper.spawnShockwave(serverLevel, pos);

        serverLevel.playSound(
                null,
                BlockPos.containing(pos),
                ModSounds.THUNDER_HIT,
                SoundSource.WEATHER,
                10.0f,
                1.0f
        );

        AbilityHelper.performSustainedChain(serverLevel, player, pos);

        Vec3 look = player.getLookAngle();
        player.push(-look.x * 0.5, 0.1, -look.z * 0.5);
        player.hurtMarked = true;

        player.getCooldowns().addCooldown(stack, 30);
        return true;
    }

    // ───────────────────────── ITEM BEHAVIOR ─────────────────────────

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return MAX_WINDUP_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !stack.getOrDefault(WAS_ON_COOLDOWN, false);
    }

    // ───────────────────────── REGISTRATION ─────────────────────────

    public static Item register(
            String name,
            Function<Item.Properties, Item> factory,
            Item.Properties settings
    ) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(Untitled.MOD_ID, name)
        );
        Item item = factory.apply(settings.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static final Item LIGHTNING_STICK = register(
            "lightning_stick",
            LightningStick::new,
            new Item.Properties().component(
                    DataComponents.LORE,
                    new ItemLore(List.of(
                            Component.literal("Sustained Cataclysm")
                                    .withStyle(net.minecraft.ChatFormatting.GOLD),
                            Component.literal("Shields user & chains targets")
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW),
                            Component.literal("Cost: 20 Mana")
                                    .withStyle(net.minecraft.ChatFormatting.AQUA)
                    ))
            )
    );

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(LIGHTNING_STICK));
    }
}
