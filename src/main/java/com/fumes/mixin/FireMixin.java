package com.fumes.mixin;

import com.fumes.gas.GasSources;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fire burning under a roof fills the room with carbon monoxide. */
@Mixin(FireBlock.class)
public abstract class FireMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void fumes$smoke(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (random.nextInt(6) == 0) GasSources.smoke(level, pos, 1);
    }
}
