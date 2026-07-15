package moze_intel.projecte.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Test;

class BlockItemTranslationKeyTest {
    @Test
    void blockItemsUseBlockTranslationKeys() {
        Identifier id = Identifier.fromNamespaceAndPath("projecte", "alchemical_coal_block");
        Item.Properties properties = ModBlocks.blockItemProperties(id);

        assertEquals("block.projecte.alchemical_coal_block", effectiveDescriptionId(properties));
    }

    private static String effectiveDescriptionId(Item.Properties properties) {
        try {
            Method method = Item.Properties.class.getDeclaredMethod("effectiveDescriptionId");
            method.setAccessible(true);
            return (String) method.invoke(properties);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail("Minecraft item description key cannot be inspected", e);
        }
        throw new AssertionError("unreachable");
    }
}
