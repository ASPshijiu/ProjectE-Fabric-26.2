package moze_intel.projecte;

import moze_intel.projecte.api.ProjectEAPI;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectE implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ProjectEAPI.MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ProjectE for Fabric 26.2");
    }
}
