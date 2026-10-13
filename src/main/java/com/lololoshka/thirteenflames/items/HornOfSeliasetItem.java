package com.lololoshka.thirteenflames.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.List;

public class HornOfSeliasetItem extends Item {

    private static final int PUSH_RADIUS = 5;
    private static final int PUSH_MAX_TICKS = 200;   // 10 секунд
    private static final int PUSH_COOLDOWN = 40 * 20; // откат 40 секунд
    private static final int BEAM_MAX_TICKS = 300;   // 15 секунд
    private static final int BEAM_COOLDOWN = 120 * 20; // откат 120 секунд
    private static final double BEAM_LENGTH = 30.0;

    private static final String USE_TICKS_TAG = "tf_use_ticks";

    public HornOfSeliasetItem() {
        super(new Item.Properties().maxStackSize(1));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.getCooldownTracker().hasCooldown(this)) {
            return ActionResult.resultFail(stack);
        }
        if (!world.isRemote) {
            stack.getOrCreateTag().putInt(USE_TICKS_TAG, 0);
        }
        return ActionResult.resultConsume(stack);
    }

    @Override
    public void onUsingTick(ItemStack stack, LivingEntity entity, int count) {
        if (!(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        World world = player.world;
        if (world.isRemote) return;
        ServerWorld serverWorld = (ServerWorld) world;

        int ticks = stack.getOrCreateTag().getInt(USE_TICKS_TAG);
        stack.getOrCreateTag().putInt(USE_TICKS_TAG, ticks + 1);

        if (player.isSneaking()) {
            // Луч ветра (Shift+ПКМ)
            if (ticks >= BEAM_MAX_TICKS) {
                player.stopActiveHand();
                player.getCooldownTracker().setCooldown(this, BEAM_COOLDOWN);
                return;
            }
            windBeam(serverWorld, player);
        } else {
            // Отталкивание (ПКМ)
            if (ticks >= PUSH_MAX_TICKS) {
                player.stopActiveHand();
                player.getCooldownTracker().setCooldown(this, PUSH_COOLDOWN);
                return;
            }
            if (ticks % 10 == 0) { // каждые полсекунды — импульс отталкивания
                pushMobs(serverWorld, player);
            }
        }
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof PlayerEntity) || world.isRemote) return;
        PlayerEntity player = (PlayerEntity) entity;
        int used = stack.getOrCreateTag().getInt(USE_TICKS_TAG);
        stack.getOrCreateTag().putInt(USE_TICKS_TAG, 0);
        if (used <= 0) return;
        // Откат после использования
        player.getCooldownTracker().setCooldown(this,
                player.isSneaking() ? BEAM_COOLDOWN : PUSH_COOLDOWN);
    }

    // ---------- Отталкивание в радиусе 5 блоков ----------

    private void pushMobs(ServerWorld world, PlayerEntity player) {
        List<LivingEntity> list = world.getEntitiesWithinAABB(LivingEntity.class,
                player.getBoundingBox().grow(PUSH_RADIUS), e -> e != player && e.isAlive());
        for (LivingEntity target : list) {
            double dx = target.getPosX() - player.getPosX();
            double dz = target.getPosZ() - player.getPosZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.001) continue;
            double power = 1.2 * (1.0 - dist / (PUSH_RADIUS + 1.0));
            target.setMotion(dx / dist * power, 0.35, dz / dist * power);
        }
        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP,
                SoundCategory.PLAYERS, 1.0F, 1.2F);
        // Визуальный порыв
        Vector3d look = player.getLookVec();
        world.spawnParticle(ParticleTypes.CLOUD,
                player.getPosX() + look.x, player.getPosY() + 1.0, player.getPosZ() + look.z,
                10, 0.5, 0.5, 0.5, 0.15);
    }

    // ---------- Луч ветра (Shift+ПКМ) ----------

    private void windBeam(ServerWorld world, PlayerEntity player) {
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d dir = player.getLookVec().normalize();
        Vector3d end = eye.add(dir.scale(BEAM_LENGTH));

        // Частицы мини-торнадо вдоль луча
        for (int i = 2; i < BEAM_LENGTH; i += 2) {
            double t = i;
            double spin = (world.getGameTime() % 20) * Math.PI / 10.0;
            world.spawnParticle(ParticleTypes.CLOUD,
                    eye.x + dir.x * t + Math.cos(spin + i) * 0.6,
                    eye.y + dir.y * t,
                    eye.z + dir.z * t + Math.sin(spin + i) * 0.6,
                    2, 0.2, 0.4, 0.2, 0.01);
        }

        List<LivingEntity> list = world.getEntitiesWithinAABB(LivingEntity.class,
                new AxisAlignedBB(eye, end).grow(2.5),
                e -> e != player && e.isAlive());
        for (LivingEntity target : list) {
            Vector3d center = target.getPositionVec().add(0, target.getHeight() / 2.0, 0);
            Vector3d toTarget = center.subtract(eye);
            double along = toTarget.dotProduct(dir);
            if (along < 0 || along > BEAM_LENGTH) continue;
            Vector3d closest = eye.add(dir.scale(along));
            if (center.squareDistanceTo(closest) > 2.25) continue; // в пределах ~1.5 блока от луча

            // 2-3 хп в секунду + отталкивание вдоль луча
            if (world.getGameTime() % 20 == 0) {
                target.attackEntityFrom(DamageSource.causePlayerDamage(player), 2.5F);
            }
            Vector3d motion = target.getMotion();
            target.setMotion(motion.x + dir.x * 0.12,
                    motion.y + dir.y * 0.06 + 0.02,
                    motion.z + dir.z * 0.12);
        }
        if (world.getGameTime() % 10 == 0) {
            world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP,
                    SoundCategory.PLAYERS, 0.7F, 1.4F);
        }
    }

    // ---------- «Торнадо»: запуск из чата ----------

    public static void startTornado(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        if (player.getPersistentData().contains("tf_tornado_end")) return; // уже активно

        player.getPersistentData().putLong("tf_tornado_end", world.getGameTime() + 20 * 20);
        player.getPersistentData().putDouble("tf_tornado_x", player.getPosX());
        player.getPersistentData().putDouble("tf_tornado_y", player.getPosY() + 4.0);
        player.getPersistentData().putDouble("tf_tornado_z", player.getPosZ());
        player.getPersistentData().putBoolean("tf_weather_rain", world.isRaining());
        player.getPersistentData().putBoolean("tf_weather_thunder", world.isThundering());

        world.setRaining(true);
        world.setThundering(true);
        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                SoundCategory.WEATHER, 2.0F, 0.8F);
        player.sendStatusMessage(new net.minecraft.util.text.TranslationTextComponent(
                "message.thirteenflames.horn_tornado_start"), true);
    }
}
