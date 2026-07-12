package moze_intel.projecte.client;

import moze_intel.projecte.api.ProjectEAPI;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectEClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectEAPI.MOD_ID + "/client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing ProjectE client for Fabric 26.2");
    }
}
