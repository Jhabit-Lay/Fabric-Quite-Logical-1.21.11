package net.jhabit.qlogic.items;

import net.jhabit.qlogic.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class CopperGoatHornItem extends InstrumentItem {

    public CopperGoatHornItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof AbstractHorse horse && !player.level().isClientSide()) {
            UUID currentLinkedUuid = stack.get(ModComponents.LINKED_HORSE);
            UUID clickedHorseUuid = horse.getUUID();

            if (currentLinkedUuid != null && currentLinkedUuid.equals(clickedHorseUuid)) {
                stack.remove(ModComponents.LINKED_HORSE);
                player.displayClientMessage(Component.translatable("message.quitelogical.horse_unlinked", horse.getName()), true);
                return InteractionResult.SUCCESS;
            }

            if (horse.isTamed()) {
                stack.set(ModComponents.LINKED_HORSE, clickedHorseUuid);
                player.displayClientMessage(Component.translatable("message.quitelogical.horse_linked", horse.getName()), true);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.translatable("message.quitelogical.only_tamed"), true);
                return InteractionResult.CONSUME;
            }
        }
        return super.interactLivingEntity(stack, player, entity, hand);
    }

    // [수정 포인트 1] 반환 타입을 InteractionResultHolder<ItemStack>으로 변경
    @Override
    public @NotNull InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // 구리 뿔 사운드 재생
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0),
                SoundSource.RECORDS, 3.0F, 1.4F);

        if (!level.isClientSide()) {
            UUID horseUuid = itemStack.get(ModComponents.LINKED_HORSE);
            if (horseUuid != null) {
                ServerLevel serverLevel = (ServerLevel) level;
                Entity targetEntity = serverLevel.getEntity(horseUuid);

                if (targetEntity instanceof AbstractHorse horse) {
                    float yaw = player.getYRot() * ((float) Math.PI / 180F);

                    // 1. 소환/목적지 좌표 계산
                    double spawnDist = 3.5;
                    double spawnX = player.getX() + (Math.sin(yaw) * spawnDist);
                    double spawnZ = player.getZ() - (Math.cos(yaw) * spawnDist);

                    double forwardDist = 3.3;
                    double sideDist = 2.8;
                    double targetX = player.getX() - (Math.sin(yaw) * forwardDist) + (Math.cos(yaw) * sideDist);
                    double targetZ = player.getZ() + (Math.cos(yaw) * forwardDist) + (Math.sin(yaw) * sideDist);

                    // 2. 소환 및 AI 이동 강제
                    horse.teleportTo(spawnX, player.getY(), spawnZ);
                    horse.getNavigation().stop();
                    // moveTo(x, y, z, speed)를 사용하여 명확한 지점으로 이동하게 함
                    horse.getNavigation().moveTo(targetX, player.getY(), targetZ, 1.4D);

                    horse.getLookControl().setLookAt(player, 30.0F, 30.0F);
                    horse.makeSound(SoundEvents.HORSE_ANGRY);

                    // 3. 내구도 및 쿨타임 처리
                    EquipmentSlot slot = (hand == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    itemStack.hurtAndBreak(1, player, slot);

                    // [수정 포인트 2] addCooldown이 해결됩니다.
                    player.getCooldowns().addCooldown(itemStack, 100);

                    player.displayClientMessage(Component.translatable("message.quitelogical.horse_summoned", horse.getName()), true);
                }
            }
        }

        player.startUsingItem(hand);
        // [수정 포인트 3] InteractionResultHolder 형태로 반환
        return InteractionResult.CONSUME;
    }

    // [수정 포인트 4] @Override를 붙여서 부모 클래스의 메서드임을 명시
    // CopperGoatHornItem.java 내부
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        // ingredient(재료)가 구리 주괴인지 확인합니다.
        return ingredient.is(Items.COPPER_INGOT);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModComponents.LINKED_HORSE);
    }

    // [수정 포인트 5] @Override 추가 및 툴팁 시그니처 보정
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        UUID horseUuid = stack.get(ModComponents.LINKED_HORSE);
        if (horseUuid != null) {
            tooltip.add(Component.translatable("tooltip.quitelogical.linked_horse", horseUuid.toString().substring(0, 8)).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.quitelogical.no_linked_horse").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("itemGroup.tools").withStyle(ChatFormatting.BLUE));
    }
}