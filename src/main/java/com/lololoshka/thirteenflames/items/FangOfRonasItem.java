package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import com.lololoshka.thirteenflames.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.SwordItem;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;

/** Клык Ронаса: Яд II цели, владелец получает Слабость ("анемия"). */
public class FangOfRonasItem extends SwordItem {

    public FangOfRonasItem() {
        super(ModItems.TF_TIER, 4, -2.0F,
                new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxDamage(2200).rarity(Rarity.EPIC));
    }

    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addPotionEffect(new EffectInstance(Effects.POISON, 200, 1));
        attacker.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 200, 0));
        return true;
    }
}
