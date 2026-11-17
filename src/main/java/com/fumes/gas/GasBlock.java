package com.fumes.gas;

import com.fumes.Fumes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A cell of gas. AMOUNT is the concentration (1..8). It renders as a faint coloured haze plus particles, has no
 * collision and can be replaced by anything. Physics run on scheduled ticks: gases rise or sink, spread out until the
 * concentration is even, blow away under open sky. Flammable gases explode when they touch a flame.
 */
public class GasBlock extends Block {

    public static final int MAX = 8;
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, MAX);
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final int MAX_CLOUD = 512;

    public final GasType type;

    public GasBlock(GasType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(AMOUNT, MAX));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AMOUNT);
    }

    // ---------------------------------------------------------------- shape / rendering

    //rendered as a translucent volume (denser = more opaque); faces between cells of the same gas are hidden like glass
    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return neighbor.is(this) || super.skipRendering(state, neighbor, direction);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        float chance = type.visibility * state.getValue(AMOUNT) / MAX;
        int n = chance > 1 ? 2 : 1;
        for (int i = 0; i < n; i++) {
            if (random.nextFloat() >= chance) continue;
            double x = pos.getX() + random.nextDouble(), y = pos.getY() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
            if (type == GasType.STEAM) level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0.03, 0);
            else level.addParticle(new DustParticleOptions(type.color, 1.6f), x, y, z, 0, 0, 0);
        }
    }

    // ---------------------------------------------------------------- ticking

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !oldState.is(this)) level.scheduleTick(pos, this, delay(level.getRandom()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && !level.getBlockTicks().hasScheduledTick(pos, this))
            level.scheduleTick(pos, this, type.flammable ? 1 : delay(level.getRandom()));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) tick(state, level, pos, random);
    }

    private int delay(RandomSource r) {
        return type.delay + r.nextInt(type.delay + 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (type.flammable && touchesFlame(level, pos)) {
            ignite(level, pos);
            return;
        }
        if (type == GasType.CARBON_DIOXIDE) smother(level, pos);

        int a = state.getValue(AMOUNT);
        boolean open = level.getBrightness(LightLayer.SKY, pos) >= 15;
        if (random.nextFloat() < (open ? type.openDecay : type.closedDecay)) {
            if (--a <= 0) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
            level.setBlock(pos, state.setValue(AMOUNT, a), Block.UPDATE_ALL);
        }

        boolean moved = false;
        if (type.buoyancy != 0) moved = flowVertical(level, pos, a, type.buoyancy > 0 ? Direction.UP : Direction.DOWN);
        if (!moved) {
            List<Direction> dirs = new ArrayList<>(List.of(HORIZONTAL));
            if (type.buoyancy == 0) {
                dirs.add(Direction.UP);
                dirs.add(Direction.DOWN);
            }
            Collections.shuffle(dirs, new java.util.Random(random.nextLong()));
            for (Direction d : dirs) {
                if (spread(level, pos, a, d)) {
                    moved = true;
                    break;
                }
            }
        }
        //keep moving while something happens; settled gas sleeps until a neighbour changes or a random tick
        if (moved && level.getBlockState(pos).is(this)) level.scheduleTick(pos, this, delay(random));
    }

    private BlockState with(int amount) {
        return defaultBlockState().setValue(AMOUNT, amount);
    }

    private static boolean isEmpty(BlockState s) {
        return s.isAir();
    }

    /** Rising or sinking: the whole cell moves or merges, and lighter/heavier gases swap places. */
    private boolean flowVertical(ServerLevel level, BlockPos pos, int a, Direction dir) {
        BlockPos to = pos.relative(dir);
        if (!level.isInsideBuildHeight(to.getY())) return false;
        BlockState target = level.getBlockState(to);
        if (isEmpty(target)) {
            level.setBlock(to, with(a), Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        if (target.is(this)) {
            int b = target.getValue(AMOUNT);
            int t = Math.min(a, MAX - b);
            if (t <= 0) return false;
            level.setBlock(to, with(b + t), Block.UPDATE_ALL);
            level.setBlock(pos, a - t > 0 ? with(a - t) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        if (target.getBlock() instanceof GasBlock other) {
            boolean swap = dir == Direction.UP ? other.type.buoyancy < type.buoyancy : other.type.buoyancy > type.buoyancy;
            if (!swap) return false;
            BlockState mine = level.getBlockState(pos);
            level.setBlock(pos, target, Block.UPDATE_ALL);
            level.setBlock(to, mine, Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    /** Sideways (and up/down for neutral gases): concentration evens out with the neighbour. */
    private boolean spread(ServerLevel level, BlockPos pos, int a, Direction dir) {
        BlockPos to = pos.relative(dir);
        if (!level.isInsideBuildHeight(to.getY())) return false;
        BlockState target = level.getBlockState(to);
        if (isEmpty(target)) {
            if (a < 2) return false; //a lone thin wisp stays where it is
            int give = a / 2;
            level.setBlock(to, with(give), Block.UPDATE_ALL);
            level.setBlock(pos, with(a - give), Block.UPDATE_ALL);
            return true;
        }
        if (target.is(this)) {
            int b = target.getValue(AMOUNT);
            if (a - b < 2) return false;
            int give = (a - b) / 2;
            level.setBlock(to, with(b + give), Block.UPDATE_ALL);
            level.setBlock(pos, with(a - give), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- fire

    public static boolean isFlame(BlockState s) {
        if (s.is(BlockTags.FIRE) || s.getFluidState().is(FluidTags.LAVA) || s.is(Fumes.IGNITES_GAS)) return true;
        return s.is(Fumes.IGNITES_GAS_WHEN_LIT) && s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT);
    }

    private static boolean touchesFlame(ServerLevel level, BlockPos pos) {
        if (isFlame(level.getBlockState(pos))) return true;
        for (Direction d : Direction.values()) if (isFlame(level.getBlockState(pos.relative(d)))) return true;
        return false;
    }

    /** Carbon dioxide puts out fire and campfires next to it. */
    private static void smother(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos p = pos.relative(d);
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.FIRE)) level.removeBlock(p, false);
            else if (s.is(BlockTags.CAMPFIRES) && s.getValue(BlockStateProperties.LIT))
                level.setBlock(p, s.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        }
    }

    /**
     * The whole connected cloud of flammable gas burns at once: it's removed and explosions go off through it,
     * stronger where the gas was denser. Done in one go so the first blast can't just erase the rest of the cloud.
     */
    public static int ignite(ServerLevel level, BlockPos start) {
        List<BlockPos> cloud = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && cloud.size() < MAX_CLOUD) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof GasBlock g) || !g.type.flammable) continue;
            cloud.add(p);
            states.add(s);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) queue.add(n);
            }
        }
        if (cloud.isEmpty()) return 0;
        for (net.minecraft.server.level.ServerPlayer pl : level.players())
            if (pl.blockPosition().closerThan(start, 24)) GasEffects.award(pl, "firedamp");
        for (BlockPos p : cloud) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

        //one blast every few cells; small clouds still get at least one
        int blasts = Math.max(1, Math.min(32, cloud.size() / 3));
        RandomSource r = level.getRandom();
        for (int i = 0; i < blasts; i++) {
            int k = blasts == 1 ? 0 : r.nextInt(cloud.size());
            BlockPos p = cloud.get(k);
            BlockState s = states.get(k);
            GasType t = ((GasBlock) s.getBlock()).type;
            float power = 1.0f + s.getValue(AMOUNT) * t.powerPerLevel;
            level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, power, t == GasType.METHANE, Level.ExplosionInteraction.BLOCK);
        }
        return cloud.size();
    }
}
