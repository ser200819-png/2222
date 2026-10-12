package com.lololoshka.thirteenflames;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ThirteenFlames.MOD_ID)
public class ModEvents {

    /** Лунный Лук Кнефмтити: попадание стрелы призывает молнию. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        Entity trueSource = event.getSource().getTrueSource();
        if (!(trueSource instanceof AbstractArrowEntity)) return;
        AbstractArrowEntity arrow = (AbstractArrowEntity) trueSource;
        if (!arrow.getPersistentData().getBoolean("TF_LIGHTNING_ARROW")) return;

        LivingEntity target = event.getEntityLiving();
        if (target.world.isRemote) return;

        LightningBoltEntity bolt = new LightningBoltEntity(
                target.world, target.getPosX(), target.getPosY(), target.getPosZ(), false);
        target.world.addEntity(bolt);
    }

    /** Павший из Чёрной Розы: зомби-союзник не атакует игроков. */
    @SubscribeEvent
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getEntity() instanceof ZombieEntity)) return;
        ZombieEntity zombie = (ZombieEntity) event.getEntity();
        if (!zombie.getPersistentData().getBoolean(ThirteenFlames.FALLEN_TAG)) return;

        LivingEntity target = event.getTarget();
        if (target instanceof PlayerEntity) {
            zombie.setAttackTarget(null);
        }
    }
}
