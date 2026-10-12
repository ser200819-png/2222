package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModEvents;
import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;

public class BowOfKnefmtitiItem extends Item {

    public static final int SHOT_COOLDOWN_TICKS = 60;      // выстрел раз в 3 секунды
    public static final int ABILITY_COOLDOWN_TICKS = 2400; // откат Града молний: 120 секунд
    public static final int CHANNEL_TICKS = 60;            // заряд способности: 3 секунды
    public static final int MIN_DRAW_TICKS = 10;           // минимальная натяжка для выстрела
    public static final int FULL_DRAW_TICKS = 30;          // полная натяжка (макс. урон)

    public BowOfKnefmtitiItem() {
        super(new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxStackSize(1).rarity(Rarity.EPIC));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // Способность: Shift+ПКМ — «Град молний»
        if (player.isSneaking()) {
            if (!world.isRemote) {
                CompoundNBT d = player.getPersistentData();
                long now = world.getGameTime();
                long last = d.getLong("TfBowAbilityCd");
                if (now - last < ABILITY_COOLDOWN_TICKS) {
                    long remainSec = (ABILITY_COOLDOWN_TICKS - (now - last)) / 20L + 1L;
                    player.sendStatusMessage(new StringTextComponent("§cГрад молний: откат " + remainSec + " сек."), true);
                } else {
                    // цель: твёрдый блок в прицеле (до 45 блоков), иначе — точка в воздухе
                    Vector3d eye = player.getEyePosition(1.0F);
                    Vector3d end = eye.add(player.getLook(1.0F).scale(45.0D));
                    BlockRayTraceResult trace = world.rayTraceBlocks(new RayTraceContext(eye, end,
                            RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
                    Vector3d center;
                    if (trace.getType() == RayTraceResult.Type.BLOCK) {
                        BlockPos p = trace.getPos();
                        center = new Vector3d(p.getX() + 0.5D, p.getY() + 1.0D, p.getZ() + 0.5D);
                    } else {
                        center = end;
                    }
                    d.putLong("TfBowAbilityCd", now);
                    d.putLong("TfChannelStart", now);
                    d.putDouble("TfChannelX", center.x);
                    d.putDouble("TfChannelY", center.y);
                    d.putDouble("TfChannelZ", center.z);
                    d.putDouble("TfAnchorX", player.getPosX());
                    d.putDouble("TfAnchorY", player.getPosY());
                    d.putDouble("TfAnchorZ", player.getPosZ());
                    player.sendStatusMessage(new StringTextComponent("§9Зарядка Града молний... не двигайся!"), true);
                }
            }
            return ActionResult.resultSuccess(stack);
        }

        // Обычный выстрел: натяжка, отпускание — стрела
        if (player.getCooldownTracker().hasCooldown(this)) {
            return ActionResult.resultFail(stack);
        }
        player.setActiveHand(hand);
        return ActionResult.resultConsume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    // Визуал зарядки: сгусток заклинания сжимается перед игроком
    @Override
    public void onUsingTick(ItemStack stack, LivingEntity entity, int count) {
        if (entity.world.isRemote || !(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        ServerWorld world = (ServerWorld) entity.world;
        int elapsed = Math.min(this.getUseDuration(stack) - count, FULL_DRAW_TICKS);
        double t = elapsed / (double) FULL_DRAW_TICKS;
        if (elapsed % 2 == 0) {
            Vector3d look = player.getLook(1.0F);
            double reach = 0.8D + t * 0.9D;
            double px = player.getPosX() + look.x * reach;
            double py = player.getPosY() + 1.4D + look.y * reach;
            double pz = player.getPosZ() + look.z * reach;
            double spread = 0.45D - t * 0.3D;
            for (int i = 0; i < 2; i++) {
                double a = elapsed * 0.7D + i * Math.PI;
                world.spawnParticle(ParticleTypes.ENCHANT,
                        px + Math.cos(a) * spread, py + Math.sin(a) * spread, pz,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            if (t > 0.7D) {
                world.spawnParticle(ParticleTypes.ENCHANT, px, py, pz, 1, 0.05D, 0.05D, 0.05D, 0.01D);
            }
        }
        if (elapsed == FULL_DRAW_TICKS) {
            world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.8F, 1.8F);
        }
    }

    // Выстрел при отпускании ПКМ
    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft) {
        if (world.isRemote || !(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        int draw = this.getUseDuration(stack) - timeLeft;
        if (draw < MIN_DRAW_TICKS) return;

        double t = Math.min(1.0D, draw / (double) FULL_DRAW_TICKS);
        Vector3d dir = player.getLook(1.0F);
        ArrowEntity arrow = new ArrowEntity(world, player);
        arrow.shoot(dir.x, dir.y, dir.z, 3.0F, 0.5F);
        arrow.setDamage(4.0D + 6.0D * t); // 2..5 сердец в зависимости от натяжки
        arrow.pickupStatus = ArrowEntity.PickupStatus.CREATIVE_ONLY;
        world.addEntity(arrow);
        ModEvents.markLightningArrow(arrow);

        // Кинематографичный выстрел: гром, кольцо фейерверка, вспышка
        ServerWorld sw = (ServerWorld) world;
        world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(),
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.5F, 0.6F);
        world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 0.4F, 1.8F);
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24.0D;
            sw.spawnParticle(ParticleTypes.FIREWORK,
                    player.getPosX() + Math.cos(a) * 0.9D, player.getPosY() + 1.3D, player.getPosZ() + Math.sin(a) * 0.9D,
                    1, 0.02D, 0.02D, 0.02D, 0.05D);
        }
        sw.spawnParticle(ParticleTypes.EXPLOSION, player.getPosX(), player.getPosY() + 1.0D, player.getPosZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);

        player.getCooldownTracker().setCooldown(this, SHOT_COOLDOWN_TICKS);
    }

    @Override
    public boolean isDamageable() {
        return false; // лук не ломается
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new StringTextComponent("§bСтреляет без стрел. Натяни тетиву и отпусти ПКМ."));
        tooltip.add(new StringTextComponent("§bСтрелы бьют молнией: до 5 сердец урона."));
        tooltip.add(new StringTextComponent("§dShift+ПКМ: Град молний — заряд 3 сек., откат 120 сек."));
    }
}
