package dev.wildercord.monster;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A glob of resin lobbed by a Pitchback Beetle: whatever it lands on is gummed up and slowed, and a second glob while it's
 * still gummed sticks it fast where it stands. On the ground it just splats.
 */
public class ResinGlob extends ThrowableItemProjectile {
	public ResinGlob(EntityType<? extends ResinGlob> type, Level level) {
		super(type, level);
	}

	public ResinGlob(Level level, LivingEntity thrower) {
		super(MonsterContent.RESIN_GLOB, thrower, level, new ItemStack(Items.RESIN_CLUMP));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.RESIN_CLUMP;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 3) {
			ItemParticleOption resin = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(new ItemStack(getDefaultItem())));
			for (int i = 0; i < 12; i++) {
				level().addParticle(resin, getX(), getY(), getZ(), (random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.12,
					(random.nextDouble() - 0.5) * 0.2);
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		Entity entity = result.getEntity();
		if (level() instanceof ServerLevel level && entity instanceof LivingEntity living && entity != getOwner()) {
			living.hurtServer(level, damageSources().thrown(this, getOwner()), 2.0F);
			MobEffectInstance slow = living.getEffect(MobEffects.SLOWNESS);
			if (PitchbackRules.sticks(slow == null ? -1 : slow.getAmplifier())) {
				MonsterMagic.root(level, living, PitchbackRules.STUCK_TICKS);
			} else {
				living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PitchbackRules.GUM_TICKS, PitchbackRules.GUM_LEVEL), getEffectSource());
			}
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!level().isClientSide()) {
			level().playSound(null, getX(), getY(), getZ(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.HOSTILE, 0.9F, 0.7F);
			level().broadcastEntityEvent(this, (byte) 3);
			discard();
		}
	}
}
