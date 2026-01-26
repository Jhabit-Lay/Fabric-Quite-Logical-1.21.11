package net.jhabit.qlogic.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {

    @Unique private int poseIndex = 0;
    @Unique private boolean lastPowered = false;

    /**
     * [해결] 소스 코드 248번 줄 확인: tickHeadTurn(float f)
     * ArmorStand 클래스 내에 직접 구현되어 있어 Mixin이 확실하게 타겟팅할 수 있습니다.
     */
    @Inject(method = "tickHeadTurn", at = @At("HEAD"))
    private void onTickHeadTurn(float f, CallbackInfo ci) {
        ArmorStand stand = (ArmorStand) (Object) this;
        Level level = stand.level();

        if (!level.isClientSide()) {
            BlockPos pos = stand.blockPosition();
            boolean isPowered = level.hasNeighborSignal(pos);

            // 레드스톤 신호 Rising Edge 감지
            if (isPowered && !this.lastPowered) {
                this.poseIndex = (this.poseIndex + 1) % 13;
                this.updatePose(stand, this.poseIndex);
            }
            this.lastPowered = isPowered;
        }
    }

    /**
     * [해결] 소스 코드 139번 줄 확인: interactAt(Player, Vec3, InteractionHand)
     */
    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void onInteractAt(Player player, Vec3 vec3, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (interactionHand != InteractionHand.MAIN_HAND) return;

        ArmorStand stand = (ArmorStand) (Object) this;
        if (player.isShiftKeyDown()) {
            if (!player.level().isClientSide()) {
                this.swapEquipment(player, stand);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else if (player.getMainHandItem().isEmpty()) {
            if (!player.level().isClientSide()) {
                this.poseIndex = (this.poseIndex + 1) % 13;
                this.updatePose(stand, this.poseIndex);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    /**
     * [해결] 소스 코드 104번 줄 확인: addAdditionalSaveData(ValueOutput)
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onSave(ValueOutput valueOutput, CallbackInfo ci) {
        valueOutput.putInt("PoseIndex", this.poseIndex);
        valueOutput.putBoolean("LastPowered", this.lastPowered);
    }

    /**
     * [해결] 소스 코드 116번 줄 확인: readAdditionalSaveData(ValueInput)
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onLoad(ValueInput valueInput, CallbackInfo ci) {
        this.poseIndex = valueInput.getIntOr("PoseIndex", 0);
        this.lastPowered = valueInput.getBooleanOr("LastPowered", false);
        this.updatePose((ArmorStand) (Object) this, this.poseIndex);
    }

    @Unique
    private void swapEquipment(Player player, ArmorStand stand) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack standItem = stand.getItemBySlot(slot).copy();
            ItemStack playerItem = player.getItemBySlot(slot).copy();
            player.setItemSlot(slot, standItem);
            stand.setItemSlot(slot, playerItem);
        }
    }

    @Unique
    private void updatePose(ArmorStand stand, int index) {
        stand.setShowArms(true);
        switch (index) {
            case 0 -> setAll(stand, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0);
            case 1 -> setAll(stand, 0,0,0, 0,0,0, 322,0,26, 291,326,0, 0,0,0, 0,0,0);
            case 2 -> setAll(stand, 0,0,0, 0,0,0, 0,0,0, 291,22,0, 0,0,0, 0,0,0);
            case 3 -> setAll(stand, 0,0,350, 0,0,350, 0,103,360, 231,113,0, 0,0,350, 0,0,0);
            case 4 -> setAll(stand, 0,0,360, 0,0,360, 291,22,360, 231,0,0, 0,0,360, 0,22,10);
            case 5 -> setAll(stand, 0,0,0, 0,0,0, 231,53,0, 231,326,0, 0,0,0, 0,0,0);
            case 6 -> setAll(stand, 0,0,0, 0,0,0, 231,326,0, 231,53,0, 0,0,0, 0,0,0);
            case 7 -> setAll(stand, 0,0,0, 0,0,0, 0,0,0, 291,326,0, 0,0,0, 0,0,0);
            case 8 -> setAll(stand, 18,22,360, 0,0,0, 170,0,57, 261,356,0, 0,0,0, 18,22,0);
            case 9 -> setAll(stand, 0,0,350, 0,0,0, 261,0,0, 261,0,0, 0,0,0, 322,0,0);
            case 10 -> setAll(stand, 0,22,0, 0,22,0, 13,0,241, 13,0,116, 257,29,0, 0,0,346);
            case 11 -> setAll(stand, 0,308,0, 0,347,0, 13,0,234, 337,0,119, 0,22,13, 236,22,88);
            case 12 -> setAll(stand, 0,335,5, 0,22,0, 0,144,340, 261,53,0, 0,0,346, 18,0,8);
        }
    }

    @Unique
    private void setAll(ArmorStand stand, float hX, float hY, float hZ, float bX, float bY, float bZ,
                        float lAX, float lAY, float lAZ, float rAX, float rAY, float rAZ,
                        float lLX, float lLY, float lLZ, float rLX, float rLY, float rLZ) {
        stand.setHeadPose(new Rotations(hX, hY, hZ));
        stand.setBodyPose(new Rotations(bX, bY, bZ));
        stand.setLeftArmPose(new Rotations(lAX, lAY, lAZ));
        stand.setRightArmPose(new Rotations(rAX, rAY, rAZ));
        stand.setLeftLegPose(new Rotations(lLX, lLY, lLZ));
        stand.setRightLegPose(new Rotations(rLX, rLY, rLZ));
    }
}