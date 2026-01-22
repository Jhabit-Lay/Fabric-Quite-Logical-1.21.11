package net.jhabit.qlogic.items;

import net.jhabit.qlogic.QuiteLogical;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.Map;

import static net.jhabit.qlogic.QuiteLogical.STEEL_ARMOR_MATERIAL_KEY;

public class ModArmorMaterials {

        // QuiteLogical.STEEL_ARMOR_MATERIAL_KEY가 Identifier.fromNamespaceAndPath("qlogic", "steel") 를 담고있음

        public static final int BASE_DURABILITY = 23;

        // 수리 아이템 태그 (#qlogic:repairs_steel_armor)
        public static final TagKey<Item> REPAIRS_STEEL_ARMOR = TagKey.create(
                BuiltInRegistries.ITEM.key(),
                Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "repairs_steel_armor")
        );

        public static final ArmorMaterial STEEL = new ArmorMaterial(
                BASE_DURABILITY,
                Map.of(
                        ArmorType.HELMET, 2, // 철: 2, 다이아: 3
                        ArmorType.CHESTPLATE, 7, // 철: 6, 다이아: 8
                        ArmorType.LEGGINGS, 5, // 철: 5, 다이아: 6
                        ArmorType.BOOTS, 2 // 철: 2, 다이아: 3
                ),
                12,
                SoundEvents.ARMOR_EQUIP_IRON,
                1.0F, // 강인함
                0.0F, // 밀치기 저항
                REPAIRS_STEEL_ARMOR,
                // data/qlogic/equipment/steel.json
                STEEL_ARMOR_MATERIAL_KEY
        );
}