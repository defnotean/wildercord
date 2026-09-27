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
 * Mana potions. Clarity speeds up mana regeneration for a while; Mana restores it at once.
 * Both brew from an Awkward Potion (amethyst shard for Clarity, lapis lazuli for Mana).
 */
public final class WildercordEffects {
	private WildercordEffects() {}

	/** +50% mana regeneration per level. */
	public static final Holder<MobEffect> CLARITY = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("clarity"),
		new ClarityEffect());

	/** Instantly restores 60 mana, doubled per level. */
	public static final Holder<MobEffect> MANA = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Wildercord.id("mana"),
		new ManaEffect());

	public static final Holder<Potion> CLARITY_POTION = potion("clarity", new Potion("wildercord_clarity", new MobEffectInstance(CLARITY, 3600)));
	public static final Holder<Potion> LONG_CLARITY_POTION = potion("long_clarity", new Potion("wildercord_clarity", new MobEffectInstance(CLARITY, 9600)));
	public static final Holder<Potion> STRONG_CLARITY_POTION = potion("strong_clarity", new Potion("wildercord_clarity", new MobEffectInstance(CLARITY, 1800, 1)));
	public static final Holder<Potion> MANA_POTION = potion("mana", new Potion("wildercord_mana", new MobEffectInstance(MANA, 1)));
	public static final Holder<Potion> STRONG_MANA_POTION = potion("strong_mana", new Potion("wildercord_mana", new MobEffectInstance(MANA, 1, 1)));

	private static Holder<Potion> potion(String path, Potion potion) {
		return Registry.registerForHolder(BuiltInRegistries.POTION, ResourceKey.create(Registries.POTION, Wildercord.id(path)), potion);
	}

	public static void init() {}

	private static final class ClarityEffect extends MobEffect {
		ClarityEffect() {
			super(MobEffectCategory.BENEFICIAL, 0xB8A8FF);
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
