package net.jhabit.quitelogical;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.jhabit.quitelogical.block.ModBlocks;
import net.jhabit.quitelogical.items.ModItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class QuiteLogical implements ModInitializer {
	public static final String MOD_ID = "qlogic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModItems.initialize();

		// 아이템 그룹 등록
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(entries -> entries.accept(ModBlocks.GLOW_STICK));

		// [오류 해결] 양조 레시피 등록 코드를 메서드 안으로 이동
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder ->
				builder.registerPotionRecipe(Potions.AWKWARD, Ingredient.of(Items.POISONOUS_POTATO), Potions.POISON)
		);

		FabricDefaultAttributeRegistry.register(EntityType.COW, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 2.0));
		FabricDefaultAttributeRegistry.register(EntityType.MOOSHROOM, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 2.0));

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

		LOGGER.info("Quite Logical (Mojmap) 초기화 완료!");
	}
}