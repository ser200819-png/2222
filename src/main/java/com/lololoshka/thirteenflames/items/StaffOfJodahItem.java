package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/** Посох Джодаха: ПКМ - телепорт на 12 блоков в сторону взгляда. */
public class StaffOfJodahItem extends Item {

    public StaffOfJodahItem() {
        super(new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxStackSize(1).maxDamage(240).rarity(Rarity.EPIC));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        if (!world.isRemote) {
            Vector3d look = player.getLookVec();
            Vector3d pos = player.getPositionVec().add(look.scale(12.0D));
            player.setPositionAndUpdate(pos.x, pos.y, pos.z);
            player.getCooldownTracker().setCooldown(this, 60);
        }
        return ActionResult.resultSuccess(player.getHeldItem(hand));
    }
}
