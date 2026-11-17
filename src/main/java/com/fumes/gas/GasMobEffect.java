package com.fumes.gas;

import com.fumes.Fumes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The mod's own status effects. Each gas gives one; it lasts a few seconds and is refreshed every second while the
 * entity breathes the gas, so it wears off shortly after getting out. Damage ignores armor.
 */
public class GasMobEffect extends MobEffect {

    /** Methane (dense) and carbon dioxide: not enough air. */
    public static Holder<MobEffect> ASPHYXIA;
    /** Carbon monoxide: weak, slow, clumsy; hurts when it gets worse. */
    public static Holder<MobEffect> CO_POISONING;
    /** Hydrogen sulfide: a poison that can kill. */
    public static Holder<MobEffect> H2S_POISONING;
    /** Steam burns. */
    public static Holder<MobEffect> SCALDED;

    private final ResourceKey<DamageType> damageType;
    /** ticks between hits at amplifier 0 and at amplifier 1+ */
    private final int slowInterval, fastInterval;
    /** damage per hit = base + amplifier * perLevel */
    private final float base, perLevel;

    GasMobEffect(int color, ResourceKey<DamageType> damageType, int slowInterval, int fastInterval, float base, float perLevel) {
        super(MobEffectCategory.HARMFUL, color);
        this.damageType = damageType;
        this.slowInterval = slowInterval;
        this.fastInterval = fastInterval;
        this.base = base;
        this.perLevel = perLevel;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        int every = amplifier > 0 ? fastInterval : slowInterval;
        return every > 0 && tickCount % every == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        float dmg = base + amplifier * perLevel;
        if (dmg > 0) mob.hurtServer(level, level.damageSources().source(damageType), dmg);
        return true;
    }

    private static Holder<MobEffect> reg(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Fumes.id(name), effect);
    }

    public static void init() {
        ASPHYXIA = reg("asphyxia", new GasMobEffect(0x5A6B8C, Fumes.GAS_DAMAGE, 20, 20, 1, 1)
                .addAttributeModifier(Attributes.MOVEMENT_SPEED, Fumes.id("asphyxia"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        CO_POISONING = reg("carbon_monoxide_poisoning", new GasMobEffect(0xB03A48, Fumes.GAS_DAMAGE, 0, 40, 0, 1)
                .addAttributeModifier(Attributes.MOVEMENT_SPEED, Fumes.id("co_slow"), -0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .addAttributeModifier(Attributes.ATTACK_DAMAGE, Fumes.id("co_weak"), -1.5, AttributeModifier.Operation.ADD_VALUE)
                .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, Fumes.id("co_fatigue"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        H2S_POISONING = reg("hydrogen_sulfide_poisoning", new GasMobEffect(0xB5A642, Fumes.GAS_DAMAGE, 30, 20, 1, 0));
        SCALDED = reg("scalded", new GasMobEffect(0xE8E8E8, Fumes.STEAM_DAMAGE, 20, 20, 1, 1));
    }
}
