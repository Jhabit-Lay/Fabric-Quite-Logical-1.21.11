package net.jhabit.qlogic.mixin;

import net.minecraft.core.Rotations;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {

    @Shadow public abstract void setShowArms(boolean showArms);
    @Shadow public abstract void setHeadPose(Rotations angle);
    @Shadow public abstract void setBodyPose(Rotations angle);
    @Shadow public abstract void setLeftArmPose(Rotations angle);
    @Shadow public abstract void setRightArmPose(Rotations angle);
    @Shadow public abstract void setLeftLegPose(Rotations angle);
    @Shadow public abstract void setRightLegPose(Rotations angle);

    @Unique private int poseIndex = 0;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.setShowArms(true);
    }

    // 데이터 저장: ValueOutput의 putInt 사용
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onSave(ValueOutput valueOutput, CallbackInfo ci) {
        valueOutput.putInt("PoseIndex", this.poseIndex);
    }

    // 데이터 로드: ValueInput의 getIntOr 사용
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onLoad(ValueInput valueInput, CallbackInfo ci) {
        // 기존에 작동했던 readInt()가 있다면 사용 가능하나, getIntOr가 더 안전한 표준입니다.
        this.poseIndex = valueInput.getIntOr("PoseIndex", 0);
        setPose(this.poseIndex);
    }

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void onInteract(Player player, Vec3 hitPos, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND) return;

        // 지적해주신 대로 level().isClientSide() 메서드 호출
        if (player.isShiftKeyDown()) {
            if (!player.level().isClientSide()) {
                swapEquipment(player);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
        else if (player.getMainHandItem().isEmpty()) {
            if (!player.level().isClientSide()) {
                this.poseIndex = (this.poseIndex + 1) % 13;
                setPose(this.poseIndex);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Unique
    private void swapEquipment(Player player) {
        LivingEntity stand = (LivingEntity) (Object) this;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack standItem = stand.getItemBySlot(slot).copy();
            ItemStack playerItem = player.getItemBySlot(slot).copy();
            player.setItemSlot(slot, standItem);
            stand.setItemSlot(slot, playerItem);
        }
    }

    @Unique
    private void setPose(int index) {
        // 베드락 13종 포즈 로직 (동일)
        switch (index) {
            case 0 -> setAll(0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0, 0,0,0);                             // Default
            case 1 -> setAll(0,0,0, 0,0,0, 322,0,26, 291,326,0, 0,0,0, 0,0,0);                     // No
            case 2 -> setAll(0,0,0, 0,0,0, 0,0,0, 291,22,0, 0,0,0, 0,0,0);                         // Solemn
            case 3 -> setAll(0,0,350, 0,0,350, 0,103,360, 231,113,0, 0,0,350, 0,0,0);              // Athena
            case 4 -> setAll(0,0,360, 0,0,360, 291,22,360, 231,0,0, 0,0,360, 0,22,10);             // Brandish
            case 5 -> setAll(0,0,0, 0,0,0, 231,53,0, 231,326,0, 0,0,0, 0,0,0);                     // Honor
            case 6 -> setAll(0,0,0, 0,0,0, 231,326,0, 231,53,0, 0,0,0, 0,0,0);                     // Entertain
            case 7 -> setAll(0,0,0, 0,0,0, 0,0,0, 291,326,0, 0,0,0, 0,0,0);                         // Salute
            case 8 -> setAll(18,22,360, 0,0,0, 170,0,57, 261,356,0, 0,0,0, 18,22,0);               // Riposte
            case 9 -> setAll(0,0,350, 0,0,0, 261,0,0, 261,0,0, 0,0,0, 322,0,0);                     // Zombie
            case 10 -> setAll(0,22,0, 0,22,0, 13,0,241, 13,0,116, 257,29,0, 0,0,346);               // Cancan A
            case 11 -> setAll(0,308,0, 0,347,0, 13,0,234, 337,0,119, 0,22,13, 236,22,88);           // Cancan B
            case 12 -> setAll(0,335,5, 0,22,0, 0,144,340, 261,53,0, 0,0,346, 18,0,8);               // Hero
        }
    }

    @Unique
    private void setAll(float hX, float hY, float hZ, float bX, float bY, float bZ,
                        float lAX, float lAY, float lAZ, float rAX, float rAY, float rAZ,
                        float lLX, float lLY, float lLZ, float rLX, float rLY, float rLZ) {
        setHeadPose(new Rotations(hX, hY, hZ));
        setBodyPose(new Rotations(bX, bY, bZ));
        setLeftArmPose(new Rotations(lAX, lAY, lAZ));
        setRightArmPose(new Rotations(rAX, rAY, rAZ));
        setLeftLegPose(new Rotations(lLX, lLY, lLZ));
        setRightLegPose(new Rotations(rLX, rLY, rLZ));
    }
}