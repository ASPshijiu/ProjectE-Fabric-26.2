package moze_intel.projecte.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import moze_intel.projecte.emc.NormalizedStackKey;
import moze_intel.projecte.emc.recipe.MinecraftStackKeyFactory;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /projecte knowledge sub-tree: learn, unlearn, test, clear.
 *
 * <p>Pure operations are unit-tested directly; the Brigadier node resolves the item argument into a
 * normalized stack key through {@link MinecraftStackKeyFactory} and reports feedback.
 */
public final class KnowledgeCommand {
    private KnowledgeCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register(CommandBuildContext context) {
        return Commands.literal("knowledge")
              .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
              .then(Commands.literal("learn")
                    .then(Commands.argument("item", ItemArgument.item(context))
                          .executes(ctx -> learn(ctx))))
              .then(Commands.literal("unlearn")
                    .then(Commands.argument("item", ItemArgument.item(context))
                          .executes(ctx -> unlearn(ctx))))
              .then(Commands.literal("test")
                    .then(Commands.argument("item", ItemArgument.item(context))
                          .executes(ctx -> test(ctx))))
              .then(Commands.literal("clear")
                    .executes(ctx -> clear(ctx)));
    }

    // ---- pure operations (unit tested) ----

    public static boolean learn(PlayerDataService service, NormalizedStackKey key) {
        return service.learn(key);
    }

    public static boolean unlearn(PlayerDataService service, NormalizedStackKey key) {
        return service.unlearn(key);
    }

    public static boolean hasKnowledge(PlayerDataService service, NormalizedStackKey key) {
        return service.hasKnowledge(key);
    }

    public static void clear(PlayerDataService service) {
        service.clearKnowledge();
    }

    public static void setFullKnowledge(PlayerDataService service, boolean full) {
        service.setFullKnowledge(full);
    }

    // ---- Brigadier dispatch ----

    private static int learn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        NormalizedStackKey key = keyOf(ctx, target);
        PlayerDataService service = service(target);
        boolean changed = learn(service, key);
        ctx.getSource().sendSuccess(() -> Component.literal(
              target.getName().getString() + (changed ? " learned " : " already knew ") + key.canonicalString()), true);
        return changed ? 1 : 0;
    }

    private static int unlearn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        NormalizedStackKey key = keyOf(ctx, target);
        PlayerDataService service = service(target);
        boolean changed = unlearn(service, key);
        ctx.getSource().sendSuccess(() -> Component.literal(
              target.getName().getString() + (changed ? " forgot " : " did not know ") + key.canonicalString()), true);
        return changed ? 1 : 0;
    }

    private static int test(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        NormalizedStackKey key = keyOf(ctx, target);
        PlayerDataService service = service(target);
        boolean knows = hasKnowledge(service, key);
        ctx.getSource().sendSuccess(() -> Component.literal(
              target.getName().getString() + (knows ? " knows " : " does not know ") + key.canonicalString()), false);
        return knows ? 1 : 0;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        PlayerDataService service = service(target);
        clear(service);
        ctx.getSource().sendSuccess(() -> Component.literal(
              "Cleared knowledge of " + target.getName().getString()), true);
        return 1;
    }

    private static NormalizedStackKey keyOf(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        ItemInput input = ItemArgument.getItem(ctx, "item");
        MinecraftStackKeyFactory factory = new MinecraftStackKeyFactory(player.registryAccess());
        return factory.key(input.createItemStack(1));
    }

    private static PlayerDataService service(ServerPlayer player) {
        return new PlayerDataService(PlayerAttachmentKeys.fabricAdapter(player));
    }
}
