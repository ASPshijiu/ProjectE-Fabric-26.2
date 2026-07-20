package moze_intel.projecte.content.menu.slots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import moze_intel.projecte.emc.EmcMappingSnapshot;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.player.PlayerAttachmentAccess;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TransmuteOutputSlotTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void rejectsStaleOutputAfterKnowledgeIsRemoved() {
        ItemStackKey stone = new ItemStackKey(
              Identifier.withDefaultNamespace("stone"), Map.of());
        EmcMappingSnapshot<moze_intel.projecte.emc.NormalizedStackKey> snapshot =
              new EmcMappingSnapshot<>(1, Map.of(stone, EmcValue.of(1)));
        PlayerDataService service = new PlayerDataService(PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue));
        service.setEmc(EmcValue.of(10));
        SimpleContainer output = new SimpleContainer(new ItemStack(Items.STONE));
        TransmuteOutputSlot slot = new TransmuteOutputSlot(
              output, 0, 0, 0, () -> true, service, ignored -> stone, () -> snapshot);

        ItemStack extracted = slot.remove(1);

        assertTrue(extracted.isEmpty());
        assertEquals(EmcValue.of(10), service.emc());
    }
}
