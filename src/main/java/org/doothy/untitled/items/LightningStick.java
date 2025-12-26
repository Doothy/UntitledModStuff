package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.Blocks;
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

public class LightningStick extends Item {

    public static final int MAX_WINDUP_TICKS = 72000;
    public static final int REQUIRED_WINDUP = 20;

    public LightningStick(Properties settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;

        boolean isOnCooldown = player.getCooldowns().isOnCooldown(stack);
        boolean wasOnCooldown = stack.getOrDefault(WAS_ON_COOLDOWN, false);

        if (player.isUsingItem() && player.getUseItem() == stack) {
            HitResult hit = player.pick(25.0D, 0.0F, false);

            // 1. Particle Feedback for Aiming
            if (hit.getType() != HitResult.Type.MISS) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        hit.getLocation().x, hit.getLocation().y + 0.1, hit.getLocation().z,
                        3, 0.2, 0.1, 0.2, 0.05);
            }

            // 2. Lightning Shield Logic (Passive while charging)
            AABB shieldArea = player.getBoundingBox().inflate(1.5);
            level.getEntitiesOfClass(LivingEntity.class, shieldArea, e -> e != player && e.isAlive()).forEach(enemy -> {
                Vec3 pushDir = enemy.position().subtract(player.position()).normalize().multiply(0.3, 0, 0.3);
                enemy.push(pushDir.x, 0.2, pushDir.z);
                enemy.hurt(level.damageSources().lightningBolt(), 1.0f);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, enemy.getX(), enemy.getY() + 1, enemy.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
            });
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
            HitResult hit = player.pick(25.0D, 0.0F, false);

            if (hit.getType() != HitResult.Type.MISS) {
                Vec3 pos = hit.getLocation();

                ManaAttachment mana = player.getAttachedOrCreate(ModAttachments.MANA, () -> new ManaAttachment(100, 100));
                mana.setMana(mana.getMana() - 20);
                player.setAttached(ModAttachments.MANA, mana);
                ServerPlayNetworking.send((ServerPlayer) player, new ManaPayload(mana.getMana(), mana.getMaxMana()));

                // FIXED: applyThunderClap now uses the proper distance calculation
                applyThunderClap(serverLevel, player, pos);
                spawnDramaticBolt(serverLevel, pos);
                world.playSound(null, BlockPos.containing(pos), ModSounds.THUNDER_HIT, SoundSource.WEATHER, 10.0f, 1.0f);

                performSustainedChain(serverLevel, player, pos);

                player.getCooldowns().addCooldown(stack, 30);
            }
            return true;
        }
        return false;
    }

    private void applyThunderClap(ServerLevel level, Player source, Vec3 pos) {
        double radius = 6.0;
        AABB area = new AABB(pos.x - radius, pos.y - 2, pos.z - radius, pos.x + radius, pos.y + 4, pos.z + radius);

        level.getEntitiesOfClass(LivingEntity.class, area, e -> e != source && e.isAlive()).forEach(target -> {
            Vec3 dir = target.position().subtract(pos).normalize();
            // FIXED: Using target.position().distanceTo(pos) instead of target.distanceTo(pos)
            double dist = target.position().distanceTo(pos);
            double strength = 1.2 * (1.0 - (dist / radius));

            target.push(dir.x * strength, 0.4, dir.z * strength);
            target.hurtMarked = true;
        });
    }

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

            target.hurt(level.damageSources().lightningBolt(), currentDamage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 4));
            target.setRemainingFireTicks(40);
            struckEntities.add(target);

            Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);
            spawnJumpParticles(level, currentSource, targetCenter);

            level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.8f, 1.2f + (i * 0.05f));

            currentSource = targetCenter;
        }
    }

    private void spawnJumpParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double distance = start.distanceTo(end);
        int points = (int) (distance * 8);
        java.util.Random random = new java.util.Random();

        for (int i = 0; i <= points; i++) {
            double pct = (double) i / points;
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

    private void spawnDramaticBolt(ServerLevel level, Vec3 pos) {
        double curX = pos.x;
        double curZ = pos.z;
        java.util.Random random = new java.util.Random();
        ColorParticleOption flashWhite = ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF);

        for (double y = pos.y; y < pos.y + 55; y += 0.6) {
            curX += (random.nextDouble() - 0.5) * 0.6;
            curZ += (random.nextDouble() - 0.5) * 0.6;
            level.sendParticles(flashWhite, curX, y, curZ, 1, 0, 0, 0, 0);
        }

        for (int i = 0; i < 64; i++) {
            double angle = Math.toRadians(i * 5.625);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, pos.x, pos.y + 0.1, pos.z, 0, Math.cos(angle), 0, Math.sin(angle), 1.5);
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