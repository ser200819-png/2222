package com.lololoshka.thirteenflames.items;

import com.lololoshka.thirteenflames.ModItemGroup;
import com.lololoshka.thirteenflames.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.Rarity;
import net.minecraft.tags.Tags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Молот Монту: добывает руду областью 3x3x3 одним ударом. */
public class HammerOfMontuItem extends PickaxeItem {

    public HammerOfMontuItem() {
        super(ModItems.TF_TIER, 1, -2.6F,
                new Item.Properties().group(ModItemGroup.THIRTEEN_FLAMES).maxDamage(2600).rarity(Rarity.EPIC));
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, PlayerEntity player) {
        World world = player.world;
        BlockState center = world.getBlockState(pos);
        if (Tags.Blocks.ORES.contains(center.getBlock())) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos p = pos.add(dx, dy, dz);
                        BlockState s = world.getBlockState(p);
                        if (Tags.Blocks.ORES.contains(s.getBlock())) {
                            world.destroyBlock(p, true, player);
                            stack.damageItem(1, player, (p2) -> p2.sendBreakAnimation(EquipmentSlotType.MAINHAND));
                        }
                    }
        }
        return super.onBlockStartBreak(stack, pos, player);
    }
}
