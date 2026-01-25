package net.jhabit.qlogic.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HorseGuiMixin {

    @Shadow protected abstract @Nullable LivingEntity getPlayerVehicleWithHealth();
    @Shadow protected abstract int getVehicleMaxHearts(@Nullable LivingEntity livingEntity);
    @Shadow protected abstract void renderFood(GuiGraphics guiGraphics, Player player, int i, int j);

    /**
     * Fix: Target renderHearts instead of Profiler for better stability
     * This forces the food bar to render even if riding a horse
     */
    @Inject(
            method = "renderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V", shift = At.Shift.AFTER)
    )
    private void qlogic$forceRenderFoodBar(GuiGraphics guiGraphics, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        // If the vehicle has health, vanilla will skip renderFood. We call it here.
        int vehicleHearts = this.getVehicleMaxHearts(this.getPlayerVehicleWithHealth());
        if (vehicleHearts > 0) {
            int y = guiGraphics.guiHeight() - 39;
            int x = guiGraphics.guiWidth() / 2 + 91;
            this.renderFood(guiGraphics, player, y, x);
        }
    }

    /**
     * Move vehicle hearts up to the oxygen bar position
     */
    @ModifyVariable(method = "renderVehicleHealth", at = @At("STORE"), ordinal = 2)
    private int qlogic$moveHorseHeartsUp(int k) {
        // Shift up by 10 pixels to leave space for the food bar
        return k - 10;
    }

    /**
     * Adjust oxygen bubbles position if horse health is shown
     */
    @ModifyVariable(method = "renderPlayerHealth", at = @At("STORE"), ordinal = 10)
    private int qlogic$shiftOxygenBarUp(int r) {
        // Offset oxygen bar by 10 pixels if vehicle hearts are present
        if (this.getVehicleMaxHearts(this.getPlayerVehicleWithHealth()) > 0) {
            return r - 10;
        }
        return r;
    }
}