package me.tommymyers.llo;

import org.lwjgl.glfw.GLFW;

import me.shedaniel.cloth.hooks.ClothClientHooks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class LightLevelOverlay implements ClientModInitializer {

    private static final String KEYBIND_CATEGORY = "Light Level Overlay";
    private static final Identifier ENABLE_OVERLAY_KEYBIND = new Identifier("lightleveloverlay", "enable_overlay");
    private static FabricKeyBinding enableOverlay;
    private static boolean enabled = false;

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
                .create(ENABLE_OVERLAY_KEYBIND, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, KEYBIND_CATEGORY).build());
        ClothClientHooks.HANDLE_INPUT.register(minecraftClient -> {
            while (enableOverlay.wasPressed())
                enabled = !enabled;
        });
        launchPoller();
        ClothClientHooks.DEBUG_RENDER_PRE.register(() -> {
            if (LightLevelOverlay.enabled) {
                Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                PlayerEntity player = client.player;
                if (player == null)
                    return;

                double x = camera.getPos().x;
                double y = camera.getPos().y - 0.005D;
                double z = camera.getPos().z;
                renderer.render(x, y, z, poller.overlays);
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

    public static boolean enabled() {
        return enabled;
    }

}