# EMC Core Verification

- Minecraft: 26.2
- Java: 25 (Temurin 25.0.3+9)
- Loom: 1.17.14
- Gradle: 9.5.1
- Loader: 0.19.3
- Fabric API: 0.154.2+26.2
- ProjectE baseline: 15d4ce65bd06eb4222709b984255fbf5080e78bc
- Commands: gradlew clean build packageAudit verifyBaseline -Pprojecte_upstream=../.upstream/projecte -Pprojecte_upstream_commit=15d4ce65bd06eb4222709b984255fbf5080e78bc
- Result: PASS (37 tests, 0 failures, 0 errors, 1 skipped — PackagedJarTest only runs under packageAudit)
- Runtime jar audit: PASS (build/libs/projecte-fabric-0.1.0-alpha.1.jar contains metadata, both entrypoints and LICENSE; no porting tooling)
- Deterministic baseline: PASS (verifyBaseline byte-identical to committed docs/porting/baseline/projecte-1.21.1.json)

## Verified behavior

- EMC numeric bounds (EmcValueTest, 3 tests): rejects negatives, checked arithmetic, rejects underflow/overflow.
- Graph parity (EmcGraphMapperTest, 7 tests): set-before propagation, set-after non-propagation, lowest-valid conversion selection, returned/negative ingredients, zero-dependency omission, unresolved cycles and overflow-to-unmapped.
- Normalized stack keys (NormalizedStackKeyTest, 3 tests): item component canonicalization, tag/fake separation, blank-identifier rejection, stable ordering.
- Explicit EMC decoding (ExplicitEmcLoaderTest, 3 tests): deterministic key order, BEFORE/AFTER phases, duplicate-key rejection, negative/overflow rejection, resource-bearing errors.
- Recipe conversions (RecipeConversionCollectorTest, 5 tests): ingredient merging, multi-output division, negative remainders, choice expansion cap, damaged-output exclusion.
- Atomic snapshots (EmcMappingServiceTest, 3 tests): immutable defensive copies, monotonic versions, concurrent-swap safety, rebuild-failure preservation.
- Minecraft stack adaptation (MinecraftStackKeyFactoryTest, 2 tests): registered items + canonical component payloads, empty-stack rejection.
- Reload processing (EmcReloadProcessorTest, 2 tests): explicit + recipe deterministic rebuild, malformed-reload snapshot preservation.
- Reload pipeline (EmcReloadListenerTest, 3 tests): explicit values published from data resources, injected recipe-source conversions solved, malformed reload preserves previous snapshot.
- ProjectEEmc singleton (ProjectEEmcTest, 2 tests): stable service reference, empty pre-reload snapshot.

## Scope notes

- The reload pipeline is fully wired and authoritative. The 26.2 vanilla RecipeManager adapter
  (converting the new RecipeDisplay/SlotDisplay graph into RecipeConversion entries) is intentionally
  registered later as a RecipeConversionSource during the content/mappers phase; the listener already
  consumes any registered source and the empty-source path is verified.
- All public EMC values remain non-negative long EmcValue instances; internal arithmetic uses
  BigFraction only and never appears in published snapshots.
