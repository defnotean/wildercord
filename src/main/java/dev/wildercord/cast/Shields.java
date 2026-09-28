package dev.wildercord.cast;

import dev.wildercord.content.ShieldOption;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.player.WildercordAttachments.SpellShield;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Shield: a one-time spell block. Raising it opens the spell's magic circle in front of whoever it
 * guards, which settles into a small circle circling them. When a harmful spell comes at them, the
 * circle spawns in full size between the spell and them, facing it. A spell that cost as much mana
 * as the one that raised the Shield, or less, strikes the circle and stops (the circle ripples and
 * fades, spent); a spell that cost more cracks it and shatters it like glass, and goes through to
 * strike whatever it was aimed at. Only spells: arrows and blades pass as if it weren't there.
 *
 * <p>The Shield is a synced attachment that every nearby client draws the little circle from; a
 * block or a shatter is sent as one {@link ShieldOption} particle that each client turns into the
 * whole effect.</p>
 */
public final class Shields {
	private Shields() {}

	/** The circles' colour: warm amber light. */
	public static final int COLOR = 0xF5B04A;
	/** How long a spell a shield stopped stays stopped at that creature: its other effects and later strikes. */
	private static final int BLOCK_MEMORY = 20 * 15;
	/** Everyone within this range sees a shield block or shatter (the wearer included). */
	private static final double RANGE = 96.0;

	private record Blocked(Object cast, long until) {}

	/** A flying spell closing in on a shielded creature, whose circle has already appeared in front of it. */
	private record Seen(Object cast, UUID target, long until) {}

	private static final List<Seen> SEEN = new ArrayList<>();
	/** How far from a creature a spell flying at it makes its circle appear. */
	private static final double APPROACH = 7.0;

	/** Where a flying spell meets a Shield's circle: the creature it guards, and the point. */
	public record Interception(LivingEntity target, Vec3 at) {}

	/** Spells a shield has stopped, by creature: the rest of the same spell is stopped too. */
	private static final Map<UUID, List<Blocked>> BLOCKED = new HashMap<>();
	/** Creatures wearing a shield, so each ends when its time is up. */
	private static final Set<LivingEntity> WEARING = Collections.newSetFromMap(new WeakHashMap<>());

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Shields::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BLOCKED.clear();
			WEARING.clear();
			SEEN.clear();
		});
	}

	/** Raises a shield on {@code t} for {@code ticks}; renewing one keeps the stronger of the two. */
	static void raise(Cast cast, LivingEntity t, int ticks) {
		long now = cast.level.getGameTime();
		float strength = (float) cast.weight();
		SpellShield old = t.getAttached(WildercordAttachments.SPELL_SHIELD);
		if (old != null && old.until() > now) {
			strength = Math.max(strength, old.strength());
		}
		List<String> runes = cast.info.spell().isEmpty() ? List.of(dev.wildercord.spell.Runes.SHIELD.id())
			: cast.info.spell().stream().map(dev.wildercord.spell.RuneDef::id).toList();
		give(t, strength, ticks, runes);
		raised(cast.level, t, runes);
		Casters.tell(t, net.minecraft.network.chat.Component.translatable("message.wildercord.shield_up", Math.round(strength)).withColor(COLOR));
	}

	/**
	 * Puts a Shield of {@code strength} on {@code t} for {@code ticks}, its circles written with
	 * {@code runes}, with no flourish (commands and tests; a cast goes through {@link #raise}).
	 */
	public static void give(LivingEntity t, float strength, int ticks, List<String> runes) {
		t.setAttached(WildercordAttachments.SPELL_SHIELD, new SpellShield(strength, t.level().getGameTime() + ticks, COLOR, runes));
		WEARING.add(t);
	}

	/** The mana a spell must cost to break {@code t}'s shield, or 0 if it has none. */
	public static float strength(LivingEntity t) {
		SpellShield shield = t.getAttached(WildercordAttachments.SPELL_SHIELD);
		return shield == null || shield.until() < t.level().getGameTime() ? 0 : shield.strength();
	}

	/** The harmful touches of a cast that {@code targets}' shields let through (see {@link #stops}). */
	static List<LivingEntity> screen(Cast cast, List<LivingEntity> targets, Cast.Hit hit) {
		List<LivingEntity> through = null;
		for (int i = 0; i < targets.size(); i++) {
			LivingEntity t = targets.get(i);
			Vec3 c = t.getBoundingBox().getCenter();
			// Where it came from: the point it struck, unless that's the creature itself (then the caster).
			Vec3 from = hit.point().distanceToSqr(c) > 0.36 ? hit.point() : cast.caster.getEyePosition();
			if (stops(cast, t, from)) {
				if (through == null) {
					through = new ArrayList<>(targets.subList(0, i));
				}
			} else if (through != null) {
				through.add(t);
			}
		}
		return through == null ? targets : through;
	}

	/**
	 * Whether a harmful spell stops at {@code target}'s shield. The first touch of a cast decides:
	 * weighing no more than the shield, it's blocked (and so is the rest of that spell, there); weighing
	 * more, the shield shatters and the spell goes through. Either way the shield is spent.
	 *
	 * @param from where the spell comes from, for where it strikes the shell
	 */
	static boolean stops(Cast cast, LivingEntity target, Vec3 from) {
		if (target == cast.caster) {
			return false;
		}
		long now = cast.level.getGameTime();
		List<Blocked> blocked = BLOCKED.get(target.getUUID());
		if (blocked != null) {
			for (Blocked b : blocked) {
				if (b.cast() == cast.identity() && b.until() >= now) {
					return true;
				}
			}
		}
		SpellShield shield = target.getAttached(WildercordAttachments.SPELL_SHIELD);
		if (shield == null) {
			return false;
		}
		target.removeAttached(WildercordAttachments.SPELL_SHIELD);
		WEARING.remove(target);
		if (shield.until() < now) {
			return false;
		}
		Vec3 dir = impact(target, from);
		if (cast.weight() > shield.strength() + 1e-6) {
			shatter(cast.level, target, dir, shield);
			if (cast.caster instanceof ServerPlayer player) {
				Grimoire.feat(player, dev.wildercord.spell.Feats.SHIELDBREAKER);
			}
			return false;
		}
		BLOCKED.computeIfAbsent(target.getUUID(), k -> new ArrayList<>()).add(new Blocked(cast.identity(), now + BLOCK_MEMORY));
		block(cast.level, target, dir, shield, punched(shield.strength(), cast.weight()));
		if (target instanceof ServerPlayer wearer) {
			Grimoire.feat(wearer, dev.wildercord.spell.Feats.SPELLGUARD);
		}
		return true;
	}

	/**
	 * For a spell flying from {@code from} to {@code to} this tick: the Shield it reaches, if any. A
	 * Shield stands its circle about a block out from the creature it guards, so a spell on course to
	 * hit them strikes the circle first (and stops there, or breaks it and goes on). One still closing
	 * in, on course, makes the circle appear in front of it. A spell that would miss passes by.
	 */
	public static Interception intercept(Cast cast, Vec3 from, Vec3 to) {
		if (WEARING.isEmpty()) {
			return null;
		}
		Vec3 motion = to.subtract(from);
		double length = motion.length();
		if (length < 1.0E-4) {
			return null;
		}
		Vec3 dir = motion.scale(1 / length);
		long now = cast.level.getGameTime();
		Interception found = null;
		double nearest = Double.MAX_VALUE;
		for (LivingEntity t : WEARING) {
			if (t.level() != cast.level || t == cast.caster || !t.isAlive() || !Targets.canHarm(cast.caster, t)) {
				continue;
			}
			SpellShield shield = t.getAttached(WildercordAttachments.SPELL_SHIELD);
			if (shield == null || shield.until() < now) {
				continue;
			}
			Vec3 c = t.getBoundingBox().getCenter();
			double off = front(t, shield.strength());
			double distance = from.distanceTo(c);
			if (distance > APPROACH + off + length) {
				continue;
			}
			// On course: the spell's line runs into the creature itself.
			net.minecraft.world.phys.AABB body = t.getBoundingBox().inflate(0.15);
			if (!body.contains(from) && body.clip(from, from.add(dir.scale(distance + 2))).isEmpty()) {
				continue;
			}
			if (distance <= APPROACH + off) {
				appear(cast, t, from.subtract(c), shield);
			}
			// Does this step reach the circle (a sphere of radius `off` round the creature, where the circle stands)?
			double along = c.subtract(from).dot(dir);
			double miss = c.subtract(from.add(dir.scale(along))).lengthSqr();
			if (miss > off * off) {
				continue;
			}
			double enter = along - Math.sqrt(off * off - miss);
			if (distance <= off) {
				enter = 0;
			}
			if (enter <= length && enter >= -0.01 && distance < nearest) {
				nearest = distance;
				found = new Interception(t, from.add(dir.scale(Math.max(0, enter))));
			}
		}
		return found;
	}

	/** A spell is closing in: the circle spawns in, in front of it (once per spell and creature). */
	private static void appear(Cast cast, LivingEntity t, Vec3 toward, SpellShield shield) {
		long now = cast.level.getGameTime();
		for (Seen seen : SEEN) {
			if (seen.cast() == cast.identity() && seen.target().equals(t.getUUID()) && seen.until() >= now) {
				return;
			}
		}
		SEEN.add(new Seen(cast.identity(), t.getUUID(), now + 40));
		Vec3 dir = toward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : toward.normalize();
		send(cast.level, option(ShieldOption.APPEAR, t, dir, shield, 0), t.getBoundingBox().getCenter());
		Fx.sound(cast.level, circleAt(t, dir, shield), WildercordSounds.CIRCLE_OPEN, 0.7F, 1.0F);
	}

	/** Whether a Shield stopped this cast at {@code target} (so a piercing bolt stops too). */
	public static boolean blocked(Cast cast, LivingEntity target) {
		List<Blocked> blocked = BLOCKED.get(target.getUUID());
		return blocked != null && blocked.stream().anyMatch(b -> b.cast() == cast.identity());
	}

	/** From the creature's centre toward where the spell came from. */
	private static Vec3 impact(LivingEntity t, Vec3 from) {
		Vec3 d = from == null ? Vec3.ZERO : from.subtract(t.getBoundingBox().getCenter());
		if (d.lengthSqr() < 1.0E-4) {
			double a = t.getRandom().nextDouble() * Math.PI * 2;
			d = new Vec3(Math.cos(a), 0.3, Math.sin(a));
		}
		return d.normalize();
	}

	/** Mana of a Shield's strength for each circle in its stack. */
	public static final float MANA_PER_LAYER = 8;

	/** How many circles a Shield of this strength stacks, one behind another: one for every 8 mana, 1 to 7. */
	public static int layers(float strength) {
		return Math.max(1, Math.min(ShieldOption.MAX_LAYERS, (int) Math.ceil(strength / MANA_PER_LAYER - 1.0E-4)));
	}

	/**
	 * How many of the stack's circles, from the front, a spell that's stopped still shatters: as many
	 * as its mana would pay for, a circle's worth at a time (never all of them: then it isn't stopped).
	 */
	public static int punched(float strength, double weight) {
		int layers = layers(strength);
		return Math.max(0, Math.min(layers - 1, (int) Math.floor(weight / (strength / layers) + 1.0E-6)));
	}

	/** How far out from a creature's centre its Shield's back circle stands, facing the spell. */
	public static float offset(LivingEntity t) {
		return t.getBbWidth() * 0.5F + 0.6F;
	}

	/** How far out its front circle stands: the one a spell meets first. */
	public static float front(LivingEntity t, float strength) {
		return offset(t) + ShieldOption.SPACING * (layers(strength) - 1);
	}

	/** The circle's radius: enough to cover a creature of this size. */
	public static float radius(LivingEntity t) {
		return Math.max(0.8F, Math.min(2.5F, Math.max(t.getBbHeight(), t.getBbWidth()) * 0.55F));
	}

	/**
	 * Raised: its circles open in front of them, the way they face, one behind another (as many as its
	 * strength stacks), then fade. From then on it's invisible, until a spell comes.
	 */
	private static void raised(ServerLevel level, LivingEntity t, List<String> runes) {
		Vec3 look = t.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		Vec3 facing = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
		List<dev.wildercord.spell.RuneDef> defs = runes.stream().map(dev.wildercord.spell.Runes::get).flatMap(java.util.Optional::stream).toList();
		int layers = layers(strength(t));
		for (int i = 0; i < layers; i++) {
			Vec3 at = t.getBoundingBox().getCenter().add(facing.scale(offset(t) + ShieldOption.SPACING * i));
			int delay = i;
			Scheduler.later(1 + delay, () -> {
				if (t.isAlive()) {
					Sigils.spell(level, at, facing, defs, COLOR, radius(t) * 0.9F, 22 - delay);
				}
			});
		}
		Sigils.flash(level, t.getBoundingBox().getCenter().add(facing.scale(front(t, strength(t)))), COLOR, 1.4F);
		Fx.sound(level, t.position(), WildercordSounds.SHIELD_UP, 0.8F, 1.0F);
	}

	/** Where the front circle stands when it meets a spell from {@code dir}. */
	private static Vec3 circleAt(LivingEntity t, Vec3 dir, SpellShield shield) {
		return t.getBoundingBox().getCenter().add(dir.scale(front(t, shield.strength())));
	}

	private static ShieldOption option(int kind, LivingEntity t, Vec3 dir, SpellShield shield, int broken) {
		return new ShieldOption(kind, t.getId(), 0xFF000000 | shield.color(), (float) dir.x, (float) dir.y, (float) dir.z, offset(t), radius(t),
			shield.runes(), layers(shield.strength()), broken);
	}

	/**
	 * A spell strikes the stack and stops: it shatters as many circles from the front as its mana pays
	 * for, then the next one holds (it flashes and ripples, sparks glance off), and the Shield is spent.
	 */
	private static void block(ServerLevel level, LivingEntity t, Vec3 dir, SpellShield shield, int broken) {
		Vec3 c = t.getBoundingBox().getCenter();
		Vec3 at = circleAt(t, dir, shield);
		send(level, option(ShieldOption.BLOCK, t, dir, shield, broken), c);
		cascade(level, at, broken);
		Sigils.flash(level, at, 0xFFFFFF, 1.1F);
		Fx.send(level, ParticleTypes.ELECTRIC_SPARK, at, 10, 0.15, 0.25);
		Fx.send(level, ParticleTypes.ENCHANTED_HIT, at, 8, 0.2, 0.3);
		Fx.sound(level, at, WildercordSounds.SHIELD_BLOCK, 1.0F, 1.0F);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_HIT, 0.9F, 1.4F);
		Casters.tell(t, net.minecraft.network.chat.Component.translatable("message.wildercord.shield_blocked").withColor(shield.color()));
	}

	/** A stronger spell shatters every circle in the stack, front to back, like glass; it goes on through. */
	private static void shatter(ServerLevel level, LivingEntity t, Vec3 dir, SpellShield shield) {
		Vec3 c = t.getBoundingBox().getCenter();
		Vec3 at = circleAt(t, dir, shield);
		int layers = layers(shield.strength());
		send(level, option(ShieldOption.BREAK, t, dir, shield, layers), c);
		cascade(level, at, layers - 1);
		Sigils.flash(level, at, 0xFFFFFF, 1.6F);
		Fx.sound(level, c, WildercordSounds.SHIELD_BREAK, 1.1F, 1.0F);
		Fx.sound(level, c, SoundEvents.GLASS_BREAK, 1.0F, 0.85F);
		Fx.sound(level, c, SoundEvents.GLASS_BREAK, 0.7F, 1.2F);
		if (t instanceof ServerPlayer player) {
			ScreenFx.punch(player, 0.6F);
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.shield_shattered").withColor(0xFF8A6A));
		}
	}

	/** Each circle a spell breaks through, after the first, rings as it goes: a quick run of breaking glass. */
	private static void cascade(ServerLevel level, Vec3 at, int more) {
		for (int i = 1; i <= more; i++) {
			float pitch = 0.9F + 0.1F * i;
			Scheduler.later(2 * i, () -> Fx.sound(level, at, SoundEvents.GLASS_BREAK, 0.7F, pitch));
		}
	}

	/** To everyone nearby, the wearer included, however close: the circles are around them. */
	private static void send(ServerLevel level, ShieldOption option, Vec3 at) {
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(at) <= RANGE * RANGE) {
				level.sendParticles(player, option, true, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
		}
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) {
			return;
		}
		if (!WEARING.isEmpty()) {
			for (Iterator<LivingEntity> it = WEARING.iterator(); it.hasNext(); ) {
				LivingEntity t = it.next();
				SpellShield shield = t.getAttached(WildercordAttachments.SPELL_SHIELD);
				if (t.isRemoved() || shield == null) {
					it.remove();
				} else if (!t.isAlive() || shield.until() < t.level().getGameTime()) {
					// Its time is up: it fades away (each client fades it as the attachment goes).
					t.removeAttached(WildercordAttachments.SPELL_SHIELD);
					it.remove();
				}
			}
		}
		if (!BLOCKED.isEmpty() && server.getTickCount() % 40 == 0) {
			long now = server.overworld().getGameTime();
			BLOCKED.values().removeIf(list -> {
				list.removeIf(b -> b.until() < now || b.until() - now > BLOCK_MEMORY);
				return list.isEmpty();
			});
			SEEN.removeIf(s -> s.until() < now || s.until() - now > 40);
		}
	}
}
