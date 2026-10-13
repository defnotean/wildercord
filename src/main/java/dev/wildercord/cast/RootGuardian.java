package dev.wildercord.cast;

import static dev.wildercord.cast.RootGuardianRules.*;

import dev.wildercord.content.dungeons.DungeonSounds;
import dev.wildercord.monster.MonsterMagic;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Root Guardian: a walking knot of trunk and root with a heart of amber under three root bindings. Its bindings turn most of
 * every blow; shears (right click) or fire cut them one at a time, and with the last gone it reels back with its heart bare for a
 * while before they grow back, as they do again at each of its phases.
 * <p>
 * Between its spells it fights as a tree would if it could walk (see {@link RootGuardianRules}): it sinks its arms into the floor
 * and drives roots up under everyone's feet; uncoils its lowest binding into a whip and lashes it across before it, snagging it in
 * the floor when it lashes at nothing; lobs seed pods out of its crown that burst into briars; gushes sap from its heart down a lane
 * that holds whoever it washes over and lies sticky after; plants a leg and sends a root burrowing after whoever keeps away, to drag
 * them in; from its second phase bursts thorns out along spokes all round it, tears its legs out of the floor to charge and tangles
 * itself in its own roots after, and shakes a pollen cloud out of its crown; and at the last brings the canopy down on shadows round
 * everyone. Fire is its bane all through: briars burn away, its sap and its pollen catch and flare, a burning foe sets its creeping
 * root alight, and each flare it is caught in burns through a binding.
 */
public final class RootGuardian extends DungeonBoss {
	private static final int COLOR = 0x438835, ACCENT = 0xFFC95C, NAME = 0x83CC55, SAP_TINT = 0xD9A030, POLLEN_TINT = 0xF2D45C, BARK = 0x6B4A2E;
	private static final EntityDataAccessor<Integer> BINDINGS = SynchedEntityData.defineId(RootGuardian.class, EntityDataSerializers.INT);
	/** Which arm its lash comes from, 1 or -1; whether its pollen hangs about it; whether its creeping root holds someone, for its pose. */
	private static final EntityDataAccessor<Float> DATA_SIDE = SynchedEntityData.defineId(RootGuardian.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_CLOUD = SynchedEntityData.defineId(RootGuardian.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_GRIP = SynchedEntityData.defineId(RootGuardian.class, EntityDataSerializers.FLOAT);

	private static final List<List<RuneDef>> PHASE_1 = List.of(List.of(Runes.ARC, Runes.MIRE), List.of(Runes.WAVE, Runes.ROOTSNARE));
	private static final List<List<RuneDef>> PHASE_2 = List.of(List.of(Runes.RAIN, Runes.VENOM), List.of(Runes.RING, Runes.ROOTSNARE));
	private static final List<List<RuneDef>> PHASE_3 = List.of(List.of(Runes.PILLAR, Runes.TREMOR), List.of(Runes.CONE, Runes.ROOTSNARE),
		List.of(Runes.WAVE, Runes.AFTERSHOCK));

	private int bindings = 3;
	private long pruneAt, exposedUntil;
	/** Whoever its move under way has struck already, so no one is struck twice by one swing. */
	private final Set<UUID> struck = new HashSet<>();
	/** Where its move under way is aimed (degrees, locked a few ticks before the blow), and which arm it lashes with. */
	private float aim;
	private int side = 1;
	/** Whether its lash caught anyone, and whether its charge ran into stone. */
	private boolean landed, walled;
	/** Where its roots will come up, and where its boughs will fall, in order. */
	private final List<Vec3> marks = new ArrayList<>();
	private final List<LivingEntity> marked = new ArrayList<>();
	/** Its creeping root: its tip, its heading, whoever it caught and how long it has dragged them. */
	private Vec3 tip;
	private double tipYaw;
	private LivingEntity caught;
	private int dragged;
	/** How far its sap runs or its charge carries before stone, how far its charge has carried, its thorns' turn, and how many boughs fall. */
	private double reach, spokeOffset, charged;
	private int drops;
	/** Pods in the air, briars where they burst, sap lying in pools, and when its pollen settles. */
	private final List<Pod> flying = new ArrayList<>();
	private final List<Patch> briars = new ArrayList<>();
	private final List<Pool> pools = new ArrayList<>();
	private long cloudUntil;
	/** Its pollen and its grip, eased (client). */
	private float cloud, cloudO, grip, gripO;

	/** A seed pod lobbed from {@code from}, landing on {@code to} at {@code lands}. */
	private record Pod(Vec3 from, Vec3 to, long lands) {}
	/** Briars grown where a pod burst, until {@code until}. */
	private record Patch(Vec3 at, long until) {}
	/** Sap lying in a strip from {@code from} to {@code to}, until {@code until}. */
	private record Pool(Vec3 from, Vec3 to, long until) {}

	public RootGuardian(EntityType<? extends RootGuardian> type, Level level) { super(type, level, BossEvent.BossBarColor.GREEN); }

	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, 220).add(Attributes.MOVEMENT_SPEED, .20)
			.add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ATTACK_DAMAGE, 7);
	}

	/** Wakes the Guardian at its altar: roots heave up out of the floor and it pulls itself free of them. */
	public static RootGuardian rise(ServerLevel level, BlockPos altar) {
		RootGuardian boss = place(level, DungeonEntities.ROOT_GUARDIAN, altar, 1);
		if (boss == null) return null;
		Vec3 at = boss.position();
		Sigils.ground(level, at, COLOR, ACCENT, 4, 60);
		ElementFx.leafSpiral(level, at, 1.6, 3.5, 24);
		ElementFx.petals(level, at.add(0, 2, 0), 1.5, 20);
		ElementFx.crack(level, at, 2.5, 50);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, ElementFx.groundBlock(level, at)), at.add(0, .3, 0), 30, 1.2, .2);
		Fx.sound(level, at, DungeonSounds.BOSS_RISE, 2, 1);
		Fx.sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 1.6F, .5F);
		Fx.sound(level, at, SoundEvents.WOOD_BREAK, 1.2F, .5F);
		boss.announce(level, "message.wildercord.root_guardian_wakes", NAME);
		return boss;
	}

	@Override protected List<List<RuneDef>> spells(int phase) { return phase == 1 ? PHASE_1 : phase == 2 ? PHASE_2 : PHASE_3; }
	@Override protected String feat() { return "root_guardian"; }
	@Override protected int color() { return COLOR; }
	@Override protected int accent() { return ACCENT; }

	@Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(BINDINGS, 3);
		builder.define(DATA_SIDE, 1F);
		builder.define(DATA_CLOUD, 0F);
		builder.define(DATA_GRIP, 0F);
	}

	@Override public void tick() {
		super.tick();
		if (level().isClientSide()) {
			cloudO = cloud; gripO = grip;
			cloud = Mth.approach(cloud, entityData.get(DATA_CLOUD), .08F);
			grip = Mth.approach(grip, entityData.get(DATA_GRIP), .2F);
		}
	}

	/** 0: the arm its lash comes from (1 or -1); 1: its pollen hanging about it; 2: its creeping root holding someone. */
	@Override public float extra(int i, float partial) {
		return i == 0 ? entityData.get(DATA_SIDE) : i == 1 ? Mth.lerp(partial, cloudO, cloud) : i == 2 ? Mth.lerp(partial, gripO, grip) : 0;
	}

	public int bindings() { return level().isClientSide() ? entityData.get(BINDINGS) : bindings; }

	@Override protected void approach(ServerLevel level, LivingEntity target) {
		if (distanceTo(target) > 4) getNavigation().moveTo(target, 1);
		else { getNavigation().stop(); getLookControl().setLookAt(target, 30, 30); }
	}

	// ------------------------------------------------------------------ how it reckons where things are

	/** Those it may harm within {@code radius} of {@code at}. */
	private List<LivingEntity> foes(ServerLevel level, Vec3 at, double radius) {
		return level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, 4, radius), e -> e != this && e.isAlive() && Targets.canHarm(this, e));
	}

	private void hurt(ServerLevel level, LivingEntity t, double damage) {
		t.hurtServer(level, level.damageSources().mobAttack(this), (float) (damage * Runebound.power(level)));
	}

	/** Strikes {@code t} once in its move under way, at most. True if it did. */
	private boolean strike(ServerLevel level, LivingEntity t, double damage) {
		if (!struck.add(t.getUUID())) return false;
		hurt(level, t, damage);
		return true;
	}

	/** Throws {@code t} away from {@code from}, {@code out} along the floor and {@code up}. */
	private static void throwFrom(LivingEntity t, Vec3 from, double out, double up) {
		Vec3 away = new Vec3(t.getX() - from.x, 0, t.getZ() - from.z);
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
		Effects.push(t, new Vec3(away.x * out, up, away.z * out));
	}

	private static Vec3 ahead(double yaw) {
		double r = Math.toRadians(yaw);
		return new Vec3(-Math.sin(r), 0, Math.cos(r));
	}

	private static float yawFrom(Vec3 from, Vec3 at) { return (float) Math.toDegrees(Math.atan2(-(at.x - from.x), at.z - from.z)); }
	private float yawTo(Vec3 at) { return yawFrom(position(), at); }
	private static double flat(Vec3 a, Vec3 b) { return Math.hypot(a.x - b.x, a.z - b.z); }

	private void face(float yaw) { setYRot(yaw); yBodyRot = yaw; yHeadRot = yaw; }
	private void turn(float yaw, float most) { face(getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -most, most)); }

	/** How far {@code at} lies along {@code yaw} from {@code from}, and how far across. */
	private static double[] local(Vec3 from, double yaw, Vec3 at) {
		Vec3 f = ahead(yaw);
		double dx = at.x - from.x, dz = at.z - from.z;
		return new double[] {dx * f.x + dz * f.z, dx * -f.z + dz * f.x};
	}

	/** How far along {@code yaw} from its feet the floor runs clear before stone, out to {@code most}. */
	private double clear(ServerLevel level, double yaw, double most) {
		Vec3 from = position().add(0, .6, 0);
		HitResult hit = level.clip(new ClipContext(from, from.add(ahead(yaw).scale(most)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return hit.getType() == HitResult.Type.MISS ? most : Math.max(1.5, hit.getLocation().distanceTo(from) - .3);
	}

	/** {@code at} brought within {@code most} of its arena's heart. */
	private Vec3 within(Vec3 at, double most) {
		Vec3 h = home != null ? Vec3.atBottomCenterOf(home) : position();
		double dx = at.x - h.x, dz = at.z - h.z, d = Math.hypot(dx, dz);
		return d <= most ? at : new Vec3(h.x + dx / d * most, at.y, h.z + dz / d * most);
	}

	/** The floor under {@code at}, or {@code at} itself over nothing. */
	private static Vec3 ground(ServerLevel level, Vec3 at) {
		Vec3 g = ElementFx.floor(level, at.add(0, 1.5, 0), 4);
		return g != null ? g : at;
	}

	/** A cone on the floor from its feet, {@code yaw} on, {@code reach} long and {@code half} degrees each way: its edges and its rim. */
	private void cone(ServerLevel level, double yaw, double reach, double half, int color, int lifetime) {
		Vec3 feet = position().add(0, .1, 0);
		for (int s = -1; s <= 1; s += 2) ElementFx.ray(level, feet, feet.add(ahead(yaw + half * s).scale(reach)), color, .12, lifetime);
		int steps = (int) Math.max(3, half / 12);
		Vec3 last = feet.add(ahead(yaw - half).scale(reach));
		for (int i = 1; i <= steps * 2; i++) {
			Vec3 next = feet.add(ahead(yaw - half + half * i / steps).scale(reach));
			ElementFx.ray(level, last, next, color, .1, lifetime);
			last = next;
		}
	}

	/** A lane on the floor from its feet, {@code yaw} on, from {@code from} to {@code to} out and {@code half} wide each way. */
	private void lane(ServerLevel level, double yaw, double from, double to, double half, int color, int lifetime) {
		Vec3 feet = position().add(0, .1, 0), f = ahead(yaw), s = ahead(yaw + 90).scale(half), a = feet.add(f.scale(from)), b = feet.add(f.scale(to));
		ElementFx.ray(level, a.add(s), b.add(s), color, .12, lifetime);
		ElementFx.ray(level, a.subtract(s), b.subtract(s), color, .12, lifetime);
		ElementFx.ray(level, b.add(s), b.subtract(s), color, .12, lifetime);
	}

	/** Dirt and root thrown up out of the floor at {@code at}. */
	private static void heave(ServerLevel level, Vec3 at, int count) {
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, ElementFx.groundBlock(level, at)), at.add(0, .2, 0), count, .4, .15);
	}

	// ------------------------------------------------------------------ its bindings: cut by shears and fire, grown back

	/** Its heart, low on the front of its trunk. */
	private Vec3 heart() { return position().add(ahead(yBodyRot).scale(.55)).add(0, 1.4, 0); }
	/** Its lowest binding still on it, where its lash comes from. */
	private Vec3 binding() { return position().add(0, 2.2 - Math.max(0, bindings - 1) * .45, 0); }

	@Override protected void mechanic(ServerLevel level, long now) {
		if (bindings == 0 && now >= exposedUntil) regrow(level);
		entityData.set(BINDINGS, bindings);
		setState(GUARDED, bindings > 0);
		setState(EXPOSED, bindings == 0);
		if (now % 20 == 0) Vfx.emit(level, bindings > 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.WAX_ON, position().add(0, 1.5, 0), 3, .6, .01);
		if (bindings == 0 && now % 4 == 0) Motes.glow(level, heart(), ACCENT, .3, 8, new Vec3(0, .02, 0), .02);
		aloft(level, now);
		briars(level, now);
		pools(level, now);
		cloud(level, now);
	}

	/** Its bindings creep back up round its trunk and knot shut over its heart. */
	private void regrow(ServerLevel level) {
		bindings = 3;
		entityData.set(BINDINGS, bindings);
		Vec3 at = position();
		ElementFx.leafSpiral(level, at, .9, 2.6, 14);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), at.add(0, 1.4, 0), 18, .5, .05);
		Fx.sound(level, at, SoundEvents.MANGROVE_ROOTS_PLACE, 1.4F, .6F);
		Fx.sound(level, at, SoundEvents.ROOTS_PLACE, 1.2F, .5F);
	}

	/** Cuts through one of its bindings: shears and fire wait on its last cut, a flare of its own sap or pollen ({@code force}) does not. */
	private boolean cut(ServerLevel level, boolean force) {
		long now = level.getGameTime();
		if (bindings == 0 || !force && now < pruneAt || shifting > 0 || state(SHIFTING) || !isAlive()) return false;
		Vec3 at = binding();
		bindings--;
		pruneAt = now + 12;
		entityData.set(BINDINGS, bindings);
		Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, position().add(0, 1.5, 0), 12, .7, .04);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), at, 14, .6, .1);
		Fx.sound(level, at, SoundEvents.MANGROVE_ROOTS_BREAK, 1.3F, .7F);
		if (bindings == 0) {
			// The last one gone, it reels back with its heart bare.
			exposedUntil = now + 120;
			interrupt();
			setState(EXPOSED, true);
			setState(GUARDED, false);
			pose(REEL, 0, REEL_TICKS);
			Motes.burst(level, heart(), 18, ACCENT, .3, 20, .15);
			Vfx.emit(level, ParticleTypes.WAX_ON, heart(), 12, .4, .05);
			Fx.sound(level, at, SoundEvents.WOOD_BREAK, 1.5F, .6F);
			Fx.sound(level, at, SoundEvents.RAVAGER_STUNNED, 1, 1.3F);
		}
		return true;
	}

	/** Prunes a binding (shears or fire), at most one every twelve ticks; never needs a rare rune. */
	public boolean prune(ServerLevel level) { return cut(level, false); }

	@Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!player.getItemInHand(hand).is(Items.SHEARS)) return super.mobInteract(player, hand);
		if (level() instanceof ServerLevel server && prune(server)) player.sendOverlayMessage(Component.translatable("message.wildercord.root_pruned", bindings));
		return InteractionResult.SUCCESS;
	}

	@Override protected float resist(ServerLevel level, DamageSource source, float damage) {
		boolean fire = source.is(DamageTypeTags.IS_FIRE) || Dungeons.spellLanding() && isOnFire();
		if (fire && cloudUntil > level.getGameTime()) flash(level);
		if (fire && bindings > 0) prune(level);
		return damage * taken(bindings, move == TANGLED);
	}

	// ------------------------------------------------------------------ what its moves leave lying about

	/** Its pollen catches: the whole cloud goes up in one flash, burning whoever is in it, and a binding with it. */
	private void flash(ServerLevel level) {
		if (cloudUntil <= level.getGameTime()) return;
		cloudUntil = 0;
		entityData.set(DATA_CLOUD, 0F);
		Vec3 c = position().add(0, 1.2, 0);
		ElementFx.flameBurst(level, c, CLOUD_RADIUS, 60);
		ElementFx.fireImpact(level, c, 2.5);
		Motes.burst(level, c, 30, POLLEN_TINT, .4, 16, .35);
		Fx.sound(level, c, SoundEvents.FIRECHARGE_USE, 2, .6F);
		Fx.sound(level, c, SoundEvents.BLAZE_SHOOT, 1.4F, .5F);
		for (LivingEntity t : foes(level, position(), CLOUD_RADIUS))
			if (flat(t.position(), position()) <= CLOUD_RADIUS) { hurt(level, t, FLASH_DAMAGE); t.igniteForTicks(Math.max(t.getRemainingFireTicks(), 60)); }
		cut(level, true);
	}

	private void cloud(ServerLevel level, long now) {
		if (cloudUntil == 0) return;
		if (now >= cloudUntil) { cloudUntil = 0; entityData.set(DATA_CLOUD, 0F); return; }
		Vec3 c = position();
		if (now % 3 == 0) {
			Motes.clouds(level, c.add(0, 1.4, 0), 3, CLOUD_RADIUS * .45, POLLEN_TINT, 1.4, 30, new Vec3(0, .01, 0), .02, .3);
			Vfx.emit(level, ParticleTypes.FALLING_NECTAR, c.add(0, 2.5, 0), 3, CLOUD_RADIUS * .4, 0);
		}
		if (now % 10 == 0) ElementFx.groundRing(level, c.add(0, .05, 0), POLLEN_TINT, CLOUD_RADIUS - .2, CLOUD_RADIUS, .08, 12);
		List<LivingEntity> in = foes(level, c, CLOUD_RADIUS).stream().filter(t -> flat(t.position(), c) <= CLOUD_RADIUS).toList();
		// A spark anywhere in it, it on fire, or anyone burning walking in, and it all goes up.
		if (isOnFire() || in.stream().anyMatch(Entity::isOnFire) || now % 2 == 0 && Effects.scorching(level, getBoundingBox().inflate(CLOUD_RADIUS, 1, CLOUD_RADIUS))) {
			flash(level);
			return;
		}
		if ((cloudUntil - now) % CLOUD_EVERY == 0) for (LivingEntity t : in) {
			hurt(level, t, CLOUD_DAMAGE);
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0, false, true));
			t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, true));
		}
	}

	/** Pods in the air: each rides its arc, and bursts into briars where it lands. */
	private void aloft(ServerLevel level, long now) {
		for (var it = flying.iterator(); it.hasNext();) {
			Pod p = it.next();
			if (now < p.lands()) {
				double f = 1 - (p.lands() - now) / (double) SEEDS_FLIGHT;
				Vec3 at = p.from().lerp(p.to(), f).add(0, lob(f) * 3.5, 0);
				Motes.glow(level, at, BARK, .45, 4, Vec3.ZERO, 0);
				Vfx.emit(level, ParticleTypes.SPORE_BLOSSOM_AIR, at, 1, .05, 0);
				continue;
			}
			it.remove();
			burst(level, p.to(), now);
		}
	}

	/** A pod bursts: thorns flung out round it, and briars grown up where it fell unless there's fire there. */
	private void burst(ServerLevel level, Vec3 at, long now) {
		for (LivingEntity t : foes(level, at, SEEDS_RADIUS + 1))
			if (flat(t.position(), at) <= SEEDS_RADIUS && t.getY() - at.y > -1 && t.getY() - at.y < 2.5) strike(level, t, SEEDS_DAMAGE);
		ElementFx.bloom(level, at.add(0, .3, 0), at, 1.4);
		ElementFx.petals(level, at.add(0, .4, 0), .8, 8);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()), at.add(0, .3, 0), 16, .6, .1);
		Fx.sound(level, at, SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.4F, .6F);
		Fx.sound(level, at, SoundEvents.SPORE_BLOSSOM_BREAK, 1, .8F);
		if (Effects.scorching(level, new AABB(at, at).inflate(SEEDS_RADIUS, 1, SEEDS_RADIUS))) { ElementFx.embers(level, at, SEEDS_RADIUS, 10); return; }
		if (briars.size() >= BRIAR_MOST) briars.removeFirst();
		briars.add(new Patch(at, now + BRIAR_TICKS));
	}

	/** Briars: they prick and slow whoever stands in them, and burn away at a touch of fire. */
	private void briars(ServerLevel level, long now) {
		for (var it = briars.iterator(); it.hasNext();) {
			Patch b = it.next();
			if (now >= b.until()) { ElementFx.petals(level, b.at().add(0, .3, 0), .8, 4); it.remove(); continue; }
			List<LivingEntity> in = foes(level, b.at(), SEEDS_RADIUS + .5).stream()
				.filter(t -> flat(t.position(), b.at()) <= SEEDS_RADIUS && Math.abs(t.getY() - b.at().y) < 1.5).toList();
			if (in.stream().anyMatch(Entity::isOnFire) || now % 2 == 0 && Effects.scorching(level, new AABB(b.at(), b.at()).inflate(SEEDS_RADIUS, 1, SEEDS_RADIUS))) {
				ElementFx.embers(level, b.at(), SEEDS_RADIUS, 16);
				ElementFx.flames(level, b.at(), SEEDS_RADIUS * .8, 1, 10);
				Motes.smoke(level, b.at().add(0, .4, 0), 4, .6);
				Fx.sound(level, b.at(), SoundEvents.FIRE_EXTINGUISH, .8F, 1.2F);
				it.remove();
				continue;
			}
			if (now % 6 == 0) {
				ElementFx.groundRing(level, b.at().add(0, .05, 0), COLOR, SEEDS_RADIUS - .15, SEEDS_RADIUS, .1, 7);
				Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()), b.at().add(0, .3, 0), 3, .7, 0);
			}
			if ((b.until() - now) % BRIAR_EVERY == 0) for (LivingEntity t : in) {
				hurt(level, t, BRIAR_DAMAGE);
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 0, false, true));
				Fx.sound(level, t.position(), SoundEvents.SWEET_BERRY_BUSH_BREAK, .8F, 1.2F);
			}
		}
	}

	/** How far {@code at} lies across the floor from the strip {@code a} to {@code b}. */
	private static double offStrip(Vec3 at, Vec3 a, Vec3 b) {
		double dx = b.x - a.x, dz = b.z - a.z, len = dx * dx + dz * dz;
		double f = len < 1.0E-6 ? 0 : Mth.clamp(((at.x - a.x) * dx + (at.z - a.z) * dz) / len, 0, 1);
		return Math.hypot(at.x - (a.x + dx * f), at.z - (a.z + dz * f));
	}

	private static boolean onPool(Vec3 at, Pool p) { return offStrip(at, p.from(), p.to()) <= SAP_HALF && Math.abs(at.y - p.from().y) < 1.5; }

	/** Sap lying in pools: sticky underfoot, and quick to catch at a touch of fire. */
	private void pools(ServerLevel level, long now) {
		for (var it = pools.iterator(); it.hasNext();) {
			Pool p = it.next();
			if (now >= p.until()) { it.remove(); continue; }
			Vec3 mid = p.from().lerp(p.to(), .5);
			double len = flat(p.from(), p.to()) / 2 + SAP_HALF;
			List<LivingEntity> on = foes(level, mid, len).stream().filter(t -> onPool(t.position(), p)).toList();
			boolean self = onPool(position(), p);
			AABB box = new AABB(p.from(), p.to()).inflate(SAP_HALF, .5, SAP_HALF);
			if (alight(on) || self && isOnFire() || now % 2 == 0 && Effects.scorching(level, box)) {
				it.remove();
				flare(level, p, on, self);
				continue;
			}
			if (now % 4 == 0) {
				Vfx.emit(level, ParticleTypes.LANDING_HONEY, p.from().lerp(p.to(), level.getRandom().nextDouble()).add(0, .1, 0), 2, SAP_HALF * .6, 0);
				Vec3 s = ahead(yawFrom(p.from(), p.to()) + 90).scale(SAP_HALF);
				ElementFx.ray(level, p.from().add(s).add(0, .06, 0), p.to().add(s).add(0, .06, 0), SAP_TINT, .08, 5);
				ElementFx.ray(level, p.from().subtract(s).add(0, .06, 0), p.to().subtract(s).add(0, .06, 0), SAP_TINT, .08, 5);
			}
			for (LivingEntity t : on) t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 2, false, true));
		}
	}

	private static boolean alight(List<LivingEntity> on) { return on.stream().anyMatch(Entity::isOnFire); }

	/** A pool of its sap catches and flares along its length, burning whoever stands in it, and it too if it stands in it. */
	private void flare(ServerLevel level, Pool p, List<LivingEntity> on, boolean self) {
		int steps = (int) Math.max(2, flat(p.from(), p.to()) / 1.5);
		for (int i = 0; i <= steps; i++) ElementFx.flames(level, p.from().lerp(p.to(), i / (double) steps), SAP_HALF, 1.6, 6);
		ElementFx.fireImpact(level, p.from().lerp(p.to(), .5), 1.5);
		Fx.sound(level, p.from().lerp(p.to(), .5), SoundEvents.FIRECHARGE_USE, 1.6F, .7F);
		Fx.sound(level, p.from().lerp(p.to(), .5), SoundEvents.GENERIC_BURN, 1.2F, .8F);
		for (LivingEntity t : on) { hurt(level, t, SCORCH_DAMAGE); t.igniteForTicks(Math.max(t.getRemainingFireTicks(), 80)); }
		if (self) cut(level, true);
	}

	// ------------------------------------------------------------------ its moves

	@Override protected int ownMoves() { return PICKS.length; }

	@Override protected void beginMove(ServerLevel level, int move, LivingEntity target) {
		getNavigation().stop();
		start(level, move, target, false);
	}

	/** Starts {@code move} at {@code target}, its tell cut short when it runs straight on from another. */
	private void start(ServerLevel level, int move, LivingEntity target, boolean chained) {
		int tell = tell(move, chained);
		struck.clear();
		marks.clear();
		landed = false;
		walled = false;
		moveTarget = target;
		aim = yawTo(target.position());
		int length = switch (move) {
			case SPEARS -> tell + (phase >= 3 ? SPEARS_SECOND : 0) + SPEARS_RECOVER;
			case LASH -> tell + (phase >= 3 ? LASH_BACK : 0) + LASH_RECOVER;
			case SEEDS -> tell + SEEDS_FLIGHT + SEEDS_RECOVER;
			case SAP -> tell + SAP_RUN + SAP_RECOVER;
			case CREEP -> tell + CREEP_TICKS + DRAG_MOST + CREEP_RECOVER;
			case THORNS -> tell + THORNS_RUN + (phase >= 3 ? THORNS_SECOND + THORNS_RUN : 0) + THORNS_RECOVER;
			case UPROOT -> tell + CHARGE_TICKS + (phase >= 3 ? RETELL + CHARGE_TICKS : 0);
			case POLLEN -> tell + POLLEN_RECOVER;
			case CANOPY -> tell + boughs(level, target) * CANOPY_EVERY + CANOPY_RECOVER;
			default -> tell;
		};
		if (move == LASH) { side = -side; entityData.set(DATA_SIDE, (float) side); }
		pose(move, tell, length);
	}

	@Override protected void moveTick(ServerLevel level, int move, int t, LivingEntity target) {
		switch (move) {
			case SPEARS -> spears(level, t, target);
			case LASH -> lash(level, t, target);
			case SEEDS -> seeds(level, t, target);
			case SAP -> sap(level, t, target);
			case CREEP -> creep(level, t, target);
			case THORNS -> thorns(level, t, target);
			case UPROOT -> uproot(level, t, target);
			case POLLEN -> pollen(level, t, target);
			case CANOPY -> canopy(level, t, target);
			case SNAGGED -> snagged(level, t);
			case TANGLED -> tangled(level, t);
			case STAGGER -> stagger(level, t);
			case REEL -> reel(level, t);
			default -> {}
		}
	}

	/** Through its tell it turns after its prey until {@code lock} ticks before the blow at {@code blow}, then holds its aim. */
	private void track(LivingEntity target, int t, int blow, int lock) {
		if (target != null && t < blow - lock) { aim = yawTo(target.position()); turn(aim, (float) TURN); }
		else face(aim);
	}

	private void track(LivingEntity target, int t, int lock) { track(target, t, moveTell, lock); }

	/** Its foes nearest it first, at most {@code most}; its prey alone when there's no one else. */
	private List<LivingEntity> nearest(ServerLevel level, LivingEntity target, int most) {
		List<LivingEntity> all = new ArrayList<>(foes(level, position(), 24));
		all.removeIf(e -> !(e instanceof Player) && e != target);
		all.sort((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b)));
		if (all.isEmpty() && target != null) all.add(target);
		return all.size() > most ? all.subList(0, most) : all;
	}

	// ------------------------------------------------------------------ root spears: roots driven up under everyone's feet

	private void spears(ServerLevel level, int t, LivingEntity target) {
		if (target != null && t < moveTell - SPEARS_LOCK) turn(yawTo(target.position()), (float) TURN);
		Vec3 feet = position(), hands = feet.add(ahead(getYRot()).scale(1.1));
		int second = moveTell + SPEARS_SECOND;
		if (t == 0) {
			ElementFx.crack(level, hands, 1.4, moveTell + 4);
			Fx.sound(level, hands, SoundEvents.ROOTED_DIRT_BREAK, 1.4F, .6F);
			Fx.sound(level, hands, SoundEvents.WARDEN_DIG, .7F, 1.5F);
		}
		if (t < moveTell) {
			// Its arms sunk to the elbow, roots feel out under everyone's feet: rings that follow them until they settle.
			if (t % 2 == 0) heave(level, hands, 3);
			if (t < moveTell - SPEARS_LOCK) { if (t % 3 == 0) feel(level, target, 4); }
			else if (t == moveTell - SPEARS_LOCK) settle(level, target, SPEARS_LOCK);
			return;
		}
		if (t == moveTell) { erupt(level); return; }
		if (phase < 3 || t > second) { if (t % 4 == 0) heave(level, hands, 2); return; }
		// In its last phase a second crop follows at once under wherever they have run to.
		if (t < second - SPEARS_LOCK) { if (t % 3 == 0) feel(level, target, 4); }
		else if (t == second - SPEARS_LOCK) { struck.clear(); settle(level, target, SPEARS_LOCK); }
		else if (t == second) erupt(level);
	}

	/** Rings following where its roots will come up. */
	private void feel(ServerLevel level, LivingEntity target, int lifetime) {
		for (LivingEntity e : nearest(level, target, SPEARS_MOST)) {
			Vec3 at = ground(level, e.position());
			ElementFx.groundRing(level, at.add(0, .05, 0), COLOR, SPEARS_RADIUS - .2, SPEARS_RADIUS, .1, lifetime);
			heave(level, at, 1);
		}
	}

	/** The rings settle: here they come up, whoever stands there now or not. */
	private void settle(ServerLevel level, LivingEntity target, int lock) {
		marks.clear();
		for (LivingEntity e : nearest(level, target, SPEARS_MOST)) marks.add(ground(level, e.position()));
		for (Vec3 at : marks) {
			ElementFx.groundRing(level, at.add(0, .05, 0), ACCENT, SPEARS_RADIUS - .25, SPEARS_RADIUS, .2, lock + 2);
			ElementFx.crack(level, at, SPEARS_RADIUS, lock + 6);
			heave(level, at, 6);
			Fx.sound(level, at, SoundEvents.ROOTED_DIRT_HIT, 1, .7F);
		}
	}

	/** The roots burst up through the rings. */
	private void erupt(ServerLevel level) {
		for (Vec3 at : marks) {
			for (LivingEntity e : foes(level, at, SPEARS_RADIUS + 1))
				if (flat(e.position(), at) <= SPEARS_RADIUS && e.getY() - at.y > -.5 && e.getY() - at.y < SPEARS_HEIGHT && strike(level, e, SPEARS_DAMAGE))
					Effects.push(e, new Vec3(0, .55, 0));
			for (int i = 0; i < 4; i++) {
				Vec3 base = at.add(ElementFx.flatDir(i * Math.PI / 2 + .4).scale(.45));
				ElementFx.ray(level, base, base.add(ElementFx.flatDir(i * Math.PI / 2 + .4).scale(.3)).add(0, SPEARS_HEIGHT - i * .3, 0), BARK, .22, 10);
			}
			ElementFx.stoneShards(level, at.add(0, .2, 0), ElementFx.groundBlock(level, at), 12, .35);
			Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), at.add(0, 1, 0), 14, .4, .2);
			ElementFx.earthImpact(level, at, 1);
			Fx.sound(level, at, SoundEvents.EVOKER_FANGS_ATTACK, 1.2F, .6F);
			Fx.sound(level, at, SoundEvents.ROOTS_BREAK, 1.4F, .6F);
		}
	}

	// ------------------------------------------------------------------ the lash: its lowest binding swung across before it

	private void lash(ServerLevel level, int t, LivingEntity target) {
		int back = moveTell + LASH_BACK;
		boolean twice = phase >= 3 && t > moveTell;
		track(target, t, twice ? back : moveTell, LASH_LOCK);
		Vec3 feet = position(), whip = binding();
		if (t == 0) { Fx.sound(level, whip, SoundEvents.HANGING_ROOTS_BREAK, 1.4F, .5F); Fx.sound(level, whip, SoundEvents.WOOD_HIT, 1, .5F); }
		int blow = twice ? back : moveTell;
		// In its last phase it hauls the whip straight back the other way.
		if (twice && t == moveTell + 1) { struck.clear(); side = -side; entityData.set(DATA_SIDE, (float) side); }
		if (t < blow) {
			// It uncoils the binding off its trunk and draws it back over its shoulder.
			if (t % 2 == 0) Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), whip, 2, .3, .02);
			if (t < blow - LASH_LOCK && t % 4 == 0) cone(level, aim, LASH_REACH, LASH_HALF, COLOR, 5);
			if (t == blow - LASH_LOCK) {
				cone(level, aim, LASH_REACH, LASH_HALF, ACCENT, LASH_LOCK + 2);
				Fx.sound(level, whip, SoundEvents.MANGROVE_ROOTS_HIT, 1.2F, .5F);
			}
			return;
		}
		if (t != blow) return;
		// The whip cracks across the cone and flings whoever it catches aside.
		Vec3 aside = ahead(aim - 90 * side);
		for (LivingEntity e : foes(level, feet, LASH_REACH + 1)) {
			double[] at = local(feet, aim, e.position());
			double dy = e.getY() - feet.y;
			if (inArc(at[0], at[1], LASH_REACH, LASH_HALF) && dy > -.5 && dy < LASH_HEIGHT && strike(level, e, LASH_DAMAGE)) {
				Effects.push(e, aside.scale(LASH_SHOVE).add(ahead(aim).scale(.2)).add(0, .3, 0));
				landed = true;
			}
		}
		ElementFx.slash(level, feet.add(0, 1, 0), new Vec3(0, 1, 0), ahead(aim), COLOR, LASH_REACH * .85, Math.toRadians(LASH_HALF * 2), .14, 2, 8);
		ElementFx.slash(level, feet.add(0, .8, 0), new Vec3(0, 1, 0), ahead(aim), BARK, LASH_REACH * .7, Math.toRadians(LASH_HALF * 2), .1, 2, 10);
		Vfx.emit(level, ParticleTypes.SWEEP_ATTACK, feet.add(ahead(aim).scale(3)).add(0, 1, 0), 3, 1.2, 0);
		Fx.sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, .5F);
		Fx.sound(level, feet, SoundEvents.MANGROVE_ROOTS_BREAK, 1.2F, .8F);
	}

	// ------------------------------------------------------------------ seed pods: lobbed out of its crown, bursting into briars

	private void seeds(ServerLevel level, int t, LivingEntity target) {
		if (target != null && t < moveTell - SEEDS_LOCK) turn(yawTo(target.position()), (float) TURN);
		Vec3 crown = position().add(0, 3, 0);
		if (t == 0) { Fx.sound(level, crown, SoundEvents.AZALEA_LEAVES_BREAK, 1.4F, .6F); Fx.sound(level, crown, SoundEvents.BIG_DRIPLEAF_TILT_DOWN, 1, .6F); }
		if (t < moveTell) {
			// Its crown swells and shakes, pods fattening among the leaves.
			if (t % 2 == 0) { Vfx.emit(level, ParticleTypes.PALE_OAK_LEAVES, crown, 2, .8, .02); Motes.glows(level, crown, 2, .7, BARK, .3, 10, Vec3.ZERO, .02); }
			if (t < moveTell - SEEDS_LOCK) {
				if (t % 3 == 0) for (Vec3 at : aimed(level, target)) ElementFx.groundRing(level, at.add(0, .05, 0), COLOR, SEEDS_RADIUS - .2, SEEDS_RADIUS, .1, 4);
			} else if (t == moveTell - SEEDS_LOCK) {
				marks.clear();
				marks.addAll(aimed(level, target));
				for (int i = marks.size(); i < RootGuardianRules.pods(phase); i++) {
					double a = level.getRandom().nextDouble() * Math.PI * 2, d = SEEDS_NEAR + 1 + level.getRandom().nextDouble() * (SEEDS_FAR - SEEDS_NEAR - 1);
					marks.add(ground(level, within(position().add(ElementFx.flatDir(a).scale(d)), leash() - 2)));
				}
				for (Vec3 at : marks) {
					ElementFx.groundRing(level, at.add(0, .05, 0), ACCENT, SEEDS_RADIUS - .25, SEEDS_RADIUS, .18, SEEDS_LOCK + SEEDS_FLIGHT + 2);
					Sigils.target(level, at.add(0, .06, 0), ACCENT, (float) SEEDS_RADIUS, SEEDS_LOCK + SEEDS_FLIGHT + 2);
				}
				Fx.sound(level, crown, SoundEvents.AZALEA_LEAVES_HIT, 1.2F, .5F);
			}
			return;
		}
		if (t == moveTell) {
			for (Vec3 at : marks) flying.add(new Pod(crown, at, level.getGameTime() + SEEDS_FLIGHT));
			Vfx.emit(level, ParticleTypes.PALE_OAK_LEAVES, crown, 12, 1, .1);
			Fx.sound(level, crown, SoundEvents.EGG_THROW, 1.4F, .5F);
			Fx.sound(level, crown, SoundEvents.AZALEA_LEAVES_BREAK, 1.2F, .8F);
		}
	}

	/** Where its pods aimed at its foes will land: along the line to each, never close in under its crown, never past its reach. */
	private List<Vec3> aimed(ServerLevel level, LivingEntity target) {
		List<Vec3> spots = new ArrayList<>();
		Vec3 from = position();
		for (LivingEntity e : nearest(level, target, RootGuardianRules.pods(phase))) {
			double d = flat(from, e.position());
			Vec3 dir = d < .1 ? ahead(getYRot()) : new Vec3(e.getX() - from.x, 0, e.getZ() - from.z).normalize();
			spots.add(ground(level, within(new Vec3(from.x, e.getY(), from.z).add(dir.scale(podReach(d))), leash() - 2)));
		}
		return spots;
	}

	// ------------------------------------------------------------------ sap: gushed from its heart down a lane, left lying in a pool

	private void sap(ServerLevel level, int t, LivingEntity target) {
		track(target, t, SAP_LOCK);
		Vec3 h = heart(), feet = position();
		if (t == 0) { Fx.sound(level, h, SoundEvents.HONEY_BLOCK_SLIDE, 1.4F, .5F); Fx.sound(level, h, SoundEvents.WOOD_HIT, 1, .4F); }
		if (t < moveTell) {
			// Its heart bulges and weeps sap.
			if (t % 2 == 0) Vfx.emit(level, ParticleTypes.DRIPPING_HONEY, h, 2, .25, 0);
			if (t % 3 == 0) Motes.glows(level, h, 2, .3, SAP_TINT, .25, 12, Vec3.ZERO, .01);
			if (t < moveTell - SAP_LOCK && t % 4 == 0) lane(level, aim, SAP_FROM, clear(level, aim, SAP_TO), SAP_HALF, COLOR, 5);
			if (t == moveTell - SAP_LOCK) {
				reach = clear(level, aim, SAP_TO);
				lane(level, aim, SAP_FROM, reach, SAP_HALF, ACCENT, SAP_LOCK + SAP_RUN + 2);
				Fx.sound(level, h, SoundEvents.HONEY_BLOCK_PLACE, 1.4F, .5F);
			}
			return;
		}
		int k = t - moveTell;
		if (k >= SAP_RUN) { if (k % 3 == 0) Vfx.emit(level, ParticleTypes.DRIPPING_HONEY, h, 1, .2, 0); return; }
		// The gush runs out down the lane, holding fast whoever it washes over.
		double was = Math.min(reach, front(k, SAP_SPEED, SAP_FROM)), now = Math.min(reach, front(k + 1, SAP_SPEED, SAP_FROM));
		if (k == 0) {
			Fx.sound(level, h, SoundEvents.HONEY_DRINK, 1.6F, .5F);
			Fx.sound(level, h, SoundEvents.HONEY_BLOCK_BREAK, 1.4F, .6F);
		}
		Vec3 f = ahead(aim);
		for (double d = was; d <= now; d += .5) {
			Vec3 at = feet.add(f.scale(d)).add(0, .3, 0);
			Vfx.emit(level, ParticleTypes.FALLING_HONEY, at.add(0, .5, 0), 3, SAP_HALF * .5, 0);
			Motes.glows(level, at, 2, SAP_HALF * .4, SAP_TINT, .35, 14, new Vec3(0, -.01, 0), .02);
		}
		for (LivingEntity e : foes(level, feet, reach + 1)) {
			double[] at = local(feet, aim, e.position());
			if (onLane(at[0], at[1], SAP_FROM, now, SAP_HALF) && Math.abs(e.getY() - feet.y) < 2 && strike(level, e, SAP_DAMAGE)) {
				MonsterMagic.root(level, e, SAP_HOLD);
				Vfx.emit(level, ParticleTypes.LANDING_HONEY, e.position().add(0, .2, 0), 8, .4, 0);
			}
		}
		if (k == SAP_RUN - 1) {
			if (pools.size() >= POOL_MOST) pools.removeFirst();
			pools.add(new Pool(ground(level, feet.add(f.scale(SAP_FROM))), ground(level, feet.add(f.scale(reach))), level.getGameTime() + POOL_TICKS));
		}
	}

	// ------------------------------------------------------------------ the creeping root: burrowed out after whoever keeps away

	private void creep(ServerLevel level, int t, LivingEntity target) {
		Vec3 feet = position(), leg = feet.add(ahead(getYRot() + 35 * side).scale(.7));
		if (t == 0) {
			caught = null;
			dragged = 0;
			tipYaw = aim;
			ElementFx.crack(level, leg, 1.2, moveTell + 10);
			Fx.sound(level, leg, SoundEvents.ROOTED_DIRT_BREAK, 1.4F, .5F);
			Fx.sound(level, leg, SoundEvents.SNIFFER_DIGGING, 1.2F, .6F);
		}
		if (t < moveTell) {
			// It stamps a leg down and the floor before it bulges where the root is gathering.
			track(target, t, 4);
			tip = ground(level, feet.add(ahead(aim).scale(1.4)));
			tipYaw = aim;
			if (t % 2 == 0) heave(level, leg, 3);
			if (t % 3 == 0) ElementFx.groundRing(level, tip.add(0, .05, 0), COLOR, .3, .6, .1, 4);
			if (t == moveTell - 4) {
				ElementFx.groundRing(level, tip.add(0, .05, 0), ACCENT, .4, .8, .16, 8);
				Fx.sound(level, tip, SoundEvents.ROOTED_DIRT_HIT, 1.2F, .5F);
			}
			return;
		}
		face(aim);
		if (caught != null) { drag(level); return; }
		if (landed) return;
		if (tip == null) tip = ground(level, feet.add(ahead(aim).scale(1.4)));
		if (t >= moveTell + CREEP_TICKS) {
			// It found no one, and has to wrench its leg back out of the floor.
			heave(level, tip, 10);
			Fx.sound(level, tip, SoundEvents.ROOTS_BREAK, 1.2F, .6F);
			tip = null;
			pose(STAGGER, 0, STAGGER_TICKS);
			return;
		}
		// The root runs under the floor after its prey, a mound of earth heaving along over it. It turns wide.
		if (target != null) tipYaw = steer(tipYaw, yawFrom(tip, target.position()), CREEP_TURN);
		tip = ground(level, within(tip.add(ahead(tipYaw).scale(CREEP_SPEED)), leash() - 1));
		heave(level, tip, 3);
		if (t % 2 == 0) ElementFx.groundRing(level, tip.add(0, .05, 0), COLOR, .25, .55, .1, 3);
		if (t % 4 == 0) Fx.sound(level, tip, SoundEvents.ROOTED_DIRT_STEP, 1.2F, .6F);
		if (t % 8 == 0) Fx.sound(level, tip, SoundEvents.SNIFFER_DIGGING, .8F, .8F);
		for (LivingEntity e : foes(level, tip, CREEP_CATCH + 1)) {
			double dy = e.getY() - tip.y;
			if (flat(e.position(), tip) > CREEP_CATCH || dy < -1 || dy > .8) continue;
			Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), tip.add(0, .5, 0), 16, .4, .2);
			if (e.isOnFire()) {
				// It closes on someone burning, and the fire runs back up the root into it.
				ElementFx.flames(level, tip, .6, 1.2, 14);
				for (double d = 0; d <= 1; d += .2) ElementFx.embers(level, tip.lerp(feet, d).add(0, .3, 0), .3, 3);
				Fx.sound(level, tip, SoundEvents.FIRECHARGE_USE, 1.2F, .8F);
				Fx.sound(level, feet, SoundEvents.FIRE_EXTINGUISH, 1.2F, .6F);
				tip = null;
				cut(level, true);
				if (move == CREEP) pose(STAGGER, 0, STAGGER_TICKS);
				return;
			}
			if (strike(level, e, CREEP_DAMAGE)) {
				caught = e;
				landed = true;
				MonsterMagic.root(level, e, DRAG_MOST + 4);
				entityData.set(DATA_GRIP, 1F);
				Fx.sound(level, tip, SoundEvents.ROOTS_BREAK, 1.4F, .5F);
				Fx.sound(level, tip, SoundEvents.MANGROVE_ROOTS_HIT, 1.4F, .5F);
				return;
			}
		}
	}

	/** It reels in whoever its root caught, hand over hand, until they're at its feet. */
	private void drag(ServerLevel level) {
		Vec3 feet = position();
		LivingEntity e = caught;
		if (!e.isAlive() || e.level() != level || ++dragged > DRAG_MOST || flat(e.position(), feet) <= DRAG_NEAR || flat(e.position(), feet) > leash() || e.isOnFire()) {
			if (e.isAlive() && e.isOnFire()) { ElementFx.embers(level, e.position().add(0, .5, 0), .4, 8); Fx.sound(level, e.position(), SoundEvents.FIRE_EXTINGUISH, 1, .8F); }
			release();
			return;
		}
		Vec3 in = new Vec3(feet.x - e.getX(), 0, feet.z - e.getZ()).normalize().scale(DRAG_SPEED);
		e.setDeltaMovement(in.x, Math.min(.1, e.getDeltaMovement().y), in.z);
		e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 6, 6, false, false, true));
		MonsterMagic.sync(e);
		tip = e.position();
		ElementFx.ray(level, feet.add(ahead(getYRot()).scale(.6)).add(0, .3, 0), e.position().add(0, .4, 0), BARK, .16, 2);
		if (dragged % 2 == 0) heave(level, e.position(), 3);
		if (dragged % 5 == 0) Fx.sound(level, e.position(), SoundEvents.ROOTED_DIRT_STEP, 1.2F, .5F);
	}

	private void release() {
		caught = null;
		tip = null;
		entityData.set(DATA_GRIP, 0F);
		moveLeft = Math.min(moveLeft, CREEP_RECOVER);
	}

	// ------------------------------------------------------------------ thorns: burst out along spokes all round it

	private void thorns(ServerLevel level, int t, LivingEntity target) {
		face(aim);
		Vec3 feet = position();
		int second = moveTell + THORNS_RUN + THORNS_SECOND;
		if (t == 0) {
			spokeOffset = 0;
			Fx.sound(level, feet, SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.4F, .5F);
			Fx.sound(level, feet, SoundEvents.WOOD_HIT, 1.2F, .5F);
		}
		if (t < moveTell) {
			// It hunches and its bark bristles, and lines split the floor out from it where the thorns will run.
			bristle(level, t);
			spokes(level, t, moveTell);
			return;
		}
		int k = t - moveTell;
		if (k < THORNS_RUN) { run(level, k); return; }
		if (phase < 3 || t >= second + THORNS_RUN) return;
		// In its last phase it bristles again at once, and the next thorns run out between the first.
		if (t == moveTell + THORNS_RUN) { struck.clear(); spokeOffset = 180.0 / THORNS_SPOKES; Fx.sound(level, feet, SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.4F, .7F); }
		if (t < second) { bristle(level, t); spokes(level, t - moveTell - THORNS_RUN, THORNS_SECOND); return; }
		run(level, t - second);
	}

	private void bristle(ServerLevel level, int t) {
		if (t % 2 != 0) return;
		Vec3 feet = position();
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()), feet.add(0, 1.4, 0), 3, .6, .05);
		Vfx.emit(level, ParticleTypes.CRIT, feet.add(0, 1.2, 0), 2, .7, .05);
	}

	/** The spokes on the floor through a tell of {@code tell} ticks, {@code t} in: following lines, then fixed bright ones. */
	private void spokes(ServerLevel level, int t, int tell) {
		int lock = Math.min(6, tell - 2);
		if (t < tell - lock ? t % 4 != 0 : t != tell - lock) return;
		boolean fixed = t >= tell - lock;
		Vec3 feet = position().add(0, .1, 0);
		for (int i = 0; i < THORNS_SPOKES; i++) {
			Vec3 d = ahead(aim + spokeOffset + i * 360.0 / THORNS_SPOKES);
			ElementFx.ray(level, feet.add(d.scale(THORNS_FROM)), feet.add(d.scale(THORNS_REACH)), fixed ? ACCENT : COLOR, fixed ? .16 : .1, fixed ? lock + THORNS_RUN + 1 : 5);
		}
		if (fixed) {
			ElementFx.crack(level, position(), 2, lock + 4);
			Fx.sound(level, feet, SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.4F, .5F);
		}
	}

	/** The thorns' front running out along every spoke, {@code k} ticks after they set off. */
	private void run(ServerLevel level, int k) {
		Vec3 feet = position();
		double was = k == 0 ? THORNS_FROM - .5 : Math.min(THORNS_REACH, front(k, THORNS_SPEED, THORNS_FROM)), now = Math.min(THORNS_REACH, front(k + 1, THORNS_SPEED, THORNS_FROM));
		if (k == 0) {
			Fx.sound(level, feet, SoundEvents.THORNS_HIT, 1.6F, .6F);
			Fx.sound(level, feet, SoundEvents.EVOKER_FANGS_ATTACK, 1.2F, .7F);
		}
		if (k % 2 == 0) Fx.sound(level, feet, SoundEvents.SWEET_BERRY_BUSH_BREAK, 1.2F, .6F + k * .05F);
		for (int i = 0; i < THORNS_SPOKES; i++) {
			Vec3 d = ahead(aim + spokeOffset + i * 360.0 / THORNS_SPOKES);
			for (double r = Math.max(THORNS_FROM, was); r <= now; r += .6) {
				Vec3 at = ground(level, feet.add(d.scale(r)));
				ElementFx.ray(level, at, at.add(d.scale(.15)).add(0, .9 + level.getRandom().nextDouble() * .7, 0), BARK, .1, 8);
				Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SWEET_BERRY_BUSH.defaultBlockState()), at.add(0, .4, 0), 2, .2, .05);
			}
		}
		for (LivingEntity e : foes(level, feet, THORNS_REACH + 1)) {
			double[] at = local(feet, aim, e.position());
			double along = spoke(at[0], at[1], THORNS_SPOKES, spokeOffset, THORNS_HALF), dy = e.getY() - feet.y;
			if (along >= THORNS_FROM - .5 && along <= now && dy > -.5 && dy < THORNS_HEIGHT && strike(level, e, THORNS_DAMAGE)) {
				throwFrom(e, feet, .3, .45);
				Vfx.emit(level, ParticleTypes.CRIT, e.position().add(0, 1, 0), 8, .3, .1);
			}
		}
	}

	// ------------------------------------------------------------------ uprooting: its legs torn out of the floor and a charge

	private void uproot(ServerLevel level, int t, LivingEntity target) {
		Vec3 feet = position();
		int again = moveTell + CHARGE_TICKS, second = again + RETELL;
		boolean twice = phase >= 3;
		if (t == 0) {
			Fx.sound(level, feet, SoundEvents.ROOTED_DIRT_BREAK, 1.6F, .5F);
			Fx.sound(level, feet, SoundEvents.WOOD_BREAK, 1.2F, .5F);
			Fx.sound(level, feet, SoundEvents.RAVAGER_ROAR, 1, .5F);
		}
		if (t < moveTell) { aimCharge(level, t, moveTell, target); return; }
		if (t < again) { charge(level, t - moveTell); return; }
		if (!twice) return;
		if (t < second) {
			// In its last phase it digs its feet in, wheels after its prey and goes again.
			if (t == again) { struck.clear(); setDeltaMovement(0, getDeltaMovement().y, 0); Fx.sound(level, feet, SoundEvents.RAVAGER_STEP, 1.4F, .5F); }
			aimCharge(level, t - again, RETELL, target);
			return;
		}
		charge(level, t - second);
	}

	/** Its legs tearing free while it turns after its prey, {@code t} into a wind-up of {@code tell}; the lane it will run fixed near the end. */
	private void aimCharge(ServerLevel level, int t, int tell, LivingEntity target) {
		track(target, t, tell, UPROOT_LOCK);
		Vec3 feet = position();
		double most = CHARGE_SPEED * CHARGE_TICKS + 1;
		if (t % 2 == 0) for (int s = -1; s <= 1; s += 2) heave(level, feet.add(ahead(getYRot() + 90 * s).scale(.45)), 3);
		if (t % 5 == 0) Fx.sound(level, feet, SoundEvents.ROOTS_BREAK, 1.2F, .5F + t * .02F);
		if (t < tell - UPROOT_LOCK) { if (t % 4 == 0) lane(level, aim, .5, clear(level, aim, most), CHARGE_HALF, COLOR, 5); }
		else if (t == tell - UPROOT_LOCK) {
			reach = clear(level, aim, most);
			lane(level, aim, .5, reach, CHARGE_HALF, ACCENT, UPROOT_LOCK + 6);
			ElementFx.crack(level, feet, 1.5, UPROOT_LOCK + 6);
			Fx.sound(level, feet, SoundEvents.WOOD_FALL, 1.4F, .5F);
		}
	}

	/** Its charge, {@code k} ticks in: it bowls over whoever stands in its lane, and runs itself into the wall if it meets one. */
	private void charge(ServerLevel level, int k) {
		Vec3 feet = position(), f = ahead(aim);
		face(aim);
		if (k == 0) {
			charged = 0;
			Fx.sound(level, feet, SoundEvents.RAVAGER_ROAR, 1.4F, .6F);
			Fx.sound(level, feet, SoundEvents.WOOD_FALL, 1.2F, .5F);
		}
		boolean stopped = charged >= reach;
		if (k > 1 && (horizontalCollision || stopped && reach < CHARGE_SPEED * CHARGE_TICKS)) {
			// It runs headlong into the wall, and its roots tangle round it.
			walled = true;
			setDeltaMovement(0, getDeltaMovement().y, 0);
			Vec3 front = feet.add(f.scale(1)).add(0, 1, 0);
			ElementFx.earthImpact(level, front, 1.4);
			ElementFx.stoneShards(level, front, ElementFx.groundBlock(level, feet), 16, .4);
			Fx.sound(level, front, SoundEvents.WOOD_BREAK, 1.6F, .4F);
			Fx.sound(level, front, SoundEvents.RAVAGER_STUNNED, 1.4F, .6F);
			pose(TANGLED, 0, TANGLED_WALL);
			return;
		}
		if (stopped || home != null && flat(feet, Vec3.atBottomCenterOf(home)) > CHARGE_HOME) {
			setDeltaMovement(0, getDeltaMovement().y, 0);
			return;
		}
		setDeltaMovement(f.x * CHARGE_SPEED, getDeltaMovement().y, f.z * CHARGE_SPEED);
		charged += CHARGE_SPEED;
		heave(level, feet, 4);
		if (k % 2 == 0) ElementFx.crack(level, feet, 1, 12);
		if (k % 3 == 0) Fx.sound(level, feet, SoundEvents.RAVAGER_STEP, 1.4F, .5F);
		for (LivingEntity e : foes(level, feet, 3)) {
			double[] at = local(feet, aim, e.position());
			double dy = e.getY() - feet.y;
			if (at[0] > -.6 && at[0] < 1.8 && Math.abs(at[1]) <= CHARGE_HALF && dy > -.5 && dy < 2.8 && strike(level, e, TRAMPLE_DAMAGE)) {
				Vec3 out = ahead(aim + (at[1] >= 0 ? 90 : -90));
				Effects.push(e, f.scale(TRAMPLE_SHOVE).add(out.scale(.45)).add(0, .4, 0));
				ElementFx.earthImpact(level, e.position(), .8);
				Fx.sound(level, e.position(), SoundEvents.RAVAGER_ATTACK, 1.2F, .7F);
			}
		}
	}

	// ------------------------------------------------------------------ pollen: a golden cloud shaken out of its crown

	private void pollen(ServerLevel level, int t, LivingEntity target) {
		if (target != null) turn(yawTo(target.position()), 8);
		Vec3 feet = position(), crown = feet.add(0, 3, 0);
		if (t == 0) {
			Fx.sound(level, crown, SoundEvents.AZALEA_LEAVES_BREAK, 1.4F, .5F);
			Fx.sound(level, crown, SoundEvents.SPORE_BLOSSOM_BREAK, 1.4F, .6F);
		}
		if (t < moveTell) {
			// It shakes its crown harder and harder, gold dust sifting down out of it.
			int n = 1 + t / 4;
			Vfx.emit(level, ParticleTypes.FALLING_SPORE_BLOSSOM, crown, n, .9, 0);
			Motes.glows(level, crown, n, .8, POLLEN_TINT, .25, 14, new Vec3(0, -.03, 0), .03);
			if (t % 4 == 0) Fx.sound(level, crown, SoundEvents.AZALEA_LEAVES_HIT, 1.2F, .6F + t * .03F);
			if (t == moveTell - 6) {
				ElementFx.groundRing(level, feet.add(0, .05, 0), POLLEN_TINT, CLOUD_RADIUS - .25, CLOUD_RADIUS, .16, 8);
				Fx.sound(level, crown, SoundEvents.CHERRY_LEAVES_BREAK, 1.4F, .5F);
			}
			return;
		}
		if (t != moveTell) return;
		// The cloud bursts out of it and hangs: it saps the strength of all in it, and a spark sets it all off.
		cloudUntil = level.getGameTime() + cloudTicks(phase);
		entityData.set(DATA_CLOUD, 1F);
		Motes.clouds(level, feet.add(0, 1.4, 0), 30, CLOUD_RADIUS * .6, POLLEN_TINT, .9, 40, Vec3.ZERO, .04, .6);
		Vfx.emit(level, ParticleTypes.SPORE_BLOSSOM_AIR, feet.add(0, 1.5, 0), 40, CLOUD_RADIUS * .6, .02);
		Fx.sound(level, crown, SoundEvents.SPORE_BLOSSOM_BREAK, 1.6F, .5F);
		Fx.sound(level, crown, SoundEvents.AZALEA_LEAVES_BREAK, 1.6F, .7F);
	}

	// ------------------------------------------------------------------ the canopy: boughs brought down on shadows round everyone

	/** Picks who its boughs fall on, and says how many fall in all: one on each of them and more about. */
	private int boughs(ServerLevel level, LivingEntity target) {
		marked.clear();
		marked.addAll(nearest(level, target, 4));
		drops = Mth.clamp(marked.size() * 2, 4, 8);
		return drops;
	}

	private void canopy(ServerLevel level, int t, LivingEntity target) {
		if (target != null && t < moveTell - CANOPY_LOCK) turn(yawTo(target.position()), (float) TURN);
		Vec3 feet = position(), crown = feet.add(0, 3.2, 0);
		if (t == 0) {
			Fx.sound(level, crown, SoundEvents.WOOD_PLACE, 1.6F, .4F);
			Fx.sound(level, crown, SoundEvents.CHERRY_LEAVES_BREAK, 1.4F, .5F);
			Fx.sound(level, feet, SoundEvents.BIG_DRIPLEAF_TILT_DOWN, 1.4F, .5F);
		}
		if (t < moveTell) {
			// It raises its arms high and spreads them, and the boughs overhead creak and drop leaves on whoever stands below.
			if (t % 2 == 0) Vfx.emit(level, ParticleTypes.PALE_OAK_LEAVES, crown, 3, 1.2, .02);
			if (t % 5 == 0) Fx.sound(level, crown, SoundEvents.WOOD_HIT, 1.2F, .4F + t * .02F);
			if (t < moveTell - CANOPY_LOCK) {
				if (t % 3 == 0) for (LivingEntity e : marked) if (e.isAlive()) shadow(level, ground(level, e.position()), COLOR, 4);
			} else if (t == moveTell - CANOPY_LOCK) {
				marks.clear();
				for (LivingEntity e : marked) if (e.isAlive() && e.level() == level) marks.add(ground(level, e.position()));
				for (int tries = 0; marks.size() < drops && tries < 40; tries++) {
					double a = level.getRandom().nextDouble() * Math.PI * 2, d = 2.5 + level.getRandom().nextDouble() * 7;
					Vec3 at = ground(level, within(feet.add(ElementFx.flatDir(a).scale(d)), leash() - 2));
					if (marks.stream().allMatch(m -> flat(m, at) > CANOPY_RADIUS * 1.6)) marks.add(at);
				}
				for (int i = 0; i < marks.size(); i++) {
					Vec3 at = marks.get(i);
					shadow(level, at, ACCENT, CANOPY_LOCK + i * CANOPY_EVERY + 2);
					Sigils.target(level, at.add(0, .06, 0), ACCENT, (float) CANOPY_RADIUS, CANOPY_LOCK + i * CANOPY_EVERY + 2);
				}
				Fx.sound(level, crown, SoundEvents.WOOD_BREAK, 1.4F, .5F);
			}
			return;
		}
		int i = t - moveTell;
		if (i % CANOPY_EVERY != 0 || i / CANOPY_EVERY >= marks.size()) return;
		// One by one the boughs come crashing down on their shadows.
		Vec3 at = marks.get(i / CANOPY_EVERY);
		for (LivingEntity e : foes(level, at, CANOPY_RADIUS + 1))
			if (flat(e.position(), at) <= CANOPY_RADIUS && e.getY() - at.y > -.5 && e.getY() - at.y < 2.5 && strike(level, e, CANOPY_DAMAGE))
				Effects.push(e, new Vec3(0, -.2, 0));
		ElementFx.ray(level, at.add(0, 7, 0), at, BARK, .4, 5);
		ElementFx.earthImpact(level, at, 1.3);
		ElementFx.stoneShards(level, at.add(0, .2, 0), ElementFx.groundBlock(level, at), 10, .35);
		Vfx.emit(level, ParticleTypes.PALE_OAK_LEAVES, at.add(0, 1, 0), 18, 1, .1);
		Sigils.flash(level, at.add(0, .1, 0), ACCENT, (float) CANOPY_RADIUS);
		Fx.sound(level, at, SoundEvents.WOOD_BREAK, 1.6F, .5F);
		Fx.sound(level, at, SoundEvents.AZALEA_LEAVES_BREAK, 1.2F, .6F);
	}

	/** A bough's shadow on the floor at {@code at}. */
	private void shadow(ServerLevel level, Vec3 at, int color, int lifetime) {
		ElementFx.groundRing(level, at.add(0, .05, 0), color, CANOPY_RADIUS - .3, CANOPY_RADIUS, color == ACCENT ? .2 : .1, lifetime);
		Vfx.emit(level, ParticleTypes.PALE_OAK_LEAVES, at.add(0, 4, 0), 2, .8, 0);
	}

	// ------------------------------------------------------------------ what its moves leave it in

	/** Its lash met nothing and the binding sank into the floor: it can't turn until it wrenches it out, so its flanks lie open. */
	private void snagged(ServerLevel level, int t) {
		face(getYRot());
		Vec3 end = ground(level, position().add(ahead(getYRot() - 50 * side).scale(3.2)));
		if (t == 0) {
			ElementFx.crack(level, end, 1, SNAG_TICKS);
			Fx.sound(level, end, SoundEvents.MANGROVE_ROOTS_PLACE, 1.4F, .5F);
			Fx.sound(level, end, SoundEvents.WOOD_HIT, 1.2F, .5F);
		}
		if (t % 2 == 0) ElementFx.ray(level, binding(), end.add(0, .1, 0), BARK, .14, 3);
		if (t % 4 == 0) Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), end.add(0, .3, 0), 4, .3, .05);
		if (t % 10 == 5) Fx.sound(level, end, SoundEvents.ROOTS_HIT, 1, .6F);
		if (t == SNAG_TICKS - 6) { heave(level, end, 12); Fx.sound(level, end, SoundEvents.ROOTS_BREAK, 1.4F, .6F); }
	}

	/** Its own roots wound round its legs after a charge: it takes harder blows through its bindings until it tears free. */
	private void tangled(ServerLevel level, int t) {
		setDeltaMovement(0, getDeltaMovement().y, 0);
		Vec3 feet = position();
		if (t == 0) {
			ElementFx.leafSpiral(level, feet, 1, 1.4, 10);
			Fx.sound(level, feet, SoundEvents.HANGING_ROOTS_PLACE, 1.4F, .5F);
			Fx.sound(level, feet, SoundEvents.ROOTS_PLACE, 1.4F, .6F);
		}
		if (t % 3 == 0) Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), feet.add(0, .6, 0), 3, .5, .05);
		if (t % 10 == 0) { ElementFx.groundRing(level, feet.add(0, .05, 0), COLOR, .6, 1, .1, 8); Fx.sound(level, feet, SoundEvents.ROOTS_HIT, 1.2F, .5F); }
	}

	/** It wrenches its leg back up out of the floor, its root having found no one. */
	private void stagger(ServerLevel level, int t) {
		Vec3 leg = position().add(ahead(getYRot() + 35 * side).scale(.7));
		if (t == 0) { heave(level, leg, 14); Fx.sound(level, leg, SoundEvents.ROOTED_DIRT_BREAK, 1.4F, .6F); Fx.sound(level, leg, SoundEvents.WOOD_HIT, 1.2F, .4F); }
		if (t % 4 == 0) heave(level, leg, 3);
		if (t == STAGGER_TICKS - 8) { heave(level, leg, 10); Fx.sound(level, leg, SoundEvents.ROOTS_BREAK, 1.2F, .7F); }
	}

	/** Thrown back with its last binding cut, its heart bare. */
	private void reel(ServerLevel level, int t) {
		if (t == 0) {
			Vec3 back = ahead(getYRot()).scale(-1);
			setDeltaMovement(back.x * .4, .1, back.z * .4);
		}
		if (t % 4 == 0) Vfx.emit(level, ParticleTypes.PALE_OAK_LEAVES, position().add(0, 3, 0), 4, 1, .02);
	}

	// ------------------------------------------------------------------ how its moves end, and what it reaches for next

	@Override protected void moveDone(ServerLevel level, int move, LivingEntity target) {
		switch (move) {
			// Its whip met no one and sank into the floor: it can't turn until it wrenches it out.
			case LASH -> { if (landed) runOn(level, move, target); else pose(SNAGGED, 0, SNAG_TICKS); }
			// Its charge spent, its own roots wind round its legs.
			case UPROOT -> { setDeltaMovement(0, getDeltaMovement().y, 0); pose(TANGLED, 0, TANGLED_TICKS); }
			default -> runOn(level, move, target);
		}
	}

	/** In its last phase, a move may run straight on into another with a shorter tell. */
	private void runOn(ServerLevel level, int move, LivingEntity target) {
		if (target == null || move > PICKS.length) return;
		int next = chain(phase, move, distanceTo(target), bindings, random.nextDouble());
		if (next <= 0 || RootGuardianRules.wakes(next) > phase) return;
		deck.played(next);
		start(level, next, target, true);
	}

	@Override protected void moveBroken(ServerLevel level, int move) {
		caught = null;
		tip = null;
		entityData.set(DATA_GRIP, 0F);
		setState(SLAMMING, false);
		struck.clear();
		marks.clear();
		marked.clear();
		setDeltaMovement(0, getDeltaMovement().y, 0);
	}

	@Override protected int rest(ServerLevel level, int move) { return RootGuardianRules.rest(phase, move, random.nextDouble()); }

	@Override protected void weigh(ServerLevel level, LivingEntity target, double[] w) {
		List<LivingEntity> all = nearest(level, target, 8);
		int crowd = (int) all.stream().filter(e -> distanceTo(e) <= CROWD).count();
		double[] mine = weights(phase, distanceTo(target), wrap(yawTo(target.position()) - getYRot()), target.getY() - getY(), bindings, crowd,
			all.size(), cloudUntil > level.getGameTime(), briars.size());
		System.arraycopy(mine, 0, w, 0, Math.min(mine.length, w.length));
	}

	@Override protected double spellWeight(ServerLevel level, LivingEntity target, int index) { return RootGuardianRules.spellWeight(distanceTo(target)); }

	// ------------------------------------------------------------------ between phases: growing fresh roots

	@Override protected void onPhase(ServerLevel level, int phase) {
		bindings = 3;
		exposedUntil = 0;
		entityData.set(BINDINGS, bindings);
		flying.clear();
		briars.clear();
		pools.clear();
		cloudUntil = 0;
		entityData.set(DATA_CLOUD, 0F);
		Vec3 c = position();
		Sigils.ground(level, c.add(0, .05, 0), COLOR, ACCENT, 5, SHIFT_TICKS + 10);
		ElementFx.leafSpiral(level, c, 1.8, 4, 30);
		ElementFx.crack(level, c, 3, SHIFT_TICKS);
		Fx.sound(level, c, DungeonSounds.BOSS_PHASE, 2, 1);
		Fx.sound(level, c, SoundEvents.ROOTED_DIRT_BREAK, 1.6F, .5F);
		announce(level, "message.wildercord.root_guardian_phase", NAME);
	}

	@Override protected void shiftTick(ServerLevel level, int left) {
		Vec3 c = position();
		float tall = getBbHeight();
		// It sinks its legs deep and throws its arms up, sap rising gold through its heart and leaves whirling up round it as fresh
		// roots knot over it; then it stamps, and a ring of roots bursts up round it and throws everyone back.
		if (left % 3 == 0) { ElementFx.leafSpiral(level, c, 1.4, tall + 1, 6); ElementFx.petals(level, c.add(0, tall, 0), 1.2, 3); }
		if (left % 4 == 0) ElementFx.swirl(level, c, 1.8, tall + 1, 2, COLOR, ACCENT);
		if (left % 2 == 0) heave(level, c.add(ElementFx.flatDir(left * .7).scale(1.2)), 3);
		Motes.glows(level, heart(), 1, .2, ACCENT, .3, 10, new Vec3(0, .05, 0), .02);
		if (left % 10 == 0 && left > 20) ElementFx.groundRing(level, c.add(0, .05, 0), COLOR, SHIFT_REACH - .3, SHIFT_REACH, .14, 10);
		if (left == 50) Fx.sound(level, c, SoundEvents.ROOTED_DIRT_BREAK, 1.6F, .5F);
		if (left == 40) Fx.sound(level, c, SoundEvents.WOOD_FALL, 1.6F, .4F);
		if (left == 30) Fx.sound(level, c, SoundEvents.RAVAGER_ROAR, 1.4F, .5F);
		if (left == 20) {
			setState(SLAMMING, true);
			ElementFx.groundRing(level, c.add(0, .05, 0), ACCENT, SHIFT_REACH - .3, SHIFT_REACH, .24, 18);
			ElementFx.crack(level, c, SHIFT_REACH, 20);
			Fx.sound(level, c, SoundEvents.MANGROVE_ROOTS_HIT, 1.6F, .5F);
		}
		if (left != 4) return;
		setState(SLAMMING, false);
		for (LivingEntity t : foes(level, c, SHIFT_REACH))
			if (flat(t.position(), c) <= SHIFT_REACH && Math.abs(t.getY() - c.y) < 2.5) { hurt(level, t, SHIFT_DAMAGE); throwFrom(t, c, 1.4, .5); }
		ElementFx.groundRing(level, c.add(0, .05, 0), COLOR, .5, SHIFT_REACH + .5, .3, 14);
		ElementFx.earthImpact(level, c, 2.5);
		for (int i = 0; i < 10; i++) {
			Vec3 at = ground(level, c.add(ElementFx.flatDir(i * Math.PI / 5).scale(SHIFT_REACH * .75)));
			ElementFx.ray(level, at, at.add(ElementFx.flatDir(i * Math.PI / 5).scale(.3)).add(0, 2.2, 0), BARK, .2, 12);
		}
		Fx.sound(level, c, SoundEvents.ROOTS_BREAK, 2, .5F);
		Fx.sound(level, c, SoundEvents.WOOD_BREAK, 1.8F, .4F);
	}

	/** What it's doing on the boss bar, and whether its heart lies bare; just its bindings while it's between moves. */
	@Override protected Component status() {
		Component open = bindings == 0 ? Component.translatable("boss.wildercord.root_open").withColor(ACCENT) : Component.translatable("boss.wildercord.root_bound", bindings);
		if (move <= 0 || move > POSES) return open;
		Component doing = Component.translatable("boss.wildercord.root_guardian.move." + RootGuardianRules.key(move)).withColor(move > PICKS.length ? ACCENT : NAME);
		return bindings == 0 && move != REEL ? Component.translatable("boss.wildercord.status", doing, open) : doing;
	}

	// ------------------------------------------------------------------ death

	@Override protected void dying(ServerLevel level, int tick) {
		Vec3 c = position().add(0, getBbHeight() * .5, 0);
		// It splits and sags, its leaves coming down in drifts and its roots letting go of the floor.
		if (tick % 2 == 0) { Vfx.emit(level, ParticleTypes.PALE_OAK_LEAVES, c.add(0, 1.2, 0), 3, 1, .02); heave(level, position(), 2); }
		if (tick % 6 == 0) Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), c, 6, .6, .05);
		if (tick % 4 == 0) Motes.glow(level, heart(), ACCENT, .3 * (1 - tick / (double) DEATH_TICKS), 10, new Vec3(0, .03, 0), .02);
		if (tick == 1) { Fx.sound(level, c, SoundEvents.WOOD_BREAK, 2, .4F); Fx.sound(level, c, SoundEvents.RAVAGER_DEATH, 1.2F, .6F); }
		if (tick % 12 == 0) Fx.sound(level, c, SoundEvents.WOOD_FALL, 1.2F, .5F + tick / 120F);
	}

	@Override protected void vanish(ServerLevel level) {
		Vec3 at = position(), c = at.add(0, .6, 0);
		ElementFx.leafSpiral(level, at, 1.2, 3, 20);
		ElementFx.petals(level, c.add(0, 1, 0), 1.4, 24);
		ElementFx.bloom(level, c, at, 2);
		Motes.burst(level, c.add(0, .8, 0), 20, ACCENT, .25, 24, .2);
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()), c, 30, 1, .15);
		Fx.sound(level, at, SoundEvents.MANGROVE_ROOTS_BREAK, 1.6F, .5F);
	}

	@Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WOOD_HIT; }
	@Override protected SoundEvent getDeathSound() { return null; }

	@Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putInt("bindings", bindings); out.putLong("exposed_until", exposedUntil); }
	@Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); bindings = Math.clamp(in.getIntOr("bindings", 3), 0, 3); exposedUntil = in.getLongOr("exposed_until", 0); }
}
