package net.jhabit.qlogic.items;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

import static net.jhabit.qlogic.tags.ModTags.STEEL_TOOL_MATERIALS;

public class ModToolMaterials {
    public static final ToolMaterial STEEL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, // 채굴 등급 (철급)
            650,                              // 내구도
            7.0F,                             // 효율(속도)
            2.5F,                             // 공격력 보너스
            12,                               // 마법 부여 수치
            STEEL_TOOL_MATERIALS              // 방금 만든 수리 태그 사용!
    );
}