package com.lololoshka.thirteenflames;

import net.minecraft.item.IItemTier;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.lololoshka.thirteenflames.items.*;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ThirteenFlames.MOD_ID);

    /** Единый материал для артефактного оружия. */
    public static final IItemTier TF_TIER = new IItemTier() {
        @Override public int getMaxUses() { return 2200; }
        @Override public float getEfficiency() { return 9.0F; }
        @Override public float getAttackDamage() { return 7.0F; }
        @Override public int getHarvestLevel() { return 4; }
        @Override public int getEnchantability() { return 20; }
        @Override public Ingredient getRepairMaterial() { return Ingredient.EMPTY; }
    };

    public static final RegistryObject<Item> HAMMER_OF_MONTU = ITEMS.register("hammer_of_montu", HammerOfMontuItem::new);
    public static final RegistryObject<Item> FANG_OF_RONAS = ITEMS.register("fang_of_ronas", FangOfRonasItem::new);
    public static final RegistryObject<Item> BOW_OF_KNEFMTITI = ITEMS.register("bow_of_knefmtiti", BowOfKnefmtitiItem::new);
    public static final RegistryObject<Item> HORN_OF_SELIASET = ITEMS.register("horn_of_seliaset", HornOfSeliasetItem::new);
    public static final RegistryObject<Item> TRAVELER_SWORD = ITEMS.register("traveler_sword", TravelerSwordItem::new);
    public static final RegistryObject<Item> INTERWORLD_MASK = ITEMS.register("interworld_mask", InterworldMaskItem::new);
    public static final RegistryObject<Item> LOLOLOSHKA_MASK = ITEMS.register("lololoshka_mask", LololoshkaMaskItem::new);
    public static final RegistryObject<Item> JOHN_SCARF = ITEMS.register("john_scarf", JohnScarfItem::new);
    public static final RegistryObject<Item> JOHN_GLASSES = ITEMS.register("john_glasses", JohnGlassesItem::new);
    public static final RegistryObject<Item> JOHN_BADGE = ITEMS.register("john_badge", JohnBadgeItem::new);
}
