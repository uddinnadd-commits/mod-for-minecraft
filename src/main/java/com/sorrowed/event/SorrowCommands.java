package com.sorrowed.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sorrowed.Sorrowed;
import com.sorrowed.mind.Sorrow;
import com.sorrowed.mind.SorrowEvents;
import com.sorrowed.story.Objectives;
import com.sorrowed.story.Progress;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** /sorrowed sorrow, /sorrowed event, /sorrowed story (op only, for testing). */
@Mod.EventBusSubscriber(modid = Sorrowed.MODID)
public class SorrowCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sorrowed")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("sorrow")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    Sorrow.set(p, value);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Sorrow set to " + value), false);
                                    return 1;
                                })))
                .then(Commands.literal("event")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    boolean ok = SorrowEvents.fire(name, p);
                                    ctx.getSource().sendSuccess(() -> Component.literal(ok
                                            ? "Fired " + name
                                            : "Could not fire '" + name + "' here. Events: " + SorrowEvents.names()), false);
                                    return ok ? 1 : 0;
                                })))
                // /sorrowed story status | complete <id|all> | reset
                .then(Commands.literal("story")
                        .then(Commands.literal("status").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            StringBuilder sb = new StringBuilder("Stage " + Progress.stage(p) + ", "
                                    + Progress.count(p) + "/" + Objectives.ALL.size() + ":");
                            for (Objectives.Objective o : Objectives.ALL) {
                                sb.append(' ').append(o.id()).append(Progress.done(p, o.id()) ? "[x]" : "[ ]");
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                            return 1;
                        }))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            List<String> ids = new ArrayList<>();
                                            ids.add("all");
                                            for (Objectives.Objective o : Objectives.ALL) ids.add(o.id());
                                            return SharedSuggestionProvider.suggest(ids, b);
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            String id = StringArgumentType.getString(ctx, "id");
                                            if ("all".equals(id)) {
                                                for (Objectives.Objective o : Objectives.ALL) Progress.complete(p, o.id());
                                            } else if (Objectives.get(id) == null || !Progress.complete(p, id)) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown or already done: " + id));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("Completed " + id), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("reset").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            Progress.reset(p);
                            ctx.getSource().sendSuccess(() -> Component.literal("Story progress reset"), false);
                            return 1;
                        }))));
    }
}
