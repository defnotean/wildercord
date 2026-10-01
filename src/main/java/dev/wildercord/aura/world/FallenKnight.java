package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.cast.Targets;
import dev.wildercord.monster.MonsterMagic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A fallen knight: an old suit of armour that a swordsman's aura still walks in, long after the swordsman. It haunts the deep
 * places (strongholds, ancient cities, the expeditions, the spawner rooms of dungeons), its visor slit and the cracks in its
 * plate glowing a dim, smoky version of the method it once breathed.
 *
 * <p>It fights as a swordsman of Edge would, every move with a tell and an answer:</p>
 * <ul>
 *   <li><b>the slash</b>: it raises its blade high and holds it (its visor flares, a rising hum), and a moment before it
 *       swings a line of light marks the ground where the crescent will fly. Step off the line, meet it with your own slash
 *       (they clash), or turn it with a perfect guard. After the swing it's open for a second;</li>
 *   <li><b>the guard</b>: when its blow is resting and you're close it braces, blade across its body. A rushed swing meets
 *       its perfect moment and staggers you; the held guard halves the rest. Wait it out, strike from the side or behind, or
 *       break it with an axe, which leaves it reeling and open (a quarter more harm) for a while.</li>
 * </ul>
 * Its rank follows where it haunts (see {@link AuraWorldRules#knightRank}): health, armour, its slash and how often. Its aura
 * is magic, so its slash meets a player's spell defences like any monster's spell. It drops pages of its method's manual and
 * sometimes an Aura Shard, from its loot table (the pages' method from {@link KnightLoot}).
 */
public class FallenKnight extends AuraFighter implements Enemy {
	private int rank = 1;
	/** After its slash it's open until this: no guard. */
	private long openUntil;
	/** Its guard broken by an axe: it takes more until this. */
	private long brokenUntil;
	private boolean guardNext;

	public FallenKnight(EntityType<? extends FallenKnight> type, Level level) {
		super(type, level);
		this.xpReward = 15;
		setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, AuraWorldRules.knightHealth(1))
			.add(Attributes.ARMOR, AuraWorldRules.knightArmour(1))
			.add(Attributes.MOVEMENT_SPEED, 0.24)
			.add(Attributes.ATTACK_DAMAGE, 1.0)
			.add(Attributes.ATTACK_KNOCKBACK, 0.3)
			.add(Attributes.FOLLOW_RANGE, 28.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) {
			@Override
			public boolean canUse() {
				return free() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return free() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	/** Whether it's free to walk and strike: not winding up, guarding or caught out. */
	private boolean free() {
		return slashAt == 0 && !guarding() && !staggered();
	}

	// ------------------------------------------------------------------ what it is

	public int rank() {
		return rank;
	}

	/** Sets its rank (where it haunts): its health, armour and blade follow. */
	public void setRank(int rank) {
		this.rank = AuraWorldRules.clampRank(rank);
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(AuraWorldRules.knightHealth(this.rank));
		}
		AttributeInstance armour = getAttribute(Attributes.ARMOR);
		if (armour != null) {
			armour.setBaseValue(AuraWorldRules.knightArmour(this.rank));
		}
		setHealth(getMaxHealth());
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(this.rank >= 3 ? Items.DIAMOND_SWORD : Items.IRON_SWORD));
		coat();
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData group) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, group);
		setMethod(BreathingMethods.BUILT_IN.get(getRandom().nextInt(BreathingMethods.BUILT_IN.size())));
		setStage(AuraRules.EDGE);
		setRank(1);
		setState(DRAWN, true);
		return data;
	}

	// ------------------------------------------------------------------ fighting

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		tickGuard(now);
		tickStagger(now);
		tickSlash(level, now);
		setState(DRAWN, true);
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || slashAt > 0 || staggered()) {
			return;
		}
		double d = distanceTo(target);
		if (guarding()) {
			faceTarget(target);
			getNavigation().stop();
			if (d > 6) {
				dropGuard();
			}
			return;
		}
		if (guardNext && now >= openUntil) {
			guardNext = false;
			raiseGuard();
			return;
		}
		if (d >= 5 && d <= AuraWorldRules.KNIGHT_SLASH_RANGE - 1 && now >= nextSlashAt && hasLineOfSight(target)) {
			windUp(target, AuraWorldRules.knightSlashWindup(rank), AuraWorldRules.knightSlashCooldown(rank));
			return;
		}
		if (d < 4 && now >= openUntil && now % 10 == 0 && getRandom().nextDouble() < AuraWorldRules.KNIGHT_GUARD_CHANCE) {
			raiseGuard();
		}
	}

	@Override
	protected void afterSlash(ServerLevel level) {
		openUntil = level.getGameTime() + AuraWorldRules.KNIGHT_RECOVER;
		guardReadyAt = Math.max(guardReadyAt, openUntil);
	}

	@Override
	protected void windupSound(ServerLevel level) {
		MonsterMagic.sound(level, position().add(0, 1.5, 0), "duelist_knight_windup", 1.1F, 1.0F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		float through = guarded(level, source, damage);
		if (through < 0) {
			return false;
		}
		if (level.getGameTime() < brokenUntil) {
			// Its guard broken, it reels: a quarter more.
			through *= (float) AuraWorldRules.KNIGHT_BROKEN_BONUS;
		}
		boolean hurt = super.hurtServer(level, source, through);
		if (hurt && isAlive() && !guarding() && slashAt == 0 && source.getEntity() instanceof LivingEntity && getRandom().nextDouble() < 0.4) {
			guardNext = true;
		}
		return hurt;
	}

	@Override
	protected void guardBroken(ServerLevel level, Player by) {
		super.guardBroken(level, by);
		brokenUntil = level.getGameTime() + AuraWorldRules.KNIGHT_BROKEN_TICKS;
		staggerUntil = brokenUntil;
	}

	// ------------------------------------------------------------------ its slash

	@Override
	protected double slashDamage() {
		return AuraWorldRules.knightSlash(rank);
	}

	@Override
	protected double slashSpeed() {
		return AuraWorldRules.KNIGHT_SLASH_SPEED;
	}

	@Override
	protected double slashRange() {
		return AuraWorldRules.KNIGHT_SLASH_RANGE;
	}

	@Override
	protected double slashWidth() {
		return AuraWorldRules.KNIGHT_SLASH_WIDTH;
	}

	/** What a monster's spell may harm: players, their pets, golems and what it's hunting, never another monster. */
	@Override
	protected Predicate<Entity> mayCut() {
		return e -> Targets.canHarm(this, e);
	}

	// ------------------------------------------------------------------ saved

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("rank", rank);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		rank = AuraWorldRules.clampRank(input.getIntOr("rank", 1));
	}

	// ------------------------------------------------------------------ sounds

	@Override
	public SoundSource getSoundSource() {
		return SoundSource.HOSTILE;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return MonsterMagic.kit("duelist_knight_ambient");
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return MonsterMagic.kit("duelist_knight_hurt");
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return MonsterMagic.kit("duelist_knight_death");
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		SoundEvent step = MonsterMagic.kit("duelist_knight_step");
		if (step != null) {
			playSound(step, 0.45F, 0.9F + getRandom().nextFloat() * 0.2F);
		}
	}
}
