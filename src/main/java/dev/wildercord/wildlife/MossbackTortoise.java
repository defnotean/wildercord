package dev.wildercord.wildlife;

import dev.wildercord.content.MoteOption;
import dev.wildercord.wildlife.WildlifeRules.Garden;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A mossback tortoise: huge, slow and patient, with a little garden growing on its shell. What grows there comes from
 * where it lives (blue orchids and a mushroom in swamp moss, a lily pad and a propagule in mangroves, ferns and a
 * flower in the jungle); one that wanders into another of those lands for long enough grows that land's garden.
 *
 * <p>Struck, it pulls into its shell for a few seconds and takes far less. It loves melon: it follows a slice and two
 * fed together raise a baby, whose shell is bare until it grows. Now and then an adult lets go of a Mossback Scute, and
 * a baby leaves one behind as it grows up.</p>
 */
public class MossbackTortoise extends Animal {
	private static final EntityDataAccessor<Byte> DATA_GARDEN = SynchedEntityData.defineId(MossbackTortoise.class, EntityDataSerializers.BYTE);
	/** Ticks left in its shell, synced for the model. */
	private static final EntityDataAccessor<Integer> DATA_HIDING = SynchedEntityData.defineId(MossbackTortoise.class, EntityDataSerializers.INT);

	private int scuteTime;
	/** A garden it's been living among, and for how many checks in a row. */
	private @Nullable Garden newGarden;
	private int newGardenChecks;
	/** Client: how far into its shell, eased (this tick's and last tick's). */
	public float hide, hideO;

	public MossbackTortoise(EntityType<? extends MossbackTortoise> type, Level level) {
		super(type, level);
		scuteTime = WildlifeRules.scuteInterval(random.nextDouble());
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.13).add(Attributes.ARMOR, 4.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_GARDEN, (byte) Garden.SWAMP.ordinal());
		builder.define(DATA_HIDING, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.2, this::isFood, false));
		goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	public Garden garden() {
		return Garden.byId(entityData.get(DATA_GARDEN));
	}

	public void setGarden(Garden garden) {
		entityData.set(DATA_GARDEN, (byte) garden.ordinal());
	}

	public int hiding() {
		return entityData.get(DATA_HIDING);
	}

	public boolean hidden() {
		return hiding() > 0;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData group) {
		Garden here = gardenAt(level, blockPosition());
		setGarden(here != null ? here : Garden.byId(random.nextInt(Garden.values().length)));
		return super.finalizeSpawn(level, difficulty, reason, group);
	}

	private static @Nullable Garden gardenAt(ServerLevelAccessor level, BlockPos pos) {
		return level.getBiome(pos).unwrapKey().map(k -> WildlifeRules.garden(k.identifier().toString())).orElse(null);
	}

	// ------------------------------------------------------------------ its shell

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		float taken = hidden() ? amount * WildlifeRules.SHELL_GUARD : amount;
		boolean hurt = super.hurtServer(level, source, taken);
		if (hurt && isAlive()) {
			boolean wasHidden = hidden();
			entityData.set(DATA_HIDING, WildlifeRules.SHELL_TICKS);
			getNavigation().stop();
			if (!wasHidden) {
				SoundEvent tuck = Wildlife.sound("tortoise_hide");
				if (tuck != null) {
					level.playSound(null, getX(), getY(), getZ(), tuck, SoundSource.NEUTRAL, 1.0F, isBaby() ? 1.4F : 1.0F);
				}
			}
		}
		return hurt;
	}

	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || hidden();
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			hideO = hide;
			// Quick to pull in, slow to come out again.
			hide = WildlifeRules.approach(hide, hidden() ? 1 : 0, hidden() ? 0.25F : 0.06F);
		}
		if (level() instanceof ServerLevel level && isAlive()) {
			if (hidden()) {
				entityData.set(DATA_HIDING, hiding() - 1);
				setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
			}
			if (!isBaby() && --scuteTime <= 0) {
				dropScute(level);
				scuteTime = WildlifeRules.scuteInterval(random.nextDouble());
			}
			if (tickCount % 1200 == 0) {
				tendGarden(level);
			}
		}
	}

	/** Lets a scute go, with a little puff of moss. */
	public void dropScute(ServerLevel level) {
		spawnAtLocation(level, new ItemStack(Wildlife.MOSSBACK_SCUTE));
		SoundEvent sound = Wildlife.sound("tortoise_scute");
		if (sound != null) {
			level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 0.8F, 1.0F);
		}
		level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 0.6, getZ(), 6, 0.5, 0.2, 0.5, 0);
	}

	/** A minute's look at where it is: long enough among another land's plants and its garden grows over to match. */
	private void tendGarden(ServerLevel level) {
		Garden here = gardenAt(level, blockPosition());
		if (here == null || here == garden()) {
			newGarden = null;
			newGardenChecks = 0;
			return;
		}
		if (here != newGarden) {
			newGarden = here;
			newGardenChecks = 0;
		}
		if (++newGardenChecks >= WildlifeRules.GARDEN_REGROW_CHECKS) {
			regrow(level, here);
		}
	}

	/** Grows a new garden on its shell. */
	public void regrow(ServerLevel level, Garden garden) {
		setGarden(garden);
		newGarden = null;
		newGardenChecks = 0;
		level.sendParticles(new MoteOption(MoteOption.GLOW, 0xB8F08C, 0.08F, 30, 0, 0.015F, 0, 0.03F), getX(), getY() + 1.1, getZ(), 10, 0.5, 0.2, 0.5, 0);
	}

	@Override
	protected void ageBoundaryReached() {
		super.ageBoundaryReached();
		// A baby's first scute, left behind as its shell hardens.
		if (!isBaby() && level() instanceof ServerLevel level) {
			dropScute(level);
		}
	}

	// ------------------------------------------------------------------ breeding

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.MELON_SLICE);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		MossbackTortoise baby = Wildlife.MOSSBACK_TORTOISE.create(level, EntitySpawnReason.BREEDING);
		if (baby != null) {
			Garden here = gardenAt(level, blockPosition());
			baby.setGarden(here != null ? here : (random.nextBoolean() || !(partner instanceof MossbackTortoise other) ? garden() : other.garden()));
		}
		return baby;
	}

	// ------------------------------------------------------------------ sound

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return hidden() ? null : Wildlife.sound("tortoise_grumble");
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return Wildlife.sound("tortoise_hurt");
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return Wildlife.sound("tortoise_death");
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		SoundEvent step = Wildlife.sound("tortoise_step");
		if (step != null) {
			playSound(step, 0.35F, isBaby() ? 1.5F : 1.0F);
		} else {
			super.playStepSound(pos, state);
		}
	}

	@Override
	public int getAmbientSoundInterval() {
		return 300;
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putByte("garden", (byte) garden().ordinal());
		output.putInt("scute_time", scuteTime);
		output.putInt("hiding", hiding());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setGarden(Garden.byId(input.getByteOr("garden", (byte) 0)));
		scuteTime = input.getIntOr("scute_time", WildlifeRules.scuteInterval(random.nextDouble()));
		entityData.set(DATA_HIDING, input.getIntOr("hiding", 0));
	}
}
