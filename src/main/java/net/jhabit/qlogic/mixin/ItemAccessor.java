package net.jhabit.qlogic.mixin;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Item.class)
public interface ItemAccessor {
    @Accessor("components")
    DataComponentMap getComponents();

    @Accessor("components")
    @Mutable
    void setComponents(DataComponentMap components);
}