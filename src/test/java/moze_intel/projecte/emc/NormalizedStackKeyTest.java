package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.Map;
import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class NormalizedStackKeyTest {
    private static final Identifier DIAMOND = Identifier.fromNamespaceAndPath("minecraft", "diamond");

    @Test
    void canonicalizesItemsAndComponentOrder() {
        ItemStackKey plain = new ItemStackKey(DIAMOND, Map.of());
        Map<String, JsonPrimitive> firstOrder = new LinkedHashMap<>();
        firstOrder.put("minecraft:name", new JsonPrimitive("Test"));
        firstOrder.put("minecraft:damage", new JsonPrimitive(1));
        Map<String, JsonPrimitive> secondOrder = new LinkedHashMap<>();
        secondOrder.put("minecraft:damage", new JsonPrimitive(1));
        secondOrder.put("minecraft:name", new JsonPrimitive("Test"));

        ItemStackKey first = new ItemStackKey(DIAMOND, firstOrder);
        ItemStackKey second = new ItemStackKey(DIAMOND, secondOrder);

        assertEquals("item|minecraft:diamond|{}", plain.canonicalString());
        assertEquals("item|minecraft:diamond|{\"minecraft:damage\":1,\"minecraft:name\":\"Test\"}", first.canonicalString());
        assertEquals(first, second);
    }

    @Test
    void distinguishesComponentPayloadsAndKeyKinds() {
        ItemStackKey undamaged = new ItemStackKey(DIAMOND, Map.of("minecraft:damage", new JsonPrimitive(0)));
        ItemStackKey damaged = new ItemStackKey(DIAMOND, Map.of("minecraft:damage", new JsonPrimitive(1)));
        TagStackKey tag = new TagStackKey(Identifier.fromNamespaceAndPath("c", "gems/diamond"));
        FakeStackKey fake = new FakeStackKey(ProjectEAPI.id("test_group"));

        assertNotEquals(undamaged, damaged);
        assertEquals("tag|c:gems/diamond|", tag.canonicalString());
        assertEquals("fake|projecte:test_group|", fake.canonicalString());
        assertNotEquals(tag.canonicalString(), fake.canonicalString());
    }

    @Test
    void rejectsMissingIdentifiersAndComponents() {
        assertThrows(NullPointerException.class, () -> new ItemStackKey(null, Map.of()));
        assertThrows(NullPointerException.class, () -> new ItemStackKey(DIAMOND, null));
        assertThrows(NullPointerException.class, () -> new TagStackKey(null));
        assertThrows(NullPointerException.class, () -> new FakeStackKey(null));
    }
}
