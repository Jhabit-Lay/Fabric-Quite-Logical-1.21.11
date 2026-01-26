package net.jhabit.qlogic.util;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// 데이터를 꺼내오기 위한 인터페이스
public interface RespawnPosAngleAccessor {
    net.minecraft.world.phys.Vec3 getPosition();
    float getYaw();
    float getPitch();
}

// 프라이빗 레코드에 인터페이스를 주입하는 Mixin
@Mixin(targets = "net.minecraft.server.level.ServerPlayer$RespawnPosAngle")
abstract class RespawnPosAngleMixin implements RespawnPosAngleAccessor {
    @Shadow
    public abstract net.minecraft.world.phys.Vec3 position();
    @Shadow public abstract float yaw();
    @Shadow public abstract float pitch();

    @Override public net.minecraft.world.phys.Vec3 getPosition() { return position(); }
    @Override public float getYaw() { return yaw(); }
    @Override public float getPitch() { return pitch(); }
}
