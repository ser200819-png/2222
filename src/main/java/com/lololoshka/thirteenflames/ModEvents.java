package com.lololoshka.thirteenflames;

import com.lololoshka.thirteenflames.items.BowOfKnefmtitiItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ThirteenFlames.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    // Лунный лук Кнефмтити: попадание стрелой вызывает молнию в точке попадания
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getImmediateSource() instanceof AbstractArrowEntity)) return;
        if (!(source.getTrueSource() instanceof PlayerEntity)) return;

        PlayerEntity shooter = (PlayerEntity) source.getTrueSource();
        ItemStack held = shooter.getHeldItemMainhand();
        if (!(held.getItem() instanceof BowOfKnefmtitiItem)) return;

        LivingEntity target = event.getEntityLiving();
        if (target.world.isRemote) return;

        ServerWorld world = (ServerWorld) target.world;
        LightningBoltEntity bolt = new LightningBoltEntity(EntityType.LIGHTNING_BOLT, world);
        bolt.setPosition(target.getPosX(), target.getPosY(), target.getPosZ());
        world.addEntity(bolt);
    }
}
