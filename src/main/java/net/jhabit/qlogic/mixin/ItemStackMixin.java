package net.jhabit.qlogic.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract Item getItem();

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void qlogic$modifyStackSize(CallbackInfoReturnable<Integer> cir) {
        Item item = this.getItem();
        String itemId = item.toString();

        // [64개 그룹]
        if (isSign(item) || isBanner(item) || item == Items.FLOWER_POT ||
                item == Items.DECORATED_POT || item == Items.HONEY_BOTTLE || item == Items.ENCHANTED_BOOK) {
            cir.setReturnValue(64);
        }
        // [16개 그룹]
        else if (item instanceof BedItem ||
                item.components().has(DataComponents.JUKEBOX_PLAYABLE) || // 음반
                item.components().has(DataComponents.INSTRUMENT) ||       // 염소 뿔
                item == Items.SADDLE ||
                isHorseArmor(item) ||
                isNautilusArmor(item) ||
                isStew(item) ||
                item == Items.CAKE ||
                item instanceof BoatItem ||
                item instanceof MinecartItem) {
            cir.setReturnValue(16);
        }
    }

    private boolean isSign(Item item) { return item instanceof SignItem || item instanceof HangingSignItem; }
    private boolean isBanner(Item item) { return item instanceof BannerItem; }
    private boolean isStew(Item item) {
        return item == Items.MUSHROOM_STEW || item == Items.RABBIT_STEW || item == Items.SUSPICIOUS_STEW || item == Items.BEETROOT_SOUP;
    }
    private boolean isHorseArmor(Item item) {
        return item == Items.COPPER_HORSE_ARMOR || item == Items.LEATHER_HORSE_ARMOR || item == Items.IRON_HORSE_ARMOR ||
                item == Items.GOLDEN_HORSE_ARMOR || item == Items.DIAMOND_HORSE_ARMOR || item == Items.NETHERITE_HORSE_ARMOR;
    }

    private boolean isNautilusArmor(Item item) {
        return item == Items.COPPER_NAUTILUS_ARMOR || item == Items.GOLDEN_NAUTILUS_ARMOR ||
                item == Items.IRON_NAUTILUS_ARMOR || item == Items.DIAMOND_NAUTILUS_ARMOR || item == Items.NETHERITE_NAUTILUS_ARMOR;
    }
}