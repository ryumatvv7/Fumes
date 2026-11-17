package com.fumes.gas;

/**
 * Every gas the mod knows. Buoyancy: +1 rises, -1 sinks, 0 drifts in any direction.
 * Decay chances are per physics tick: under open sky gases blow away, underground most of them stay.
 */
public enum GasType {
    //explosive, lighter than air: collects under cave ceilings and in mine shafts above coal
    METHANE("methane", 0xA8C4A2, 0.10f, +1, 0.18f, 0.0f, 6, true, 0.35f),
    //explosive and toxic, heavier than air: fills deep pits and the bottom of deep caves
    HYDROGEN_SULFIDE("hydrogen_sulfide", 0xD6CC5A, 0.30f, -1, 0.12f, 0.0f, 8, true, 0.22f),
    //chokes, heavier than air, puts out fires
    CARBON_DIOXIDE("carbon_dioxide", 0x8A8F96, 0.18f, -1, 0.12f, 0.0f, 8, false, 0f),
    //from fires, furnaces and campfires in closed rooms; nearly invisible, drifts everywhere
    CARBON_MONOXIDE("carbon_monoxide", 0x5A5A5A, 0.05f, 0, 0.12f, 0.004f, 10, false, 0f),
    //from lava meeting water; scalds, rises fast and condenses quickly
    STEAM("steam", 0xF2F4F7, 0.9f, +1, 0.45f, 0.10f, 3, false, 0f);

    public final String id;
    public final int color;
    /** chance per client animation tick and per full cell that a particle is shown */
    public final float visibility;
    public final int buoyancy;
    public final float openDecay;
    public final float closedDecay;
    /** ticks between physics steps */
    public final int delay;
    public final boolean flammable;
    /** explosion power added per concentration level */
    public final float powerPerLevel;

    GasType(String id, int color, float visibility, int buoyancy, float openDecay, float closedDecay, int delay,
            boolean flammable, float powerPerLevel) {
        this.id = id;
        this.color = color;
        this.visibility = visibility;
        this.buoyancy = buoyancy;
        this.openDecay = openDecay;
        this.closedDecay = closedDecay;
        this.delay = delay;
        this.flammable = flammable;
        this.powerPerLevel = powerPerLevel;
    }

    /** Protected by a gas mask? Steam burns skin, a mask doesn't help against it. */
    public boolean maskProtects() {
        return this != STEAM;
    }
}
