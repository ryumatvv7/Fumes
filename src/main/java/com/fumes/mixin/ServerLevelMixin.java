package com.fumes.mixin;

import com.fumes.gas.GasSources;
import com.fumes.gas.GasType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lava meeting water (the "fizz" level event) makes a puff of steam above it. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    private static final int LAVA_FIZZ = 1501;

    @Inject(method = "levelEvent", at = @At("HEAD"))
    private void fumes$steam(@Nullable Entity source, int type, BlockPos pos, int data, CallbackInfo ci) {
        if (type != LAVA_FIZZ) return;
        ServerLevel level = (ServerLevel) (Object) this;
        if (!GasSources.add(level, pos.above(), GasType.STEAM, 5)) GasSources.add(level, pos, GasType.STEAM, 3);
    }
}
