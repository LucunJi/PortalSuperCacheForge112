package com.lucunji.superportalcache;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber
public class PortalEventHandler {

    @SubscribeEvent
    public static void onPortalBlockEvent(BlockEvent e) {
        if (e.getWorld() instanceof WorldServer && e.getPos().getY() < e.getWorld().provider.getActualHeight()) {
            SuperCacheHandler handler;
            if (e.getWorld().provider.getDimension() == -1) {
                handler = SuperCacheHandler.getHandlerNether();
            } else if (e.getWorld().provider.getDimension() == 0) {
                handler = SuperCacheHandler.getHandlerOverworld();
            } else {
                return;
            }
            if (handler.isMarked(new ChunkPos(e.getPos()))) {
                if (e.getState().getBlock() == Blocks.PORTAL) {
                    handler.addPortal(e.getPos());
                } else {
                    handler.removePortal(e.getPos());
                }
            }

        }
    }

    @SubscribeEvent
    public static void onPortalTeleport(EntityTravelToDimensionEvent e) throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Entity entityEvent = e.getEntity();
        if (entityEvent.dimension == 0 && e.getDimension() == -1 || entityEvent.dimension == -1 && e.getDimension() == 0) {
            if (entityEvent instanceof EntityPlayerMP) {
                onPlayerTeleport(e);
            } else {
                onEntityTeleport(e);
            }
            e.setCanceled(true);
        }
    }

    private static void onEntityTeleport(EntityTravelToDimensionEvent e) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Entity entityEvent = e.getEntity();
        Teleporter teleporter = new SuperCacheTeleporter(entityEvent.getServer().getWorld(e.getDimension()));

        entityEvent.world.profiler.startSection("changeDimension");
        MinecraftServer minecraftserver = entityEvent.getServer();
        int i = entityEvent.dimension;
        WorldServer worldserver = minecraftserver.getWorld(i);
        WorldServer worldserver1 = minecraftserver.getWorld(e.getDimension());
        entityEvent.dimension = e.getDimension();

        entityEvent.world.removeEntity(entityEvent);
        entityEvent.isDead = false;
        entityEvent.world.profiler.startSection("reposition");
        BlockPos blockpos;

        double moveFactor = worldserver.provider.getMovementFactor() / worldserver1.provider.getMovementFactor();
        double d0 = MathHelper.clamp(entityEvent.posX * moveFactor, worldserver1.getWorldBorder().minX() + 16.0D, worldserver1.getWorldBorder().maxX() - 16.0D);
        double d1 = MathHelper.clamp(entityEvent.posZ * moveFactor, worldserver1.getWorldBorder().minZ() + 16.0D, worldserver1.getWorldBorder().maxZ() - 16.0D);
        double d2 = 8.0D;

        if (false && e.getDimension() == -1) {
            d0 = MathHelper.clamp(d0 / 8.0D, worldserver1.getWorldBorder().minX() + 16.0D, worldserver1.getWorldBorder().maxX() - 16.0D);
            d1 = MathHelper.clamp(d1 / 8.0D, worldserver1.getWorldBorder().minZ() + 16.0D, worldserver1.getWorldBorder().maxZ() - 16.0D);
        } else if (false && e.getDimension() == 0) {
            d0 = MathHelper.clamp(d0 * 8.0D, worldserver1.getWorldBorder().minX() + 16.0D, worldserver1.getWorldBorder().maxX() - 16.0D);
            d1 = MathHelper.clamp(d1 * 8.0D, worldserver1.getWorldBorder().minZ() + 16.0D, worldserver1.getWorldBorder().maxZ() - 16.0D);
        }

        d0 = (double) MathHelper.clamp((int) d0, -29999872, 29999872);
        d1 = (double) MathHelper.clamp((int) d1, -29999872, 29999872);
        float f = entityEvent.rotationYaw;
        entityEvent.setLocationAndAngles(d0, entityEvent.posY, d1, 90.0F, 0.0F);
        teleporter.placeEntity(worldserver1, entityEvent, f);
        blockpos = new BlockPos(entityEvent);

        worldserver.updateEntityWithOptionalForce(entityEvent, false);
        entityEvent.world.profiler.endStartSection("reloading");
        Entity entity = EntityList.newEntity(entityEvent.getClass(), worldserver1);

        if (entity != null) {
            Method method = Entity.class.getDeclaredMethod("func_180432_n", Entity.class);
//            Method method = Entity.class.getDeclaredMethod("copyDataFromOld", Entity.class);
            method.setAccessible(true);
            method.invoke(entity, entityEvent);

            entity.moveToBlockPosAndAngles(blockpos, entity.rotationYaw, entity.rotationPitch);

            boolean flag = entity.forceSpawn;
            entity.forceSpawn = true;
            worldserver1.spawnEntity(entity);
            entity.forceSpawn = flag;
            worldserver1.updateEntityWithOptionalForce(entity, false);
        }

        entityEvent.isDead = true;
        entityEvent.world.profiler.endSection();
        worldserver.resetUpdateEntityTick();
        worldserver1.resetUpdateEntityTick();
        entityEvent.world.profiler.endSection();
    }

    private static void onPlayerTeleport(EntityTravelToDimensionEvent e) throws IllegalAccessException, NoSuchFieldException {
        EntityPlayerMP player = ((EntityPlayerMP) e.getEntity());
        Teleporter teleporter = new SuperCacheTeleporter(player.getServer().getWorld(e.getDimension()));

        Field enteredNetherPositionField = EntityPlayerMP.class.getDeclaredField("field_193110_cw");
//        Field enteredNetherPositionField = EntityPlayerMP.class.getDeclaredField("enteredNetherPosition");
        enteredNetherPositionField.setAccessible(true);

        if (player.dimension == 0 && e.getDimension() == -1) {
            enteredNetherPositionField.set(player, new Vec3d(player.posX, player.posY, player.posZ));
//            player.enteredNetherPosition = new Vec3d(this.posX, this.posY, this.posZ);
        } else if (player.dimension != -1 && e.getDimension() != 0) {
            enteredNetherPositionField.set(player, null);
//            player.enteredNetherPosition = null;
        }

        player.mcServer.getPlayerList().transferPlayerToDimension(player, e.getDimension(), teleporter);
        player.connection.sendPacket(new SPacketEffect(1032, BlockPos.ORIGIN, 0, false));
        Field lastExperienceField = EntityPlayerMP.class.getDeclaredField("field_71144_ck");
        Field lastHealthField = EntityPlayerMP.class.getDeclaredField("field_71149_ch");
        Field lastFoodLevelField = EntityPlayerMP.class.getDeclaredField("field_71146_ci");
//        Field lastExperienceField = EntityPlayerMP.class.getDeclaredField("lastExperience");
//        Field lastHealthField = EntityPlayerMP.class.getDeclaredField("lastHealth");
//        Field lastFoodLevelField = EntityPlayerMP.class.getDeclaredField("lastFoodLevel");
        lastExperienceField.setAccessible(true);
        lastHealthField.setAccessible(true);
        lastFoodLevelField.setAccessible(true);
        lastExperienceField.setInt(player, -1);
        lastHealthField.setFloat(player, -1.0F);
        lastFoodLevelField.setInt(player, -1);
//        player.lastExperience = -1;
//        player.lastHealth = -1.0F;
//        player.lastFoodLevel = -1;
    }
}
