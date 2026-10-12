package dev.wildercord.monster;

import com.mojang.math.Transformation;
import dev.wildercord.cast.BlockFx;
import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Timbermaul Bear: a huge old brown bear of the taigas, moss and ferns grown in the fur of its back and shoulder hump,
 * its shoulders scarred; the second of the biome elites (see {@link EliteTier}), a champion. It ambles about its woods
 * and leaves be whoever leaves it be.
 *
 * <p>Whoever comes near, unless sneaking, it stands up tall and huffs at: a warning. Back away and it drops down and lets
 * them be a good while; come closer, wait it out, or strike it, and it charges. Fighting, it never makes the same move twice
 * running, and each has its own tell and its own way out (see {@link TimbermaulRules}): the rake, the snap, the crush, the
 * pounce, the charge, the rolling log and, at whoever gets behind it, the dig. Between moves it stands planted a moment,
 * the time to get round it. The first time it is hurt to half it rears and roars, throwing everyone back, and fights on
 * roused: quicker, its moves in pairs, and a hug that pins whoever it falls on.</p>
 */
public class TimbermaulBear extends EliteMonster {
	private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(TimbermaulBear.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TELL = SynchedEntityData.defineId(TimbermaulBear.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SIDE = SynchedEntityData.defineId(TimbermaulBear.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN = SynchedEntityData.defineId(TimbermaulBear.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Boolean> ROUSED = SynchedEntityData.defineId(TimbermaulBear.class, EntityDataSerializers.BOOLEAN);
	private static final int MOVES = TimbermaulRules.MOVES;
	private static final DustParticleOptions MARK = new DustParticleOptions(0x6B4A2B, 1.3F), LOCKED = new DustParticleOptions(0xB0413E, 1.3F),
		MOSS = new DustParticleOptions(0x5E7F3A, 1.1F), DAZE = new DustParticleOptions(0xF2D45C, 0.9F);
	/** The log it sends rolling: this long and this thick. */
	private static final float LOG_SPAN = (float) (2 * TimbermaulRules.LOG_WIDTH), LOG_GIRTH = 0.9F;

	/** A log rolling down its lane. */
	private static final class Log {
		final Display.BlockDisplay shown;
		final Vec3 from, way;
		final double startY;
		final Set<UUID> hit = new HashSet<>();
		Vec3 at;
		double rolled;

		Log(Display.BlockDisplay shown, Vec3 from, Vec3 way) {
			this.shown = shown;
			this.from = from;
			this.way = way;
			this.startY = from.y;
			this.at = from;
		}
	}

	private final MoveDeck deck = new MoveDeck(TimbermaulRules.MEMORY);
	private final Set<UUID> struck = new HashSet<>();
	private final List<Log> logs = new ArrayList<>();
	private final float[] blend = new float[MOVES], blendO = new float[MOVES];
	private int left, rest, pressed, logsLeft;
	private long calmUntil;
	private double broke;
	private boolean roused, again;
	private Vec3 dir = new Vec3(0, 0, 1), spot = Vec3.ZERO, from = Vec3.ZERO;
	private @Nullable LivingEntity wary;
	private @Nullable UUID pinned;

	public TimbermaulBear(EntityType<? extends TimbermaulBear> type, Level level) {
		super(type, level, BossEvent.BossBarColor.RED);
		this.xpReward = 40;
	}

	public static AttributeSupplier.Builder createAttributes() {
		EliteTier tier = TimbermaulRules.TIER;
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, tier.health)
			.add(Attributes.ARMOR, tier.armour)
			.add(Attributes.ARMOR_TOUGHNESS, tier.toughness)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, tier.hit(TimbermaulRules.RAKE_DAMAGE))
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public EliteTier tier() {
		return TimbermaulRules.TIER;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MOVE, TimbermaulRules.IDLE);
		builder.define(TELL, 0);
		builder.define(SIDE, 1);
		builder.define(BEGAN, 0L);
		builder.define(ROUSED, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6) {
			@Override
			public boolean canUse() {
				return roaming() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return roaming() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F) {
			@Override
			public boolean canUse() {
				return roaming() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return roaming() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
			@Override
			public boolean canUse() {
				return roaming() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return roaming() && super.canContinueToUse();
			}
		});
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.REND), List.of(Runes.RING, Runes.TREMOR), List.of(Runes.SELF, Runes.WARCRY));
	}

	/** Whether it's ambling about its woods, fighting no one. */
	private boolean roaming() {
		return move() == TimbermaulRules.IDLE && getTarget() == null;
	}

	// ------------------------------------------------------------------ what the client sees

	/** The move under way (see {@link TimbermaulRules}). */
	public int move() {
		return entityData.get(MOVE);
	}

	/** How long the tell of the move under way was given. */
	public int tell() {
		return entityData.get(TELL);
	}

	/** Which side the rake comes from: 1 its right, -1 its left. */
	public int side() {
		return entityData.get(SIDE);
	}

	/** Whether it has roared at half health and fights on roused. */
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

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			int move = move();
			for (int i = 0; i < MOVES; i++) {
				blendO[i] = blend[i];
				blend[i] = Mth.approach(blend[i], i == move ? 1 : 0, 0.25F);
			}
			// Roused, its breath steams and moss shakes loose from its back.
			if (roused() && random.nextInt(3) == 0) {
				Vec3 look = ahead(yBodyRot);
				level().addParticle(ParticleTypes.CLOUD, getX() + look.x * 1.6, getY() + 1.5, getZ() + look.z * 1.6,
					look.x * 0.05, 0.02, look.z * 0.05);
			}
			if (roused() && random.nextInt(4) == 0) {
				level().addParticle(MOSS, getX() + (random.nextDouble() - 0.5) * 1.6, getY() + getBbHeight(),
					getZ() + (random.nextDouble() - 0.5) * 1.6, 0, -0.05, 0);
			}
		}
	}
	// ------------------------------------------------------------------ the fight

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!isAlive()) {
			return;
		}
		LivingEntity target = getTarget();
		if (target != null && (!target.isAlive() || !Targets.canHarm(this, target) || distanceTo(target) > TimbermaulRules.GIVE_UP)) {
			setTarget(null);
			target = null;
		}
		if (left > 0) {
			left--;
		}
		if (tickCount % 20 == 0 && pressed > 0) {
			pressed--;
		}
		logs(level);
		switch (move()) {
			case TimbermaulRules.BLUFF -> bluff(level, target);
			case TimbermaulRules.RAKE -> rake(level, target);
			case TimbermaulRules.SNAP -> snap(level, target);
			case TimbermaulRules.CRUSH -> crush(level, target);
			case TimbermaulRules.TOPPLED, TimbermaulRules.SPRAWLED, TimbermaulRules.DAZED -> lying(level);
			case TimbermaulRules.POUNCE -> pounce(level, target);
			case TimbermaulRules.CHARGE -> charge(level);
			case TimbermaulRules.LOG -> log(level, target);
			case TimbermaulRules.DIG -> dig(level);
			case TimbermaulRules.HUG -> hug(level, target);
			case TimbermaulRules.PINNED -> pinned(level);
			case TimbermaulRules.ROAR -> roar(level);
			default -> idle(level, target);
		}
		int move = move();
		boolean telling = since() < tell() && move != TimbermaulRules.BLUFF && move != TimbermaulRules.IDLE;
		setState(VEILED, roused);
		setState(WINDUP, telling);
		setState(ACTING, !telling && move != TimbermaulRules.IDLE && move != TimbermaulRules.BLUFF && !lyingDown(move));
		setState(ALT, move == TimbermaulRules.BLUFF || move == TimbermaulRules.ROAR);
		setState(STUNNED, lyingDown(move));
	}

	private static boolean lyingDown(int move) {
		return move == TimbermaulRules.TOPPLED || move == TimbermaulRules.SPRAWLED || move == TimbermaulRules.DAZED;
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

	/** Back on all fours, standing planted a moment: the time to get round it. */
	private void settle() {
		pose(TimbermaulRules.IDLE, 0, 0);
		rest = roused ? TimbermaulRules.ROUSED_REST : TimbermaulRules.REST;
		again = false;
		noPhysics = false;
	}

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

	private void faceTo(LivingEntity target) {
		dir = flat(target.position().subtract(position()), dir);
		face(dir);
	}

	private boolean crowded(ServerLevel level) {
		return pressed >= 3 || level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(5),
			p -> p.isAlive() && Targets.canHarm(this, p)).size() >= 2;
	}

	// ------------------------------------------------------------------ before the fight

	/** Ambling about: noticing whoever comes near (or, calmed, only whoever comes right up to it). */
	private void roam(ServerLevel level) {
		if (tickCount % 5 != 0) {
			return;
		}
		boolean calm = level.getGameTime() < calmUntil;
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(TimbermaulRules.NOTICE_RANGE),
			p -> p.isAlive() && Targets.canHarm(this, p))) {
			double distance = distanceTo(player);
			boolean near = calm ? TimbermaulRules.crowds(distance) : TimbermaulRules.notices(distance, player.isShiftKeyDown());
			if (distance < best && near && hasLineOfSight(player)) {
				nearest = player;
				best = distance;
			}
		}
		if (nearest == null) {
			return;
		}
		getNavigation().stop();
		if (calm) {
			fight(nearest);
		} else {
			wary = nearest;
			pose(TimbermaulRules.BLUFF, 0, 0);
			faceTo(nearest);
			playSound(SoundEvents.POLAR_BEAR_WARNING, 1.8F, 0.6F);
		}
	}

	/** Standing up tall, huffing at whoever came near: they back off and are let be, or it charges. */
	private void bluff(ServerLevel level, @Nullable LivingEntity target) {
		hold();
		if (target != null) {
			wary = null;
			settle();
			rest = 6;
			return;
		}
		if (wary == null || !wary.isAlive() || wary.level() != level || !Targets.canHarm(this, wary)) {
			wary = null;
			pose(TimbermaulRules.IDLE, 0, 0);
			return;
		}
		getLookControl().setLookAt(wary, 30, 30);
		long in = since();
		if (in % 20 == 0 && in > 0) {
			playSound(SoundEvents.POLAR_BEAR_WARNING, 1.6F, 0.6F + random.nextFloat() * 0.1F);
		}
		double distance = distanceTo(wary);
		if (TimbermaulRules.letsGo(distance)) {
			wary = null;
			calmUntil = level.getGameTime() + TimbermaulRules.CALM_TICKS;
			pose(TimbermaulRules.IDLE, 0, 0);
		} else if (TimbermaulRules.crowds(distance) || in >= TimbermaulRules.BLUFF_TICKS) {
			fight(wary);
			wary = null;
		}
	}

	private void fight(LivingEntity victim) {
		setTarget(victim);
		settle();
		rest = 6;
		playSound(SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.5F);
	}

	/** Between moves: closing in, standing planted a moment, then the next move off the deck. */
	private void idle(ServerLevel level, @Nullable LivingEntity target) {
		if (target == null) {
			roam(level);
			return;
		}
		getLookControl().setLookAt(target, 30, 30);
		if (TimbermaulRules.roars(roused, getHealth(), getMaxHealth())) {
			faceTo(target);
			pose(TimbermaulRules.ROAR, TimbermaulRules.ROAR_TELL, TimbermaulRules.ROAR_TICKS);
			playSound(SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.45F);
			return;
		}
		if (rest > 0) {
			rest--;
			hold();
			face(dir);
			return;
		}
		double distance = distanceTo(target);
		double dot = dir.dot(flat(target.position().subtract(position()), dir));
		int move = TimbermaulRules.choose(deck, distance, TimbermaulRules.behind(dot), crowded(level), roused, random.nextDouble());
		if (move < 0 || !begin(level, target, move)) {
			if (tickCount % 10 == 0 || getNavigation().isDone()) {
				getNavigation().moveTo(target, roused ? TimbermaulRules.ROUSED_CHASE : TimbermaulRules.CHASE);
			}
			dir = ahead(getYRot());
			return;
		}
		deck.played(move);
	}

	/** Starts a move off the deck, facing its prey (the dig keeps its back to them); false if it can't be made from here. */
	private boolean begin(ServerLevel level, LivingEntity target, int move) {
		if (move != TimbermaulRules.DIG) {
			faceTo(target);
		}
		from = position();
		again = false;
		switch (move) {
			case TimbermaulRules.RAKE -> {
				entityData.set(SIDE, random.nextBoolean() ? 1 : -1);
				int tell = roused ? TimbermaulRules.ROUSED_RAKE_TELL : TimbermaulRules.RAKE_TELL;
				pose(move, tell, tell + TimbermaulRules.RAKE_SWEEP + TimbermaulRules.RAKE_RECOVER);
				sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.2F, 0.85F);
			}
			case TimbermaulRules.SNAP -> {
				int tell = roused ? TimbermaulRules.ROUSED_SNAP_TELL : TimbermaulRules.SNAP_TELL;
				pose(move, tell, tell + TimbermaulRules.SNAP_LUNGE + TimbermaulRules.SNAP_RECOVER);
				sound(level, position(), SoundEvents.POLAR_BEAR_AMBIENT, 1.5F, 0.45F);
			}
			case TimbermaulRules.CRUSH -> {
				int tell = roused ? TimbermaulRules.ROUSED_CRUSH_TELL : TimbermaulRules.CRUSH_TELL;
				pose(move, tell, tell + TimbermaulRules.CRUSH_RECOVER);
				sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.8F, 0.5F);
			}
			case TimbermaulRules.POUNCE -> {
				spot = within(target.position(), TimbermaulRules.pounceLength(distanceTo(target)));
				if (Math.abs(spot.y - getY()) > 3 || !level.noCollision(this, getBoundingBox().move(spot.subtract(position())))) {
					return false;
				}
				int tell = roused ? TimbermaulRules.ROUSED_POUNCE_TELL : TimbermaulRules.POUNCE_TELL;
				pose(move, tell, tell + TimbermaulRules.POUNCE_FLIGHT + TimbermaulRules.POUNCE_RECOVER);
				sound(level, position(), SoundEvents.HOGLIN_ANGRY, 1.3F, 0.5F);
			}
			case TimbermaulRules.CHARGE -> {
				if (!onGround()) {
					return false;
				}
				int tell = roused ? TimbermaulRules.ROUSED_CHARGE_TELL : TimbermaulRules.CHARGE_TELL;
				pose(move, tell, tell + TimbermaulRules.CHARGE_RUN + TimbermaulRules.CHARGE_RECOVER);
				sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.8F, 0.55F);
			}
			case TimbermaulRules.LOG -> {
				int tell = roused ? TimbermaulRules.ROUSED_LOG_TELL : TimbermaulRules.LOG_TELL;
				logsLeft = roused ? 2 : 1;
				pose(move, tell, tell + TimbermaulRules.LOG_RECOVER + (roused ? TimbermaulRules.LOG_AGAIN : 0));
				sound(level, position(), SoundEvents.ROOTED_DIRT_BREAK, 1.4F, 0.6F);
			}
			case TimbermaulRules.DIG -> {
				int tell = roused ? TimbermaulRules.ROUSED_DIG_TELL : TimbermaulRules.DIG_TELL;
				pose(move, tell, tell + TimbermaulRules.DIG_SPRAY + TimbermaulRules.DIG_RECOVER);
				sound(level, position(), SoundEvents.POLAR_BEAR_AMBIENT, 1.4F, 0.6F);
			}
			case TimbermaulRules.HUG -> {
				spot = floor(position().add(dir.scale(TimbermaulRules.HUG_AHEAD)));
				pose(move, TimbermaulRules.HUG_TELL, TimbermaulRules.HUG_TELL + TimbermaulRules.HUG_FALL);
				sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.6F);
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/** The rake: a paw drawn back on one side and swept across its front; roused, the other paw backhands after. */
	private void rake(ServerLevel level, @Nullable LivingEntity target) {
		hold();
		long s = since();
		int tell = tell(), side = side();
		if (target != null && s < tell - 3) {
			faceTo(target);
		}
		face(dir);
		if (s < tell) {
			// The arc it will sweep, the side the paw starts from marked red.
			if (s % 3 == 0) {
				for (int a = -80; a <= 80; a += 20) {
					boolean start = side * a >= 50;
					particle(level, arcAt(a, TimbermaulRules.RAKE_REACH * 0.85), start || s >= tell - 3 ? LOCKED : MARK);
				}
			}
			return;
		}
		long into = s - tell;
		if (into < TimbermaulRules.RAKE_SWEEP) {
			double swept = TimbermaulRules.swept(into);
			if (into == 0) {
				sound(level, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.55F);
				sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.2F, 0.9F);
			}
			Vec3 paw = arcAt(side * (90 - swept), 2.2).add(0, 1.0, 0);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, paw.x, paw.y, paw.z, 1, 0, 0, 0, 0);
			Vec3 right = new Vec3(-dir.z, 0, dir.x);
			for (LivingEntity victim : prey(level, TimbermaulRules.RAKE_REACH + 1)) {
				Vec3 to = victim.position().subtract(position());
				double off = Math.toDegrees(Math.atan2(to.dot(right), to.dot(dir)));
				if (!struck.contains(victim.getUUID()) && TimbermaulRules.rakes(to.horizontalDistance(), off, side, swept)
					&& hit(level, victim, TimbermaulRules.RAKE_DAMAGE)) {
					struck.add(victim.getUUID());
					shove(victim, right.scale(-side * TimbermaulRules.RAKE_THROW).add(dir.scale(0.3)), 0.3);
					sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F);
				}
			}
			return;
		}
		if (roused && !again && into == TimbermaulRules.RAKE_SWEEP + 2) {
			// The backhand: the other paw, from the side the first one ended on.
			again = true;
			entityData.set(SIDE, -side);
			pose(TimbermaulRules.RAKE, TimbermaulRules.RAKE_BACK_TELL,
				TimbermaulRules.RAKE_BACK_TELL + TimbermaulRules.RAKE_SWEEP + TimbermaulRules.RAKE_RECOVER);
			sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.3F, 0.95F);
			return;
		}
		if (left == 0) {
			settle();
		}
	}

	/** The snap: head low and jaws open, a lunge down a short lane; roused, a second snap after. */
	private void snap(ServerLevel level, @Nullable LivingEntity target) {
		long s = since();
		int tell = tell();
		if (s < tell) {
			hold();
			if (target != null && s < tell - TimbermaulRules.SNAP_LOCK) {
				faceTo(target);
			}
			face(dir);
			from = position();
			if (s % 3 == 0) {
				line(level, from, dir, TimbermaulRules.SNAP_LENGTH, s >= tell - TimbermaulRules.SNAP_LOCK ? LOCKED : MARK);
			}
			return;
		}
		long into = s - tell;
		if (into < TimbermaulRules.SNAP_LUNGE) {
			face(dir);
			setDeltaMovement(dir.x * TimbermaulRules.SNAP_STRIDE, getDeltaMovement().y, dir.z * TimbermaulRules.SNAP_STRIDE);
			if (into == 0) {
				sound(level, position(), SoundEvents.FOX_BITE, 1.6F, 0.5F);
				sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.4F, 0.8F);
			}
			double travelled = position().subtract(from).dot(dir);
			double reach = Math.min(TimbermaulRules.SNAP_LENGTH, travelled + 2.5);
			Vec3 right = new Vec3(-dir.z, 0, dir.x);
			for (LivingEntity victim : prey(level, TimbermaulRules.SNAP_LENGTH + 2)) {
				Vec3 rel = victim.position().subtract(from);
				if (!struck.contains(victim.getUUID()) && TimbermaulRules.inLane(rel.dot(dir), rel.dot(right), reach, TimbermaulRules.SNAP_WIDTH)
					&& Math.abs(victim.getY() - getY()) < 2.5 && hit(level, victim, TimbermaulRules.SNAP_DAMAGE)) {
					struck.add(victim.getUUID());
					shove(victim, dir.scale(0.6), 0.25);
					sound(level, victim.position(), SoundEvents.FOX_BITE, 1.2F, 0.6F);
				}
			}
			return;
		}
		hold();
		if (roused && !again && into == TimbermaulRules.SNAP_LUNGE + 2) {
			again = true;
			pose(TimbermaulRules.SNAP, TimbermaulRules.SNAP_AGAIN_TELL,
				TimbermaulRules.SNAP_AGAIN_TELL + TimbermaulRules.SNAP_LUNGE + TimbermaulRules.SNAP_RECOVER);
			sound(level, position(), SoundEvents.POLAR_BEAR_AMBIENT, 1.5F, 0.55F);
			return;
		}
		if (left == 0) {
			settle();
		}
	}

	/** The crush: reared up tall, then both forepaws brought down on a fan of ground before it; roused, it rears again. */
	private void crush(ServerLevel level, @Nullable LivingEntity target) {
		hold();
		long s = since();
		int tell = tell();
		if (s < tell) {
			if (target != null && s < tell - 4) {
				faceTo(target);
			}
			face(dir);
			if (s % 3 == 0) {
				ParticleOptions mark = s >= tell - 4 ? LOCKED : MARK;
				for (int a = -70; a <= 70; a += 10) {
					particle(level, arcAt(a, TimbermaulRules.CRUSH_RADIUS), mark);
				}
				for (double r = 1; r < TimbermaulRules.CRUSH_RADIUS; r += 1) {
					particle(level, arcAt(-70, r), mark);
					particle(level, arcAt(70, r), mark);
				}
			}
			return;
		}
		if (s == tell) {
			sound(level, position(), SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.6F, 0.7F);
			sound(level, position(), groundSound(level, position()), 1.6F, 0.6F);
			for (int a = -60; a <= 60; a += 20) {
				for (double r = 1.5; r <= TimbermaulRules.CRUSH_RADIUS; r += 1.2) {
					dirt(level, arcAt(a, r), 3, 0.25, 0.12);
				}
			}
			Vec3 paws = position().add(dir.scale(1.6));
			level.sendParticles(ParticleTypes.POOF, paws.x, paws.y + 0.2, paws.z, 10, 0.6, 0.1, 0.6, 0.05);
			for (LivingEntity victim : prey(level, TimbermaulRules.CRUSH_RADIUS + 1)) {
				Vec3 to = victim.position().subtract(position());
				if (TimbermaulRules.inFan(to.horizontalDistance(), dir.dot(flat(to, dir))) && Math.abs(to.y) < 2.5
					&& hit(level, victim, TimbermaulRules.CRUSH_DAMAGE)) {
					shove(victim, flat(to, dir).scale(0.4), TimbermaulRules.CRUSH_LIFT);
					victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, TimbermaulRules.CRUSH_SLOW, 1), this);
				}
			}
			return;
		}
		if (roused && !again && s == tell + 4) {
			again = true;
			pose(TimbermaulRules.CRUSH, TimbermaulRules.CRUSH_AGAIN_TELL, TimbermaulRules.CRUSH_AGAIN_TELL + TimbermaulRules.CRUSH_RECOVER);
			sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.8F, 0.55F);
			return;
		}
		if (left == 0) {
			settle();
		}
	}

	/** Where a pounce at its prey would come down, if it can get there: else null. */
	private @Nullable Vec3 leap(ServerLevel level, LivingEntity target) {
		Vec3 at = within(target.position(), TimbermaulRules.pounceLength(distanceTo(target)));
		boolean clear = Math.abs(at.y - getY()) <= 3 && level.noCollision(this, getBoundingBox().move(at.subtract(position())));
		return clear ? at : null;
	}

	/** The pounce: haunches down, then a leap on a ring where its prey stood; a miss leaves it sprawled. Roused, it leaps twice. */
	private void pounce(ServerLevel level, @Nullable LivingEntity target) {
		long s = since();
		int tell = tell();
		if (s < tell) {
			hold();
			if (target != null && s < tell - TimbermaulRules.POUNCE_LOCK) {
				Vec3 at = leap(level, target);
				if (at != null) {
					spot = at;
				}
			}
			dir = flat(spot.subtract(position()), dir);
			face(dir);
			from = position();
			if (s % 2 == 0) {
				ring(level, spot, TimbermaulRules.POUNCE_RADIUS, s >= tell - TimbermaulRules.POUNCE_LOCK ? LOCKED : MARK);
			}
			if (s % 4 == 0) {
				dirt(level, position().subtract(dir.scale(0.8)), 4, 0.4, 0.05);
			}
			return;
		}
		long into = s - tell;
		if (into < TimbermaulRules.POUNCE_FLIGHT) {
			if (into == 0) {
				noPhysics = true;
				sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.8F, 0.7F);
				dirt(level, from, 14, 0.6, 0.15);
			}
			double t = Math.min(1, (into + 1.0) / TimbermaulRules.POUNCE_FLIGHT);
			setPos(from.lerp(spot, t).add(0, Math.sin(t * Math.PI) * TimbermaulRules.POUNCE_ARC, 0));
			setDeltaMovement(Vec3.ZERO);
			face(dir);
			if (into == TimbermaulRules.POUNCE_FLIGHT - 1) {
				land(level, target);
			}
			return;
		}
		hold();
		if (left == 0) {
			settle();
		}
	}

	/** Coming down from a pounce: whoever is under it struck, then a second leap (roused), a sprawl (missed) or a breath. */
	private void land(ServerLevel level, @Nullable LivingEntity target) {
		noPhysics = false;
		setPos(spot);
		sound(level, spot, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.3F, 0.85F);
		sound(level, spot, groundSound(level, spot), 1.5F, 0.6F);
		dirt(level, spot, 30, 1.2, 0.2);
		boolean caught = false;
		for (LivingEntity victim : prey(level, TimbermaulRules.POUNCE_RADIUS + 1)) {
			Vec3 to = victim.position().subtract(spot);
			if (TimbermaulRules.inRing(to.horizontalDistance(), to.y, TimbermaulRules.POUNCE_RADIUS)
				&& hit(level, victim, TimbermaulRules.POUNCE_DAMAGE)) {
				shove(victim, flat(to, dir).scale(0.6), 0.4);
				caught = true;
			}
		}
		Vec3 next = roused && !again && target != null ? leap(level, target) : null;
		if (next != null) {
			again = true;
			spot = next;
			pose(TimbermaulRules.POUNCE, TimbermaulRules.POUNCE_AGAIN_TELL,
				TimbermaulRules.POUNCE_AGAIN_TELL + TimbermaulRules.POUNCE_FLIGHT + TimbermaulRules.POUNCE_RECOVER);
			sound(level, position(), SoundEvents.HOGLIN_ANGRY, 1.3F, 0.55F);
		} else if (!caught) {
			pose(TimbermaulRules.SPRAWLED, 0, TimbermaulRules.SPRAWLED_TICKS);
			sound(level, position(), SoundEvents.POLAR_BEAR_HURT, 1.3F, 0.5F);
		}
	}

	/** The charge: pawing the ground, then a run down a long line; led into a tree or a wall it stands dazed. Roused, it runs back. */
	private void charge(ServerLevel level) {
		long s = since();
		int tell = tell();
		LivingEntity target = getTarget();
		if (s < tell) {
			hold();
			if (target != null && s < tell - TimbermaulRules.CHARGE_LOCK) {
				faceTo(target);
			}
			face(dir);
			from = position();
			if (s % 3 == 0) {
				line(level, from, dir, TimbermaulRules.CHARGE_LENGTH, s >= tell - TimbermaulRules.CHARGE_LOCK ? LOCKED : MARK);
			}
			if (s % 5 == 0) {
				// Pawing the ground, a forefoot dragged back.
				dirt(level, position().add(dir.scale(1.0)), 6, 0.3, 0.12);
				sound(level, position(), groundSound(level, position()), 1.0F, 0.7F);
			}
			return;
		}
		long into = s - tell;
		double run = position().subtract(from).dot(dir);
		if (into < TimbermaulRules.CHARGE_RUN && run < TimbermaulRules.CHARGE_LENGTH) {
			face(dir);
			setDeltaMovement(dir.x * TimbermaulRules.CHARGE_SPEED, getDeltaMovement().y, dir.z * TimbermaulRules.CHARGE_SPEED);
			if (into == 0) {
				sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.55F);
			}
			if (into % 3 == 0) {
				dirt(level, position(), 6, 0.5, 0.1);
				sound(level, position(), SoundEvents.POLAR_BEAR_STEP, 1.2F, 0.6F);
			}
			Vec3 right = new Vec3(-dir.z, 0, dir.x);
			for (LivingEntity victim : prey(level, 3)) {
				Vec3 rel = victim.position().subtract(position());
				double along = rel.dot(dir), across = rel.dot(right);
				if (!struck.contains(victim.getUUID()) && along > -0.5 && along < 2.2 && Math.abs(across) <= TimbermaulRules.CHARGE_WIDTH
					&& Math.abs(rel.y) < 2.5 && hit(level, victim, TimbermaulRules.CHARGE_DAMAGE)) {
					struck.add(victim.getUUID());
					// Tossed off the side it stood to, and on along the way it runs.
					shove(victim, right.scale(across >= 0 ? 0.9 : -0.9).add(dir.scale(0.6)), 0.5);
					sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.6F);
				}
			}
			if (horizontalCollision && into > 2) {
				crash(level);
			}
			return;
		}
		hold();
		if (roused && !again && target != null && into <= TimbermaulRules.CHARGE_RUN + 1) {
			// A clean run, roused: it wheels about and runs back at them.
			again = true;
			faceTo(target);
			pose(TimbermaulRules.CHARGE, TimbermaulRules.CHARGE_AGAIN_TELL,
				TimbermaulRules.CHARGE_AGAIN_TELL + TimbermaulRules.CHARGE_RUN + TimbermaulRules.CHARGE_RECOVER);
			sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.8F, 0.6F);
			return;
		}
		if (left == 0) {
			settle();
		}
	}

	/** Run headlong into a tree or a wall: bark and stone fly, leaves come down, and it stands dazed. */
	private void crash(ServerLevel level) {
		hold();
		Vec3 front = position().add(dir.scale(1.4)).add(0, 1, 0);
		BlockState struckBlock = level.getBlockState(BlockPos.containing(front));
		if (!struckBlock.isAir()) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, struckBlock), front.x, front.y, front.z, 30, 0.5, 0.6, 0.5, 0.2);
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LEAVES.defaultBlockState()),
			front.x, front.y + 2.5, front.z, 25, 1.2, 0.6, 1.2, 0.05);
		sound(level, front, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1.6F, 0.6F);
		sound(level, front, SoundEvents.ANVIL_LAND, 0.6F, 0.5F);
		pose(TimbermaulRules.DAZED, 0, TimbermaulRules.DAZED_TICKS);
	}

	/** Toppled, sprawled or dazed: lying open, taking more from every blow. */
	private void lying(ServerLevel level) {
		hold();
		noPhysics = false;
		int move = move();
		if (move == TimbermaulRules.DAZED && tickCount % 3 == 0) {
			double a = tickCount * 0.5;
			level.sendParticles(DAZE, getX() + Math.cos(a) * 0.8, getY() + getBbHeight() + 0.3, getZ() + Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
		}
		if (move == TimbermaulRules.TOPPLED && tickCount % 8 == 0) {
			dirt(level, position(), 4, 0.8, 0.05);
		}
		if (move == TimbermaulRules.SPRAWLED && tickCount % 10 == 0) {
			level.sendParticles(ParticleTypes.CLOUD, getX() + dir.x * 1.6, getY() + 0.5, getZ() + dir.z * 1.6, 2, 0.1, 0.05, 0.1, 0.01);
		}
		if (left == 0) {
			settle();
		}
	}

	/** The log: claws dug in under a fallen trunk, heaved up and sent rolling down a lane (jump it). Roused, a second one after. */
	private void log(ServerLevel level, @Nullable LivingEntity target) {
		hold();
		long s = since();
		int tell = tell();
		long into = s - tell;
		// The lock comes before the first heave and, roused, again before the second.
		long until = into < 0 ? -into : TimbermaulRules.LOG_AGAIN - into;
		boolean aiming = into < 0 || (logsLeft > 0 && into > 0 && into < TimbermaulRules.LOG_AGAIN);
		if (aiming) {
			if (target != null && until > TimbermaulRules.LOG_LOCK) {
				faceTo(target);
			}
			face(dir);
			if (s % 3 == 0) {
				ParticleOptions mark = until <= TimbermaulRules.LOG_LOCK ? LOCKED : MARK;
				Vec3 right = new Vec3(-dir.z, 0, dir.x);
				Vec3 start = position().add(dir.scale(1.5));
				line(level, start.add(right.scale(TimbermaulRules.LOG_WIDTH)), dir, TimbermaulRules.LOG_LENGTH, mark);
				line(level, start.add(right.scale(-TimbermaulRules.LOG_WIDTH)), dir, TimbermaulRules.LOG_LENGTH, mark);
			}
			if (s % 4 == 0) {
				sound(level, position(), SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.7F);
				dirt(level, position().add(dir.scale(1.4)), 5, 0.6, 0.1);
			}
			return;
		}
		if (logsLeft > 0 && (into == 0 || into == TimbermaulRules.LOG_AGAIN)) {
			roll(level);
			return;
		}
		if (left == 0) {
			settle();
		}
	}

	/** A trunk heaved up and sent rolling down the lane before it. */
	private void roll(ServerLevel level) {
		logsLeft--;
		Vec3 start = floor(position().add(dir.scale(1.5)));
		sound(level, start, SoundEvents.WOOD_BREAK, 1.4F, 0.6F);
		sound(level, start, SoundEvents.POLAR_BEAR_WARNING, 1.6F, 0.7F);
		dirt(level, start, 20, 1.0, 0.15);
		Display.BlockDisplay shown = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (shown == null) {
			return;
		}
		shown.snapTo(start.x, start.y, start.z);
		shown.setBlockState(Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		shown.setTransformation(logShape(dir, 0));
		shown.setPosRotInterpolationDuration(1);
		BlockFx.fresh(shown);
		level.addFreshEntity(shown);
		logs.add(new Log(shown, start, dir));
	}

	/** A log lying across the way it rolls, its foot on the ground beneath, turned so far about its own length. */
	private static Transformation logShape(Vec3 way, double roll) {
		Quaternionf turn = new Quaternionf().rotateY((float) Math.atan2(way.x, way.z)).rotateX((float) roll);
		Vector3f half = turn.transform(new Vector3f(LOG_SPAN / 2, LOG_GIRTH / 2, LOG_GIRTH / 2));
		Vector3f shift = new Vector3f(0, LOG_GIRTH / 2, 0).sub(half);
		return new Transformation(shift, turn, new Vector3f(LOG_SPAN, LOG_GIRTH, LOG_GIRTH), new Quaternionf());
	}

	/** The logs it has sent rolling: on down their lanes, bowling over whoever they meet, until they crash or run out. */
	private void logs(ServerLevel level) {
		for (var it = logs.iterator(); it.hasNext(); ) {
			Log log = it.next();
			log.rolled += TimbermaulRules.LOG_SPEED;
			Vec3 next = floor(log.from.add(log.way.scale(log.rolled)));
			if (!log.shown.isAlive() || next.y > log.startY + 1.2 || log.rolled >= TimbermaulRules.LOG_LENGTH) {
				Vec3 at = log.at.add(0, 0.5, 0);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LOG.defaultBlockState()),
					at.x, at.y, at.z, 30, LOG_SPAN / 3, 0.3, LOG_SPAN / 3, 0.15);
				sound(level, at, SoundEvents.WOOD_BREAK, 1.4F, 0.7F);
				log.shown.discard();
				it.remove();
				continue;
			}
			log.at = next;
			log.shown.setPos(next.x, next.y, next.z);
			log.shown.setTransformationInterpolationDelay(0);
			log.shown.setTransformationInterpolationDuration(1);
			log.shown.setTransformation(logShape(log.way, log.rolled / (LOG_GIRTH / 2)));
			if (tickCount % 2 == 0) {
				dirt(level, next, 3, LOG_SPAN / 3, 0.05);
			}
			if (tickCount % 4 == 0) {
				sound(level, next, groundSound(level, next), 0.8F, 0.6F);
			}
			Vec3 right = new Vec3(-log.way.z, 0, log.way.x);
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
				log.shown.getBoundingBox().inflate(TimbermaulRules.LOG_WIDTH + 0.5, 2.5, TimbermaulRules.LOG_WIDTH + 0.5),
				e -> e != this && e.isAlive() && Targets.canHarm(this, e))) {
				Vec3 rel = victim.position().subtract(log.from);
				if (!log.hit.contains(victim.getUUID())
					&& TimbermaulRules.bowls(rel.dot(log.way), rel.dot(right), victim.getY() - next.y, log.rolled)
					&& hit(level, victim, TimbermaulRules.LOG_DAMAGE)) {
					log.hit.add(victim.getUUID());
					shove(victim, log.way.scale(0.8), 0.4);
					sound(level, victim.position(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.9F, 0.8F);
				}
			}
		}
	}

	private void dropLogs() {
		for (Log log : logs) {
			log.shown.discard();
		}
		logs.clear();
	}

	@Override
	public void remove(RemovalReason reason) {
		dropLogs();
		super.remove(reason);
	}

	/** The dig: its back on whoever got behind it, scrabbling, then earth flung back in a spray that blinds. */
	private void dig(ServerLevel level) {
		hold();
		long s = since();
		int tell = tell();
		LivingEntity target = getTarget();
		if (target != null && s < tell - 3) {
			// It keeps its back to them as they go round, its rump square on them.
			dir = flat(position().subtract(target.position()), dir);
		}
		face(dir);
		double cone = roused ? TimbermaulRules.ROUSED_DIG_CONE : TimbermaulRules.DIG_CONE;
		int half = (int) Math.toDegrees(Math.acos(-cone));
		if (s < tell) {
			if (s % 3 == 0) {
				ParticleOptions mark = s >= tell - 3 ? LOCKED : MARK;
				for (int a = 180 - half; a <= 180 + half; a += 10) {
					particle(level, arcAt(a, TimbermaulRules.DIG_RANGE), mark);
				}
				for (double r = 1.5; r < TimbermaulRules.DIG_RANGE; r += 1) {
					particle(level, arcAt(180 - half, r), mark);
					particle(level, arcAt(180 + half, r), mark);
				}
			}
			if (s % 3 == 1) {
				dirt(level, position().add(dir.scale(1.2)), 5, 0.4, 0.1);
			}
			if (s % 5 == 0) {
				sound(level, position(), SoundEvents.SNIFFER_DIGGING, 1.4F, 0.6F);
			}
			return;
		}
		long into = s - tell;
		if (into < TimbermaulRules.DIG_SPRAY) {
			if (into == 0) {
				sound(level, position(), SoundEvents.ROOTED_DIRT_BREAK, 1.6F, 0.5F);
				sound(level, position(), groundSound(level, position()), 1.6F, 0.6F);
			}
			if (into % 2 == 0) {
				BlockParticleOption earth = new BlockParticleOption(ParticleTypes.BLOCK, ground(level, position()));
				for (int i = 0; i < 8; i++) {
					double a = Math.toRadians(180 + (random.nextDouble() * 2 - 1) * half);
					Vec3 way = arcAt(Math.toDegrees(a), 1).subtract(position());
					level.sendParticles(earth, getX() - dir.x * 1.2, getY() + 0.6, getZ() - dir.z * 1.2, 0,
						way.x, 0.25 + random.nextDouble() * 0.2, way.z, 0.6 + random.nextDouble() * 0.4);
				}
			}
			for (LivingEntity victim : prey(level, TimbermaulRules.DIG_RANGE + 1)) {
				Vec3 to = victim.position().subtract(position());
				if (!struck.contains(victim.getUUID()) && TimbermaulRules.inSpray(to.horizontalDistance(), dir.dot(flat(to, dir)), roused)
					&& Math.abs(to.y) < 2.5 && hit(level, victim, TimbermaulRules.DIG_DAMAGE)) {
					struck.add(victim.getUUID());
					shove(victim, flat(to, dir).scale(0.3), 0.1);
					victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), this);
					victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0), this);
				}
			}
			return;
		}
		if (left == 0) {
			settle();
		}
	}

	/** The hug, roused: reared up wide, then falling forward on whoever stands before it and pinning them. */
	private void hug(ServerLevel level, @Nullable LivingEntity target) {
		hold();
		long s = since();
		int tell = tell();
		if (s < tell) {
			if (target != null && s < tell - 4) {
				faceTo(target);
				spot = floor(position().add(dir.scale(TimbermaulRules.HUG_AHEAD)));
			}
			face(dir);
			if (s % 2 == 0) {
				ring(level, spot, TimbermaulRules.HUG_RADIUS, s >= tell - 4 ? LOCKED : MARK);
			}
			return;
		}
		face(dir);
		if (s - tell == TimbermaulRules.HUG_FALL - 1) {
			sound(level, spot, groundSound(level, spot), 1.6F, 0.5F);
			dirt(level, spot, 20, 1.0, 0.1);
			LivingEntity caught = null;
			double best = Double.MAX_VALUE;
			for (LivingEntity victim : prey(level, TimbermaulRules.HUG_AHEAD + TimbermaulRules.HUG_RADIUS + 1)) {
				Vec3 to = victim.position().subtract(spot);
				double across = to.horizontalDistance();
				if (TimbermaulRules.inRing(across, to.y, TimbermaulRules.HUG_RADIUS) && across < best) {
					caught = victim;
					best = across;
				}
			}
			if (caught != null && hit(level, caught, TimbermaulRules.HUG_MAUL)) {
				pinned = caught.getUUID();
				broke = 0;
				pose(TimbermaulRules.PINNED, 0, TimbermaulRules.HUG_HOLD);
				sound(level, spot, SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.5F);
			} else {
				pose(TimbermaulRules.SPRAWLED, 0, TimbermaulRules.SPRAWLED_TICKS);
				sound(level, position(), SoundEvents.POLAR_BEAR_HURT, 1.3F, 0.5F);
			}
			return;
		}
		if (left == 0) {
			settle();
		}
	}

	private @Nullable LivingEntity held(ServerLevel level) {
		return pinned != null && level.getEntity(pinned) instanceof LivingEntity victim && victim.isAlive() && victim.distanceTo(this) <= 6
			? victim : null;
	}

	/** Lying on whoever it caught: holding them and mauling now and then, until they strike it off them or it throws them. */
	private void pinned(ServerLevel level) {
		hold();
		face(dir);
		LivingEntity victim = held(level);
		if (victim == null) {
			pinned = null;
			pose(TimbermaulRules.SPRAWLED, 0, TimbermaulRules.SPRAWLED_TICKS / 2);
			return;
		}
		victim.setDeltaMovement(spot.subtract(victim.position()).scale(0.5));
		victim.needsSync = true;
		victim.resetFallDistance();
		if (broke >= TimbermaulRules.HUG_BREAK) {
			// Struck hard enough, it rolls off them, sprawled.
			pinned = null;
			shove(victim, dir.scale(0.3), 0.2);
			pose(TimbermaulRules.SPRAWLED, 0, TimbermaulRules.SPRAWLED_TICKS);
			sound(level, position(), SoundEvents.POLAR_BEAR_HURT, 1.4F, 0.55F);
			return;
		}
		long s = since();
		if (s > 0 && s % TimbermaulRules.HUG_MAUL_EVERY == 0 && hit(level, victim, TimbermaulRules.HUG_MAUL)) {
			sound(level, spot, SoundEvents.POLAR_BEAR_WARNING, 1.3F, 0.7F);
			level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 0.5, victim.getZ(), 8, 0.3, 0.2, 0.3, 0.2);
		}
		if (left == 0) {
			pinned = null;
			shove(victim, dir.scale(TimbermaulRules.HUG_THROW), 0.5);
			sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 1.8F, 0.6F);
			settle();
		}
	}

	/** The roar at half health: reared up, then a roar that throws everyone near back and weakens them; it fights on roused. */
	private void roar(ServerLevel level) {
		hold();
		face(dir);
		long s = since();
		if (s < TimbermaulRules.ROAR_TELL && s % 3 == 0) {
			ring(level, position(), TimbermaulRules.ROAR_RADIUS, s >= TimbermaulRules.ROAR_TELL - 4 ? LOCKED : MARK);
		}
		if (s == TimbermaulRules.ROAR_TELL) {
			roused = true;
			entityData.set(ROUSED, true);
			sound(level, position(), SoundEvents.RAVAGER_ROAR, 2.0F, 0.7F);
			sound(level, position(), SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.45F);
			level.sendParticles(MOSS, getX(), getY() + 1.5, getZ(), 30, 1.6, 0.6, 1.6, 0.2);
			level.sendParticles(ParticleTypes.CLOUD, getX() + dir.x * 1.6, getY() + 2.2, getZ() + dir.z * 1.6, 20, 0.8, 0.4, 0.8, 0.1);
			for (LivingEntity victim : prey(level, TimbermaulRules.ROAR_RADIUS)) {
				if (victim.distanceTo(this) <= TimbermaulRules.ROAR_RADIUS) {
					shove(victim, flat(victim.position().subtract(position()), dir).scale(TimbermaulRules.ROAR_PUSH), 0.35);
					victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, TimbermaulRules.ROAR_WEAKNESS, 0), this);
				}
			}
		}
		if (left == 0) {
			deck.clear();
			settle();
		}
	}

	// ------------------------------------------------------------------ reach, marks and sound

	/** A point on the ground about it, at an angle off the way it faces (to its right positive) and a distance out. */
	private Vec3 arcAt(double degrees, double reach) {
		double a = Math.toRadians(degrees);
		Vec3 right = new Vec3(-dir.z, 0, dir.x);
		return floor(position().add(dir.scale(Math.cos(a) * reach)).add(right.scale(Math.sin(a) * reach)));
	}

	private void particle(ServerLevel level, Vec3 at, ParticleOptions mark) {
		level.sendParticles(mark, at.x, at.y + 0.15, at.z, 1, 0.05, 0, 0.05, 0);
	}

	/** A line marked on the ground out along a way. */
	private void line(ServerLevel level, Vec3 start, Vec3 way, double length, ParticleOptions mark) {
		for (double d = 0; d <= length; d += 0.8) {
			particle(level, floor(start.add(way.scale(d))), mark);
		}
	}

	/** A ring marked on the ground about a point. */
	private void ring(ServerLevel level, Vec3 at, double radius, ParticleOptions mark) {
		int count = Math.max(12, (int) (radius * 8));
		double turn = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			double a = turn + i * Math.PI * 2 / count;
			level.sendParticles(mark, at.x + Math.cos(a) * radius, at.y + 0.15, at.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
		}
	}

	/** Whoever it may strike within a reach of it. */
	private List<LivingEntity> prey(ServerLevel level, double range) {
		return level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range, 3, range),
			e -> e != this && e.isAlive() && Targets.canHarm(this, e));
	}

	/** A blow from it, scaled to its tier; whether it landed. */
	private boolean hit(ServerLevel level, LivingEntity victim, double damage) {
		return victim.hurtServer(level, damageSources().mobAttack(this), tier().hit(damage));
	}

	/** Whoever it struck thrown across the ground and up. */
	private static void shove(LivingEntity victim, Vec3 push, double lift) {
		victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, lift, push.z));
		victim.needsSync = true;
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
		return state.isAir() ? Blocks.PODZOL.defaultBlockState() : state;
	}

	private SoundEvent groundSound(ServerLevel level, Vec3 at) {
		return ground(level, at).getSoundType().getBreakSound();
	}

	private void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
	}

	private void dirt(ServerLevel level, Vec3 at, int count, double spread, double speed) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground(level, at)), at.x, at.y + 0.1, at.z, count, spread, 0.1, spread, speed);
	}

	// ------------------------------------------------------------------ blows

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		int move = move();
		boolean reared = (move == TimbermaulRules.CRUSH || move == TimbermaulRules.HUG) && since() < tell();
		float before = getHealth();
		boolean hit = super.hurtServer(level, source, TimbermaulRules.hurt(damage, move));
		if (!hit) {
			return false;
		}
		pressed++;
		if (isAlive() && TimbermaulRules.topples(damage, reared)) {
			// Struck hard while reared up tall, it goes over on its back.
			noPhysics = false;
			pose(TimbermaulRules.TOPPLED, 0, TimbermaulRules.TOPPLED_TICKS);
			playSound(SoundEvents.RAVAGER_STUNNED, 1.4F, 0.6F);
			dirt(level, position(), 24, 1.2, 0.15);
		} else if (move == TimbermaulRules.PINNED) {
			broke += Math.max(0, before - getHealth());
		}
		if (source.getEntity() instanceof LivingEntity attacker && Targets.canHarm(this, attacker)) {
			if (getTarget() == null) {
				setTarget(attacker);
			}
			wary = null;
		}
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
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

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.POLAR_BEAR_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.55F + random.nextFloat() * 0.1F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.POLAR_BEAR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.POLAR_BEAR_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 1.4F;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.POLAR_BEAR_STEP, 0.3F, 0.7F);
	}
}
