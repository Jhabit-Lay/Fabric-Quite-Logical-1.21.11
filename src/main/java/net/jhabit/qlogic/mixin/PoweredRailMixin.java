package net.jhabit.qlogic.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * [KR] 바닐라 파워레일 블록에 산화 기능을 추가하는 Mixin
 * [EN] Mixin to add oxidation functionality to the vanilla Powered Rail block
 */
@Mixin(PoweredRailBlock.class)
public abstract class PoweredRailMixin extends Block implements WeatheringCopper {

    public PoweredRailMixin(Properties properties) {
        super(properties);
    }

    /**
     * [KR] 바닐라 파워레일이 무작위 틱을 받아 산화 로직을 실행할지 여부를 결정합니다.
     * [EN] Determines if the vanilla Powered Rail should receive random ticks for oxidation.
     */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // [KR] 다음 산화 단계(Exposed)가 레지스트리에 등록되어 있다면 true를 반환합니다.
        return WeatheringCopper.getNext(state.getBlock()).isPresent();
    }

    /**
     * [KR] 무작위 틱 발생 시 산화 진행 로직을 호출합니다.
     * [EN] Calls the oxidation logic during a random tick.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // [KR] WeatheringCopper 인터페이스의 기본 산화 로직 수행
        this.changeOverTime(state, world, pos, random);
    }

    /**
     * [KR] 바닐라 파워레일의 산화 단계를 '산화되지 않음'으로 정의합니다.
     * [EN] Defines the oxidation state of the vanilla Powered Rail as 'UNAFFECTED'.
     */
    @Override
    public WeatherState getAge() {
        return WeatherState.UNAFFECTED;
    }
}