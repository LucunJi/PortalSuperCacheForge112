package com.lucunji.superportalcache;

import net.minecraft.block.state.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

import java.util.Comparator;

public class SuperCacheTeleporter extends Teleporter {
    protected static final Comparator<BlockPos> vallinaComparator =
        Comparator.comparing(BlockPos::getX).thenComparing(BlockPos::getZ).thenComparing(p -> -p.getY());

    public SuperCacheTeleporter(WorldServer worldIn) {
        super(worldIn);
    }

    @Override
    public boolean isVanilla() {
        return false;
    }

    @Override
    public boolean placeInExistingPortal(Entity entityIn, float rotationYaw) {
        int i = 128;
        double d0 = -1.0D;
        int j = MathHelper.floor(entityIn.posX);
        int k = MathHelper.floor(entityIn.posZ);
        boolean flag = true;
        BlockPos blockpos = BlockPos.ORIGIN;
        long l = ChunkPos.asLong(j, k);

        //When there is vanilla cache
        if (false) {
//        if (this.destinationCoordinateCache.containsKey(l)) {
            Teleporter.PortalPosition teleporter$portalposition = (Teleporter.PortalPosition) this.destinationCoordinateCache.get(l);
            d0 = 0.0D;
            blockpos = teleporter$portalposition;
            teleporter$portalposition.lastUpdateTime = this.world.getTotalWorldTime();
            flag = false;
        } else {
            //Otherwise, use super cache
            BlockPos searchCenter = new BlockPos(entityIn);
            int maxX = searchCenter.getX() + 128, minX = searchCenter.getX() - 128, maxZ = searchCenter.getZ() + 128, minZ = searchCenter.getZ() - 128;
            for (int cx = (searchCenter.getX() >> 4) - 8; cx <= (searchCenter.getX() >> 4) + 8; ++cx) {
                for (int cz = (searchCenter.getZ() >> 4) - 8; cz <= (searchCenter.getZ() >> 4) + 8; ++cz) {

                    SuperCacheHandler handler;
                    if (entityIn.dimension == DimensionType.NETHER.getId()) {
                        handler = SuperCacheHandler.getHandlerNether();
                    } else {
                        handler = SuperCacheHandler.getHandlerOverworld();
                    }
                    ChunkPos cPos = new ChunkPos(cx, cz);

                    //When chunk is not super-cached, use vanilla method and check portals
                    if (!handler.isMarked(cPos)) {
                        handler.markChunk(cPos);
                        for (int dx = 0; dx < 16; ++dx) {
                            for (int dz = 0; dz < 16; ++dz) {
                                for (int y = world.getActualHeight() - 1; y >= 0; --y) {
                                    BlockPos bPos = new BlockPos(
                                            (cx << 4) + dx,
                                            y,
                                            (cz << 4) + dz);
                                    if (world.getBlockState(bPos).getBlock() == Blocks.PORTAL) {
                                        handler.addPortal(bPos);
                                    }
                                }
                            }
                        }
                    }

                    //Or look into the portal map
                    for (BlockPos portalPos : handler.getChunkPortalIterable(cPos)) {
                        if (portalPos.getX() >= minX
                                && portalPos.getX() <= maxX
                                && portalPos.getZ() >= minZ
                                && portalPos.getZ() <= maxZ) {
                            double d1 = portalPos.distanceSq(searchCenter);
                            if (d0 < 0.0D || d1 < d0 || (d1 == d0 && vallinaComparator.compare(portalPos, blockpos) < 0)) {
                                d0 = d1;
                                blockpos = portalPos;
                            }
                        }
                    }
                }
            }
        }

        if (d0 >= 0.0D) {
            if (flag) {
                this.destinationCoordinateCache.put(l, new Teleporter.PortalPosition(blockpos, this.world.getTotalWorldTime()));
            }

            double d5 = (double) blockpos.getX() + 0.5D;
            double d7 = (double) blockpos.getZ() + 0.5D;
            BlockPattern.PatternHelper blockpattern$patternhelper = Blocks.PORTAL.createPatternHelper(this.world, blockpos);
            boolean flag1 = blockpattern$patternhelper.getForwards().rotateY().getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE;
            double d2 = blockpattern$patternhelper.getForwards().getAxis() == EnumFacing.Axis.X ? (double) blockpattern$patternhelper.getFrontTopLeft().getZ() : (double) blockpattern$patternhelper.getFrontTopLeft().getX();
            double d6 = (double) (blockpattern$patternhelper.getFrontTopLeft().getY() + 1) - entityIn.getLastPortalVec().y * (double) blockpattern$patternhelper.getHeight();

            if (flag1) {
                ++d2;
            }

            if (blockpattern$patternhelper.getForwards().getAxis() == EnumFacing.Axis.X) {
                d7 = d2 + (1.0D - entityIn.getLastPortalVec().x) * (double) blockpattern$patternhelper.getWidth() * (double) blockpattern$patternhelper.getForwards().rotateY().getAxisDirection().getOffset();
            } else {
                d5 = d2 + (1.0D - entityIn.getLastPortalVec().x) * (double) blockpattern$patternhelper.getWidth() * (double) blockpattern$patternhelper.getForwards().rotateY().getAxisDirection().getOffset();
            }

            float f = 0.0F;
            float f1 = 0.0F;
            float f2 = 0.0F;
            float f3 = 0.0F;

            if (blockpattern$patternhelper.getForwards().getOpposite() == entityIn.getTeleportDirection()) {
                f = 1.0F;
                f1 = 1.0F;
            } else if (blockpattern$patternhelper.getForwards().getOpposite() == entityIn.getTeleportDirection().getOpposite()) {
                f = -1.0F;
                f1 = -1.0F;
            } else if (blockpattern$patternhelper.getForwards().getOpposite() == entityIn.getTeleportDirection().rotateY()) {
                f2 = 1.0F;
                f3 = -1.0F;
            } else {
                f2 = -1.0F;
                f3 = 1.0F;
            }

            double d3 = entityIn.motionX;
            double d4 = entityIn.motionZ;
            entityIn.motionX = d3 * (double) f + d4 * (double) f3;
            entityIn.motionZ = d3 * (double) f2 + d4 * (double) f1;
            entityIn.rotationYaw = rotationYaw - (float) (entityIn.getTeleportDirection().getOpposite().getHorizontalIndex() * 90) + (float) (blockpattern$patternhelper.getForwards().getHorizontalIndex() * 90);

            if (entityIn instanceof EntityPlayerMP) {
                ((EntityPlayerMP) entityIn).connection.setPlayerLocation(d5, d6, d7, entityIn.rotationYaw, entityIn.rotationPitch);
            } else {
                entityIn.setLocationAndAngles(d5, d6, d7, entityIn.rotationYaw, entityIn.rotationPitch);
            }

            return true;
        } else {
            return false;
        }
    }
}
