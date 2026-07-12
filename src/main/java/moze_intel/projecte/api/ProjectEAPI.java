package moze_intel.projecte.api;

import net.minecraft.resources.Identifier;

public final class ProjectEAPI {
    public static final String MOD_ID = "projecte";

    private ProjectEAPI() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
