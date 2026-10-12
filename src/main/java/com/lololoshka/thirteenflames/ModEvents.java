package com.lololoshka.thirteenflames;

import com.lololoshka.thirteenflames.items.BowOfKnefmtitiItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Mod.EventBusSubscriber(modid = ThirteenFlames.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    private static final Set<Integer> LIGHTNING_ARROWS = new HashSet<>();
    private static final RedstoneParticleData BLUE_RING = new RedstoneParticleData(0.35F, 0.55F, 1.0F, 1.0F);

    public static void markLightningArrow(ArrowEntity arrow) {
        LIGHTNING_ARROWS.add(arrow.getEntityId());
    }

    // Молния при попадании стрелы в моба ИЛИ в блок
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getEntity() instanceof ArrowEntity)) return;
        ArrowEntity arrow = (ArrowEntity) event.getEntity();
        if (!LIGHTNING_ARROWS.remove(arrow.getEntityId())) return;
        if (arrow.world.isRemote) return;

        ServerWorld world = (ServerWorld) arrow.world;
        LightningBoltEntity bolt = new LightningBoltEntity(EntityType.LIGHTNING_BOLT, world);
        bolt.setPosition(arrow.getPosX(), arrow.getPosY(), arrow.getPosZ());
        world.addEntity(bolt);
    }

    // Заряд «Града молний»: игрок обездвижен, три голубых кольца, затем град стрел с молниями
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PlayerEntity player = event.player;
        if (player.world.isRemote) return;
        CompoundNBT data = player.getPersistentData();
        if (!data.contains("TfChannelStart")) return;

        ServerWorld world = (ServerWorld) player.world;
        long start = data.getLong("TfChannelStart");
        long elapsed = world.getGameTime() - start;

        // игрок не может двигаться
        player.setMotion(Vector3d.ZERO);
        player.fallDistance = 0.0F;

        double cx = data.getDouble("TfChannelX");
        double cy = data.getDouble("TfChannelY");
        double cz = data.getDouble("TfChannelZ");

        if (elapsed < BowOfKnefmtitiItem.CHANNEL_TICKS) {
            // три голубых кольца диаметром 5 блоков на высоте 15 блоков над точкой
            for (int ring = 0; ring < 3; ring++) {
                double ry = cy + 15.0D - ring * 0.4D;
                for (int i = 0; i < 24; i++) {
                    double angle = (Math.PI * 2 * i) / 24.0D + elapsed * 0.08D + ring * 0.7D;
                    world.spawnParticle(BLUE_RING,
                            cx + Math.cos(angle) * 2.5D, ry, cz + Math.sin(angle) * 2.5D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        } else {
            data.remove("TfChannelStart");
            // град стрел: 15-20 штук, каждая бьёт молнией при попадании
            Random rand = world.rand;
            int count = 15 + rand.nextInt(6);
            for (int i = 0; i < count; i++) {
                double angle = rand.nextDouble() * Math.PI * 2.0D;
                double dist = rand.nextDouble() * 2.5D;
                ArrowEntity arrow = new ArrowEntity(world, player);
                arrow.setPosition(cx + Math.cos(angle) * dist, cy + 15.0D, cz + Math.sin(angle) * dist);
                arrow.shoot(0.0D, -1.0D, 0.0D, 2.5F, 0.2F);
                arrow.setDamage(BowOfKnefmtitiItem.ARROW_DAMAGE);
                arrow.pickupStatus = ArrowEntity.PickupStatus.CREATIVE_ONLY;
                world.addEntity(arrow);
                LIGHTNING_ARROWS.add(arrow.getEntityId());
            }
            player.sendStatusMessage(new TranslationTextComponent("message.thirteenflames.bow_ready"), true);
        }
    }
}
