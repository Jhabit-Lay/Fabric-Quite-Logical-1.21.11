package net.jhabit.quitelogical.items;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Repairable;

import java.util.function.Function;

public class ModItems {

    // 1. 구리 염소 뿔 키 생성
    public static final ResourceKey<Item> COPPER_GOAT_HORN_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("qlogic", "copper_goat_horn")
    );

    // 2. 구리 염소 뿔 등록 (setId 메서드 사용)
    public static final Item COPPER_GOAT_HORN = registerItem(
            COPPER_GOAT_HORN_KEY,
            new CopperGoatHornItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(30)
                    .setId(COPPER_GOAT_HORN_KEY)
                    // .component (단수)를 사용하고 HolderSet.direct로 구리 주괴를 감쌉니다.
                    .component(DataComponents.REPAIRABLE, new Repairable(
                            net.minecraft.core.HolderSet.direct(Items.COPPER_INGOT.builtInRegistryHolder())
                    ))
            )
    );


    private static Item registerItem(ResourceKey<Item> key, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    // 3. 범용 등록 메서드 (setId 메서드 사용)
    public static <GenericItem extends Item> GenericItem register(String name, Function<Item.Properties, GenericItem> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath("qlogic", name)
        );

        // settings.id(itemKey)가 아니라 setId(itemKey)를 호출해야 합니다.
        GenericItem item = itemFactory.apply(settings.setId(itemKey));

        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }



    public static void initialize() {
        // 메인 클래스에서 이 메서드를 호출하여 아이템 등록을 확정합니다.
    }
}