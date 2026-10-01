package dev.wildercord.monster;

import dev.wildercord.cast.Dungeons;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.SpellDefence;
import dev.wildercord.cast.Vfx;
import dev.wildercord.content.WildercordSounds;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * What the monsters' magic shares. Their spells land through {@link SpellDefence} like any other (so a player's armour,
 * Warding, the Potion of Warding and the spellguard all answer them, and the game scales them by difficulty as it does any
 * monster's harm); their vines hold like the Root rune; and what spell is landing on one of them right now can be asked.
 */
public final class MonsterMagic {
	private MonsterMagic() {}

	/**
	 * A monster's spell hitting a creature: magic, from the monster, through the spell defences. Returns whether it hurt.
	 */
	public static boolean hurt(ServerLevel level, Mob caster, LivingEntity target, float amount) {
		return SpellDefence.hurt(level, target, level.damageSources().indirectMagic(caster, caster), amount);
	}

	/**
	 * Vines hold {@code target} where it stands for {@code ticks}, as the Root rune does: it can turn and strike back, but
	 * not walk away. Roots twist up round its legs while they last.
	 */
	public static void root(ServerLevel level, LivingEntity target, int ticks) {
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 6, false, false, true));
		target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
		sync(target);
		Vfx.root(level, target, ticks);
	}

	/**
	 * Throws {@code target} along {@code impulse} (less for a creature that resists knockback; a player feels all of it, as
	 * spells push them).
	 */
	public static void shove(LivingEntity target, Vec3 impulse) {
		double resist = target instanceof net.minecraft.world.entity.player.Player ? 0 : target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		target.setDeltaMovement(target.getDeltaMovement().add(impulse.scale(Math.max(0, 1 - resist))));
		sync(target);
	}

	/** Knocks {@code target} back away from {@code from}, {@code power} hard, with a little lift. */
	public static void knock(LivingEntity target, Vec3 from, double power) {
		Vec3 away = target.position().subtract(from).multiply(1, 0, 1);
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
		shove(target, new Vec3(away.x * power, 0.3 + power * 0.1, away.z * power));
	}

	/** Tells everyone (the creature's own client above all, if it's a player) that its motion changed. */
	public static void sync(LivingEntity target) {
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	/** Whether {@code target} is held by vines right now (the slow only a root gives). */
	public static boolean rooted(LivingEntity target) {
		MobEffectInstance slow = target.getEffect(MobEffects.SLOWNESS);
		return slow != null && slow.getAmplifier() >= 6;
	}

	/** Whether the damage being dealt right now is a spell's (not a blade's, an arrow's, a fall's or the fire under it). */
	public static boolean spellLanding() {
		return Dungeons.spellLanding();
	}

	/** The element of the spell effect landing right now, or empty outside one. */
	public static String spellElement() {
		return Effects.currentElementNow();
	}

	/** A sound from the feel kit (tools/feel/monster.py) at a point, as hostile creatures' sounds are. */
	public static void sound(ServerLevel level, Vec3 at, String kit, float volume, float pitch) {
		SoundEvent event = WildercordSounds.kit(kit);
		if (event != null) {
			level.playSound(null, at.x, at.y, at.z, event, SoundSource.HOSTILE, volume, pitch * (0.97F + level.getRandom().nextFloat() * 0.06F));
		}
	}

	/** A kit sound for one of a monster's own noises (ambient, hurt, death), or null while the kit lacks it. */
	public static SoundEvent kit(String name) {
		return WildercordSounds.kit(name);
	}

	/** A short line above the hotbar for a player, to explain what just happened (at most once in a while: the caller decides). */
	public static void tell(LivingEntity who, net.minecraft.network.chat.Component line) {
		if (who instanceof ServerPlayer player) {
			player.sendOverlayMessage(line);
		}
	}
}
