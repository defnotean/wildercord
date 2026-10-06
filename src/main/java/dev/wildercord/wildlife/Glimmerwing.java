package dev.wildercord.wildlife;

import dev.wildercord.content.MoteOption;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A glimmerwing: a soft-glowing moth the size of a hand, out at night in forests and flower fields in little swarms
 * that keep together. Moths go to light, and these go to magic too: a lamp, a torch or a campfire draws them to circle
 * it, and so does a player who has just cast a spell. They shed a faint glittering dust as they go (drawn by each
 * client) and fade at daybreak. Harmless, and gone at a touch.
 *
 * <p>A swarm shares one colouring, from where it rose: moonlit blue in most woods, rose in flower forests and cherry
 * groves, amber in meadows.</p>
 */
public class Glimmerwing extends AmbientCreature {
	public static final int MOONLIT = 0;
	public static final int ROSE = 1;
	public static final int AMBER = 2;
	/** Each colouring's glow, for its dust. */
	public static final int[] COLORS = {0xA8D8FF, 0xFFB0D8, 0xFFD27A};

	private static final EntityDataAccessor<Byte> DATA_VARIANT = SynchedEntityData.defineId(Glimmerwing.class, EntityDataSerializers.BYTE);

	/** The spot its swarm keeps near, unless something draws it off. */
	private @Nullable Vec3 home;
	/** What it's flying to now. */
	private @Nullable Vec3 target;
	/** A light or a caster it's circling, until it loses interest. */
	private @Nullable Vec3 lure;
	private int lureLeft;
	private @Nullable BlockPos flower;
	private int retarget;
	/** Client: the wings' phase and how hard they beat (this tick's and last tick's phase), and a glide's ticks left. */
	public float flapPhase, flapPhaseO, flapStrength = 1;
	private int glide;

	public Glimmerwing(EntityType<? extends Glimmerwing> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0).add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, (byte) MOONLIT);
	}

	public int variant() {
		return Mth.clamp(entityData.get(DATA_VARIANT), 0, COLORS.length - 1);
	}

	public void setVariant(int variant) {
		entityData.set(DATA_VARIANT, (byte) Mth.clamp(variant, 0, COLORS.length - 1));
	}

	public int color() {
		return COLORS[variant()];
	}

	/** Its swarm's colouring, so the moths rising together match. */
	private record Swarm(int variant) implements SpawnGroupData {}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData group) {
		if (group instanceof Swarm swarm) {
			setVariant(swarm.variant());
		} else {
			var biome = level.getBiome(blockPosition());
			String id = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
			int variant = switch (id) {
				case "minecraft:flower_forest", "minecraft:cherry_grove" -> ROSE;
				case "minecraft:meadow", "minecraft:sunflower_plains" -> AMBER;
				default -> biome.is(BiomeTags.IS_FOREST) ? MOONLIT : random.nextInt(COLORS.length);
			};
			// Now and then a swarm strays from its colour.
			if (random.nextInt(6) == 0) {
				variant = random.nextInt(COLORS.length);
			}
			setVariant(variant);
			group = new Swarm(variant);
		}
		home = position();
		return super.finalizeSpawn(level, difficulty, reason, group);
	}

	// ------------------------------------------------------------------ flight

	@Override
	public void tick() {
		super.tick();
		// Each beat of its wings checks a climb or a drop: it bobs, never soars.
		setDeltaMovement(getDeltaMovement().multiply(1.0, 0.8, 1.0));
		if (level().isClientSide()) {
			flapPhaseO = flapPhase;
			// Quick beats, broken now and then by a moment's glide on raised wings.
			if (glide > 0) {
				glide--;
			} else if (random.nextInt(70) == 0) {
				glide = 6 + random.nextInt(10);
			}
			flapStrength = WildlifeRules.approach(flapStrength, glide > 0 ? 0.1F : 1, 0.25F);
			flapPhase += 0.25F + 0.95F * flapStrength;
		}
		if (level().isClientSide() && tickCount % 4 == (getId() & 3) && random.nextInt(3) != 0) {
			// A faint glitter of dust, sinking where it passed.
			level().addParticle(new MoteOption(MoteOption.GLOW, color(), 0.05F + random.nextFloat() * 0.03F, 22 + random.nextInt(14),
				(random.nextFloat() - 0.5F) * 0.004F, -0.012F, (random.nextFloat() - 0.5F) * 0.004F, 0.015F), getX(), getY() + 0.12, getZ(), 0, 0, 0);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (home == null) {
			home = position();
		}
		if(flower!=null && tickCount%10==0 && MoonreedBlock.pollinate(level,flower,this)) {flower=null;lure=null;lureLeft=0;target=null;}
		// A synchronous pollination callback may remove or transfer this actual actor.
		if(!isAlive()||isRemoved()||level()!=level)return;
		if ((tickCount + getId()) % 40 == 0) {
			seekLure(level);
		}
		if (lureLeft > 0 && --lureLeft == 0) {
			lure = null;
		}
		if (target == null || --retarget <= 0 || target.distanceToSqr(position()) < 0.5 || !level.getBlockState(BlockPos.containing(target)).isAir()) {
			target = pickTarget(level);
			retarget = 15 + random.nextInt(30);
		}
		Vec3 to = target.subtract(position());
		Vec3 v = getDeltaMovement();
		// Moths don't fly straight: a jitter on every beat, a little lift, and a pull toward where it's going.
		double jitter = 0.05;
		Vec3 wanted = new Vec3(Math.signum(to.x) * Math.min(0.22, Math.abs(to.x) * 0.2), Math.signum(to.y) * Math.min(0.16, Math.abs(to.y) * 0.25),
			Math.signum(to.z) * Math.min(0.22, Math.abs(to.z) * 0.2));
		Vec3 next = v.add((wanted.x - v.x) * 0.14 + (random.nextDouble() - 0.5) * jitter, (wanted.y - v.y) * 0.14 + (random.nextDouble() - 0.5) * jitter,
			(wanted.z - v.z) * 0.14 + (random.nextDouble() - 0.5) * jitter);
		setDeltaMovement(next);
		float yaw = (float) (Mth.atan2(next.z, next.x) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.35F);
		yBodyRot = getYRot();
		zza = 0.5F;

		// They belong to the night: by day, one by one, they fade.
		if (!level.isDarkOutside() && !isPersistenceRequired() && level.getBrightness(LightLayer.SKY, blockPosition()) > 8 && random.nextInt(240) == 0) {
			fade(level);
		}
	}

	/** Finds something to circle: a caster fresh from a spell, or else the brightest lamp near. */
	private void seekLure(ServerLevel level) {
		ServerPlayer caster = null;
		double best = WildlifeRules.CAST_LURE_RANGE * WildlifeRules.CAST_LURE_RANGE;
		for (ServerPlayer player : level.players()) {
			double d = player.distanceToSqr(this);
			if (d < best && !player.isSpectator() && Wildlife.castRecently(player, WildlifeRules.CAST_LURE_TICKS)) {
				best = d;
				caster = player;
			}
		}
		if (caster != null) {
			flower=null;
			lure = caster.getEyePosition().add(0, 0.6, 0);
			lureLeft = 80;
			return;
		}
		// Open-sky flowers are column tops: at most 49 resident cells every two seconds.
        flower=null;
        if(WetlandRules.night(level.getOverworldClockTime())) {
            flower=MoonreedBlock.findBud(level,blockPosition());
            if(flower!=null){lure=Vec3.atCenterOf(flower).add(0,.4,0);lureLeft=100;target=null;return;}
        }
        // A few looks around for light, keeping the brightest: over a few tries the swarm finds the lamp.
		BlockPos here = blockPosition();
		int brightest = lure == null ? WildlifeRules.LIGHT_LURE - 1 : level.getBrightness(LightLayer.BLOCK, BlockPos.containing(lure));
		Vec3 found = null;
		for (int i = 0; i < 5; i++) {
			BlockPos look = here.offset(random.nextInt(17) - 8, random.nextInt(9) - 4, random.nextInt(17) - 8);
			int light = level.getBrightness(LightLayer.BLOCK, look);
			if (light > brightest && level.getBlockState(look).isAir()) {
				brightest = light;
				found = Vec3.atCenterOf(look);
			}
		}
		if (found != null) {
			lure = found;
			lureLeft = 200;
		}
	}

	private Vec3 pickTarget(ServerLevel level) {
		if(flower!=null)return Vec3.atCenterOf(flower).add((random.nextDouble()-.5)*.3,.4,(random.nextDouble()-.5)*.3);
		if (lure != null) {
			// Round and round the light, never quite touching it.
			double angle = random.nextDouble() * Mth.TWO_PI;
			double r = 0.9 + random.nextDouble() * 1.4;
			return lure.add(Math.cos(angle) * r, (random.nextDouble() - 0.4) * 1.2, Math.sin(angle) * r);
		}
		Vec3 base = home == null ? position() : home;
		double x = base.x + (random.nextDouble() - 0.5) * 10;
		double z = base.z + (random.nextDouble() - 0.5) * 10;
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
		double y = Math.min(ground + 0.8 + random.nextDouble() * 3.2, base.y + 4);
		return new Vec3(x, Math.max(y, ground + 0.6), z);
	}

	/** Fades into a puff of its own dust. */
	public void fade(ServerLevel level) {
		level.sendParticles(new MoteOption(MoteOption.GLOW, color(), 0.08F, 30, 0, 0.01F, 0, 0.03F), getX(), getY() + 0.15, getZ(), 6, 0.15, 0.1, 0.15, 0);
		discard();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	// ------------------------------------------------------------------ sound

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return Wildlife.sound("glimmerwing_flutter");
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return Wildlife.sound("glimmerwing_hurt");
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return Wildlife.sound("glimmerwing_hurt");
	}

	@Override
	protected float getSoundVolume() {
		return 0.35F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putByte("variant", (byte) variant());
		if (home != null) {
			output.putDouble("home_x", home.x);
			output.putDouble("home_y", home.y);
			output.putDouble("home_z", home.z);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setVariant(input.getByteOr("variant", (byte) MOONLIT));
		double y = input.getDoubleOr("home_y", Double.NaN);
		if (!Double.isNaN(y)) {
			home = new Vec3(input.getDoubleOr("home_x", getX()), y, input.getDoubleOr("home_z", getZ()));
		}
	}
}
