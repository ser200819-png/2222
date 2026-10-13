package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ITooltipFlag;
import net.minecraft.item.Rarity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class FangOfRhonosItem extends Item {

    public FangOfRhonosItem() {
        super(new Item.Properties().maxStackSize(1)
                .rarity(Rarity.RARE)
                .group(ModItems.GROUP));
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("tooltip.thirteenflames.fang_of_rhonos"));
    }
}
