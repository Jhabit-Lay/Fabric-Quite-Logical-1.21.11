package net.jhabit.qlogic.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow private String itemName;
    @Shadow @Final private DataSlot cost;

    public AnvilMenuMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slotDefinition) {
        super(menuType, containerId, inventory, access, slotDefinition);
    }

    /**
     * Name Tag Cost 0 Exp
     */
    @Inject(method = "createResult", at = @At("TAIL"))
    private void onFreeRename(CallbackInfo ci) {
        ItemStack inputStack = this.inputSlots.getItem(0);

        if (!inputStack.isEmpty()) {
            if (this.itemName != null && !this.itemName.equals(inputStack.getHoverName().getString())) {
                this.cost.set(0);
            }
        }
    }

    /**
     * 0 Exp can take Anvil Item
     */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void onMayPickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        // 비용이 0원이고 결과 슬롯에 아이템이 있다면 무조건 가져갈 수 있게 함 (true 반환)
        if (this.cost.get() <= 0 && hasStack) {
            cir.setReturnValue(true);
        }
    }
}