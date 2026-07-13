package moze_intel.projecte.client;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.client.screen.TransmutationTableScreen;
import moze_intel.projecte.content.ModMenuTypes;
import moze_intel.projecte.emc.ProjectEEmc;
import moze_intel.projecte.network.payloads.EmcMappingSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectEClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectEAPI.MOD_ID + "/client");

    public static final KeyMapping.Category PROJECTE_CATEGORY =
          KeyMapping.Category.register(ProjectEAPI.id("category"));

    public static final KeyMapping CHARGE_KEY = new KeyMapping(
          "key.projecte.charge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, PROJECTE_CATEGORY);

    public static final KeyMapping MODE_KEY = new KeyMapping(
          "key.projecte.mode", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, PROJECTE_CATEGORY);

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenuTypes.TRANSMUTATION_TABLE, TransmutationTableScreen::new);
        EmcTooltipHandler.register();

        // Receive the authoritative EMC mapping from the server and publish it into the client's
        // display cache. Must run on the client thread; the Fabric handler already dispatches there.
        ClientPlayNetworking.registerGlobalReceiver(EmcMappingSyncPayload.TYPE,
              (payload, ctx) -> ctx.client().execute(() ->
                    ProjectEEmc.service().replace(payload.values())));

        LOGGER.info("Initializing ProjectE client for Fabric 26.2");
    }
}
