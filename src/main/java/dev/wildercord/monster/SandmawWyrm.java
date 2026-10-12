package dev.wildercord.monster;

import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Sandmaw Wyrm: a great segmented worm of the deserts and badlands, the first of the biome elites (see
 * {@link EliteTier}), sand-hided and ring-mouthed. It lies under the sand and feels footsteps from far off, though barely
 * a sneak.
 *
 * <p>Under the sand it can't be struck; a furrow of sand trails it as it swims after its prey. Beneath them the ground
 * rumbles (step off it) and it bursts up, flinging all about; or, now and then, a furrow runs out at its prey from afar and
 * it breaches clean out along it, down across them, and lies beached a while after. Up, it never makes the same move twice
 * running (see {@link SandmawRules}): the slam along a line, the spit down a cone, the gulp on a marked spot that swallows
 * whoever stands there, the sinkhole that drags everyone in toward its jaws, geysers sent up beneath its prey one after
 * another, and its spines loosed along marked rays. Half beaten it thrashes and fights on roused.</p>
 *
 * <p>The move under way, when it began and how long its tell is are synced, and the client eases each move's pose in and out
 * ({@link #blend}); the old pose flags are still kept for the veil and the shadow.</p>
 */
public class SandmawWyrm extends EliteMonster {
	private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(SandmawWyrm.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TELL = SynchedEntityData.defineId(SandmawWyrm.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN = SynchedEntityData.defineId(SandmawWyrm.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Boolean> ROUSED = SynchedEntityData.defineId(SandmawWyrm.class, EntityDataSerializers.BOOLEAN);
	private static final int MOVES = SandmawRules.THRASH + 1;
	private static final DustParticleOptions SAND = new DustParticleOptions(0xD9B26F, 1.4F), SPINE = new DustParticleOptions(0x8A5A2B, 1.1F),
		SPINE_NEXT = new DustParticleOptions(0xE8D9A8, 1.1F), MAW = new DustParticleOptions(0xB0413E, 1.5F);

	/** A geyser marked beneath someone, and when it goes up. */
	private record Geyser(Vec3 at, long when) {}

	private final MoveDeck deck = new MoveDeck(SandmawRules.MEMORY);
	private final Set<UUID> struck = new HashSet<>();
	private final List<Geyser> geysers = new ArrayList<>();
	private final float[] blend = new float[MOVES], blendO = new float[MOVES];
	private int left, down, up, rest, pressed, marked, gouts;
	private double hurtSinceRise, broke, quillStart;
	private boolean lastBreach, again, roused;
	private Vec3 dir = new Vec3(0, 0, 1), aim = new Vec3(0, 0, 1), spot = Vec3.ZERO, from = Vec3.ZERO, to = Vec3.ZERO;
	private @Nullable UUID swallowed;

	public SandmawWyrm(EntityType<? extends SandmawWyrm> type, Level level) {
		super(type, level, BossEvent.BossBarColor.YELLOW);
		this.xpReward = 30;
		setState(VEILED, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		EliteTier tier = SandmawRules.TIER;
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, tier.health)
			.add(Attributes.ARMOR, tier.armour)
			.add(Attributes.ARMOR_TOUGHNESS, tier.toughness)
			.add(Attributes.MOVEMENT_SPEED, 0.26)
			.add(Attributes.ATTACK_DAMAGE, tier.hit(6))
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	public EliteTier tier() {
		return SandmawRules.TIER;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MOVE, SandmawRules.BURROW);
		builder.define(TELL, 0);
		builder.define(BEGAN, 0L);
		builder.define(ROUSED, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.RING, Runes.TREMOR), List.of(Runes.BOLT, Runes.BLIND), List.of(Runes.TOUCH, Runes.PUSH));
	}

	// ------------------------------------------------------------------ what the client sees

	/** The move under way (see {@link SandmawRules}). */
	public int move() {
		return entityData.get(MOVE);
	}

	/** How long the tell of the move under way was given. */
	public int tell() {
		return entityData.get(TELL);
	}

	/** Whether it has thrashed at half health and fights on roused. */
	public boolean roused() {
		return entityData.get(ROUSED);
	}

	/** Ticks since the move under way began. */
	public float since(float partial) {
		return (float) (level().getGameTime() - entityData.get(BEGAN)) + partial;
	}

	/** How far a move's pose has eased in on the client, 0 to 1. */
	public float blend(int move, float partial) {
		return move < 0 || move >= MOVES ? 0 : Mth.lerp(partial, blendO[move], blend[move]);
	}

	/** Whether it is under the sand right now, out of reach. */
	public boolean burrowed() {
		int move = move();
		long since = level().getGameTime() - entityData.get(BEGAN);
		return move == SandmawRules.BURROW || move == SandmawRules.RUMBLE
			|| move == SandmawRules.DIVE && since >= SandmawRules.DIVE_TICKS / 2
			|| move == SandmawRules.BREACH && since < tell();
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			int move = move();
			for (int i = 0; i < MOVES; i++) {
				blendO[i] = blend[i];
				blend[i] = Mth.approach(blend[i], i == move ? 1 : 0, 0.25F);
			}
			// Roused, the sand streams off it in a little storm of its own.
			if (roused() && !burrowed() && random.nextInt(2) == 0) {
				double a = random.nextDouble() * Math.PI * 2, r = 1 + random.nextDouble() * 2.5;
				level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
					getX() + Math.cos(a) * r, getY() + random.nextDouble() * getBbHeight(), getZ() + Math.sin(a) * r,
					-Math.sin(a) * 0.2, 0.02, Math.cos(a) * 0.2);
			}
		}
	}

	// ------------------------------------------------------------------ the hunt

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!isAlive()) {
			return;
		}
		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target)
			&& distanceTo(target) < SandmawRules.FEEL_RANGE * 2;
		if (!hunting && target != null) {
			setTarget(null);
			target = null;
		}
		if (left > 0) {
			left--;
		}
		if (tickCount % 20 == 0 && pressed > 0) {
			pressed--;
		}
		geysers(level);
		switch (move()) {
			case SandmawRules.BURROW -> burrow(level, target);
			case SandmawRules.RUMBLE -> rumble(level);
			case SandmawRules.ERUPT -> {
				hold();
				if (left == 0) {
					settle();
				}
			}
			case SandmawRules.DIVE -> diving(level);
			case SandmawRules.SLAM -> slam(level, target);
			case SandmawRules.STUCK -> stuck(level);
			case SandmawRules.SPIT -> spit(level, target);
			case SandmawRules.GULP -> gulp(level, target);
			case SandmawRules.GULPED -> gulped(level);
			case SandmawRules.CHOKING -> choking(level);
			case SandmawRules.SINKHOLE -> sinkhole(level);
			case SandmawRules.GEYSER -> geyser(level, target);
			case SandmawRules.QUILLS -> quills(level);
			case SandmawRules.BREACH -> breach(level, target);
			case SandmawRules.BEACHED -> beached(level);
			case SandmawRules.THRASH -> thrash(level);
			default -> idle(level, target);
		}
		int move = move();
		if (!burrowed()) {
			up++;
		}
		long since = since();
		setState(VEILED, burrowed() || move == SandmawRules.DIVE);
		setState(WINDUP, move == SandmawRules.RUMBLE || telling(move, since));
		setState(GUARD, move == SandmawRules.ERUPT);
		setState(ACTING, !telling(move, since) && (move == SandmawRules.SLAM || move == SandmawRules.SPIT || move == SandmawRules.GULP
			|| move == SandmawRules.QUILLS || move == SandmawRules.BREACH));
		setState(ALT, move == SandmawRules.SPIT);
		setState(STUNNED, move == SandmawRules.STUCK || move == SandmawRules.CHOKING || move == SandmawRules.BEACHED);
	}

	/** Whether a move is still in its tell. */
	private boolean telling(int move, long since) {
		return since < tell() && (move == SandmawRules.SLAM || move == SandmawRules.SPIT || move == SandmawRules.GULP
			|| move == SandmawRules.SINKHOLE || move == SandmawRules.GEYSER || move == SandmawRules.QUILLS || move == SandmawRules.BREACH);
	}

	private void pose(int move, int tell, int ticks) {
		entityData.set(MOVE, move);
		entityData.set(TELL, tell);
		entityData.set(BEGAN, level().getGameTime());
		left = ticks;
		struck.clear();
		getNavigation().stop();
	}

	private long since() {
		return level().getGameTime() - entityData.get(BEGAN);
	}

	/** Between moves: a pause, quicker roused. */
	private void settle() {
		pose(SandmawRules.IDLE, 0, 0);
		rest = roused ? SandmawRules.ROUSED_REST : SandmawRules.REST;
	}

	/** Planted where it is. */
	private void hold() {
		getNavigation().stop();
		setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
	}

	private void face(Vec3 way) {
		float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
		setYRot(yaw);
		setYHeadRot(yaw);
		setYBodyRot(yaw);
	}

	/**
	 * Under the sand: feeling for footsteps, then swimming after them, a furrow of sand in its wake, to rise beneath; or, now and
	 * then, with its prey a way off, breaching out at them along a furrow instead.
	 */
	private void burrow(ServerLevel level, @Nullable LivingEntity target) {
		down++;
		if (target == null) {
			hold();
			if (tickCount % 10 == 0) {
				feel(level);
			}
			if (tickCount % 40 == 0) {
				sand(level, position(), 2, 0.4, 0.02);
			}
			return;
		}
		double across = target.position().subtract(position()).horizontalDistance();
		if (tickCount % 10 == 0 && SandmawRules.breaches(across, down, lastBreach, random.nextDouble())
			&& beginBreach(level, target, roused ? SandmawRules.ROUSED_BREACH_TELL : SandmawRules.BREACH_TELL, false)) {
			return;
		}
		if (SandmawRules.rises(across, down)) {
			lastBreach = false;
			pose(SandmawRules.RUMBLE, SandmawRules.RUMBLE_TICKS, SandmawRules.RUMBLE_TICKS);
			return;
		}
		if (getNavigation().isDone() || tickCount % 8 == 0) {
			getNavigation().moveTo(target, 1.4);
		}
		if (tickCount % 2 == 0) {
			sand(level, position(), 2, 0.3, 0.03);
		}
	}

	/** Whoever walks near enough, unless sneaking, it feels and hunts. */
	private void feel(ServerLevel level) {
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(SandmawRules.FEEL_RANGE),
			p -> p.isAlive() && p.onGround() && Targets.canHarm(this, p))) {
			double distance = distanceTo(player);
			if (distance < best && SandmawRules.feels(distance, player.isShiftKeyDown())) {
				nearest = player;
				best = distance;
			}
		}
		if (nearest != null) {
			setTarget(nearest);
			down = 0;
		}
	}

	/** The ground rumbling where it is about to burst up: step off it. */
	private void rumble(ServerLevel level) {
		hold();
		sand(level, position(), 6, 0.8, 0.05);
		ring(level, position(), SandmawRules.ERUPT_RADIUS, SAND, 14);
		if (tickCount % 4 == 0) {
			playSound(groundSound(level, position()), 1.2F, 0.5F);
		}
		if (left == 0) {
			erupt(level);
		}
	}

	/** Bursting up out of the sand: all about it struck and flung. */
	private void erupt(ServerLevel level) {
		surfaced();
		pose(SandmawRules.ERUPT, 0, SandmawRules.ERUPT_TICKS);
		sand(level, position(), 40, 1.2, 0.25);
		playSound(SoundEvents.RAVAGER_ROAR, 1.6F, 0.6F);
		playSound(groundSound(level, position()), 1.6F, 0.6F);
		double reach = SandmawRules.ERUPT_RADIUS + getBbWidth() / 2;
		for (LivingEntity victim : prey(level, reach + 1)) {
			if (victim.position().subtract(position()).horizontalDistance() <= reach + victim.getBbWidth() / 2) {
				strike(level, victim, SandmawRules.ERUPT_DAMAGE, position(), 0.4, SandmawRules.ERUPT_LAUNCH);
			}
		}
	}

	/** Just come up: the count of how long and how hard it has been up starts again. */
	private void surfaced() {
		up = 0;
		hurtSinceRise = 0;
		down = 0;
	}

	/** Back under the sand. */
	private void dive(ServerLevel level) {
		pose(SandmawRules.DIVE, 0, SandmawRules.DIVE_TICKS);
		down = 0;
		up = 0;
		playSound(SoundEvents.RAVAGER_STEP, 1.4F, 0.5F);
		playSound(groundSound(level, position()), 1.4F, 0.6F);
	}

	private void diving(ServerLevel level) {
		hold();
		sand(level, position(), 4, 0.6, 0.04);
		if (left == 0) {
			pose(SandmawRules.BURROW, 0, 0);
			down = 0;
		}
	}

	/** Up, between moves: a pause, then the next move, chosen; or back under. Half beaten, the thrash, once. */
	private void idle(ServerLevel level, @Nullable LivingEntity target) {
		hold();
		if (target == null) {
			if (up > 60) {
				dive(level);
			}
			return;
		}
		getLookControl().setLookAt(target, 30, 30);
		if (SandmawRules.thrashes(roused, getHealth(), getMaxHealth())) {
			pose(SandmawRules.THRASH, 0, SandmawRules.THRASH_TICKS);
			playSound(SoundEvents.RAVAGER_ROAR, 2F, 0.4F);
			return;
		}
		if (rest > 0) {
			rest--;
			return;
		}
		if (SandmawRules.dives(up, hurtSinceRise, getMaxHealth())) {
			dive(level);
			return;
		}
		int move = SandmawRules.choose(deck, distanceTo(target), crowded(level), random.nextDouble());
		if (move < 0) {
			dive(level);
			return;
		}
		deck.played(move);
		begin(level, move, target);
	}

	/** Two or more about it, or being struck again and again. */
	private boolean crowded(ServerLevel level) {
		return pressed >= 3 || level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(5),
			p -> p.isAlive() && Targets.canHarm(this, p)).size() >= 2;
	}

	private void begin(ServerLevel level, int move, LivingEntity target) {
		dir = flat(target.position().subtract(position()), dir);
		face(dir);
		switch (move) {
			case SandmawRules.SLAM -> {
				int tell = roused ? SandmawRules.ROUSED_SLAM_TELL : SandmawRules.SLAM_TELL;
				again = false;
				pose(move, tell, tell + 6);
				playSound(SoundEvents.RAVAGER_AMBIENT, 1.6F, 0.45F);
			}
			case SandmawRules.SPIT -> {
				gouts = roused ? 3 : 1;
				int tell = SandmawRules.SPIT_TELL;
				pose(move, tell, tell + gouts * SandmawRules.SPIT_GOUT + SandmawRules.SPIT_RECOVER);
				aim = target.getEyePosition().subtract(mouth()).normalize();
				playSound(SoundEvents.CAMEL_DASH_READY, 1.6F, 0.5F);
				playSound(SoundEvents.SAND_FALL, 1.4F, 0.6F);
			}
			case SandmawRules.GULP -> {
				int tell = roused ? SandmawRules.ROUSED_GULP_TELL : SandmawRules.GULP_TELL;
				spot = within(target.position(), SandmawRules.GULP_REACH);
				if (Math.abs(spot.y - getY()) > 4) {
					// Too far up or down to plunge on: nothing comes of it this time.
					settle();
					return;
				}
				pose(move, tell, tell + SandmawRules.GULP_PLUNGE + 1);
				playSound(SoundEvents.RAVAGER_ROAR, 1.4F, 0.8F);
				playSound(SoundEvents.CAMEL_EAT, 1.6F, 0.5F);
			}
			case SandmawRules.SINKHOLE -> {
				int pull = roused ? SandmawRules.ROUSED_SINK_PULL : SandmawRules.SINK_PULL;
				pose(move, SandmawRules.SINK_TELL, SandmawRules.SINK_TELL + pull + SandmawRules.SINK_RECOVER);
				playSound(SoundEvents.SNIFFER_DIGGING, 2F, 0.5F);
				playSound(SoundEvents.SAND_BREAK, 1.6F, 0.5F);
			}
			case SandmawRules.GEYSER -> {
				int count = roused ? SandmawRules.ROUSED_GEYSER_COUNT : SandmawRules.GEYSER_COUNT;
				int every = roused ? SandmawRules.ROUSED_GEYSER_EVERY : SandmawRules.GEYSER_EVERY;
				marked = 0;
				pose(move, SandmawRules.GEYSER_TELL, SandmawRules.GEYSER_TELL + (count - 1) * every + SandmawRules.GEYSER_FUSE + SandmawRules.GEYSER_RECOVER);
				playSound(SoundEvents.WARDEN_DIG, 1.6F, 0.7F);
			}
			case SandmawRules.QUILLS -> {
				quillStart = yawTo(target.position());
				pose(move, SandmawRules.QUILL_TELL, SandmawRules.QUILL_TELL + (roused ? SandmawRules.QUILL_AGAIN : 0) + 4 + SandmawRules.QUILL_RECOVER);
				playSound(SoundEvents.ARMADILLO_UNROLL_START, 2F, 0.5F);
			}
			default -> settle();
		}
	}

	// ------------------------------------------------------------------ the slam

	/**
	 * Reared up with its head swung round to follow its prey, the line before it scored in the sand, then slammed down along it
	 * to stick fast. Roused, it wrenches free and slams again at once if they are still near.
	 */
	private void slam(ServerLevel level, @Nullable LivingEntity target) {
		hold();
		long s = since();
		int tell = tell();
		boolean locked = s >= tell - SandmawRules.SLAM_LOCK;
		if (!locked && target != null) {
			dir = flat(target.position().subtract(position()), dir);
		}
		face(dir);
		if (s < tell && s % 2 == 0) {
			line(level, position(), dir, 1, SandmawRules.SLAM_LENGTH, locked ? MAW : SAND, 0.75);
		}
		if (s < tell && s % 5 == 0) {
			playSound(SoundEvents.SAND_STEP, 1.2F, 0.5F);
		}
		if (s == tell) {
			playSound(SoundEvents.RAVAGER_ATTACK, 1.8F, 0.55F);
			playSound(groundSound(level, position()), 1.8F, 0.6F);
			BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, ground(level, position()));
			for (double d = 1; d <= SandmawRules.SLAM_LENGTH; d += 0.75) {
				Vec3 p = position().add(dir.scale(d));
				level.sendParticles(dust, p.x, p.y + 0.2, p.z, 6, 0.5, 0.1, 0.5, 0.2);
			}
			Vec3 side = new Vec3(-dir.z, 0, dir.x);
			for (LivingEntity victim : prey(level, SandmawRules.SLAM_LENGTH + 1)) {
				Vec3 to = victim.position().subtract(position());
				if (SandmawRules.inLine(to.dot(dir), to.dot(side)) && Math.abs(to.y) < 2.5) {
					strike(level, victim, SandmawRules.SLAM_DAMAGE, position(), 0.3, 0.3);
				}
			}
		}
		if (left == 0) {
			if (roused && !again && target != null && distanceTo(target) <= SandmawRules.SLAM_LENGTH + 2) {
				again = true;
				dir = flat(target.position().subtract(position()), dir);
				pose(SandmawRules.SLAM, SandmawRules.SLAM_AGAIN_TELL, SandmawRules.SLAM_AGAIN_TELL + 6);
				playSound(SoundEvents.GRAVEL_BREAK, 1.6F, 0.6F);
				sand(level, position().add(dir.scale(2)), 16, 0.6, 0.15);
			} else {
				again = false;
				pose(SandmawRules.STUCK, 0, SandmawRules.STUCK_TICKS);
				playSound(SoundEvents.RAVAGER_STUNNED, 1.4F, 0.6F);
			}
		}
	}

	/** Its head stuck fast in the sand, tugging at it: hit it now. */
	private void stuck(ServerLevel level) {
		hold();
		long s = since();
		Vec3 head = position().add(dir.scale(getBbWidth() / 2 + 1.5));
		if (s % 6 == 0) {
			sand(level, head, 8, 0.5, 0.08);
			playSound(SoundEvents.GRAVEL_HIT, 1.2F, 0.6F + random.nextFloat() * 0.2F);
		}
		if (left == 0) {
			sand(level, head, 20, 0.8, 0.2);
			playSound(SoundEvents.SAND_BREAK, 1.6F, 0.7F);
			settle();
		}
	}

	// ------------------------------------------------------------------ the spit

	/**
	 * Its throat swelling as it draws a breath, the edges of the cone it will spit down scored on the ground, then a gout of sand
	 * down it: stinging, blinding and slowing. Roused, three gouts fanned one after another, each one's line marked during the
	 * one before.
	 */
	private void spit(ServerLevel level, @Nullable LivingEntity target) {
		hold();
		long s = since();
		int tell = tell();
		Vec3 mouth = mouth();
		if (s < tell - SandmawRules.SPIT_LOCK && target != null) {
			Vec3 to = target.getEyePosition().subtract(mouth);
			if (to.lengthSqr() > 1e-4) {
				aim = to.normalize();
				dir = flat(aim, dir);
			}
		}
		long into = s - tell;
		int k = into < 0 ? -1 : (int) (into / SandmawRules.SPIT_GOUT);
		Vec3 gout = k >= 0 && k < gouts ? fanned(k) : aim;
		face(flat(gout, dir));
		int next = into < 0 ? 0 : k + 1;
		if (next < gouts && s % 3 == 0) {
			Vec3 way = flat(fanned(next), dir);
			ParticleOptions mark = next == 0 ? SAND : SPINE_NEXT;
			double edge = Math.toDegrees(Math.acos(SandmawRules.SPIT_CONE));
			line(level, position(), way.yRot((float) Math.toRadians(edge)), 1.5, SandmawRules.SPIT_RANGE, mark, 1.2);
			line(level, position(), way.yRot((float) Math.toRadians(-edge)), 1.5, SandmawRules.SPIT_RANGE, mark, 1.2);
		}
		if (into < 0 && s % 4 == 0) {
			playSound(SoundEvents.SAND_FALL, 1.2F, 0.5F + s * 0.03F);
		}
		if (k >= 0 && k < gouts) {
			if (into % SandmawRules.SPIT_GOUT == 0) {
				struck.clear();
				playSound(SoundEvents.LLAMA_SPIT, 1.8F, 0.45F);
				playSound(SoundEvents.SAND_BREAK, 1.6F, 0.6F);
			}
			BlockParticleOption dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
			BlockParticleOption grit = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
			for (double d = 1; d <= SandmawRules.SPIT_RANGE; d += 1.5) {
				Vec3 p = mouth.add(gout.scale(d));
				double spread = 0.15 + d * 0.08;
				level.sendParticles(dust, p.x, p.y, p.z, 3, spread, spread, spread, 0.02);
			}
			for (int i = 0; i < 4; i++) {
				Vec3 v = gout.yRot((float) ((random.nextDouble() - 0.5) * 0.6)).add(0, (random.nextDouble() - 0.5) * 0.3, 0);
				level.sendParticles(grit, mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 0.9);
			}
			for (LivingEntity victim : prey(level, SandmawRules.SPIT_RANGE + 1)) {
				if (struck.contains(victim.getUUID())) {
					continue;
				}
				Vec3 to = victim.getEyePosition().subtract(mouth);
				double distance = to.length();
				double dot = distance < 1e-4 ? 1 : to.normalize().dot(gout);
				if (SandmawRules.inSpit(distance, dot) && hasLineOfSight(victim)) {
					struck.add(victim.getUUID());
					if (strike(level, victim, SandmawRules.SPIT_DAMAGE, mouth, 0.2, 0.1)) {
						victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), this);
						victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
					}
				}
			}
		}
		if (left == 0) {
			settle();
		}
	}

	/** The way of a gout: one straight down the aim, or roused, three fanned across it. */
	private Vec3 fanned(int k) {
		double off = gouts == 1 ? 0 : (k - 1) * SandmawRules.SPIT_FAN;
		return aim.yRot((float) Math.toRadians(off));
	}

	// ------------------------------------------------------------------ the gulp

	/**
	 * Rearing back over a spot marked on the ground before it, jaws gnashing, then plunging down on it maw first: whoever
	 * stands there is swallowed, and those about it knocked flying; nobody there and it chokes on a mouthful of sand.
	 */
	private void gulp(ServerLevel level, @Nullable LivingEntity target) {
		long s = since();
		int tell = tell();
		boolean locked = s >= tell - SandmawRules.GULP_LOCK;
		if (s < tell) {
			hold();
			if (!locked && target != null) {
				Vec3 next = within(target.position(), SandmawRules.GULP_REACH);
				if (Math.abs(next.y - getY()) <= 4) {
					spot = next;
				}
			}
			dir = flat(spot.subtract(position()), dir);
			face(dir);
			if (s % 2 == 0) {
				ring(level, spot, SandmawRules.GULP_RADIUS, locked ? MAW : SAND, 14);
				sand(level, spot, 2, SandmawRules.GULP_RADIUS / 2, 0.02);
			}
			if (s % 6 == 0) {
				playSound(SoundEvents.CAMEL_EAT, 1.4F, 0.5F);
			}
			if (s == tell - SandmawRules.GULP_LOCK) {
				playSound(SoundEvents.RAVAGER_ROAR, 1.6F, 0.9F);
			}
			return;
		}
		long into = s - tell;
		if (into == 0) {
			from = position();
			noPhysics = true;
			playSound(SoundEvents.PHANTOM_SWOOP, 1.6F, 0.5F);
		}
		if (into < SandmawRules.GULP_PLUNGE) {
			double t = (into + 1) / (double) SandmawRules.GULP_PLUNGE;
			Vec3 at = from.lerp(spot, t).add(0, Math.sin(t * Math.PI) * 2, 0);
			setPos(at.x, at.y, at.z);
			setDeltaMovement(Vec3.ZERO);
			face(dir);
			return;
		}
		noPhysics = false;
		setPos(spot.x, spot.y, spot.z);
		setDeltaMovement(Vec3.ZERO);
		playSound(SoundEvents.RAVAGER_ATTACK, 1.8F, 0.5F);
		playSound(groundSound(level, spot), 1.8F, 0.6F);
		sand(level, spot, 30, 1.2, 0.2);
		LivingEntity caught = null;
		double best = Double.MAX_VALUE;
		List<LivingEntity> near = prey(level, SandmawRules.GULP_SPLASH + 2);
		for (LivingEntity victim : near) {
			Vec3 to = victim.position().subtract(spot);
			double across = to.horizontalDistance();
			if (SandmawRules.swallows(across, to.y) && across < best) {
				caught = victim;
				best = across;
			}
		}
		for (LivingEntity victim : near) {
			Vec3 to = victim.position().subtract(spot);
			if (victim != caught && to.horizontalDistance() <= SandmawRules.GULP_SPLASH + victim.getBbWidth() / 2 && Math.abs(to.y) < 2.5) {
				strike(level, victim, SandmawRules.GULP_SPLASH_DAMAGE, spot, 0.7, 0.4);
			}
		}
		if (caught != null) {
			swallowed = caught.getUUID();
			broke = 0;
			pose(SandmawRules.GULPED, 0, SandmawRules.GULP_HOLD);
			playSound(SoundEvents.GENERIC_EAT.value(), 2F, 0.4F);
			caught.hurtServer(level, damageSources().mobAttack(this), tier().hit(SandmawRules.GULP_CHEW));
		} else {
			pose(SandmawRules.CHOKING, 0, SandmawRules.CHOKING_TICKS);
			playSound(SoundEvents.RAVAGER_STUNNED, 1.6F, 0.6F);
		}
	}

	/** Whoever it has swallowed, if they are still there to hold. */
	private @Nullable LivingEntity swallowed(ServerLevel level) {
		return swallowed != null && level.getEntity(swallowed) instanceof LivingEntity victim && victim.isAlive()
			&& victim.distanceTo(this) <= 6 ? victim : null;
	}

	/** Where whoever it has swallowed is held. */
	private Vec3 maw() {
		return position().add(dir.scale(getBbWidth() / 2 + 0.3)).add(0, getBbHeight() - 1.4, 0);
	}

	/**
	 * Its prey held fast in its maw and chewed now and then, until it spits them out hard; struck hard enough meanwhile, by them
	 * or anyone, it chokes and lets them go.
	 */
	private void gulped(ServerLevel level) {
		hold();
		LivingEntity victim = swallowed(level);
		if (victim == null) {
			swallowed = null;
			settle();
			return;
		}
		face(dir);
		Vec3 maw = maw();
		victim.setDeltaMovement(maw.subtract(victim.position()).scale(0.5));
		victim.needsSync = true;
		victim.resetFallDistance();
		long s = since();
		if (s % 4 == 0) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
				maw.x, maw.y + 0.5, maw.z, 4, 0.6, 0.4, 0.6, 0.02);
		}
		if (s > 0 && s % SandmawRules.GULP_CHEW_EVERY == 0) {
			victim.hurtServer(level, damageSources().mobAttack(this), tier().hit(SandmawRules.GULP_CHEW));
			victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0), this);
			playSound(SoundEvents.GENERIC_EAT.value(), 1.6F, 0.5F);
			level.sendParticles(MAW, maw.x, maw.y + 0.5, maw.z, 6, 0.4, 0.3, 0.4, 0);
		}
		if (left == 0) {
			spitOut(level, victim, SandmawRules.GULP_THROW, SandmawRules.GULP_THROW_UP);
			victim.hurtServer(level, damageSources().mobAttack(this), tier().hit(SandmawRules.GULP_SPAT));
			settle();
			rest = SandmawRules.GULP_RECOVER;
		}
	}

	private void spitOut(ServerLevel level, LivingEntity victim, double out, double lift) {
		swallowed = null;
		victim.setDeltaMovement(dir.x * out, lift, dir.z * out);
		victim.needsSync = true;
		playSound(SoundEvents.PLAYER_BURP, 2F, 0.5F);
		Vec3 maw = maw();
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
			maw.x, maw.y + 0.5, maw.z, 25, 0.6, 0.4, 0.6, 0.25);
	}

	/** Coughing up sand, or its prey: hit it now. */
	private void choking(ServerLevel level) {
		hold();
		long s = since();
		if (s % 3 == 0) {
			Vec3 mouth = mouth();
			level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
				mouth.x, mouth.y, mouth.z, 0, dir.x, -0.2, dir.z, 0.4);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
				mouth.x, mouth.y, mouth.z, 3, 0.3, 0.2, 0.3, 0.1);
		}
		if (s % 12 == 0) {
			playSound(SoundEvents.RAVAGER_STUNNED, 1.4F, 0.6F);
			playSound(SoundEvents.HUSK_HURT, 1.2F, 0.5F);
		}
		if (left == 0) {
			settle();
		}
	}

	// ------------------------------------------------------------------ the sinkhole

	/**
	 * Its head sunk in the sand, a wide ring marked about it, then drawing in: the sand all about slides toward its jaws and
	 * everyone on it with it, running only just outpacing it; at the end the jaws snap shut, the bite marked on the ground a
	 * moment before.
	 */
	private void sinkhole(ServerLevel level) {
		hold();
		long s = since();
		int tell = tell();
		int pull = roused ? SandmawRules.ROUSED_SINK_PULL : SandmawRules.SINK_PULL;
		if (s < tell) {
			if (s % 4 == 0) {
				ring(level, position(), SandmawRules.SINK_RADIUS, SAND, 40);
				playSound(SoundEvents.SAND_FALL, 1.4F, 0.5F);
			}
			sand(level, position(), 3, 1, 0.05);
			return;
		}
		long into = s - tell;
		if (into < pull) {
			for (LivingEntity victim : prey(level, SandmawRules.SINK_RADIUS + 1)) {
				Vec3 to = victim.position().subtract(position());
				if (!SandmawRules.drags(to.horizontalDistance(), to.y)) {
					continue;
				}
				Vec3 in = new Vec3(-to.x, 0, -to.z).normalize();
				victim.setDeltaMovement(in.x * SandmawRules.SINK_DRAG, victim.getDeltaMovement().y, in.z * SandmawRules.SINK_DRAG);
				victim.needsSync = true;
			}
			if (s % 2 == 0) {
				BlockParticleOption grit = new BlockParticleOption(ParticleTypes.BLOCK, ground(level, position()));
				double turn = s * 0.15;
				for (int i = 0; i < 10; i++) {
					double a = turn + i * Math.PI * 2 / 10, r = 1.5 + random.nextDouble() * (SandmawRules.SINK_RADIUS - 1.5);
					double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
					// Inward and round: the sand swirling down toward the jaws.
					level.sendParticles(grit, x, getY() + 0.1, z, 0, -Math.cos(a) - Math.sin(a) * 0.6, 0.05, -Math.sin(a) + Math.cos(a) * 0.6, 0.25);
				}
				ring(level, position(), SandmawRules.SINK_RADIUS, SAND, 24);
			}
			if (s % 5 == 0) {
				playSound(SoundEvents.SAND_BREAK, 1.6F, 0.4F + random.nextFloat() * 0.2F);
			}
			if (into >= pull - SandmawRules.SINK_MARK) {
				if (s % 2 == 0) {
					ring(level, position(), SandmawRules.SINK_JAWS, MAW, 16);
				}
				if (into == pull - SandmawRules.SINK_MARK) {
					playSound(SoundEvents.CAMEL_EAT, 1.8F, 0.4F);
				}
			}
			return;
		}
		if (into == pull) {
			playSound(SoundEvents.RAVAGER_ATTACK, 2F, 0.5F);
			playSound(SoundEvents.GENERIC_EAT.value(), 1.6F, 0.4F);
			sand(level, position(), 40, 1.5, 0.3);
			for (LivingEntity victim : prey(level, SandmawRules.SINK_JAWS + 1)) {
				Vec3 to = victim.position().subtract(position());
				if (SandmawRules.snaps(to.horizontalDistance(), to.y)) {
					strike(level, victim, SandmawRules.SINK_DAMAGE, position(), 0.4, 0.9);
				}
			}
		}
		if (s % 4 == 0) {
			sand(level, position(), 6, 0.8, 0.08);
		}
		if (left == 0) {
			settle();
		}
	}

	// ------------------------------------------------------------------ the geysers

	/**
	 * Its head driven into the sand, drumming, then geysers sent up one after another beneath its prey, each spot marked and
	 * bubbling a moment before it goes up: keep moving.
	 */
	private void geyser(ServerLevel level, @Nullable LivingEntity target) {
		hold();
		long s = since();
		int tell = tell();
		int count = roused ? SandmawRules.ROUSED_GEYSER_COUNT : SandmawRules.GEYSER_COUNT;
		int every = roused ? SandmawRules.ROUSED_GEYSER_EVERY : SandmawRules.GEYSER_EVERY;
		if (target != null) {
			dir = flat(target.position().subtract(position()), dir);
			face(dir);
		}
		if (s < tell) {
			if (s % 3 == 0) {
				sand(level, position().add(dir.scale(getBbWidth() / 2 + 1)), 6, 0.6, 0.1);
			}
			if (s % 4 == 0) {
				playSound(SoundEvents.WARDEN_DIG, 1.2F, 0.8F);
			}
			return;
		}
		long into = s - tell;
		if (into % every == 0 && marked < count) {
			marked++;
			if (target != null && target.position().subtract(position()).horizontalDistance() <= SandmawRules.GEYSER_REACH) {
				Vec3 at = floor(target.position());
				if (Math.abs(at.y - target.getY()) > 4) {
					at = target.position();
				}
				geysers.add(new Geyser(at, level.getGameTime() + SandmawRules.GEYSER_FUSE));
				sound(level, at, SoundEvents.SAND_BREAK, 1.4F, 0.6F);
				sound(level, at, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.2F, 0.6F);
			}
		}
		if (left == 0) {
			settle();
		}
	}

	/** The geysers marked: bubbling until they go up, flinging whoever still stands on them. Whatever it is doing. */
	private void geysers(ServerLevel level) {
		if (geysers.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		geysers.removeIf(g -> {
			Vec3 at = g.at();
			long fuse = g.when() - now;
			if (fuse > 0) {
				if (fuse % 2 == 0) {
					ring(level, at, SandmawRules.GEYSER_RADIUS, fuse <= 4 ? MAW : SAND, 10);
				}
				sand(level, at, 2, 0.4, 0.05);
				return false;
			}
			sound(level, at, SoundEvents.BREEZE_SHOOT, 1.4F, 0.6F);
			sound(level, at, groundSound(level, at), 1.6F, 0.6F);
			BlockParticleOption grit = new BlockParticleOption(ParticleTypes.BLOCK, ground(level, at));
			for (double h = 0; h < 4; h += 0.5) {
				level.sendParticles(grit, at.x, at.y + h, at.z, 6, 0.3, 0.2, 0.3, 0.12);
			}
			level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
				at.x, at.y + 4, at.z, 16, 0.8, 0.4, 0.8, 0.02);
			level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.3, at.z, 8, 0.4, 0.1, 0.4, 0.05);
			double r = SandmawRules.GEYSER_RADIUS;
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(at, r * 2 + 2, 4, r * 2 + 2),
				e -> e != this && e.isAlive() && Targets.canHarm(this, e))) {
				Vec3 to = victim.position().subtract(at);
				if (to.horizontalDistance() <= r + victim.getBbWidth() / 2 && to.y > -1 && to.y < 2) {
					strike(level, victim, SandmawRules.GEYSER_DAMAGE, at, 0, SandmawRules.GEYSER_LAUNCH);
				}
			}
			return true;
		});
	}

	// ------------------------------------------------------------------ the spines

	/**
	 * Its spines raised and rattling, the rays they will fly along scored on the ground all round, then loosed: stand in a gap.
	 * Roused, a second volley follows down the gaps of the first, its rays marked as soon as the first has flown.
	 */
	private void quills(ServerLevel level) {
		hold();
		long s = since();
		int tell = tell();
		double second = quillStart + 180.0 / SandmawRules.QUILL_RAYS;
		if (s < tell && s % 3 == 0) {
			rays(level, quillStart, SPINE);
		}
		if (s < tell && s % 4 == 0) {
			playSound(SoundEvents.ARMADILLO_SCUTE_DROP, 1.4F, 0.5F + random.nextFloat() * 0.3F);
		}
		if (s == tell) {
			volley(level, quillStart);
		}
		if (roused && s > tell && s < tell + SandmawRules.QUILL_AGAIN) {
			if (s % 3 == 0) {
				rays(level, second, SPINE_NEXT);
			}
			if (s % 4 == 0) {
				playSound(SoundEvents.ARMADILLO_SCUTE_DROP, 1.4F, 0.7F + random.nextFloat() * 0.3F);
			}
		}
		if (roused && s == tell + SandmawRules.QUILL_AGAIN) {
			volley(level, second);
		}
		if (left == 0) {
			settle();
		}
	}

	/** The rays a volley will fly along, scored on the ground. */
	private void rays(ServerLevel level, double start, ParticleOptions mark) {
		for (int i = 0; i < SandmawRules.QUILL_RAYS; i++) {
			line(level, position(), ahead(start + i * 360.0 / SandmawRules.QUILL_RAYS), 1.5, SandmawRules.QUILL_LENGTH, mark, 1);
		}
	}

	private void volley(ServerLevel level, double start) {
		struck.clear();
		playSound(SoundEvents.ARROW_SHOOT, 2F, 0.5F);
		playSound(SoundEvents.ARMADILLO_ROLL, 1.6F, 0.5F);
		for (int i = 0; i < SandmawRules.QUILL_RAYS; i++) {
			Vec3 way = ahead(start + i * 360.0 / SandmawRules.QUILL_RAYS);
			for (double d = 1; d <= SandmawRules.QUILL_LENGTH; d += 0.7) {
				Vec3 p = position().add(way.scale(d));
				level.sendParticles(ParticleTypes.CRIT, p.x, p.y + 0.9, p.z, 1, 0.05, 0.05, 0.05, 0);
			}
			level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.2, getZ(), 0, way.x, 0.02, way.z, 1.4);
		}
		for (LivingEntity victim : prey(level, SandmawRules.QUILL_LENGTH + 1)) {
			Vec3 to = victim.position().subtract(position());
			if (SandmawRules.quilled(yawTo(victim.position()), to.horizontalDistance(), to.y, start) && struck.add(victim.getUUID())) {
				strike(level, victim, SandmawRules.QUILL_DAMAGE, position(), 0.5, 0.2);
			}
		}
	}

	// ------------------------------------------------------------------ the breach

	/**
	 * From under the sand, a furrow run out toward its prey and on past them, to breach clean out along it; fails, and returns
	 * false, if the far end is much higher or lower than here.
	 */
	private boolean beginBreach(ServerLevel level, LivingEntity target, int tell, boolean back) {
		Vec3 to = target.position().subtract(position());
		Vec3 way = flat(to, dir);
		Vec3 end = floor(position().add(way.scale(SandmawRules.breachLength(to.horizontalDistance()))));
		if (Math.abs(end.y - getY()) > 5) {
			return false;
		}
		dir = way;
		from = position();
		this.to = end;
		lastBreach = true;
		again = back;
		pose(SandmawRules.BREACH, tell, tell + SandmawRules.BREACH_FLIGHT);
		face(dir);
		playSound(SoundEvents.WARDEN_DIG, 1.6F, 0.5F);
		return true;
	}

	/**
	 * The furrow running out from where it lies toward its prey, following them until a moment before, the far end marked,
	 * then a burst of sand where it lies, and it breaches out along the furrow in a great arc, striking all along it, and
	 * crashes down at the far end. Roused, it dives straight back in and breaches back at them once more.
	 */
	private void breach(ServerLevel level, @Nullable LivingEntity target) {
		long s = since();
		int tell = tell();
		if (s < tell) {
			hold();
			boolean locked = s >= tell - SandmawRules.BREACH_LOCK;
			if (!locked && target != null) {
				Vec3 off = target.position().subtract(from);
				Vec3 way = flat(off, dir);
				Vec3 end = floor(from.add(way.scale(SandmawRules.breachLength(off.horizontalDistance()))));
				if (Math.abs(end.y - from.y) <= 5) {
					dir = way;
					to = end;
				}
			}
			face(dir);
			if (s % 2 == 0) {
				BlockParticleOption grit = new BlockParticleOption(ParticleTypes.BLOCK, ground(level, from));
				double length = to.subtract(from).horizontalDistance();
				double reached = Math.min(length, length * (s + 4) / (double) (tell - SandmawRules.BREACH_LOCK + 4));
				for (double d = 0; d <= reached; d += 1) {
					Vec3 p = from.lerp(to, length < 1e-4 ? 0 : d / length);
					level.sendParticles(grit, p.x, p.y + 0.1, p.z, 2, 0.3, 0.05, 0.3, 0.05);
				}
				ring(level, to, SandmawRules.BREACH_CRASH, locked ? MAW : SAND, 16);
			}
			if (s % 6 == 0) {
				playSound(groundSound(level, from), 1.4F, 0.5F);
				sound(level, from.lerp(to, 0.5), SoundEvents.SAND_STEP, 1.4F, 0.5F);
			}
			return;
		}
		long into = s - tell;
		if (into == 0) {
			noPhysics = true;
			surfaced();
			playSound(SoundEvents.RAVAGER_ROAR, 2F, 0.6F);
			playSound(groundSound(level, from), 2F, 0.6F);
			sand(level, from, 40, 1.2, 0.3);
			for (LivingEntity victim : prey(level, SandmawRules.BREACH_BURST + 1)) {
				if (victim.position().subtract(from).horizontalDistance() <= SandmawRules.BREACH_BURST + victim.getBbWidth() / 2) {
					strike(level, victim, SandmawRules.BREACH_BURST_DAMAGE, from, 0.5, 0.6);
				}
			}
		}
		double t = Math.min(1, (into + 1) / (double) SandmawRules.BREACH_FLIGHT);
		Vec3 at = from.lerp(to, t).add(0, Math.sin(t * Math.PI) * SandmawRules.BREACH_ARC, 0);
		setPos(at.x, at.y, at.z);
		setDeltaMovement(Vec3.ZERO);
		face(dir);
		level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
			getX(), getY() + 1, getZ(), 5, 0.5, 0.8, 0.5, 0.02);
		// Its long body trails behind it down to the furrow: whoever stands on the furrow where it passes is struck.
		for (LivingEntity victim : prey(level, 2.5)) {
			Vec3 off = victim.position().subtract(position());
			if (off.horizontalDistance() <= 1.5 + victim.getBbWidth() / 2 && victim.getY() < getY() + getBbHeight()
				&& !struck.contains(victim.getUUID())) {
				struck.add(victim.getUUID());
				if (strike(level, victim, SandmawRules.BREACH_PASS_DAMAGE, position(), 0, 0.5)) {
					victim.setDeltaMovement(victim.getDeltaMovement().add(dir.x * 0.8, 0, dir.z * 0.8));
				}
			}
		}
		if (into + 1 < SandmawRules.BREACH_FLIGHT) {
			return;
		}
		noPhysics = false;
		setPos(to.x, to.y, to.z);
		playSound(SoundEvents.RAVAGER_ATTACK, 2F, 0.5F);
		playSound(groundSound(level, to), 2F, 0.5F);
		sand(level, to, 50, 1.5, 0.3);
		for (LivingEntity victim : prey(level, SandmawRules.BREACH_CRASH + 1)) {
			Vec3 off = victim.position().subtract(to);
			if (off.horizontalDistance() <= SandmawRules.BREACH_CRASH + victim.getBbWidth() / 2 && Math.abs(off.y) < 2.5) {
				strike(level, victim, SandmawRules.BREACH_CRASH_DAMAGE, to, 0.8, 0.6);
			}
		}
		if (roused && !again && target != null && beginBreach(level, target, SandmawRules.ROUSED_BREACH_TELL, true)) {
			return;
		}
		again = false;
		pose(SandmawRules.BEACHED, 0, SandmawRules.BEACHED_TICKS);
		playSound(SoundEvents.RAVAGER_STUNNED, 1.6F, 0.5F);
	}

	/** Lying beached where it came down, writhing to get back under: hit it now. */
	private void beached(ServerLevel level) {
		hold();
		long s = since();
		if (s % 4 == 0) {
			sand(level, position(), 6, 1.2, 0.08);
		}
		if (s % 10 == 0) {
			playSound(SoundEvents.RAVAGER_STUNNED, 1.4F, 0.5F);
		}
		if (left == 0) {
			settle();
		}
	}

	// ------------------------------------------------------------------ the thrash

	/** Half beaten: coiling, then a thrash that flings everyone near it away, and it fights on roused, quicker and fiercer. */
	private void thrash(ServerLevel level) {
		hold();
		long s = since();
		if (s < 8 && s % 2 == 0) {
			sand(level, position(), 10, 1, 0.1);
		}
		if (s == 8) {
			roused = true;
			entityData.set(ROUSED, true);
			playSound(SoundEvents.RAVAGER_ROAR, 2.2F, 0.35F);
			playSound(groundSound(level, position()), 2F, 0.5F);
			for (LivingEntity victim : prey(level, SandmawRules.THRASH_RADIUS + 1)) {
				Vec3 off = victim.position().subtract(position());
				if (off.horizontalDistance() <= SandmawRules.THRASH_RADIUS && Math.abs(off.y) < 3) {
					strike(level, victim, SandmawRules.THRASH_DAMAGE, position(), SandmawRules.THRASH_PUSH, 0.5);
				}
			}
		}
		if (s >= 8 && s <= 20) {
			ring(level, position(), 1 + (s - 8) / 12.0 * (SandmawRules.THRASH_RADIUS - 1), SAND, 24);
		}
		if (s > 8 && s % 4 == 0) {
			sand(level, position(), 12, 1.4, 0.15);
			playSound(SoundEvents.SAND_BREAK, 1.4F, 0.5F);
		}
		if (left == 0) {
			deck.clear();
			settle();
		}
	}

	// ------------------------------------------------------------------ reach, sand and sound

	/** Whoever it may strike within a reach of it. */
	private List<LivingEntity> prey(ServerLevel level, double range) {
		return level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range, 3, range),
			e -> e != this && e.isAlive() && Targets.canHarm(this, e));
	}

	/** A blow from it, the victim thrown away from a point and up; whether it landed. */
	private boolean strike(ServerLevel level, LivingEntity victim, double damage, Vec3 at, double push, double lift) {
		if (!victim.hurtServer(level, damageSources().mobAttack(this), tier().hit(damage))) {
			return false;
		}
		Vec3 away = flat(victim.position().subtract(at), dir).scale(push);
		victim.setDeltaMovement(victim.getDeltaMovement().add(away.x, lift, away.z));
		victim.needsSync = true;
		return true;
	}

	/** The way across the ground along a vector, or the fallback if it has none. */
	private static Vec3 flat(Vec3 v, Vec3 fallback) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1e-6 ? fallback : f.normalize();
	}

	/** The way across the ground at a yaw. */
	private static Vec3 ahead(double yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	private float yawTo(Vec3 at) {
		return (float) Math.toDegrees(Math.atan2(-(at.x - getX()), at.z - getZ()));
	}

	private Vec3 mouth() {
		return position().add(dir.scale(getBbWidth() / 2)).add(0, getBbHeight() * 0.85, 0);
	}

	/** A point brought within a reach of it across the ground, set down on the ground. */
	private Vec3 within(Vec3 at, double reach) {
		Vec3 off = at.subtract(position());
		double across = off.horizontalDistance();
		return floor(across <= reach ? at : position().add(off.x * reach / across, 0, off.z * reach / across));
	}

	/** A point set down on the ground. */
	private Vec3 floor(Vec3 at) {
		return new Vec3(at.x, level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z)), at.z);
	}

	private BlockState ground(ServerLevel level, Vec3 at) {
		BlockState state = level.getBlockState(BlockPos.containing(at).below());
		return state.isAir() ? Blocks.SAND.defaultBlockState() : state;
	}

	private SoundEvent groundSound(ServerLevel level, Vec3 at) {
		return ground(level, at).getSoundType().getBreakSound();
	}

	private void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
	}

	private void sand(ServerLevel level, Vec3 at, int count, double spread, double speed) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground(level, at)), at.x, at.y + 0.1, at.z, count, spread, 0.1, spread, speed);
	}

	/** A ring marked on the ground about a point. */
	private void ring(ServerLevel level, Vec3 at, double radius, ParticleOptions mark, int count) {
		double turn = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			double a = turn + i * Math.PI * 2 / count;
			level.sendParticles(mark, at.x + Math.cos(a) * radius, at.y + 0.15, at.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
		}
	}

	/** A line marked on the ground out along a way, from one distance to another. */
	private void line(ServerLevel level, Vec3 at, Vec3 way, double near, double far, ParticleOptions mark, double step) {
		for (double d = near; d <= far; d += step) {
			level.sendParticles(mark, at.x + way.x * d, at.y + 0.15, at.z + way.z * d, 1, 0.05, 0, 0.05, 0);
		}
	}

	// ------------------------------------------------------------------ out of reach under the sand

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		float dealt = SandmawRules.hurt(damage, burrowed(), move());
		if (dealt <= 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		float before = getHealth();
		boolean hit = super.hurtServer(level, source, dealt > 0 ? dealt : damage);
		if (hit) {
			float lost = Math.max(0, before - getHealth());
			hurtSinceRise += lost;
			pressed++;
			if (move() == SandmawRules.GULPED && isAlive()) {
				broke += lost;
				if (broke >= SandmawRules.GULP_BREAK) {
					LivingEntity victim = swallowed(level);
					if (victim != null) {
						spitOut(level, victim, 0.5, 0.4);
					}
					swallowed = null;
					pose(SandmawRules.CHOKING, 0, SandmawRules.CHOKING_TICKS);
					playSound(SoundEvents.RAVAGER_STUNNED, 1.6F, 0.6F);
				}
			}
			if (getTarget() == null && source.getEntity() instanceof LivingEntity attacker && Targets.canHarm(this, attacker)) {
				setTarget(attacker);
			}
		}
		return hit;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Roused", roused);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		roused = input.getBooleanOr("Roused", false);
		entityData.set(ROUSED, roused);
	}

	@Override
	public boolean isPickable() {
		return !burrowed() && super.isPickable();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return burrowed() ? null : SoundEvents.RAVAGER_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F + random.nextFloat() * 0.1F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RAVAGER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
	}
}
