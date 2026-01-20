package net.jhabit.quitelogical;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;
import java.util.UUID;

public class ModComponents {
    // 말의 UUID를 저장할 데이터 컴포넌트 등록
    public static final DataComponentType<UUID> LINKED_HORSE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("qlogic", "linked_horse"),
            DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).build()
    );

    public static void register() {
        // 메인 클래스의 onInitialize에서 호출하여 초기화
    }
}