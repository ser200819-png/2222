package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class InterworldMaskItem extends Item {
    public InterworldMaskItem() {
        super(new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxStackSize(1).rarity(Rarity.EPIC));
    }

    // Shift+ПКМ: переносит игрока в точку спавна мира
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (!worldIn.isRemote && playerIn.isSneaking() && worldIn instanceof ServerWorld) {
            BlockPos spawn = serverWorld.getSpawnPoint();
            BlockPos spawn = serverWorld.getSharedSpawnPos();
            playerIn.setPositionAndUpdate(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            return ActionResult.resultSuccess(stack);
        }
        return ActionResult.resultPass(stack);
    }
}
