package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Горн Селиасэт: ПКМ - ветер отбрасывает всех врагов в радиусе 5 блоков. */
public class HornOfSeliasetItem extends Item {

    public HornOfSeliasetItem() {
        super(new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxStackSize(1).rarity(Rarity.RARE));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        if (!world.isRemote) {
            for (Entity e : world.getEntitiesWithinAABBExcludingEntity(player, player.getBoundingBox().grow(5))) {
                if (!(e instanceof LivingEntity)) continue;
                double dx = e.getPosX() - player.getPosX();
                double dz = e.getPosZ() - player.getPosZ();
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < 0.1D) d = 0.1D;
                e.addVelocity(dx / d * 1.5D, 0.4D, dz / d * 1.5D);
            }
            player.getCooldownTracker().setCooldown(this, 100);
        }
        return ActionResult.resultSuccess(player.getHeldItem(hand));
    }
}
