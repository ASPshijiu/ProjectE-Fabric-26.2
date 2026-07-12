# ProjectE Fabric 26.2 Transmutation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Port ProjectE's transmutation gameplay to Fabric 26.2 in dependency order: first the world-transmutation data subsystem and Philosopher's-Stone block conversion (right-click / shift-right-click), then the transmutation table & tablet menus, then the Tome of Knowledge. Behavior must match ProjectE 1.21.1 (default world-transmutation set, origin/result/alt-result, state-property copying, slime/glowstone special-cases, knowledge requirements and EMC spending).

**Architecture:** World transmutations load as server data (`data/<ns>/pe_world_transmutations/*.json`) into an immutable `WorldTransmutationRegistry` keyed by origin block, mirroring the EMC reload pipeline. The Philosopher's Stone is a registered item whose use callback performs the block conversion server-side. The transmutation table/tablet are `MenuType` + `AbstractContainerMenu` + `BlockEntity`/item whose slots interact with `PlayerDataService` and `EmcMappingSnapshot`. The Tome sets the player's full-knowledge flag while in inventory.

**Tech Stack:** Java 25, Fabric 26.2, Fabric Data Attachment, Fabric Networking, JUnit 5.11.4, Gson.

## Global Constraints

- ProjectE commit 15d4ce65bd06eb4222709b984255fbf5080e78bc remains the behavioral baseline.
- World transmutations are server-data driven; the default set must load from the frozen baseline (4 files: colors, defaults, oxidization, planks).
- Block conversion happens server-side and is authoritative; the client only predicts visuals.
- EMC spending and knowledge checks go through `PlayerDataService` and `EmcMappingSnapshot`.
- No empty stubs, no permanent defaults, no GUI registered without behavior.

---

### Task 1: World Transmutation Data Model and Codec (TDD)

**Files:**
- Create: src/main/java/moze_intel/projecte/transmutation/world/SimpleWorldTransmutation.java
- Create: src/main/java/moze_intel/projecte/transmutation/world/WorldTransmutationFile.java
- Test: src/test/java/moze_intel/projecte/transmutation/world/SimpleWorldTransmutationTest.java

**Interfaces:**
- SimpleWorldTransmutation(origin Holder<Block>, result Holder<Block>, altResult Holder<Block>); CODEC; STREAM_CODEC; hasAlternate().

- [ ] **Step 1: Write failing model/codec tests** — origin/result/alt-result round-trip; alt defaults to result when absent; state-property copy hint; codec rejects unknown blocks in test fixtures.
- [ ] **Step 2: Verify RED**
- [ ] **Step 3: Implement the record + DFU Codec + StreamCodec** mirroring upstream.
- [ ] **Step 4: Verify and commit** — Commit message: feat: add world transmutation data model

---

### Task 2: World Transmutation Registry Reload

**Files:**
- Create: src/main/java/moze_intel/projecte/transmutation/world/WorldTransmutationRegistry.java
- Create: src/main/java/moze_intel/projecte/transmutation/world/WorldTransmutationReloadListener.java
- Modify: src/main/java/moze_intel/projecte/ProjectE.java
- Test: src/test/java/moze_intel/projecte/transmutation/world/WorldTransmutationRegistryTest.java

**Interfaces:**
- WorldTransmutationRegistry.entries() immutable map; forOrigin(Block) list; empty/frozen defaults.
- Reload listener reads `data/<ns>/pe_world_transmutations/*.json`, aggregates deterministically, replaces the registry atomically.

- [ ] **Step 1: Write failing registry/reload tests** — fixture files load into the registry; forOrigin returns matching transmutations; duplicate origin+result rejected; deterministic order; reload preserves prior on failure.
- [ ] **Step 2: Verify RED**
- [ ] **Step 3: Implement registry + SimpleReloadListener-style listener** (reuse the Fabric v1 reloader API like EmcReloadListener).
- [ ] **Step 4: Wire into ProjectE.onInitialize**
- [ ] **Step 5: Verify and commit** — Commit message: feat: reload world transmutations as server data

---

### Task 3: Block Conversion Logic

**Files:**
- Create: src/main/java/moze_intel/projecte/transmutation/world/WorldTransmutationAction.java
- Test: src/test/java/moze_intel/projecte/transmutation/world/WorldTransmutationActionTest.java

**Interfaces:**
- WorldTransmutationAction.apply(level, pos, origin, useAlternate, registry): boolean — copies matching state properties, returns true if a conversion occurred.

- [ ] **Step 1: Write failing conversion tests** — uses a stub BlockState/Block pair (bootstrap MC for real blocks); verifies result vs alt-result selection and state-property copying.
- [ ] **Step 2: Verify RED**
- [ ] **Step 3: Implement conversion** — lookup registry by origin block, pick result/alt-result, copy shared state properties via BlockState copyable properties.
- [ ] **Step 4: Verify and commit** — Commit message: feat: convert world blocks via transmutation

---

### Task 4: Register the Philosopher's Stone Item

**Files:**
- Create: src/main/java/moze_intel/projecte/content/ModItems.java (registration hub)
- Create: src/main/java/moze_intel/projecte/content/items/PhilosophersStoneItem.java
- Modify: src/main/java/moze_intel/projecte/ProjectE.java
- Create: src/main/resources/assets/projecte/lang/en_us.json (seed)
- Test: src/test/java/moze_intel/projecte/content/ModItemsTest.java (registration ids)

**Interfaces:**
- ModItems.PHILOSOPHERS_STONE Item register; on right-click block performs WorldTransmutationAction (normal) or shift = alt-result; sneaking right-click air opens crafting-like menu (deferred).

- [ ] **Step 1: Write failing registration test** — item id `projecte:philosophers_stone` registered.
- [ ] **Step 2: Verify RED**
- [ ] **Step 3: Implement ModItems + PhilosophersStoneItem** with use-on-block callback delegating to WorldTransmutationAction.
- [ ] **Step 4: Wire registration + lang**
- [ ] **Step 5: Verify and commit** — Commit message: feat: register philosopher's stone item

---

### Task 5: Transmutation Table Block, BlockEntity, Menu (core)

**Files:**
- Create: ModBlocks, TransmutationTableBlock, TransmutationTableBlockEntity, TransmutationTableMenu, TransmutationTableScreen (client)
- Test: GameTest or container-behavior test

**Interfaces:**
- Menu has input, lock, output slots; reads PlayerDataService knowledge + EmcMappingSnapshot to populate outputs; spending/learning server-authoritative.

- [ ] **Step 1: Implement block + block entity + menu registration**
- [ ] **Step 2: Implement slot behavior and EMC/knowledge integration**
- [ ] **Step 3: Add client screen**
- [ ] **Step 4: Verify and commit** — Commit message: feat: add transmutation table menu

---

### Task 6: Transmutation Tablet (portable), Tome of Knowledge

- [ ] Implement tablet item with the same menu; Tome sets full-knowledge flag while in inventory.

---

### Task 7: Transmutation Completion Audit

- [ ] Run gate, record evidence, update feature matrix.

---

**Note:** Tasks 5–6 are large and GUI-bound; they may be split into further sub-plans during execution. Each task still ends in a build/test command and focused commit.
