package moze_intel.projecte.transmutation.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.player.PlayerKnowledge;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class TransmutationOutputResolverTest {
    private static NormalizedStackKey key(String path) {
        return new ItemStackKey(
              Identifier.fromNamespaceAndPath("projecte", path), Map.of());
    }

    private EmcMappingSnapshot<NormalizedStackKey> snapshot(Map<NormalizedStackKey, EmcValue> values) {
        return new EmcMappingSnapshot<>(1L, values);
    }

    @Test
    void returnsKnownItemsDescendingByEmcLikeUpstream() {
        NormalizedStackKey dirt = key("dirt");
        NormalizedStackKey iron = key("iron");
        NormalizedStackKey diamond = key("diamond");
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(Map.of(
              dirt, EmcValue.of(1), iron, EmcValue.of(256), diamond, EmcValue.of(8192)));
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(dirt).learn(iron).learn(diamond);

        List<NormalizedStackKey> outputs = TransmutationOutputResolver.resolve(
              snap, knowledge, EmcValue.of(Long.MAX_VALUE));

        assertEquals(List.of(diamond, iron, dirt), outputs);
    }

    @Test
    void filtersToAffordableItems() {
        NormalizedStackKey dirt = key("dirt");
        NormalizedStackKey iron = key("iron");
        NormalizedStackKey diamond = key("diamond");
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(Map.of(
              dirt, EmcValue.of(1), iron, EmcValue.of(256), diamond, EmcValue.of(8192)));
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(dirt).learn(iron).learn(diamond);

        List<NormalizedStackKey> outputs = TransmutationOutputResolver.resolve(
              snap, knowledge, EmcValue.of(300));

        assertEquals(List.of(iron, dirt), outputs);
    }

    @Test
    void excludesUnknownItemsEvenIfTheyHaveEmc() {
        NormalizedStackKey dirt = key("dirt");
        NormalizedStackKey iron = key("iron");
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(Map.of(
              dirt, EmcValue.of(1), iron, EmcValue.of(256)));
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(dirt);

        List<NormalizedStackKey> outputs = TransmutationOutputResolver.resolve(
              snap, knowledge, EmcValue.of(Long.MAX_VALUE));

        assertEquals(List.of(dirt), outputs);
    }

    @Test
    void fullKnowledgeIncludesEveryMappedItem() {
        NormalizedStackKey dirt = key("dirt");
        NormalizedStackKey iron = key("iron");
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(Map.of(
              dirt, EmcValue.of(1), iron, EmcValue.of(256)));
        PlayerKnowledge full = PlayerKnowledge.empty().withFullKnowledge(true);

        Set<NormalizedStackKey> outputs = new HashSet<>(TransmutationOutputResolver.resolve(
              snap, full, EmcValue.of(Long.MAX_VALUE)));

        assertEquals(Set.of(dirt, iron), outputs);
    }

    @Test
    void capsAtOutputSlotCount() {
        Set<NormalizedStackKey> all = new LinkedHashSet<>();
        Map<NormalizedStackKey, EmcValue> values = new java.util.LinkedHashMap<>();
        for (int i = 0; i < TransmutationOutputResolver.OUTPUT_SLOT_COUNT + 10; i++) {
            NormalizedStackKey k = key("item" + i);
            all.add(k);
            values.put(k, EmcValue.of(i + 1));
        }
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(values);
        PlayerKnowledge full = PlayerKnowledge.empty().withFullKnowledge(true);

        List<NormalizedStackKey> outputs = TransmutationOutputResolver.resolve(
              snap, full, EmcValue.of(Long.MAX_VALUE));

        assertEquals(TransmutationOutputResolver.OUTPUT_SLOT_COUNT, outputs.size());
        // Highest EMC items are shown first, matching the upstream table.
        assertTrue(outputs.contains(key("item10")));
        assertTrue(outputs.contains(key("item25")));
    }

    @Test
    void exposesEveryAffordableItemAcrossPages() {
        Map<NormalizedStackKey, EmcValue> values = new java.util.LinkedHashMap<>();
        for (int i = 0; i < TransmutationOutputResolver.OUTPUT_SLOT_COUNT + 4; i++) {
            values.put(key("item" + i), EmcValue.of(i + 1));
        }
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(values);
        PlayerKnowledge full = PlayerKnowledge.empty().withFullKnowledge(true);

        TransmutationOutputResolver.Page first = TransmutationOutputResolver.resolvePage(
              snap, full, EmcValue.of(Long.MAX_VALUE), 0);
        TransmutationOutputResolver.Page second = TransmutationOutputResolver.resolvePage(
              snap, full, EmcValue.of(Long.MAX_VALUE), 1);

        assertEquals(2, first.pageCount());
        assertEquals(0, first.pageIndex());
        assertEquals(TransmutationOutputResolver.OUTPUT_SLOT_COUNT, first.outputs().size());
        assertEquals(key("item19"), first.outputs().getFirst());
        assertEquals(2, second.pageCount());
        assertEquals(1, second.pageIndex());
        assertEquals(List.of(key("item3"), key("item2"), key("item1"), key("item0")),
              second.outputs());
    }

    @Test
    void clampsAStalePageAfterTheAffordableSetShrinks() {
        NormalizedStackKey dirt = key("dirt");
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(Map.of(dirt, EmcValue.of(1)));
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(dirt);

        TransmutationOutputResolver.Page page = TransmutationOutputResolver.resolvePage(
              snap, knowledge, EmcValue.of(1), 99);

        assertEquals(0, page.pageIndex());
        assertEquals(1, page.pageCount());
        assertEquals(List.of(dirt), page.outputs());
    }

    @Test
    void lockValueCapsOutputsWithoutHidingCheaperKnowledge() {
        NormalizedStackKey dirt = key("dirt");
        NormalizedStackKey iron = key("iron");
        NormalizedStackKey diamond = key("diamond");
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(Map.of(
              dirt, EmcValue.of(1), iron, EmcValue.of(256), diamond, EmcValue.of(8192)));
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(dirt).learn(iron).learn(diamond);

        TransmutationOutputResolver.Page page = TransmutationOutputResolver.resolvePage(
              snap, knowledge, EmcValue.of(10_000), Optional.of(EmcValue.of(256)), 0);

        assertEquals(List.of(iron, dirt), page.outputs());
    }

    @Test
    void excludesZeroEmcItems() {
        NormalizedStackKey free = key("free");
        NormalizedStackKey dirt = key("dirt");
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(Map.of(
              free, EmcValue.of(0), dirt, EmcValue.of(1)));
        PlayerKnowledge knowledge = PlayerKnowledge.empty().learn(free).learn(dirt);

        List<NormalizedStackKey> outputs = TransmutationOutputResolver.resolve(
              snap, knowledge, EmcValue.of(Long.MAX_VALUE));

        assertEquals(List.of(dirt), outputs);
    }

    @Test
    void excludesInternalMappingKeysThatCannotMaterializeAnItem() {
        NormalizedStackKey dirt = key("dirt");
        NormalizedStackKey internal = new FakeStackKey(
              Identifier.fromNamespaceAndPath("projecte", "internal"));
        EmcMappingSnapshot<NormalizedStackKey> snap = snapshot(Map.of(
              dirt, EmcValue.of(1), internal, EmcValue.of(2)));
        PlayerKnowledge full = PlayerKnowledge.empty().withFullKnowledge(true);

        List<NormalizedStackKey> outputs = TransmutationOutputResolver.resolve(
              snap, full, EmcValue.of(Long.MAX_VALUE));

        assertEquals(List.of(dirt), outputs);
    }
}
