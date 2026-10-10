package dev.wildercord.content;

import dev.wildercord.Wildercord;
import dev.wildercord.player.Mana;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;

/**
 * Mana potions and the Potion of Warding. Clarity speeds up mana regeneration for a while; Mana restores it at once;
 * Warded takes a share off every spell that hurts you. All three brew from an Awkward Potion (amethyst shard for Clarity,
 * lapis lazuli for Mana, tinted glass for Warding: glass that keeps light out, against magic that is light).
 */
public final class WildercordEffects {
	private WildercordEffects() {}

	/** +50% mana regeneration per level. */
	public static final Holder<MobEffect> CLARITY = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("clarity"),
		new ClarityEffect());

	/** Instantly restores 60 mana, doubled per level. */
	public static final Holder<MobEffect> MANA = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("mana"),
		new ManaEffect());

	/** 20% less damage from spells per level, 80% at most (see {@code cast.SpellDefenceRules}). */
	public static final Holder<MobEffect> WARDED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("warded"),
		new WardedEffect());

	/** A good meal (0.12 "Tempering"): +20 max mana per level. */
	public static final Holder<MobEffect> NOURISHED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("nourished"),
		new PlainEffect(0xE8A25A));

	/** A clear head from a good meal: spells cost 8% less per level (two levels at most). */
	public static final Holder<MobEffect> FOCUSED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("focused"),
		new PlainEffect(0x7FD8E8));

	/** A mana elixir (0.13): mana refills faster, but there's less room for it (see {@code player.ElixirRules}). */
	public static final Holder<MobEffect> TORRENT = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("torrent"),
		new PlainEffect(0x5AC8F0));

	/** A mana elixir: more room for mana, but it refills slower. */
	public static final Holder<MobEffect> DEEP_WELL = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("deep_well"),
		new PlainEffect(0x3A4AB0));

	/** A mana elixir: more of the mana spent condenses toward the next circle, but every spell costs more. */
	public static final Holder<MobEffect> CONDENSING = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("condensing"),
		new PlainEffect(0xF0C860));

	public static final Holder<Potion> CLARITY_POTION = potion("clarity", new Potion("wildercord_clarity", new MobEffectInstance(CLARITY, 3600)));
	public static final Holder<Potion> LONG_CLARITY_POTION = potion("long_clarity", new Potion("wildercord_clarity", new MobEffectInstance(CLARITY, 9600)));
	public static final Holder<Potion> STRONG_CLARITY_POTION = potion("strong_clarity", new Potion("wildercord_clarity", new MobEffectInstance(CLARITY, 1800, 1)));
	public static final Holder<Potion> MANA_POTION = potion("mana", new Potion("wildercord_mana", new MobEffectInstance(MANA, 1)));
	public static final Holder<Potion> STRONG_MANA_POTION = potion("strong_mana", new Potion("wildercord_mana", new MobEffectInstance(MANA, 1, 1)));
	public static final Holder<Potion> WARDED_POTION = potion("warded", new Potion("wildercord_warded", new MobEffectInstance(WARDED, 3600)));
	public static final Holder<Potion> LONG_WARDED_POTION = potion("long_warded", new Potion("wildercord_warded", new MobEffectInstance(WARDED, 9600)));
	public static final Holder<Potion> STRONG_WARDED_POTION = potion("strong_warded", new Potion("wildercord_warded", new MobEffectInstance(WARDED, 1800, 1)));

	public static final Holder<Potion> TORRENT_POTION = potion("torrent", new Potion("wildercord_torrent", new MobEffectInstance(TORRENT, 3600)));
	public static final Holder<Potion> LONG_TORRENT_POTION = potion("long_torrent", new Potion("wildercord_torrent", new MobEffectInstance(TORRENT, 9600)));
	public static final Holder<Potion> STRONG_TORRENT_POTION = potion("strong_torrent", new Potion("wildercord_torrent", new MobEffectInstance(TORRENT, 1800, 1)));
	public static final Holder<Potion> DEEP_WELL_POTION = potion("deep_well", new Potion("wildercord_deep_well", new MobEffectInstance(DEEP_WELL, 3600)));
	public static final Holder<Potion> LONG_DEEP_WELL_POTION = potion("long_deep_well", new Potion("wildercord_deep_well", new MobEffectInstance(DEEP_WELL, 9600)));
	public static final Holder<Potion> STRONG_DEEP_WELL_POTION = potion("strong_deep_well", new Potion("wildercord_deep_well", new MobEffectInstance(DEEP_WELL, 1800, 1)));
	public static final Holder<Potion> CONDENSING_POTION = potion("condensing", new Potion("wildercord_condensing", new MobEffectInstance(CONDENSING, 3600)));
	public static final Holder<Potion> LONG_CONDENSING_POTION = potion("long_condensing", new Potion("wildercord_condensing", new MobEffectInstance(CONDENSING, 9600)));
	public static final Holder<Potion> STRONG_CONDENSING_POTION = potion("strong_condensing", new Potion("wildercord_condensing", new MobEffectInstance(CONDENSING, 1800, 1)));

	/** The level (1 = I) of an effect on a player, 0 without it. */
	public static int level(net.minecraft.world.entity.LivingEntity mob, Holder<MobEffect> effect) {
		MobEffectInstance instance = mob.getEffect(effect);
		return instance == null ? 0 : instance.getAmplifier() + 1;
	}

	private static Holder<Potion> potion(String path, Potion potion) {
		return Registry.registerForHolder(BuiltInRegistries.POTION, ResourceKey.create(Registries.POTION, Wildercord.id(path)), potion);
	}

	public static void init() {}

	private static final class PlainEffect extends MobEffect {
		PlainEffect(int color) {
			super(MobEffectCategory.BENEFICIAL, color);
		}
	}

	private static final class ClarityEffect extends MobEffect {
		ClarityEffect() {
			super(MobEffectCategory.BENEFICIAL, 0xB8A8FF);
		}
	}

	/** Its work is done where spells land (SpellDefence); the effect only has to be there. The amber of a Shield's circles. */
	private static final class WardedEffect extends MobEffect {
		WardedEffect() {
			super(MobEffectCategory.BENEFICIAL, 0xF5B04A);
		}
	}

	private static final class ManaEffect extends InstantaneousMobEffect {
		ManaEffect() {
			super(MobEffectCategory.BENEFICIAL, 0x7C5CFF);
		}

		@Override
		public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
			restore(mob, amplification, 1.0);
			return true;
		}

		@Override
		public void applyInstantaneousEffect(ServerLevel level, Entity source, Entity owner, LivingEntity mob, int amplification, double scale) {
			restore(mob, amplification, scale);
		}

		private static void restore(LivingEntity mob, int amplification, double scale) {
			if (mob instanceof ServerPlayer player) {
				Mana.restore(player, (float) (scale * (60 << amplification)));
			}
		}
	}
}
