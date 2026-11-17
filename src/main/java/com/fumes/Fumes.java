package com.fumes;

import com.fumes.gas.GasBlock;
import com.fumes.gas.GasEffects;
import com.fumes.gas.GasSources;
import com.fumes.gas.GasType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class Fumes implements ModInitializer {

    public static final String MOD_ID = "fumes";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fumes");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final TagKey<net.minecraft.world.level.block.Block> IGNITES_GAS = TagKey.create(Registries.BLOCK, id("ignites_gas"));
    public static final TagKey<net.minecraft.world.level.block.Block> IGNITES_GAS_WHEN_LIT = TagKey.create(Registries.BLOCK, id("ignites_gas_when_lit"));
    public static final TagKey<net.minecraft.world.level.block.Block> RELEASES_METHANE = TagKey.create(Registries.BLOCK, id("releases_methane"));
    public static final TagKey<net.minecraft.world.level.block.Block> GAS_POCKET_ROCK = TagKey.create(Registries.BLOCK, id("gas_pocket_rock"));
    public static final TagKey<Item> REPAIRS_GAS_MASK = TagKey.create(Registries.ITEM, id("repairs_gas_mask"));
    public static final ResourceKey<DamageType> GAS_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("gas"));
    public static final ResourceKey<DamageType> STEAM_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, id("steam"));

    public static final Map<GasType, GasBlock> GASES = new EnumMap<>(GasType.class);

    public static final ResourceKey<EquipmentAsset> GAS_MASK_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, id("gas_mask"));
    /** 1 armor point, repaired with charcoal (a fresh filter). */
    public static final ArmorMaterial GAS_MASK_MATERIAL = new ArmorMaterial(
            15, Map.of(ArmorType.HELMET, 1, ArmorType.CHESTPLATE, 0, ArmorType.LEGGINGS, 0, ArmorType.BOOTS, 0, ArmorType.BODY, 0),
            9, SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f, REPAIRS_GAS_MASK, GAS_MASK_ASSET);
    /** durability = seconds of protection inside gas */
    public static final int GAS_MASK_DURABILITY = 1200;
    public static Item GAS_MASK;

    @Override
    public void onInitialize() {
        for (GasType t : GasType.values()) {
            Identifier bid = id(t.id);
            GasBlock b = new GasBlock(t, BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, bid))
                    .mapColor(MapColor.NONE).replaceable().noCollision().noOcclusion().noLootTable()
                    .instabreak().pushReaction(PushReaction.POPPED));
            Registry.register(BuiltInRegistries.BLOCK, bid, b);
            GASES.put(t, b);
        }

        Identifier mid = id("gas_mask");
        GAS_MASK = Registry.register(BuiltInRegistries.ITEM, mid, new Item(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, mid))
                .humanoidArmor(GAS_MASK_MATERIAL, ArmorType.HELMET).durability(GAS_MASK_DURABILITY)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(e -> e.accept(GAS_MASK));

        com.fumes.gas.GasMobEffect.init();
        GasSources.init();
        GasEffects.init();
        GasCommand.init();
        SelfTest.register();
        LOGGER.info("Fumes loaded: {} gases", GASES.size());
    }

    public static GasBlock gas(GasType t) {
        return GASES.get(t);
    }

    public static boolean isGas(Block b) {
        return b instanceof GasBlock;
    }
}
