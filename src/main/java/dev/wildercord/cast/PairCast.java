package dev.wildercord.cast;

import dev.wildercord.spell.PairRunes;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * One pair fusion landing (see {@link PairRunes}): everything a hand-made pair effect in {@code dev.wildercord.pairs}
 * needs, so each is its own few lines of what it does and how it looks, not plumbing. Each pair's code decides
 * everything itself; this only keeps every one inside the same rules:
 * <ul>
 *   <li>Damage goes through the cast ({@link #hurt} and its kin), so Shields, the damage allowance, parties,
 *   Execute and Thirst all apply.</li>
 *   <li>Every creature reached beyond what the shape hit ({@link #enemiesNear}) comes out of the cast's creature
 *   budget, at most {@link #MAX_IN_AREA} at a time.</li>
 *   <li>Bosses are never moved, held or lifted ({@link #movable}), only struck.</li>
 *   <li>Anything later ({@link #later}, {@link #every}) stops when the cast ends, and keeps its Execute and element.</li>
 * </ul>
 * The look is each pair's own: particles in shapes ({@link #ring}, {@link #spiral}, {@link #line}, {@link #sphere},
 * {@link #column}, {@link #arc}, {@link #helix}), played out over time with {@link #every}, with colours
 * ({@link #dust}, {@link #shift}), sounds, camera shake and screen tint.
 */
public final class PairCast {
	/** At most this many enemies one call of {@link #enemiesNear} returns. */
	public static final int MAX_IN_AREA = 16;
	/** At most this many targets get a lingering part of their own. */
	public static final int MAX_TARGETS = 8;

	public final Cast cast;
	public final ServerLevel level;
	public final LivingEntity caster;
	public final SpellPlan.EffectNode node;
	public final Cast.Hit hit;
	/** The rune's power: every number of damage, healing and shielding is multiplied by it. */
	public final double power;
	/** Its duration factor: {@link #ticks} and every status here already use it. */
	public final double duration;
	/** Its radius factor (Widen and the like): multiply every area's size by it. */
	public final double radius;
	public final int amplify;
	private final List<LivingEntity> helped;
	private final List<LivingEntity> harmed;
	private final Set<Entity> touched;

	PairCast(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		this.cast = cast;
		this.level = cast.level;
		this.caster = cast.caster;
		this.node = node;
		this.hit = hit;
		this.power = power;
		this.duration = duration;
		this.radius = SpellNumbers.effectRadius(node);
		this.amplify = amplify;
		this.helped = helped;
		this.harmed = harmed;
		this.touched = new HashSet<>(hit.entities());
	}

	/** Runs the pair rune {@code node} holds, if it's one that's been written; says whether it was a pair at all. */
	static boolean run(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		RuneDef rune = node.effect;
		if (!PairRunes.isPair(rune)) {
			return false;
		}
		List<RuneDef> parts = PairRunes.parts(rune);
		if (parts.size() != 2) {
			return true;
		}
		// A soul pair works only for the caster whose heart owns its innate rune.
		if (cast.caster instanceof ServerPlayer owner && !owner.isCreative()) {
			for (RuneDef part : parts) {
				if (Runes.innate(part) && !dev.wildercord.player.Heart.innate(owner).equals(part.id())) {
					return true;
				}
			}
		}
		dev.wildercord.pairs.PairIndex.cast(parts.get(0).path(), parts.get(1).path(),
			new PairCast(cast, node, hit, helped, harmed, power, duration, amplify));
		return true;
	}

	// ------------------------------------------------------------------ who

	/** The enemies the shape hit (their Shields already passed), up to {@link #MAX_IN_AREA}. */
	public List<LivingEntity> enemies() {
		return harmed.size() <= MAX_IN_AREA ? harmed : harmed.subList(0, MAX_IN_AREA);
	}

	/** The allies the shape reached (the caster too, on a self cast). */
	public List<LivingEntity> allies() {
		return helped;
	}

	/** The first enemy the shape hit, or null. */
	public LivingEntity firstEnemy() {
		return harmed.isEmpty() ? null : harmed.getFirst();
	}

	/** The first ally the shape reached, or null. */
	public LivingEntity firstAlly() {
		return helped.isEmpty() ? null : helped.getFirst();
	}

	/** At most {@code n} of {@code targets}. */
	public static List<LivingEntity> first(List<LivingEntity> targets, int n) {
		return targets.size() <= n ? targets : targets.subList(0, n);
	}

	/** Where the spell landed. */
	public Vec3 point() {
		return hit.point();
	}

	/** Where the spell came from. */
	public Vec3 origin() {
		return hit.origin();
	}

	/** Which way it was going (a unit vector, never zero). */
	public Vec3 dir() {
		Vec3 d = hit.dir();
		return d.lengthSqr() < 1.0E-4 ? caster.getLookAngle() : d.normalize();
	}

	/** Whether it was cast on the caster themself. */
	public boolean self() {
		return hit.self();
	}

	/**
	 * The caster's enemies within {@code r} blocks of {@code at}, nearest first: those the shape already hit always,
	 * others only while the cast's creature budget lasts (each counted once however often it comes back to them).
	 */
	public List<LivingEntity> enemiesNear(Vec3 at, double r) {
		return SignatureFusions.take(cast, SignatureFusions.nearest(cast, at, r, MAX_IN_AREA), touched);
	}

	/** The caster's allies within {@code r} blocks of {@code at} (the caster included), nearest first. */
	public List<LivingEntity> alliesNear(Vec3 at, double r) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(at, at).inflate(r + 1),
				e -> e instanceof LivingEntity && Targets.canHelp(caster, e))) {
			if (e.getBoundingBox().getCenter().distanceTo(at) <= r + e.getBbWidth() / 2) {
				out.add((LivingEntity) e);
			}
		}
		out.sort(java.util.Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(at)));
		return out.size() <= MAX_IN_AREA ? out : new ArrayList<>(out.subList(0, MAX_IN_AREA));
	}

	/** The enemy nearest {@code at} within {@code r}, other than {@code not}, from the budget as {@link #enemiesNear}; or null. */
	public LivingEntity nearestEnemy(Vec3 at, double r, LivingEntity not) {
		for (LivingEntity t : SignatureFusions.nearest(cast, at, r, MAX_IN_AREA)) {
			if (t != not && t.isAlive()) {
				List<LivingEntity> granted = SignatureFusions.take(cast, List.of(t), touched);
				return granted.isEmpty() ? null : t;
			}
		}
		return null;
	}

	/** Whether {@code t} is still there to be touched: alive, in this world, and the cast still going. */
	public boolean here(LivingEntity t) {
		return cast.alive() && SignatureFusions.onHand(cast, t);
	}

	/** Whether {@code t} may be moved, held or lifted: never a boss. */
	public boolean movable(LivingEntity t) {
		return !Spirits.isBoss(t);
	}

	/** The middle of {@code e}'s body. */
	public static Vec3 mid(Entity e) {
		return e.getBoundingBox().getCenter();
	}

	// ------------------------------------------------------------------ harm

	/** Magic damage (through armour), {@code amount} as written: multiply by {@link #power} yourself. */
	public void hurt(LivingEntity t, double amount) {
		Effects.hurt(cast, t, SignatureFusions.magic(cast), amount);
	}

	/** Fire damage, made stronger or weaker by what the target is (wet, burning...). */
	public void burn(LivingEntity t, double amount) {
		Effects.hurt(cast, t, level.damageSources().source(DamageTypes.IN_FIRE, caster), amount * Reactions.fire(cast, t));
	}

	/** Cold damage. */
	public void freeze(LivingEntity t, double amount) {
		Effects.hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), amount);
	}

	/** Lightning damage. */
	public void shock(LivingEntity t, double amount) {
		Effects.hurt(cast, t, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), amount);
	}

	/** Withering damage (void and blood rot). */
	public void wither(LivingEntity t, double amount) {
		Effects.hurt(cast, t, level.damageSources().source(DamageTypes.WITHER, caster), amount);
	}

	/** A blow of stone or wind: ordinary damage armour can soften. */
	public void strike(LivingEntity t, double amount) {
		Effects.hurt(cast, t, level.damageSources().mobAttack(caster), amount);
	}

	/** Runs {@code task} as damage over time (a Shield can block it but not parry it): for every tick after the first. */
	public void lingering(Runnable task) {
		Effects.lingering(task);
	}

	/** A blast at {@code at} of {@code r} blocks and {@code blastPower} (never breaks blocks; enemies only). */
	public void explode(Vec3 at, double r, double blastPower) {
		Effects.explode(cast, at, r, blastPower);
	}

	// ------------------------------------------------------------------ statuses

	/** {@code seconds} as ticks, stretched by the rune's duration (Extend). */
	public int ticks(double seconds) {
		return Effects.ticks(seconds, duration);
	}

	/** A status effect for {@code seconds} (stretched by duration) at {@code amplifier} (0 is level I). */
	public void effect(LivingEntity t, Holder<MobEffect> effect, double seconds, int amplifier) {
		t.addEffect(new MobEffectInstance(effect, ticks(seconds), amplifier, false, true), caster);
	}

	public void ignite(LivingEntity t, double seconds) {
		t.igniteForTicks(ticks(seconds));
	}

	/** Frost creeping on the skin for {@code seconds}, well short of frozen solid. */
	public void chill(LivingEntity t, double seconds) {
		int add = ticks(seconds);
		t.setTicksFrozen(Math.max(t.getTicksFrozen(), Math.min(t.getTicksRequiredToFreeze() - 1, t.getTicksFrozen() + add)));
	}

	/** Leaves a reaction mark on {@code t} (frozen, soaked, bleeding...) that other elements answer. */
	public void mark(LivingEntity t, Reactions.Mark mark) {
		Reactions.mark(t, mark);
	}

	/** Heals an ally {@code amount} (as written: multiply by {@link #power} yourself). */
	public void heal(LivingEntity t, double amount) {
		t.heal((float) amount);
	}

	/** Absorption hearts: raises {@code t}'s absorption to {@code amount} if it's lower, for {@code seconds}. */
	public void absorb(LivingEntity t, double amount, double seconds) {
		t.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, ticks(seconds), 0, false, true));
		t.setAbsorptionAmount((float) Math.max(t.getAbsorptionAmount(), amount));
	}

	/** Puts out a fire on an ally. */
	public void douse(LivingEntity t) {
		t.clearFire();
	}

	// ------------------------------------------------------------------ movement

	/** Pushes {@code t} by {@code impulse} (nothing on a boss or anything anchored). */
	public void push(LivingEntity t, Vec3 impulse) {
		if (movable(t)) {
			Effects.push(t, impulse);
		}
	}

	/** Knocks {@code t} away from {@code from}: {@code strength} sideways and {@code up} upward. */
	public void knockFrom(LivingEntity t, Vec3 from, double strength, double up) {
		Vec3 away = Effects.horizontal(t.position().subtract(from), dir());
		push(t, away.scale(strength).add(0, up, 0));
	}

	/** Draws {@code t} towards {@code to}, harder the further it is ({@code strength} at most). */
	public void pullTo(LivingEntity t, Vec3 to, double strength) {
		Vec3 towards = to.subtract(t.position());
		double distance = towards.length();
		if (distance > 0.3) {
			push(t, towards.normalize().scale(Math.min(strength, 0.4 + distance * 0.2)).add(0, 0.25, 0));
		}
	}

	/** Lifts {@code t} straight up. */
	public void lift(LivingEntity t, double up) {
		push(t, new Vec3(0, up, 0));
	}

	/** Sets {@code e} down at {@code to} if it fits there safely; says whether it did (never a boss). */
	public boolean blink(LivingEntity e, Vec3 to) {
		if (!movable(e)) {
			return false;
		}
		Vec3 spot = CastEngine.ground(level, to);
		if (!Effects.safeSpot(level, e, spot)) {
			return false;
		}
		e.teleportTo(level, spot.x, spot.y, spot.z, Set.<Relative>of(), e.getYRot(), e.getXRot(), false);
		e.resetFallDistance();
		return true;
	}

	/** The ground at or under {@code at}. */
	public Vec3 ground(Vec3 at) {
		return CastEngine.ground(level, at);
	}

	// ------------------------------------------------------------------ time

	/** Runs {@code task} in {@code ticks}, if the cast still lasts then. */
	public void later(int ticks, Runnable task) {
		Scheduler.later(ticks, () -> {
			if (cast.alive()) {
				task.run();
			}
		});
	}

	/**
	 * Runs {@code step} {@code times} times, {@code every} ticks apart (step 0 at once), while the cast lasts. Every
	 * step after the first is lingering (see {@link #lingering}). The heart of an animation: draw frame {@code i}.
	 */
	public void every(int every, int times, IntConsumer step) {
		if (times <= 0) {
			return;
		}
		step.accept(0);
		for (int i = 1; i < times; i++) {
			int at = i;
			later(every * i, () -> Effects.lingering(() -> step.accept(at)));
		}
	}

	/** True only the first time this cast asks with {@code key}. */
	public boolean once(String key) {
		return cast.once(key);
	}

	/** A random number from 0 to 1. */
	public double random() {
		return level.getRandom().nextDouble();
	}

	// ------------------------------------------------------------------ look and sound

	/** A dust particle of {@code rgb} colour (0xRRGGBB), {@code size} 0.5 to 2. */
	public static ParticleOptions dust(int rgb, float size) {
		return new DustParticleOptions(rgb, size);
	}

	/** A dust particle that fades from one colour to another as it lives. */
	public static ParticleOptions shift(int from, int to, float size) {
		return new DustColorTransitionOptions(from, to, size);
	}

	/** {@code count} particles at {@code at}, spread {@code spread} blocks each way, at {@code speed}. */
	public void particles(ParticleOptions p, Vec3 at, int count, double spread, double speed) {
		level.sendParticles(p, at.x, at.y, at.z, count, spread, spread, spread, speed);
	}

	/** One particle at {@code at} moving by {@code velocity} (for streaks and sparks with a direction). */
	public void mote(ParticleOptions p, Vec3 at, Vec3 velocity) {
		level.sendParticles(p, at.x, at.y, at.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
	}

	/** A flat ring of {@code points} particles, {@code r} round {@code at}, turned by {@code turn} radians. */
	public void ring(ParticleOptions p, Vec3 at, double r, int points, double turn) {
		for (int i = 0; i < points; i++) {
			double a = turn + Math.PI * 2 * i / points;
			particles(p, at.add(Math.cos(a) * r, 0, Math.sin(a) * r), 1, 0, 0);
		}
	}

	/** A ring whose particles fly outward (a shockwave): {@code speed} blocks a tick. */
	public void wave(ParticleOptions p, Vec3 at, int points, double speed) {
		for (int i = 0; i < points; i++) {
			double a = Math.PI * 2 * i / points;
			mote(p, at, new Vec3(Math.cos(a) * speed, 0, Math.sin(a) * speed));
		}
	}

	/** A straight line of particles from {@code a} to {@code b}, {@code perBlock} to a block. */
	public void line(ParticleOptions p, Vec3 a, Vec3 b, double perBlock) {
		Vec3 d = b.subtract(a);
		int n = Math.max(1, (int) Math.ceil(d.length() * perBlock));
		for (int i = 0; i <= n; i++) {
			particles(p, a.add(d.scale((double) i / n)), 1, 0, 0);
		}
	}

	/** A jagged line, like lightning, from {@code a} to {@code b}: {@code jag} blocks of wander at most. */
	public void zigzag(ParticleOptions p, Vec3 a, Vec3 b, double jag, double perBlock) {
		Vec3 d = b.subtract(a);
		int segments = Math.max(2, (int) Math.ceil(d.length() / 1.2));
		Vec3 last = a;
		for (int i = 1; i <= segments; i++) {
			Vec3 next = a.add(d.scale((double) i / segments));
			if (i < segments) {
				next = next.add((random() - 0.5) * 2 * jag, (random() - 0.5) * 2 * jag, (random() - 0.5) * 2 * jag);
			}
			line(p, last, next, perBlock);
			last = next;
		}
	}

	/** An arc from {@code a} to {@code b} bowing {@code height} blocks up in the middle. */
	public void arc(ParticleOptions p, Vec3 a, Vec3 b, double height, int points) {
		for (int i = 0; i <= points; i++) {
			double f = (double) i / points;
			Vec3 at = a.add(b.subtract(a).scale(f)).add(0, Math.sin(Math.PI * f) * height, 0);
			particles(p, at, 1, 0, 0);
		}
	}

	/** A rising spiral round {@code at}: {@code turns} turns over {@code height} blocks, {@code r} wide. */
	public void spiral(ParticleOptions p, Vec3 at, double r, double height, double turns, int points) {
		for (int i = 0; i < points; i++) {
			double f = (double) i / points;
			double a = Math.PI * 2 * turns * f;
			particles(p, at.add(Math.cos(a) * r, height * f, Math.sin(a) * r), 1, 0, 0);
		}
	}

	/** Two spirals wound round each other (a double helix), the second in {@code q}. */
	public void helix(ParticleOptions p, ParticleOptions q, Vec3 at, double r, double height, double turns, int points) {
		for (int i = 0; i < points; i++) {
			double f = (double) i / points;
			double a = Math.PI * 2 * turns * f;
			particles(p, at.add(Math.cos(a) * r, height * f, Math.sin(a) * r), 1, 0, 0);
			particles(q, at.add(Math.cos(a + Math.PI) * r, height * f, Math.sin(a + Math.PI) * r), 1, 0, 0);
		}
	}

	/** A shell of {@code points} particles evenly over a sphere of {@code r} round {@code at}. */
	public void sphere(ParticleOptions p, Vec3 at, double r, int points) {
		double golden = Math.PI * (3 - Math.sqrt(5));
		for (int i = 0; i < points; i++) {
			double y = 1 - 2 * (i + 0.5) / points;
			double rr = Math.sqrt(1 - y * y);
			double a = golden * i;
			particles(p, at.add(Math.cos(a) * rr * r, y * r, Math.sin(a) * rr * r), 1, 0, 0);
		}
	}

	/** A pillar of particles {@code height} tall, {@code r} wide, standing on {@code at}. */
	public void column(ParticleOptions p, Vec3 at, double r, double height, int points) {
		for (int i = 0; i < points; i++) {
			double a = random() * Math.PI * 2;
			double rr = Math.sqrt(random()) * r;
			particles(p, at.add(Math.cos(a) * rr, random() * height, Math.sin(a) * rr), 1, 0, 0);
		}
	}

	/** A filled disc on the ground ({@code points} scattered across it). */
	public void disc(ParticleOptions p, Vec3 at, double r, int points) {
		column(p, at, r, 0.1, points);
	}

	/** A star of {@code rays} straight rays out from {@code at}, {@code length} long, turned by {@code turn}. */
	public void star(ParticleOptions p, Vec3 at, int rays, double length, double turn) {
		for (int i = 0; i < rays; i++) {
			double a = turn + Math.PI * 2 * i / rays;
			line(p, at, at.add(Math.cos(a) * length, 0, Math.sin(a) * length), 3);
		}
	}

	/** A sound at {@code at} (players hear it as the caster's). */
	public void sound(SoundEvent sound, Vec3 at, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	public void sound(Holder<SoundEvent> sound, Vec3 at, float volume, float pitch) {
		sound(sound.value(), at, volume, pitch);
	}

	/** Shakes the camera of everyone within {@code r} of {@code at} ({@code strength} 0 to 1). */
	public void shake(Vec3 at, float strength, double r) {
		ScreenFx.shake(level, at, strength, r);
	}

	/** A sharp punch on the caster's own camera, for a heavy blow ({@code strength} 0 to 1). */
	public void punch(float strength) {
		ScreenFx.punch(caster, strength);
	}

	/** Tints the edges of the screen of every player within {@code r} of {@code at} in {@code rgb}, for {@code ticks}. */
	public void tint(Vec3 at, double r, int rgb, int ticks) {
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(at) <= r) {
				ScreenFx.tint(player, rgb, ticks);
			}
		}
	}

	/** A bolt of lightning that only looks and sounds like one (it neither hurts nor burns). */
	public void bolt(Vec3 at) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.snapTo(at.x, at.y, at.z);
			level.addFreshEntity(bolt);
		}
	}

	/** All of {@code targets} the cast may still touch, for a lingering part's next beat. */
	public List<LivingEntity> still(Collection<LivingEntity> targets) {
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity t : targets) {
			if (here(t)) {
				out.add(t);
			}
		}
		return out;
	}
}
