package dev.wildercord.monster;

import static dev.wildercord.monster.GiantRules.Bramble.*;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The Elder Bramblewalker's and the Ash Colossus's fight ({@link GiantRules.Bramble}). Each move has its own tell marked on
 * the ground and its own way out: the stomp's quake and the shockwave after it (jump the wave), the sweep of an arm across its
 * front (behind it or out past it), the hurled clod (out of the closing ring), the burrowing root (outrun it or turn it on a
 * corner), the grab (strike it to break its grip), the hammer down a lane (step out of it, then punish it while it's stuck),
 * the Elder's brambles and the Colossus's kindling. The Colossus's clods burn where they land, and roused it leaves burning
 * footprints behind it.
 */
final class BrambleGiantFight extends GiantFight {
	/** How long the stomp's shockwave rolls out (ticks). */
	private static final int WAVE = Mth.ceil((WAVE_FAR - STOMP_INNER) / WAVE_SPEED);
	private static final Vec3 UP = new Vec3(0, 1, 0), DOWN = new Vec3(0, -1, 0);

	private final boolean colossus;
	private final ElementFx.Palette tint;
	/** Its marks on the ground: where something will land, the brighter edge of it, and its brambles. */
	private final DustParticleOptions warn, bright, thorn;
	/** Which arm sweeps (1 swings right to left), and whether this is the move's second go roused. */
	private int side = 1;
	private boolean twice;
	/** Where the clod will land, and where it was thrown from. */
	private Vec3 aim = Vec3.ZERO, from = Vec3.ZERO;
	/** The hammer's lane: where it starts and the way it runs. */
	private Vec3 laneFrom = Vec3.ZERO, lane = new Vec3(0, 0, 1);
	/** Each burrowing root's head: where it is and the way it's going. */
	private final List<Vec3[]> roots = new ArrayList<>();
	/** Whoever it holds, and how much it has been hurt since it caught them. */
	private @Nullable UUID held;
	private float heldTaken;
	/** What it has left on the ground. */
	private final List<Patch> patches = new ArrayList<>();
	/** How long its fists are stuck this time. */
	private int stuckFor;
	/** Where its prey was last tick and how it has been moving (to lead a throw), and its last burning footprint. */
	private @Nullable Vec3 seen;
	private Vec3 drift = Vec3.ZERO, lastPrint = Vec3.ZERO;

	/** Something left on the ground: burning, or brambles waiting {@code fuse} ticks to burst up and then lingering. */
	private record Patch(Vec3 at, double radius, int born, int fuse, int until, boolean burn) {}

	BrambleGiantFight(WildMonster mob, boolean colossus) {
		super(mob, MEMORY);
		this.colossus = colossus;
		this.tint = colossus ? ElementFx.FIRE : ElementFx.EARTH;
		this.warn = new DustParticleOptions(colossus ? 0xFF6A2A : 0xC8A060, 1.6F);
		this.bright = new DustParticleOptions(colossus ? 0xFFD060 : 0xF0E0A0, 2.0F);
		this.thorn = new DustParticleOptions(0x5AC850, 1.4F);
	}

	// ------------------------------------------------------------------ the fight

	@Override
	protected int choose(ServerLevel level, LivingEntity target) {
		return GiantRules.Bramble.choose(deck, across(target), high(target), crowded(level), !colossus, roll());
	}

	@Override
	protected void begin(ServerLevel level, int move, LivingEntity target) {
		twice = false;
		switch (move) {
			case STOMP -> stomp(level, STOMP_TELL);
			case SWEEP -> sweep(level, SWEEP_TELL);
			case HURL -> hurl(level, target, HURL_TELL);
			case ROOTS -> {
				roots.clear();
				pose(ROOTS, ROOTS_TELL, ROOTS_TELL + ROOT_TICKS + 8);
				MonsterMagic.sound(level, feet(), "monster_bramble_rear", 2.0F, 0.6F);
				sound(level, feet(), SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.5F);
			}
			case GRAB -> {
				pose(GRAB, GRAB_TELL, GRAB_TELL + GRAB_REACH);
				MonsterMagic.sound(level, feet(), "monster_bramble_ambient", 2.0F, 0.5F);
			}
			case HAMMER -> {
				laneFrom = feet();
				lane = dir;
				pose(HAMMER, HAMMER_TELL, HAMMER_TELL + 4);
				MonsterMagic.sound(level, feet(), "monster_bramble_rear", 2.4F, 0.45F);
			}
			case BRAMBLES -> {
				pose(BRAMBLES, BRAMBLES_TELL, BRAMBLES_TELL + 6);
				sound(level, feet(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 2.0F, 0.5F);
				MonsterMagic.sound(level, feet(), "monster_bramble_ambient", 2.0F, 0.7F);
			}
			case KINDLE -> {
				pose(KINDLE, KINDLE_TELL, KINDLE_TELL + 10);
				sound(level, feet(), SoundEvents.FIRECHARGE_USE, 2.0F, 0.5F);
				sound(level, feet(), SoundEvents.BLAZE_AMBIENT, 2.0F, 0.5F);
			}
			default -> settle();
		}
	}

	@Override
	protected void act(ServerLevel level, int move, int t, @Nullable LivingEntity target) {
		switch (move) {
			case STOMP -> stomping(level, t);
			case SWEEP -> sweeping(level, t, target);
			case HURL -> hurling(level, t, target);
			case ROOTS -> rooting(level, t, target);
			case GRAB -> grabbing(level, t, target);
			case CLUTCH -> clutching(level, t);
			case STUMBLE -> stumbling(level, t);
			case HAMMER -> hammering(level, t, target);
			case STUCK -> stuck(level, t);
			case BRAMBLES -> brambling(level, t);
			case KINDLE -> kindling(level, t);
			case ROAR -> roaring(level, t);
			default -> hold();
		}
	}

	@Override
	protected void after(ServerLevel level, int move, @Nullable LivingEntity target) {
		boolean again = roused && !twice && target != null;
		switch (move) {
			case STOMP -> {
				if (again) {
					twice = true;
					stomp(level, STOMP_AGAIN_TELL);
				}
			}
			case SWEEP -> {
				if (again && across(target) <= SWEEP_FAR + 2) {
					twice = true;
					sweep(level, SWEEP_BACK_TELL);
				}
			}
			case HURL -> {
				if (again && across(target) <= HURL_FAR) {
					twice = true;
					hurl(level, target, HURL_AGAIN);
				}
			}
			case GRAB -> stumble(level);
			case CLUTCH -> fling(level);
			case HAMMER -> {
				stuckFor = roused ? ROUSED_STUCK_TICKS : STUCK_TICKS;
				pose(STUCK, 0, stuckFor);
			}
			default -> {}
		}
	}

	@Override
	protected float hurt(float damage, int move) {
		return GiantRules.Bramble.hurt(damage, move);
	}

	@Override
	protected void roar(ServerLevel level) {
		roots.clear();
		pose(ROAR, 0, ROAR_TICKS);
		MonsterMagic.sound(level, feet(), "monster_bramble_rear", 2.5F, 0.4F);
	}

	@Override
	protected int restAfter() {
		return roused ? (roll() < CHAIN ? 4 : ROUSED_REST) : REST;
	}

	@Override
	protected void hit(ServerLevel level, DamageSource source, float lost) {
		if (move != CLUTCH) {
			return;
		}
		heldTaken += lost;
		if (breaks(heldTaken, mob.getMaxHealth())) {
			LivingEntity victim = holding(level);
			release(level);
			if (victim != null) {
				MonsterMagic.tell(victim, Component.translatable("message.wildercord.giant.free"));
				MonsterMagic.shove(victim, dir.scale(0.6).add(0, 0.3, 0));
			}
			stumble(level);
		}
	}

	@Override
	protected void release(ServerLevel level) {
		held = null;
		heldTaken = 0;
	}

	@Override
	protected void ambient(ServerLevel level) {
		LivingEntity target = mob.getTarget();
		if (target != null) {
			Vec3 now = target.position();
			drift = seen != null && now.distanceToSqr(seen) < 9 ? drift.scale(0.7).add(now.subtract(seen).scale(0.3)) : Vec3.ZERO;
			seen = now;
		}
		if (colossus && roused && mob.tickCount % 6 == 0 && feet().distanceToSqr(lastPrint) > 4) {
			lastPrint = feet();
			leave(floor(feet()), 1.4, 0, BURN_TICKS, true);
		}
		tickPatches(level);
	}

	// ------------------------------------------------------------------ the stomp

	private void stomp(ServerLevel level, int tell) {
		pose(STOMP, tell, tell + WAVE + 6);
		MonsterMagic.sound(level, feet(), "monster_bramble_rear", 2.2F, 0.55F);
	}

	/** A foot reared high, the ring at its feet marked; stamped down in a quake, and a shockwave rolling out across the ground. */
	private void stomping(ServerLevel level, int t) {
		hold();
		Vec3 feet = feet();
		double inner = STOMP_INNER + hw();
		if (t < tell) {
			if (t % 3 == 0) {
				ring(level, feet, inner, warn, 30);
			}
			if (t % 5 == 0) {
				clods(level, feet, 6, inner * 0.4, 0.05);
			}
			if (t == tell - 6) {
				sound(level, feet, SoundEvents.RAVAGER_STEP, 2.0F, 0.5F);
			}
			return;
		}
		if (t == tell) {
			sound(level, feet, SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.5F);
			sound(level, feet, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.5F, 0.6F);
			ElementFx.crack(level, feet, inner, 40);
			ElementFx.earthImpact(level, feet.add(0, 0.2, 0), 2.5);
			ElementFx.groundRing(level, feet, tint.primary(), hw(), inner, 0.8, 12);
			ElementFx.groundRing(level, feet, tint.secondary(), inner, WAVE_FAR + hw(), WAVE_BAND * 2, WAVE);
			clods(level, feet, 60, inner * 0.5, 0.35);
			if (colossus) {
				ElementFx.flameBurst(level, feet.add(0, 0.3, 0), inner, 40);
			} else {
				ElementFx.petals(level, feet.add(0, 1, 0), inner * 0.6, 24);
			}
			for (LivingEntity e : prey(level, STOMP_INNER)) {
				if (across(e) <= STOMP_INNER && !high(e) && first(e)) {
					strike(level, e, STOMP_HIT, feet, 0.9, 0.5);
				}
			}
			return;
		}
		double radius = STOMP_INNER + (t - tell) * WAVE_SPEED;
		if (radius > WAVE_FAR) {
			return;
		}
		BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, ground(level, feet));
		int count = (int) (radius * 5);
		ring(level, feet, radius + hw(), dirt, count);
		ring(level, feet, radius + hw(), colossus ? ParticleTypes.FLAME : bright, count / 2);
		if ((t - tell) % 3 == 0) {
			sound(level, feet, groundSound(level, feet), 1.5F, 0.5F);
		}
		for (LivingEntity e : prey(level, radius + 1)) {
			if (onWave(across(e), radius, e.onGround()) && first(e)) {
				strike(level, e, WAVE_HIT, feet, 0.6, 0.35);
			}
		}
	}

	// ------------------------------------------------------------------ the sweep

	private void sweep(ServerLevel level, int tell) {
		side = -side;
		mob.giantState(WildMonster.ALT, side < 0);
		pose(SWEEP, tell, tell + SWEEP_SWING + 10);
		MonsterMagic.sound(level, feet(), "monster_bramble_lash", 1.6F, 0.5F);
	}

	/** An arm drawn back to one side while the arc it will cover is marked; swung across its front, flinging aside whoever it meets. */
	private void sweeping(ServerLevel level, int t, @Nullable LivingEntity target) {
		hold();
		Vec3 feet = feet();
		if (target != null && t < tell - 6) {
			dir = flat(target.position().subtract(feet), dir);
		}
		if (t < tell) {
			if (t % 3 == 0) {
				arc(level, feet, dir, SWEEP_FAR + hw(), SWEEP_ARC, warn, 26);
				arc(level, feet, dir, SWEEP_NEAR + hw(), SWEEP_ARC, warn, 8);
				arc(level, feet, turned(side * SWEEP_ARC / 2), SWEEP_FAR + hw() - 1, 16, bright, 4);
			}
			return;
		}
		if (t == tell) {
			sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
			MonsterMagic.sound(level, feet, "monster_bramble_lash", 2.2F, 0.6F);
			ElementFx.slash(level, feet.add(0, mob.getBbHeight() * 0.35, 0), side > 0 ? UP : DOWN, dir, tint.secondary(),
				SWEEP_FAR + hw(), Math.toRadians(SWEEP_ARC), 1.2, SWEEP_SWING, 10);
		}
		if (t >= tell + SWEEP_SWING) {
			return;
		}
		double half = SWEEP_ARC / 2;
		double a0 = side * half * (1 - 2.0 * (t - tell) / SWEEP_SWING), a1 = side * half * (1 - 2.0 * (t - tell + 1) / SWEEP_SWING);
		double lo = Math.min(a0, a1) - 8, hi = Math.max(a0, a1) + 8;
		Vec3 swept = turned((a0 + a1) / 2);
		for (double d = SWEEP_NEAR + hw(); d <= SWEEP_FAR + hw(); d += 0.8) {
			level.sendParticles(colossus ? ParticleTypes.FLAME : ParticleTypes.SWEEP_ATTACK, feet.x + swept.x * d, feet.y + 1.2, feet.z + swept.z * d,
				1, 0.1, 0.2, 0.1, 0);
		}
		clods(level, feet.add(swept.scale(SWEEP_FAR + hw() - 1)), 5, 0.4, 0.15);
		Vec3 fling = right().scale(-side);
		for (LivingEntity e : prey(level, SWEEP_FAR)) {
			double d = across(e), a = angleTo(e.position());
			if (d >= SWEEP_NEAR && d <= SWEEP_FAR && a >= lo && a <= hi && !high(e) && first(e)) {
				Vec3 out = flat(e.position().subtract(feet), dir);
				smack(level, e, SWEEP_HIT, fling.scale(SWEEP_THROW).add(out.scale(0.4)).add(0, 0.45, 0));
			}
		}
	}

	// ------------------------------------------------------------------ the hurl

	private void hurl(ServerLevel level, LivingEntity target, int tell) {
		aim = within(target.position(), HURL_FAR + hw());
		pose(HURL, tell, tell + HURL_FLIGHT + 6);
		sound(level, feet(), groundSound(level, feet()), 2.0F, 0.5F);
	}

	/** A clod torn up and heaved overhead while a ring closes on where it will land; thrown, it bursts there. */
	private void hurling(ServerLevel level, int t, @Nullable LivingEntity target) {
		hold();
		Vec3 feet = feet();
		if (target != null && t < tell - HURL_LOCK) {
			aim = within(twice ? lead(target, tell - t + HURL_FLIGHT) : target.position(), HURL_FAR + hw());
			dir = flat(aim.subtract(feet), dir);
		}
		if (t < tell) {
			if (t % 2 == 0) {
				ring(level, aim, HURL_RADIUS + 2.5 * (1 - (double) t / tell), warn, 18);
			}
			if (t % 4 == 0) {
				ring(level, aim, HURL_RADIUS, bright, 14);
			}
			Vec3 front = feet.add(dir.scale(hw() + 0.8));
			if (t < 8) {
				clods(level, front, 8, 0.6, 0.2);
				if (t % 3 == 0) {
					sound(level, front, groundSound(level, front), 1.8F, 0.6F);
				}
			} else {
				clump(level, hand(), 3);
			}
			if (t == tell - HURL_LOCK) {
				sound(level, aim, colossus ? SoundEvents.FIRECHARGE_USE : SoundEvents.ROOTED_DIRT_BREAK, 1.5F, 0.7F);
			}
			return;
		}
		if (t == tell) {
			from = hand();
			sound(level, from, SoundEvents.RAVAGER_ATTACK, 2.0F, 0.5F);
			sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 0.4F);
		}
		if (t > tell + HURL_FLIGHT) {
			return;
		}
		double f = (t - tell) / (double) HURL_FLIGHT;
		Vec3 p = from.lerp(aim, f).add(0, Math.sin(f * Math.PI) * (4 + from.distanceTo(aim) * 0.25), 0);
		clump(level, p, 6);
		if (t < tell + HURL_FLIGHT) {
			return;
		}
		sound(level, aim, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.8F);
		sound(level, aim, groundSound(level, aim), 2.0F, 0.6F);
		ElementFx.earthImpact(level, aim.add(0, 0.3, 0), 1.6);
		ElementFx.crack(level, aim, HURL_RADIUS, 30);
		ElementFx.groundRing(level, aim, tint.primary(), 0.5, HURL_RADIUS + 0.5, 0.6, 10);
		clods(level, aim, 40, 1.2, 0.3);
		for (LivingEntity e : preyAt(level, aim, HURL_RADIUS, 2.5)) {
			if (first(e)) {
				strike(level, e, HURL_HIT, aim, 0.8, 0.5);
			}
		}
		if (colossus) {
			ElementFx.fireImpact(level, aim.add(0, 0.3, 0), 1.4);
			leave(aim, 2.5, 0, BURN_TICKS, true);
		}
	}

	// ------------------------------------------------------------------ the root

	/** Its fists sunk in the earth while it stares you down; a root bursts away under the ground after you, turning as you run. */
	private void rooting(ServerLevel level, int t, @Nullable LivingEntity target) {
		hold();
		Vec3 feet = feet();
		if (t < tell) {
			if (target != null) {
				dir = flat(target.position().subtract(feet), dir);
			}
			Vec3 front = feet.add(dir.scale(hw() + 0.6));
			if (t % 2 == 0) {
				clods(level, front, 5, 0.5, 0.1);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), front.x, front.y + 0.2, front.z,
					4, 0.4, 0.1, 0.4, 0.1);
			}
			if (t % 5 == 0) {
				sound(level, front, SoundEvents.ROOTED_DIRT_BREAK, 1.6F, 0.5F + t * 0.02F);
			}
			if (t % 4 == 0) {
				line(level, feet, dir, hw() + 1, hw() + 1 + 4.0 * t / tell, warn, 0.6);
			}
			return;
		}
		if (t == tell) {
			Vec3 start = floor(feet.add(dir.scale(hw() + 1)));
			roots.clear();
			if (roused) {
				roots.add(new Vec3[] {start, turned(-ROOT_SPREAD)});
				roots.add(new Vec3[] {start, turned(ROOT_SPREAD)});
			} else {
				roots.add(new Vec3[] {start, dir});
			}
			sound(level, start, SoundEvents.ROOTED_DIRT_BREAK, 2.2F, 0.4F);
			sound(level, start, groundSound(level, start), 2.0F, 0.5F);
			clods(level, start, 30, 0.8, 0.25);
		}
		ParticleOptions trail = colossus ? ParticleTypes.FLAME : thorn;
		Iterator<Vec3[]> it = roots.iterator();
		while (it.hasNext()) {
			Vec3[] root = it.next();
			Vec3 at = root[0], way = target != null ? steer(root[1], target.position().subtract(root[0]), ROOT_TURN) : root[1];
			Vec3 next = floor(at.add(way.scale(ROOT_SPEED)));
			if (Math.abs(next.y - at.y) > 1.5 || t - tell >= ROOT_TICKS) {
				clods(level, at, 16, 0.5, 0.2);
				sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 1.4F, 0.8F);
				it.remove();
				continue;
			}
			root[0] = next;
			root[1] = way;
			clods(level, next, 4, 0.25, 0.12);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), next.x, next.y + 0.15, next.z,
				3, 0.2, 0.05, 0.2, 0.08);
			level.sendParticles(trail, next.x, next.y + 0.2, next.z, 2, 0.15, 0.05, 0.15, 0.01);
			if ((t - tell) % 4 == 0) {
				sound(level, next, groundSound(level, next), 0.9F, 0.6F);
			}
			boolean caught = false;
			for (LivingEntity e : preyAt(level, next, ROOT_CATCH, 1.5)) {
				if (e.getY() - next.y < 1 && first(e)) {
					caught = true;
					if (strike(level, e, ROOT_HIT, next, 0.1, 0.6)) {
						MonsterMagic.root(level, e, MonsterRules.rootTicks(level.getDifficulty().getId()));
					}
				}
			}
			if (caught) {
				sound(level, next, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.4F);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), next.x, next.y + 0.8, next.z,
					30, 0.4, 0.8, 0.4, 0.2);
				if (colossus) {
					ElementFx.flames(level, next, 0.8, 2, 10);
				}
				it.remove();
			}
		}
		if (roots.isEmpty() && t > tell && left > 8) {
			left = 8;
		}
	}

	/** A way turned toward another by at most so many degrees. */
	private static Vec3 steer(Vec3 way, Vec3 toward, double most) {
		Vec3 to = flat(toward, way);
		double from = Math.toDegrees(Math.atan2(-way.x, way.z)), goal = Math.toDegrees(Math.atan2(-to.x, to.z));
		return ahead(from + Mth.clamp(Mth.wrapDegrees(goal - from), -most, most));
	}

	// ------------------------------------------------------------------ the grab

	/** An arm drawn back with the hand open while its reach is marked; it lunges and closes on whoever is in it. */
	private void grabbing(ServerLevel level, int t, @Nullable LivingEntity target) {
		Vec3 feet = feet();
		if (t < tell) {
			hold();
			if (target != null && t < tell - 4) {
				dir = flat(target.position().subtract(feet), dir);
			}
			if (t % 3 == 0) {
				arc(level, feet, dir, GRAB_RANGE + hw(), 90, warn, 18);
				arc(level, feet, dir, GRAB_NEAR + hw(), 90, warn, 6);
			}
			if (t == tell - 4) {
				arc(level, feet, dir, GRAB_RANGE + hw(), 90, bright, 18);
				sound(level, feet, SoundEvents.RAVAGER_STEP, 2.0F, 0.6F);
			}
			return;
		}
		if (t == tell) {
			sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, 1.8F, 0.4F);
			MonsterMagic.sound(level, feet, "monster_bramble_lash", 2.0F, 0.5F);
		}
		mob.setDeltaMovement(dir.x * 0.3, mob.getDeltaMovement().y, dir.z * 0.3);
		Vec3 hand = hand().add(0, -mob.getBbHeight() * 0.5, 0);
		level.sendParticles(colossus ? ParticleTypes.FLAME : ParticleTypes.SWEEP_ATTACK, hand.x, hand.y, hand.z, 2, 0.3, 0.3, 0.3, 0);
		for (LivingEntity e : prey(level, GRAB_RANGE)) {
			if (grabs(across(e), dot(e.position())) && !high(e)) {
				held = e.getUUID();
				heldTaken = 0;
				pose(CLUTCH, 0, CLUTCH_TICKS);
				MonsterMagic.tell(e, Component.translatable("message.wildercord.giant.caught"));
				sound(level, e.position(), SoundEvents.RAVAGER_ATTACK, 2.0F, 0.6F);
				MonsterMagic.sound(level, feet, "monster_bramble_rear", 2.0F, 0.8F);
				return;
			}
		}
	}

	/** Whoever it caught held up before its face and squeezed now and then, until it throws them or a blow breaks its grip. */
	private void clutching(ServerLevel level, int t) {
		hold();
		LivingEntity victim = holding(level);
		if (victim == null) {
			release(level);
			settle();
			return;
		}
		Vec3 grip = feet().add(dir.scale(hw() + 1.6)).add(0, mob.getBbHeight() * 0.7, 0);
		victim.setDeltaMovement(grip.subtract(victim.position()).scale(0.5));
		MonsterMagic.sync(victim);
		victim.resetFallDistance();
		if (t % 4 == 0) {
			level.sendParticles(colossus ? ParticleTypes.SMALL_FLAME : thorn, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
				4, 0.4, 0.5, 0.4, 0.01);
		}
		if (t > 0 && t % SQUEEZE_EVERY == 0) {
			if (victim.hurtServer(level, mob.damageSources().mobAttack(mob), SQUEEZE_HIT) && colossus) {
				victim.igniteForSeconds(2);
			}
			sound(level, grip, SoundEvents.WOOD_BREAK, 1.6F, 0.5F);
			MonsterMagic.sound(level, grip, "monster_bramble_hurt", 1.2F, 0.5F);
			if (colossus) {
				ElementFx.embers(level, grip, 0.8, 10);
			} else {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), grip.x, grip.y, grip.z,
					10, 0.5, 0.5, 0.5, 0.1);
			}
		}
	}

	/** Whoever it held hurled away ahead of it. */
	private void fling(ServerLevel level) {
		LivingEntity victim = holding(level);
		release(level);
		if (victim == null) {
			return;
		}
		victim.hurtServer(level, mob.damageSources().mobAttack(mob), THROWN_HIT);
		MonsterMagic.shove(victim, dir.scale(CLUTCH_THROW).add(0, CLUTCH_THROW_UP, 0));
		sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.4F);
		sound(level, feet(), SoundEvents.RAVAGER_ROAR, 1.6F, 0.7F);
	}

	/** Its grab missed or was broken: it stumbles forward onto a hand, dazed and open. */
	private void stumble(ServerLevel level) {
		pose(STUMBLE, 0, STUMBLE_TICKS);
		sound(level, feet(), SoundEvents.RAVAGER_STUNNED, 2.0F, 0.5F);
	}

	private void stumbling(ServerLevel level, int t) {
		if (t < 8) {
			mob.setDeltaMovement(dir.x * 0.12, mob.getDeltaMovement().y, dir.z * 0.12);
		} else {
			hold();
		}
		if (t == 6) {
			Vec3 palm = feet().add(dir.scale(hw() + 1));
			clods(level, palm, 20, 0.6, 0.2);
			sound(level, palm, groundSound(level, palm), 2.0F, 0.5F);
		}
		if (t % 4 == 0) {
			Vec3 head = head();
			level.sendParticles(ParticleTypes.CRIT, head.x, head.y, head.z, 4, 0.6, 0.2, 0.6, 0.05);
		}
	}

	// ------------------------------------------------------------------ the hammer

	/** Both fists raised high over its head while the lane ahead is marked out; slammed down along the length of it. */
	private void hammering(ServerLevel level, int t, @Nullable LivingEntity target) {
		hold();
		Vec3 feet = feet();
		if (t < tell) {
			if (target != null && t < tell - HAMMER_LOCK) {
				lane = flat(target.position().subtract(feet), lane);
				laneFrom = feet;
				dir = lane;
			}
			if (t % 3 == 0) {
				DustParticleOptions mark = t >= tell - HAMMER_LOCK ? bright : warn;
				Vec3 edge = new Vec3(-lane.z, 0, lane.x).scale(HAMMER_WIDTH);
				line(level, laneFrom.add(edge), lane, hw(), HAMMER_LENGTH + hw(), mark, 0.7);
				line(level, laneFrom.subtract(edge), lane, hw(), HAMMER_LENGTH + hw(), mark, 0.7);
				Vec3 end = laneFrom.add(lane.scale(HAMMER_LENGTH + hw()));
				line(level, end.subtract(edge), new Vec3(-lane.z, 0, lane.x), 0, HAMMER_WIDTH * 2, mark, 0.5);
			}
			if (t == tell - HAMMER_LOCK) {
				sound(level, feet, SoundEvents.RAVAGER_ROAR, 1.8F, 0.6F);
			}
			return;
		}
		if (t != tell) {
			return;
		}
		sound(level, feet, SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.6F);
		sound(level, feet, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.5F, 0.5F);
		ElementFx.earthImpact(level, laneFrom.add(lane.scale(hw() + 1)).add(0, 0.3, 0), 2);
		for (double d = hw(); d <= HAMMER_LENGTH + hw(); d += 1.5) {
			Vec3 p = floor(laneFrom.add(lane.scale(d)));
			ElementFx.crack(level, p, 1.4, 30);
			clods(level, p, 10, 0.7, 0.3);
			if (colossus) {
				ElementFx.flames(level, p, 0.9, 1.4, 5);
			}
		}
		if (colossus) {
			for (double d = hw() + 2; d <= HAMMER_LENGTH + hw(); d += 3) {
				leave(floor(laneFrom.add(lane.scale(d))), 1.3, 0, BURN_TICKS, true);
			}
		}
		for (LivingEntity e : prey(level, HAMMER_LENGTH + 1)) {
			Vec3 off = e.position().subtract(laneFrom);
			double along = off.x * lane.x + off.z * lane.z, aside = off.z * lane.x - off.x * lane.z;
			if (GiantRules.inLane(along, aside, HAMMER_LENGTH + hw(), HAMMER_WIDTH) && !high(e) && first(e)) {
				strike(level, e, HAMMER_HIT, e.position().subtract(lane), 0.3, 0.7);
			}
		}
	}

	/** Its fists stuck fast in the ground: it strains to tear them free, wide open to blows, and wrenches loose in a spray of earth. */
	private void stuck(ServerLevel level, int t) {
		hold();
		Vec3 fists = floor(feet().add(dir.scale(hw() + 1.2)));
		if (t % 4 == 0) {
			clods(level, fists, 6, 0.6, 0.12);
		}
		if (t % 10 == 0) {
			sound(level, fists, SoundEvents.WOOD_BREAK, 1.6F, 0.4F + t * 0.01F);
			if (colossus) {
				Motes.smoke(level, fists.add(0, 0.5, 0), 4, 0.6);
			}
		}
		if (t % 5 == 0) {
			Vec3 head = head();
			level.sendParticles(ParticleTypes.CRIT, head.x, head.y, head.z, 3, 0.6, 0.2, 0.6, 0.05);
		}
		if (t == stuckFor - 6) {
			sound(level, fists, groundSound(level, fists), 2.2F, 0.5F);
			sound(level, fists, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.5F);
			clods(level, fists, 40, 1.0, 0.35);
			ElementFx.stoneShards(level, fists.add(0, 0.3, 0), ground(level, fists), 16, 0.4);
		}
	}

	// ------------------------------------------------------------------ the Elder's brambles

	/** Its fists planted and the ground greening about it; brambles marked out under everyone near burst up a moment later. */
	private void brambling(ServerLevel level, int t) {
		hold();
		Vec3 feet = feet();
		if (t < tell) {
			if (t % 4 == 0) {
				ElementFx.leafSpiral(level, feet, hw() + 1, mob.getBbHeight(), 10);
			}
			if (t % 3 == 0) {
				ring(level, feet, hw() + 0.5, thorn, 16);
			}
			return;
		}
		if (t != tell) {
			return;
		}
		sound(level, feet, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.6F);
		ElementFx.petals(level, feet.add(0, 1, 0), hw() + 2, 24);
		for (Player p : players(level, BRAMBLE_REACH, BRAMBLE_MAX)) {
			if (p.isSpectator()) {
				continue;
			}
			Vec3 under = floor(p.position());
			leave(under, BRAMBLE_RADIUS, BRAMBLE_FUSE, BRAMBLE_LINGER, false);
			Vec3 ahead = floor(p == mob.getTarget() ? lead(p, BRAMBLE_FUSE) : p.position());
			if (roused && ahead.distanceToSqr(under) > BRAMBLE_RADIUS * BRAMBLE_RADIUS) {
				leave(ahead, BRAMBLE_RADIUS, BRAMBLE_FUSE, BRAMBLE_LINGER, false);
			}
		}
	}

	// ------------------------------------------------------------------ the Colossus's kindling

	/** Embers drawn into it until it glows white hot, a ring of fire marking the blast's reach; then the air about it bursts into flame. */
	private void kindling(ServerLevel level, int t) {
		hold();
		Vec3 feet = feet();
		double reach = KINDLE_RADIUS + hw();
		if (t < tell) {
			ElementFx.embers(level, feet.add(0, mob.getBbHeight() * 0.5, 0), hw() + 1, 2 + t / 5);
			if (t % 3 == 0) {
				ring(level, feet, reach, ParticleTypes.FLAME, 40);
				ring(level, feet, hw() + (reach - hw()) * t / tell, warn, 24);
			}
			if (t % 8 == 0) {
				sound(level, feet, SoundEvents.FIRECHARGE_USE, 1.6F, 0.5F + t * 0.03F);
			}
			if (t == tell - 8) {
				ElementFx.heatFlare(level, feet.add(0, mob.getBbHeight() * 0.6, 0), 3);
				sound(level, feet, SoundEvents.BLAZE_SHOOT, 2.0F, 0.5F);
			}
			return;
		}
		if (t != tell) {
			return;
		}
		sound(level, feet, SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.7F);
		sound(level, feet, SoundEvents.BLAZE_SHOOT, 2.5F, 0.4F);
		ElementFx.flameBurst(level, feet.add(0, 1, 0), reach, 80);
		ElementFx.fireImpact(level, feet.add(0, 1.5, 0), 3);
		ElementFx.groundRing(level, feet, tint.primary(), hw(), reach, 1.2, 10);
		Motes.smoke(level, feet.add(0, mob.getBbHeight() * 0.5, 0), 12, hw() + 1);
		for (LivingEntity e : prey(level, KINDLE_RADIUS)) {
			if (across(e) <= KINDLE_RADIUS && !high(e) && first(e) && strike(level, e, KINDLE_HIT, feet, 1.0, 0.5)) {
				e.igniteForSeconds(KINDLE_BURN);
			}
		}
		if (roused) {
			for (int i = 0; i < 6; i++) {
				leave(floor(feet.add(turned(i * 60 + 30).scale(reach * 0.75))), 1.4, 0, BURN_TICKS, true);
			}
		}
	}

	// ------------------------------------------------------------------ the roar

	/** Half beaten, it rears and roars, throwing back whoever is near. */
	private void roaring(ServerLevel level, int t) {
		hold();
		Vec3 feet = feet();
		if (t == 6) {
			sound(level, feet, SoundEvents.RAVAGER_ROAR, 3.0F, 0.4F);
			for (LivingEntity e : prey(level, ROAR_RADIUS)) {
				if (across(e) <= ROAR_RADIUS) {
					MonsterMagic.knock(e, feet, 1.4);
				}
			}
			ElementFx.gustRing(level, feet, ROAR_RADIUS + hw());
			if (colossus) {
				ElementFx.flameBurst(level, feet.add(0, 1, 0), ROAR_RADIUS, 60);
			} else {
				ElementFx.petals(level, feet.add(0, 2, 0), ROAR_RADIUS * 0.6, 40);
				ElementFx.leafSpiral(level, feet, hw() + 2, mob.getBbHeight() * 1.2, 16);
			}
		}
		if (t > 6 && t % 4 == 0) {
			Vec3 head = head();
			if (colossus) {
				ElementFx.embers(level, head, 1.5, 8);
			} else {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), head.x, head.y, head.z,
					8, 1.2, 0.6, 1.2, 0.1);
			}
		}
	}

	// ------------------------------------------------------------------ what it leaves on the ground

	private void tickPatches(ServerLevel level) {
		if (patches.isEmpty()) {
			return;
		}
		Iterator<Patch> it = patches.iterator();
		while (it.hasNext()) {
			Patch p = it.next();
			if (mob.tickCount >= p.until()) {
				it.remove();
				continue;
			}
			int age = mob.tickCount - p.born();
			if (p.burn()) {
				burning(level, p, age);
			} else {
				brambles(level, p, age);
			}
		}
	}

	/** Burning ground, setting alight whoever stands in it. */
	private void burning(ServerLevel level, Patch p, int age) {
		if (age % 4 == 0) {
			ElementFx.flames(level, p.at(), p.radius(), 0.8, 3);
		}
		if (age % 10 == 0) {
			for (LivingEntity e : preyAt(level, p.at(), p.radius(), 1.5)) {
				e.igniteForSeconds(3);
			}
		}
	}

	/** Brambles marked out, bursting up when the fuse runs out, rooting and pricking whoever is in them, then lingering a while. */
	private void brambles(ServerLevel level, Patch p, int age) {
		Vec3 at = p.at();
		double r = p.radius();
		if (age < p.fuse()) {
			if (age % 3 == 0) {
				ring(level, at, r, thorn, 14);
				ring(level, at, r * age / p.fuse(), warn, 8);
			}
			if (age % 6 == 0) {
				clods(level, at, 3, r * 0.5, 0.05);
			}
			return;
		}
		if (age == p.fuse()) {
			sound(level, at, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.8F, 0.6F);
			sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 1.8F, 0.6F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), at.x, at.y + 0.5, at.z,
				30, r * 0.5, 0.4, r * 0.5, 0.2);
			ElementFx.leafSpiral(level, at, r, 2, 8);
			for (LivingEntity e : preyAt(level, at, r, 2)) {
				if (e.getY() - at.y < 1.5 && e.hurtServer(level, mob.damageSources().mobAttack(mob), BRAMBLE_HIT)) {
					MonsterMagic.root(level, e, BRAMBLE_ROOT);
				}
			}
			return;
		}
		if (age % 5 == 0) {
			level.sendParticles(thorn, at.x, at.y + 0.4, at.z, 6, r * 0.5, 0.3, r * 0.5, 0);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()), at.x, at.y + 0.3, at.z,
				2, r * 0.5, 0.2, r * 0.5, 0);
		}
		if (age % 10 == 0) {
			for (LivingEntity e : preyAt(level, at, r, 1.5)) {
				if (e.onGround()) {
					e.hurtServer(level, mob.damageSources().mobAttack(mob), BRAMBLE_PRICK);
				}
			}
		}
	}

	private void leave(Vec3 at, double radius, int fuse, int life, boolean burn) {
		patches.add(new Patch(at, radius, mob.tickCount, fuse, mob.tickCount + fuse + life, burn));
	}

	// ------------------------------------------------------------------ its body

	private double hw() {
		return mob.getBbWidth() / 2;
	}

	/** Its hand raised high, a clod in it. */
	private Vec3 hand() {
		return feet().add(dir.scale(hw() + 0.6)).add(0, mob.getBbHeight(), 0);
	}

	private Vec3 head() {
		return feet().add(dir.scale(hw() * 0.5)).add(0, mob.getBbHeight() * 0.95, 0);
	}

	/** Where its prey will be in so many ticks, going the way it has been. */
	private Vec3 lead(LivingEntity target, int ticks) {
		return target.position().add(drift.scale(ticks));
	}

	/** Whoever it holds, if they're still there. */
	private @Nullable LivingEntity holding(ServerLevel level) {
		return held != null && level.getEntity(held) instanceof LivingEntity v && v.isAlive() && v.distanceTo(mob) <= 12 ? v : null;
	}

	/** Bits of a clod: earth for the Elder, glowing rock for the Colossus. */
	private void clump(ServerLevel level, Vec3 p, int n) {
		if (colossus) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()), p.x, p.y, p.z, n, 0.3, 0.3, 0.3, 0.05);
			level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, n, 0.3, 0.3, 0.3, 0.02);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 1, 0.2, 0.2, 0.2, 0.01);
		} else {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), p.x, p.y, p.z, n, 0.3, 0.3, 0.3, 0.05);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), p.x, p.y, p.z, n, 0.3, 0.3, 0.3, 0.05);
		}
	}

	/** A blow that sends its victim off along an impulse of its own. */
	private void smack(ServerLevel level, LivingEntity e, float damage, Vec3 impulse) {
		if (e.hurtServer(level, mob.damageSources().mobAttack(mob), damage)) {
			MonsterMagic.shove(e, impulse);
		}
	}
}
