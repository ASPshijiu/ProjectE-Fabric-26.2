package moze_intel.projecte.content.menu.slots;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import moze_intel.projecte.content.items.ManualItem;
import moze_intel.projecte.player.PlayerAttachmentAccess;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TransmuteConsumeSlotTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void sellingLegacyManualWithoutEmcPermanentlyUnlocksAllKnowledge() throws Exception {
        PlayerDataService service = new PlayerDataService(PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue));
        TransmuteConsumeSlot slot = new TransmuteConsumeSlot(
              new SimpleContainer(1), 0, 0, 0, () -> true, service, stack -> Optional.empty());
        ItemStack manual = new ItemStack(Holder.direct(
              allocateManual(), DataComponentMap.EMPTY));

        assertTrue(slot.mayPlace(manual));
        slot.set(manual);

        assertTrue(service.knowledge().fullKnowledge());
        assertTrue(slot.getItem().isEmpty());
    }

    private static ManualItem allocateManual() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (ManualItem) allocateInstance.invoke(unsafe, ManualItem.class);
    }
}
