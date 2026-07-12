package moze_intel.projecte.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import moze_intel.projecte.api.ProjectEAPI;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Registers the {@code /projecte} command tree, host to the {@code emc} and {@code knowledge}
 * sub-trees. Registration is invoked from the Fabric command callback.
 */
public final class ProjectECommands {
    private ProjectECommands() {
    }

    public static void register(
          CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context
    ) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ProjectEAPI.MOD_ID)
              .then(EmcCommand.register())
              .then(KnowledgeCommand.register(context));
        dispatcher.register(root);
    }
}
