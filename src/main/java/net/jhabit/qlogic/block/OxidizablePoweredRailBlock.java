package net.jhabit.qlogic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [KR] 산화 단계가 있는 파워레일 블록 클래스 (무한 루프 방지 처리)
 * [EN] Powered Rail block class with oxidation states (Anti-recursion fix)
 */
public class OxidizablePoweredRailBlock extends PoweredRailBlock implements WeatheringCopper {
    private final WeatheringCopper.WeatherState weatherState;

    public OxidizablePoweredRailBlock(WeatheringCopper.WeatherState weatherState, Properties properties) {
        super(properties);
        this.weatherState = weatherState;
    }

    /**
     * [KR] 무작위 틱 발생 시 실행되는 메서드입니다.
     * [EN] Method executed during a random tick.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // [KR] onRandomTick 대신 직접 산화 로직을 호출하여 StackOverflowError를 방지합니다.
        // [EN] Directly call the change logic instead of onRandomTick to prevent StackOverflowError.
        this.changeOverTime(state, world, pos, random);
    }

    /**
     * [KR] 현재 블록이 무작위 틱을 받아 산화될 수 있는지 여부를 결정합니다.
     * [EN] Determines whether the block can receive random ticks for oxidation.
     */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // [KR] 다음 단계의 블록이 존재할 때만 무작위 틱을 활성화합니다.
        return WeatheringCopper.getNext(state.getBlock()).isPresent();
    }

    @Override
    public WeatherState getAge() {
        return this.weatherState;
    }
}