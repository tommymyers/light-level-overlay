package me.tommymyers.llo;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RailBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;

public class OverlayPoller extends Thread {

    public volatile ArrayList<Overlay>[][] overlays;

    public void run() {
        int radius = 0;
        while (true) {
            int chunkRadius = updateChunkRadius();
            radius = radius % chunkRadius + 1;
            if (LightLevelOverlay.enabled())
                updateLightLevel(radius, chunkRadius);
            try {
                sleep(LightLevelOverlay.POLLING_INTERVAL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int updateChunkRadius() {
        int size = LightLevelOverlay.CHUNK_RADIUS;
        if (overlays == null || overlays.length != size * 2 + 1) {
            overlays = new ArrayList[size * 2 + 1][size * 2 + 1];
            for (int i = 0; i < overlays.length; i++)
                for (int j = 0; j < overlays[i].length; j++)
                    overlays[i][j] = new ArrayList<Overlay>();
        }
        return size;
    }

    private void updateLightLevel(int radius, int chunkRadius) {

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null)
            return;

        ClientWorld world = mc.world;
		int playerPosY = (int)Math.floor(mc.player.y);
		int playerChunkX = mc.player.chunkX;
        int playerChunkZ = mc.player.chunkZ; 
		//int skyLightSub = 15;// TODO find replacement - world.calculateSkylightSubtracted(1.0f);
		int displayMode = LightLevelOverlay.DISPLAY_MODE;
		boolean useSkyLight = LightLevelOverlay.USE_SKY_LIGHT;
		
		for (int chunkX = playerChunkX - radius; chunkX <= playerChunkX + radius; chunkX++)
		for (int chunkZ = playerChunkZ - radius; chunkZ <= playerChunkZ + radius; chunkZ++) {
            Chunk chunk = mc.world.getChunk(chunkX, chunkZ);
            if (!world.isChunkLoaded(chunkX, chunkZ)) continue;
			ArrayList<Overlay> buffer = new ArrayList<Overlay>();
			for (int offsetX = 0; offsetX < 16; offsetX++)
			for (int offsetZ = 0; offsetZ < 16; offsetZ++) {
				int posX = (chunkX << 4) + offsetX;
				int posZ = (chunkZ << 4) + offsetZ;
                int maxY = playerPosY + 4, minY = Math.max(playerPosY - 40, 0);
                BlockState preBlockState = null, curBlockState = chunk.getBlockState(new BlockPos(offsetX, maxY, offsetZ));
				Block preBlock = null, curBlock = curBlockState.getBlock();
				BlockPos prePos = null, curPos = new BlockPos(posX, maxY, posZ);
				for (int posY = maxY - 1; posY >= minY; posY--) {
					preBlockState = curBlockState;
					curBlockState = chunk.getBlockState(new BlockPos(offsetX, posY, offsetZ));
					preBlock = curBlock;
					curBlock = curBlockState.getBlock();
					prePos = curPos;
                    curPos = new BlockPos(posX, posY, posZ);
					if (curBlock == Blocks.AIR ||
						curBlock == Blocks.BEDROCK ||
                        curBlock == Blocks.BARRIER ||
                        curBlock.getMaterial(curBlockState).isLiquid() ||
                        preBlockState.isFullBoundsCubeForCulling() ||
						// TODO find replacement - preBlockState.isBlockNormalCube() ||
                        RailBlock.isRail(preBlockState) ||
                        !Block.isSolidSmallSquare(world, curPos, Direction.UP) ||
						preBlockState.emitsRedstonePower()) {
						//curBlockState.isSideSolid(world, curPos, EnumFacing.UP) == false ||
						continue;
					}
                    double offsetY = 0;
					/*if (preBlock == Blocks.SNOW || preBlock.getMaterial(preBlockState).equals(Material.CARPET)) {
						offsetY = preBlockState.getBoundingBox(world, prePos).maxY;
                        if (offsetY >= 0.15) continue; // Snow layer too high
                        continue;
                    }*/
                    int blockLight = chunk.getLightLevel(prePos, 0, false);
					//int blockLight = chunk.getLightFor(EnumSkyBlock.BLOCK, prePos);
					//int   skyLight = chunk.getLightFor(EnumSkyBlock.SKY, prePos) - skyLightSub;
					int mixedLight = blockLight;//Math.max(blockLight, skyLight);
					int lightIndex = useSkyLight ? mixedLight : blockLight;
					if (displayMode == 1) {
						if (mixedLight >= 8 && blockLight < 8) lightIndex += 32;
					} else if (displayMode == 2) {
						if (blockLight >= 8) continue;
						if (lightIndex >= 8) lightIndex += 32;
					}
					if (lightIndex >= 8 && lightIndex < 24) lightIndex ^= 16;
					buffer.add(new Overlay(posX, posY + offsetY + 1, posZ, lightIndex));
				}
			}
			int len = chunkRadius * 2 + 1;
			int arrayX = (chunkX % len + len) % len;
			int arrayZ = (chunkZ % len + len) % len;
			overlays[arrayX][arrayZ] = buffer;
		}
		
	}
	
}