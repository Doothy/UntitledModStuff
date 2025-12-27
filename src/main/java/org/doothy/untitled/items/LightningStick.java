package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import org.doothy.untitled.network.ManaPayload;
import org.doothy.untitled.sound.ModSounds;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static org.doothy.untitled.Untitled.WAS_ON_COOLDOWN;

/**
 * LIGHTNING STICK - TECHNICAL DOCUMENTATION
 * * CORE MECHANICS:
 * 1. CHARGING: Right-click initiates a windup. Requires 20 Mana.
 * 2. TARGETING: Uses raycasting to find the exact block the player is looking at.
 * 3. SHIELDING: Passive 1.5-block radius "repel" zone while charging (Max 5s).
 * 4. STRIKE: On release, summons a dramatic vertical bolt and triggers a Chain Lightning event.
 * 5. CHAIN LIGHTNING: Jumps between 12 entities with a 15% damage decay per jump.
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
     * inventoryTick handles real-time visual feedback and the defensive shield.
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

            /* * COMPLEX SNIPPET: RAYCASTING
             * player.pick project a line from the player's eyes 25 blocks forward.
             * This finds the intersection point (HitResult) with blocks or fluids.
             * (This runs independently of the shield timer so aiming always works)
             */
            HitResult hit = player.pick(TARGET_REACH, 0.0F, false);

            if (hit.getType() != HitResult.Type.MISS) {
                // Spawn sparks at the target point so the player knows where the bolt will land.
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        hit.getLocation().x, hit.getLocation().y + 0.1, hit.getLocation().z,
                        3, 0.2, 0.1, 0.2, 0.05);
            }

            /*
             * DEFENSIVE SHIELD (TIMED)
             * Only active for the first SHIELD_DURATION_TICKS.
             */
            if (ticksUsed < SHIELD_DURATION_TICKS) {
                // A. VISUALS: Render the Repel Zone Ring
                double radius = 2.0;
                double particleCount = 10;
                double increment = 2 * Math.PI / particleCount;
                double offset = (ticksUsed * 0.1); // Spin animation

                for (int i = 0; i < particleCount; i++) {
                    double angle = (i * increment) + offset;
                    double px = player.getX() + (Math.cos(angle) * radius);
                    double pz = player.getZ() + (Math.sin(angle) * radius);

                    // Count 0 sends particle to specific coords without velocity spread
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, player.getY(), pz, 0, 0, 0, 0, 0);
                }

                // B. MECHANICS: The Push Effect
                AABB shieldArea = player.getBoundingBox().inflate(1.5);
                level.getEntitiesOfClass(LivingEntity.class, shieldArea, e -> e != player && e.isAlive()).forEach(enemy -> {
                    Vec3 pushDir = enemy.position().subtract(player.position()).normalize().multiply(0.3, 0, 0.3);
                    enemy.push(pushDir.x, 0.2, pushDir.z);
                    enemy.hurt(level.damageSources().lightningBolt(), 1.0f);
                    level.sendParticles(ParticleTypes.SCULK_SOUL, enemy.getX(), enemy.getY() + 1, enemy.getZ(), 3, 0.1, 0.1, 0.1, 0.05);
                });

            } else if (ticksUsed == SHIELD_DURATION_TICKS) {
                // --- FIZZLE EVENT (Exact Tick) ---
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 2.0f);

                // Visual "Smoke puff" indicating shield failure
                for (int i = 0; i < 20; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = 1.5;
                    level.sendParticles(ParticleTypes.SMOKE,
                            player.getX() + Math.cos(angle) * r,
                            player.getY() + 0.5,
                            player.getZ() + Math.sin(angle) * r,
                            1, 0, 0, 0, 0.05);
                }
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

        // Check Mana via Data Attachments
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

                // Mana consumption and sync to client
                ManaAttachment mana = player.getAttachedOrCreate(ModAttachments.MANA, () -> new ManaAttachment(100, 100));
                mana.setMana(mana.getMana() - 20);
                player.setAttached(ModAttachments.MANA, mana);
                ServerPlayNetworking.send((ServerPlayer) player, new ManaPayload(mana.getMana(), mana.getMaxMana()));

                // Trigger effects
                applyThunderClap(serverLevel, player, pos);
                spawnDramaticBolt(serverLevel, pos);
                spawnShockwave(serverLevel, pos);

                world.playSound(null, BlockPos.containing(pos), ModSounds.THUNDER_HIT, SoundSource.WEATHER, 10.0f, 1.0f);

                performSustainedChain(serverLevel, player, pos);

                // Player Recoil
                Vec3 look = player.getLookAngle();
                player.push(-look.x * 0.5, 0.1, -look.z * 0.5);
                player.hurtMarked = true;

                player.getCooldowns().addCooldown(stack, 30);
            }
            return true;
        }
        return false;
    }

    /**
     * COMPLEX SNIPPET: EXPLOSIVE KNOCKBACK
     * Projects entities away from the impact point based on their distance.
     */
    private void applyThunderClap(ServerLevel level, Player source, Vec3 pos) {
        double radius = 6.0;
        AABB area = new AABB(pos.x - radius, pos.y - 2, pos.z - radius, pos.x + radius, pos.y + 4, pos.z + radius);

        level.getEntitiesOfClass(LivingEntity.class, area, e -> e != source && e.isAlive()).forEach(target -> {
            Vec3 dir = target.position().subtract(pos).normalize();
            double dist = target.position().distanceTo(pos);
            // Linear strength decay: 1.2 at center, 0 at the edge of 6 blocks.
            double strength = 1.2 * (1.0 - (dist / radius));

            target.push(dir.x * strength, 0.4, dir.z * strength);
            target.hurtMarked = true;
        });
    }

    /**
     * COMPLEX SNIPPET: CHAIN LIGHTNING ALGORITHM
     * Iteratively finds the closest target that hasn't been struck yet.
     */
    private void performSustainedChain(ServerLevel level, Player source, Vec3 startPos) {
        Vec3 currentSource = startPos;
        List<Entity> struckEntities = new ArrayList<>();
        int maxJumps = 12;
        double jumpRange = 12.0;
        float baseDamage = 10.0f;
        float decayRate = 0.85f;

        for (int i = 0; i < maxJumps; i++) {
            Vec3 finalSource = currentSource;
            float currentDamage = baseDamage * (float) Math.pow(decayRate, i);

            // Find closest living entity within range that isn't the player or already hit
            LivingEntity target = level.getEntitiesOfClass(LivingEntity.class,
                            new AABB(currentSource.x - jumpRange, currentSource.y - jumpRange, currentSource.z - jumpRange,
                                    currentSource.x + jumpRange, currentSource.y + jumpRange, currentSource.z + jumpRange),
                            e -> e != source && e.isAlive() && e.distanceToSqr(finalSource) <= (jumpRange * jumpRange) &&
                                    (!struckEntities.contains(e) || struckEntities.size() > 3))
                    .stream()
                    .filter(e -> !e.equals(source))
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(finalSource)))
                    .orElse(null);

            if (target == null) break;

            // FIX: Set fire BEFORE damage.
            // If the damage kills the entity, it must already be on fire for loot tables to drop cooked meat.
            target.setRemainingFireTicks(60);
            target.hurt(level.damageSources().lightningBolt(), currentDamage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 4));

            struckEntities.add(target);

            // Visual link between jumps
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);
            spawnJumpParticles(level, currentSource, targetCenter);

            level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.8f, 1.2f + (i * 0.05f));

            currentSource = targetCenter;
        }
    }

    /**
     * Renders a line of particles between two points.
     */
    private void spawnJumpParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double distance = start.distanceTo(end);
        int points = (int) (distance * 8); // 8 particles per block for a solid line look
        java.util.Random random = new java.util.Random();

        for (int i = 0; i <= points; i++) {
            double pct = (double) i / points;
            // Add slight jitter to the lightning bolt line
            double jX = (random.nextDouble() - 0.5) * 0.7;
            double jY = (random.nextDouble() - 0.5) * 0.7;
            double jZ = (random.nextDouble() - 0.5) * 0.7;

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    start.x + (diff.x * pct) + jX,
                    start.y + (diff.y * pct) + jY,
                    start.z + (diff.z * pct) + jZ,
                    1, 0, 0, 0, 0);
        }
    }

    /**
     * SUMMONS VERTICAL BOLT
     * Creates a zig-zagging vertical line of flash particles from the sky to the ground.
     */
    private void spawnDramaticBolt(ServerLevel level, Vec3 pos) {
        double curX = pos.x;
        double curZ = pos.z;
        java.util.Random random = new java.util.Random();
        ColorParticleOption flashWhite = ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF);

        // Sky-to-ground vertical effect
        for (double y = pos.y; y < pos.y + 55; y += 0.6) {
            curX += (random.nextDouble() - 0.5) * 0.6;
            curZ += (random.nextDouble() - 0.5) * 0.6;
            level.sendParticles(flashWhite, curX, y, curZ, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Creates an expanding ring of particles at the target position on impact.
     */
    private void spawnShockwave(ServerLevel level, Vec3 pos) {
        // Circular ground shockwave using Gust particles
        for (int i = 0; i < 64; i++) {
            double angle = Math.toRadians(i * 5.625);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                    pos.x, pos.y + 0.1, pos.z,
                    0, Math.cos(angle), 0, Math.sin(angle), 1.5);
        }
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