package moze_intel.projecte.content.menu.slots;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import moze_intel.projecte.content.items.ManualItem;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.ItemStackKey;
import moze_intel.projecte.emc.StackEmcResolver;
import moze_intel.projecte.player.PlayerAttachmentAccess;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    @Test
    void overflowKeepsSoldItemAndDoesNotLearnIt() {
        PlayerDataService service = new PlayerDataService(PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue));
        service.setEmc(EmcValue.of(Long.MAX_VALUE));
        ItemStackKey stone = new ItemStackKey(
              Identifier.withDefaultNamespace("stone"), java.util.Map.of());
        TransmuteConsumeSlot slot = new TransmuteConsumeSlot(
              new SimpleContainer(1), 0, 0, 0, () -> true, service,
              stack -> Optional.of(new StackEmcResolver.Resolved(stone, EmcValue.of(1))));

        slot.set(new ItemStack(Items.STONE));

        assertTrue(slot.getItem().is(Items.STONE));
        assertEquals(EmcValue.of(Long.MAX_VALUE), service.emc());
        assertFalse(service.hasKnowledge(stone));
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
