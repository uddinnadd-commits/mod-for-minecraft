package com.sorrowed.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sorrowed.Sorrowed;
import com.sorrowed.mind.Sorrow;
import com.sorrowed.mind.SorrowEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /sorrowed sorrow <0-100>   and   /sorrowed event <name>   (op only, for testing). */
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
                                }))));
    }
}
