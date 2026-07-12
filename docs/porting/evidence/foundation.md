# Foundation Verification

Date: 2026-07-12
Branch: port/foundation

- Minecraft: 26.2
- Java: 25.0.3
- Loom: 1.17.14
- Gradle: 9.5.1
- Loader: 0.19.3
- Fabric API: 0.154.2+26.2
- ProjectE baseline: 15d4ce65bd06eb4222709b984255fbf5080e78bc
- Command: gradlew clean build packageAudit verifyBaseline --warning-mode all
- Result: PASS; 13 Gradle actions executed
- Unit suite: 7 discovered, 6 executed, 1 package-only test skipped, 0 failures
- Runtime jar audit: 1 executed, 0 failures
- Deterministic baseline: PASS
- Gradle deprecation audit: PASS with no warnings under --warning-mode all
- Runtime jar: build/libs/projecte-fabric-0.1.0-alpha.1.jar

## Frozen baseline counts

- Advancements: 173
- Blockstates: 22
- Custom conversions: 2
- Item models: 250
- Loot tables: 21
- Recipes: 156
- Sounds: 15
- Tags: 30
- Textures: 170
- World transmutations: 4

## Remaining feature-matrix categories

- Registration IDs and content registries
- EMC mapping and recipe inference
- Player EMC, knowledge, persistence and networking
- Transmutation table, tablet, stone and world transformations
- Machines, storage and automation
- Tools, equipment, rings, amulets and pedestal behavior
- Full content data generation and assets
- Optional integrations and NeoForge data migration
- Client, dedicated server, multiplayer and survival-flow final verification
