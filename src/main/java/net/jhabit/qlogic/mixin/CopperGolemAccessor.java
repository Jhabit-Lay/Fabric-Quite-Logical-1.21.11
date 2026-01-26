package net.jhabit.qlogic.mixin;

import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * [KR] CopperGolem의 프라이빗 필드 nextWeatheringTick에 접근하기 위한 액세서
 * [EN] Accessor to reach private field 'nextWeatheringTick' in CopperGolem
 */
@Mixin(CopperGolem.class)
public interface CopperGolemAccessor {
    @Accessor("nextWeatheringTick")
    long getNextWeatheringTick();

    @Accessor("nextWeatheringTick")
    void setNextWeatheringTick(long tick);
}