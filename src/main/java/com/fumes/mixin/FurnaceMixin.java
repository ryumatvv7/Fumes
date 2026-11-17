package com.fumes.mixin;

import com.fumes.gas.GasSources;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A burning furnace (also smoker and blast furnace) in a closed room slowly fills it with carbon monoxide. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceMixin {

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void fumes$smoke(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity entity, CallbackInfo ci) {
        if (state.getValue(AbstractFurnaceBlock.LIT) && level.getRandom().nextInt(160) == 0) GasSources.smoke(level, pos, 2);
    }
}
