package net.jhabit.qlogic.block;

import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.jhabit.qlogic.QuiteLogical;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

import static net.minecraft.world.level.block.Blocks.POWERED_RAIL;

/**
 * [KR] 모드의 모든 블록 등록 및 산화/밀랍칠 관계를 정의하는 클래스
 * [EN] Class for registering all blocks and defining oxidation/waxing relationships
 */
public class ModBlocks {

    // --- [1] 글로우 스틱 (Glow Sticks) ---
    public static final Block WALL_GLOW_STICK = registerTorch(
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

    public static final Block GLOW_STICK = registerTorch(
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

    // --- [2] 구리 파워레일 (Copper Powered Rails) ---
    public static final Block EXPOSED_POWERED_RAIL = register(
            "exposed_powered_rail",
            (settings) -> new OxidizablePoweredRailBlock(WeatheringCopper.WeatherState.EXPOSED, settings),
            BlockBehaviour.Properties.ofFullCopy(POWERED_RAIL).randomTicks().sound(SoundType.COPPER)
    );

    public static final Block WEATHERED_POWERED_RAIL = register(
            "weathered_powered_rail",
            (settings) -> new OxidizablePoweredRailBlock(WeatheringCopper.WeatherState.WEATHERED, settings),
            BlockBehaviour.Properties.ofFullCopy(POWERED_RAIL).randomTicks().sound(SoundType.COPPER)
    );

    public static final Block OXIDIZED_POWERED_RAIL = register(
            "oxidized_powered_rail",
            (settings) -> new OxidizablePoweredRailBlock(WeatheringCopper.WeatherState.OXIDIZED, settings),
            BlockBehaviour.Properties.ofFullCopy(POWERED_RAIL).randomTicks().sound(SoundType.COPPER)
    );

    // --- [3] 밀랍칠된 파워레일 (Waxed Powered Rails) ---
    public static final Block WAXED_POWERED_RAIL = register(
            "waxed_powered_rail",
            PoweredRailBlock::new,
            BlockBehaviour.Properties.ofFullCopy(POWERED_RAIL)
    );

    public static final Block WAXED_EXPOSED_POWERED_RAIL = register(
            "waxed_exposed_powered_rail",
            PoweredRailBlock::new,
            BlockBehaviour.Properties.ofFullCopy(POWERED_RAIL)
    );

    public static final Block WAXED_WEATHERED_POWERED_RAIL = register(
            "waxed_weathered_powered_rail",
            PoweredRailBlock::new,
            BlockBehaviour.Properties.ofFullCopy(POWERED_RAIL)
    );

    public static final Block WAXED_OXIDIZED_POWERED_RAIL = register(
            "waxed_oxidized_powered_rail",
            PoweredRailBlock::new,
            BlockBehaviour.Properties.ofFullCopy(POWERED_RAIL)
    );

    // --- [4] 등록 도우미 메서드 (Registration Helpers) ---
    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties settings) {
        Identifier id = Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Block block = blockFactory.apply(settings.setId(blockKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static Block registerTorch(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties settings, boolean shouldRegisterItem) {
        Identifier id = Identifier.fromNamespaceAndPath(QuiteLogical.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

        Block block = blockFactory.apply(settings.setId(blockKey));
        if (shouldRegisterItem) {
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
            StandingAndWallBlockItem blockItem = new StandingAndWallBlockItem(
                    block,
                    WALL_GLOW_STICK,
                    Direction.DOWN,
                    new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()
            );
            Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        }
        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    /**
     * [KR] 산화 및 밀랍칠 관계를 Fabric Registry에 등록합니다.
     */
    public static void initialize() {
        // 산화 단계 등록 (Oxidizable Block Pairs)
        OxidizableBlocksRegistry.registerOxidizableBlockPair(POWERED_RAIL, EXPOSED_POWERED_RAIL);
        OxidizableBlocksRegistry.registerOxidizableBlockPair(EXPOSED_POWERED_RAIL, WEATHERED_POWERED_RAIL);
        OxidizableBlocksRegistry.registerOxidizableBlockPair(WEATHERED_POWERED_RAIL, OXIDIZED_POWERED_RAIL);

        // 밀랍칠 관계 등록 (Waxable Block Pairs)
        OxidizableBlocksRegistry.registerWaxableBlockPair(POWERED_RAIL, WAXED_POWERED_RAIL);
        OxidizableBlocksRegistry.registerWaxableBlockPair(EXPOSED_POWERED_RAIL, WAXED_EXPOSED_POWERED_RAIL);
        OxidizableBlocksRegistry.registerWaxableBlockPair(WEATHERED_POWERED_RAIL, WAXED_WEATHERED_POWERED_RAIL);
        OxidizableBlocksRegistry.registerWaxableBlockPair(OXIDIZED_POWERED_RAIL, WAXED_OXIDIZED_POWERED_RAIL);
    }
}