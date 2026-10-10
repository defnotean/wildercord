package dev.wildercord.wildlife;

import dev.wildercord.content.MoteOption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A skyray: a great manta of the open sky, three blocks from wingtip to wingtip, gliding in slow loops high over
 * mountains, windswept hills and meadows. It climbs from the summit where it spawned to its cruise, then circles a
 * point that drifts across the land, leaning into its turns, its wings rippling now and then. The stars on its back
 * glow at night. Now and then a membrane comes loose and falls.
 *
 * <p>Struck, it climbs and leaves. It stays while anyone's within 96 blocks (it's meant to be seen) and goes like any
 * ambient creature beyond that.</p>
 */
public class Skyray extends AmbientCreature {
	/** The point it circles, which wanders. */
	private @Nullable Vec3 anchor;
	private double angle;
	/** +1 or -1: which way round it goes. */
	private int way = 1;
	private double radius = 18;
	/** 0 to 1, between its low and high cruise. */
	private double altitude = 0.5;
	/** Its wander: the way the anchor drifts, radians. */
	private double drift;
	private int membraneTime;
	/** Ticks left fleeing after being struck. */
	private int fleeing;

	public Skyray(EntityType<? extends Skyray> type, Level level) {
		super(type, level);
		setNoGravity(true);
		membraneTime = WildlifeRules.membraneInterval(random.nextDouble());
	}

	/** Client: the wing wave's phase and strength, and its lean into turns (this tick's and last tick's). */
	public float flapPhase, flapPhaseO, flapStrength, bank, bankO;
	/** Client: ticks left in a run of full strokes it gives now and then. */
	private int strokes;

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			flapPhaseO = flapPhase;
			bankO = bank;
			// Mostly it glides on a ripple; climbing, or now and then for a few beats, it strokes.
			if (strokes > 0) {
				strokes--;
			} else if (random.nextInt(500) == 0) {
				strokes = 60 + random.nextInt(60);
			}
			float climb = Mth.clamp((float) getDeltaMovement().y * 14, 0, 1);
			flapStrength = WildlifeRules.approach(flapStrength, Math.max(climb, strokes > 0 ? 0.85F : 0), 0.02F);
			flapPhase += 0.05F + 0.09F * flapStrength;
			bank = WildlifeRules.approach(bank, WildlifeRules.bank(Mth.wrapDegrees(getYRot() - yRotO)), 0.02F);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData group) {
		anchor = position();
		angle = random.nextDouble() * Mth.TWO_PI;
		way = random.nextBoolean() ? 1 : -1;
		radius = Mth.lerp(random.nextDouble(), WildlifeRules.SKYRAY_MIN_RADIUS, WildlifeRules.SKYRAY_MAX_RADIUS);
		altitude = random.nextDouble();
		drift = random.nextDouble() * Mth.TWO_PI;
		return super.finalizeSpawn(level, difficulty, reason, group);
	}

	/** Sets where it circles and at what height (tests and the tour hold it in view). */
	public void setLoop(Vec3 anchor, double radius, double altitude) {
		this.anchor = anchor;
		this.radius = radius;
		this.altitude = altitude;
	}

	// ------------------------------------------------------------------ flight

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (steered(level)) {
			return;
		}
		if (anchor == null) {
			anchor = position();
		}
		if (fleeing > 0) {
			fleeing--;
		}
		// The loop itself: a slow arc, a lap every half minute or more, breathing in and out a little.
		double speed = fleeing > 0 ? 0.4 : 0.17;
		angle += way * speed / Math.max(radius, 6);
		radius = Mth.clamp(radius + Math.sin(tickCount * 0.004 + getId()) * 0.02, WildlifeRules.SKYRAY_MIN_RADIUS, WildlifeRules.SKYRAY_MAX_RADIUS);
		altitude = Mth.clamp(altitude + Math.sin(tickCount * 0.0031 + getId() * 0.7) * 0.002 + (fleeing > 0 ? 0.004 : 0), 0, 1);
		// The anchor wanders across the land, so it isn't always over the same hill.
		drift += (random.nextDouble() - 0.5) * 0.02;
		anchor = anchor.add(Math.cos(drift) * 0.02, 0, Math.sin(drift) * 0.02);
		if (random.nextInt(2400) == 0) {
			way = -way;
		}

		double tx = anchor.x + Math.cos(angle) * radius;
		double tz = anchor.z + Math.sin(angle) * radius;
		int ground = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(tx), Mth.floor(tz)),
			level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPosition().getX(), blockPosition().getZ()));
		double ty = WildlifeRules.cruise(ground, altitude);
		Vec3 to = new Vec3(tx - getX(), ty - getY(), tz - getZ());
		Vec3 v = getDeltaMovement();
		double length = to.length();
		Vec3 wanted = length < 1e-4 ? Vec3.ZERO : to.scale(Math.min(speed, length * 0.1) / length);
		// Heavy and smooth: it eases toward its course, so turns are arcs and climbs are long.
		Vec3 next = v.add(wanted.subtract(v).scale(0.04));
		if (horizontalCollision || verticalCollision) {
			next = next.add(0, 0.05, 0);
			altitude = Math.min(1, altitude + 0.05);
		}
		setDeltaMovement(next);
		if (next.horizontalDistanceSqr() > 1e-5) {
			float yaw = (float) (Mth.atan2(next.z, next.x) * Mth.RAD_TO_DEG) - 90.0F;
			setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.1F);
		}
		setXRot((float) Mth.clamp(-next.y * 90, -20, 20));
		yBodyRot = getYRot();
		yHeadRot = getYRot();

		if (--membraneTime <= 0) {
			membraneTime = WildlifeRules.membraneInterval(random.nextDouble());
			if (level.getNearestPlayer(this, 64) != null) {
				shedMembrane(level);
			}
		}
	}

	/** A skyray that's flown some other way (a {@link BondedSkyray} carrying its rider) skips the loop; true when it has. */
	protected boolean steered(ServerLevel level) {
		return false;
	}

	/** Lets a membrane come loose, to fall to the ground below. */
	public ItemEntity shedMembrane(ServerLevel level) {
		ItemEntity membrane = new ItemEntity(level, getX(), getY() - 0.3, getZ(), new ItemStack(Wildlife.SKYRAY_MEMBRANE));
		membrane.setDeltaMovement(getDeltaMovement().scale(0.5));
		level.addFreshEntity(membrane);
		level.sendParticles(new MoteOption(MoteOption.GLOW, 0xA8D0FF, 0.1F, 40, 0, -0.01F, 0, 0.03F), getX(), getY(), getZ(), 8, 0.6, 0.1, 0.6, 0);
		return membrane;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() != null && anchor != null) {
			// Up and away from whoever did it.
			Vec3 away = position().subtract(source.getEntity().position()).multiply(1, 0, 1);
			if (away.lengthSqr() < 1e-4) {
				away = new Vec3(1, 0, 0);
			}
			anchor = position().add(away.normalize().scale(48));
			fleeing = 300;
		}
		return hurt;
	}

	/** It's meant to be seen: within 96 blocks of a player it stays, and beyond that it goes as ambient creatures do. */
	@Override
	public boolean removeWhenFarAway(double distanceSqr) {
		return distanceSqr > 96 * 96;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	// ------------------------------------------------------------------ sound

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return Wildlife.sound("skyray_call");
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return Wildlife.sound("skyray_hurt");
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return Wildlife.sound("skyray_death");
	}

	@Override
	public int getAmbientSoundInterval() {
		return 500;
	}

	/** Its call carries: loud enough to be heard from the ground far below. */
	@Override
	protected float getSoundVolume() {
		return 3.0F;
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (anchor != null) {
			output.putDouble("anchor_x", anchor.x);
			output.putDouble("anchor_y", anchor.y);
			output.putDouble("anchor_z", anchor.z);
		}
		output.putDouble("radius", radius);
		output.putDouble("altitude", altitude);
		output.putInt("way", way);
		output.putInt("membrane_time", membraneTime);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		double y = input.getDoubleOr("anchor_y", Double.NaN);
		if (!Double.isNaN(y)) {
			anchor = new Vec3(input.getDoubleOr("anchor_x", getX()), y, input.getDoubleOr("anchor_z", getZ()));
		}
		radius = input.getDoubleOr("radius", 18);
		altitude = input.getDoubleOr("altitude", 0.5);
		way = input.getIntOr("way", 1) < 0 ? -1 : 1;
		membraneTime = input.getIntOr("membrane_time", WildlifeRules.membraneInterval(random.nextDouble()));
	}
}
