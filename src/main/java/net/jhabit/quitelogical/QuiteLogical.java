package net.jhabit.quitelogical;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.jhabit.quitelogical.block.ModBlocks;
import net.jhabit.quitelogical.items.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuiteLogical implements ModInitializer {
	public static final String MOD_ID = "qlogic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModItems.initialize();

		// 아이템 그룹 등록
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register((itemGroup) -> itemGroup.accept(ModBlocks.GLOW_STICK));

		// [오류 해결] 양조 레시피 등록 코드를 메서드 안으로 이동
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
			builder.registerPotionRecipe(
					Potions.AWKWARD,
					Ingredient.of(Items.POISONOUS_POTATO),
					Potions.POISON
			);
		});

		// [소 변이 대응] 소와 무시룸 모두에게 공격력 속성 부여
		// Mixin에서 오류가 나는 속성 등록은 여기서 하는 것이 가장 안전합니다.
		FabricDefaultAttributeRegistry.register(EntityType.COW, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 2.0));
		FabricDefaultAttributeRegistry.register(EntityType.MOOSHROOM, Cow.createAttributes().add(Attributes.ATTACK_DAMAGE, 2.0));

		LOGGER.info("Quite Logical 초기화 완료!");
	}
}