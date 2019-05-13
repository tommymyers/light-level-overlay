package me.tommymyers.llo;

import java.awt.Color;

import com.mojang.blaze3d.platform.GlStateManager;

import me.shedaniel.cloth.hooks.ClothClientHooks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;

public class LightLevelOverlay implements ClientModInitializer {

    private static final String KEYBIND_CATEGORY = "key.lightleveloverlay.category";
    private static final Identifier ENABLE_OVERLAY_KEYBIND = new Identifier("lightleveloverlay", "enable_overlay");
    /*
     * private static final BoundingBox TEST_BOX = new BoundingBox(0.6D / 2D, 0,
     * 0.6D / 2D, 1D - 0.6D / 2D, 1D, 1D - 0.6D / 2D);
     */
    private static FabricKeyBinding enableOverlay;
    private static boolean enabled = false;
    // private static int reach = 12;

    public static final int POLLING_INTERVAL = 200;
    public static final int CHUNK_RADIUS = 3;
    public static final int DISPLAY_MODE = 0;
    public static final boolean USE_SKY_LIGHT = false;

    public OverlayRenderer renderer;
    public OverlayPoller poller;

    @Override
    public void onInitializeClient() {
        MinecraftClient client = MinecraftClient.getInstance();
        renderer = new OverlayRenderer();
        poller = new OverlayPoller();
        KeyBindingRegistryImpl.INSTANCE.addCategory(KEYBIND_CATEGORY);
        KeyBindingRegistryImpl.INSTANCE.register(enableOverlay = FabricKeyBinding.Builder
                .create(ENABLE_OVERLAY_KEYBIND, InputUtil.Type.SCANCODE, 296, KEYBIND_CATEGORY).build());
        ClothClientHooks.HANDLE_INPUT.register(minecraftClient -> {
            while (enableOverlay.wasPressed())
                enabled = !enabled;
        });
        launchPoller();
        ClothClientHooks.DEBUG_RENDER_PRE.register(() -> {
            if (LightLevelOverlay.enabled) {
                Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                ClientPlayerEntity player = client.player;
                if (player == null)
                    return;

                //double d0 = camera.getPos().x;
                //double d1 = camera.getPos().y - .005D;
                //double d2 = camera.getPos().z;

                double x = camera.getPos().x;//player.prevX + (player.x - player.prevX);// * event.getPartialTicks();
                double y = camera.getPos().y - 0.005D;//player.prevY + (player.y - player.prevY);// * event.getPartialTicks();
                double z = camera.getPos().z;//player.prevZ + (player.z - player.prevZ);// * event.getPartialTicks();
                renderer.render(x, y, z, poller.overlays);
                /*
                 * PlayerEntity playerEntity = client.player; World world = client.world;
                 * GlStateManager.disableTexture(); GlStateManager.disableBlend(); BlockPos
                 * playerPos = new BlockPos(playerEntity.x, playerEntity.y, playerEntity.z);
                 * BlockPos.iterate(playerPos.add(-reach, -reach, -reach), playerPos.add(reach,
                 * reach, reach)) .forEach(pos -> { if (world.getBiome(pos).getMaxSpawnLimit() >
                 * 0) { CrossType type = getCrossType(pos, world, playerEntity); if (type !=
                 * CrossType.NONE) { //VoxelShape shape =
                 * world.getBlockState(pos).getCollisionShape(world, pos); Color color = type ==
                 * CrossType.RED ? Color.RED : Color.YELLOW; renderCross(pos, color,
                 * playerEntity); } } }); GlStateManager.enableBlend();
                 * GlStateManager.enableTexture();
                 */
            }
        });
    }

    private void launchPoller() {
        for (int i = 0; i < 3; i++) {
            if (poller.isAlive())
                return;
            try {
                poller.start();
            } catch (Exception e) {
                e.printStackTrace();
                poller = new OverlayPoller();
            }
        }
    }

    public static CrossType getCrossType(BlockPos pos, World world, PlayerEntity playerEntity) {
        BlockState blockBelowState = world.getBlockState(pos.down());
        if (blockBelowState.getBlock() == Blocks.BEDROCK || blockBelowState.getBlock() == Blocks.BARRIER)
            return CrossType.NONE;
        if ((!blockBelowState.getMaterial().blocksLight() && blockBelowState.isTranslucent(world, pos.down()))
                || !SpawnHelper.isClearForSpawn(world, pos, world.getBlockState(pos), world.getFluidState(pos)))
            return CrossType.NONE;
        if (blockBelowState.isAir() || !world.getBlockState(pos).isAir()
                || !blockBelowState.hasSolidTopSurface(world, pos, playerEntity)
                || !world.getFluidState(pos.down()).isEmpty())
            return CrossType.NONE;
        if (world.getLightLevel(LightType.BLOCK, pos) >= 8)
            return CrossType.NONE;
        if (world.getLightLevel(LightType.SKY, pos) >= 8)
            return CrossType.YELLOW;
        return CrossType.RED;
    }

    public static void renderCross(BlockPos pos, Color color, PlayerEntity entity) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        GlStateManager.lineWidth(1.0F);
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBufferBuilder();
        double d0 = camera.getPos().x;
        double d1 = camera.getPos().y - .005D;
        double d2 = camera.getPos().z;

        buffer.begin(1, VertexFormats.POSITION_COLOR);
        buffer.vertex(pos.getX() + .01 - d0, pos.getY() - d1, pos.getZ() + .01 - d2)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        buffer.vertex(pos.getX() - .01 + 1 - d0, pos.getY() - d1, pos.getZ() - .01 + 1 - d2)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        buffer.vertex(pos.getX() - .01 + 1 - d0, pos.getY() - d1, pos.getZ() + .01 - d2)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        buffer.vertex(pos.getX() + .01 - d0, pos.getY() - d1, pos.getZ() - .01 + 1 - d2)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture();
    }

    public static boolean enabled() {
        return enabled;
    }

    private static enum CrossType {
        YELLOW, RED, NONE
    }

}