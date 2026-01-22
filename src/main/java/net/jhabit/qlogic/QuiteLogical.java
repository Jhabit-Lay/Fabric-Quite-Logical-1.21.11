package net.jhabit.qlogic;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.jhabit.qlogic.block.ModBlocks;
import net.jhabit.qlogic.entity.ModEntities;
import net.jhabit.qlogic.items.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class QuiteLogical implements ModInitializer {
	public static final String MOD_ID = "qlogic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 1. 핵심 모듈 초기화
		ModComponents.register();
		ModBlocks.initialize();
		ModItems.initialize();

		// 2. 엔티티 등록 및 속성 초기화 (ModEntities 내부에서 처리)
		ModEntities.initialize();

		// 3. 바이옴별 스폰 설정
		setupSpawns();

		// 4. 바닐라 엔티티 속성 수정
		registerVanillaAttributeModifiers();

		// 5. 아이템 그룹(크리에이티브 탭) 설정
		setupItemGroups();

		// 6. 양조 레시피 등록
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder ->
				builder.registerPotionRecipe(Potions.AWKWARD, Ingredient.of(Items.POISONOUS_POTATO), Potions.POISON)
		);

		// 7. 게임 내 상호작용 이벤트 등록 (레진 밀랍칠)
		registerEvents();


		LOGGER.info("Quite Logical 모드가 성공적으로 로드되었습니다!");
	}

	private void setupItemGroups() {
		// 1. 기능 블록 탭 (Functional Blocks): 글로우 스틱 배치
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			// 레드스톤 횃불(REDSTONE_TORCH) 바로 뒤에 글로우 스틱 배치
			entries.addAfter(Items.REDSTONE_TORCH, ModBlocks.GLOW_STICK);
		});

		// 2. 도구 및 유틸리티 탭 (Tools & Utilities): 금 괭이 뒤에 강철 도구들 나열
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			// 염소 뿔(GOAT_HORN) 뒤에 구리 염소 뿔 배치 (자동으로 첫 번째 음반인 13번 음반 앞에 위치함)
			entries.addAfter(Items.GOAT_HORN, ModItems.COPPER_GOAT_HORN);

			// 금 괭이 뒤에 삽 -> 곡괭이 -> 도끼 -> 괭이 순서로 나열
			entries.addAfter(Items.GOLDEN_HOE,
					ModItems.STEEL_SHOVEL,
					ModItems.STEEL_PICKAXE,
					ModItems.STEEL_AXE,
					ModItems.STEEL_HOE
			);
		});

		// 3. 전투 탭 (Combat): 금 검 뒤에 강철 검 배치
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
			// 금 검 뒤에 강철 검 배치
			entries.addAfter(Items.GOLDEN_SWORD, ModItems.STEEL_SWORD);

			entries.addAfter(Items.GOLDEN_BOOTS,
					ModItems.STEEL_HELMET,
					ModItems.STEEL_CHESTPLATE,
					ModItems.STEEL_LEGGINGS,
					ModItems.STEEL_BOOTS
			);
		});

		// 4. 재료 탭 (Ingredients): 금 원석/주괴 뒤에 배치
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			// 금 원석 뒤에 탄화철 배치
			entries.addAfter(Items.RAW_GOLD, ModItems.CARBONIZED_IRON);

			// 금 주괴 뒤에 강철 주괴 배치
			entries.addAfter(Items.GOLD_INGOT, ModItems.STEEL_INGOT);
		});
	}


	// 스폰 셋업
	private void setupSpawns() {
		BiomeModifications.addSpawn(
				BiomeSelectors.includeByKey(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE),
				MobCategory.MONSTER, ModEntities.JUNGLE_ZOMBIE, 160, 2, 4
		);
		BiomeModifications.addSpawn(
				BiomeSelectors.includeByKey(Biomes.TAIGA, Biomes.FROZEN_PEAKS, Biomes.SNOWY_TAIGA, Biomes.SNOWY_PLAINS),
				MobCategory.MONSTER, ModEntities.FROSTBITE, 160, 2, 4
		);
	}

	private void registerVanillaAttributeModifiers() {
		FabricDefaultAttributeRegistry.register(EntityType.COW, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 1.0));
		FabricDefaultAttributeRegistry.register(EntityType.MOOSHROOM, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 1.0));
		FabricDefaultAttributeRegistry.register(EntityType.BEE, Bee.createAttributes()
				.add(Attributes.SCALE, 0.7)
				.add(Attributes.ATTACK_DAMAGE, 2.0));
	}

	public static final ResourceKey<EquipmentAsset> STEEL_ARMOR_MATERIAL_KEY
			= ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("qlogic", "steel"));


	private void registerEvents() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (stack.is(Items.RESIN_CLUMP)) {
				var pos = hitResult.getBlockPos();
				BlockState state = world.getBlockState(pos);
				Optional<BlockState> waxedState = HoneycombItem.getWaxed(state);

				if (waxedState.isPresent()) {
					if (!world.isClientSide()) {
						world.setBlock(pos, waxedState.get(), 11);
						world.levelEvent(null, 3003, pos, 0);

						if (!player.isCreative()) {
							stack.shrink(1);
						}
					}
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});
	}
}