package dev.wildercord.wildlife;

import dev.wildercord.content.MoteOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A bonded skyray (see {@link SkyMountRules}): one called down by a Skyray Bridle to carry its owner. Ridden, it flies
 * where its rider looks; left alone, it hovers a little while and then goes back to the sky. Only its owner may climb on,
 * and its rider can't strike it. Brought down, its bridle rests for five minutes.
 */
public class BondedSkyray extends Skyray {
	private @Nullable UUID owner;
	/** Ticks since anyone rode it. */
	private int unridden;

	public BondedSkyray(EntityType<? extends BondedSkyray> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Skyray.createAttributes().add(Attributes.MAX_HEALTH, 40.0);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
	}

	/**
	 * Calls {@code player}'s skyray down and puts them on it, sending off any other of theirs nearby. Returns null when they
	 * can't be carried now (already riding something).
	 */
	public static @Nullable BondedSkyray call(ServerLevel level, ServerPlayer player) {
		if (player.isPassenger()) {
			return null;
		}
		for (BondedSkyray old : level.getEntitiesOfClass(BondedSkyray.class, player.getBoundingBox().inflate(256), r -> player.getUUID().equals(r.owner))) {
			old.leave(level);
		}
		BondedSkyray ray = MountContent.BONDED_SKYRAY.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (ray == null) {
			return null;
		}
		ray.snapTo(player.getX(), player.getY() + 0.2, player.getZ(), player.getYRot(), 0);
		ray.setOwner(player.getUUID());
		level.addFreshEntity(ray);
		player.startRiding(ray);
		level.sendParticles(new MoteOption(MoteOption.GLOW, 0xA8D0FF, 0.12F, 40, 0, 0.01F, 0, 0.04F), ray.getX(), ray.getY() + 0.3, ray.getZ(),
			24, 1.2, 0.3, 1.2, 0);
		net.minecraft.sounds.SoundEvent call = Wildlife.sound("skyray_call");
		if (call != null) {
			level.playSound(null, ray.getX(), ray.getY(), ray.getZ(), call, SoundSource.NEUTRAL, 1.5F, 1.1F);
		}
		return ray;
	}

	/** Goes back to the sky. */
	public void leave(ServerLevel level) {
		level.sendParticles(new MoteOption(MoteOption.GLOW, 0xA8D0FF, 0.12F, 40, 0, 0.03F, 0, 0.04F), getX(), getY(), getZ(), 16, 1.0, 0.2, 1.0, 0);
		ejectPassengers();
		discard();
	}

	// ------------------------------------------------------------------ flight

	@Override
	protected boolean steered(ServerLevel level) {
		if (isVehicle()) {
			unridden = 0;
			return true;
		}
		// Riderless, it holds where it is, rising and falling a little on the air.
		setDeltaMovement(getDeltaMovement().scale(0.8).add(0, Math.sin(tickCount * 0.1) * 0.004, 0));
		if (++unridden > SkyMountRules.LINGER_TICKS) {
			leave(level);
		}
		return true;
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
	}

	@Override
	protected void tickRidden(Player player, Vec3 input) {
		super.tickRidden(player, input);
		setRot(player.getYRot(), player.getXRot() * 0.4F);
		yRotO = yBodyRot = yHeadRot = getYRot();
		if (canSimulateMovement()) {
			double[] wanted = SkyMountRules.steer(player.getYRot(), player.getXRot(), player.zza, player.xxa);
			Vec3 v = getDeltaMovement();
			setDeltaMovement(SkyMountRules.ease(v.x, wanted[0]), SkyMountRules.ease(v.y, wanted[1]), SkyMountRules.ease(v.z, wanted[2]));
		}
	}

	/** Its course is set in {@link #tickRidden}; the ordinary movement only carries it along. */
	@Override
	protected Vec3 getRiddenInput(Player player, Vec3 input) {
		return Vec3.ZERO;
	}

	@Override
	protected float getRiddenSpeed(Player player) {
		return (float) SkyMountRules.SPEED;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!isVehicle() && player.getUUID().equals(owner) && !player.isSecondaryUseActive()) {
			if (!level().isClientSide()) {
				player.startRiding(this);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty() && passenger instanceof Player;
	}

	/** A rider stepping off in the air drifts down rather than falls. */
	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (!level().isClientSide() && passenger instanceof LivingEntity living && isAlive()) {
			living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SkyMountRules.SLOW_FALL_TICKS, 0, false, false, true));
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity by = source.getEntity();
		if (by != null && (hasPassenger(by) || by.getUUID().equals(owner))) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level && owner != null && level.getPlayerByUUID(owner) instanceof Player rider) {
			rider.getCooldowns().addCooldown(new ItemStack(MountContent.SKYRAY_BRIDLE), SkyMountRules.FALLEN_COOLDOWN);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distanceSqr) {
		return false;
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (owner != null) {
			output.putString("owner", owner.toString());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		String id = input.getStringOr("owner", "");
		try {
			owner = id.isEmpty() ? null : UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			owner = null;
		}
	}
}
