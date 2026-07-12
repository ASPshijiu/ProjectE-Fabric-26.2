package moze_intel.projecte;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.emc.reload.EmcReloadListener;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectE implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ProjectEAPI.MOD_ID);

    @Override
    public void onInitialize() {
        // The vanilla 26.2 RecipeManager adapter is registered as a recipe-conversion source by
        // the content/mappers phase. Until then the reload pipeline stays wired and authoritative,
        // publishing explicit EMC values and any registered source's conversions.
        new EmcReloadListener(java.util.List.of()).register();
        LOGGER.info("Initializing ProjectE for Fabric 26.2");
    }
}
