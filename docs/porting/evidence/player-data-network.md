# Player Data & Network Verification

- Minecraft: 26.2
- Java: 25 (Temurin 25.0.3+9)
- Loom: 1.17.14
- Gradle: 9.5.1
- Loader: 0.19.3
- Fabric API: 0.154.2+26.2
- ProjectE baseline: 15d4ce65bd06eb4222709b984255fbf5080e78bc
- Commands: gradlew build packageAudit
- Result: PASS (92 tests, 0 failures, 0 errors, 1 skipped — PackagedJarTest runs only under packageAudit)
- Runtime jar audit: PASS (build/libs/projecte-fabric-0.1.0-alpha.1.jar)

## Verified behavior

- Player knowledge (PlayerKnowledgeTest, 9 tests): learn/unlearn change reporting, known-item no-op, full-knowledge short-circuit, toggle round-trip, immutable learned set, copy independence.
- Input locks (PlayerInputLocksTest, 5 tests): empty state, set/get round-trip, null clear, out-of-bounds rejection, deep copy.
- Codecs (PlayerDataCodecsTest, 6 tests): knowledge empty/learned/full round-trip, default full_knowledge false, input-lock mixed-slot round-trip, EmcValue non-negative long round-trip.
- Attachment registration (PlayerAttachmentsTest, 5 tests): default values, negative-EMC persistence rejection. (Registration metadata is runtime-verified — the Fabric attachment-sync subsystem is mixin-implemented and cannot class-load in a plain JUnit run.)
- Server data service (PlayerDataServiceTest, 12 tests): default state, add/set/remove with overflow/underflow guards, tryRemoveEmc atomic guard, learn/unlearn, full-knowledge, input-lock round-trip and clear, gem-armor toggle.
- Sync lifecycle (PlayerSyncHandlersTest, 3 tests): join initializes all attachments without mutating personal state; reload rebroadcast fans the snapshot out without touching personal EMC.
- Reload listener integration (EmcReloadListenerTest, 4 tests): explicit values published, injected recipe conversions solved, malformed reload preserves previous snapshot, reload callback fires only on success.
- Commands (EmcCommandTest 7 + KnowledgeCommandTest 7): set/add/remove/get/test and learn/unlearn/test/clear with overflow/underflow rejection, idempotent learn, full-knowledge reset on clear, negative-argument rejection.

## Architecture decisions

- Player state lives in four Fabric data attachments: knowledge (PlayerKnowledge), emc (EmcValue long),
  input_locks (PlayerInputLocks), gem_armor_state (Boolean). All are persistent, copy-on-death, and
  synced to the owning player only via AttachmentSyncPredicate.targetOnly().
- EMC player storage remains non-negative long EmcValue (ProjectE 1.21.1 semantics). BigInteger player
  storage from Fabricated Exchange is a documented later opt-in that must not change the long contract.
- The server is authoritative: clients read ClientPlayerData (read-only facade) over the synced
  attachment; all mutations go through PlayerDataService on the server.
- PlayerDataService is unit-testable via a PlayerAttachmentAccess boundary that abstracts the Fabric
  read/write (the mixin-backed AttachmentRegistryImpl static initializer cannot run in plain JUnit).
  Production wiring maps opaque string keys to the real AttachmentType via PlayerAttachmentKeys.fabricAdapter.
- Commands gate on the 26.2 Permissions.COMMANDS_GAMEMASTER permission (replacing the legacy level-2 int).
- The reload callback (withReloadCallback) fires only on a successful reload, so the player sync handler
  rebroadcasts the shared mapping without touching personal state.

## Deferred items

- The shared EMC-mapping S2C payload (full ItemStackKey→EmcValue map for transmutation-table display)
  is intentionally wired later by the transmutation phase; the reload-completion callback plumbing is
  in place and tested, only the final per-player broadcast is deferred.
- The vanilla 26.2 RecipeManager adapter remains tracked under the content/mappers phase.
