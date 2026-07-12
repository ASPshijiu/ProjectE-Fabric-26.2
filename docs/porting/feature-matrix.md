# ProjectE Fabric 26.2 Feature Matrix

Baseline: ProjectE 1.21.1 commit 15d4ce65bd06eb4222709b984255fbf5080e78bc

| Category | Upstream evidence | Fabric implementation | Status | Verification | Difference |
| --- | --- | --- | --- | --- | --- |
| Build and metadata | fabric.mod.json, Gradle build | Foundation tasks 1-2 | Verified | gradlew build; PackagedJarTest | None |
| EMC numeric bounds | ProjectE long EMC semantics | EmcValue | Verified | EmcValueTest | Checked overflow is stricter |
| Registration IDs | PEItems, PEBlocks, registries | Not started | Not started | Frozen baseline | None recorded |
| EMC mapping | emc package and data files | Graph solver, normalized keys, explicit loader, recipe conversion, atomic snapshots, server reload listener | Verified | EMC core evidence; EmcGraphMapperTest, EmcReloadListenerTest, EmcReloadProcessorTest | Vanilla 26.2 RecipeManager adapter deferred to content/mappers phase as a RecipeConversionSource |
| Player EMC and knowledge | components and network packages | Not started | Not started | Player data plan | None recorded |
| Transmutation | table, tablet, stone and world data | Not started | Not started | Transmutation plan | None recorded |
| Machines and storage | block entities and containers | Not started | Not started | Machines plan | None recorded |
| Tools and equipment | item classes, armor and pedestal | Not started | Not started | Equipment plan | None recorded |
| Assets and datagen | frozen baseline categories | Not started | Not started | Content plan | None recorded |
| Optional integrations | integration package | Not started | Not started | Compatibility plan | None recorded |
