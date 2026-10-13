package com.lololoshka.thirteenflames.items;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.List;

public class TravelerSwordItem extends SwordItem {

    private static final int MAX_CHARGE_TICKS = 72000;
    private static final int TELEPORT_COOLDOWN = 9 * 20;
    private static final int TELEPORT_RANGE = 16;

    public TravelerSwordItem() {
        super(new IItemTier() {
            public int getMaxUses()        { return 2031; }
            public float getEfficiency()   { return 9.0F; }
            public float getAttackDamage() { return 5.0F; } // +3 от меча = 9 урона
            public int getHarvestLevel()   { return 3; }
            public int getEnchantability() { return 15; }
            public Ingredient getRepairMaterial() { return Ingredient.fromItems(Items.DIAMOND); }
        }, 3, -2.4F, new Item.Properties().maxStackSize(1).rarity(net.minecraft.util.Rarity.EPIC));
        // Если в старом конструкторе был .group(...) — перенеси его в Properties выше
    }

    // ---------- ПКМ: телепорт / начало зарядки ----------

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (player.isSneaking()) {
            // Shift+ПКМ — зарядка сильной атаки
            if (player.getCooldownTracker().hasCooldown(this)) return ActionResult.resultFail(stack);
            player.setActiveHand(hand);
            return ActionResult.resultConsume(stack);
        } else {
            // ПКМ — телепорт по направлению курсора
            if (player.getCooldownTracker().hasCooldown(this)) return ActionResult.resultFail(stack);
            if (!world.isRemote) {
                teleport(world, player);
                player.getCooldownTracker().setCooldown(this, TELEPORT_COOLDOWN);
            }
            player.swingArm(hand);
            return ActionResult.resultSuccess(stack);
        }
    }

    private void teleport(World world, PlayerEntity player) {
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d look = player.getLookVec().normalize();
        Vector3d end = eye.add(look.scale(TELEPORT_RANGE));

        RayTraceResult hit = world.rayTraceBlocks(new RayTraceContext(eye, end,
                RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.NONE, player));

        Vector3d target;
        if (hit != null && hit.getType() == RayTraceResult.Type.BLOCK) {
            target = hit.getHitVec().subtract(look.scale(0.5)); // чуть перед блоком
        } else {
            target = end;
        }

        double x = target.x, y = target.y - 1.0, z = target.z;
        player.setPositionAndUpdate(x, y, z);
        player.fallDistance = 0.0F;

        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (world instanceof ServerWorld) {
            ServerWorld sw = (ServerWorld) world;
            sw.spawnParticle(ParticleTypes.PORTAL, x, y + 1.0, z, 20, 0.3, 0.6, 0.3, 0.5);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) { return MAX_CHARGE_TICKS; }

    @Override
    public UseAction getUseAction(ItemStack stack) { return UseAction.NONE; }

    // ---------- Зарядка: таймер и частицы ----------

    @Override
    public void onUsingTick(ItemStack stack, LivingEntity entity, int count) {
        if (!(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        World world = player.world;

        long elapsed = MAX_CHARGE_TICKS - count;
        double seconds = elapsed / 20.0;

        if (!world.isRemote && world instanceof ServerWorld) {
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

            // Таймер: каждые полсекунды, с пульсацией после 100 секунд
            boolean pulse = seconds > 100;
            if (elapsed % 10 == 0 || (pulse && elapsed % 4 == 0)) {
                TextFormatting color;
                if (seconds <= 10)      color = TextFormatting.GREEN;
                else if (seconds <= 20) color = TextFormatting.RED;
                else if (seconds <= 100) color = TextFormatting.DARK_PURPLE;
                else color = (world.getGameTime() % 8 < 4) ? TextFormatting.LIGHT_PURPLE : TextFormatting.DARK_PURPLE;

                player.sendStatusMessage(new StringTextComponent("Заряд: "
                        + String.format("%.1f", seconds) + " с").mergeStyle(color), true);
            }
        }
    }

    // ---------- Отпускание: атака по уровню заряда ----------

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        if (world.isRemote) return;
        ServerWorld sw = (ServerWorld) world;

        long elapsedTicks = MAX_CHARGE_TICKS - timeLeft;
        double seconds = elapsedTicks / 20.0;

        if (seconds >= 2 && seconds <= 10) {
            // Изумрудный горизонтальный разрез
            slashLine(sw, player, 2, 17.0F, "emerald");
            player.getCooldownTracker().setCooldown(this, 2 * 20);
        } else if (seconds <= 20) {
            // Огненный диагональный разрез
            slashLine(sw, player, 5, 25.0F, "fire");
            player.getCooldownTracker().setCooldown(this, 6 * 20);
        } else if (seconds <= 100) {
            // Чёрно-фиолетовый вертикальный разрез + полукруг блоков
            slashLine(sw, player, 8, 45.0F, "purple");
            breakVerticalSemicircle(sw, player, 15);
            player.getCooldownTracker().setCooldown(this, 30 * 20);
        } else {
            // Ульта: сфера 45 блоков, урон 75 в радиусе 5
            ultimateSphere(sw, player, 45);
            player.getCooldownTracker().setCooldown(this, 90 * 20);
        }
    }

    // ---------- Разрезы ----------

    private void slashLine(ServerWorld world, PlayerEntity player, double radius, float damage, String kind) {
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d look = player.getLookVec().normalize();
        Vector3d up = new Vector3d(0, 1, 0);
        Vector3d side = look.crossProduct(up).normalize(); // перпендикуляр к взгляду

        ParticleTypes pt;
        Vector3d dir1, dir2;
        if (kind.equals("emerald")) {
            pt = ParticleTypes.HAPPY_VILLAGER;
            dir1 = side;              dir2 = side;      // горизонтальная линия
        } else if (kind.equals("fire")) {
            pt = ParticleTypes.FLAME;
            dir1 = side.add(up);      dir2 = side.subtract(up); // диагональные крест
        } else {
            pt = ParticleTypes.SOUL_FIRE_FLAME;
            dir1 = up;                dir2 = up;        // вертикальная линия
        }

        // Полоса частиц по следу разреза
        for (double t = -radius; t <= radius; t += 0.5) {
            Vector3d p1 = eye.add(dir1.normalize().scale(t));
            Vector3d p2 = eye.add(dir2.normalize().scale(t));
            world.spawnParticle(pt, p1.x, p1.y, p1.z, 2, 0.05, 0.05, 0.05, 0.0);
            if (!kind.equals("emerald")) {
                world.spawnParticle(pt, p2.x, p2.y, p2.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
        }

        // Урон всем мобам в поле зрения
        List<LivingEntity> targets = world.getEntitiesWithinAABB(LivingEntity.class,
                player.getBoundingBox().grow(radius + 1.0), e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            Vector3d toTarget = target.getPositionVec().add(0, target.getHeight() / 2.0, 0).subtract(eye);
            if (toTarget.dotProduct(look) <= 0) continue;      // только перед игроком
            if (toTarget.length() > radius + 1.0) continue;
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
        }

        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 1.2F, kind.equals("emerald") ? 1.2F : 0.7F);
    }

    // ---------- Вертикальный полукруг блоков ----------

    private void breakVerticalSemicircle(ServerWorld world, PlayerEntity player, int radius) {
        BlockPos center = player.getPosition();
        Vector3d look = player.getLookVec().normalize();
        double lh = Math.sqrt(look.x * look.x + look.z * look.z);
        if (lh < 0.01) lh = 0.01;
        double hx = look.x / lh, hz = look.z / lh; // горизонталь взгляда

        for (int angleDeg = -90; angleDeg <= 90; angleDeg += 5) {
            double a = Math.toRadians(angleDeg);
            for (int r = 1; r <= radius; r++) {
                double dx = hx * r * Math.cos(a);
                double dy = r * Math.sin(a);
                double dz = hz * r * Math.cos(a);
                BlockPos p = center.add(
                        (int) Math.round(dx), (int) Math.round(dy) + 1, (int) Math.round(dz));
                destroyBlockNoDrop(world, p);
                destroyBlockNoDrop(world, p.add(hx > 0 ? 1 : -1, 0, hz > 0 ? 1 : -1));
            }
        }
        world.playSound(null, center, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.8F, 0.6F);
    }

    // ---------- Ульта: сфера 45 ----------

    private void ultimateSphere(ServerWorld world, PlayerEntity player, int radius) {
        BlockPos feet = player.getPosition();

        // Урон 75 всем в радиусе 5
        List<LivingEntity> targets = world.getEntitiesWithinAABB(LivingEntity.class,
                player.getBoundingBox().grow(5.0), e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), 75.0F);
        }

        // Масса частиц пламени по краям сферы
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

        // Ломаем сферу, кроме платформы 3x3 под игроком
        for (BlockPos p : BlockPos.getAllInBoxMutable(
                feet.add(-radius, -radius, -radius), feet.add(radius, radius, radius))) {
            double distSq = p.distanceSq(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, true);
            if (distSq > (double) radius * radius) continue;
            // платформа 3x3: слой под ногами и слой ног
            if (p.getX() >= feet.getX() - 1 && p.getX() <= feet.getX() + 1
                    && p.getZ() >= feet.getZ() - 1 && p.getZ() <= feet.getZ() + 1
                    && (p.getY() == feet.getY() || p.getY() == feet.getY() - 1)) continue;
            destroyBlockNoDrop(world, p);
        }

        world.playSound(null, feet, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 2.0F, 0.5F);
    }

    private void destroyBlockNoDrop(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return;
        if (state.getBlockHardness(world, pos) < 0) return;   // бедрок и прочее
        if (world.getTileEntity(pos) != null) return;          // сундуки и прочие не трогаем
        world.destroyBlock(pos, false);
    }

    // Разрез не «тратит» прочность меча — иначе ульта за один удар съедала бы инструмент
    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return false;
    }
}
