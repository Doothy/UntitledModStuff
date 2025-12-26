package org.doothy.untitled.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
            int useTicks = player.getTicksUsingItem();

            if (hit.getType() != HitResult.Type.MISS) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        hit.getLocation().x, hit.getLocation().y + 0.1, hit.getLocation().z,
                        3, 0.2, 0.1, 0.2, 0.05);
            }

            if (useTicks >= REQUIRED_WINDUP) {
                for (int i = 0; i < 2; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double xOff = Math.cos(angle) * 0.7;
                    double zOff = Math.sin(angle) * 0.7;
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX() + xOff, player.getY() + 1.0, player.getZ() + zOff,
                            1, 0.0, 0.1, 0.0, 0.02);
                }
            }
        }

        if (!isOnCooldown && !player.isUsingItem()) {
            if (level.random.nextFloat() < 0.15f) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        1, 0.2, 0.2, 0.2, 0.02);
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
        if (mana.getMana() < 20) {
            if (!world.isClientSide()) {
                user.displayClientMessage(Component.literal("Not enough Mana!").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

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

        if (elapsed >= REQUIRED_WINDUP) {
            if (world.isClientSide()) {
                LightningSoundHelper.Holder.INSTANCE.applyRecoil();
            }

            if (world instanceof ServerLevel serverLevel) {
                ManaAttachment mana = player.getAttachedOrCreate(ModAttachments.MANA, () -> new ManaAttachment(100, 100));
                HitResult hit = player.pick(25.0D, 0.0F, false);

                if (hit.getType() != HitResult.Type.MISS) {
                    // 1. Mana & Data
                    mana.setMana(mana.getMana() - 20);
                    player.setAttached(ModAttachments.MANA, mana);
                    ServerPlayNetworking.send((ServerPlayer) player, new ManaPayload(mana.getMana(), mana.getMaxMana()));

                    Vec3 pos = hit.getLocation();
                    BlockPos center = BlockPos.containing(pos);

                    // 2. Visuals: Lightning Bolt
                    serverLevel.globalLevelEvent(2006, center, 0);
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 30, 0.6, 0.6, 0.6, 0.2);

                    // 3. SHOCKWAVES (One at player, one at destination)
                    spawnShockwave(serverLevel, player.position().add(0, 0.1, 0), 0.5); // Player backblast
                    spawnShockwave(serverLevel, pos.add(0, 0.1, 0), 0.8);             // Target impact wave (faster)

                    // 4. Physical Recoil
                    Vec3 recoilDir = player.getLookAngle().reverse().multiply(0.4, 0, 0.4).add(0, 0.15, 0);
                    player.push(recoilDir.x, recoilDir.y, recoilDir.z);
                    player.hurtMarked = true;

                    // 5. Combat Logic
                    AABB area = new AABB(pos.x - 3, pos.y - 3, pos.z - 3, pos.x + 3, pos.y + 3, pos.z + 3);
                    world.getEntitiesOfClass(LivingEntity.class, area).forEach(t -> t.hurt(world.damageSources().lightningBolt(), 10.0f));

                    for (BlockPos targetPos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 1, 3))) {
                        if (world.getBlockState(targetPos).isAir() && world.getBlockState(targetPos.below()).isFaceSturdy(world, targetPos.below(), Direction.UP)) {
                            if (world.random.nextFloat() < 0.1f) world.setBlockAndUpdate(targetPos, Blocks.FIRE.defaultBlockState());
                        }
                    }

                    // 6. Audio
                    world.playSound(null, center, ModSounds.THUNDER_HIT, SoundSource.WEATHER, 5.0f, 1.0f);
                    player.getCooldowns().addCooldown(stack, 20);
                }
            }
            return true;
        } else {
            if (world.isClientSide()) player.displayClientMessage(Component.literal("Spell Fizzled...").withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return false;
        }
    }

    /**
     * Helper to spawn a ring of cloud particles expanding from a center point
     */
    private void spawnShockwave(ServerLevel level, Vec3 pos, double speed) {
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double xDir = Math.cos(angle);
            double zDir = Math.sin(angle);
            // Count 0 allows X, Y, Z to act as velocity vectors
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 0, xDir, 0.0, zDir, speed);
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
                    Component.literal("Hold to charge a lightning strike!").withStyle(net.minecraft.ChatFormatting.GOLD),
                    Component.literal("Cost: 20 Mana").withStyle(net.minecraft.ChatFormatting.AQUA),
                    Component.literal("Charge: 1.0s").withStyle(net.minecraft.ChatFormatting.GRAY)
            )))
    );

    public static void initialize(){
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register((itemGroup) -> itemGroup.accept(LightningStick.LIGHTNING_STICK));
    }
}