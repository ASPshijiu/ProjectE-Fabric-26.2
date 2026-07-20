package moze_intel.projecte.content.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.player.PlayerAttachmentAccess;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ActiveEmcItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    @Test
    void inactiveItemDoesNotConsumeEmc() throws Exception {
        TestActiveEmcItem item = allocateWithoutRegistering();
        PlayerDataService service = new PlayerDataService(PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue));
        service.setEmc(EmcValue.of(10));

        item.onTick(null, ItemStack.EMPTY, service);

        assertEquals(EmcValue.of(10), service.emc());
    }

    @Test
    void activeItemStillConsumesEmc() throws Exception {
        TestActiveEmcItem item = allocateWithoutRegistering();
        item.setActive(ItemStack.EMPTY, true);
        PlayerDataService service = new PlayerDataService(PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue));
        service.setEmc(EmcValue.of(10));

        item.onTick(null, ItemStack.EMPTY, service);

        assertEquals(EmcValue.of(8), service.emc());
    }

    private static TestActiveEmcItem allocateWithoutRegistering() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field field = unsafeClass.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Object unsafe = field.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (TestActiveEmcItem) allocateInstance.invoke(unsafe, TestActiveEmcItem.class);
    }

    private static final class TestActiveEmcItem extends ActiveEmcItem {
        private boolean active;

        private TestActiveEmcItem() {
            super(new Item.Properties());
        }

        @Override
        public long getEmcPerTick() {
            return 2;
        }

        @Override
        public boolean isActive(ItemStack stack) {
            return active;
        }

        @Override
        public void setActive(ItemStack stack, boolean active) {
            this.active = active;
        }
    }
}
