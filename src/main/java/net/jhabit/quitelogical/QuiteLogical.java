package net.jhabit.quitelogical;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.jhabit.quitelogical.block.ModBlocks;
import net.jhabit.quitelogical.items.ModItems;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.item.CreativeModeTabs;

public class QuiteLogical implements ModInitializer {
	public static final String MOD_ID = "qlogic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModItems.initialize();

		// 아이템 그룹 등록
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register((itemGroup) -> itemGroup.accept(ModBlocks.GLOW_STICK)); // 불필요한 세미콜론 제거

		// 1.21.11 환경에서의 양조 레시피 등록 (반드시 onInitialize 안에 위치해야 함)
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
			builder.registerPotionRecipe(
					Potions.AWKWARD,                       // 하단: 어색한 포션
					Ingredient.of(Items.POISONOUS_POTATO), // 상단: 썩은 감자
					Potions.POISON                         // 결과: 독 포션
			);
		});

		LOGGER.info("Quite Logical 모드가 성공적으로 초기화되었습니다!");
	} // onInitialize 메서드가 여기서 끝나야 합니다.
}