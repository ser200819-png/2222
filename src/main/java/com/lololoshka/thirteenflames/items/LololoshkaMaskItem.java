package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;

/** LololoshkaMaskItem: эффект, пока предмет надет. */
public class LololoshkaMaskItem extends ArmorItem {

    public LololoshkaMaskItem() {
        super(ArmorMaterial.NETHERITE, EquipmentSlotType.HEAD,
                new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxStackSize(1).rarity(Rarity.RARE));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote || !(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        if (player.getItemStackFromSlot(EquipmentSlotType.HEAD) == stack) {
            player.addPotionEffect(new EffectInstance(Effects.SPEED, 40, 0, false, false));
        }
    }
}
