package moze_intel.projecte.emc;

/**
 * Server-owned entrypoint for the resolved EMC mapping.
 *
 * <p>The {@link EmcMappingService} held here is the single authoritative source of EMC values for
 * the running logical server. The service starts with an empty snapshot and is atomically replaced
 * by {@code EmcReloadListener} whenever server data packs reload. Client code must never mutate the
 * service directly; it only reads {@link EmcMappingSnapshot} instances published by the server.
 */
public final class ProjectEEmc {
    private static final EmcMappingService<NormalizedStackKey> SERVICE = new EmcMappingService<>();

    private ProjectEEmc() {
    }

    /**
     * @return the shared, never-{@code null} authoritative EMC mapping service.
     */
    public static EmcMappingService<NormalizedStackKey> service() {
        return SERVICE;
    }
}
