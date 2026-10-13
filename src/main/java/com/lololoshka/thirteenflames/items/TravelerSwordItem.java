package com.lololoshka.thirteenflames.items;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.SwordItem;
import net.minecraft.item.UseAction;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Locale;

public class TravelerSwordItem extends SwordItem {

    public static final int MAX_CHARGE_TICKS = 72000; // 60 минут удержания максимум
    private static final int TELEPORT_COOLDOWN = 9 * 20;
    private static final int TELEPORT_RANGE = 16;

    public TravelerSwordItem() {
        super(new IItemTier() {
            @Override public int getMaxUses()        { return 2031; }
            @Override public float getEfficiency()   { return 9.0F; }
            @Override public float getAttackDamage() { return 5.0F; } // +3 базовых меча = 9 урона
            @Override public int getHarvestLevel()   { return 3; }
            @Override public int getEnchantability() { return 15; }
            @Override public Ingredient getRepairMaterial() { return Ingredient.fromItems(Items.DIAMOND); }
        }, 3, -2.4F, new Item.Properties().maxStackSize(1).rarity(Rarity.EPIC));
        // Если в старом конструкторе был .group(...) — перенеси его в Properties выше
    }

    // ---------- ПКМ: телепорт. Shift+ПКМ: зарядка ----------

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (player.isSneaking()) {
            if (player.getCooldownTracker().hasCooldown(this)) return ActionResult.resultFail(stack);
            player.setActiveHand(hand);
            return ActionResult.resultConsume(stack);
        } else {
            if (player.getCooldownTracker().hasCooldown(this)) return ActionResult.resultFail(stack);
            if (!world.isRemote) {
                teleport((ServerWorld) world, player);
                player.getCooldownTracker().setCooldown(this, TELEPORT_COOLDOWN);
            }
            player.swingArm(hand);
            return ActionResult.resultSuccess(stack);
        }
    }

    private void teleport(ServerWorld world, PlayerEntity player) {
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d look = player.getLookVec().normalize();
        Vector3d end = eye.add(look.scale(TELEPORT_RANGE));

        RayTraceResult hit = world.rayTraceBlocks(new RayTraceContext(eye, end,
                RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, player));

        Vector3d target = (hit != null && hit.getType() == RayTraceResult.Type.BLOCK)
                ? hit.getHitVec().subtract(look.scale(0.5)) // чуть перед блоком
                : end;

        double x = target.x;
        double y = target.y - 1.0;
        double z = target.z;
        player.setPositionAndUpdate(x, y, z);
        player.fallDistance = 0.0F;

        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        world.spawnParticle(ParticleTypes.PORTAL, x, y + 1.0, z, 20, 0.3, 0.6, 0.3, 0.5);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_CHARGE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    // ---------- Зарядка: частицы и таймер с цветами ----------

    @Override
    public void onUsingTick(ItemStack stack, LivingEntity entity, int count) {
        if (!(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        World world = player.world;
        if (world.isRemote) return;

        long elapsed = MAX_CHARGE_TICKS - count;
        double seconds = elapsed / 20.0;

        ServerWorld sw = (ServerWorld) world;
        Vector3d pos = player.getPositionVec().add(0, 1.0, 0);

        if (seconds <= 10) {
            if (elapsed % 10 == 0) {
                sw.spawnParticle(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 4, 0.4, 0.6, 0.4, 0.0);
            }
        } else if (seconds <= 20) {
            if (elapsed % 5 == 0) {
                sw.spawnParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 6, 0.5, 0.8, 0.5, 0.01);
            }
        } else {
            if (elapsed % 2 == 0) {
                sw.spawnParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 8, 0.6, 1.0, 0.6, 0.02);
                sw.spawnParticle(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 1.0, pos.z, 3, 0.5, 0.5, 0.5, 0.01);
            }
        }

        boolean pulse = seconds > 100;
        if (elapsed % 10 == 0 || (pulse && elapsed % 4 == 0)) {
            TextFormatting color;
            if (seconds <= 10)       color = TextFormatting.GREEN;
            else if (seconds <= 20)  color = TextFormatting.RED;
            else if (seconds <= 100) color = TextFormatting.DARK_PURPLE;
            else color = (world.getGameTime() % 8 < 4) ? TextFormatting.LIGHT_PURPLE : TextFormatting.DARK_PURPLE;

            player.sendStatusMessage(new StringTextComponent("Заряд: "
                    + String.format(Locale.US, "%.1f", seconds) + " с").mergeStyle(color), true);
        }
    }

    // ---------- Отпускание: атака по уровню заряда ----------

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof PlayerEntity)) return;
        if (world.isRemote) return;
        PlayerEntity player = (PlayerEntity) entity;
        ServerWorld sw = (ServerWorld) world;

        long elapsedTicks = MAX_CHARGE_TICKS - timeLeft;
        double seconds = elapsedTicks / 20.0;

        if (seconds >= 2 && seconds <= 10) {
            slashLine(sw, player, 2, 17.0F, SlashKind.EMERALD);
            player.getCooldownTracker().setCooldown(this, 2 * 20);
        } else if (seconds <= 20) {
            slashLine(sw, player, 5, 25.0F, SlashKind.FIRE);
            player.getCooldownTracker().setCooldown(this, 6 * 20);
        } else if (seconds <= 100) {
            slashLine(sw, player, 8, 45.0F, SlashKind.PURPLE);
            breakVerticalSemicircle(sw, player, 15);
            player.getCooldownTracker().setCooldown(this, 30 * 20);
        } else {
            ultimateSphere(sw, player, 45);
            player.getCooldownTracker().setCooldown(this, 90 * 20);
        }
    }

    private enum SlashKind { EMERALD, FIRE, PURPLE }

    // ---------- Разрезы ----------

    private void slashLine(ServerWorld world, PlayerEntity player, double radius, float damage, SlashKind kind) {
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d look = player.getLookVec().normalize();
        Vector3d up = new Vector3d(0, 1, 0);
        Vector3d side = look.crossProduct(up).normalize();

        IParticleData particle;
        Vector3d dir1;
        Vector3d dir2 = null;
        switch (kind) {
            case EMERALD:
            default:
                particle = ParticleTypes.HAPPY_VILLAGER;
                dir1 = side;                            // горизонтальная линия
                break;
            case FIRE:
                particle = ParticleTypes.FLAME;
                dir1 = side.add(up).normalize();        // диагональ 1
                dir2 = side.subtract(up).normalize();   // диагональ 2 (крест)
                break;
            case PURPLE:
                particle = ParticleTypes.SOUL_FIRE_FLAME;
                dir1 = up;                              // вертикальная линия
                break;
        }

        for (double t = -radius; t <= radius; t += 0.5) {
            Vector3d p1 = eye.add(dir1.scale(t));
            world.spawnParticle(particle, p1.x, p1.y, p1.z, 2, 0.05, 0.05, 0.05, 0.0);
            if (dir2 != null) {
                Vector3d p2 = eye.add(dir2.scale(t));
                world.spawnParticle(particle, p2.x, p2.y, p2.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
        }

        List<LivingEntity> targets = world.getEntitiesWithinAABB(LivingEntity.class,
                player.getBoundingBox().grow(radius + 1.0), e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            Vector3d toTarget = target.getPositionVec().add(0, target.getHeight() / 2.0, 0).subtract(eye);
            if (toTarget.dotProduct(look) <= 0) continue;   // только перед игроком
            if (toTarget.length() > radius + 1.0) continue;
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
        }

        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 1.2F, kind == SlashKind.EMERALD ? 1.2F : 0.7F);
    }

    // ---------- Вертикальный полукруг блоков (уровень 3) ----------

    private void breakVerticalSemicircle(ServerWorld world, PlayerEntity player, int radius) {
        BlockPos center = player.getPosition();
        Vector3d look = player.getLookVec().normalize();
        double lh = Math.sqrt(look.x * look.x + look.z * look.z);
        if (lh < 0.01) lh = 0.01;
        double hx = look.x / lh, hz = look.z / lh;

        for (int angleDeg = -90; angleDeg <= 90; angleDeg += 5) {
            double a = Math.toRadians(angleDeg);
            for (int r = 1; r <= radius; r++) {
                double dx = hx * r * Math.cos(a);
                double dy = r * Math.sin(a);
                double dz = hz * r * Math.cos(a);
                BlockPos p = center.add((int) Math.round(dx), (int) Math.round(dy) + 1, (int) Math.round(dz));
                destroyBlockNoDrop(world, p);
            }
        }
        world.playSound(null, center, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.8F, 0.6F);
    }

    // ---------- Ульта: сфера 45 (уровень 4) ----------

    private void ultimateSphere(ServerWorld world, PlayerEntity player, int radius) {
        BlockPos feet = player.getPosition();

        List<LivingEntity> targets = world.getEntitiesWithinAABB(LivingEntity.class,
                player.getBoundingBox().grow(5.0), e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), 75.0F);
        }

        for (int i = 0; i < 60; i++) {
            double a = world.rand.nextDouble() * Math.PI * 2;
            double b = world.rand.nextDouble() * Math.PI - Math.PI / 2;
            double rr = radius * (0.9 + world.rand.nextDouble() * 0.1);
            world.spawnParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    feet.getX() + 0.5 + Math.cos(a) * Math.cos(b) * rr,
                    feet.getY() + Math.sin(b) * rr,
                    feet.getZ() + 0.5 + Math.sin(a) * Math.cos(b) * rr,
                    1, 0.1, 0.1, 0.1, 0.0);
        }

        // Ломаем сферу без дропа, кроме платформы 3x3 (слой ног и слой под ногами)
        for (BlockPos p : BlockPos.getAllInBoxMutable(
                feet.add(-radius, -radius, -radius), feet.add(radius, radius, radius))) {
            if (p.distanceSq(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, true)
                    > (double) radius * radius) continue;
            boolean onPlatform = p.getX() >= feet.getX() - 1 && p.getX() <= feet.getX() + 1
                    && p.getZ() >= feet.getZ() - 1 && p.getZ() <= feet.getZ() + 1
                    && (p.getY() == feet.getY() || p.getY() == feet.getY() - 1);
            if (onPlatform) continue;
            destroyBlockNoDrop(world, p);
        }

        world.playSound(null, feet, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 2.0F, 0.5F);
    }

    private void destroyBlockNoDrop(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return;
        if (state.getBlockHardness(world, pos) < 0) return; // бедрок не трогаем
        if (world.getTileEntity(pos) != null) return;       // сундуки и прочие не трогаем
        world.destroyBlock(pos, false);
    }

    // Разрезы не тратят прочность меча
    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return false;
    }
}
