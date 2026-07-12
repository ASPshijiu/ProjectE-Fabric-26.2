package moze_intel.projecte.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import moze_intel.projecte.emc.FakeStackKey;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.player.PlayerAttachmentAccess;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class KnowledgeCommandTest {
    private PlayerDataService service() {
        return new PlayerDataService(PlayerAttachmentAccess.inMemory(PlayerAttachmentKeys::initialValue));
    }

    private static NormalizedStackKey key(String path) {
        return new FakeStackKey(Identifier.fromNamespaceAndPath("projecte", path));
    }

    @Test
    void learnThenTestTrue() {
        PlayerDataService svc = service();
        KnowledgeCommand.learn(svc, key("gem"));
        assertTrue(KnowledgeCommand.hasKnowledge(svc, key("gem")));
        assertFalse(KnowledgeCommand.hasKnowledge(svc, key("other")));
    }

    @Test
    void learnIsIdempotent() {
        PlayerDataService svc = service();
        assertTrue(KnowledgeCommand.learn(svc, key("gem")));
        assertFalse(KnowledgeCommand.learn(svc, key("gem")), "second learn is a no-op");
    }

    @Test
    void unlearnRemovesKnowledge() {
        PlayerDataService svc = service();
        KnowledgeCommand.learn(svc, key("gem"));
        assertTrue(KnowledgeCommand.unlearn(svc, key("gem")));
        assertFalse(KnowledgeCommand.hasKnowledge(svc, key("gem")));
    }

    @Test
    void unlearnUnknownIsNoOp() {
        PlayerDataService svc = service();
        assertFalse(KnowledgeCommand.unlearn(svc, key("missing")));
    }

    @Test
    void clearEmptiesLearnedSet() {
        PlayerDataService svc = service();
        KnowledgeCommand.learn(svc, key("gem"));
        KnowledgeCommand.learn(svc, key("ring"));
        KnowledgeCommand.clear(svc);
        assertFalse(KnowledgeCommand.hasKnowledge(svc, key("gem")));
        assertFalse(KnowledgeCommand.hasKnowledge(svc, key("ring")));
    }

    @Test
    void clearKeepsFullKnowledgeFlagOffByDefault() {
        PlayerDataService svc = service();
        KnowledgeCommand.clear(svc);
        assertFalse(svc.knowledge().fullKnowledge());
    }

    @Test
    void setFullKnowledgeThenClearResetsIt() {
        PlayerDataService svc = service();
        KnowledgeCommand.setFullKnowledge(svc, true);
        assertTrue(svc.knowledge().fullKnowledge());
        KnowledgeCommand.clear(svc);
        assertFalse(svc.knowledge().fullKnowledge());
        assertFalse(KnowledgeCommand.hasKnowledge(svc, key("anything")));
    }
}
