package com.lololoshka.thirteenflames;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public class ModItemGroup extends ItemGroup {
    public static final ModItemGroup THIRTEEN_FLAMES = new ModItemGroup("thirteenflames");

    public ModItemGroup(String name) {
        super(name);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ModItems.HAMMER_OF_MONTU.get());
    }
}
