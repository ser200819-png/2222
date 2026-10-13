package com.lololoshka.thirteenflames.items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HammerOfMontuItem extends Item {

    public static final int MAX_DURABILITY = 1600;
    private static final int SLEEP_THRESHOLD = 50;
    private static final int ORE_HUNTER_MIN = 540;
    private static final int WALL_MIN = 330;
    private static final int ORE_HUNTER_COST = 500;
    private static final int WALL_COST = 300;

    private static final int SCAN_RADIUS = 50;
    private static final String SCAN_TAG = "tf_ore_scan";
    private static final String EXHAUSTED_TAG = "tf_exhausted";
    private static final Random RAND = new Random();

    public HammerOfMontuItem() {
        super(new Item.Properties().maxDamage(MAX_DURABILITY));
        // Если в старом конструкторе был .group(...) или тир — перенеси эту строку сюда
    }

    // ---------- Состояние «Истощённая» ----------

    public static boolean isExhausted(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(EXHAUSTED_TAG);
    }

    private static void setExhausted(ItemStack stack, boolean value) {
        CompoundNBT tag = stack.getOrCreateTag();
        if (value) {
            tag.putBoolean(EXHAUSTED_TAG, true);
            stack.setDamage(MAX_DURABILITY - 1);
        } else {
            tag.remove(EXHAUSTED_TAG);
        }
    }

    private static int durability(ItemStack stack) {
        return MAX_DURABILITY - stack.getDamage();
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return isExhausted(stack)
                ? super.getTranslationKey(stack) + "_exhausted"
                : super.getTranslationKey(stack);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        if (isExhausted(stack)) {
            tooltip.add(new TranslationTextComponent("tooltip.thirteenflames.hammer_exhausted")
                    .mergeStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TranslationTextComponent("tooltip.thirteenflames.hammer_of_montu"));
        }
        tooltip.add(new TranslationTextComponent("tooltip.thirteenflames.hammer_durability",
                durability(stack), MAX_DURABILITY).mergeStyle(TextFormatting.DARK_GREEN));
    }

    // ---------- Копание 3x3 и руды 3x3x3 ----------

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, PlayerEntity player) {
        if (isExhausted(stack)) return true;
        if (player.world.isRemote) return false;

        ServerWorld world = (ServerWorld) player.world;
        BlockState center = world.getBlockState(pos);
        boolean centerIsOre = Tags.Blocks.ORES.contains(center.getBlock());

        for (BlockPos p : area3x3(pos, player.getHorizontalFacing())) {
            breakBlockWithDrops(world, player, p);
        }
        if (centerIsOre) {
            BlockPos.getAllInBox(pos.add(-1, -1, -1), pos.add(1, 1, 1)).forEach(p -> {
                BlockState s = world.getBlockState(p);
                if (Tags.Blocks.ORES.contains(s.getBlock())) breakBlockWithDrops(world, player, p);
            });
        }
        return false;
    }

    @Override
    public boolean canHarvestBlock(ItemStack stack, BlockState state) {
        return !isExhausted(stack);
    }

    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return !isExhausted(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(net.minecraft.inventory.EquipmentSlotType slot, ItemStack stack) {
        if (isExhausted(stack)) {
            return HashMultimap.create();
        }
        return super.getAttributeModifiers(slot, stack);
    }

    private static List<BlockPos> area3x3(BlockPos center, Direction facing) {
        Direction side = facing.rotateY();
        List<BlockPos> list = new ArrayList<>();
        for (int dw = -1; dw <= 1; dw++)
            for (int dh = -1; dh <= 1; dh++)
                list.add(center.offset(side, dw).up(dh));
        return list;
    }

    private static void breakBlockWithDrops(ServerWorld world, PlayerEntity player, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir() || world.getTileEntity(pos) != null) return;
        if (state.getBlockHardness(world, pos) < 0) return;
        world.destroyBlock(pos, false);
        for (ItemStack drop : Block.getDrops(state, world, pos, null, player, player.getHeldItemMainhand())) {
            world.addEntity(new ItemEntity(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
        }
    }

    // ---------- Способности по ПКМ ----------

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (isExhausted(stack)) return ActionResult.resultFail(stack);

        if (player.isSneaking()) {
            // «Стена земли»
            if (durability(stack) < WALL_MIN) {
                if (!world.isRemote) player.sendStatusMessage(
                        new TranslationTextComponent("message.thirteenflames.hammer_wall_low"), true);
                return ActionResult.resultFail(stack);
            }
            if (!world.isRemote && buildEarthWall((ServerWorld) world, player)) {
                spendDurability(stack, player, WALL_COST);
            }
            player.swingArm(hand);
            return ActionResult.resultSuccess(stack);
        } else {
            // «Охотник на руды» — запуск постепенного скана
            if (durability(stack) < ORE_HUNTER_MIN) {
                if (!world.isRemote) player.sendStatusMessage(
                        new TranslationTextComponent("message.thirteenflames.hammer_ore_low"), true);
                return ActionResult.resultFail(stack);
            }
            if (!world.isRemote) {
                BlockPos center = player.getPosition();
                CompoundNBT tag = stack.getOrCreateTag();
                CompoundNBT scan = new CompoundNBT();
                scan.putInt("x", center.getX());
                scan.putInt("y", center.getY());
                scan.putInt("z", center.getZ());
                scan.putInt("dy", -SCAN_RADIUS);
                tag.put(SCAN_TAG, scan);
                spendDurability(stack, player, ORE_HUNTER_COST);
                player.sendStatusMessage(
                        new TranslationTextComponent("message.thirteenflames.hammer_ore_start"), true);
                world.playSound(null, center, SoundType.STONE.getBreakSound(),
                        SoundCategory.BLOCKS, 1.0F, 0.5F);
            }
            player.swingArm(hand);
            return ActionResult.resultSuccess(stack);
        }
    }

    // ---------- Постепенный скан руды: один слой за тик ----------

    private void tickOreScan(ItemStack stack, ServerWorld world, PlayerEntity player) {
        CompoundNBT tag = stack.getTag().getCompound(SCAN_TAG);
        int cx = tag.getInt("x");
        int cy = tag.getInt("y");
        int cz = tag.getInt("z");
        int dy = tag.getInt("dy");

        int y = cy + dy;
        boolean foundOre = false;

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                BlockState state = world.getBlockState(p);
                if (Tags.Blocks.ORES.contains(state.getBlock())) {
                    List<ItemStack> drops = Block.getDrops(state, world, p.toImmutable(), null);
                    world.destroyBlock(p, false);
                    for (ItemStack drop : drops) {
                        ItemEntity entity = new ItemEntity(world,
                                p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, drop);
                        entity.setMotion((RAND.nextDouble() - 0.5) * 0.4, 0.9, (RAND.nextDouble() - 0.5) * 0.4);
                        world.addEntity(entity);
                    }
                    foundOre = true;
                }
            }
        }

        double angle = (world.getGameTime() % 20) * Math.PI / 10.0;
        world.spawnParticle(ParticleTypes.CLOUD,
                cx + Math.cos(angle) * SCAN_RADIUS, y + 0.5, cz + Math.sin(angle) * SCAN_RADIUS,
                4, 1.0, 1.0, 1.0, 0.01);
        if (foundOre) {
            world.playSound(null, new BlockPos(cx, y, cz),
                    SoundType.NETHERRACK.getBreakSound(), SoundCategory.BLOCKS, 0.8F, 0.6F);
        }

        dy++;
        if (dy > SCAN_RADIUS) {
            stack.getTag().remove(SCAN_TAG);
            player.sendStatusMessage(
                    new TranslationTextComponent("message.thirteenflames.hammer_ore_done"), true);
        } else {
            tag.putInt("dy", dy);
        }
    }

    private boolean buildEarthWall(ServerWorld world, PlayerEntity player) {
        BlockPos feet = player.getPosition();
        BlockState groundState = world.getBlockState(feet.down());
        if (groundState.isAir()) groundState = Blocks.DIRT.getDefaultState();

        Direction facing = player.getHorizontalFacing();
        Direction side = facing.rotateY();
        BlockPos base = feet.down().offset(facing, 1);

        for (int w = -2; w <= 1; w++) {       // 4 блока в ширину
            for (int h = 0; h < 3; h++) {     // 3 блока в высоту
                BlockPos p = base.offset(side, w).up(h);
                if (world.getBlockState(p).isAir()) world.setBlockState(p, groundState);
            }
        }

        world.spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, groundState),
                feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 40, 1.0, 0.5, 1.0, 0.2);
        world.playSound(null, feet, groundState.getSoundType().getPlaceSound(),
                SoundCategory.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    // ---------- Трата и восстановление прочности ----------

    private void spendDurability(ItemStack stack, PlayerEntity player, int cost) {
        int remaining = durability(stack) - cost;
        if (remaining < SLEEP_THRESHOLD) {
            setExhausted(stack, true);
            player.sendStatusMessage(
                    new TranslationTextComponent("message.thirteenflames.hammer_sleeping"), true);
        } else {
            stack.setDamage(MAX_DURABILITY - remaining);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean isSelected) {
        if (world.isRemote || !(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;

        if (stack.hasTag() && stack.getTag().contains(SCAN_TAG)) {
            tickOreScan(stack, (ServerWorld) world, player);
        }

        if (world.getGameTime() % 20 != 0) return;

        if (isExhausted(stack)) {
            int rate;
            if (player.getHeldItemMainhand() == stack)      rate = 20;
            else if (player.getHeldItemOffhand() == stack)  rate = 10;
            else                                            rate = 5;
            stack.setDamage(Math.max(0, stack.getDamage() - rate));
            if (stack.getDamage() == 0) {
                setExhausted(stack, false);
                player.sendStatusMessage(
                        new TranslationTextComponent("message.thirteenflames.hammer_awake"), true);
            }
        } else if (stack.getDamage() > 0) {
            stack.setDamage(stack.getDamage() - 4);
        }
    }
}
