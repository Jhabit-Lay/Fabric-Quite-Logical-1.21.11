package net.jhabit.quitelogical;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.jhabit.quitelogical.block.ModBlocks;
import net.jhabit.quitelogical.entity.JungleZombie;
import net.jhabit.quitelogical.entity.LeaderZombie;
import net.jhabit.quitelogical.items.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

import static net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND;

public class QuiteLogical implements ModInitializer {
	public static final String MOD_ID = "qlogic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier ZOMBIE_LEADER_ID =
			Identifier.fromNamespaceAndPath(MOD_ID, "zombie_leader");

	public static final EntityType<LeaderZombie> ZOMBIE_LEADER = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ZOMBIE_LEADER_ID,
			EntityType.Builder.<LeaderZombie>of(LeaderZombie::new, MobCategory.MONSTER)
					.sized(0.6f * 1.1f, 1.95f * 1.1f)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, ZOMBIE_LEADER_ID))
	);

	public static final Identifier JUNGLE_ZOMBIE_ID =
			Identifier.fromNamespaceAndPath(MOD_ID, "jungle_zombie");

	public static final EntityType<JungleZombie> JUNGLE_ZOMBIE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			JUNGLE_ZOMBIE_ID,
			EntityType.Builder.<JungleZombie>of(JungleZombie::new, MobCategory.MONSTER)
					.sized(0.6f, 1.95f * 0.95f)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, JUNGLE_ZOMBIE_ID))
	);

	@Override
	public void onInitialize() {
		ModComponents.register();
		ModBlocks.initialize();
		ModItems.initialize();

		FabricDefaultAttributeRegistry.register(ZOMBIE_LEADER,
				Zombie.createAttributes()
						.add(Attributes.SCALE,1.05)
						.add(Attributes.MAX_HEALTH, 30.0)
						.add(Attributes.MOVEMENT_SPEED, 0.23)
						.add(Attributes.ARMOR_TOUGHNESS, 2)
		);


		FabricDefaultAttributeRegistry.register(JUNGLE_ZOMBIE,
				Zombie.createAttributes()
						.add(Attributes.SCALE,0.95)
						.add(Attributes.MAX_HEALTH, 15.0)   // 일반 좀비(20)보다 낮음
						.add(Attributes.MOVEMENT_SPEED, 0.29) // 일반 좀비(0.23)보다 빠름
						.add(Attributes.ARMOR_TOUGHNESS, 0)
		);

		BiomeModifications.addSpawn(
				BiomeSelectors.includeByKey(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE),
				MobCategory.MONSTER,
				QuiteLogical.JUNGLE_ZOMBIE,
				140, // 가중치 (일반 좀비는 보통 100)
				2,  // 최소 그룹 크기
				4   // 최대 그룹 크기
		);
		SpawnPlacements.register(
				QuiteLogical.JUNGLE_ZOMBIE,
				SpawnPlacementTypes.ON_GROUND, // Type -> PlacementTypes로 변경
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Monster::checkMonsterSpawnRules
		);

		FabricDefaultAttributeRegistry.register(EntityType.COW, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 1.0));
		FabricDefaultAttributeRegistry.register(EntityType.MOOSHROOM, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 1.0));

		FabricDefaultAttributeRegistry.register(EntityType.BEE, Bee.createAttributes()
				.add(Attributes.SCALE, 0.7)
				.add(Attributes.ATTACK_DAMAGE, 2.0));

		FabricDefaultAttributeRegistry.register(EntityType.COW, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 2.0));
		FabricDefaultAttributeRegistry.register(EntityType.MOOSHROOM, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 2.0));

		// 아이템 그룹 등록
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(entries -> entries.accept(ModBlocks.GLOW_STICK));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
			content.accept(ModItems.COPPER_GOAT_HORN);

			// [오류 해결] 양조 레시피 등록 코드를 메서드 안으로 이동
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder ->
			builder.registerPotionRecipe(Potions.AWKWARD, Ingredient.of(Items.POISONOUS_POTATO), Potions.POISON)
			);

			// [레진 밀랍칠 기능 구현]
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

		LOGGER.info("Quite Logical 엔티티 등록 완료!");
		});
	}
}