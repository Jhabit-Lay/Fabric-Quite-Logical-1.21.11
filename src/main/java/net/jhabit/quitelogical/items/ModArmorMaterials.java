package net.jhabit.quitelogical.items;

import net.jhabit.quitelogical.QuiteLogical;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.Map;

import static net.jhabit.quitelogical.QuiteLogical.STEEL_ARMOR_MATERIAL_KEY;

class ModArmorMaterials {

        public static final int BASE_DURABILITY = 23;

        // 1. 수리에 사용될 아이템 태그 정의 (#qlogic:repairs_steel_armor)
        public static final TagKey<Item> REPAIRS_STEEL_ARMOR = TagKey.create(
                BuiltInRegistries.ITEM.key(),
                Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, "repairs_steel_armor")
        );

        // 2. 강철 갑옷 재질 인스턴스 (철과 다이아 사이의 스펙)
        public static final ArmorMaterial STEEL = new ArmorMaterial(
                BASE_DURABILITY, // 내구도 계수 (철: 15, 다이아: 33)
                Map.of(
                        ArmorType.HELMET, 2,      // 철: 2, 다이아: 3
                        ArmorType.CHESTPLATE, 7,  // 철: 6, 다이아: 8
                        ArmorType.LEGGINGS, 5,    // 철: 5, 다이아: 6
                        ArmorType.BOOTS, 2        // 철: 2, 다이아: 3
                ),
                12, // 마법 부여 효율 (철: 9, 다이아: 10, 금: 25)
                SoundEvents.ARMOR_EQUIP_IRON, // 장착 소리
                1.0F, // 강인함(Toughness) - 철: 0, 다이아: 2
                0.0F, // 밀치기 저항 (Knockback Resistance)
                REPAIRS_STEEL_ARMOR, // 수리 태그
                STEEL_ARMOR_MATERIAL_KEY // 에셋 키 (텍스처 경로 결정)
        );
}