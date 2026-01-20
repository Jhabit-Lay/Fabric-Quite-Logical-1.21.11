package net.jhabit.quitelogical.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.InstrumentItem; // 수정된 경로
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(InstrumentItem.class)
public class InstrumentItemMixin {

    @Inject(method = "use", at = @At("HEAD"))
    private void quitelogical$wolfCommand(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);

        // 아이템이 염소 뿔(GOAT_HORN)이거나 우리가 만든 구리 염소 뿔일 때만 작동
        if (!level.isClientSide() && (stack.is(Items.GOAT_HORN) || stack.getItem().toString().contains("copper_goat_horn"))) {
            Entity target = getLookedAtEntity(player, 32);

            if (target instanceof LivingEntity livingTarget) {
                level.getEntitiesOfClass(Wolf.class, player.getBoundingBox().inflate(32),
                                wolf -> wolf.isTame() && wolf.getOwner() == player)
                        .forEach(wolf -> wolf.setTarget(livingTarget));
            }
        }
    }

    private Entity getLookedAtEntity(Player player, double dist) {
        Vec3 start = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 end = start.add(viewVec.scale(dist));
        AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(dist)).inflate(1.0D);
        Predicate<Entity> filter = e -> !e.isSpectator() && e.isPickable();

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,          // 1. Level 대신 Entity(player)를 넣습니다.
                start,
                end,
                searchBox,
                filter,
                dist * dist      // 2. 마지막에 거리의 제곱(distanceSq)을 추가합니다.
        );
        return hit != null ? hit.getEntity() : null;
    }
}