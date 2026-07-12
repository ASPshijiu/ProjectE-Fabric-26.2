package moze_intel.projecte.emc;

public record EmcValue(long longValue) implements Comparable<EmcValue> {
    public static final EmcValue ZERO = new EmcValue(0);

    public EmcValue {
        if (longValue < 0) {
            throw new IllegalArgumentException("EMC cannot be negative: " + longValue);
        }
    }

    public static EmcValue of(long value) {
        return value == 0 ? ZERO : new EmcValue(value);
    }

    public EmcValue add(EmcValue other) {
        return of(Math.addExact(longValue, other.longValue));
    }

    public EmcValue subtract(EmcValue other) {
        long result = Math.subtractExact(longValue, other.longValue);
        if (result < 0) {
            throw new ArithmeticException("EMC underflow");
        }
        return of(result);
    }

    public EmcValue multiply(long factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("EMC factor cannot be negative: " + factor);
        }
        return of(Math.multiplyExact(longValue, factor));
    }

    @Override
    public int compareTo(EmcValue other) {
        return Long.compare(longValue, other.longValue);
    }
}
