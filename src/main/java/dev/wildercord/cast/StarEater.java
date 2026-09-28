package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.content.dungeons.DungeonSounds;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Star-Eater, keeper of the Astral Observatory: a floating knot of void with an eye at its heart
 * and star shards circling it. While its shard shield is up it turns every spell back on whoever
 * cast it: the Shield mechanics in reverse. It's a real Shield (see {@link Shields}), so a spell that
 * costs more mana than the shield is strong breaks it, the way it would break yours; so does a star
 * shard it throws, knocked back into it (parried) with a blade, a bolt or a Shield of your own.
 * Broken, it drops and lies open for a few seconds, then grows its shield again, stronger each phase.
 */
public class StarEater extends DungeonBoss {
	private static final int COLOR = 0xB45AF0;
	private static final int ACCENT = 0xE8D8FF;
	/** How long its shield takes to grow back once broken, by phase. */
	private static final int[] OPEN_TICKS = {0, 180, 160, 130};
	/** Its shield's strength (the mana a spell must cost to break it), by phase. */
	private static final float[] STRENGTH = {0, 20, 28, 36};
	/** How much of its shield a parried shard knocks off. */
	private static final float SHARD_CRACK = 16;
	private static final int SHIELD_TICKS = 20 * 60 * 20;
	private static final List<String> SHIELD_RUNES = List.of(Runes.SHIELD.id(), Runes.STARFALL.id(), Runes.INFINITY.id());

	private static final List<List<RuneDef>> PHASE_1 = List.of(
		List.of(Runes.SPARK, Runes.HARM, Runes.VOLLEY_MOD),
		List.of(Runes.ORB, Runes.PULL),
		List.of(Runes.BOLT, Runes.BLIND));
	private static final List<List<RuneDef>> PHASE_2 = List.of(
		List.of(Runes.COMET, Runes.BLACKFLAME),
		List.of(Runes.RAIN, Runes.HARM, Runes.WIDEN),
		List.of(Runes.BEAM, Runes.BANISH),
		List.of(Runes.BOLT, Runes.HEX, Runes.DELAY, Runes.LANCE, Runes.HARM));
	private static final List<List<RuneDef>> PHASE_3 = List.of(
		List.of(Runes.ZONE, Runes.STARFALL),
		List.of(Runes.LANCE, Runes.HARM, Runes.AMPLIFY),
		List.of(Runes.BOLT, Runes.PULL, Runes.DELAY, Runes.COMET, Runes.HARM),
		List.of(Runes.DOMAIN, Runes.HEX, Runes.HARM));

	/** A star shard in flight: thrown at a foe, or knocked back at the Star-Eater. */
	private static final class Shard {
		Vec3 pos;
		Vec3 motion;
		LivingEntity target;
		Player parriedBy;
		int age;
	}

	private final List<Shard> shards = new ArrayList<>();
	private long openUntil;
	private long nextVolley;
	private long volleyAt;
	private double orbit;
	private int reflections;
	private int parries;
	private final Map<UUID, Long> told = new HashMap<>();

	public StarEater(EntityType<? extends StarEater> type, Level level) {
		super(type, level, BossEvent.BossBarColor.PURPLE);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 320.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/** Wakes the Star-Eater over its star-altar: the stars in the floor go dark, and it opens its eye. */
	public static StarEater rise(ServerLevel level, BlockPos altar) {
		StarEater boss = place(level, DungeonEntities.STAR_EATER, altar, 3.0);
		if (boss == null) {
			return null;
		}
		Vec3 at = Vec3.atBottomCenterOf(altar).add(0, 3.5, 0);
		Sigils.ground(level, Vec3.atBottomCenterOf(altar).add(0, 1.02, 0), COLOR, ACCENT, 6.0F, 80);
		ElementFx.implode(level, at, 6.0, 12);
		ElementFx.starSeal(level, at, new Vec3(0, 1, 0), 3.0, 60);
		Vfx.radial(level, ParticleTypes.END_ROD, at, 50, 0.3);
		Fx.sound(level, at, DungeonSounds.BOSS_RISE, 2.0F, 1.0F);
		Fx.sound(level, at, SoundEvents.END_PORTAL_SPAWN, 0.6F, 1.4F);
		boss.raiseShield(level);
		boss.announce(level, "message.wildercord.star_eater_wakes", COLOR);
		return boss;
	}

	@Override
	protected List<List<RuneDef>> spells(int phase) {
		return phase == 1 ? PHASE_1 : phase == 2 ? PHASE_2 : PHASE_3;
	}

	@Override
	protected String feat() {
		return Feats.STAR_EATER;
	}

	@Override
	protected int color() {
		return COLOR;
	}

	@Override
	protected int accent() {
		return ACCENT;
	}

	@Override
	protected double castRange() {
		return 32.0;
	}

	@Override
	protected double leash() {
		return 20.0;
	}

	// ------------------------------------------------------------------ its shard shield

	/** Whether its shield is up right now. */
	public boolean shielded() {
		return Shields.strength(this) > 0;
	}

	/** Its shield's strength: the mana a spell must cost to break it. */
	public float shieldStrength() {
		return Shields.strength(this);
	}

	/** How many spells it has turned back, and how many of its shards have been parried (for the tests). */
	public int reflections() {
		return reflections;
	}

	public int parries() {
		return parries;
	}

	private void raiseShield(ServerLevel level) {
		Shields.give(this, STRENGTH[phase], SHIELD_TICKS, SHIELD_RUNES);
		setState(GUARDED, true);
		setState(EXPOSED, false);
		Vec3 c = getBoundingBox().getCenter();
		ElementFx.orbit(level, c, 2.2, 8, 30, COLOR, ACCENT);
		Sigils.layer(level, c, new Vec3(0, 1, 0), SigilOption.RING, ACCENT, 2.4F, 30, 0.2F);
		Fx.sound(level, c, WildercordSounds.SHIELD_UP, 1.4F, 0.8F);
	}

	/**
	 * A spell its shield stopped (see {@link Shields}): the shield holds, and the spell comes back out
	 * of it, a moment later, at whoever cast it.
	 */
	void reflect(Cast cast, float strength) {
		if (!(level() instanceof ServerLevel level) || !isAlive() || cast.caster == this) {
			return;
		}
		// The block spent the Shield: its shard shield doesn't spend. It holds at the strength it had,
		// so shards parried into it before still count.
		Shields.give(this, strength, SHIELD_TICKS, SHIELD_RUNES);
		LivingEntity back = cast.caster;
		List<RuneDef> spell = reflected(cast.info.spell());
		reflections++;
		Vec3 c = getBoundingBox().getCenter();
		Fx.sound(level, c, DungeonSounds.STAR_EATER_REFLECT, 1.4F, 1.0F);
		Vec3 toward = back.getEyePosition().subtract(c);
		Sigils.circle(level, c.add(toward.normalize().scale(1.6)), toward, ACCENT, COLOR, 1.6F, 16);
		if (back instanceof ServerPlayer player) {
			long now = level.getGameTime();
			Long last = told.get(player.getUUID());
			if (last == null || now - last > 200) {
				told.put(player.getUUID(), now);
				player.sendOverlayMessage(Component.translatable("message.wildercord.star_eater_reflects", Math.round(STRENGTH[phase])).withColor(ACCENT));
			}
		}
		Scheduler.later(6, () -> {
			if (isAlive() && back.isAlive() && back.level() == level && shifting == 0) {
				Runebound.aimAt(this, back);
				Runebound.cast(level, this, spell, power());
			}
		});
	}

	/** The harmful part of a spell, to throw back: its shapes, modifiers, links and harmful effects. */
	static List<RuneDef> reflected(List<RuneDef> spell) {
		List<RuneDef> out = new ArrayList<>();
		boolean harms = false;
		for (RuneDef rune : spell) {
			if (rune == Runes.IMBUE) {
				// What follows an Imbue is stored, not cast: nothing of it comes back.
				break;
			}
			if (rune.family() == RuneFamily.EFFECT) {
				if (rune.kind() != EffectKind.HARMFUL) {
					continue;
				}
				harms = true;
			}
			if (out.isEmpty() && (rune == Runes.SELF || rune == Runes.TOUCH)) {
				// Its own body or its reach mean nothing thrown back from across the room: it flies.
				out.add(Runes.BOLT);
				continue;
			}
			out.add(rune);
		}
		if (!harms || out.isEmpty() || out.getFirst().family() != RuneFamily.SHAPE) {
			return List.of(Runes.BOLT, Runes.HARM);
		}
		return out;
	}

	private void shieldBroken(ServerLevel level, long now) {
		setState(GUARDED, false);
		setState(EXPOSED, true);
		openUntil = now + OPEN_TICKS[phase];
		interrupt();
		volleyAt = 0;
		Vec3 c = getBoundingBox().getCenter();
		Vfx.radial(level, ParticleTypes.END_ROD, c, 40, 0.45);
		ElementFx.shards(level, c, 2.5, 16);
		Sigils.flash(level, c, ACCENT, 3.0F);
		Fx.sound(level, c, DungeonSounds.STAR_EATER_HURT, 1.6F, 0.8F);
		for (ServerPlayer player : level.players()) {
			if (player.distanceTo(this) < 40) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.star_eater_open").withColor(ACCENT));
			}
		}
	}

	// ------------------------------------------------------------------ drifting, and its star shards

	@Override
	protected void approach(ServerLevel level, LivingEntity target) {
	}

	@Override
	protected boolean mayCast(ServerLevel level, LivingEntity target) {
		return volleyAt == 0 && !state(EXPOSED);
	}

	@Override
	protected void mechanic(ServerLevel level, long now) {
		// Drifting in a slow circle high over the altar; sagging low while its shield is down.
		orbit += state(EXPOSED) ? 0.004 : 0.012;
		Vec3 centre = Vec3.atBottomCenterOf(home);
		double height = state(EXPOSED) ? 2.2 : 5.0 + Math.sin(tickCount * 0.05) * 0.6;
		Vec3 goal = centre.add(Math.cos(orbit) * 4.5, height, Math.sin(orbit) * 4.5);
		Vec3 pull = goal.subtract(position()).scale(0.06);
		if (pull.length() > 0.35) {
			pull = pull.normalize().scale(0.35);
		}
		setDeltaMovement(pull);
		LivingEntity target = getTarget();
		if (target != null) {
			getLookControl().setLookAt(target, 30, 30);
		}
		if (now % 6 == 0) {
			Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, getBoundingBox().getCenter(), 2, 0.6, 0.02);
		}
		// Its shield: broken (by a spell that cost more, or a parried shard), then grown back.
		boolean up = shielded();
		if (state(GUARDED) && !up) {
			shieldBroken(level, now);
		} else if (state(EXPOSED) && now >= openUntil || !state(GUARDED) && !state(EXPOSED)) {
			// Grown back; or never raised since it was loaded (its pose isn't saved, only its shield).
			raiseShield(level);
			up = true;
		}
		// A volley of star shards: telegraphed, then loosed at its target.
		if (volleyAt > 0 && now >= volleyAt) {
			volleyAt = 0;
			if (target != null && target.isAlive()) {
				loose(level, target);
			}
		} else if (volleyAt == 0 && castAt == 0 && up && target != null && target.isAlive() && now >= nextVolley && hasLineOfSight(target)) {
			volleyAt = now + 20;
			nextVolley = now + 150 - phase * 20 + level.getRandom().nextInt(40);
			Vec3 c = getBoundingBox().getCenter();
			Vec3 toward = target.getEyePosition().subtract(c);
			Sigils.circle(level, c.add(toward.normalize().scale(1.4)), toward, ACCENT, COLOR, 1.2F, 24);
			Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.6F);
			updateName(null);
		}
		tickShards(level);
	}

	private void loose(ServerLevel level, LivingEntity target) {
		Vec3 c = getBoundingBox().getCenter();
		Vec3 aim = target.getEyePosition().subtract(c).normalize();
		for (int i = 0; i < phase; i++) {
			Shard shard = new Shard();
			double spread = (i - (phase - 1) / 2.0) * 0.35;
			shard.motion = aim.yRot((float) spread).scale(0.45);
			shard.pos = c.add(shard.motion.normalize().scale(1.4));
			shard.target = target;
			shards.add(shard);
		}
		Fx.sound(level, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 1.4F);
	}

	private void tickShards(ServerLevel level) {
		Iterator<Shard> it = shards.iterator();
		while (it.hasNext()) {
			Shard shard = it.next();
			shard.age++;
			if (shard.age > 160) {
				Vfx.emit(level, ParticleTypes.END_ROD, shard.pos, 6, 0.2, 0.05);
				it.remove();
				continue;
			}
			if (shard.parriedBy != null) {
				// Knocked back: it flies home, fast, and breaks on the shield (or strikes, with no shield to break).
				Vec3 home = getBoundingBox().getCenter();
				Vec3 to = home.subtract(shard.pos);
				if (to.length() < 1.3) {
					shardReturns(level, shard);
					it.remove();
					continue;
				}
				shard.motion = to.normalize().scale(0.8);
			} else if (shard.target != null && shard.target.isAlive()) {
				Vec3 to = shard.target.getBoundingBox().getCenter().subtract(shard.pos);
				shard.motion = shard.motion.add(to.normalize().scale(0.04)).normalize().scale(0.45);
			}
			Vec3 from = shard.pos;
			shard.pos = shard.pos.add(shard.motion);
			ElementFx.ray(level, from, shard.pos, ACCENT, 0.06, 3);
			Vfx.emit(level, new DustParticleOptions(COLOR, 1.0F), shard.pos, 1, 0.05, 0.0);
			if (shard.parriedBy != null) {
				continue;
			}
			BlockPos cell = BlockPos.containing(shard.pos);
			if (!level.getBlockState(cell).getCollisionShape(level, cell).isEmpty()) {
				Vfx.emit(level, ParticleTypes.END_ROD, shard.pos, 8, 0.2, 0.06);
				Fx.sound(level, shard.pos, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.6F);
				it.remove();
				continue;
			}
			if (parried(level, shard)) {
				continue;
			}
			LivingEntity hit = null;
			for (Entity e : level.getEntities(this, new AABB(shard.pos, shard.pos).inflate(0.6), e -> Targets.canHarm(this, e))) {
				hit = (LivingEntity) e;
				break;
			}
			if (hit != null) {
				if (hit instanceof Player player && Shields.strength(player) > 0) {
					// A Shield of its own turns the shard back where it came from.
					parry(level, shard, player);
					continue;
				}
				hit.hurtServer(level, level.damageSources().indirectMagic(this, this), (float) (6 * Runebound.power(level)));
				Vfx.radial(level, ParticleTypes.END_ROD, shard.pos, 12, 0.2);
				Fx.sound(level, shard.pos, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.2F);
				it.remove();
			}
		}
	}

	/** Whether someone knocks this shard back: a swing of a blade (or a fist) just as it arrives, or a bolt of their own. */
	private boolean parried(ServerLevel level, Shard shard) {
		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(shard.pos, shard.pos).inflate(3.0))) {
			if (!player.isAlive() || player.isSpectator() || !player.isSwinging() || player.getSwingAnimation(1.0F) > 0.6F) {
				continue;
			}
			Vec3 toShard = shard.pos.subtract(player.getEyePosition());
			if (toShard.length() > 3.2 || player.getLookAngle().dot(toShard.normalize()) < 0.3) {
				continue;
			}
			parry(level, shard, player);
			return true;
		}
		for (RuneBolt bolt : level.getEntitiesOfClass(RuneBolt.class, new AABB(shard.pos, shard.pos).inflate(1.3))) {
			if (bolt.getOwner() instanceof Player player) {
				bolt.discard();
				parry(level, shard, player);
				return true;
			}
		}
		return false;
	}

	/** A shard knocked back: it turns, with a bright ring, and flies home. */
	void parry(ServerLevel level, Shard shard, Player by) {
		shard.parriedBy = by;
		shard.age = 0;
		parries++;
		Sigils.flash(level, shard.pos, ACCENT, 1.4F);
		Vfx.emit(level, ParticleTypes.CRIT, shard.pos, 10, 0.2, 0.3);
		Fx.sound(level, shard.pos, DungeonSounds.SHARD_PARRY, 1.4F, 1.0F);
	}

	private void shardReturns(ServerLevel level, Shard shard) {
		Vec3 c = getBoundingBox().getCenter();
		float strength = Shields.strength(this);
		Vfx.radial(level, ParticleTypes.END_ROD, shard.pos, 18, 0.3);
		if (strength > 0) {
			float left = strength - SHARD_CRACK;
			if (left <= 0) {
				// Broken by its own shard: noticed next tick, like a spell breaking it.
				removeAttached(WildercordAttachments.SPELL_SHIELD);
				ElementFx.shards(level, c, 2.0, 12);
				Fx.sound(level, c, WildercordSounds.SHIELD_BREAK, 1.6F, 0.9F);
			} else {
				Shields.give(this, left, SHIELD_TICKS, SHIELD_RUNES);
				Sigils.flash(level, c, ACCENT, 2.0F);
				Fx.sound(level, c, WildercordSounds.SHIELD_BLOCK, 1.4F, 0.8F);
			}
		} else if (shard.parriedBy != null) {
			hurtServer(level, level.damageSources().indirectMagic(shard.parriedBy, shard.parriedBy), 10);
		}
	}

	/** For the tests: a shard thrown at {@code target}, and whether any of its shards is on its way back. */
	public void throwShard(LivingEntity target) {
		if (level() instanceof ServerLevel level) {
			loose(level, target);
		}
	}

	public boolean shardReturning() {
		return shards.stream().anyMatch(s -> s.parriedBy != null);
	}

	public int shardsInFlight() {
		return shards.size();
	}

	/** For the tests: the first shard in flight is knocked back by {@code player}, as a parry would. */
	public boolean parryFirst(Player player) {
		if (!(level() instanceof ServerLevel level) || shards.isEmpty()) {
			return false;
		}
		parry(level, shards.getFirst(), player);
		return true;
	}

	// ------------------------------------------------------------------ harm

	@Override
	protected float resist(ServerLevel level, DamageSource source, float damage) {
		if (!Dungeons.spellLanding() && shielded()) {
			// Arrows and blades glance off the circling shards.
			Vec3 c = getBoundingBox().getCenter();
			Vfx.emit(level, ParticleTypes.END_ROD, c, 6, 0.6, 0.05);
			Fx.sound(level, c, SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 1.4F);
			return 0;
		}
		return state(EXPOSED) ? damage * 1.25F : damage;
	}

	// ------------------------------------------------------------------ between phases: an eclipse

	@Override
	protected void onPhase(ServerLevel level, int phase) {
		Vec3 c = getBoundingBox().getCenter();
		Fx.sound(level, c, DungeonSounds.BOSS_PHASE, 2.0F, 1.0F);
		ElementFx.blackCore(level, c, 1.5, SHIFT_TICKS);
		announce(level, "message.wildercord.star_eater_phase." + phase, COLOR);
		for (int i = 0; i < 2; i++) {
			Mob guard = (i == 0 ? EntityTypes.STRAY : EntityTypes.SKELETON).create(level, EntitySpawnReason.MOB_SUMMONED);
			if (guard == null) {
				continue;
			}
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 spot = CastEngine.ground(level, Vec3.atBottomCenterOf(home).add(Math.cos(a) * 7, 2, Math.sin(a) * 7));
			guard.snapTo(spot.x, spot.y, spot.z, 0, 0);
			guard.finalizeSpawn(level, level.getCurrentDifficultyAt(guard.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
			Runebound.bind(guard, i == 0 ? List.of(Runes.SPARK, Runes.HARM) : List.of(Runes.BOLT, Runes.BLIND), phase == 3);
			level.addFreshEntity(guard);
			minion(guard);
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, spot.add(0, 1, 0), 24, 0.3);
			Sigils.ground(level, spot, COLOR, ACCENT, 1.2F, 30);
		}
	}

	@Override
	protected void shiftTick(ServerLevel level, int left) {
		Vec3 c = getBoundingBox().getCenter();
		// Everything nearby is drawn in toward its eye, then flung away as a new, stronger shield bursts open.
		if (left > 10) {
			for (Entity e : level.getEntities(this, getBoundingBox().inflate(12.0), e -> Targets.canHarm(this, e))) {
				Vec3 in = c.subtract(e.position());
				if (in.length() > 2.5) {
					Effects.push((LivingEntity) e, in.normalize().scale(0.05));
				}
			}
			if (left % 4 == 0) {
				ElementFx.implode(level, c, 5.0, 6);
			}
		}
		if (left == 5) {
			for (Entity e : level.getEntities(this, getBoundingBox().inflate(6.0), e -> Targets.canHarm(this, e))) {
				LivingEntity t = (LivingEntity) e;
				Vec3 out = t.position().subtract(c);
				Vec3 flat = new Vec3(out.x, 0, out.z).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(out.x, 0, out.z).normalize();
				t.hurtServer(level, level.damageSources().indirectMagic(this, this), (float) (5 * Runebound.power(level)));
				Effects.push(t, new Vec3(flat.x * 1.5, 0.4, flat.z * 1.5));
			}
			Vfx.radial(level, ParticleTypes.END_ROD, c, 60, 0.5);
			raiseShield(level);
		}
	}

	@Override
	protected Component status() {
		if (state(EXPOSED)) {
			return Component.translatable("boss.wildercord.star_eater_open").withColor(0xFFD060);
		}
		if (volleyAt > 0) {
			return Component.translatable("boss.wildercord.star_eater_volley").withColor(ACCENT);
		}
		float strength = Shields.strength(this);
		return strength > 0 ? Component.translatable("boss.wildercord.star_eater_shielded", Math.round(strength)).withColor(ACCENT) : null;
	}

	// ------------------------------------------------------------------ death

	@Override
	public void die(DamageSource source) {
		shards.clear();
		super.die(source);
	}

	@Override
	protected void dying(ServerLevel level, int tick) {
		Vec3 c = getBoundingBox().getCenter();
		setDeltaMovement(0, -0.01, 0);
		if (tick == 1) {
			Fx.sound(level, c, DungeonSounds.STAR_EATER_DEATH, 2.0F, 1.0F);
		}
		if (tick % 3 == 0) {
			ElementFx.implode(level, c, 3.0 * (1 - tick / (double) DEATH_TICKS) + 0.5, 6);
			Vfx.emit(level, ParticleTypes.REVERSE_PORTAL, c, 6, 0.4, 0.02);
		}
	}

	@Override
	protected void vanish(ServerLevel level) {
		Vec3 c = getBoundingBox().getCenter();
		ElementFx.starSeal(level, c, new Vec3(0, 1, 0), 4.0, 40);
		Vfx.radial(level, ParticleTypes.END_ROD, c, 80, 0.6);
		Sigils.flash(level, c, ACCENT, 5.0F);
	}

	// ------------------------------------------------------------------ sounds and such

	@Override
	protected SoundEvent getAmbientSound() {
		return DungeonSounds.STAR_EATER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return DungeonSounds.STAR_EATER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		return false;
	}

	@Override
	protected void returnHome(ServerLevel level) {
		super.returnHome(level);
		teleportTo(getX(), home.getY() + 5, getZ());
	}
}
