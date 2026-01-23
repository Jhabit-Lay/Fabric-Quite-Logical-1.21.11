package net.jhabit.qlogic;

import net.minecraft.network.chat.Component;

/**
 * @param expiryTime 만료 시간 (밀리초). -1이면 영구 유지.
 */
public record CompassData(Component name, int count, long expiryTime) {
    public static CompassData permanent(Component name, int count) {
        return new CompassData(name, count, -1L);
    }
}