package net.jhabit.qlogic.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static final TagKey<Item> STEEL_TOOL_MATERIALS = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("qlogic", "steel_tool_materials")
    );
}

