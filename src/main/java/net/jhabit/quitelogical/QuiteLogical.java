package net.jhabit.quitelogical;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.jhabit.quitelogical.block.ModBlocks;
import net.jhabit.quitelogical.items.ModItems;
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


		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register((itemGroup) -> itemGroup.accept(ModBlocks.GLOW_STICK));
	};
}