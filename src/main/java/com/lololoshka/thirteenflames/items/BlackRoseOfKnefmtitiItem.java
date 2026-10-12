package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import com.lololoshka.thirteenflames.ThirteenFlames;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

/** Чёрная Роза Кнефмтити: ПКМ по земле призывает союзного зомби "Павший". */
public class BlackRoseOfKnefmtitiItem extends Item {

    public BlackRoseOfKnefmtitiItem() {
        super(new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxStackSize(16).rarity(Rarity.RARE));
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        if (world.isRemote) return ActionResultType.SUCCESS;

        BlockPos pos = context.getPos().offset(context.getFace());
        ZombieEntity zombie = new ZombieEntity(EntityType.ZOMBIE, world);
        zombie.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        zombie.getPersistentData().putBoolean(ThirteenFlames.FALLEN_TAG, true);
        zombie.enablePersistence();
        zombie.setCustomName(new StringTextComponent("\u041f\u0430\u0432\u0448\u0438\u0439"));
        world.addEntity(zombie);

        ItemStack stack = context.getPlayer().getHeldItem(context.getHand());
        stack.shrink(1);
        return ActionResultType.SUCCESS;
    }
}
