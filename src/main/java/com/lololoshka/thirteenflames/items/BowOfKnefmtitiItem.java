package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModEvents;
import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class BowOfKnefmtitiItem extends Item {

    public static final int SHOT_COOLDOWN_TICKS = 60;      // выстрел раз в 3 секунды
    public static final int ABILITY_COOLDOWN_TICKS = 2400; // откат способности 120 секунд
    public static final int CHANNEL_TICKS = 60;            // заряд способности 3 секунды
    public static final double ARROW_DAMAGE = 10.0D;       // повышенный урон стрел (5 сердец)

    public BowOfKnefmtitiItem() {
        // без maxDamage — лук не ломается никогда
        super(new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxStackSize(1).rarity(Rarity.EPIC));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        CompoundNBT data = playerIn.getPersistentData();
        long time = worldIn.getGameTime();

        // Способность: Shift+ПКМ — «Град молний»
        if (playerIn.isSneaking()) {
            if (!worldIn.isRemote) {
                if (time - data.getLong("TfBowCd") < ABILITY_COOLDOWN_TICKS) {
                    return ActionResult.resultPass(stack);
                }
                // ищем твёрдый блок под прицелом (до 45 блоков), иначе — точка в воздухе на 45 блоков
                Vector3d eye = playerIn.getEyePosition(1.0F);
                Vector3d end = eye.add(playerIn.getLook(1.0F).scale(45.0D));
                BlockRayTraceResult trace = worldIn.rayTraceBlocks(
                        new RayTraceContext(eye, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, playerIn));
                Vector3d center;
                if (trace.getType() == RayTraceResult.Type.BLOCK) {
                    BlockPos pos = trace.getPos();
                    center = new Vector3d(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
                } else {
                    center = end;
                }
                data.putLong("TfBowCd", time);
                data.putLong("TfChannelStart", time);
                data.putDouble("TfChannelX", center.x);
                data.putDouble("TfChannelY", center.y);
                data.putDouble("TfChannelZ", center.z);
                playerIn.sendStatusMessage(new TranslationTextComponent("message.thirteenflames.bow_channel"), true);
            }
            return ActionResult.resultSuccess(stack);
        }

        // Обычный выстрел: без стрел, не чаще раза в 3 секунды
        if (!worldIn.isRemote) {
            if (time - data.getLong("TfBowShotCd") < SHOT_COOLDOWN_TICKS) {
                return ActionResult.resultPass(stack);
            }
            data.putLong("TfBowShotCd", time);
            ArrowEntity arrow = new ArrowEntity(worldIn, playerIn);
            arrow.shoot(playerIn.getLook(1.0F).x, playerIn.getLook(1.0F).y, playerIn.getLook(1.0F).z, 3.0F, 0.5F);
            arrow.setDamage(ARROW_DAMAGE);
            arrow.pickupStatus = AbstractArrowEntity.PickupStatus.CREATIVE_ONLY;
            worldIn.addEntity(arrow);
            ModEvents.markLightningArrow(arrow);
        }
        return ActionResult.resultSuccess(stack);
    }

    @Override
    public boolean isDamageable() {
        return false; // страховка: лук не изнашивается
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new TranslationTextComponent("tooltip.thirteenflames.bow_of_knefmtiti"));
        tooltip.add(new TranslationTextComponent("tooltip.thirteenflames.bow_of_knefmtiti.ability"));
    }
}
