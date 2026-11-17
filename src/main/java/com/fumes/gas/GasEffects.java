package com.fumes.gas;

import com.fumes.Fumes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/**
 * Once a second every living thing near a player breathes the gas at its head. A gas mask on the head blocks
 * everything except steam and loses one durability per protected second.
 */
public final class GasEffects {

    private static final int RANGE = 48;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(GasEffects::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.players().isEmpty()) continue;
            Set<LivingEntity> done = new HashSet<>();
            for (Player p : level.players()) {
                AABB box = p.getBoundingBox().inflate(RANGE);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive))
                    if (done.add(e)) breathe(level, e);
            }
        }
    }

    public static void breathe(ServerLevel level, LivingEntity e) {
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        BlockState s = level.getBlockState(BlockPos.containing(e.getEyePosition()));
        if (!(s.getBlock() instanceof GasBlock g)) return;
        int a = s.getValue(GasBlock.AMOUNT);
        if (g.type.maskProtects() && wearsMask(e)) {
            e.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(1, e, EquipmentSlot.HEAD);
            return;
        }
        switch (g.type) {
            case METHANE -> {
                //not poisonous, but pushes the air out
                if (a >= 4) effect(e, MobEffects.NAUSEA, 8, 0);
                if (a >= 6) hurt(level, e, Fumes.GAS_DAMAGE, a - 5);
            }
            case HYDROGEN_SULFIDE -> {
                effect(e, MobEffects.POISON, 2 + a, a >= 6 ? 1 : 0);
                if (a >= 3) effect(e, MobEffects.NAUSEA, 10, 0);
                if (a >= 6) effect(e, MobEffects.BLINDNESS, 3, 0);
            }
            case CARBON_DIOXIDE -> {
                if (a >= 2) effect(e, MobEffects.SLOWNESS, 3, a >= 6 ? 1 : 0);
                if (a >= 3) hurt(level, e, Fumes.GAS_DAMAGE, a >= 6 ? 2 : 1);
            }
            case CARBON_MONOXIDE -> {
                //sneaky: first weakness and fatigue, then real damage
                effect(e, MobEffects.WEAKNESS, 10, 0);
                if (a >= 2) effect(e, MobEffects.MINING_FATIGUE, 10, 0);
                if (a >= 3) effect(e, MobEffects.NAUSEA, 8, 0);
                if (a >= 4) hurt(level, e, Fumes.GAS_DAMAGE, a >= 7 ? 2 : 1);
            }
            case STEAM -> {
                if (a >= 3 && !e.fireImmune()) hurt(level, e, Fumes.STEAM_DAMAGE, a >= 6 ? 2 : 1);
            }
        }
    }

    public static boolean wearsMask(LivingEntity e) {
        ItemStack head = e.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(Fumes.GAS_MASK);
    }

    private static void effect(LivingEntity e, Holder<MobEffect> effect, int seconds, int amp) {
        e.addEffect(new MobEffectInstance(effect, seconds * 20, amp, false, true));
    }

    private static void hurt(ServerLevel level, LivingEntity e, net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type, float dmg) {
        e.hurtServer(level, level.damageSources().source(type), dmg);
    }

    private GasEffects() {
    }
}
