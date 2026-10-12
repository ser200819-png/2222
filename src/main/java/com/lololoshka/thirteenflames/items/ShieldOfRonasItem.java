package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.ShieldItem;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;

/** Щит Ронаса: блокирует удары, пока в левой руке даёт Сопротивление I. */
public class ShieldOfRonasItem extends ShieldItem {

    public ShieldOfRonasItem() {
        super(new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxDamage(504).rarity(Rarity.EPIC));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote || !(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        if (player.getHeldItemOffhand() == stack) {
            player.addPotionEffect(new EffectInstance(Effects.RESISTANCE, 40, 0, false, false));
        }
    }
}
