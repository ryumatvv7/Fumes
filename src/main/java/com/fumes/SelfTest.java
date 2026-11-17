package com.fumes;

import com.fumes.gas.GasBlock;
import com.fumes.gas.GasEffects;
import com.fumes.gas.GasSources;
import com.fumes.gas.GasType;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Dev only (run with -Dfumes.selfTest=true, the dev server does): checks gas physics on server start. */
final class SelfTest {

    private static final List<String> failures = new ArrayList<>();
    private static int checks;

    static void register() {
        if (!Boolean.getBoolean("fumes.selfTest")) return;
        ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
    }

    private static void check(String name, boolean ok) {
        checks++;
        if (!ok) failures.add(name);
    }

    /** Builds a closed stone box with an inner air space of the given size. */
    private static void box(ServerLevel level, BlockPos o, int sx, int sy, int sz) {
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, -1, -1), o.offset(sx, sy, sz))) {
            boolean wall = p.getX() < o.getX() || p.getY() < o.getY() || p.getZ() < o.getZ()
                    || p.getX() >= o.getX() + sx || p.getY() >= o.getY() + sy || p.getZ() >= o.getZ() + sz;
            level.setBlock(p, wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        }
    }

    /** Runs the gas physics on every cell of the area, n times (scheduled ticks don't run during the test). */
    private static void step(ServerLevel level, BlockPos o, int sx, int sy, int sz, int n) {
        for (int i = 0; i < n; i++)
            for (BlockPos p : BlockPos.betweenClosed(o, o.offset(sx - 1, sy - 1, sz - 1))) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof GasBlock) s.tick(level, p.immutable(), level.getRandom());
            }
    }

    private static int total(ServerLevel level, BlockPos o, int sx, int sy, int sz, GasType t) {
        int sum = 0;
        for (BlockPos p : BlockPos.betweenClosed(o, o.offset(sx - 1, sy - 1, sz - 1))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Fumes.gas(t))) sum += s.getValue(GasBlock.AMOUNT);
        }
        return sum;
    }

    private static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos o = new BlockPos(0, -50, 0);
        level.getChunk(o);
        try {
            // methane rises to the top of a closed shaft
            box(level, o, 1, 6, 1);
            GasSources.add(level, o, GasType.METHANE, 8);
            step(level, o, 1, 6, 1, 10);
            check("methane rises to the ceiling", level.getBlockState(o.above(5)).is(Fumes.gas(GasType.METHANE)));

            // carbon dioxide sinks to the floor
            box(level, o, 1, 6, 1);
            GasSources.add(level, o.above(5), GasType.CARBON_DIOXIDE, 8);
            step(level, o, 1, 6, 1, 10);
            check("carbon dioxide sinks to the floor", level.getBlockState(o).is(Fumes.gas(GasType.CARBON_DIOXIDE)));

            // heavy gas under light gas swaps places
            box(level, o, 1, 2, 1);
            GasSources.add(level, o, GasType.METHANE, 8);
            GasSources.add(level, o.above(), GasType.HYDROGEN_SULFIDE, 8);
            step(level, o, 1, 2, 1, 2);
            check("hydrogen sulfide sinks below methane", level.getBlockState(o).is(Fumes.gas(GasType.HYDROGEN_SULFIDE))
                    && level.getBlockState(o.above()).is(Fumes.gas(GasType.METHANE)));

            // gas spreads out in a closed room and nothing is lost
            box(level, o, 5, 1, 5);
            GasSources.add(level, o.offset(2, 0, 2), GasType.CARBON_DIOXIDE, 8);
            step(level, o, 5, 1, 5, 20);
            int cells = 0;
            for (BlockPos p : BlockPos.betweenClosed(o, o.offset(4, 0, 4))) if (level.getBlockState(p).getBlock() instanceof GasBlock) cells++;
            check("gas spreads (" + cells + " cells)", cells >= 4);
            check("gas is conserved underground (" + total(level, o, 5, 1, 5, GasType.CARBON_DIOXIDE) + ")",
                    total(level, o, 5, 1, 5, GasType.CARBON_DIOXIDE) == 8);

            // a torch sets off methane: the cloud is gone and the box is damaged
            box(level, o, 3, 3, 3);
            GasSources.pocket(level, o, GasType.METHANE, 27, 8);
            level.setBlock(o.offset(1, 0, 1), Blocks.TORCH.defaultBlockState(), 2);
            step(level, o, 3, 3, 3, 1);
            check("methane explodes at a torch", total(level, o, 3, 3, 3, GasType.METHANE) == 0);
            check("explosion broke the walls", level.getBlockState(o.offset(-1, 1, 1)).isAir() || level.getBlockState(o.offset(3, 1, 1)).isAir()
                    || level.getBlockState(o.offset(1, 3, 1)).isAir());

            // carbon dioxide puts out fire
            box(level, o, 1, 2, 1);
            level.setBlock(o.below(), Blocks.NETHERRACK.defaultBlockState(), 2);
            level.setBlock(o, Blocks.FIRE.defaultBlockState(), 2);
            GasSources.add(level, o.above(), GasType.CARBON_DIOXIDE, 8);
            level.getBlockState(o.above()).tick(level, o.above(), level.getRandom());
            check("carbon dioxide smothers fire", !level.getBlockState(o).is(Blocks.FIRE));

            // breathing: poisoned without a mask, protected with one (and the mask wears)
            box(level, o, 1, 3, 1);
            GasSources.add(level, o, GasType.HYDROGEN_SULFIDE, 8);
            GasSources.add(level, o.above(), GasType.HYDROGEN_SULFIDE, 8);
            FakePlayer fp = FakePlayer.get(level);
            fp.snapTo(o.getX() + 0.5, o.getY(), o.getZ() + 0.5, 0, 0);
            fp.removeAllEffects();
            GasEffects.breathe(level, fp);
            check("hydrogen sulfide poisons", fp.hasEffect(com.fumes.gas.GasMobEffect.H2S_POISONING));
            fp.removeAllEffects();
            ItemStack mask = new ItemStack(Fumes.GAS_MASK);
            fp.setItemSlot(EquipmentSlot.HEAD, mask);
            GasEffects.breathe(level, fp);
            check("gas mask protects", !fp.hasEffect(com.fumes.gas.GasMobEffect.H2S_POISONING));
            check("gas mask wears out", fp.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 1);
            fp.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

            // own effects: asphyxia hurts over time
            var pig = net.minecraft.world.entity.EntityTypes.PIG.spawn(level, o, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (pig != null) {
                float hp = pig.getHealth();
                com.fumes.gas.GasMobEffect.ASPHYXIA.value().applyEffectTick(level, pig, 1);
                check("asphyxia deals damage (" + hp + " -> " + pig.getHealth() + ")", pig.getHealth() < hp);
                pig.discard();
            } else check("pig spawned for the damage test", false);

            // every advancement loaded
            for (String adv : new String[]{"root", "gas_mask", "filter_works", "asphyxia", "carbon_monoxide", "hydrogen_sulfide", "scalded", "firedamp"})
                check("advancement " + adv + " loaded", server.getAdvancements().get(Fumes.id(adv)) != null);

            // steam from lava meeting water
            box(level, o, 1, 3, 1);
            level.levelEvent(null, 1501, o, 0);
            check("lava fizz makes steam", level.getBlockState(o.above()).is(Fumes.gas(GasType.STEAM)));
        } catch (Throwable t) {
            failures.add("threw " + t);
            Fumes.LOGGER.error("Self test crashed", t);
        }
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-2, -2, -2), o.offset(6, 8, 6))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);

        if (failures.isEmpty()) Fumes.LOGGER.info("FUMES SELF TEST PASSED ({} checks)", checks);
        else {
            Fumes.LOGGER.error("FUMES SELF TEST FAILED: {} of {} checks", failures.size(), checks);
            failures.forEach(f -> Fumes.LOGGER.error("  failure: {}", f));
        }
    }
}
