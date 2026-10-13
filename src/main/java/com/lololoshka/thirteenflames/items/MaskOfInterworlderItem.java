package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.item.TooltipFlag;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class MaskOfInterworlderItem extends Item {

    public MaskOfInterworlderItem() {
        super(new Item.Properties().maxStackSize(1)
                .rarity(Rarity.EPIC)
                .group(ModItems.GROUP));
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<ITextComponent> tooltip, TooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("tooltip.thirteenflames.mask_of_interworlder"));
    }
}
