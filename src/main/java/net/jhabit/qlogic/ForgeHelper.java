package net.jhabit.qlogic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class ForgeHelper {
    // 1. 탄화철 레시피인지 확인
    public static boolean isSteelRecipe(ItemStack left, ItemStack right) {
        return left.is(Items.RAW_IRON) && right.is(Items.COAL);
    }

    // 2. 단조 로직 실행 (요청하신 시그니처 반영)
    public static void processForge(Player player, Level level, BlockPos pos, ItemStack resultStack, ItemStack left, ItemStack right) {
        // [버그 수정] 1개만 줄이는 것이 아니라, 집어간 아이템 뭉치의 수량만큼 재료를 소모합니다.
        int count = resultStack.getCount();
        left.shrink(count);
        right.shrink(count);

        // 제련 소리 재생
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ANVIL_USE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}