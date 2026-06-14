package dev.strengthsmp;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class StrengthCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("strength")
                // /strength — show your own strength
                .executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrThrow();
                    int level = StrengthSMP.getStrength(player.getUuid());
                    ctx.getSource().sendFeedback(() ->
                        Text.literal("§7Your strength: §e" + level +
                            " §7(§a+" + Math.max(level, 0) + "§7/§c" + Math.min(level, 0) + "§7 dmg)"),
                        false);
                    return 1;
                })
                // /strength <player> — check someone else's strength
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> {
                        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                        int level = StrengthSMP.getStrength(target.getUuid());
                        ctx.getSource().sendFeedback(() ->
                            Text.literal("§e" + target.getName().getString() +
                                "§7's strength: §e" + level),
                            false);
                        return 1;
                    })
                    // /strength <player> set <value> — op only
                    .then(CommandManager.literal("set")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("value", IntegerArgumentType.integer(
                                StrengthSMP.MIN_STRENGTH, StrengthSMP.MAX_STRENGTH))
                            .executes(ctx -> {
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                StrengthSMP.setStrength(target, value);
                                ctx.getSource().sendFeedback(() ->
                                    Text.literal("§7Set §e" + target.getName().getString() +
                                        "§7's strength to §e" + value),
                                    true);
                                return 1;
                            })
                        )
                    )
                )
                // /strength top — leaderboard (online players)
                .then(CommandManager.literal("top")
                    .executes(ctx -> {
                        var server = ctx.getSource().getServer();
                        var players = server.getPlayerManager().getPlayerList();
                        var sb = new StringBuilder("§7--- Strength Leaderboard ---\n");
                        players.stream()
                            .sorted((a, b) -> Integer.compare(
                                StrengthSMP.getStrength(b.getUuid()),
                                StrengthSMP.getStrength(a.getUuid())))
                            .forEach(p -> sb.append("§e")
                                .append(p.getName().getString())
                                .append("§7: §e")
                                .append(StrengthSMP.getStrength(p.getUuid()))
                                .append("\n"));
                        ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                        return 1;
                    })
                )
            );
        });
    }
}
