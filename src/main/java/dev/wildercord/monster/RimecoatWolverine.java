package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Rimecoat Wolverine: a low, broad, shaggy wolverine of the snowy plains, dark brown with a pale band along each flank
 * and across its brow, its coat stiff with rime, and a thief.
 *
 * <p>It goes for your food. Close in it growls, then darts in, bites, and comes away with whatever you were eating, or
 * whatever food it can get its teeth into from your pack, a few at most, and runs. Far enough off, or after a while, it
 * stops to eat it, and what it eats heals it. Hit it as it runs and it may drop what it carries; kill it and you get it
 * back. Food on the ground draws it too, so a thrown porkchop buys you some peace. With no food to take it just bites.
 * But cornered, badly hurt, it drops whatever it carries and turns on you, biting twice as fast.</p>
 *
 * <p>What it carries is held in its jaws as its main-hand item, so it is saved, shown and dropped with it.</p>
 *
 * <p>Its pose flags: {@code WINDUP} the growl before a snatch, {@code ACTING} a bite or snatch, {@code ALT} carrying,
 * {@code VEILED} head down eating, {@code GUARD} cornered and furious.</p>
 */
public class RimecoatWolverine extends WildMonster {
	private long snatchAt = -1;
	private long nextSnatch;
	private long nextBite;
	private long actingUntil;
	private long eatingUntil = -1;
	private int carried;
	private int repath;
	private @Nullable ItemEntity scent;

	public RimecoatWolverine(EntityType<? extends RimecoatWolverine> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 28.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ARMOR, 2.0)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
			@Override
			public boolean canUse() {
				return getTarget() == null && !carrying() && scent == null && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.FROST), List.of(Runes.SELF, Runes.HASTE));
	}

	/** Whether it has something in its jaws. */
	public boolean carrying() {
		return !getMainHandItem().isEmpty();
	}

	private boolean furious() {
		return WolverineRules.furious(getHealth(), getMaxHealth());
	}

	private static boolean isFood(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.FOOD);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return !carrying() && super.removeWhenFarAway(distance);
	}

	// ------------------------------------------------------------------ the hunt

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		boolean furious = furious();
		if (furious && carrying()) {
			// Cornered: it drops what it carries and turns.
			dropCarried(level);
			eatingUntil = -1;
			playSound(SoundEvents.POLAR_BEAR_WARNING, 1.0F, 1.5F);
		}
		setState(GUARD, furious);
		setState(ALT, carrying());
		setState(ACTING, now < actingUntil);
		setState(VEILED, eatingUntil >= 0);

		if (eatingUntil >= 0) {
			getNavigation().stop();
			chew(level, now);
			return;
		}

		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target);

		if (carrying()) {
			carried++;
			if (!hunting || WolverineRules.eats(distanceTo(target), carried)) {
				eatingUntil = now + WolverineRules.EAT_TICKS;
				getNavigation().stop();
			} else if (--repath <= 0 || getNavigation().isDone()) {
				repath = 10;
				flee(target);
			}
			return;
		}
		carried = 0;

		// Food on the ground draws it before anything else, unless it's cornered.
		if (!furious && now % 10 == 0) {
			scent = sniff(level);
		}
		if (scent != null) {
			if (!scent.isAlive() || furious) {
				scent = null;
			} else {
				snatchAt = -1;
				setState(WINDUP, false);
				if (distanceTo(scent) <= 1.6) {
					grab(scent.getItem());
					if (scent.getItem().isEmpty()) {
						scent.discard();
					}
					scent = null;
				} else if (--repath <= 0 || getNavigation().isDone()) {
					repath = 10;
					getNavigation().moveTo(scent, 1.25);
				}
				return;
			}
		}

		if (!hunting) {
			snatchAt = -1;
			setState(WINDUP, false);
			return;
		}
		double distance = distanceTo(target);
		getLookControl().setLookAt(target, 30, 30);

		if (snatchAt >= 0) {
			face(target);
			getNavigation().moveTo(target, 1.3);
			if (now >= snatchAt) {
				snatchAt = -1;
				setState(WINDUP, false);
				if (distance <= WolverineRules.SNATCH_REACH + 0.6) {
					snatch(level, target, now);
				}
			}
			return;
		}

		if (WolverineRules.snatches(distance, now, nextSnatch, false, foodSlot(target) >= 0, furious)) {
			snatchAt = now + WolverineRules.SNATCH_WINDUP;
			setState(WINDUP, true);
			playSound(SoundEvents.POLAR_BEAR_WARNING, 0.8F, 1.6F);
			return;
		}

		if (distance <= WolverineRules.BITE_REACH && now >= nextBite) {
			nextBite = now + WolverineRules.biteCooldown(furious);
			face(target);
			doHurtTarget(level, target);
			actingUntil = now + 6;
			setState(ACTING, true);
		}
		if (--repath <= 0 || getNavigation().isDone()) {
			repath = 8;
			getNavigation().moveTo(target, furious ? 1.35 : 1.15);
		}
	}

	/** Away from its prey, as fast as it goes. */
	private void flee(LivingEntity from) {
		Vec3 off = position().subtract(from.position()).multiply(1, 0, 1);
		if (off.lengthSqr() < 1.0E-4) {
			off = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
		}
		Vec3 spot = position().add(off.normalize().scale(8));
		getNavigation().moveTo(spot.x, spot.y, spot.z, 1.4);
	}

	/** Head down, chewing; once done what it ate heals it and its jaws are empty for more. */
	private void chew(ServerLevel level, long now) {
		ItemStack food = getMainHandItem();
		if (food.isEmpty()) {
			eatingUntil = -1;
			return;
		}
		if (now % 5 == 0) {
			playSound(SoundEvents.FOX_EAT, 0.8F, 0.8F + random.nextFloat() * 0.2F);
			Vec3 mouth = position().add(getLookAngle().multiply(0.8, 0, 0.8)).add(0, 0.3, 0);
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(food)), mouth.x, mouth.y, mouth.z,
				4, 0.1, 0.05, 0.1, 0.05);
		}
		if (now >= eatingUntil) {
			FoodProperties props = food.get(DataComponents.FOOD);
			heal(WolverineRules.heal(props == null ? 1 : props.nutrition(), food.getCount()));
			setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			playSound(SoundEvents.PLAYER_BURP, 0.7F, 0.6F);
			eatingUntil = -1;
			nextSnatch = now + WolverineRules.SNATCH_COOLDOWN;
		}
	}

	/** Darting in: a bite, and whatever food it gets its teeth into. */
	private void snatch(ServerLevel level, LivingEntity target, long now) {
		nextSnatch = now + WolverineRules.SNATCH_COOLDOWN;
		nextBite = now + WolverineRules.BITE_COOLDOWN;
		actingUntil = now + 8;
		setState(ACTING, true);
		doHurtTarget(level, target);
		int slot = foodSlot(target);
		if (!(target instanceof Player player) || slot < 0) {
			return;
		}
		Inventory inventory = player.getInventory();
		ItemStack from = inventory.getItem(slot);
		ItemStack stolen = from.split(WolverineRules.take(from.getCount()));
		inventory.setChanged();
		grab(stolen);
		player.sendOverlayMessage(Component.translatable("monster.wildercord.rimecoat_wolverine.stole", stolen.getHoverName()));
	}

	/** Takes food in its jaws (as much of {@code stack} as it can carry, the rest left where it was). */
	private void grab(ItemStack stack) {
		if (!isFood(stack) || carrying()) {
			return;
		}
		ItemStack taken = stack.split(WolverineRules.take(stack.getCount()));
		setItemSlot(EquipmentSlot.MAINHAND, taken);
		setGuaranteedDrop(EquipmentSlot.MAINHAND);
		carried = 0;
		playSound(SoundEvents.ITEM_PICKUP, 0.8F, 0.8F);
		playSound(SoundEvents.POLAR_BEAR_AMBIENT, 0.8F, 1.7F);
	}

	private void dropCarried(ServerLevel level) {
		ItemStack food = getMainHandItem();
		if (!food.isEmpty()) {
			setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			ItemEntity dropped = spawnAtLocation(level, food);
			if (dropped != null) {
				dropped.setPickUpDelay(40);
			}
			playSound(SoundEvents.FOX_SPIT, 1.0F, 0.7F);
		}
	}

	/** Where its prey keeps food it can get at: what it holds first, else any stack in its pack; -1 none. */
	private int foodSlot(LivingEntity target) {
		if (!(target instanceof Player player)) {
			return -1;
		}
		Inventory inventory = player.getInventory();
		if (isFood(inventory.getSelectedItem())) {
			return inventory.getSelectedSlot();
		}
		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			if (isFood(inventory.getItem(i))) {
				slots.add(i);
			}
		}
		if (isFood(inventory.getItem(Inventory.SLOT_OFFHAND))) {
			slots.add(Inventory.SLOT_OFFHAND);
		}
		return slots.isEmpty() ? -1 : slots.get(random.nextInt(slots.size()));
	}

	/** The nearest food lying on the ground within its nose's reach. */
	private @Nullable ItemEntity sniff(ServerLevel level) {
		ItemEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(WolverineRules.SCENT_RANGE),
			e -> e.isAlive() && !e.hasPickUpDelay() && isFood(e.getItem()))) {
			double d = distanceToSqr(item);
			if (d < bestDistance) {
				bestDistance = d;
				best = item;
			}
		}
		return best;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && isAlive() && carrying() && WolverineRules.dropsWhenHit(random.nextFloat())) {
			dropCarried(level);
			eatingUntil = -1;
		}
		return hurt;
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return carrying() ? null : SoundEvents.POLAR_BEAR_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 1.5F + random.nextFloat() * 0.2F;
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
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.WOLF_STEP.value(), 0.3F, 0.8F);
	}
}
