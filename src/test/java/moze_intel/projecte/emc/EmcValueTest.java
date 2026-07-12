package moze_intel.projecte.emc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class EmcValueTest {
    @Test
    void rejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> EmcValue.of(-1));
    }

    @Test
    void performsCheckedArithmetic() {
        assertEquals(EmcValue.of(12), EmcValue.of(5).add(EmcValue.of(7)));
        assertEquals(EmcValue.of(15), EmcValue.of(5).multiply(3));
        assertEquals(EmcValue.of(3), EmcValue.of(5).subtract(EmcValue.of(2)));
    }

    @Test
    void rejectsUnderflowAndOverflow() {
        assertThrows(ArithmeticException.class, () -> EmcValue.of(2).subtract(EmcValue.of(3)));
        assertThrows(ArithmeticException.class, () -> EmcValue.of(Long.MAX_VALUE).add(EmcValue.of(1)));
        assertThrows(ArithmeticException.class, () -> EmcValue.of(Long.MAX_VALUE).multiply(2));
    }
}
