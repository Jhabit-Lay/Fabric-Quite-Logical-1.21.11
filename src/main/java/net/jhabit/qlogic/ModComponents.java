package net.jhabit.qlogic;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;
import java.util.UUID;

public class ModComponents {
    // Save Horse's UUID
    public static final DataComponentType<UUID> LINKED_HORSE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("qlogic", "linked_horse"),
            DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).build()
    );

    // Horse name storage component
    public static final DataComponentType<Component> LINKED_HORSE_NAME = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("qlogic", "linked_horse_name"),
            DataComponentType.<Component>builder().persistent(ComponentSerialization.CODEC).build()
    );

    public static void register() {}
}