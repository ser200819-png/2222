package com.lololoshka.thirteenflames;

import com.lololoshka.thirteenflames.items.BowOfKnefmtitiItem;
import com.lololoshka.thirteenflames.items.HornOfSeliasetItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

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

    // ---------- «Торнадо» по слову в чате ----------

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String message = event.getMessage().trim();
        if (!message.equalsIgnoreCase("Торнадо")) return;

        ServerPlayerEntity player = event.getPlayer();
        ItemStack held = player.getHeldItemMainhand();
        if (!(held.getItem() instanceof HornOfSeliasetItem)) {
            held = player.getHeldItemOffhand();
            if (!(held.getItem() instanceof HornOfSeliasetItem)) return;
        }
        HornOfSeliasetItem.startTornado(player);
        event.setCanceled(true); // слово не улетает в общий чат
    }

    // ---------- Тик торнадо (серверный тик) ----------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                CompoundNBT data = player.getPersistentData();
                if (!data.contains("tf_tornado_end")) continue;
                if (data.getLong("tf_tornado_end") > world.getGameTime()) {
                    tickTornado(world, player, data);
                } else {
                    endTornado(world, player, data);
                }
            }
        }
    }

    private static void tickTornado(ServerWorld world, ServerPlayerEntity player, CompoundNBT data) {
        // Игрок завис на 4 блока выше точки старта и не двигается
        double x = data.getDouble("tf_tornado_x");
        double y = data.getDouble("tf_tornado_y");
        double z = data.getDouble("tf_tornado_z");
        player.setPositionAndUpdate(x, y, z);
        player.setMotion(Vector3d.ZERO);
        player.fallDistance = 0.0F;

        long gameTime = world.getGameTime();

        // Кольцо вихря радиусом 6 под игроком
        if (gameTime % 2 == 0) {
            double spin = (gameTime % 40) * Math.PI / 20.0;
            for (int i = 0; i < 8; i++) {
                double angle = spin + i * Math.PI / 4.0;
                world.spawnParticle(ParticleTypes.CLOUD,
                        x + Math.cos(angle) * 6.0, y - 2.0, z + Math.sin(angle) * 6.0,
                        2, 0.3, 1.5, 0.3, 0.02);
            }
        }

        // Притягиваем мобов в радиусе 100 в кольцо радиусом 6 и крутим
        double radiusSq = 100.0 * 100.0;
        for (LivingEntity target : world.getEntitiesWithinAABB(LivingEntity.class,
                player.getBoundingBox().grow(100), e -> e != player && e.isAlive())) {
            double dx = target.getPosX() - x;
            double dz = target.getPosZ() - z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist * dist > radiusSq || dist < 0.001) continue;

            double nx = dx / dist;
            double nz = dz / dist;
            double corr = (dist - 6.0) * 0.05;   // доводим до кольца радиусом 6
            double orbit = 0.35;                  // скорость полёта по кругу
            target.setMotion(-nz * orbit - nx * corr, 0.05, nx * orbit - nz * corr);

            if (gameTime % 20 == 0) {
                target.attackEntityFrom(DamageSource.causePlayerDamage(player), 4.0F);
            }
        }
    }

    private static void endTornado(ServerWorld world, ServerPlayerEntity player, CompoundNBT data) {
        // Рог ломается
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        if (main.getItem() instanceof HornOfSeliasetItem) main.shrink(1);
        else if (off.getItem() instanceof HornOfSeliasetItem) off.shrink(1);
        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ITEM_BREAK,
                SoundCategory.PLAYERS, 1.0F, 0.8F);

        // Слабость и тошнота на 20 минут
        player.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 20 * 60 * 20));
        player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 20 * 60 * 20));
        player.sendStatusMessage(new TranslationTextComponent(
                "message.thirteenflames.horn_tornado_end"), true);

        // Погода возвращается, какой была
        net.minecraft.world.storage.IServerWorldInfo info = (net.minecraft.world.storage.IServerWorldInfo) world.getWorldInfo();
        info.setRaining(data.getBoolean("tf_weather_rain"));
        info.setThundering(data.getBoolean("tf_weather_thunder"));



        data.remove("tf_tornado_end");
        data.remove("tf_tornado_x");
        data.remove("tf_tornado_y");
        data.remove("tf_tornado_z");
        data.remove("tf_weather_rain");
        data.remove("tf_weather_thunder");
    }
}
