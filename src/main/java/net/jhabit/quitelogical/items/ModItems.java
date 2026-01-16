package net.jhabit.quitelogical.items;

import net.jhabit.quitelogical.QuiteLogical;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {

    //public static final Item GLOW_STICK = register("glow_stick", Item::new, new Item.Properties());

    public static <GenericItem extends Item> GenericItem register(String name, Function<Item.Properties, GenericItem> itemFactory, Item.Properties settings) {

        // Create the item key.
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, name));

        // Creating Item instance // 아이템 인스턴스 제작
        GenericItem item = itemFactory.apply(settings.setId(itemKey));

        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static void initialize() {
        // This method is called to ensure the class is loaded and its static fields are initialized.
    }
}
