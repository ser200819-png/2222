package com.lololoshka.thirteenflames;

import com.lololoshka.thirteenflames.items.BowOfKnefmtitiItem;
import com.lololoshka.thirteenflames.items.FangOfRhonosItem;
import com.lololoshka.thirteenflames.items.HammerOfMontuItem;
import com.lololoshka.thirteenflames.items.HornOfSeliasetItem;
import com.lololoshka.thirteenflames.items.MaskOfInterworlderItem;
import com.lololoshka.thirteenflames.items.TravelerSwordItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {

    // Своя творческая вкладка мода
    public static final ItemGroup GROUP = new ItemGroup("thirteenflames") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(HAMMER_OF_MONTU.get());
        }
    };

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ThirteenFlames.MOD_ID);

    // ---------- Предметы мода ----------

    public static final RegistryObject<Item> BOW_OF_KNEFMTITI =
            ITEMS.register("bow_of_knefmtiti", BowOfKnefmtitiItem::new);

    public static final RegistryObject<Item> HAMMER_OF_MONTU =
            ITEMS.register("hammer_of_montu", HammerOfMontuItem::new);

    public static final RegistryObject<Item> HORN_OF_SELIASET =
            ITEMS.register("horn_of_seliasset", HornOfSeliasetItem::new);

    public static final RegistryObject<Item> TRAVELER_SWORD =
            ITEMS.register("traveler_sword", TravelerSwordItem::new);

    public static final RegistryObject<Item> FANG_OF_RHONOS =
            ITEMS.register("fang_of_rhonos", FangOfRhonosItem::new);

    public static final RegistryObject<Item> MASK_OF_INTERWORLDER =
            ITEMS.register("mask_of_interworlder", MaskOfInterworlderItem::new);
}
