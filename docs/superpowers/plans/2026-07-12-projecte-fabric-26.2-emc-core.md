# ProjectE Fabric 26.2 EMC Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Port ProjectE's loader-neutral EMC graph, normalized keys, explicit values, recipe conversions, atomic snapshots, and server reload pipeline to Fabric 26.2 without changing ProjectE 1.21.1 calculation semantics.

**Architecture:** Keep graph calculation independent from Minecraft registries by using generic mapping interfaces and an internal BigFraction arithmetic layer. Adapt Minecraft items, components, recipes, and resource reloads at the boundary; publish only immutable EmcMappingSnapshot instances through a server-owned EmcMappingService.

**Tech Stack:** Java 25, Fabric 26.2, JUnit 5.11.4, Apache Commons Math 3.6.1 BigFraction, Gson 2.14.0, Fabric Resource Loader v1.

## Global Constraints

- ProjectE commit 15d4ce65bd06eb4222709b984255fbf5080e78bc remains the behavioral baseline.
- Preserve set-before propagation, set-after non-propagation, multiple-recipe minimum selection, zero dependencies, negative returned ingredients, free sentinel semantics, cycles, and overflow-to-unmapped behavior.
- Final public EMC values remain non-negative long values represented by EmcValue.
- Internal fractional values may use BigFraction; no floating-point arithmetic is allowed.
- Reload failure must preserve the previous valid snapshot.
- Client code must not calculate authoritative EMC.
- Every production behavior is introduced after a failing focused test.

---

## File Structure

- src/main/java/moze_intel/projecte/api/mapper/EmcMappingCollector.java — loader-neutral conversion collection API.
- src/main/java/moze_intel/projecte/api/mapper/EmcValueGenerator.java — immutable value generation API.
- src/main/java/moze_intel/projecte/emc/graph/EmcGraphMapper.java — ProjectE-compatible conversion graph.
- src/main/java/moze_intel/projecte/emc/graph/EmcConversion.java — validated conversion edge.
- src/main/java/moze_intel/projecte/emc/graph/FractionalEmcArithmetic.java — zero/free/fraction operations.
- src/main/java/moze_intel/projecte/emc/graph/LongToFractionCollector.java — long-facing collector adapter.
- src/main/java/moze_intel/projecte/emc/graph/FractionToLongGenerator.java — final checked long conversion.
- src/main/java/moze_intel/projecte/emc/NormalizedStackKey.java — stable item/tag/fake key contract.
- src/main/java/moze_intel/projecte/emc/ItemStackKey.java — item identifier plus canonical component payload.
- src/main/java/moze_intel/projecte/emc/TagStackKey.java — registry tag key.
- src/main/java/moze_intel/projecte/emc/FakeStackKey.java — internal synthetic group key.
- src/main/java/moze_intel/projecte/emc/EmcMappingSnapshot.java — immutable resolved mapping with version.
- src/main/java/moze_intel/projecte/emc/EmcMappingService.java — atomic snapshot owner.
- src/main/java/moze_intel/projecte/emc/data/ExplicitEmcEntry.java — decoded set-before/set-after entry.
- src/main/java/moze_intel/projecte/emc/data/ExplicitEmcLoader.java — data/projecte/emc JSON loader.
- src/main/java/moze_intel/projecte/emc/recipe/RecipeConversion.java — loader-neutral recipe conversion.
- src/main/java/moze_intel/projecte/emc/recipe/RecipeConversionCollector.java — 26.2 recipe adaptation boundary.
- src/main/java/moze_intel/projecte/emc/reload/EmcReloadListener.java — prepare/apply resource reload transaction.
- src/test/java/moze_intel/projecte/emc/* — graph, normalized key, explicit data, snapshot, recipe and reload tests.

---

### Task 1: Add Fraction Arithmetic Dependency and Port Graph Parity Tests

**Files:**
- Modify: gradle.properties
- Modify: build.gradle
- Create: src/test/java/moze_intel/projecte/emc/graph/EmcGraphMapperTest.java

**Interfaces:**
- Consumes: EmcValue long compatibility boundary.
- Produces: failing behavioral specification for EmcMappingCollector<String> and EmcValueGenerator<String>.

- [ ] **Step 1: Add dependency version and nested runtime dependency**

Add to gradle.properties:

~~~properties
commons_math_version=3.6.1
~~~

Add to dependencies:

~~~groovy
implementation(include("org.apache.commons:commons-math3:${commons_math_version}"))
~~~

- [ ] **Step 2: Write graph parity tests before graph production code**

Create EmcGraphMapperTest with these tests and exact expectations:

~~~java
package moze_intel.projecte.emc.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.List;
import java.util.Map;
import moze_intel.projecte.api.mapper.EmcMappingCollector;
import moze_intel.projecte.api.mapper.EmcValueGenerator;
import moze_intel.projecte.emc.EmcValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmcGraphMapperTest {
    private EmcMappingCollector<String> collector;
    private EmcValueGenerator<String> generator;

    @BeforeEach
    void setUp() {
        EmcGraphMapper<String> mapper = EmcGraphMapper.create();
        collector = mapper.collector();
        generator = mapper.generator();
    }

    @Test
    void derivesSimpleAndMultiOutputValues() {
        collector.setValueBefore("a", EmcValue.of(1));
        collector.addConversion(1, "b", Map.of("a", 2));
        collector.addConversion(2, "c", Map.of("b", 2));
        Map<String, EmcValue> values = generator.generate();
        assertEquals(EmcValue.of(1), values.get("a"));
        assertEquals(EmcValue.of(2), values.get("b"));
        assertEquals(EmcValue.of(2), values.get("c"));
    }

    @Test
    void choosesLowestValidConversion() {
        collector.setValueBefore("a", EmcValue.of(2));
        collector.setValueBefore("b", EmcValue.of(9));
        collector.addConversion(1, "out", Map.of("a", 3));
        collector.addConversion(1, "out", Map.of("b", 1));
        assertEquals(EmcValue.of(6), generator.generate().get("out"));
    }

    @Test
    void appliesSetAfterWithoutPropagation() {
        collector.setValueBefore("a", EmcValue.of(2));
        collector.addConversion(1, "b", Map.of("a", 2));
        collector.addConversion(1, "c", Map.of("b", 2));
        collector.setValueAfter("b", EmcValue.of(100));
        Map<String, EmcValue> values = generator.generate();
        assertEquals(EmcValue.of(100), values.get("b"));
        assertEquals(EmcValue.of(8), values.get("c"));
    }

    @Test
    void supportsReturnedIngredientsAndZeroDependencies() {
        collector.setValueBefore("container", EmcValue.of(5));
        collector.setValueBefore("content", EmcValue.of(10));
        collector.addConversion(1, "filled", Map.of("container", 1, "content", 1));
        collector.addConversion(1, "content", Map.of("filled", 1, "container", -1));
        collector.addConversion(1, "dependent", Map.of("content", 1, "missing", 0));
        Map<String, EmcValue> values = generator.generate();
        assertEquals(EmcValue.of(15), values.get("filled"));
        assertFalse(values.containsKey("dependent"));
    }

    @Test
    void leavesUnresolvedCyclesAndOverflowUnmapped() {
        collector.addConversion(1, "x", Map.of("y", 1));
        collector.addConversion(1, "y", Map.of("x", 1));
        collector.setValueBefore("max", EmcValue.of(Long.MAX_VALUE));
        collector.addConversion(1, "overflow", Map.of("max", 2));
        Map<String, EmcValue> values = generator.generate();
        assertFalse(values.containsKey("x"));
        assertFalse(values.containsKey("y"));
        assertFalse(values.containsKey("overflow"));
    }
}
~~~

- [ ] **Step 3: Verify RED**

Run: gradlew test --tests moze_intel.projecte.emc.graph.EmcGraphMapperTest

Expected: compilation fails because mapper APIs and EmcGraphMapper do not exist.

- [ ] **Step 4: Commit the red tests and dependency declaration only after recording the expected failure**

Commit message: test: define ProjectE EMC graph parity

---

### Task 2: Port the Loader-Neutral Graph Solver

**Files:**
- Create: src/main/java/moze_intel/projecte/api/mapper/EmcMappingCollector.java
- Create: src/main/java/moze_intel/projecte/api/mapper/EmcValueGenerator.java
- Create: src/main/java/moze_intel/projecte/emc/graph/EmcConversion.java
- Create: src/main/java/moze_intel/projecte/emc/graph/FractionalEmcArithmetic.java
- Create: src/main/java/moze_intel/projecte/emc/graph/EmcGraphMapper.java
- Create: src/main/java/moze_intel/projecte/emc/graph/LongToFractionCollector.java
- Create: src/main/java/moze_intel/projecte/emc/graph/FractionToLongGenerator.java

**Interfaces:**
- Produces: collector(), generator(), setValueBefore, setValueAfter, addConversion, generate.
- Internal free sentinel: BigFraction.MINUS_ONE; it never appears in the final map.

- [ ] **Step 1: Add minimal public interfaces**

~~~java
package moze_intel.projecte.api.mapper;

import java.util.Map;
import moze_intel.projecte.emc.EmcValue;

public interface EmcMappingCollector<K> {
    void addConversion(int outputCount, K output, Map<K, Integer> ingredients);
    void setValueBefore(K key, EmcValue value);
    void setValueAfter(K key, EmcValue value);
}
~~~

~~~java
package moze_intel.projecte.api.mapper;

import java.util.Map;
import moze_intel.projecte.emc.EmcValue;

@FunctionalInterface
public interface EmcValueGenerator<K> {
    Map<K, EmcValue> generate();
}
~~~

- [ ] **Step 2: Port the graph algorithm from the frozen upstream**

Use these exact source references from commit 15d4ce65bd06eb4222709b984255fbf5080e78bc:

- src/main/java/moze_intel/projecte/emc/SimpleGraphMapper.java
- src/main/java/moze_intel/projecte/emc/arithmetic/HiddenBigFractionArithmetic.java
- src/main/java/moze_intel/projecte/emc/collector/LongToBigFractionCollector.java
- src/main/java/moze_intel/projecte/emc/generator/BigFractionToLongGenerator.java

Keep the calculation branches and iteration order intact. Adapt only the public wrapper types above, replace PECore.LOGGER with ProjectE.LOGGER, remove HolderLookup from finishCollection, and convert final positive exact fractions to EmcValue. Fractions with denominator not equal to one use integer floor exactly as upstream BigFractionToLongGenerator; values outside 0..Long.MAX_VALUE are omitted.

EmcConversion validates outputCount > 0, non-null output, non-null keys and non-null integer amounts, then stores Map.copyOf(ingredients).

- [ ] **Step 3: Verify GREEN and full suite**

Run:

~~~powershell
./gradlew.bat test --tests moze_intel.projecte.emc.graph.EmcGraphMapperTest
./gradlew.bat test
~~~

Expected: all five graph tests and all prior tests pass.

- [ ] **Step 4: Commit**

Commit message: feat: port ProjectE EMC graph solver

---

### Task 3: Define Stable Normalized Stack Keys

**Files:**
- Create: src/main/java/moze_intel/projecte/emc/NormalizedStackKey.java
- Create: src/main/java/moze_intel/projecte/emc/ItemStackKey.java
- Create: src/main/java/moze_intel/projecte/emc/TagStackKey.java
- Create: src/main/java/moze_intel/projecte/emc/FakeStackKey.java
- Test: src/test/java/moze_intel/projecte/emc/NormalizedStackKeyTest.java

**Interfaces:**
- Produces: type(), identifier(), canonicalComponentData(), canonicalString().
- Key ordering is lexicographic by canonicalString and stable across restarts.

- [ ] **Step 1: Write failing key tests**

Test that item keys distinguish canonical component payloads, tags and fake keys cannot collide with item keys, map insertion order does not affect canonical component JSON, and invalid blank identifiers are rejected.

Use these exact expected strings:

- item|minecraft:diamond|{}
- item|minecraft:diamond|{"minecraft:damage":1}
- tag|c:gems/diamond|
- fake|projecte:test_group|

- [ ] **Step 2: Verify RED**

Run: gradlew test --tests moze_intel.projecte.emc.NormalizedStackKeyTest

Expected: compilation failure because the key types do not exist.

- [ ] **Step 3: Implement the sealed key model**

NormalizedStackKey is a sealed Comparable interface permitting ItemStackKey, TagStackKey and FakeStackKey. ItemStackKey accepts an Identifier and a SortedMap<String,String>; it copies into an unmodifiable TreeMap and emits JSON by escaping keys/values. TagStackKey and FakeStackKey store a non-blank Identifier. Equality is record equality; compareTo delegates to canonicalString.

- [ ] **Step 4: Verify and commit**

Run focused and full tests. Commit message: feat: add normalized EMC stack keys

---

### Task 4: Add Explicit EMC Data Decoding

**Files:**
- Create: src/main/java/moze_intel/projecte/emc/data/ExplicitEmcEntry.java
- Create: src/main/java/moze_intel/projecte/emc/data/ExplicitEmcLoader.java
- Test: src/test/java/moze_intel/projecte/emc/data/ExplicitEmcLoaderTest.java
- Create: src/test/resources/emc/valid.json
- Create: src/test/resources/emc/invalid-negative.json

**Interfaces:**
- Consumes: JSON object mapping canonical stack keys to {value, phase}.
- Produces: sorted immutable List<ExplicitEmcEntry>; phase enum BEFORE or AFTER.

- [ ] **Step 1: Write failing decoder tests**

Test deterministic key order, BEFORE default, AFTER parsing, duplicate canonical key rejection across files, negative/overflow value rejection, unknown phase rejection, and resource-id-bearing error messages.

- [ ] **Step 2: Verify RED**

Run focused test; expect missing types.

- [ ] **Step 3: Implement strict Gson decoder**

Accept only object entries with integral JSON number value and optional phase. Reject unknown fields. Parse values through BigInteger before longValueExact and EmcValue.of. Aggregate all resource files in Identifier order; duplicate keys are an error.

- [ ] **Step 4: Verify and commit**

Commit message: feat: load explicit EMC data entries

---

### Task 5: Add Recipe Conversion Boundaries

**Files:**
- Create: src/main/java/moze_intel/projecte/emc/recipe/RecipeConversion.java
- Create: src/main/java/moze_intel/projecte/emc/recipe/RecipeConversionCollector.java
- Create: src/main/java/moze_intel/projecte/emc/recipe/IngredientChoiceExpander.java
- Test: src/test/java/moze_intel/projecte/emc/recipe/RecipeConversionCollectorTest.java

**Interfaces:**
- Produces: sorted Stream<RecipeConversion> from 26.2 RecipeManager and RegistryAccess.
- RecipeConversion contains recipe Identifier, output count/key, and ingredient amount map.

- [ ] **Step 1: Write failing pure conversion tests**

Use fixture ingredients independent from Minecraft Recipe objects to test duplicate ingredient merging, multi-output division, returned container negative amounts, empty alternatives, deterministic expansion, and exclusion of damaged/non-craftable outputs.

- [ ] **Step 2: Verify RED**

Expect missing conversion types.

- [ ] **Step 3: Implement the pure collector and thin Minecraft adapter**

Keep choice expansion capped by a configurable maximum of 65,536 combinations per recipe; recipes exceeding the cap are skipped with recipe ID and count logged. Sort recipes by Identifier and choices by canonical key before adding conversions.

- [ ] **Step 4: Verify and commit**

Commit message: feat: collect deterministic recipe EMC conversions

---

### Task 6: Publish Immutable EMC Snapshots Atomically

**Files:**
- Create: src/main/java/moze_intel/projecte/emc/EmcMappingSnapshot.java
- Create: src/main/java/moze_intel/projecte/emc/EmcMappingService.java
- Test: src/test/java/moze_intel/projecte/emc/EmcMappingServiceTest.java

**Interfaces:**
- EmcMappingSnapshot.empty(), version(), valueFor(key), values().
- EmcMappingService.current(), replace(snapshot), rebuild(Supplier<snapshot>).

- [ ] **Step 1: Write failing snapshot tests**

Test immutable defensive copies, monotonically increasing versions, readers seeing either old or new complete snapshots under concurrent replacement, and rebuild failure preserving the prior snapshot.

- [ ] **Step 2: Verify RED**

Expect missing service types.

- [ ] **Step 3: Implement atomic service**

Use AtomicReference<EmcMappingSnapshot>. rebuild calculates outside the reference, validates the candidate, assigns next version only on success, and returns a result record containing success and optional exception. Never expose mutable maps.

- [ ] **Step 4: Verify and commit**

Commit message: feat: publish atomic EMC mapping snapshots

---

### Task 7: Integrate Server Resource Reload

**Files:**
- Create: src/main/java/moze_intel/projecte/emc/reload/EmcReloadListener.java
- Create: src/main/java/moze_intel/projecte/emc/ProjectEEmc.java
- Modify: src/main/java/moze_intel/projecte/ProjectE.java
- Test: src/test/java/moze_intel/projecte/emc/reload/EmcReloadListenerTest.java

**Interfaces:**
- ProjectEEmc.service():EmcMappingService<NormalizedStackKey>.
- EmcReloadListener prepare parses explicit values and recipe conversions; apply solves and atomically publishes.

- [ ] **Step 1: Write failing transaction tests**

Use an in-memory resource fixture and fake recipe source. Verify successful publish, malformed JSON preservation of old snapshot, graph overflow omission, deterministic identical reload result, and set-after application after graph solving.

- [ ] **Step 2: Verify RED**

Expect missing reload listener.

- [ ] **Step 3: Implement and register the listener**

Register with Fabric ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener. Listener ID is projecte:emc. Preparation performs parsing/collection off-thread; apply calls EmcMappingService.replace only after all validation succeeds.

- [ ] **Step 4: Verify and commit**

Commit message: feat: rebuild EMC mappings on server reload

---

### Task 8: EMC Core Completion Audit

**Files:**
- Modify: docs/porting/feature-matrix.md
- Create: docs/porting/evidence/emc-core.md
- Modify: docs/superpowers/plans/2026-07-12-projecte-fabric-26.2-roadmap.md

- [ ] **Step 1: Run complete gate**

~~~powershell
./gradlew.bat clean build packageAudit verifyBaseline --warning-mode all "-Pprojecte_upstream=../.upstream/projecte" "-Pprojecte_upstream_commit=15d4ce65bd06eb4222709b984255fbf5080e78bc"
~~~

Expected: no failures or deprecation warnings.

- [ ] **Step 2: Record evidence**

Record exact test counts, graph parity cases, explicit data fixtures, snapshot concurrency test, reload preservation test, and output jar path in docs/porting/evidence/emc-core.md.

- [ ] **Step 3: Update matrix accurately**

Mark EMC mapping Verified only if graph, explicit values, recipe conversion and reload tests all pass. Component-specific EMC processors remain assigned to the later content/equipment plans and must stay Not started in their own matrix rows.

- [ ] **Step 4: Commit**

Commit message: docs: record EMC core verification
