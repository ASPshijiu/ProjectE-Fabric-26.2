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

    @Test
    void removeQuotesWithoutChargingAndOnTakeCharges() {
        ItemStackKey stone = new ItemStackKey(
              Identifier.withDefaultNamespace("stone"), Map.of());
        EmcMappingSnapshot<moze_intel.projecte.emc.NormalizedStackKey> snapshot =
              new EmcMappingSnapshot<>(1, Map.of(stone, EmcValue.of(4)));
        PlayerDataService service = new PlayerDataService(PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue));
        service.setEmc(EmcValue.of(10));
        service.learn(stone);
        SimpleContainer output = new SimpleContainer(new ItemStack(Items.STONE));
        TransmuteOutputSlot slot = new TransmuteOutputSlot(
              output, 0, 0, 0, () -> true, service, ignored -> stone, () -> snapshot);

        // remove 只限量不扣费（数量还受堆叠上限约束，此处只断言"未扣费"这一不变式）。
        ItemStack extracted = slot.remove(64);
        int taken = extracted.getCount();
        assertTrue(taken > 0, "买得起时 remove 应产出物品");
        assertEquals(EmcValue.of(10), service.emc(), "remove 不应扣费");

        // 扣费发生在 onTake（覆盖 SWAP 等不经 remove 的路径）。
        slot.chargeOnTake(extracted);
        assertEquals(taken, extracted.getCount(), "买得起时不应没收物品");
        assertEquals(EmcValue.of(10 - 4L * taken), service.emc(), "onTake 应按实际取走数量扣费");
    }

    @Test
    void onTakeConfiscatesWhenBalanceRaced() {
        ItemStackKey stone = new ItemStackKey(
              Identifier.withDefaultNamespace("stone"), Map.of());
        EmcMappingSnapshot<moze_intel.projecte.emc.NormalizedStackKey> snapshot =
              new EmcMappingSnapshot<>(1, Map.of(stone, EmcValue.of(4)));
        PlayerDataService service = new PlayerDataService(PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue));
        service.setEmc(EmcValue.of(10));
        service.learn(stone);
        SimpleContainer output = new SimpleContainer(new ItemStack(Items.STONE));
        TransmuteOutputSlot slot = new TransmuteOutputSlot(
              output, 0, 0, 0, () -> true, service, ignored -> stone, () -> snapshot);

        ItemStack extracted = slot.remove(2);
        // 模拟同 tick 其他路径耗尽余额。
        service.setEmc(EmcValue.of(3));
        slot.chargeOnTake(extracted);

        assertEquals(0, extracted.getCount());
        assertEquals(EmcValue.of(3), service.emc());
    }

    @Test
    void mayPickupGatesSwapByAffordability() {
        ItemStackKey stone = new ItemStackKey(
              Identifier.withDefaultNamespace("stone"), Map.of());
        EmcMappingSnapshot<moze_intel.projecte.emc.NormalizedStackKey> snapshot =
              new EmcMappingSnapshot<>(1, Map.of(stone, EmcValue.of(4)));
        PlayerDataService service = new PlayerDataService(PlayerAttachmentAccess.inMemory(
              PlayerAttachmentKeys::initialValue));
        service.learn(stone);
        SimpleContainer output = new SimpleContainer(new ItemStack(Items.STONE));
        TransmuteOutputSlot slot = new TransmuteOutputSlot(
              output, 0, 0, 0, () -> true, service, ignored -> stone, () -> snapshot);

        service.setEmc(EmcValue.of(3));
        assertTrue(!slot.mayPickup(null), "买不起时 SWAP 门禁应拒绝");
        service.setEmc(EmcValue.of(4));
        assertTrue(slot.mayPickup(null), "买得起时应放行");
    }
}
