package dev.wildercord.runesmith;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.MethodSources;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.npc.villager.VillagerTrades.ItemListing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 1.21.11 has no data-driven villager trades: the {@code trade_set}/{@code villager_trade} registries and their
 * tags only exist from 26.x on. The JSONs stay in the jar (they are still the single source of truth), and this
 * reads them at startup into {@link VillagerTrades#TRADES} the way 1.21.11 expects: one {@link ItemListing} per
 * offer, built fresh each restock. It also folds in the vanilla additions under {@code data/minecraft/tags/
 * villager_trade} (the cleric and weaponsmith manuals, the wandering trader's uncommon slot).
 */
public final class VillagerTradeCompat {
	private VillagerTradeCompat() {}

	public static void init() {
		// The manual's item registers in its own class's static init; touch it first so the
		// cleric and weaponsmith trades below can resolve wildercord:breathing_manual.
		if (BreathingManualItem.MANUAL == null) {
			throw new IllegalStateException("breathing manual not registered before trades");
		}
		for (int level = 1; level <= 5; level++) {
			ItemListing[] listings = loadTag("wildercord", "runesmith", "level_" + level);
			if (listings.length > 0) {
				VillagerTrades.TRADES.computeIfAbsent(Runesmith.PROFESSION, k -> new Int2ObjectOpenHashMap<>())
					.put(level, listings);
			}
		}
		addVanilla(VillagerProfession.CLERIC, 5, loadTag("minecraft", "cleric", "level_5"));
		addVanilla(VillagerProfession.WEAPONSMITH, 5, loadTag("minecraft", "weaponsmith", "level_5"));
		ItemListing[] uncommon = loadTag("minecraft", "wandering_trader", "uncommon");
		// 1.21.11's WANDERING_TRADER_TRADES is an immutable list; the offers go in at restock
		// through WanderingTraderTradesMixin instead.
		wanderingUncommon = uncommon;
	}

	private static ItemListing[] wanderingUncommon = new ItemListing[0];

	/** The wandering trader's uncommon slot, appended when it builds its offers. */
	public static void appendWanderingTrades(net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader trader,
			net.minecraft.server.level.ServerLevel level) {
		if (wanderingUncommon.length == 0) {
			return;
		}
		net.minecraft.world.item.trading.MerchantOffers offers = trader.getOffers();
		for (ItemListing listing : wanderingUncommon) {
			MerchantOffer offer = listing.getOffer(level, trader, RandomSource.create());
			if (offer != null) {
				offers.add(offer);
			}
		}
	}

	private static void addVanilla(ResourceKey<VillagerProfession> profession, int level, ItemListing[] extra) {
		if (extra.length == 0) {
			return;
		}
		Int2ObjectMap<ItemListing[]> byLevel = VillagerTrades.TRADES.computeIfAbsent(profession, k -> new Int2ObjectOpenHashMap<>());
		ItemListing[] existing = byLevel.getOrDefault(level, new ItemListing[0]);
		List<ItemListing> merged = new ArrayList<>(List.of(existing));
		merged.addAll(List.of(extra));
		byLevel.put(level, merged.toArray(new ItemListing[0]));
	}

	/** The offers a level's tag lists, read from {@code data/<ns>/villager_trade/<path>.json}. */
	private static ItemListing[] loadTag(String namespace, String profession, String level) {
		List<ItemListing> listings = new ArrayList<>();
		for (String id : readTagValues(namespace, profession, level)) {
			Identifier tradeId = Identifier.parse(id);
			JsonObject json = readJson("data/" + tradeId.getNamespace() + "/villager_trade/" + tradeId.getPath() + ".json");
			if (json != null) {
				listings.add(listing(tradeId, json));
			}
		}
		return listings.toArray(new ItemListing[0]);
	}

	private static List<String> readTagValues(String namespace, String profession, String level) {
		JsonObject tag = readJson("data/" + namespace + "/tags/villager_trade/" + profession + "/" + level + ".json");
		List<String> ids = new ArrayList<>();
		if (tag != null && tag.has("values")) {
			for (JsonElement value : tag.getAsJsonArray("values")) {
				ids.add(value.getAsString());
			}
		}
		return ids;
	}

	private static JsonObject readJson(String path) {
		try (InputStream in = VillagerTradeCompat.class.getResourceAsStream("/" + path)) {
			if (in == null) {
				return null;
			}
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			return null;
		}
	}

	private static ItemListing listing(Identifier id, JsonObject json) {
		ItemCost costA = cost(json.getAsJsonObject("wants"));
		ItemCost costB = json.has("additional_wants") ? cost(json.getAsJsonObject("additional_wants")) : null;
		ItemStack gives = stack(json.getAsJsonObject("gives"));
		int maxUses = json.has("max_uses") ? json.get("max_uses").getAsInt() : 12;
		int xp = json.has("xp") ? json.get("xp").getAsInt() : 0;
		float priceMultiplier = json.has("reputation_discount") ? json.get("reputation_discount").getAsFloat() : 0.0F;
		int demand = json.has("demand") ? json.get("demand").getAsInt() : 0;
		String breathingSource = null;
		if (json.has("given_item_modifiers")) {
			for (JsonElement element : json.getAsJsonArray("given_item_modifiers")) {
				JsonObject modifier = element.getAsJsonObject();
				if ("wildercord:random_breathing_method".equals(optString(modifier, "function"))) {
					breathingSource = optString(modifier, "source");
				}
			}
		}
		final String source = breathingSource;
		return (serverLevel, trader, random) -> {
			ItemStack result = gives.copy();
			if (source != null) {
				// The same draw wildercord:random_breathing_method does on a loot table.
				java.util.Random dice = new java.util.Random(random.nextLong());
				String method = MethodSources.byId(source).flatMap(s -> s.draw(dice))
					.orElseGet(() -> BreathingMethods.BUILT_IN.get(dice.nextInt(BreathingMethods.BUILT_IN.size())).id());
				result.set(BreathingManualItem.METHOD, method);
			}
			return new MerchantOffer(costA, java.util.Optional.ofNullable(costB), result, maxUses, xp, demand, priceMultiplier);
		};
	}

	private static String optString(JsonObject json, String field) {
		return json.has(field) ? json.get(field).getAsString() : "";
	}

	private static ItemCost cost(JsonObject wants) {
		ItemStack stack = stack(wants);
		return new ItemCost(stack.getItem(), stack.getCount());
	}

	private static ItemStack stack(JsonObject json) {
		Identifier itemId = Identifier.parse(json.get("id").getAsString());
		ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId).orElseThrow());
		if (json.has("count")) {
			stack.setCount(json.get("count").getAsInt());
		}
		return stack;
	}
}
