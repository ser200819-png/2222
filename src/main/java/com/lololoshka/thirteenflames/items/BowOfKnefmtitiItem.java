package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;

/** Лунный Лук Кнефмтити: стрелы призывают молнию (обработка в ModEvents). */
public class BowOfKnefmtitiItem extends BowItem {

    public BowOfKnefmtitiItem() {
        super(new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxDamage(768).rarity(Rarity.EPIC));
    }

    @Override
    public AbstractArrowEntity customeArrow(AbstractArrowEntity arrow) {
        arrow.getPersistentData().putBoolean("TF_LIGHTNING_ARROW", true);
        return arrow;
    }
}
