package dev.wildercord.runesmith;

import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Duplicates always have value: while a player trades with a Runesmith, its list gains offers made
 * for them alone. It buys back any rune they already know (for emeralds), and for each tier where
 * they hold two known runes it offers a reroll into one of that tier they haven't learned. The
 * offers are added as the trading screen opens and taken away when it closes, so they're never
 * seen by anyone else or saved with the villager for long.
 */
public final class DuplicateSwap {
	private DuplicateSwap() {}

	/** Villagers with swap offers in their list, and who they were made for. */
	private static final Map<Villager, ServerPlayer> OPEN = new HashMap<>();

	public static void init() {
		// Before vanilla opens the trading screen, which sends the list.
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer && !serverPlayer.isSpectator()
					&& entity instanceof Villager villager && Runesmith.is(villager) && !villager.isTrading() && !villager.isBaby() && villager.isAlive()) {
				offer(serverPlayer, villager);
			}
			return InteractionResult.PASS;
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 10 != 0 || OPEN.isEmpty()) {
				return;
			}
			Iterator<Map.Entry<Villager, ServerPlayer>> it = OPEN.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Villager, ServerPlayer> open = it.next();
				Villager villager = open.getKey();
				if (villager.isRemoved() || villager.getTradingPlayer() != open.getValue()) {
					if (!villager.isRemoved()) {
						strip(villager);
					}
					it.remove();
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			OPEN.keySet().forEach(DuplicateSwap::strip);
			OPEN.clear();
		});
	}

	/** Adds this player's buyback and reroll offers to a Runesmith's list. Returns how many were added. */
	public static int offer(ServerPlayer player, Villager villager) {
		strip(villager);
		MerchantOffers offers = villager.getOffers();
		if (offers.isEmpty()) {
			return 0;
		}
		Map<String, Integer> held = knownHeld(player);
		int added = 0;
		for (String id : RuneTrades.buybacks(held)) {
			RuneDef rune = Runes.get(id).orElseThrow();
			// No price multiplier: reputation and demand never change what a rune fetches.
			offers.add(new MerchantOffer(cost(id), new ItemStack(Items.EMERALD, RuneTrades.buybackPrice(rune.tier())), 16, 1, 0.0F));
			added++;
		}
		Set<String> known = new HashSet<>(Spellbooks.get(player).learned());
		long day = player.level().getOverworldClockTime() / 24000L;
		for (RuneTrades.Pair pair : RuneTrades.rerolls(held)) {
			// The same pick all day for this villager, until something new is learned.
			long seed = player.getUUID().getLeastSignificantBits() * 31 + villager.getUUID().getMostSignificantBits() + day * 1_000_003L
				+ pair.tier() * 7919L + known.size();
			Optional<RuneDef> result = RuneTrades.reroll(pair.tier(), known, seed);
			if (result.isPresent()) {
				// Both halves of the price are single runes, so no discount can take one away.
				offers.add(new MerchantOffer(cost(pair.first()), Optional.of(cost(pair.second())), RuneItem.stack(result.get()), 1, 2, 0.0F));
				added++;
			}
		}
		if (added > 0) {
			OPEN.put(villager, player);
		}
		return added;
	}

	/** Takes every swap offer back out of a villager's list (a Runesmith's own trades never ask for runes). */
	public static void strip(Villager villager) {
		villager.getOffers().removeIf(DuplicateSwap::isSwap);
	}

	public static boolean isSwap(MerchantOffer offer) {
		return offer.getItemCostA().item().value() == WildercordItems.RUNE;
	}

	private static ItemCost cost(String runeId) {
		return new ItemCost(WildercordItems.RUNE.builtInRegistryHolder(), 1, DataComponentExactPredicate.expect(WildercordComponents.RUNE, runeId));
	}

	/** Every rune in the player's inventory that they already know, and how many of each. */
	static Map<String, Integer> knownHeld(ServerPlayer player) {
		Map<String, Integer> held = new LinkedHashMap<>();
		List<ItemStack> items = player.getInventory().getNonEquipmentItems();
		for (ItemStack stack : items) {
			String id = stack.is(WildercordItems.RUNE) ? stack.get(WildercordComponents.RUNE) : null;
			if (id != null && Spellbooks.knows(player, id)) {
				held.merge(id, stack.getCount(), Integer::sum);
			}
		}
		return held;
	}
}
