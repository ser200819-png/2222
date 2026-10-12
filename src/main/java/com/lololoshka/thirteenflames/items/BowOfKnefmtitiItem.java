package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.item.BowItem;
import net.minecraft.item.Rarity;

public class BowOfKnefmtitiItem extends BowItem {
    public BowOfKnefmtitiItem() {
        super(new Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxDamage(768).rarity(Rarity.EPIC));
    }
}

