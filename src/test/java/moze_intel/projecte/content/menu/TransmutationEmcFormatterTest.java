package moze_intel.projecte.content.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

class TransmutationEmcFormatterTest {
    @Test
    void keepsExactGroupedValuesBelowOneTrillion() {
        Component formatted = TransmutationEmcFormatter.format(999_999_999_999L);

        assertEquals("999,999,999,999", formatted.getString());
    }

    @Test
    void compactsLargeValuesSoTheyFitTheTableDisplay() {
        Component formatted = TransmutationEmcFormatter.format(1_234_000_000_000L);
        TranslatableContents text = assertInstanceOf(
              TranslatableContents.class, formatted.getContents());

        assertEquals("emc.projecte.postfix.0", text.getKey());
        assertEquals("1.23", text.getArgs()[0]);
    }

    @Test
    void rejectsNegativeBalances() {
        org.junit.jupiter.api.Assertions.assertThrows(
              IllegalArgumentException.class,
              () -> TransmutationEmcFormatter.format(-1));
    }
}
