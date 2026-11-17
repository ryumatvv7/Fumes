package com.fumes.gas;

import com.fumes.Fumes;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Where gas comes from:
 * - mining coal ore can open a methane pocket;
 * - mining deep rock can open a pocket of methane, carbon dioxide or (very deep) hydrogen sulfide;
 * - fires, lit furnaces and campfires in closed rooms give off carbon monoxide (see mixins);
 * - lava meeting water makes steam (see ServerLevelMixin).
 */
public final class GasSources {

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel sl) || player.isCreative()) return;
            onMined(sl, pos, state);
        });
    }

    private static void onMined(ServerLevel level, BlockPos pos, BlockState state) {
        RandomSource r = level.getRandom();
        if (state.is(Fumes.RELEASES_METHANE)) {
            if (r.nextFloat() < 0.3f) pocket(level, pos, GasType.METHANE, 10 + r.nextInt(20), 6 + r.nextInt(3));
            return;
        }
        if (!state.is(Fumes.GAS_POCKET_ROCK) || pos.getY() > 0 || level.dimension() != Level.OVERWORLD) return;
        if (r.nextFloat() >= 0.006f) return;
        GasType t;
        if (pos.getY() < -32) t = r.nextBoolean() ? GasType.HYDROGEN_SULFIDE : GasType.CARBON_DIOXIDE;
        else t = r.nextBoolean() ? GasType.METHANE : GasType.CARBON_DIOXIDE;
        pocket(level, pos, t, 16 + r.nextInt(32), 5 + r.nextInt(4));
    }

    /** Adds gas to one cell: fills air, or tops up the same gas. @return true if anything was added */
    public static boolean add(Level level, BlockPos pos, GasType type, int amount) {
        BlockState s = level.getBlockState(pos);
        GasBlock g = Fumes.gas(type);
        if (s.isAir()) {
            level.setBlock(pos, g.defaultBlockState().setValue(GasBlock.AMOUNT, Math.min(GasBlock.MAX, amount)), Block.UPDATE_ALL);
            return true;
        }
        if (s.is(g) && s.getValue(GasBlock.AMOUNT) < GasBlock.MAX) {
            level.setBlock(pos, s.setValue(GasBlock.AMOUNT, Math.min(GasBlock.MAX, s.getValue(GasBlock.AMOUNT) + amount)), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    /** Fills up to {@code cells} connected empty cells starting at {@code start} (e.g. the hole a player just mined). */
    public static int pocket(Level level, BlockPos start, GasType type, int cells, int amount) {
        int placed = 0;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && placed < cells && seen.size() < cells * 8) {
            BlockPos p = queue.poll();
            if (!level.getBlockState(p).isAir()) continue;
            if (add(level, p, type, amount)) placed++;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) queue.add(n);
            }
        }
        return placed;
    }

    /** Combustion in a closed room: puts carbon monoxide into an empty neighbour cell without sky access. */
    public static void smoke(ServerLevel level, BlockPos pos, int amount) {
        Direction[] order = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : order) {
            BlockPos p = pos.relative(d);
            BlockState s = level.getBlockState(p);
            if (!(s.isAir() || s.is(Fumes.gas(GasType.CARBON_MONOXIDE)))) continue;
            if (level.getBrightness(LightLayer.SKY, p) >= 15) return; //chimney or open air: smoke escapes
            add(level, p, GasType.CARBON_MONOXIDE, amount);
            return;
        }
    }

    private GasSources() {
    }
}
