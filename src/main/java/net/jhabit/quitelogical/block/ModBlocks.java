package net.jhabit.quitelogical.block;

import net.jhabit.quitelogical.QuiteLogical;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public class ModBlocks {

    public static final Block WALL_GLOW_STICK = register(
            "wall_glow_stick",
            (settings) -> new WaterproofWallTorchBlock(ParticleTypes.GLOW, settings),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .noCollision()
                    .instabreak()
                    .lightLevel(state -> 15)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY),
            false
    );

    // 2. 바닥용 블록
    public static final Block GLOW_STICK = register(
            "glow_stick",
            (settings) -> new WaterproofTorchBlock(ParticleTypes.GLOW, settings),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .noCollision()
                    .instabreak()
                    .lightLevel(state -> 15)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY),
            true
    );

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties settings, boolean shouldRegisterItem) {
        // Identifier.of 대신 Identifier.fromNamespaceAndPath 사용
        Identifier id = Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

        Block block = blockFactory.apply(settings.setId(blockKey));

        if (shouldRegisterItem) {
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

            // 오류 메시지 기준 생성자 순서 교정: (StandingBlock, WallBlock, Direction, Properties)
            StandingAndWallBlockItem blockItem = new StandingAndWallBlockItem(
                    block,
                    WALL_GLOW_STICK,
                    Direction.DOWN, // 방향이 먼저
                    new Item.Properties().setId(itemKey).useBlockDescriptionPrefix() // 설정이 나중
            );

            Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        }

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    public static void initialize() {
        // 메인 클래스에서 로드를 보장하기 위한 빈 메서드
    }
}