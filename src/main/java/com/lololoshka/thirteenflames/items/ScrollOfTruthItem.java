package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Свиток Правды Хэтинтэпа: подсвечивает все сущности в радиусе 16 блоков. */
public class ScrollOfTruthItem extends Item {

    public ScrollOfTruthItem() {
        super(new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxStackSize(16).rarity(Rarity.RARE));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        if (!world.isRemote) {
            for (Entity e : world.getEntitiesWithinAABBExcludingEntity(player, player.getBoundingBox().grow(16))) {
                if (e instanceof LivingEntity) {
                    ((LivingEntity) e).addPotionEffect(new EffectInstance(Effects.GLOWING, 200, 0));
                }
            }
            player.getCooldownTracker().setCooldown(this, 200);
        }
        return ActionResult.resultSuccess(player.getHeldItem(hand));
    }
}
