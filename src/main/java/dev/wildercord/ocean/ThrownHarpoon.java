package dev.wildercord.ocean;

import dev.wildercord.aura.world.BossDamage;
import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thrown Tide-Cleaver Harpoon entity. Punctures submerged foes with bonus damage and reels them back
 * toward the wielder, or anchors into stone/coral and zips the diver toward the anchor when sneaking.
 */
public class ThrownHarpoon extends ThrowableItemProjectile {
	private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(ThrownHarpoon.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> ANCHORED = SynchedEntityData.defineId(ThrownHarpoon.class, EntityDataSerializers.BOOLEAN);
	private boolean dealtDamage;
	private int anchorTicks;
	private BlockPos anchorPos;

	public ThrownHarpoon(EntityType<? extends ThrownHarpoon> type, Level level) {
		super(type, level);
	}

	public ThrownHarpoon(Level level, LivingEntity thrower, ItemStack stack) {
		super(OceanEntities.THROWN_HARPOON, thrower, level);
		setItem(stack.copy());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(RETURNING, false);
		builder.define(ANCHORED, false);
	}

	@Override
	protected Item getDefaultItem() {
		return OceanItems.TIDE_CLEAVER_HARPOON;
	}

	@Override
	protected double getDefaultGravity() {
		return isReturning() || isAnchored() ? 0.0 : (isInWater() ? 0.008 : 0.03);
	}

	public boolean isReturning() {
		return entityData.get(RETURNING);
	}

	public boolean isAnchored() {
		return entityData.get(ANCHORED);
	}

	@Override
	public void tick() {
		Entity owner = getOwner();
		if (!level().isClientSide() && owner != null && distanceToSqr(owner) > OceanRules.HARPOON_MAX_RANGE * OceanRules.HARPOON_MAX_RANGE) {
			beginReturn();
		}

		if (isAnchored()) {
			setDeltaMovement(Vec3.ZERO);
			if (!level().isClientSide() && level() instanceof ServerLevel server) {
				anchorTicks++;
				if (owner instanceof LivingEntity living && living.isShiftKeyDown() && anchorTicks <= 30) {
					double[] v = OceanRules.computePullVector(
						living.getX(), living.getY(), living.getZ(),
						getX(), getY(), getZ(),
						OceanRules.HARPOON_PULL_MAX_SPEED
					);
					living.setDeltaMovement(v[0], v[1], v[2]);
					living.hurtMarked = true;
				}
				if (anchorTicks > 35 || owner == null || !owner.isAlive()
					|| (anchorPos != null && level().getBlockState(anchorPos).isAir())) {
					beginReturn();
				}
			}
			super.tick();
			return;
		}

		if (isReturning()) {
			if (owner == null || !owner.isAlive()) {
				if (!level().isClientSide()) discard();
				return;
			}
			noPhysics = true;
			Vec3 toOwner = owner.getEyePosition().subtract(0.0, 0.35, 0.0).subtract(position());
			double dist = toOwner.length();
			if (dist < 1.35) {
				if (!level().isClientSide()) {
					if (owner instanceof Player p && !p.getAbilities().instabuild) {
						ItemStack drop = getItem().isEmpty() ? new ItemStack(OceanItems.TIDE_CLEAVER_HARPOON) : getItem().copy();
						if (!p.getInventory().add(drop)) {
							p.spawnAtLocation(drop.getItem(), 1);
						}
					}
					level().playSound(null, owner.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.7f, 1.1f);
					discard();
				}
				return;
			}
			setDeltaMovement(toOwner.normalize().scale(Math.min(1.85, 0.65 + dist * 0.08)));
		}

		super.tick();

		if (level().isClientSide()) {
			if (isInWater() || tickCount % 2 == 0) {
				level().addParticle(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 0.0, 0.02, 0.0);
			}
		} else if (!dealtDamage && !isReturning() && tickCount > 80) {
			beginReturn();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		if (dealtDamage || isReturning()) return;
		Entity target = result.getEntity();
		Entity owner = getOwner();
		if (target == owner) return;

		if (level() instanceof ServerLevel server) {
			boolean wet = target.isInWaterOrRain() || target.isInWater();
			float dmg = wet ? OceanRules.HARPOON_SUBMERGED_BONUS_DAMAGE : OceanRules.HARPOON_RANGED_DAMAGE;
			target.hurt( damageSources().thrown(this, owner),  dmg);
			dealtDamage = true;

			if (target instanceof LivingEntity victim && owner != null && !BossDamage.boss(victim)) {
				double[] pull = OceanRules.computePullVector(
					victim.getX(), victim.getY(), victim.getZ(),
					owner.getX(), owner.getY(), owner.getZ(),
					OceanRules.HARPOON_REEL_TARGET_SPEED
				);
				victim.setDeltaMovement(pull[0], pull[1], pull[2]);
				victim.hurtMarked = true;
			}

			server.playSound(null, blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0f, 1.0f);
			server.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.04);
			beginReturn();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (isReturning() || isAnchored()) return;
		if (!level().isClientSide() && level() instanceof ServerLevel server) {
			anchorPos = result.getBlockPos();
			entityData.set(ANCHORED, true);
			setDeltaMovement(Vec3.ZERO);
			server.playSound(null, blockPosition(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 0.9f, 1.15f);
			server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.05);
			Entity owner = getOwner();
			if (owner instanceof ServerPlayer sp && sp.isShiftKeyDown()) {
				double[] v = OceanRules.computePullVector(
					sp.getX(), sp.getY(), sp.getZ(),
					getX(), getY(), getZ(),
					OceanRules.HARPOON_PULL_MAX_SPEED
				);
				sp.setDeltaMovement(v[0], v[1], v[2]);
				sp.hurtMarked = true;
			}
		}
	}

	private void beginReturn() {
		entityData.set(ANCHORED, false);
		entityData.set(RETURNING, true);
		noPhysics = true;
		dealtDamage = true;
	}

	@Override
	public void readAdditionalSaveData(CompoundTag inputTag) { McValueInput input = new McValueInput(inputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.readAdditionalSaveData(inputTag);
		dealtDamage = input.getBooleanOr("DealtDamage", false);
		anchorTicks = input.getIntOr("AnchorTicks", 0);
		Optional<Boolean> ret = input.read("Returning", com.mojang.serialization.Codec.BOOL);
		if (ret.orElse(false)) entityData.set(RETURNING, true);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag outputTag) { McValueOutput output = new McValueOutput(outputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.addAdditionalSaveData(outputTag);
		output.putBoolean("DealtDamage", dealtDamage);
		output.putInt("AnchorTicks", anchorTicks);
		output.putBoolean("Returning", isReturning());
	}
}
