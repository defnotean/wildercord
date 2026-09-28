package dev.wildercord.familiar;

import dev.wildercord.cast.LeyWalker;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.world.LeyLines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * A wisp: a small creature of light, an orb with a soft trailing tail and two tiny eyes, tinted by
 * its element. Wild ones drift along ley lines at night and shy away from people; strike one with
 * magic of its own element (or feed it its food) a few times and it bonds, becoming a familiar
 * that floats at its owner's shoulder (see {@link Familiars}).
 *
 * <p>Wisps can't be hurt: they're mana given a shape. A familiar out with its owner isn't saved
 * with the world (it lives on the player, in {@link Bonds}, and is called out again as needed);
 * only one left waiting at a Wellstone is.</p>
 */
public class Wisp extends PathfinderMob implements OwnableEntity {
	public static final byte WILD = 0;
	public static final byte FOLLOW = 1;
	public static final byte STAY = 2;
	public static final byte WAITING = 3;

	private static final EntityDataAccessor<String> DATA_ELEMENT = SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_OWNER =
		SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
	private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.INT);
	/** The game time of its last flare (a cast, a chime): the client draws it brighter for a moment. */
	private static final EntityDataAccessor<Long> DATA_FLARE = SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.LONG);

	/** A wild wisp fades after this long (five minutes), and sooner once day comes. */
	private static final int WILD_LIFE = 6000;

	/** Its bond's id, once it's a familiar. */
	String bondId = "";
	/** Where a familiar told to stay hovers. */
	Vec3 home;
	/** Server only: taming so far, the last cast offered (so one cast counts once), and a fright. */
	private WispRules.Taming taming = WispRules.Taming.NONE;
	private Object lastCast;
	private int frightened;
	private int life;
	/** Ticks until its next little spell, and a lighter check for things that need no target. */
	int helpCooldown = 60;
	int watchCooldown;
	/** Set while the wisp names itself, so a name tag's rename can be told apart. */
	private boolean naming;

	public Wisp(EntityType<? extends Wisp> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 20, true);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 8.0)
			.add(Attributes.FLYING_SPEED, 0.6)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ELEMENT, "arcane");
		builder.define(DATA_OWNER, Optional.empty());
		builder.define(DATA_MODE, WILD);
		builder.define(DATA_LEVEL, 1);
		builder.define(DATA_FLARE, 0L);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FollowGoal());
		goalSelector.addGoal(1, new StayGoal());
		goalSelector.addGoal(2, new FleeGoal());
		goalSelector.addGoal(3, new DriftGoal());
	}

	// ------------------------------------------------------------------ what it is

	public String element() {
		return entityData.get(DATA_ELEMENT);
	}

	public void setElement(String element) {
		entityData.set(DATA_ELEMENT, WispRules.ELEMENTS.contains(element) ? element : "arcane");
	}

	public int color() {
		return RuneColors.element(element());
	}

	public byte mode() {
		return entityData.get(DATA_MODE);
	}

	void setMode(byte mode) {
		entityData.set(DATA_MODE, mode);
		if (mode == STAY || mode == WAITING) {
			home = position();
		}
	}

	public boolean wild() {
		return mode() == WILD;
	}

	/** Its level as a familiar (1-3), synced for how big and bright it's drawn. */
	public int familiarLevel() {
		return entityData.get(DATA_LEVEL);
	}

	void setFamiliarLevel(int level) {
		entityData.set(DATA_LEVEL, level);
	}

	public String bondId() {
		return bondId;
	}

	/** Game ticks since its last flare, for the renderer. */
	public float sinceFlare(float partial) {
		return level().getGameTime() - entityData.get(DATA_FLARE) + partial;
	}

	void flare() {
		entityData.set(DATA_FLARE, level().getGameTime());
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getOwnerReference() {
		return entityData.get(DATA_OWNER).orElse(null);
	}

	void setOwner(@Nullable UUID owner) {
		entityData.set(DATA_OWNER, Optional.ofNullable(owner).map(EntityReference::<LivingEntity>of));
	}

	public @Nullable UUID ownerId() {
		EntityReference<LivingEntity> ref = getOwnerReference();
		return ref == null ? null : ref.getUUID();
	}

	public boolean isOwnedBy(Player player) {
		return player.getUUID().equals(ownerId());
	}

	/** The owner if they're online (in any dimension), on the server. */
	public @Nullable ServerPlayer ownerPlayer() {
		UUID id = ownerId();
		return id == null || !(level() instanceof ServerLevel level) ? null : level.getServer().getPlayerList().getPlayer(id);
	}

	/** Sets the nameplate: its own name (from a name tag) and its owner's, or "Alex's Fire Wisp". */
	void updateName(String ownerName, String name) {
		Component element = Component.translatable("element.wildercord." + element());
		Component plate = name.isEmpty()
			? Component.translatable("entity.wildercord.wisp.familiar", ownerName, element).withColor(color())
			: Component.literal(name).withColor(color()).append(Component.literal(" · " + ownerName).withColor(0xB8B0C8));
		naming = true;
		try {
			setCustomName(plate);
		} finally {
			naming = false;
		}
		setCustomNameVisible(true);
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		// A name tag on a familiar renames it (the name is kept on its bond, the plate shows the owner too).
		if (!naming && name != null && !wild() && level() instanceof ServerLevel) {
			Familiars.rename(this, name.getString());
			return;
		}
		super.setCustomName(name);
	}

	// ------------------------------------------------------------------ taming

	/**
	 * A player offers the wisp magic (or food) of {@code offered}; {@code cast} is the spell's
	 * identity (so its several effects count once), or null for food. Returns what happened.
	 */
	public WispRules.Outcome offer(ServerPlayer player, String offered, @Nullable Object cast) {
		if (!wild() || !isAlive()) {
			return WispRules.Outcome.IGNORED;
		}
		if (cast != null) {
			if (cast == lastCast) {
				return WispRules.Outcome.IGNORED;
			}
			lastCast = cast;
		}
		WispRules.Step step = WispRules.offer(taming, player.getStringUUID(), offered, element(), level().getGameTime());
		taming = step.taming();
		switch (step.outcome()) {
			case PROGRESS -> Familiars.tamingProgress(this, player, taming.hits());
			case BONDED -> Familiars.bond(player, this);
			case SPOOKED -> {
				frightened = 60;
				Familiars.spooked(this, player, offered);
			}
			default -> { }
		}
		return step.outcome();
	}

	/** The player who has made a start on taming it, if any: it lets them come closer. */
	private boolean tamedBy(Player player) {
		return taming.hits() > 0 && taming.player().equals(player.getStringUUID());
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!wild() && isOwnedBy(player) && player.isShiftKeyDown()) {
			if (player instanceof ServerPlayer server) {
				Familiars.toggleStay(server, this);
			}
			return InteractionResult.SUCCESS;
		}
		if (wild() && !stack.isEmpty()) {
			String food = WispRules.foodElement(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
			if (!food.isEmpty()) {
				if (player instanceof ServerPlayer server) {
					WispRules.Outcome outcome = offer(server, food, null);
					if (outcome == WispRules.Outcome.PROGRESS || outcome == WispRules.Outcome.BONDED) {
						usePlayerItem(player, hand, stack);
					}
				}
				return InteractionResult.SUCCESS;
			}
		}
		return super.mobInteract(player, hand);
	}

	// ------------------------------------------------------------------ ticking

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			sparkle();
			return;
		}
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (frightened > 0) {
			frightened--;
		}
		if (wild()) {
			life++;
			if (WispRules.forgotten(taming, level.getGameTime())) {
				taming = WispRules.Taming.NONE;
			}
			// Wild wisps are passing things: they fade with the day, or after a while.
			if (life > WILD_LIFE || life > 600 && level.isBrightOutside() && random.nextInt(400) == 0) {
				Familiars.fade(this);
			}
			return;
		}
		Familiars.tickFamiliar(level, this);
	}

	/** A trail of motes in its colour, and now and then a particle of its element. */
	private void sparkle() {
		if (tickCount % 2 == 0) {
			Vec3 back = getDeltaMovement().scale(-2.0);
			level().addParticle(new DustParticleOptions(color(), 0.55F), getX() + back.x + (random.nextDouble() - 0.5) * 0.15,
				getY() + 0.22 + back.y + (random.nextDouble() - 0.5) * 0.15, getZ() + back.z + (random.nextDouble() - 0.5) * 0.15, 0, 0, 0);
		}
		if (random.nextInt(wild() ? 10 : 7) == 0) {
			level().addParticle(elementParticle(element()), getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 0.2 + (random.nextDouble() - 0.5) * 0.4,
				getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
		}
	}

	/** The small particle each element's wisp gives off, in its element's visual language. */
	public static ParticleOptions elementParticle(String element) {
		return switch (element) {
			case "fire" -> ParticleTypes.SMALL_FLAME;
			case "frost" -> ParticleTypes.SNOWFLAKE;
			case "storm" -> ParticleTypes.ELECTRIC_SPARK;
			case "wind" -> ParticleTypes.WHITE_ASH;
			case "earth" -> ParticleTypes.CRIT;
			case "life" -> ParticleTypes.CHERRY_LEAVES;
			case "void" -> ParticleTypes.REVERSE_PORTAL;
			default -> ParticleTypes.ENCHANT;
		};
	}

	/** Where a familiar floats: just above and behind its owner's right shoulder, bobbing a little. */
	Vec3 shoulder(LivingEntity owner) {
		double yaw = Math.toRadians(owner.yBodyRot);
		Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
		Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
		double bob = Math.sin(tickCount * 0.1) * 0.12;
		return owner.position().add(right.scale(0.75)).add(back.scale(0.3)).add(0, owner.getBbHeight() + 0.1 + bob, 0);
	}

	// ------------------------------------------------------------------ the goals

	/** A familiar follows at its owner's shoulder, flying round things, and blinks back to them if it falls far behind. */
	private final class FollowGoal extends Goal {
		private int repath;
		private int stuck;
		private double best;

		FollowGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return mode() == FOLLOW && getOwner() != null;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity owner = getOwner();
			if (owner == null) {
				return;
			}
			Vec3 target = shoulder(owner);
			double distance = position().distanceTo(target);
			if (distance > 14) {
				teleportTo(target.x, target.y, target.z);
				getNavigation().stop();
				stuck = 0;
				return;
			}
			if (distance > 4) {
				if (--repath <= 0) {
					repath = 10;
					getNavigation().moveTo(target.x, target.y, target.z, 1.8);
				}
				// No closer for three seconds: something's in the way, so it blinks over it.
				if (distance < best - 0.5) {
					best = distance;
					stuck = 0;
				} else if (++stuck > 60) {
					teleportTo(target.x, target.y, target.z);
					stuck = 0;
				}
			} else {
				getNavigation().stop();
				getMoveControl().setWantedPosition(target.x, target.y, target.z, Math.min(1.4, 0.4 + distance));
				best = distance;
				stuck = 0;
			}
			getLookControl().setLookAt(owner.getX() + owner.getLookAngle().x * 4, owner.getEyeY(), owner.getZ() + owner.getLookAngle().z * 4);
		}
	}

	/** Told to stay (or waiting at a Wellstone): it hovers where it was left. */
	private final class StayGoal extends Goal {
		StayGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return mode() == STAY || mode() == WAITING;
		}

		@Override
		public void tick() {
			if (home == null) {
				home = position();
			}
			double bob = Math.sin(tickCount * 0.08) * 0.15;
			if (position().distanceToSqr(home.x, home.y + bob, home.z) > 0.04) {
				getMoveControl().setWantedPosition(home.x, home.y + bob, home.z, 0.5);
			}
		}
	}

	/** A wild wisp keeps its distance from people: six blocks, or two and a half from someone sneaking, or who's taming it. */
	private final class FleeGoal extends Goal {
		private Player from;
		private int repath;

		FleeGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		private Player nearest() {
			Player player = level().getNearestPlayer(Wisp.this, 6.0);
			if (player == null || player.isSpectator()) {
				return null;
			}
			boolean calm = player.isShiftKeyDown() || tamedBy(player);
			return frightened > 0 || !calm || player.distanceTo(Wisp.this) < 2.5 ? player : null;
		}

		@Override
		public boolean canUse() {
			if (!wild()) {
				return false;
			}
			from = nearest();
			return from != null;
		}

		@Override
		public boolean canContinueToUse() {
			return wild() && from != null && from.isAlive() && (frightened > 0 || from.distanceTo(Wisp.this) < 8);
		}

		@Override
		public void start() {
			repath = 0;
		}

		@Override
		public void tick() {
			if (--repath > 0) {
				return;
			}
			repath = 10;
			Vec3 away = position().subtract(from.position()).multiply(1, 0, 1);
			away = away.lengthSqr() < 1.0E-4 ? new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5) : away;
			Vec3 to = position().add(away.normalize().scale(6));
			double ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(to.x), Mth.floor(to.z));
			getNavigation().moveTo(to.x, Math.max(ground + 1.5, Math.min(ground + 4, to.y + 0.5)), to.z, frightened > 0 ? 2.2 : 1.6);
		}

		@Override
		public void stop() {
			from = null;
		}
	}

	/** A wild wisp drifts slowly, low over the ground, choosing wherever the ley line runs strongest nearby. */
	private final class DriftGoal extends Goal {
		DriftGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return wild() && getNavigation().isDone() && random.nextInt(30) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return wild() && !getNavigation().isDone();
		}

		@Override
		public void start() {
			long seed = level() instanceof ServerLevel level ? LeyWalker.seed(level) : 0;
			Vec3 best = null;
			double bestScore = -1;
			for (int i = 0; i < 8; i++) {
				double x = getX() + (random.nextDouble() - 0.5) * 24;
				double z = getZ() + (random.nextDouble() - 0.5) * 24;
				double score = (level().dimension() == Level.OVERWORLD ? LeyLines.strength(seed, x, z) : 0) + random.nextDouble() * 0.2;
				if (score > bestScore) {
					bestScore = score;
					double ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
					best = new Vec3(x, ground + 1.5 + random.nextDouble() * 2, z);
				}
			}
			if (best != null) {
				getNavigation().moveTo(best.x, best.y, best.z, 0.7);
			}
		}
	}

	// ------------------------------------------------------------------ never hurt, never saved by accident

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Mana given a shape: only the void or a kill command can end a wisp.
		if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) {
			return super.hurtServer(level, source, damage);
		}
		return false;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return wild();
	}

	@Override
	public boolean requiresCustomPersistence() {
		return !wild() || super.requiresCustomPersistence();
	}

	/** Only a familiar waiting at a Wellstone is kept with the world; the rest live on their owners, or not at all. */
	@Override
	public boolean shouldBeSaved() {
		return mode() == WAITING && super.shouldBeSaved();
	}

	@Override
	public void onRemoval(RemovalReason reason) {
		super.onRemoval(reason);
		if (!level().isClientSide()) {
			Familiars.forget(this);
		}
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return FamiliarContent.WISP_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	@Override
	protected float getSoundVolume() {
		return 0.45F;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return null;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return null;
	}

	// ------------------------------------------------------------------ saving (only ever a waiting familiar)

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("element", element());
		output.putByte("mode", mode());
		output.putString("bond", bondId);
		output.putInt("level", familiarLevel());
		EntityReference.store(getOwnerReference(), output, "owner");
		if (home != null) {
			output.putDouble("home_x", home.x);
			output.putDouble("home_y", home.y);
			output.putDouble("home_z", home.z);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setElement(input.getStringOr("element", "arcane"));
		entityData.set(DATA_MODE, input.getByteOr("mode", WILD));
		bondId = input.getStringOr("bond", "");
		setFamiliarLevel(input.getIntOr("level", 1));
		EntityReference<LivingEntity> owner = EntityReference.read(input, "owner");
		entityData.set(DATA_OWNER, Optional.ofNullable(owner));
		if (!Double.isNaN(input.getDoubleOr("home_y", Double.NaN))) {
			home = new Vec3(input.getDoubleOr("home_x", getX()), input.getDoubleOr("home_y", getY()), input.getDoubleOr("home_z", getZ()));
		}
	}
}
