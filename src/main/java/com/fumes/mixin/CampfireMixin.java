package com.fumes.mixin;

import com.fumes.gas.GasSources;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A lit campfire (only lit ones tick on the server) indoors without a chimney gives off carbon monoxide. */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireMixin {

    @Inject(method = "cookTick", at = @At("HEAD"))
    private static void fumes$smoke(ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity entity,
                                    RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipeCache, CallbackInfo ci) {
        if (level.getRandom().nextInt(120) == 0) GasSources.smoke(level, pos, 2);
    }
}
