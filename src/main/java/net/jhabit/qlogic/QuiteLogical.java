package net.jhabit.qlogic;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import net.jhabit.qlogic.block.ModBlocks;
import net.jhabit.qlogic.entity.ModEntities;
import net.jhabit.qlogic.items.ModItems;
import net.jhabit.qlogic.mixin.CopperGolemAccessor;
import net.jhabit.qlogic.network.CrawlPayload;
import net.jhabit.qlogic.network.PingPayload;
import net.jhabit.qlogic.network.RemovePingPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.jhabit.qlogic.block.ModBlocks.*;
import static net.minecraft.world.level.block.Blocks.POWERED_RAIL;

public class QuiteLogical implements ModInitializer {
	public static final String MOD_ID = "qlogic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final EntityDataAccessor<Boolean> QLOGIC$CRAWLING = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);

	private static final Map<UUID, BlockPos> PLAYER_PINGS = new HashMap<>();

	@Override
	public void onInitialize() {
		ModComponents.register();
		ModBlocks.initialize();
		ModItems.initialize();
		ModEntities.initialize();

		setupSpawns();
		registerVanillaAttributeModifiers();
		setupItemGroups();

		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder ->
				builder.registerPotionRecipe(Potions.AWKWARD, Ingredient.of(Items.POISONOUS_POTATO), Potions.POISON)
		);

		registerEvents();

		// --- [1] 패킷 타입 등록 (C2S/S2C 공통) / Packet Type Registration ---
		PayloadTypeRegistry.playC2S().register(PingPayload.TYPE, PingPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(PingPayload.TYPE, PingPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RemovePingPayload.TYPE, RemovePingPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(RemovePingPayload.TYPE, RemovePingPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CrawlPayload.TYPE, CrawlPayload.CODEC);

		// [KR] 핑 추가 수신기 (1인 1핑 로직) / [EN] Ping Add Receiver (1-ping limit)
		ServerPlayNetworking.registerGlobalReceiver(PingPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				UUID uuid = payload.senderUuid();

				// 1. 기존 핑이 있다면 삭제 패킷을 모든 유저에게 먼저 전송
				if (PLAYER_PINGS.containsKey(uuid)) {
					BlockPos oldPos = PLAYER_PINGS.get(uuid);
					RemovePingPayload removePacket = new RemovePingPayload(oldPos);
					for (ServerPlayer p : context.server().getPlayerList().getPlayers()) {
						ServerPlayNetworking.send(p, removePacket);
					}
				}

				// 2. 새 위치 저장 및 모든 유저에게 브로드캐스트
				PLAYER_PINGS.put(uuid, payload.pos());
				for (ServerPlayer p : context.server().getPlayerList().getPlayers()) {
					ServerPlayNetworking.send(p, payload);
				}

				context.player().level().playSound(null, payload.pos(),
						SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 0.5f);
			});
		});

		// 핑 제거 수신기 / Ping Removal
		ServerPlayNetworking.registerGlobalReceiver(RemovePingPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				PLAYER_PINGS.remove(context.player().getUUID());
				for (ServerPlayer p : context.server().getPlayerList().getPlayers()) {
					ServerPlayNetworking.send(p, payload);
				}
			});
		});

		// 엎드리기 수신기 / Crawl Receiver
		ServerPlayNetworking.registerGlobalReceiver(CrawlPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				player.getEntityData().set(QLOGIC$CRAWLING, payload.isCrawling());
				player.setPose(payload.isCrawling() ? Pose.SWIMMING : Pose.STANDING);
			});
		});
		LOGGER.info("Quite Logical 모드가 성공적으로 로드되었습니다!");
	}

	private void setupItemGroups() {
		// 1. 기능 블록 탭 (Functional Blocks)
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			// 레드스톤 횃불(REDSTONE_TORCH) 바로 뒤에 글로우 스틱 배치
			entries.addAfter(Items.REDSTONE_TORCH, ModBlocks.GLOW_STICK);
		});
		// 2. 도구 및 유틸리티 탭
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			// 염소 뿔(GOAT_HORN) 뒤에 구리 염소 뿔 배치 (자동으로 첫 번째 음반인 13번 음반 앞에 위치함)
			entries.addAfter(Items.GOAT_HORN, ModItems.COPPER_GOAT_HORN);
			entries.addAfter(Items.POWERED_RAIL,
					ModBlocks.EXPOSED_POWERED_RAIL,
					WEATHERED_POWERED_RAIL,
					OXIDIZED_POWERED_RAIL,
					ModBlocks.WAXED_POWERED_RAIL,
					ModBlocks.WAXED_EXPOSED_POWERED_RAIL,
					ModBlocks.WAXED_WEATHERED_POWERED_RAIL,
					WAXED_OXIDIZED_POWERED_RAIL
			);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
			// 일반 파워레일 뒤에 모든 구리 파워레일 단계를 순서대로 추가
			entries.addAfter(Items.POWERED_RAIL,
					ModBlocks.EXPOSED_POWERED_RAIL,
					WEATHERED_POWERED_RAIL,
					OXIDIZED_POWERED_RAIL,
					ModBlocks.WAXED_POWERED_RAIL,
					ModBlocks.WAXED_EXPOSED_POWERED_RAIL,
					ModBlocks.WAXED_WEATHERED_POWERED_RAIL,
					WAXED_OXIDIZED_POWERED_RAIL
			);
		});
	}

	// Spawn setup
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

	private void registerEvents() {
		// --- 1. 블록 밀랍칠 로직 (기존 유지) ---
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
						if (!player.isCreative()) stack.shrink(1);
					}
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});

		// --- 2. 구리 골렘 밀랍칠 및 도끼 회수 로직 ---
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (entity instanceof CopperGolem golem) {
				CopperGolemAccessor accessor = (CopperGolemAccessor) golem;

				// [Case A] 레진으로 밀랍칠하기
				if (stack.is(Items.RESIN_CLUMP)) {
					if (accessor.getNextWeatheringTick() == -2L) return InteractionResult.PASS;
					if (!world.isClientSide()) {
						accessor.setNextWeatheringTick(-2L);
						world.levelEvent(null, 3003, entity.blockPosition(), 0);
						if (!player.isCreative()) stack.shrink(1);
					}
					return InteractionResult.SUCCESS;
				}

				// [Case B] 도끼로 밀랍 벗기기 (레진 드롭 로직 제거됨)
				if (stack.is(net.minecraft.tags.ItemTags.AXES) && accessor.getNextWeatheringTick() == -2L) {
					if (!world.isClientSide()) {
						accessor.setNextWeatheringTick(-1L);
						world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
								net.minecraft.sounds.SoundEvents.AXE_WAX_OFF, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
						world.levelEvent(null, 3004, entity.blockPosition(), 0);

						// 내구도 소모 로직 유지
						EquipmentSlot slot = (hand == net.minecraft.world.InteractionHand.MAIN_HAND)
								? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
						stack.hurtAndBreak(1, player, slot);
					}
					return InteractionResult.SUCCESS;
				}
			}
			return InteractionResult.PASS;
		});
	}
}