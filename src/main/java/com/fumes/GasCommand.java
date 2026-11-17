package com.fumes;

import com.fumes.gas.GasBlock;
import com.fumes.gas.GasSources;
import com.fumes.gas.GasType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.Locale;

/**
 * /gas spawn &lt;gas&gt; [cells] [amount]   - release a cloud where you stand
 * /gas clear [radius]                   - remove all gas around you
 * /gas ignite                           - set off the flammable cloud you're standing in
 * Operators only.
 */
public final class GasCommand {

    public static void init() {
        CommandRegistrationCallback.EVENT.register((d, ctx, sel) -> d.register(Commands.literal("gas")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn").then(Commands.argument("gas", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(GasType.values()).map(t -> t.id), b))
                        .executes(c -> spawn(c, 20, GasBlock.MAX))
                        .then(Commands.argument("cells", IntegerArgumentType.integer(1, 2000))
                                .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "cells"), GasBlock.MAX))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, GasBlock.MAX))
                                        .executes(c -> spawn(c, IntegerArgumentType.getInteger(c, "cells"), IntegerArgumentType.getInteger(c, "amount")))))))
                .then(Commands.literal("clear").executes(c -> clear(c, 16))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64)).executes(c -> clear(c, IntegerArgumentType.getInteger(c, "radius")))))
                .then(Commands.literal("ignite").executes(GasCommand::ignite))));
    }

    private static int spawn(CommandContext<CommandSourceStack> c, int cells, int amount) {
        String name = StringArgumentType.getString(c, "gas").toLowerCase(Locale.ROOT);
        GasType t = Arrays.stream(GasType.values()).filter(g -> g.id.equals(name)).findFirst().orElse(null);
        if (t == null) {
            c.getSource().sendFailure(Component.literal("Unknown gas: " + name));
            return 0;
        }
        BlockPos pos = BlockPos.containing(c.getSource().getPosition()).above();
        int n = GasSources.pocket(c.getSource().getLevel(), pos, t, cells, amount);
        c.getSource().sendSuccess(() -> Component.literal("Released " + t.id + " into " + n + " cells"), true);
        return n;
    }

    private static int clear(CommandContext<CommandSourceStack> c, int r) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos center = BlockPos.containing(c.getSource().getPosition());
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (level.getBlockState(p).getBlock() instanceof GasBlock) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                n++;
            }
        }
        int fn = n;
        c.getSource().sendSuccess(() -> Component.literal("Removed " + fn + " gas cells"), true);
        return n;
    }

    private static int ignite(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = BlockPos.containing(c.getSource().getPosition());
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 3, 2))) {
            if (level.getBlockState(p).getBlock() instanceof GasBlock g && g.type.flammable) {
                int n = GasBlock.ignite(level, p.immutable());
                c.getSource().sendSuccess(() -> Component.literal("Ignited " + n + " cells"), true);
                return n;
            }
        }
        c.getSource().sendFailure(Component.literal("No flammable gas nearby"));
        return 0;
    }

    private GasCommand() {
    }
}
