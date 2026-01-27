package net.jhabit.qlogic.util;

import net.minecraft.network.chat.Component;

/**
 * [KR] 나침반 HUD 데이터 구조 (색상 필드 포함)
 * [EN] Data structure for the compass HUD (including color field)
 */
public record CompassData(Component name, int count, int color, long expiryTime) {
    public static CompassData permanent(Component name, int count, int color) {
        return new CompassData(name, count, color, -1L);
    }
}