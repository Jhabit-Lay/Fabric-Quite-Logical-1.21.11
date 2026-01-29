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
    }
}