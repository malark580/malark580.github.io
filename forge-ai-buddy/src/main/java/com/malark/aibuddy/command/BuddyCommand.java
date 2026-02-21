package com.malark.aibuddy.command;

import com.malark.aibuddy.buddy.BuddyController;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BuddyCommand {
    private BuddyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("buddy")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("spawn")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            BuddyController.spawnFor(player);
                            ctx.getSource().sendSuccess(() -> Component.literal("AI Buddy spawned."), false);
                            return 1;
                        }))
                .then(Commands.literal("mine")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            BuddyController.mineFor(player);
                            return 1;
                        }))
                .then(Commands.literal("build")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            BuddyController.buildFor(player);
                            return 1;
                        }))
                .then(Commands.literal("say")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String text = StringArgumentType.getString(ctx, "text");
                                    BuddyController.handlePrompt(player, text);
                                    return 1;
                                }))));
    }
}
