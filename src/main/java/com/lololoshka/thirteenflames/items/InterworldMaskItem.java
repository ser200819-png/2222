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
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.WorldInfo;

/** Маска Междумирца: шлем с Огнеустойчивостью; Shift+ПКМ - "Отправление" к спавну. */
public class InterworldMaskItem extends ArmorItem {

    public InterworldMaskItem() {
        super(ArmorMaterial.NETHERITE, EquipmentSlotType.HEAD,
                new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxStackSize(1).rarity(Rarity.EPIC));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote || !(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;
        if (player.getItemStackFromSlot(EquipmentSlotType.HEAD) == stack) {
            player.addPotionEffect(new EffectInstance(Effects.FIRE_RESISTANCE, 40, 0, false, false));
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        if (!world.isRemote && player.isSneaking()) {
            WorldInfo info = world.getWorldInfo();
            player.setPositionAndUpdate(info.getSpawnX() + 0.5D, info.getSpawnY(), info.getSpawnZ() + 0.5D);
            player.getCooldownTracker().setCooldown(this, 200);
            return ActionResult.resultSuccess(player.getHeldItem(hand));
        }
        return ActionResult.resultPass(player.getHeldItem(hand));
    }
}
