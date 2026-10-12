package dev.wildercord.monster;

import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.Targets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A giant's fight, in place of its own kind's AI while it has someone to fight: a rest, then a move picked from a deck that
 * never plays the same one twice running, then a rest again. Each move is a pose synced to the client ({@link
 * WildMonster#giantMove()}) with a tell before it lands, and whatever the move does is up to the giant
 * ({@link BrambleGiantFight}, {@link MatriarchFight}). Half beaten it roars and fights on roused.
 */
public abstract class GiantFight extends Goal {
	/** How far off it keeps hold of whoever it's fighting, and how far it looks for someone new. */
	private static final double KEEP = 48, SPOT = 24;

	protected final WildMonster mob;
	protected final MoveDeck deck;
	/** Whoever the move under way has hit already. */
	protected final Set<UUID> struck = new HashSet<>();
	/** The move under way, its tell, ticks into it, ticks left of it, and the rest before the next. */
	protected int move, tell, since, left, rest;
	/** How often it has been struck lately. */
	protected int pressed;
	protected boolean roused;
	/** The way it faces across the ground. */
	protected Vec3 dir = new Vec3(0, 0, 1);

	protected GiantFight(WildMonster mob, int memory) {
		this.mob = mob;
		this.deck = new MoveDeck(memory);
		this.roused = mob.giantRoused();
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	// ------------------------------------------------------------------ what each giant brings

	/** Picks its next move against its prey; -1 to close in instead. */
	protected abstract int choose(ServerLevel level, LivingEntity target);

	/** Sets a move going (its pose, tell and length, and whatever it marks out). */
	protected abstract void begin(ServerLevel level, int move, LivingEntity target);

	/** One tick of the move under way, {@code t} ticks into it. Its prey may be gone. */
	protected abstract void act(ServerLevel level, int move, int t, @Nullable LivingEntity target);

	/** The move under way has run out: chain into another with {@link #pose}, or {@link #settle}. */
	protected abstract void after(ServerLevel level, int move, @Nullable LivingEntity target);

	/** What a blow deals it in the middle of a move. */
	protected abstract float hurt(float damage, int move);

	/** Its roar at half health: break off whatever it was doing and pose for it. */
	protected abstract void roar(ServerLevel level);

	/** The rest after a move (roused, shorter, and sometimes next to none). */
	protected abstract int restAfter();

	/** A blow landed on it in the middle of a move ({@code lost} the health it cost). */
	protected void hit(ServerLevel level, DamageSource source, float lost) {}

	/** Lets go of whatever it holds, the fight over or broken off. */
	protected void release(ServerLevel level) {}

	/** Every tick of the fight, between moves or in them: whatever it has left lying about. */
	protected void ambient(ServerLevel level) {}

	// ------------------------------------------------------------------ the goal

	@Override
	public boolean canUse() {
		LivingEntity target = mob.getTarget();
		if ((target == null || !target.isAlive()) && mob.tickCount % 10 == 0 && mob.level() instanceof ServerLevel level) {
			target = spot(level);
			if (target != null) {
				mob.setTarget(target);
			}
		}
		return target != null && target.isAlive() && Targets.canHarm(mob, target) && mob.distanceToSqr(target) < KEEP * KEEP;
	}

	@Override
	public boolean canContinueToUse() {
		return move != 0 || canUse();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		rest = Math.max(rest, 10);
	}

	@Override
	public void stop() {
		if (mob.level() instanceof ServerLevel level) {
			release(level);
		}
		settle();
		deck.clear();
		pressed = 0;
	}

	@Override
	public void tick() {
		if (!(mob.level() instanceof ServerLevel level)) {
			return;
		}
		LivingEntity target = mob.getTarget();
		if (target != null && !target.isAlive()) {
			target = null;
		}
		if (pressed > 0 && mob.tickCount % 40 == 0) {
			pressed--;
		}
		ambient(level);
		if (move != 0) {
			int was = move;
			face(dir);
			act(level, move, since++, target);
			if (move == was && --left <= 0) {
				after(level, was, target);
				// The same move posed afresh (a second stomp, the next swipe) has ticks left again.
				if (move == was && left <= 0) {
					settle();
				}
			}
			return;
		}
		if (target == null) {
			return;
		}
		mob.getLookControl().setLookAt(target, 30, 30);
		if (rest > 0) {
			rest--;
			if (mob.tickCount % 10 == 0) {
				if (across(target) > 5) {
					mob.getNavigation().moveTo(target, 1.0);
				} else {
					mob.getNavigation().stop();
				}
			}
			return;
		}
		int next = choose(level, target);
		if (next < 0) {
			if (mob.tickCount % 10 == 0) {
				mob.getNavigation().moveTo(target, 1.0);
			}
			return;
		}
		deck.played(next);
		dir = flat(target.position().subtract(mob.position()), dir);
		face(dir);
		begin(level, next, target);
	}

	/** Someone near enough and in sight to fight, nearest first. */
	private @Nullable Player spot(ServerLevel level) {
		return level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(SPOT, 8, SPOT),
				p -> p.isAlive() && Targets.canHarm(mob, p) && mob.hasLineOfSight(p))
			.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
	}

	// ------------------------------------------------------------------ blows on it

	/** What a blow deals it, by the move under way. */
	final float taking(DamageSource source, float damage) {
		return hurt(damage, move);
	}

	/** A blow has landed: it turns on whoever struck it if it had no one, and rouses at half health. */
	final void took(ServerLevel level, DamageSource source, float lost) {
		if (mob.isDeadOrDying()) {
			return;
		}
		pressed++;
		if (mob.getTarget() == null && source.getEntity() instanceof LivingEntity attacker && Targets.canHarm(mob, attacker)) {
			mob.setTarget(attacker);
		}
		if (move != 0) {
			hit(level, source, lost);
		}
		if (GiantRules.rouses(roused, mob.getHealth(), mob.getMaxHealth())) {
			roused = true;
			mob.giantRouse();
			release(level);
			roar(level);
		}
	}

	// ------------------------------------------------------------------ moves

	/** Sets a move going: its pose and tell synced, its length, and no one struck yet. */
	protected void pose(int move, int tell, int ticks) {
		this.move = move;
		this.tell = tell;
		this.since = 0;
		this.left = ticks;
		struck.clear();
		mob.giantPose(move, tell);
		mob.getNavigation().stop();
	}

	/** Back to its rest between moves. */
	protected void settle() {
		pose(0, 0, 0);
		mob.giantState(WildMonster.VEILED, false);
		rest = restAfter();
	}

	/** Planted where it is. */
	protected void hold() {
		mob.getNavigation().stop();
		mob.setDeltaMovement(mob.getDeltaMovement().multiply(0, 1, 0));
	}

	protected void face(Vec3 way) {
		float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
		mob.setYRot(yaw);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
	}

	/** Turns it toward a way by at most so many degrees. */
	protected void turnToward(Vec3 way, double most) {
		double from = Math.toDegrees(Math.atan2(-dir.x, dir.z)), to = Math.toDegrees(Math.atan2(-way.x, way.z));
		dir = ahead(from + Mth.clamp(Mth.wrapDegrees(to - from), -most, most));
	}

	/** A chance, 0 to 1. */
	protected double roll() {
		return mob.getRandom().nextDouble();
	}

	// ------------------------------------------------------------------ reach

	/** Its feet. */
	protected Vec3 feet() {
		return mob.position();
	}

	/** How far someone is across the ground from its edge. */
	protected double across(Entity e) {
		return across(e.position());
	}

	protected double across(Vec3 at) {
		return Math.max(0, Math.sqrt(Mth.square(at.x - mob.getX()) + Mth.square(at.z - mob.getZ())) - mob.getBbWidth() / 2);
	}

	/** Whether someone is out of reach above it. */
	protected boolean high(Entity e) {
		return e.getY() - mob.getY() > GiantRules.REACH_UP + mob.getBbHeight() * 0.5;
	}

	/** Degrees between where it faces and someone. */
	protected double angleTo(Vec3 at) {
		return Mth.wrapDegrees(Math.toDegrees(Math.atan2(-(at.x - mob.getX()), at.z - mob.getZ())) - Math.toDegrees(Math.atan2(-dir.x, dir.z)));
	}

	/** How square ahead of it someone is, -1 straight behind to 1 straight ahead. */
	protected double dot(Vec3 at) {
		return flat(at.subtract(mob.position()), dir).dot(dir);
	}

	/** Two or more about it, or being struck again and again. */
	protected boolean crowded(ServerLevel level) {
		double r = 6 + mob.getBbWidth() / 2;
		return pressed >= 3 || level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(r, 3, r),
			p -> p.isAlive() && Targets.canHarm(mob, p)).size() >= 2;
	}

	/** Whoever it may strike within a reach of its edge. */
	protected List<LivingEntity> prey(ServerLevel level, double range) {
		return level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(range, 3, range),
			e -> e != mob && e.isAlive() && Targets.canHarm(mob, e));
	}

	/** Whoever it may strike within a reach of a point. */
	protected List<LivingEntity> preyAt(ServerLevel level, Vec3 at, double range, double up) {
		return level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(range, up, range),
			e -> e != mob && e.isAlive() && Targets.canHarm(mob, e)
				&& Mth.square(e.getX() - at.x) + Mth.square(e.getZ() - at.z) <= range * range);
	}

	/** Players about it it might mark something under, nearest first. */
	protected List<Player> players(ServerLevel level, double range, int most) {
		return level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(range, 8, range),
				p -> p.isAlive() && Targets.canHarm(mob, p))
			.stream().sorted(Comparator.comparingDouble(mob::distanceToSqr)).limit(most).toList();
	}

	/** Whether this is the first time the move under way catches someone. */
	protected boolean first(Entity e) {
		return struck.add(e.getUUID());
	}

	/** A blow from it, the victim thrown away from a point and up; whether it landed. */
	protected boolean strike(ServerLevel level, LivingEntity victim, double damage, Vec3 at, double push, double lift) {
		if (!victim.hurtServer(level, mob.damageSources().mobAttack(mob), (float) damage)) {
			return false;
		}
		throwFrom(victim, at, push, lift);
		return true;
	}

	/** Throws someone away from a point and up. */
	protected void throwFrom(LivingEntity victim, Vec3 at, double push, double lift) {
		Vec3 away = flat(victim.position().subtract(at), dir).scale(push);
		MonsterMagic.shove(victim, new Vec3(away.x, lift, away.z));
	}

	/** The way across the ground along a vector, or the fallback if it has none. */
	protected static Vec3 flat(Vec3 v, Vec3 fallback) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1e-6 ? fallback : f.normalize();
	}

	/** The way across the ground at a yaw. */
	protected static Vec3 ahead(double yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	/** Its way turned so many degrees. */
	protected Vec3 turned(double degrees) {
		return ahead(Math.toDegrees(Math.atan2(-dir.x, dir.z)) + degrees);
	}

	/** The way to its right across the ground. */
	protected Vec3 right() {
		return new Vec3(-dir.z, 0, dir.x);
	}

	/** A point set down on the ground. */
	protected Vec3 floor(Vec3 at) {
		return new Vec3(at.x, mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z)), at.z);
	}

	/** A point brought within a reach of it across the ground, set down on the ground. */
	protected Vec3 within(Vec3 at, double reach) {
		Vec3 off = at.subtract(mob.position());
		double far = off.horizontalDistance();
		return floor(far <= reach ? at : mob.position().add(off.x * reach / far, 0, off.z * reach / far));
	}

	// ------------------------------------------------------------------ sound and marks

	protected BlockState ground(ServerLevel level, Vec3 at) {
		BlockState state = level.getBlockState(BlockPos.containing(at).below());
		return state.isAir() ? Blocks.DIRT.defaultBlockState() : state;
	}

	protected SoundEvent groundSound(ServerLevel level, Vec3 at) {
		return ground(level, at).getSoundType().getBreakSound();
	}

	protected void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
	}

	/** Bits of the ground thrown up about a point. */
	protected void clods(ServerLevel level, Vec3 at, int count, double spread, double speed) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground(level, at)), at.x, at.y + 0.1, at.z, count, spread, 0.1, spread, speed);
	}

	/** A ring marked on the ground about a point. */
	protected void ring(ServerLevel level, Vec3 at, double radius, ParticleOptions mark, int count) {
		double turn = mob.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			double a = turn + i * Math.PI * 2 / count;
			level.sendParticles(mark, at.x + Math.cos(a) * radius, at.y + 0.15, at.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
		}
	}

	/** An arc marked on the ground ahead of it, so many degrees wide, at a radius. */
	protected void arc(ServerLevel level, Vec3 at, Vec3 way, double radius, double degrees, ParticleOptions mark, int count) {
		double base = Math.toDegrees(Math.atan2(-way.x, way.z));
		for (int i = 0; i <= count; i++) {
			Vec3 p = ahead(base - degrees / 2 + degrees * i / count).scale(radius);
			level.sendParticles(mark, at.x + p.x, at.y + 0.15, at.z + p.z, 1, 0, 0, 0, 0);
		}
	}

	/** A line marked on the ground out along a way, from one distance to another. */
	protected void line(ServerLevel level, Vec3 at, Vec3 way, double near, double far, ParticleOptions mark, double step) {
		for (double d = near; d <= far; d += step) {
			level.sendParticles(mark, at.x + way.x * d, at.y + 0.15, at.z + way.z * d, 1, 0.05, 0, 0.05, 0);
		}
	}
}
