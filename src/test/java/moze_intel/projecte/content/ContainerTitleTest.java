package moze_intel.projecte.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import moze_intel.projecte.content.blocks.TransmutationTableBlockEntity;
import moze_intel.projecte.content.items.AlchemicalBagItem;
import moze_intel.projecte.content.items.TransmutationTabletItem;
import org.junit.jupiter.api.Test;

class ContainerTitleTest {
    @Test
    void interactiveContainersUseKeysPresentInTheLanguagePack() {
        assertEquals("container.projecte.transmutation_table", TransmutationTableBlockEntity.TITLE_KEY);
        assertEquals("container.projecte.transmutation_tablet", TransmutationTabletItem.TITLE_KEY);
        assertEquals("container.projecte.alchemical_bag", AlchemicalBagItem.TITLE_KEY);
    }
}
