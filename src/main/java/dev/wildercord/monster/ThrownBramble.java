package dev.wildercord.monster;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown Living Bramble: it unfurls on whatever creature it hits and holds it where it stands, as a Bramblewalker's
 * vines do (two seconds), with a prick of thorns. Thrown at the ground it just breaks apart.
 */
public class ThrownBramble extends ThrowableItemProjectile {
	static final int ROOT_TICKS = 40;

	public ThrownBramble(EntityType<? extends ThrownBramble> type, Level level) {
		super(type, level);
	}

	public ThrownBramble(Level level, LivingEntity thrower, ItemStack stack) {
		super(MonsterContent.THROWN_BRAMBLE, thrower, level, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return MonsterContent.LIVING_BRAMBLE;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 3) {
			ItemParticleOption leaves = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(new ItemStack(getDefaultItem())));
			for (int i = 0; i < 10; i++) {
				level().addParticle(leaves, getX(), getY(), getZ(), (random.nextDouble() - 0.5) * 0.15, random.nextDouble() * 0.15,
					(random.nextDouble() - 0.5) * 0.15);
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		Entity entity = result.getEntity();
		if (level() instanceof ServerLevel level && entity instanceof LivingEntity living) {
			living.hurtServer(level, damageSources().thrown(this, getOwner()), 1.0F);
			MonsterMagic.root(level, living, ROOT_TICKS);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!level().isClientSide()) {
			level().broadcastEntityEvent(this, (byte) 3);
			discard();
		}
	}
}
