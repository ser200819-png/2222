package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import com.lololoshka.thirteenflames.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.SwordItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/** Меч Путешественника: ПКМ - рывок вперёд, урон от падения гасится. */
public class TravelerSwordItem extends SwordItem {

    public TravelerSwordItem() {
        super(ModItems.TF_TIER, 3, -2.2F,
                new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxDamage(2200).rarity(Rarity.EPIC));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        if (!world.isRemote) {
            Vector3d look = player.getLookVec();
            player.addVelocity(look.x * 1.2D, 0.3D, look.z * 1.2D);
            player.fallDistance = 0.0F;
            player.getCooldownTracker().setCooldown(this, 60);
        }
        return ActionResult.resultSuccess(player.getHeldItem(hand));
    }
}
