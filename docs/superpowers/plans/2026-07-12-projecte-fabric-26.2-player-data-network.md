# ProjectE Fabric 26.2 Player Data & Network Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Port ProjectE's per-player EMC balance, learned item knowledge, input-lock inventory, gem-armor flag, and the server-authoritative synchronization and command surface to Fabric 26.2, preserving ProjectE 1.21.1 semantics (long EMC bounds, copy-on-death, learn/unlearn rules, knowledge tome, full-knowledge flag).

**Architecture:** Store every player-owned value in Fabric Data Attachments on the `Player` entity (`AttachmentRegistry` with `persistent(Codec)`, `copyOnDeath()`, and `syncWith(StreamCodec, AttachmentSyncPredicate)`). The attachment's auto-sync replaces ProjectE's hand-rolled S2C knowledge packets for client display. Gameplay mutates state only through a server-side `PlayerDataService` facade; clients read the synced attachment but never propose authoritative values. Commands register through the Fabric command callback.

**Tech Stack:** Java 25, Fabric 26.2, Fabric Data Attachment API v1, Fabric Networking API v1, Fabric Lifecycle Events v1, JUnit 5.11.4, Gson.

## Global Constraints

- ProjectE commit 15d4ce65bd06eb4222709b984255fbf5080e78bc remains the behavioral baseline.
- Player EMC is a non-negative `long` represented by `EmcValue`; no BigInteger player storage in this phase. (Fabricated-Exchange-style arbitrary precision is a later opt-in extension that must not change the long network/save contract.)
- Learned knowledge is keyed by `ItemStackKey` (canonical item + component payload), matching the EMC graph keys.
- The server is authoritative: clients may read synced attachments for display but every mutation originates server-side and is verified there.
- Attachments must persist across save/reload, copy on death, and resync to the owning client automatically.
- No empty method stubs, no permanent default returns, no behavior disabled behind an untracked flag.
- Every production behavior is introduced after a failing focused test.

---

## File Structure

- src/main/java/moze_intel/projecte/player/PlayerAttachments.java — registers knowledge, emc, input-lock, gem-armor attachments.
- src/main/java/moze_intel/projecte/player/PlayerKnowledge.java — immutable knowledge model: learned set + full-knowledge flag, with add/remove/has/query.
- src/main/java/moze_intel/projecte/player/PlayerInputLocks.java — fixed-size (9) input-lock inventory model with copy semantics.
- src/main/java/moze_intel/projecte/player/PlayerEmcState.java — combined mutable player state record used by the service.
- src/main/java/moze_intel/projecte/player/PlayerDataService.java — server-side facade over attachments: read, learn, unlearn, add/remove/set EMC, gem-armor toggle.
- src/main/java/moze_intel/projecte/player/PlayerDataCodecs.java — Codec + StreamCodec for each attachment payload.
- src/main/java/moze_intel/projecte/network/ProjectENetworking.java — declares payload ids and registers any explicit C2S payloads (e.g. request sync).
- src/main/java/moze_intel/projecte/network/payloads/RequestEmcSyncPayload.java — C2S payload used if attachment auto-sync is insufficient on join.
- src/main/java/moze_intel/projecte/network/EmcClientCache.java — client-only read access to synced EMC snapshot for display (in client source set).
- src/main/java/moze_intel/projecte/command/ProjectECommands.java — registers /projecte emc and /projecte knowledge sub-trees.
- src/main/java/moze_intel/projecte/command/EmcCommand.java — emc add/remove/set/get/test.
- src/main/java/moze_intel/projecte/command/KnowledgeCommand.java — knowledge learn/unlearn/test/clear.
- src/main/java/moze_intel/projecte/ProjectE.java — wires attachment registration and command/lifecycle callbacks.
- src/test/java/moze_intel/projecte/player/* — knowledge, input-lock, EMC balance, codec round-trip and service tests.
- src/test/java/moze_intel/projecte/command/* — command behavior tests.

---

### Task 1: Define Player Knowledge and Input-Lock Models Test-First

**Files:**
- Create: src/main/java/moze_intel/projecte/player/PlayerKnowledge.java
- Create: src/main/java/moze_intel/projecte/player/PlayerInputLocks.java
- Test: src/test/java/moze_intel/projecte/player/PlayerKnowledgeTest.java
- Test: src/test/java/moze_intel/projecte/player/PlayerInputLocksTest.java

**Interfaces:**
- Produces: PlayerKnowledge.empty(), learn(ItemStackKey), unlearn(ItemStackKey), has(ItemStackKey), learned() immutable set, fullKnowledge() flag, setFullKnowledge(boolean), copy().
- Produces: PlayerInputLocks.empty(), 9 slots, set(int,ItemStackKey), get(int), size(), copy().

- [ ] **Step 1: Write failing PlayerKnowledge tests**

Test: learning an item adds it and returns true; learning a known item returns false and leaves the set unchanged; unlearning unknown returns false; full knowledge short-circuits has() to true and ignores the learned set; set/add operations never mutate the original (return new instances); copy() is independent.

- [ ] **Step 2: Write failing PlayerInputLocks tests**

Test: empty has 9 empty slots; set then get round-trips; set beyond 0..8 throws; copy() is an independent deep copy.

- [ ] **Step 3: Verify RED**

Run: gradlew test --tests moze_intel.projecte.player.PlayerKnowledgeTest --tests moze_intel.projecte.player.PlayerInputLocksTest

Expected: compilation failure because the types do not exist.

- [ ] **Step 4: Implement immutable models**

PlayerKnowledge is an immutable record-like class holding an unmodifiable `Set<ItemStackKey>` and a `boolean fullKnowledge`; every mutator returns a new instance. PlayerInputLocks holds a fixed `ItemStackKey[9]` (empty slots represented by a sentinel or null with explicit handling) and returns defensive copies.

- [ ] **Step 5: Verify and commit**

Commit message: feat: add player knowledge and input-lock models

---

### Task 2: Add Codec and StreamCodec for Player Attachments

**Files:**
- Create: src/main/java/moze_intel/projecte/player/PlayerDataCodecs.java
- Test: src/test/java/moze_intel/projecte/player/PlayerDataCodecsTest.java

**Interfaces:**
- Produces: Codec<PlayerKnowledge>, StreamCodec<RegistryFriendlyByteBuf,PlayerKnowledge>; Codec<PlayerInputLocks>, StreamCodec for input-locks; EmcValue already has a codec.
- Consumes: ItemStackKey codec from the EMC core.

- [ ] **Step 1: Write failing codec round-trip tests**

Test knowledge codec round-trips: empty, single learned item, full-knowledge flag true with empty set, multi-item deterministic order. Test input-lock codec round-trips mixed empty/filled slots. Use DataResult and JsonOps for Codec; assert canonical equality.

- [ ] **Step 2: Verify RED**

- [ ] **Step 3: Implement codecs**

Knowledge codec: object with `knowledge` (list of ItemStackKey) and `full_knowledge` (bool, default false). Input-lock codec: object with `slots` (list, length 9, nullable entries). StreamCodecs mirror the layout using ByteBufCodecs.

- [ ] **Step 4: Verify and commit**

Commit message: feat: encode player knowledge and input-lock state

---

### Task 3: Register Fabric Player Attachments

**Files:**
- Create: src/main/java/moze_intel/projecte/player/PlayerAttachments.java
- Create: src/main/java/moze_intel/projecte/player/PlayerEmcState.java
- Modify: src/main/java/moze_intel/projecte/ProjectE.java
- Test: src/test/java/moze_intel/projecte/player/PlayerAttachmentsTest.java

**Interfaces:**
- Produces: PlayerAttachments.KNOWLEDGE, EMC, INPUT_LOCKS, GEM_ARMOR as AttachmentType references; each persistent + copyOnDeath + syncWith(all players predicate for EMC/knowledge so the owning client sees its own data).

- [ ] **Step 1: Write failing registration tests**

Test that each attachment type is non-null, has the expected identifier, and that a synthetic AttachmentTarget round-trips the value through codec. Use the MinecraftTestHarness if ItemStackKey construction needs registries.

- [ ] **Step 2: Verify RED**

- [ ] **Step 3: Register attachments**

Use AttachmentRegistry.<Type>builder().persistent(codec).copyOnDeath().syncWith(streamCodec, AttachmentSyncPredicate.forPlayers(...)).buildAndRegister(id). EMC attachment type is `EmcValue` (non-negative long). Knowledge type is PlayerKnowledge. Input-locks type is PlayerInputLocks. Gem-armor type is Boolean.

- [ ] **Step 4: Wire registration call in ProjectE.onInitialize**

Call PlayerAttachments.init() (a class-load trigger is enough since AttachmentRegistry.create registers on static init, but add an explicit touch for clarity).

- [ ] **Step 5: Verify and commit**

Commit message: feat: register player data attachments

---

### Task 4: Implement the Server-Side Player Data Service

**Files:**
- Create: src/main/java/moze_intel/projecte/player/PlayerDataService.java
- Test: src/test/java/moze_intel/projecte/player/PlayerDataServiceTest.java

**Interfaces:**
- Produces: PlayerDataService.knowledge(Player), emc(Player), addEmc(Player,EmcValue), removeEmc(Player,EmcValue) with underflow guard, setEmc(Player,EmcValue), learn(Player,ItemStackKey), unlearn(Player,ItemStackKey), hasKnowledge(Player,ItemStackKey), inputLocks(Player), gemArmorEnabled(Player), setGemArmor(Player,boolean).
- Mutations write back to the attachment and rely on Fabric auto-sync.

- [ ] **Step 1: Write failing service tests**

Use a fake/stub attachment target (or a real ServerPlayer fixture via a lightweight harness). Test: addEmc accumulates and overflows safely; removeEmc underflow throws and leaves balance unchanged; learn then hasKnowledge true; unlearn unknown is a no-op returning false; full-knowledge player hasKnowledge returns true for anything; gem-armor toggle round-trips.

- [ ] **Step 2: Verify RED**

- [ ] **Step 3: Implement the facade**

Each method reads the current attachment value, computes the new immutable value, and writes it back via `player.setData(attachment, newValue)`. EMC arithmetic reuses EmcValue checked operations. Knowledge uses PlayerKnowledge mutators returning new instances.

- [ ] **Step 4: Verify and commit**

Commit message: feat: expose server-authoritative player data service

---

### Task 5: Wire Login Sync and Reload-Rebroadcast Lifecycle

**Files:**
- Create: src/main/java/moze_intel/projecte/player/PlayerSyncHandlers.java
- Modify: src/main/java/moze_intel/projecte/ProjectE.java
- Test: src/test/java/moze_intel/projecte/player/PlayerSyncHandlersTest.java

**Interfaces:**
- Consumes: ServerPlayConnectionEvents.JOIN, ServerTickEvents or ServerLifecycleEvents, EmcReloadListener completion.
- Produces: On join, ensure the player's EMC/knowledge attachments are marked dirty so the owning client receives them; on EMC reload completion, push the new mapping to every online player's display cache (client reads ProjectEEmc.service() snapshot through a synced summary or a dedicated payload).

- [ ] **Step 1: Write failing handler tests**

Test the join handler marks the relevant attachments dirty (using a capturing fake player/connection). Test that a reload-completion callback fans out to all players without mutating their personal EMC/knowledge (only the shared mapping).

- [ ] **Step 2: Verify RED**

- [ ] **Step 3: Implement handlers**

Register ServerPlayConnectionEvents.JOIN to call PlayerDataService.syncOnJoin(player). Add a hook from EmcReloadListener.apply (or a ProjectEEmc reload listener) that iterates online players and sends the refreshed shared-mapping summary. Keep it server-side authoritative.

- [ ] **Step 4: Verify and commit**

Commit message: feat: sync player data on join and after EMC reload

---

### Task 6: Add /projecte emc and /projecte knowledge Commands

**Files:**
- Create: src/main/java/moze_intel/projecte/command/ProjectECommands.java
- Create: src/main/java/moze_intel/projecte/command/EmcCommand.java
- Create: src/main/java/moze_intel/projecte/command/KnowledgeCommand.java
- Modify: src/main/java/moze_intel/projecte/ProjectE.java
- Test: src/test/java/moze_intel/projecte/command/EmcCommandTest.java
- Test: src/test/java/moze_intel/projecte/command/KnowledgeCommandTest.java

**Interfaces:**
- Produces: /projecte emc add|remove|set <long> [player], /projecte emc get [player], /projecte emc test <long> [player]; /projecte knowledge learn|unlearn <item> [player], /projecte knowledge test <item> [player], /projecte knowledge clear [player].

- [ ] **Step 1: Write failing command tests**

Use Brigadier directly with a fake CommandSourceStack/ServerPlayer harness. Test: emc set 100 then get returns 100; emc add to overflow rejects; emc remove below zero rejects; knowledge learn then test true; knowledge clear empties the set; permission gating refuses unauthorized sources.

- [ ] **Step 2: Verify RED**

- [ ] **Step 3: Implement commands**

Register via CommandRegistrationCallback.EVENT. Each subcommand reads/mutates through PlayerDataService and sends feedback Components. Use EmcValue checked arithmetic for add/remove/set. Item argument resolves to ItemStackKey via MinecraftStackKeyFactory.

- [ ] **Step 4: Wire in ProjectE.onInitialize**

- [ ] **Step 5: Verify and commit**

Commit message: feat: add projecte emc and knowledge commands

---

### Task 7: Add Client-Side Read Cache and Display Helpers

**Files:**
- Create: src/client/java/moze_intel/projecte/client/player/ClientPlayerData.java
- Create: src/client/java/moze_intel/projecte/client/ProjectEClient.java (modify)
- Test: src/client/java is not unit-testable for client-only classes; cover via the GameTest/smoke harness in a later phase. Document the read-only contract here.

**Interfaces:**
- Produces: ClientPlayerData.knowledge(), emc() reading the locally synced attachment on the local player.

- [ ] **Step 1: Implement the client read facade**

ClientPlayerData reads Minecraft.getInstance().player's synced attachments and exposes immutable views for the transmutation GUI and HUD. No mutation methods exist here.

- [ ] **Step 2: Verify build still passes (client compiles, packageAudit excludes client-only symbols appropriately)**

- [ ] **Step 3: Commit**

Commit message: feat: add client player data read facade

---

### Task 8: Player Data & Network Completion Audit

**Files:**
- Modify: docs/porting/feature-matrix.md
- Create: docs/porting/evidence/player-data-network.md
- Modify: docs/superpowers/plans/2026-07-12-projecte-fabric-26.2-roadmap.md

- [ ] **Step 1: Run complete gate**

~~~powershell
./gradlew.bat clean build packageAudit verifyBaseline "-Pprojecte_upstream=../.upstream/projecte" "-Pprojecte_upstream_commit=15d4ce65bd06eb4222709b984255fbf5080e78bc"
~~~

Expected: BUILD SUCCESSFUL, no failures or warnings.

- [ ] **Step 2: Record evidence**

Record test counts, codec round-trips, service mutation coverage, command coverage, and the attachment persistence/sync verification approach in docs/porting/evidence/player-data-network.md.

- [ ] **Step 3: Update matrix**

Mark "Player EMC and knowledge" Verified only if knowledge model, codecs, attachments, service, sync handlers and commands all pass. Note any deferred items (e.g. explicit C2S sync payload if auto-sync suffices) with a reason.

- [ ] **Step 4: Commit**

Commit message: docs: record player data and network verification
