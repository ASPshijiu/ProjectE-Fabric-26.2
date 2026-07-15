package moze_intel.projecte.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Optional;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.testsupport.MinecraftTestHarness;
import net.minecraft.resources.Identifier;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerDataServiceTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestHarness.bootstrap();
    }

    private static FakeStackKey key(String path) {
        return new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", path));
    }

    private PlayerDataService service() {
        return new PlayerDataService(PlayerAttachmentAccess.inMemory(PlayerAttachmentKeys::initialValue));
    }

    @Test
    void startsWithZeroEmcAndEmptyKnowledge() {
        PlayerDataService svc = service();
        assertEquals(EmcValue.ZERO, svc.emc());
        assertTrue(svc.knowledge().learned().isEmpty());
        assertFalse(svc.knowledge().fullKnowledge());
    }

    @Test
    void addEmcAccumulates() {
        PlayerDataService svc = service();
        svc.addEmc(EmcValue.of(5));
        svc.addEmc(EmcValue.of(7));
        assertEquals(EmcValue.of(12), svc.emc());
    }

    @Test
    void addEmcOverflowsSafely() {
        PlayerDataService svc = service();
        svc.setEmc(EmcValue.of(Long.MAX_VALUE - 1));
        assertThrows(ArithmeticException.class, () -> svc.addEmc(EmcValue.of(5)));
        // balance unchanged after failed add
        assertEquals(EmcValue.of(Long.MAX_VALUE - 1), svc.emc());
    }

    @Test
    void removeEmcUnderflowRejectedAndBalanceUnchanged() {
        PlayerDataService svc = service();
        svc.setEmc(EmcValue.of(3));
        assertThrows(ArithmeticException.class, () -> svc.removeEmc(EmcValue.of(10)));
        assertEquals(EmcValue.of(3), svc.emc());
    }

    @Test
    void tryRemoveEmcReturnsFalseOnUnderflowWithoutMutating() {
        PlayerDataService svc = service();
        svc.setEmc(EmcValue.of(3));
        assertFalse(svc.tryRemoveEmc(EmcValue.of(10)));
        assertEquals(EmcValue.of(3), svc.emc());
        assertTrue(svc.tryRemoveEmc(EmcValue.of(2)));
        assertEquals(EmcValue.of(1), svc.emc());
    }

    @Test
    void learnAndHasKnowledge() {
        PlayerDataService svc = service();
        NormalizedStackKey gem = key("gem");
        assertTrue(svc.learn(gem));
        assertTrue(svc.hasKnowledge(gem));
        assertFalse(svc.learn(gem), "learning known item is a no-op");
    }

    @Test
    void unlearnUnknownIsNoOp() {
        PlayerDataService svc = service();
        assertFalse(svc.unlearn(key("missing")));
    }

    @Test
    void unlearnKnownRemoves() {
        PlayerDataService svc = service();
        NormalizedStackKey gem = key("gem");
        svc.learn(gem);
        assertTrue(svc.unlearn(gem));
        assertFalse(svc.hasKnowledge(gem));
    }

    @Test
    void fullKnowledgeShortCircuitsHas() {
        PlayerDataService svc = service();
        svc.setFullKnowledge(true);
        assertTrue(svc.hasKnowledge(key("anything")));
        assertTrue(svc.knowledge().fullKnowledge());
    }

    @Test
    void inputLocksRoundTrip() {
        PlayerDataService svc = service();
        NormalizedStackKey gem = key("gem");
        svc.setInputLock(0, gem);
        Optional<NormalizedStackKey> read = svc.inputLock(0);
        assertTrue(read.isPresent());
        assertEquals(gem, read.get());
        assertTrue(svc.inputLock(1).isEmpty());
        assertTrue(svc.inputLock(0).isPresent());
    }

    @Test
    void clearInputLockBySettingNull() {
        PlayerDataService svc = service();
        svc.setInputLock(2, key("gem"));
        svc.setInputLock(2, null);
        assertTrue(svc.inputLock(2).isEmpty());
    }

    @Test
    void gemArmorToggleRoundTrips() {
        PlayerDataService svc = service();
        assertFalse(svc.gemArmorEnabled());
        svc.setGemArmor(true);
        assertTrue(svc.gemArmorEnabled());
        svc.setGemArmor(false);
        assertFalse(svc.gemArmorEnabled());
    }

    @Test
    void samePlayerServicesShareAlchemicalBagContentsByColor() {
        PlayerAttachmentAccess access = PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue);
        PlayerDataService writer = new PlayerDataService(access);
        PlayerDataService reader = new PlayerDataService(access);
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 4);
        diamonds.set(DataComponents.MAX_STACK_SIZE, 64);
        ItemContainerContents contents = ItemContainerContents.fromItems(List.of(diamonds));

        writer.setAlchemicalBagContents(DyeColor.WHITE, contents);
        ItemContainerContents stored = reader.alchemicalBagContents(DyeColor.WHITE);

        NonNullList<ItemStack> inventory = NonNullList.withSize(104, ItemStack.EMPTY);
        stored.copyInto(inventory);
        assertEquals(4, inventory.getFirst().getCount());
    }

    @Test
    void legacyBagMigrationUpdatesSharedContentsAndReturnsRemainder() {
        PlayerAttachmentAccess access = PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue);
        PlayerDataService service = new PlayerDataService(access);
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 4);
        diamonds.set(DataComponents.MAX_STACK_SIZE, 64);
        ItemContainerContents legacy = ItemContainerContents.fromItems(List.of(diamonds));

        ItemContainerContents remaining = service.migrateAlchemicalBagContents(
              DyeColor.WHITE, legacy);

        NonNullList<ItemStack> shared = NonNullList.withSize(104, ItemStack.EMPTY);
        service.alchemicalBagContents(DyeColor.WHITE).copyInto(shared);
        assertEquals(4, shared.getFirst().getCount());
        assertTrue(remaining.nonEmptyItemCopyStream().findAny().isEmpty());
    }
}
