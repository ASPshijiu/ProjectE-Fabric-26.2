# ProjectE Fabric 26.2 Feature Matrix

Baseline: ProjectE 1.21.1 commit 15d4ce65bd06eb4222709b984255fbf5080e78bc

| Category | Upstream evidence | Fabric implementation | Status | Verification | Difference |
| --- | --- | --- | --- | --- | --- |
| Build and metadata | fabric.mod.json, Gradle build | Foundation tasks 1-2 | Verified | gradlew build; PackagedJarTest | None |
| EMC numeric bounds | ProjectE long EMC semantics | EmcValue | Verified | EmcValueTest | Checked overflow is stricter |
| Registration IDs | PEItems, PEBlocks, registries | ModItems (94 items), ModBlocks (21 blocks) | Verified | gradlew build passes; all 115+ registrations compile | NeoForge DeferredRegister replaced with vanilla Registry; all IDs preserved |
| EMC mapping | emc package and data files | Graph solver, normalized keys, explicit loader, recipe conversion (VanillaRecipeConversionSource wired via server recipe source supplier), pe_custom_conversions loader, atomic snapshots, server reload listener | Verified | EMC core evidence; EmcGraphMapperTest, EmcReloadListenerTest, EmcReloadProcessorTest | — |
| Player EMC and knowledge | components and network packages | Fabric attachments (knowledge/emc/input_locks/gem_armor), PlayerDataService, join+reload sync, shared EMC-mapping S2C payload (EmcMappingSyncPayload), /projecte emc+knowledge commands, client read facade | Verified | Player data evidence; PlayerDataServiceTest, PlayerSyncHandlersTest, EmcCommandTest, KnowledgeCommandTest | — |
| Transmutation | table, tablet, stone and world data | World transmutation data model + reload + Philosopher's Stone use-on-block; Transmutation Table block/entity/menu/screen (8 input + lock + consume + unlearn + 16 output ring); Portable Transmutation Tablet; Tome of Knowledge (full-knowledge via inventory tick) | Implemented (functional) | WorldTransmutation tests pass; gradlew build passes; table menu/screen wired with learn/burn/unlearn/extract + EMC spend | Transmutation-table texture refinement (slot squares rendered in code) deferred |
| Machines and storage | block entities and containers | All machine blocks registered (collectors MK1-3, relays MK1-3, condensers MK1-2, furnaces, pedestal, alchemical chest, interdiction torch, nova explosives) | Blocks registered | All block IDs match upstream baseline | Block entities and GUI behaviors deferred to Phase 5 |
| Tools and equipment | item classes, armor and pedestal | All 94 items registered; charge system (ModDataComponents.CHARGE + IItemCharge + V-keybind + ChargeItemPayload C2S); DM/RM Hammer AoE mining; RM Katar/Morning Star AoE combat; Divining Rods (3 tiers, ore scan); Destruction Catalyst charged 3x3 tunnel; ActiveEmcItem rings/amulets/stones (Ignition/BlackHole/Harvest/Swiftwolf/Watch/Zero/Void/Body/Soul/Mind/Life) functional | Implemented (core behaviors) | gradlew build passes; charge/AoE/ring behaviors wired | Remaining plain-item lenses (hyperkinetic_lens, catalytic_lens, etc.), DM/RM armor passives, Gem armor deferred |
| Assets and datagen | frozen baseline categories | 170 textures, 15 sound files, sounds.json, 231 item models, 22 blockstates, 156 recipes, 173 advancements, 21 loot tables, 30 tag files, 4 world transmutations, 2 custom conversions, en_us.json (100+ entries) | Verified | Assets copied from upstream; build passes | Custom recipe serializers (covalence_repair, philo_stone_smelting) and neoforge:components adapters deferred |
| Optional integrations | integration package | Not started | Not started | Compatibility plan | None recorded |

## Status Summary

- **Verified**: Build & metadata, EMC bounds, Registration IDs, EMC mapping (incl. recipe + custom-conversion propagation + client sync), Player data (incl. shared-mapping S2C payload), Assets
- **Implemented (functional)**: Transmutation (world + table GUI + tablet + Tome), Tools/Equipment (charge system + DM/RM hammer AoE + RM katar/morning-star AoE + divining rods + Destruction Catalyst + ActiveEmcItem rings/amulets/stones)
- **Implemented (partial)**: Machines (blocks only)
- **Not started**: Full machine behaviors, remaining plain-item lenses, DM/RM/Gem armor passives, integrations

## Verification Gate

```
gradlew clean build packageAudit → BUILD SUCCESSFUL
All unit tests pass
All 115+ items register with upstream-matching projecte:* IDs
All 21 blocks register with upstream-matching projecte:* IDs
All 170 textures, 15 sounds, 231 item models, 22 blockstates present
All 156 recipes, 173 advancements, 21 loot tables, 30 tag files present
```
