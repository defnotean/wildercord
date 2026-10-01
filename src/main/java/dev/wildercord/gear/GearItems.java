package dev.wildercord.gear;

import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One item for every piece of casting gear in {@link GearDef}. */
public final class GearItems {
	private GearItems() {}

	private static final Map<String, CastingGearItem> BY_PATH = new LinkedHashMap<>();

	static {
		for (GearDef def : GearDef.all()) {
			Rarity rarity = switch (def.kind()) {
				case STAFF, FOCUS, SASH -> Rarity.UNCOMMON;
				case TOME -> Rarity.RARE;
				case GREATER_STAFF -> Rarity.EPIC;
			};
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(def.path()));
			CastingGearItem item = new CastingGearItem(def, new Item.Properties().stacksTo(1).rarity(rarity).setId(key));
			BY_PATH.put(def.path(), Registry.register(BuiltInRegistries.ITEM, key, item));
		}
	}

	/** Every gear item, in {@link GearDef} order (the tome, the foci, the staffs, the greater staffs). */
	public static List<CastingGearItem> all() {
		return Collections.unmodifiableList(new ArrayList<>(BY_PATH.values()));
	}

	public static CastingGearItem get(GearDef def) {
		return BY_PATH.get(def.path());
	}

	public static void init() {}
}
