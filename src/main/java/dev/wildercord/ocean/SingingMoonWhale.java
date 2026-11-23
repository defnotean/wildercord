package dev.wildercord.ocean;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Singing Moon-Whale: colossal peaceful indigo leviathan encrusted with emissive cyan star-barnacles.
 * Emits harmonic whale songs that bestow Water Breathing, Dolphin's Grace, and Regeneration to divers
 * and sheds Star-Barnacle Ambergris when fed fish/kelp or periodically after singing.
 */
public class SingingMoonWhale extends PathfinderMob {
	private static final EntityDataAccessor<Boolean> SINGING = SynchedEntityData.defineId(SingingMoonWhale.class, EntityDataSerializers.BOOLEAN);
	private int songCooldown = 80;
	private int songActiveTicks;
	private int ambergrisCooldown = 0;

	public SingingMoonWhale(EntityType<? extends SingingMoonWhale> type, Level level) {
		super(type, level);
		this.xpReward = 15;
		this.moveControl = new SmoothSwimmingMoveControl<>(this, 45, 10, 0.65F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
			.add(Attributes.MAX_HEALTH, OceanRules.MOON_WHALE_HP)
			.add(Attributes.MOVEMENT_SPEED, 0.24)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SINGING, false);
	}

	public boolean isSinging() {
		return entityData.get(SINGING);
	}

	public void setSinging(boolean singing) {
		this.songActiveTicks = singing ? 60 : 0;
		entityData.set(SINGING, singing);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.85, 60));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20.0F));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public void travel(Vec3 input) {
		if (isEffectiveAi() && isInWater()) {
			moveRelative(getSpeed(), input);
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.92));
		} else {
			super.travel(input);
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.COD) || held.is(Items.SALMON) || held.is(Items.KELP)) {
			if (level() instanceof ServerLevel server) {
				if (!player.getAbilities().instabuild) held.shrink(1);
				performWhaleSong(server);
				if (ambergrisCooldown <= 0) {
					shedAmbergris(server);
					ambergrisCooldown = 1200;
				}
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	protected void customServerAiStep(ServerLevel server) {
		super.customServerAiStep(server);
		if (ambergrisCooldown > 0) ambergrisCooldown--;

		if (songActiveTicks > 0) {
			songActiveTicks--;
			entityData.set(SINGING, songActiveTicks > 0);
		}
		if (--songCooldown <= 0) {
			songCooldown = OceanRules.MOON_WHALE_SONG_INTERVAL;
			performWhaleSong(server);
			if (ambergrisCooldown <= 0 && isInWater()) {
				shedAmbergris(server);
				ambergrisCooldown = OceanRules.MOON_WHALE_AMBERGRIS_INTERVAL;
			}
		}
	}

	private void performWhaleSong(ServerLevel server) {
		songActiveTicks = 50;
		entityData.set(SINGING, true);
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.55f);
		playSound(SoundEvents.CONDUIT_AMBIENT, 1.2f, 0.65f);
		server.sendParticles(ParticleTypes.GLOW, getX(), getY() + 1.2, getZ(), 18, 2.2, 0.8, 2.2, 0.03);
		for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(OceanRules.MOON_WHALE_SONG_RADIUS))) {
			p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, true, false, true));
			p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 180, 0, true, false, true));
			p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false, true));
		}
	}

	private void shedAmbergris(ServerLevel server) {
		spawnAtLocation(server, new ItemStack(OceanItems.STAR_BARNACLE_AMBERGRIS));
		server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 8, 0.8, 0.4, 0.8, 0.02);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.CONDUIT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ELDER_GUARDIAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ELDER_GUARDIAN_DEATH;
	}
}
