package dev.wildercord.aura.arts;

import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.MethodsBArtRules;
import dev.wildercord.cast.Motes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * What Echo, Dawn and Venom's arts do to a foe, shared by their arts and their coated blows: a foe left reeling (nausea for a player,
 * a mob's footing and aim thrown), lit up, blinded (a player's sight, a mob's hold on its target), a toxin stacked, weakened. A
 * player is held to the arts' caps ({@link ArtRules#PVP_HOLD_TICKS} for anything that takes their sight or footing); a boss is only
 * lit and poisoned.
 */
public final class MethodsBKit {
	private MethodsBKit() {}

	/** Echo's silver and violet, Dawn's white gold and rose, Venom's acid green and black. */
	public static final int SILVER = 0xE4E0F4;
	public static final int VIOLET = 0x9A7CE8;
	public static final int GOLD = 0xFFE7A0;
	public static final int ROSE = 0xF6A6BC;
	public static final int ACID = 0x8EE03C;
	public static final int BLACK = 0x161C12;

	/** Whether {@code foe} is undead (Dawn's light burns it hardest; a player never is). */
	public static boolean undead(LivingEntity foe) {
		return !(foe instanceof Player) && foe.is(EntityTypeTags.UNDEAD);
	}

	/** Echo: {@code foe} reels a while: a player's view swims, a mob stumbles and loses its aim. */
	public static void reel(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!ArtKit.harmable(player, foe) || !foe.isAlive() || ticks <= 0 || ArtKit.boss(foe)) return;
		if (foe instanceof Player) {
			foe.addEffect(new MobEffectInstance(MobEffects.NAUSEA, Math.max(20, ArtRules.hold(ticks, true, false) * 2), 0, false, true), player);
			return;
		}
		foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 0, false, true), player);
		foe.setYRot(foe.getYRot() + (foe.getRandom().nextBoolean() ? 35 : -35));
		foe.setYHeadRot(foe.getYRot());
	}

	/** Dawn: {@code foe} lit up for {@code ticks}, seen through walls by everyone. */
	public static void glow(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!ArtKit.harmable(player, foe) || !foe.isAlive() || ticks <= 0) return;
		foe.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false), player);
	}

	/** Dawn: {@code foe} blinded: a player's sight briefly, a mob's hold on its target lost. Returns whether it took. */
	public static boolean blind(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!ArtKit.harmable(player, foe) || !foe.isAlive() || ticks <= 0 || ArtKit.boss(foe)) return false;
		if (foe instanceof Player) {
			int t = ArtRules.hold(ticks, true, false);
			if (t <= 0) return false;
			foe.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, t + 10, 0, false, true), player);
			return true;
		}
		foe.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, true), player);
		if (foe instanceof Mob mob) {
			mob.setTarget(null);
			mob.getNavigation().stop();
		}
		return true;
	}

	/** Venom: a toxin stack on {@code foe} for {@code ticks}, one higher than it had (to the cap). Returns the stack (-1 for none). */
	public static int toxin(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!ArtKit.harmable(player, foe) || !foe.isAlive() || ticks <= 0) return -1;
		MobEffectInstance had = foe.getEffect(MobEffects.POISON);
		int amp = MethodsBArtRules.toxin(had == null ? -1 : had.getAmplifier(), had != null, foe instanceof Player);
		foe.addEffect(new MobEffectInstance(MobEffects.POISON, Math.max(ticks, had == null ? 0 : had.getDuration()), amp, false, true), player);
		ServerLevel level = player.level();
		Motes.glows(level, foe.getBoundingBox().getCenter(), 3 + 2 * amp, 0.3, ACID, 0.08, 18, new Vec3(0, -0.02, 0), 0.02);
		return amp;
	}

	/** Venom: {@code foe}'s blows weakened a while. */
	public static void weaken(ServerPlayer player, LivingEntity foe, int ticks) {
		if (!ArtKit.harmable(player, foe) || !foe.isAlive() || ticks <= 0) return;
		foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true), player);
	}
}
