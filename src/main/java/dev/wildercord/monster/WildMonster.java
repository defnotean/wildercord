package dev.wildercord.monster;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What the magical monsters of the wilds share: a few bits of what they're doing, synced in one byte and eased on the client
 * for their poses and glows (as the dungeon bosses do), and the spells one of them carries if it rolls Runebound.
 *
 * <ul>
 *   <li>{@link #WINDUP}: the tell before its signature attack (a Bramblewalker rearing back, a harpy's shriek...);</li>
 *   <li>{@link #ACTING}: the attack itself (the lash, the pounce, the dive, the roll, the tongue);</li>
 *   <li>{@link #GUARD}: its defence (a Geode Crawler curled up), or a frog's gulp;</li>
 *   <li>{@link #STUNNED}: caught out (a missed pounce or dive, a cracked shell): the moment to strike back;</li>
 *   <li>{@link #VEILED}: hidden in the dark (the Gloomstalker);</li>
 *   <li>{@link #ALT}: a second move (a harpy calling lightning, a frog's mouth opening, a Gloomstalker slinking away).</li>
 * </ul>
 */
public abstract class WildMonster extends Monster implements RuneboundKin {
	public static final int WINDUP = 1;
	public static final int ACTING = 2;
	public static final int GUARD = 4;
	public static final int STUNNED = 8;
	public static final int VEILED = 16;
	public static final int ALT = 32;
	private static final int FLAGS = 6;
	/** How fast each pose eases in and out on the client, per tick (a tell snaps in, the veil drifts). */
	private static final float[] EASE = {0.22F, 0.35F, 0.2F, 0.2F, 0.06F, 0.2F};

	private static final EntityDataAccessor<Byte> DATA_STATE = net.minecraft.network.syncher.SynchedEntityData.defineId(WildMonster.class,
		EntityDataSerializers.BYTE);

	/** Its biome variant ({@link MonsterVariantRules.Variant}'s ordinal) and whether it leads a pack. */
	private static final EntityDataAccessor<Byte> DATA_VARIANT = SynchedEntityData.defineId(WildMonster.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Boolean> DATA_ALPHA = SynchedEntityData.defineId(WildMonster.class, EntityDataSerializers.BOOLEAN);
	private static final Identifier VARIANT_HEALTH = dev.wildercord.Wildercord.id("variant_health");
	private static final Identifier VARIANT_DAMAGE = dev.wildercord.Wildercord.id("variant_damage");
	private static final Identifier ALPHA_HEALTH = dev.wildercord.Wildercord.id("alpha_health");
	private static final Identifier ALPHA_DAMAGE = dev.wildercord.Wildercord.id("alpha_damage");
	private static final Identifier ALPHA_SCALE = dev.wildercord.Wildercord.id("alpha_scale");

	private final float[] pose = new float[FLAGS];
	private final float[] poseO = new float[FLAGS];

	protected WildMonster(EntityType<? extends WildMonster> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STATE, (byte) 0);
		builder.define(DATA_VARIANT, (byte) 0);
		builder.define(DATA_ALPHA, false);
	}

	/** Turns one of the flags above on or off (server side; the client sees it a moment later). */
	protected void setState(int flag, boolean on) {
		byte state = entityData.get(DATA_STATE);
		byte next = (byte) (on ? state | flag : state & ~flag);
		if (next != state) {
			entityData.set(DATA_STATE, next);
		}
	}

	/**
	 * Holds a creature that has no AI (its AI would set them again each tick) in a pose: its flags exactly these. For the
	 * tests' pictures and the guide's.
	 */
	public void holdPose(int flags) {
		entityData.set(DATA_STATE, (byte) flags);
	}

	/** Whether one of the flags above is on, on either side. */
	public boolean state(int flag) {
		return (entityData.get(DATA_STATE) & flag) != 0;
	}

	/** One flag's pose on the client, eased: 0 off, 1 fully on. */
	public float pose(int flag, float partial) {
		int i = Integer.numberOfTrailingZeros(flag);
		return Mth.lerp(partial, poseO[i], pose[i]);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			for (int i = 0; i < FLAGS; i++) {
				poseO[i] = pose[i];
				boolean on = state(1 << i);
				// The veil drifts in slowly but is torn away at once (a pounce, a light).
				float ease = (1 << i) == VEILED && !on ? 0.3F : EASE[i];
				pose[i] = Mth.approach(pose[i], on ? 1 : 0, ease);
			}
			// A Frost one sheds snow, an Ash one smoulders.
			if (variant() != MonsterVariantRules.Variant.NONE && random.nextInt(6) == 0) {
				level().addParticle(variant() == MonsterVariantRules.Variant.FROST ? ParticleTypes.SNOWFLAKE : ParticleTypes.SMOKE,
					getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
			}
		}
	}

	// ------------------------------------------------------------------ variants and packs (0.12)

	/** Whether this kind comes in Frost and Ash by where it's born. */
	protected boolean hasVariants() {
		return false;
	}

	/** Whether this kind hunts in packs (calls its kin onto its prey; the first of a group may be an Alpha). */
	protected boolean packHunter() {
		return false;
	}

	public MonsterVariantRules.Variant variant() {
		return MonsterVariantRules.Variant.of(entityData.get(DATA_VARIANT));
	}

	public boolean alpha() {
		return entityData.get(DATA_ALPHA);
	}

	/** Makes it this variant (and an Alpha or not), with what each adds; for spawns, loading and the tests. */
	public void setVariant(MonsterVariantRules.Variant variant, boolean alpha) {
		entityData.set(DATA_VARIANT, (byte) variant.ordinal());
		entityData.set(DATA_ALPHA, alpha);
		modifier(Attributes.MAX_HEALTH, VARIANT_HEALTH, variant == MonsterVariantRules.Variant.FROST ? MonsterVariantRules.FROST_HEALTH : 0);
		modifier(Attributes.ATTACK_DAMAGE, VARIANT_DAMAGE, variant == MonsterVariantRules.Variant.ASH ? MonsterVariantRules.ASH_DAMAGE : 0);
		modifier(Attributes.MAX_HEALTH, ALPHA_HEALTH, alpha ? MonsterVariantRules.ALPHA_HEALTH : 0);
		modifier(Attributes.ATTACK_DAMAGE, ALPHA_DAMAGE, alpha ? MonsterVariantRules.ALPHA_DAMAGE : 0);
		modifier(Attributes.SCALE, ALPHA_SCALE, alpha ? MonsterVariantRules.ALPHA_SCALE : 0);
	}

	private void modifier(Holder<Attribute> attribute, Identifier id, double amount) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance == null) {
			return;
		}
		instance.removeModifier(id);
		if (amount != 0) {
			instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (hasVariants() || packHunter()) {
			var biome = level.getBiome(blockPosition()).value();
			MonsterVariantRules.Variant variant = hasVariants()
				? MonsterVariantRules.of(biome.getBaseTemperature(), biome.coldEnoughToSnow(blockPosition(), level.getSeaLevel()))
				: MonsterVariantRules.Variant.NONE;
			// The first of a group is the one finalized with no group data yet.
			boolean alpha = packHunter() && groupData == null && reason == EntitySpawnReason.NATURAL
				&& random.nextDouble() < MonsterVariantRules.ALPHA_CHANCE;
			setVariant(variant, alpha);
			setHealth(getMaxHealth());
			if (data == null) {
				data = new PackData();
			}
		}
		return data;
	}

	/** Marks the rest of a group as not its first. */
	private static final class PackData implements SpawnGroupData {}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		LivingEntity before = getTarget();
		super.setTarget(target);
		if (packHunter() && target != null && target != before && level() instanceof ServerLevel server) {
			double range = MonsterVariantRules.callRange(alpha());
			for (WildMonster kin : server.getEntitiesOfClass(WildMonster.class, getBoundingBox().inflate(range),
					other -> other != this && other.getType() == getType() && other.isAlive() && other.getTarget() == null)) {
				kin.setTarget(target);
			}
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			switch (variant()) {
				case FROST -> {
					living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, MonsterVariantRules.FROST_SLOW_TICKS, 0), this);
					if (living.canFreeze()) {
						living.setTicksFrozen(Math.max(living.getTicksFrozen(), MonsterVariantRules.FROST_CHILL_TICKS));
					}
				}
				case ASH -> living.igniteForSeconds(MonsterVariantRules.ASH_BURN_SECONDS);
				case NONE -> { }
			}
		}
		return hit;
	}

	@Override
	public boolean fireImmune() {
		return variant() == MonsterVariantRules.Variant.ASH || super.fireImmune();
	}

	@Override
	public boolean canFreeze() {
		return variant() != MonsterVariantRules.Variant.FROST && super.canFreeze();
	}

	/** "Frost Bramblewalker", "Alpha Gloomstalker", "Alpha Ash Gloomstalker". */
	@Override
	protected Component getTypeName() {
		Component name = super.getTypeName();
		if (variant() != MonsterVariantRules.Variant.NONE) {
			name = Component.translatable("monster.wildercord.variant." + variant().id, name);
		}
		if (alpha()) {
			name = Component.translatable("monster.wildercord.alpha", name);
		}
		return name;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putByte("Variant", entityData.get(DATA_VARIANT));
		output.putBoolean("Alpha", alpha());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_VARIANT, input.getByteOr("Variant", (byte) 0));
		entityData.set(DATA_ALPHA, input.getBooleanOr("Alpha", false));
	}

	/** Turns its body, head and look to face a creature (before an aimed attack goes off, so it flies true). */
	protected void face(LivingEntity target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		setYRot(yaw);
		setYHeadRot(yaw);
		setYBodyRot(yaw);
		getLookControl().setLookAt(target, 60, 60);
	}

	/** The difficulty as a number: 0 Peaceful, 1 Easy, 2 Normal, 3 Hard. */
	protected int difficulty() {
		return level().getDifficulty().getId();
	}

	/** Whether this is the server's copy, in a level the server runs. */
	protected ServerLevel serverLevel() {
		return level() instanceof ServerLevel server ? server : null;
	}
}
