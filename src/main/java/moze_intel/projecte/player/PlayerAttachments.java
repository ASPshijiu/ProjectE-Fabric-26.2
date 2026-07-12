package moze_intel.projecte.player;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.emc.EmcValue;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * Central registration of all per-player Fabric data attachments.
 *
 * <p>Each attachment is persistent (saved with the world), copies on death, and syncs only to the
 * owning player ({@link AttachmentSyncPredicate#targetOnly()}) so gameplay reads its own state
 * client-side while other players never learn someone else's EMC or knowledge.
 */
public final class PlayerAttachments {
    public static final AttachmentType<PlayerKnowledge> KNOWLEDGE = AttachmentRegistry.<PlayerKnowledge>builder()
          .initializer(PlayerKnowledge::empty)
          .persistent(PlayerDataCodecs.KNOWLEDGE_CODEC)
          .copyOnDeath()
          .syncWith(PlayerDataCodecs.KNOWLEDGE_STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
          .buildAndRegister(id("knowledge"));

    public static final AttachmentType<EmcValue> EMC = AttachmentRegistry.<EmcValue>builder()
          .initializer(() -> EmcValue.ZERO)
          .persistent(PlayerDataCodecs.EMC_CODEC)
          .copyOnDeath()
          .syncWith(PlayerDataCodecs.EMC_STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
          .buildAndRegister(id("emc"));

    public static final AttachmentType<PlayerInputLocks> INPUT_LOCKS = AttachmentRegistry.<PlayerInputLocks>builder()
          .initializer(PlayerInputLocks::empty)
          .persistent(PlayerDataCodecs.INPUT_LOCKS_CODEC)
          .copyOnDeath()
          .syncWith(PlayerDataCodecs.INPUT_LOCKS_STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
          .buildAndRegister(id("input_locks"));

    public static final AttachmentType<Boolean> GEM_ARMOR = AttachmentRegistry.<Boolean>builder()
          .initializer(() -> false)
          .persistent(com.mojang.serialization.Codec.BOOL)
          .copyOnDeath()
          .buildAndRegister(id("gem_armor_state"));

    private PlayerAttachments() {
    }

    /**
     * Triggers static initialization so the attachments register during mod init even though
     * {@link AttachmentRegistry} registers on first class reference.
     */
    public static void init() {
        // Touching each field forces class-load and registration.
        @SuppressWarnings("unused")
        Object touch = new Object[]{KNOWLEDGE, EMC, INPUT_LOCKS, GEM_ARMOR};
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ProjectEAPI.MOD_ID, path);
    }
}
