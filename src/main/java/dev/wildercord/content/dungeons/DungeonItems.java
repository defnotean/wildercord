package dev.wildercord.content.dungeons;

import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordBlocks;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

/** The dungeon bosses' trophies, one each, and their place in the creative tab. */
public final class DungeonItems {
	private DungeonItems() {}

	/** The Cinder Warden's heart, still warm: fire can't touch whoever holds it up (3 minutes). */
	public static final Item CINDER_HEART = register("cinder_heart", p -> new TrophyItem(p, DungeonSounds.WARDEN_AMBIENT,
		new TrophyItem.Boon(MobEffects.FIRE_RESISTANCE, 20 * 180, 0)));
	/** The lens the Star-Eater grew around its eye: see in the dark and fall like starlight (3 minutes, 1 minute). */
	public static final Item ASTRAL_LENS = register("astral_lens", p -> new TrophyItem(p, DungeonSounds.STAR_EATER_REFLECT,
		new TrophyItem.Boon(MobEffects.NIGHT_VISION, 20 * 180, 0), new TrophyItem.Boon(MobEffects.SLOW_FALLING, 20 * 60, 0)));
	/** The Tide Scribe's quill, never dry: breathe and swim like the drowned (3 minutes). */
	public static final Item DROWNED_QUILL = register("drowned_quill", p -> new TrophyItem(p, DungeonSounds.TIDE_RISE,
		new TrophyItem.Boon(MobEffects.WATER_BREATHING, 20 * 180, 0), new TrophyItem.Boon(MobEffects.DOLPHINS_GRACE, 20 * 180, 0)));

	private static Item register(String path, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().setId(key)));
	}

	public static void init() {
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(output ->
			output.insertAfter(WildercordBlocks.ARCHIVE_LECTERN, DungeonBlocks.ALTAR, CINDER_HEART, ASTRAL_LENS, DROWNED_QUILL));
	}
}
