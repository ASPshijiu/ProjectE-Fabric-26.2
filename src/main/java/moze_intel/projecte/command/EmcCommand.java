package moze_intel.projecte.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import java.util.Optional;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.emc.EmcValue;
import moze_intel.projecte.player.PlayerAttachmentKeys;
import moze_intel.projecte.player.PlayerDataService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /projecte emc sub-tree: add, remove, set, get, test.
 *
 * <p>The pure state-mutating operations are exposed as package-static methods so they can be unit
 * tested directly; the Brigadier node only resolves the target player and reports feedback.
 */
public final class EmcCommand {
    private EmcCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("emc")
              .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
              .then(Commands.literal("set")
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                          .executes(ctx -> set(ctx, false))))
              .then(Commands.literal("add")
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                          .executes(ctx -> add(ctx, false))))
              .then(Commands.literal("remove")
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                          .executes(ctx -> remove(ctx, false))))
              .then(Commands.literal("get")
                    .executes(ctx -> get(ctx, false)))
              .then(Commands.literal("test")
                    .then(Commands.argument("amount", LongArgumentType.longArg(0))
                          .executes(ctx -> test(ctx, false))));
    }

    // ---- pure operations (unit tested) ----

    public static void setEmc(PlayerDataService service, EmcValue value) {
        Objects.requireNonNull(value, "value");
        service.setEmc(value);
    }

    public static void addEmc(PlayerDataService service, EmcValue delta) {
        service.addEmc(delta);
    }

    public static void removeEmc(PlayerDataService service, EmcValue delta) {
        service.removeEmc(delta);
    }

    public static Optional<EmcValue> queryEmc(PlayerDataService service) {
        return Optional.of(service.emc());
    }

    public static boolean hasAtLeast(PlayerDataService service, EmcValue amount) {
        return service.emc().compareTo(amount) >= 0;
    }

    // ---- Brigadier dispatch ----

    private static int set(CommandContext<CommandSourceStack> ctx, boolean otherPlayer) throws CommandSyntaxException {
        long amount = LongArgumentType.getLong(ctx, "amount");
        ServerPlayer target = resolveTarget(ctx, otherPlayer);
        PlayerDataService service = service(target);
        try {
            setEmc(service, EmcValue.of(amount));
        } catch (IllegalArgumentException exception) {
            ctx.getSource().sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
              "Set EMC of " + target.getName().getString() + " to " + amount), true);
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> ctx, boolean otherPlayer) throws CommandSyntaxException {
        long amount = LongArgumentType.getLong(ctx, "amount");
        ServerPlayer target = resolveTarget(ctx, otherPlayer);
        PlayerDataService service = service(target);
        try {
            addEmc(service, EmcValue.of(amount));
        } catch (ArithmeticException exception) {
            ctx.getSource().sendFailure(Component.literal("EMC overflow"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
              "Added " + amount + " EMC to " + target.getName().getString() + " (now " + service.emc().longValue() + ")"), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx, boolean otherPlayer) throws CommandSyntaxException {
        long amount = LongArgumentType.getLong(ctx, "amount");
        ServerPlayer target = resolveTarget(ctx, otherPlayer);
        PlayerDataService service = service(target);
        try {
            removeEmc(service, EmcValue.of(amount));
        } catch (ArithmeticException exception) {
            ctx.getSource().sendFailure(Component.literal("Insufficient EMC"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
              "Removed " + amount + " EMC from " + target.getName().getString() + " (now " + service.emc().longValue() + ")"), true);
        return 1;
    }

    private static int get(CommandContext<CommandSourceStack> ctx, boolean otherPlayer) throws CommandSyntaxException {
        ServerPlayer target = resolveTarget(ctx, otherPlayer);
        PlayerDataService service = service(target);
        long value = service.emc().longValue();
        ctx.getSource().sendSuccess(() -> Component.literal(
              target.getName().getString() + " has " + value + " EMC"), false);
        return 1;
    }

    private static int test(CommandContext<CommandSourceStack> ctx, boolean otherPlayer) throws CommandSyntaxException {
        long amount = LongArgumentType.getLong(ctx, "amount");
        ServerPlayer target = resolveTarget(ctx, otherPlayer);
        PlayerDataService service = service(target);
        boolean has = hasAtLeast(service, EmcValue.of(amount));
        ctx.getSource().sendSuccess(() -> Component.literal(
              target.getName().getString() + (has ? " has at least " : " lacks ") + amount + " EMC"), false);
        return has ? 1 : 0;
    }

    private static ServerPlayer resolveTarget(CommandContext<CommandSourceStack> ctx, boolean otherPlayer) throws CommandSyntaxException {
        if (otherPlayer) {
            return EntityArgument.getPlayer(ctx, "player");
        }
        return ctx.getSource().getPlayerOrException();
    }

    private static PlayerDataService service(ServerPlayer player) {
        return new PlayerDataService(PlayerAttachmentKeys.fabricAdapter(player));
    }

    @SuppressWarnings("unused")
    private static String modId() {
        return ProjectEAPI.MOD_ID;
    }
}
