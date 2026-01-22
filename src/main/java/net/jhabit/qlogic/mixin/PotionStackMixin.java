package net.jhabit.qlogic.mixin;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents; // 정정: DataComponents
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Items.class)
public class PotionStackMixin {
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void modifyPotionStackSize(CallbackInfo ci) {
        modifyStackSize(Items.POTION, 16);
        modifyStackSize(Items.SPLASH_POTION, 16);
        modifyStackSize(Items.LINGERING_POTION, 16);
    }

    private static void modifyStackSize(Item item, int size) {
        ItemAccessor accessor = (ItemAccessor) item;
        DataComponentMap oldComponents = accessor.getComponents();

        // DataComponents.MAX_STACK_SIZE가 올바른 필드명입니다.
        DataComponentMap newComponents = DataComponentMap.builder()
                .addAll(oldComponents)
                .set(DataComponents.MAX_STACK_SIZE, size)
                .build();

        accessor.setComponents(newComponents);
    }
}