package net.jhabit.qlogic.items;

import net.jhabit.qlogic.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CopperGoatHornItem extends InstrumentItem {

    public CopperGoatHornItem(Item.Properties properties) {
        super(properties);
    }

    // [중요] 1.21.11(1.21.2 아키텍처)에서는 InteractionResultHolder가 InteractionResult에 통합됨
    @Override
    public @NotNull InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        level.playSound(null, player,
                SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(),
                SoundSource.RECORDS, 1.5F, 1.4F);

        if (!level.isClientSide()) {
            UUID horseUuid = itemStack.get(ModComponents.LINKED_HORSE);
            if (horseUuid != null) {
                ServerLevel serverLevel = (ServerLevel) level;
                Entity targetEntity = serverLevel.getEntity(horseUuid);

                if (targetEntity instanceof AbstractHorse horse) {
                    float yawRadians = player.getYRot() * 0.017453292F;
                    double lookX = -Mth.sin(yawRadians);
                    double lookZ = Mth.cos(yawRadians);

                    // 1. 이상적인 소환 좌표 (뒤쪽 4블록)
                    Vec3 idealSpawn = new Vec3(player.getX() - (lookX * 4.0), player.getY(), player.getZ() - (lookZ * 4.0));

                    // 2. [핵심] 주변에서 안전한 좌표 찾기
                    Vec3 safeSpawn = findSafePosition(serverLevel, idealSpawn);

                    // 3. 만약 주변이 온통 벽이라면 플레이어 위치를 사용 (최후의 보루)
                    if (safeSpawn == null) safeSpawn = player.position();

                    // 4. 안전한 좌표로 이동
                    horse.teleportTo(safeSpawn.x, safeSpawn.y, safeSpawn.z);
                    horse.getNavigation().stop();

                    // 5. 내 앞 4블록 지점을 향해 이동 명령
                    double targetX = player.getX() + (lookX * 4.0);
                    double targetZ = player.getZ() + (lookZ * 4.0);
                    horse.getNavigation().moveTo(targetX, player.getY(), targetZ, 1.5D);

                    horse.getLookControl().setLookAt(player, 30.0F, 30.0F);
                    horse.makeSound(SoundEvents.HORSE_ANGRY);

                    // 시스템 처리
                    EquipmentSlot slot = (hand == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    itemStack.hurtAndBreak(1, player, slot);
                    player.getCooldowns().addCooldown(itemStack, 140);

                    player.displayClientMessage(Component.translatable("message.quitelogical.horse_summoned", horse.getName()), true);
                }
            }
        }

        player.startUsingItem(hand);
        return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack);
    }

    /**
     * [KR] 입력받은 위치 주변을 검색하여 말이 끼지 않고 서 있을 수 있는 안전한 좌표를 반환합니다.
     * [EN] Searches around the input position and returns a safe coordinate where the horse can stand without clipping.
     */
    private Vec3 findSafePosition(ServerLevel level, Vec3 pos) {
        BlockPos basePos = BlockPos.containing(pos);

        // 검색 범위를 약간 넓혀서 포탈이 없는 구역을 찾습니다.
        for (int y = -2; y <= 2; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = basePos.offset(x, y, z);

                    // 1. 기본적인 물리적 안전 확인 (발밑 단단함, 위 공간 비어있음, 액체 없음)
                    if (level.getBlockState(checkPos.below()).isFaceSturdy(level, checkPos.below(), net.minecraft.core.Direction.UP) &&
                            level.getBlockState(checkPos).isAir() &&
                            level.getBlockState(checkPos.above()).isAir() &&
                            level.getFluidState(checkPos).isEmpty()) {

                        // 2. [핵심] 주변에 포탈이 있는지 체크 (반경 2블록 내)
                        if (!isPortalNearby(level, checkPos)) {
                            return Vec3.atBottomCenterOf(checkPos);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * [KR] 해당 위치 주변에 포탈 블록이 있는지 확인합니다.
     * [EN] Checks if there are any portal blocks around the given position.
     */
    private boolean isPortalNearby(ServerLevel level, BlockPos pos) {
        // 소환 지점으로부터 2블록 내에 포탈이 하나라도 있으면 위험 지역으로 간주
        for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 2, 2))) {
            if (level.getBlockState(neighbor).is(net.minecraft.tags.BlockTags.PORTALS)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof AbstractHorse horse && !player.level().isClientSide()) {
            UUID currentLinkedUuid = stack.get(ModComponents.LINKED_HORSE);
            if (currentLinkedUuid != null && currentLinkedUuid.equals(horse.getUUID())) {
                stack.remove(ModComponents.LINKED_HORSE);
                stack.remove(ModComponents.LINKED_HORSE_NAME);
                player.displayClientMessage(Component.translatable("message.quitelogical.horse_unlinked", horse.getName()), true);
                return InteractionResult.SUCCESS;
            }

            if (horse.isTamed()) {
                stack.set(ModComponents.LINKED_HORSE, horse.getUUID());
                stack.set(ModComponents.LINKED_HORSE_NAME, horse.getName());
                player.displayClientMessage(Component.translatable("message.quitelogical.horse_linked", horse.getName()), true);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component customName = stack.get(ModComponents.LINKED_HORSE_NAME);
        if (customName != null) {
            return customName.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC);
        }
        return super.getName(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModComponents.LINKED_HORSE);
    }

    // [수정] 제공된 ItemStack.java 소스 코드 1248행에 근거하여 TooltipFlag 사용
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipBuilder, TooltipFlag flag) {
        UUID horseUuid = stack.get(ModComponents.LINKED_HORSE);

        if (horseUuid != null) {
            // [KR] tooltip.add() 대신 tooltipBuilder.accept() 사용
            // [EN] Use tooltipBuilder.accept() instead of tooltip.add()
            tooltipBuilder.accept(Component.translatable("tooltip.quitelogical.linked_horse",
                    horseUuid.toString().substring(0, 8)).withStyle(ChatFormatting.GOLD));
        } else {
            tooltipBuilder.accept(Component.translatable("tooltip.quitelogical.no_linked_horse").withStyle(ChatFormatting.GRAY));
        }
    }
}