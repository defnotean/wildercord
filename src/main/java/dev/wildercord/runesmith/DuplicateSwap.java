package dev.wildercord.runesmith;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
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
 * offers are added as the trading screen opens and taken away when it closes (and before the
 * villager turns into anything else), so they're never seen by anyone else or kept for long.
 *
 * <p>So the swaps can't be farmed: only runes the Runesmith deals in itself are taken (never a
 * conjured Lightning rune, see {@link RuneTrades#takes}), only plain rank I ones pay, a player can
 * sell back {@link RuneTrades#DAILY_BUYBACKS} runes a day, and no swap gives experience.</p>
 */
public final class DuplicateSwap {
	private DuplicateSwap() {}

	/** How many runes a player has sold back, and on which day. */
	public record Sold(long day, int count) {
		public static final Sold NONE = new Sold(Long.MIN_VALUE, 0);
		public static final Codec<Sold> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("day").forGetter(Sold::day),
			Codec.INT.fieldOf("count").forGetter(Sold::count)
		).apply(i, Sold::new));
	}

	/** Each player's buybacks today. Saved and kept through death, so logging off or dying doesn't reset it. */
	public static final AttachmentType<Sold> SOLD = AttachmentRegistry.create(
		Wildercord.id("runes_sold"),
		builder -> builder
			.initializer(() -> Sold.NONE)
			.persistent(Sold.CODEC)
			.copyOnDeath()
	);

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
		int left = buybacksLeft(player);
		for (String id : left <= 0 ? List.<String>of() : RuneTrades.buybacks(held)) {
			RuneDef rune = Runes.get(id).orElseThrow();
			// No price multiplier: reputation and demand never change what a rune fetches. No experience either.
			offers.add(new MerchantOffer(cost(id), new ItemStack(Items.EMERALD, RuneTrades.buybackPrice(rune.tier())), left, 0, 0.0F));
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
				offers.add(new MerchantOffer(cost(pair.first()), Optional.of(cost(pair.second())), RuneItem.stack(result.get()), 1, 0, 0.0F));
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

	/**
	 * Whether an offer is one of these swaps: it asks for a rune, and like every swap gives no
	 * experience and ignores prices (so a data pack's trade asking for a rune isn't taken for one).
	 */
	public static boolean isSwap(MerchantOffer offer) {
		return offer.getItemCostA().item().value() == WildercordItems.RUNE && offer.getXp() == 0 && offer.getPriceMultiplier() == 0.0F;
	}

	/** A buyback (a rune for emeralds), as opposed to a reroll (two runes for one). */
	public static boolean isBuyback(MerchantOffer offer) {
		return isSwap(offer) && offer.getCostB().isEmpty() && offer.getResult().is(Items.EMERALD);
	}

	/**
	 * Whether a stack may pay for a swap: the Runesmith only takes plain rank I runes (a ranked one
	 * would go for a rank I's price). Anything but a rune is left to the offer's own check.
	 */
	public static boolean pays(ItemStack stack) {
		return !stack.is(WildercordItems.RUNE) || RuneTrades.plainRank(stack.get(WildercordComponents.RANK));
	}

	/** Buybacks this player has left today. */
	public static int buybacksLeft(ServerPlayer player) {
		Sold sold = player.getAttachedOrElse(SOLD, Sold.NONE);
		return RuneTrades.buybacksLeft(sold.day(), sold.count(), Contracts.day(player));
	}

	/**
	 * A swap was just made (called in place of the villager's trade experience, so swaps give none).
	 * A buyback counts toward the day's; once they're used up, every buyback left in the list closes.
	 */
	public static void traded(Villager villager, MerchantOffer offer) {
		if (!isBuyback(offer) || !(villager.getTradingPlayer() instanceof ServerPlayer player)) {
			return;
		}
		long today = Contracts.day(player);
		Sold sold = player.getAttachedOrElse(SOLD, Sold.NONE);
		// Time turned back doesn't make a new day: the count carries on until a later day comes.
		Sold next = sold.day() >= today ? new Sold(sold.day(), sold.count() + 1) : new Sold(today, 1);
		player.setAttached(SOLD, next);
		if (RuneTrades.buybacksLeft(next.day(), next.count(), today) > 0) {
			return;
		}
		for (MerchantOffer other : villager.getOffers()) {
			if (isBuyback(other)) {
				other.setToOutOfStock();
			}
		}
		player.sendMerchantOffers(player.containerMenu.containerId, villager.getOffers(), villager.getVillagerData().level(), villager.getVillagerXp(),
			villager.showProgressBar(), villager.canRestock());
	}

	private static ItemCost cost(String runeId) {
		return new ItemCost(WildercordItems.RUNE.builtInRegistryHolder(), 1, DataComponentExactPredicate.expect(WildercordComponents.RUNE, runeId));
	}

	/** Every plain (rank I) rune in the player's inventory that they already know, and how many of each. */
	static Map<String, Integer> knownHeld(ServerPlayer player) {
		Map<String, Integer> held = new LinkedHashMap<>();
		List<ItemStack> items = player.getInventory().getNonEquipmentItems();
		for (ItemStack stack : items) {
			String id = stack.is(WildercordItems.RUNE) && pays(stack) ? stack.get(WildercordComponents.RUNE) : null;
			if (id != null && Spellbooks.knows(player, id)) {
				held.merge(id, stack.getCount(), Integer::sum);
			}
		}
		return held;
	}
}
