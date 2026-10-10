package dev.wildercord.player;

import dev.wildercord.Wildercord;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.WildercordEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Optional;

/**
 * Max mana and regeneration, from every source in one place. Both sides use it: the server
 * to regenerate and spend, the client to draw the HUD and the Cord screen.
 *
 * <ul>
 *   <li><b>Cord</b>: the base max mana and regeneration (see {@link CordTier}).</li>
 *   <li><b>Mana Crystals</b>: +10 max mana each, permanently, up to 100.</li>
 *   <li><b>Reservoir</b> (Cord enchantment): +25 max mana per level.</li>
 *   <li><b>Wellspring</b> (Cord enchantment): +25% regeneration per level.</li>
 *   <li><b>Clarity</b> (potion effect): +50% regeneration per level.</li>
 *   <li><b>Meditation</b>: sneak and stand still for a second: +100% regeneration.</li>
 *   <li><b>Siphon</b> (Cord enchantment): each creature your spells hit returns mana.</li>
 *   <li><b>Heart Circles</b>: +15 max mana and +0.5 regeneration per circle (see {@link Heart}).</li>
 *   <li><b>Focus of the Deep Well</b> (in the off-hand): +50 max mana while held.</li>
 * </ul>
 */
public final class Mana {
	private Mana() {}

	public static final int CRYSTAL_MANA = ManaCrystalRules.MANA_PER_CRYSTAL;
	public static final int MAX_CRYSTALS = ManaCrystalRules.MAX_CRYSTALS;
	public static final int RESERVOIR_MANA = 25;
	public static final float WELLSPRING_BONUS = 0.25F;
	public static final float CLARITY_BONUS = 0.5F;
	public static final float MEDITATION_BONUS = 1.0F;
	public static final int SIPHON_MANA = 2;
	public static final int SIPHON_CAP_PER_CAST = 16;
	/** Standing on a ley line. */
	public static final float LEY_BONUS = 1.0F;
	/** Near an awake Wellstone. */
	public static final float WELL_BONUS = 0.5F;

	public static final ResourceKey<Enchantment> RESERVOIR = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("reservoir"));
	public static final ResourceKey<Enchantment> WELLSPRING = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("wellspring"));
	public static final ResourceKey<Enchantment> SIPHON = ResourceKey.create(Registries.ENCHANTMENT, Wildercord.id("siphon"));

	/** A snapshot of where a player's mana comes from. */
	public record Stats(CordTier tier, int max, float regen, float regenMultiplier,
						int crystals, int reservoir, int wellspring, int siphon, int clarity, boolean meditating, int circles, boolean ley, boolean well) {
		public static final Stats NONE = new Stats(null, 0, 0, 1, 0, 0, 0, 0, 0, false, 0, false, false);

		public boolean boosted() {
			return regenMultiplier > 1.001F;
		}
	}

	public static Stats of(Player player) {
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			return Stats.NONE;
		}
		ItemStack cord = Spellbooks.cord(player);
		int crystals = crystals(player);
		int reservoir = level(player, cord, RESERVOIR);
		int wellspring = level(player, cord, WELLSPRING);
		int siphon = level(player, cord, SIPHON);
		MobEffectInstance clarityEffect = player.getEffect(WildercordEffects.CLARITY);
		int clarity = clarityEffect == null ? 0 : clarityEffect.getAmplifier() + 1;
		boolean meditating = player.getAttachedOrElse(WildercordAttachments.MEDITATING, false);
		int circles = Heart.active(player);
		dev.wildercord.spell.CircleVows.Effect vows = Heart.vowEffect(player);
		int max = tier.maxMana + crystals * CRYSTAL_MANA + reservoir * RESERVOIR_MANA + circles * dev.wildercord.spell.Circles.MANA_PER_CIRCLE
			+ dev.wildercord.gear.Gear.extraMana(player) + vows.mana() + nourished(player) * dev.wildercord.cooking.CookingRules.NOURISHED_MANA;
		boolean ley = player.getAttachedOrElse(WildercordAttachments.ON_LEY, false);
		boolean well = player.getAttachedOrElse(WildercordAttachments.WELL_UNTIL, 0L) > player.level().getGameTime();
		float multiplier = 1 + wellspring * WELLSPRING_BONUS + clarity * CLARITY_BONUS + (meditating ? MEDITATION_BONUS : 0)
			+ (ley ? LEY_BONUS : 0) + (well ? WELL_BONUS : 0) + dev.wildercord.cast.events.ManaStorm.regenBonus(player);
		float base = Math.max(0, tier.regenPerSecond + circles * dev.wildercord.spell.Circles.REGEN_PER_CIRCLE + vows.regen());
		// The server's mana.regen_multiplier scales all of it (sent to clients, so the HUD matches).
		base *= (float) dev.wildercord.config.Config.regenMultiplier(player);
		return new Stats(tier, max, base * multiplier, multiplier, crystals, reservoir, wellspring, siphon, clarity, meditating, circles, ley, well);
	}

	private static int nourished(Player player) {
		MobEffectInstance effect = player.getEffect(WildercordEffects.NOURISHED);
		return effect == null ? 0 : effect.getAmplifier() + 1;
	}

	public static int max(Player player) {
		return of(player).max();
	}

	public static int crystals(Player player) {
		return ManaCrystalRules.count(player.getAttachedOrElse(WildercordAttachments.CRYSTALS, 0));
	}

	/** Adds mana, capped at the player's max. */
	public static void restore(ServerPlayer player, float amount) {
		if (amount <= 0 || Spellbooks.tier(player) == null) {
			return;
		}
		Spellbooks.setMana(player, Math.min(max(player), Spellbooks.mana(player) + amount));
	}

	/** The level of one of the worn Cord's enchantments (0 without a Cord). */
	public static int enchantLevel(Player player, ResourceKey<Enchantment> key) {
		return level(player, Spellbooks.cord(player), key);
	}

	private static int level(Player player, ItemStack cord, ResourceKey<Enchantment> key) {
		if (cord.isEmpty()) {
			return 0;
		}
		Optional<? extends Holder<Enchantment>> holder = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
		return holder.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, cord)).orElse(0);
	}
}
