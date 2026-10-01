package dev.wildercord.wildlife;

import dev.wildercord.content.MoteOption;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A lumen stag: a rare, shy deer of old forests, its crystal antlers glowing brighter as the moon fills. It bolts from
 * anyone who walks up openly (or runs, sneaking or not), and stands and watches a calm, sneaking one. Stay close and
 * still a few seconds and it trusts you: it bows and sheds an antler at your feet. It sheds once a day, for whoever it
 * trusts first or, if nobody does, on its own while someone is near enough to find it.
 *
 * <p>A stag that dies leaves nothing (antlers are only ever shed, never taken), and killing one brings a curse of Bad
 * Luck on whoever did it, while every stag near falls frightened.</p>
 */
public class LumenStag extends Animal {
	/** Its pose, synced for the model: still, grazing, watching someone, bowing to shed, running. */
	public static final byte IDLE = 0;
	public static final byte GRAZING = 1;
	public static final byte WATCHING = 2;
	public static final byte BOWING = 3;

	private static final EntityDataAccessor<Byte> DATA_POSE = SynchedEntityData.defineId(LumenStag.class, EntityDataSerializers.BYTE);
	/** The day it last shed (-1: never), synced so its antler shows the gap until the next day. */
	private static final EntityDataAccessor<Long> DATA_SHED_DAY = SynchedEntityData.defineId(LumenStag.class, EntityDataSerializers.LONG);

	/** Ticks it stays frightened of everyone. */
	private int fright;
	private int poseTicks;
	/** Whose trust is building, and for how long. */
	private @Nullable UUID trusting;
	private int trust;
	/** Client: its poses eased in and out for the model (this tick's and last tick's, to blend between). */
	public float graze, grazeO, watch, watchO, bow, bowO;

	public LumenStag(EntityType<? extends LumenStag> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 20.0)
			.add(Attributes.STEP_HEIGHT, 1.1);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_POSE, IDLE);
		builder.define(DATA_SHED_DAY, -1L);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 2.4));
		goalSelector.addGoal(2, new FleeGoal(this, p -> stance(p) == WildlifeRules.Stance.FLEE, WildlifeRules.STAG_NOTICE, 1.7, 2.3));
		goalSelector.addGoal(3, new WatchGoal());
		goalSelector.addGoal(5, new GrazeGoal());
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.75));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	// ------------------------------------------------------------------ what it thinks of you

	/** Its stance toward one creature (only players matter to it). */
	public WildlifeRules.Stance stance(LivingEntity who) {
		if (!(who instanceof Player player) || player.isSpectator()) {
			return WildlifeRules.Stance.IGNORE;
		}
		// How far the player moved this tick, as their client last told it (a server's own idea of a player's motion is stale).
		double speed = player.getKnownMovement().horizontalDistance();
		return WildlifeRules.stagStance(distanceTo(player), player.isShiftKeyDown() && !player.isSprinting(), speed, fright > 0);
	}

	public byte pose() {
		return entityData.get(DATA_POSE);
	}

	/** Holds a pose for {@code ticks} (its own goals set these; tests and the guide's pictures may too). */
	public void setPoseState(byte pose, int ticks) {
		entityData.set(DATA_POSE, pose);
		poseTicks = ticks;
	}

	public long shedDay() {
		return entityData.get(DATA_SHED_DAY);
	}

	/** Whether it has shed today, so one antler is gone until tomorrow. */
	public boolean shedToday() {
		return shedDay() == WildlifeRules.day(level().getOverworldClockTime());
	}

	public boolean frightened() {
		return fright > 0;
	}

	/** Frightens it for a while: it trusts no one and bolts from everyone. */
	public void frighten(int ticks) {
		fright = Math.max(fright, ticks);
		trusting = null;
		trust = 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			byte pose = pose();
			grazeO = graze;
			watchO = watch;
			bowO = bow;
			graze = WildlifeRules.approach(graze, pose == GRAZING ? 1 : 0, 0.07F);
			watch = WildlifeRules.approach(watch, pose == WATCHING ? 1 : 0, 0.1F);
			bow = WildlifeRules.approach(bow, pose == BOWING ? 1 : 0, 0.08F);
		}
		if (level().isClientSide() && random.nextInt(5) == 0) {
			// Motes of its light rise from the antlers on a moonlit night.
			float night = WildlifeRules.night(level().getSkyDarken());
			int moon = dev.wildercord.spell.ClimateRules.moonPhase(level().getOverworldClockTime());
			if (night > 0.3F && random.nextFloat() < WildlifeRules.antlerGlow(moon, night)) {
				Vec3 head = headPosition();
				level().addParticle(new MoteOption(MoteOption.GLOW, 0xBFEFFF, 0.07F, 40, (random.nextFloat() - 0.5F) * 0.01F, 0.012F,
					(random.nextFloat() - 0.5F) * 0.01F, 0.02F), head.x + (random.nextFloat() - 0.5F) * 0.7, head.y + 0.55, head.z + (random.nextFloat() - 0.5F) * 0.7,
					0, 0, 0);
			}
		}
	}

	/** About where its head is now, for its antlers' light and the antler it sheds. */
	public Vec3 headPosition() {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		return position().add(-Mth.sin(yaw) * 0.85, 1.45, Mth.cos(yaw) * 0.85);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (fright > 0) {
			fright--;
		}
		if (poseTicks > 0 && --poseTicks == 0) {
			entityData.set(DATA_POSE, IDLE);
		}
		// Nobody earned its antler today: it lets it fall on its own, while someone is near enough to come and find it.
		if (tickCount % 200 == 0 && WildlifeRules.mayShed(shedDay(), today()) && random.nextInt(40) == 0
				&& level.getNearestPlayer(this, 48) != null) {
			shed(level, null);
		}
	}

	private long today() {
		return WildlifeRules.day(level().getOverworldClockTime());
	}

	/** Sheds today's antler, toward {@code toward} if someone earned it. Returns the antler, or null if it already shed today. */
	public @Nullable ItemEntity shed(ServerLevel level, @Nullable Player toward) {
		if (!WildlifeRules.mayShed(shedDay(), today())) {
			return null;
		}
		entityData.set(DATA_SHED_DAY, today());
		setPoseState(BOWING, 40);
		Vec3 head = headPosition();
		ItemEntity antler = new ItemEntity(level, head.x, head.y - 0.6, head.z, new ItemStack(Wildlife.LUMEN_ANTLER));
		if (toward != null) {
			Vec3 push = toward.position().subtract(head).normalize().scale(0.18);
			antler.setDeltaMovement(push.x, 0.18, push.z);
		} else {
			antler.setDeltaMovement(0, 0.1, 0);
		}
		antler.setPickUpDelay(10);
		level.addFreshEntity(antler);
		SoundEvent chime = Wildlife.sound("stag_shed");
		if (chime != null) {
			level.playSound(null, getX(), getY(), getZ(), chime, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		level.sendParticles(new MoteOption(MoteOption.GLOW, 0xCFF4FF, 0.12F, 36, 0, 0.02F, 0, 0.04F), head.x, head.y + 0.4, head.z, 14, 0.35, 0.3, 0.35, 0);
		return antler;
	}

	// ------------------------------------------------------------------ harm

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt) {
			frighten(2400);
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			// Killing one is wrong, and the forest knows it.
			if (source.getEntity() instanceof ServerPlayer killer) {
				killer.addEffect(new MobEffectInstance(MobEffects.UNLUCK, WildlifeRules.STAG_CURSE_TICKS, 0));
				killer.sendOverlayMessage(Component.translatable("message.wildercord.lumen_stag.curse"));
			}
			for (LumenStag other : level.getEntitiesOfClass(LumenStag.class, getBoundingBox().inflate(48), s -> s != this)) {
				other.frighten(6000);
			}
			level.sendParticles(new MoteOption(MoteOption.GLOW, 0x9FB8C8, 0.1F, 50, 0, -0.005F, 0, 0.02F), getX(), getY() + 1.2, getZ(), 20, 0.4, 0.5, 0.4, 0);
		}
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return 0;
	}

	// ------------------------------------------------------------------ a wild thing, never bred

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public boolean canFallInLove() {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	// ------------------------------------------------------------------ sound

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return Wildlife.sound("stag_call");
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return Wildlife.sound("stag_hurt");
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return Wildlife.sound("stag_death");
	}

	@Override
	public int getAmbientSoundInterval() {
		return 400;
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putLong("shed_day", shedDay());
		output.putInt("fright", fright);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_SHED_DAY, input.getLongOr("shed_day", -1L));
		fright = input.getIntOr("fright", 0);
	}

	// ------------------------------------------------------------------ goals

	/** Stands still for a calm player and watches them; one who stays close and still long enough is trusted with an antler. */
	private final class WatchGoal extends Goal {
		private @Nullable Player watched;

		WatchGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		private @Nullable Player find() {
			Player best = null;
			double nearest = Double.MAX_VALUE;
			for (Player player : level().players()) {
				WildlifeRules.Stance stance = stance(player);
				if (stance == WildlifeRules.Stance.WATCH || stance == WildlifeRules.Stance.TRUST) {
					double d = distanceToSqr(player);
					if (d < nearest) {
						nearest = d;
						best = player;
					}
				}
			}
			return best;
		}

		@Override
		public boolean canUse() {
			watched = find();
			return watched != null;
		}

		@Override
		public boolean canContinueToUse() {
			if (watched == null || !watched.isAlive()) {
				return false;
			}
			WildlifeRules.Stance stance = stance(watched);
			return stance == WildlifeRules.Stance.WATCH || stance == WildlifeRules.Stance.TRUST;
		}

		@Override
		public void start() {
			getNavigation().stop();
		}

		@Override
		public void stop() {
			watched = null;
			trusting = null;
			trust = 0;
			if (pose() == WATCHING) {
				entityData.set(DATA_POSE, IDLE);
			}
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (watched == null) {
				return;
			}
			getNavigation().stop();
			getLookControl().setLookAt(watched, 20, 20);
			if (pose() != BOWING) {
				setPoseState(WATCHING, 10);
			}
			if (stance(watched) != WildlifeRules.Stance.TRUST) {
				trust = Math.max(0, trust - 2);
				return;
			}
			if (!watched.getUUID().equals(trusting)) {
				trusting = watched.getUUID();
				trust = 0;
			}
			if (++trust >= WildlifeRules.STAG_TRUST_TICKS && level() instanceof ServerLevel level) {
				trust = 0;
				if (shed(level, watched) != null) {
					watched.sendOverlayMessage(Component.translatable("message.wildercord.lumen_stag.shed"));
				}
			}
		}
	}

	/** Lowers its head to graze now and then, when nothing's about. */
	private final class GrazeGoal extends Goal {
		private int left;

		GrazeGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return onGround() && random.nextInt(260) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return left > 0;
		}

		@Override
		public void start() {
			left = 60 + random.nextInt(60);
			getNavigation().stop();
			setPoseState(GRAZING, left);
		}

		@Override
		public void tick() {
			left--;
		}

		@Override
		public void stop() {
			left = 0;
			if (pose() == GRAZING) {
				entityData.set(DATA_POSE, IDLE);
			}
		}
	}
}
