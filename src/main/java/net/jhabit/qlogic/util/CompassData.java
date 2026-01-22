package net.jhabit.qlogic.util;

import net.minecraft.network.chat.Component;

/**
 * 나침반의 이름과 개수를 저장하는 데이터 구조입니다.
 */
public record CompassData(Component name, int count) {
    // count를 증가시킨 새로운 객체를 반환하는 헬퍼 메서드
    public CompassData withIncrementedCount() {
        return new CompassData(this.name, this.count + 1);
    }
}