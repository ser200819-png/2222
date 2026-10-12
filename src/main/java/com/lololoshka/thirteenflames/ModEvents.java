package com.lololoshka.thirteenflames;

import com.lololoshka.thirteenflames.items.BowOfKnefmtitiItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
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
    private static final RedstoneParticleData RING_DUST = new RedstoneParticleData(0.30F, 0.60F, 1.00F, 1.3F);

    public static void markLightningArrow(ArrowEntity arrow) {
        LIGHTNING_ARROWS.add(arrow.getEntityId());
    }

    // Молния при попадании стрелы в моба ИЛИ в блок + визуал удара
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

        world.spawnParticle(ParticleTypes.FIREWORK,
                arrow.getPosX(), arrow.getPosY(), arrow.getPosZ(), 10, 0.3D, 0.3D, 0.3D, 0.08D);
        world.playSound(null, arrow.getPosX(), arrow.getPosY(), arrow.getPosZ(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 0.7F, 1.2F);
    }

    // «Град молний»: обездвиживание, три голубых кольца, затем град стрел с молниями
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PlayerEntity player = event.player;
        if (player.world.isRemote) return;
        CompoundNBT d = player.getPersistentData();
        if (!d.contains("TfChannelStart")) return;

        ServerWorld world = (ServerWorld) player.world;
        long elapsed = world.getGameTime() - d.getLong("TfChannelStart");

        // жёсткая фиксация игрока: сервер возвращает его на точку-якорь каждый тик
        player.setPositionAndUpdate(d.getDouble("TfAnchorX"), d.getDouble("TfAnchorY"), d.getDouble("TfAnchorZ"));
        player.setMotion(Vector3d.ZERO);
        player.fallDistance = 0.0F;

        double cx = d.getDouble("TfChannelX");
        double cy = d.getDouble("TfChannelY");
        double cz = d.getDouble("TfChannelZ");

        if (elapsed < BowOfKnefmtitiItem.CHANNEL_TICKS) {
            // три голубых кольца диаметром 5 блоков на высоте 15 блоков
            for (int ring = 0; ring < 3; ring++) {
                double ry = cy + 15.0D - ring * 0.6D;
                for (int i = 0; i < 30; i++) {
                    double angle = Math.PI * 2 * i / 30.0D + elapsed * 0.1D + ring * 0.7D;
                    world.spawnParticle(RING_DUST,
                            cx + Math.cos(angle) * 2.5D, ry, cz + Math.sin(angle) * 2.5D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            // колонна частиц от земли вверх — чтобы заряд было видно издалека
            world.spawnParticle(ParticleTypes.END_ROD, cx, cy + 1.0D + elapsed * 0.25D, cz, 1, 0.1D, 0.0D, 0.1D, 0.0D);
            if (elapsed % 10 == 0) {
                world.playSound(null, cx, cy, cz, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 1.0F, 1.5F);
            }
        } else {
            d.remove("TfChannelStart");
            world.playSound(null, cx, cy, cz, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 2.0F, 1.0F);

            Random rand = world.rand;
            int count = 15 + rand.nextInt(6); // 15-20 стрел
            for (int i = 0; i < count; i++) {
                double angle = rand.nextDouble() * Math.PI * 2.0D;
                double dist = rand.nextDouble() * 2.5D;
                ArrowEntity arrow = new ArrowEntity(world, player);
                arrow.setPosition(cx + Math.cos(angle) * dist, cy + 15.0D, cz + Math.sin(angle) * dist);
                arrow.shoot(0.0D, -1.6D, 0.0D, 2.5F, 0.1F);
                arrow.setDamage(BowOfKnefmtitiItem.ARROW_DAMAGE_MAX);
                arrow.pickupStatus = ArrowEntity.PickupStatus.CREATIVE_ONLY;
                world.addEntity(arrow);
                LIGHTNING_ARROWS.add(arrow.getEntityId());
            }
            player.sendStatusMessage(new StringTextComponent("§9Град молний обрушился!"), true);
        }
    }
}
