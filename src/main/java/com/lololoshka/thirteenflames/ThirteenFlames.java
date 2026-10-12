package com.lololoshka.thirteenflames;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ThirteenFlames.MOD_ID)
public class ThirteenFlames {
    public static final String MOD_ID = "thirteenflames";
    public static final String FALLEN_TAG = "ThirteenFlamesFallen";

    public ThirteenFlames() {
        ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
