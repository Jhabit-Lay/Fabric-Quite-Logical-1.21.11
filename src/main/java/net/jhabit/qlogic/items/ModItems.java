package net.jhabit.qlogic.items;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Repairable;

import java.util.function.Function;


public class ModItems {

    // 1. 일반 재료 아이템
    //public static final Item CARBONIZED_IRON = register("carbonized_iron", Item::new, new Item.Properties().stacksTo(64));
    //public static final Item STEEL_INGOT = register("steel_ingot", Item::new, new Item.Properties().stacksTo(64));

    // 2. 칼 & 곡괭이 (Item::new와 속성 메서드 사용 - 에러 방지용)
//    public static final Item STEEL_SWORD = register("steel_sword",
//            Item::new,
//            new Item.Properties()
//                    .stacksTo(1)
//                    .sword(STEEL, 3.0f, -2.4f)
//                    .component(DataComponents.WEAPON, new Weapon(2))
//    );

    // 3. 우클릭 기능이 있는 도구들 (전용 클래스 유지 - 기능 보존용)
    // 사용자님이 확인하신 4인자 생성자 (재질, 공격력, 속도, 설정) 방식을 사용합니다.

//    public static final Item STEEL_AXE = register("steel_axe",
//            properties -> new AxeItem(STEEL, 6.0f, -3.1f, properties),
//            new Item.Properties());

    public static final Item COPPER_GOAT_HORN = register("copper_goat_horn",
            properties -> new CopperGoatHornItem(properties),
            new Item.Properties()
                    .stacksTo(1)
                    .durability(30)
                    .component(DataComponents.REPAIRABLE, new Repairable(
                            net.minecraft.core.HolderSet.direct(Items.COPPER_INGOT.builtInRegistryHolder())
                    ))
    );

    // --- 통합 등록 메서드 (소문자 자동 변환 및 Key 생성 포함) ---
    public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath("qlogic", name.toLowerCase())
        );

        T item = itemFactory.apply(settings.setId(itemKey));
        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    public static void initialize() {
        // 메인 클래스에서 호출하여 등록 실행
    }
}