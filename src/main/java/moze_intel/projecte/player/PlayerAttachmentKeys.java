package moze_intel.projecte.player;

import moze_intel.projecte.emc.EmcValue;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Opaque attachment key tokens plus the production adapter to real Fabric {@link AttachmentType}s.
 *
 * <p>Tests use the string constants directly as keys; production code wraps them via
 * {@link #fabricAdapter()} so the service reads/writes the registered Fabric attachments.
 */
public final class PlayerAttachmentKeys {
    public static final String KNOWLEDGE = "projecte:knowledge";
    public static final String EMC = "projecte:emc";
    public static final String INPUT_LOCKS = "projecte:input_locks";
    public static final String GEM_ARMOR = "projecte:gem_armor_state";

    private PlayerAttachmentKeys() {
    }

    /**
     * @return an access that maps the opaque string keys to the registered Fabric attachment types.
     */
    public static PlayerAttachmentAccess fabricAdapter(net.fabricmc.fabric.api.attachment.v1.AttachmentTarget target) {
        return new PlayerAttachmentAccess() {
            @Override
            public <A> A modify(Object key, Class<A> type, java.util.function.UnaryOperator<A> modifier) {
                return target.modifyAttached(resolve(key), modifier);
            }

            @SuppressWarnings("unchecked")
            private <A> AttachmentType<A> resolve(Object key) {
                return (AttachmentType<A>) switch (key.toString()) {
                    case KNOWLEDGE -> PlayerAttachments.KNOWLEDGE;
                    case EMC -> PlayerAttachments.EMC;
                    case INPUT_LOCKS -> PlayerAttachments.INPUT_LOCKS;
                    case GEM_ARMOR -> PlayerAttachments.GEM_ARMOR;
                    default -> throw new IllegalArgumentException("unknown attachment key: " + key);
                };
            }
        };
    }

    /**
     * Initial-value factory keyed by the opaque string tokens, used by both tests and the access
     * adapter so the in-memory store matches the registered attachment initializers.
     */
    public static Object initialValue(Object key) {
        return switch (key.toString()) {
            case KNOWLEDGE -> PlayerKnowledge.empty();
            case EMC -> EmcValue.ZERO;
            case INPUT_LOCKS -> PlayerInputLocks.empty();
            case GEM_ARMOR -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("unknown attachment key: " + key);
        };
    }
}
