package dev.wildercord.monster;

import static dev.wildercord.monster.GiantRules.Matriarch.*;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Motes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Frost Matriarch's fight ({@link GiantRules.Matriarch}). She is slow to turn between moves, so getting round her is the
 * way in, and her tail is waiting there. Each move has its own tell and its own way out: the pounce on a ring that follows you
 * until it settles (be out of it, and she sprawls on the snow if she misses), the swipes she walks you down with (back off),
 * the tail flung round behind her (to her flank), the howl that chills the ground and calls her pups, the frost breathed across
 * her front in a sweep (get behind her, or go the way it came from), vanishing to circle you unseen and lunging out along a line
 * (step off it, or find her by her prints and strike her to drag her out), and icicles hung over everyone near (keep moving).
 */
final class MatriarchFight extends GiantFight {
	/** Her pups, called by her howl. */
	static final String PUP = "wildercord_giant_pup";
	private static final Vec3 UP = new Vec3(0, 1, 0), DOWN = new Vec3(0, -1, 0);
	/** How far she turns a tick between moves, and how high her pounce carries her. */
	private static final double TURN = 6, LEAP_HEIGHT = 3.5;
	/** How long a chill lasts on someone at most, past freezing. */
	private static final int CHILL_MOST = 80;
	private static final ElementFx.Palette TINT = ElementFx.FROST;

	/** Her marks on the snow: where something will land, the brighter edge of it, and rime. */
	private final DustParticleOptions warn, bright, rime;
	private final BlockParticleOption snow;
	/** Whether this is the move's second go roused, and whether her pounce found anyone. */
	private boolean twice, landed;
	/** Where her pounce lands and where it leapt from. */
	private Vec3 aim = Vec3.ZERO, from = Vec3.ZERO;
	/** Her facing when she chose (her tail swings behind that), and the side her claw or tail comes from (1 the right). */
	private Vec3 facing = new Vec3(0, 0, 1);
	private int side = 1;
	/** Swipes left in the flurry, and whether the breath sweeps back the other way. */
	private int swipesLeft;
	private boolean breathBack;
	/** Circling unseen: the angle round her prey, which way round, and how long. */
	private double circle;
	private int circleWay = 1, circleFor;
	/** The lunge: the way it runs and where from. */
	private Vec3 lungeWay = new Vec3(0, 0, 1), lungeFrom = Vec3.ZERO;
	/** Icicles hung over the ground, each falling at a tick. */
	private final List<Icicle> icicles = new ArrayList<>();
	/** Where her prey was last tick and how it has been moving (to lead a pounce or an icicle). */
	private @Nullable Vec3 seen;
	private Vec3 drift = Vec3.ZERO;

	private record Icicle(Vec3 at, int falls) {}

	MatriarchFight(WildMonster mob) {
		super(mob, MEMORY);
		this.warn = new DustParticleOptions(0x9AD8FF, 1.6F);
		this.bright = new DustParticleOptions(0xF0FAFF, 2.0F);
		this.rime = new DustParticleOptions(0xCFEFFF, 1.0F);
		this.snow = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState());
	}

	// ------------------------------------------------------------------ the fight

	@Override
	protected int choose(ServerLevel level, LivingEntity target) {
		facing = dir;
		return GiantRules.Matriarch.choose(deck, across(target), high(target), behind(dot(target.position())), crowded(level),
			pups(level), roll());
	}

	@Override
	protected void begin(ServerLevel level, int move, LivingEntity target) {
		twice = false;
		switch (move) {
			case POUNCE -> pounce(level, POUNCE_TELL);
			case SWIPES -> {
				swipesLeft = roused ? ROUSED_SWIPES : SWIPE_COUNT;
				swipe(level, SWIPE_TELL + 4);
				MonsterMagic.sound(level, feet(), "monster_gloom_growl", 2.0F, 0.55F);
			}
			case TAIL -> {
				dir = facing;
				tail(level, TAIL_TELL);
			}
			case HOWL -> {
				pose(HOWL, HOWL_TELL, HOWL_TELL + 16);
				sound(level, feet(), SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.5F);
			}
			case BREATH -> breath(level, BREATH_TELL, roll() < 0.5);
			case VANISH -> {
				circleFor = CIRCLE_MIN + mob.getRandom().nextInt(CIRCLE_MAX - CIRCLE_MIN + 1);
				circleWay = roll() < 0.5 ? 1 : -1;
				Vec3 off = feet().subtract(target.position());
				circle = Math.atan2(off.z, off.x);
				aim = target.position();
				pose(VANISH, VANISH_TELL, VANISH_TELL + circleFor);
				MonsterMagic.sound(level, feet(), "monster_gloom_growl", 1.6F, 0.8F);
				sound(level, feet(), SoundEvents.POWDER_SNOW_BREAK, 2.0F, 0.6F);
			}
			case ICICLES -> {
				pose(ICICLES, ICICLES_TELL, ICICLES_TELL + (ICICLE_WAVES - 1) * ICICLE_EVERY + 6);
				sound(level, feet(), SoundEvents.FOX_SCREECH, 2.0F, 0.45F);
				sound(level, feet(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
			}
			default -> settle();
		}
	}

	@Override
	protected void act(ServerLevel level, int move, int t, @Nullable LivingEntity target) {
		switch (move) {
			case POUNCE -> pouncing(level, t, target);
			case SPRAWL -> sprawled(level, t);
			case SWIPES -> swiping(level, t, target);
			case TAIL -> tailing(level, t);
			case HOWL -> howling(level, t, target);
			case BREATH -> breathing(level, t);
			case VANISH -> vanishing(level, t, target);
			case LUNGE -> lunging(level, t, target);
			case ICICLES -> calling(level, t, target);
			case ROAR -> roaring(level, t);
			default -> hold();
		}
	}

	@Override
	protected void after(ServerLevel level, int move, @Nullable LivingEntity target) {
		boolean again = roused && !twice && target != null;
		switch (move) {
			case POUNCE -> {
				if (again) {
					double d = across(target);
					if (d >= 2 && d <= POUNCE_FAR) {
						twice = true;
						pounce(level, POUNCE_AGAIN_TELL);
					}
				}
			}
			case SWIPES -> {
				if (--swipesLeft > 0) {
					swipe(level, roused ? SWIPE_TELL - 2 : SWIPE_TELL);
				}
			}
			case TAIL -> {
				if (again && behindHer(level)) {
					twice = true;
					tail(level, 6);
				}
			}
			case BREATH -> {
				if (again && across(target) <= BREATH_RADIUS + 2) {
					twice = true;
					breath(level, 6, !breathBack);
				}
			}
			case VANISH -> lunge(level, target, LUNGE_TELL);
			case LUNGE -> {
				if (again && across(target) >= 3 && across(target) <= VANISH_FAR) {
					twice = true;
					lunge(level, target, LUNGE_TELL / 2 + 2);
				}
			}
			default -> {}
		}
	}

	@Override
	protected float hurt(float damage, int move) {
		return GiantRules.Matriarch.hurt(damage, move);
	}

	@Override
	protected void roar(ServerLevel level) {
		mob.giantState(WildMonster.VEILED, false);
		pose(ROAR, 0, ROAR_TICKS);
		MonsterMagic.sound(level, feet(), "monster_gloom_growl", 3.0F, 0.4F);
	}

	@Override
	protected int restAfter() {
		return roused ? (roll() < CHAIN ? 4 : ROUSED_REST) : REST;
	}

	@Override
	protected void hit(ServerLevel level, DamageSource source, float lost) {
		// Struck while she circles unseen, she's dragged up out of the snow and staggers.
		if (move == VANISH && since >= tell) {
			mob.giantState(WildMonster.VEILED, false);
			Vec3 feet = feet();
			level.sendParticles(snow, feet.x, feet.y + 0.6, feet.z, 50, hw(), 0.5, hw(), 0.2);
			Motes.clouds(level, feet.add(0, 0.8, 0), 10, hw(), 0xF4FBFF, 1.6, 30, new Vec3(0, 0.03, 0), 0.04, 0.6);
			MonsterMagic.sound(level, feet, "monster_gloom_hurt", 2.0F, 0.5F);
			sound(level, feet, SoundEvents.RAVAGER_STUNNED, 1.6F, 0.8F);
			pose(SPRAWL, 0, TORN_TICKS);
		}
	}

	@Override
	protected void release(ServerLevel level) {
		mob.giantState(WildMonster.VEILED, false);
	}

	@Override
	protected void ambient(ServerLevel level) {
		LivingEntity target = mob.getTarget();
		if (target != null) {
			Vec3 now = target.position();
			drift = seen != null && now.distanceToSqr(seen) < 9 ? drift.scale(0.7).add(now.subtract(seen).scale(0.3)) : Vec3.ZERO;
			seen = now;
			// Between moves she turns slowly where she stands; walking, she faces the way she goes.
			if (move == 0) {
				if (mob.getNavigation().isInProgress()) {
					dir = ahead(mob.getYRot());
				} else {
					turnToward(now.subtract(feet()), TURN);
					face(dir);
				}
			}
		}
		if (mob.tickCount % 4 == 0 && move != VANISH) {
			Vec3 mouth = mouth();
			Motes.clouds(level, mouth, 1, 0.1, 0xF4FBFF, 0.5, 20, dir.scale(0.03).add(0, 0.01, 0), 0.01, 0.35);
		}
		tickIcicles(level);
	}

	// ------------------------------------------------------------------ the pounce

	private void pounce(ServerLevel level, int tell) {
		landed = false;
		pose(POUNCE, tell, tell + POUNCE_LEAP + 6);
		MonsterMagic.sound(level, feet(), "monster_gloom_growl", 2.0F, 0.6F);
		sound(level, feet(), SoundEvents.POWDER_SNOW_STEP, 1.6F, 0.6F);
	}

	/** Crouched with her haunches up, a ring on the snow follows her prey until it settles; then she leaps onto it. */
	private void pouncing(ServerLevel level, int t, @Nullable LivingEntity target) {
		double r = POUNCE_RADIUS + hw() * 0.5;
		if (t < tell) {
			hold();
			if (target != null && t < tell - POUNCE_LOCK) {
				aim = within(twice ? lead(target, tell - t + POUNCE_LEAP / 2) : target.position(), POUNCE_FAR + hw());
				dir = flat(aim.subtract(feet()), dir);
			}
			if (t == tell - POUNCE_LOCK) {
				sound(level, aim, SoundEvents.POWDER_SNOW_BREAK, 1.6F, 0.7F);
				sound(level, feet(), SoundEvents.POLAR_BEAR_WARNING, 1.8F, 0.6F);
			}
			if (t % 2 == 0) {
				boolean locked = t >= tell - POUNCE_LOCK;
				ring(level, aim, r, locked ? bright : warn, 20);
				ring(level, aim, r * 0.5, rime, 8);
				Vec3 way = flat(aim.subtract(feet()), dir);
				line(level, feet(), way, hw() + 0.5, Math.max(hw() + 0.5, feet().distanceTo(aim) - r), rime, 1.2);
			}
			Vec3 feet = feet();
			level.sendParticles(snow, feet.x, feet.y + 0.2, feet.z, 3, hw(), 0.1, hw(), 0.05);
			return;
		}
		int k = t - tell;
		if (k == 0) {
			from = feet();
			MonsterMagic.sound(level, from, "monster_gloom_pounce", 2.2F, 0.6F);
			sound(level, from, SoundEvents.POWDER_SNOW_BREAK, 2.0F, 0.6F);
		}
		if (k < POUNCE_LEAP) {
			double f = (k + 1) / (double) POUNCE_LEAP;
			Vec3 to = from.add(aim.subtract(from).scale(f)).add(0, LEAP_HEIGHT * 4 * f * (1 - f), 0);
			mob.move(MoverType.SELF, to.subtract(feet()));
			mob.setDeltaMovement(Vec3.ZERO);
			mob.resetFallDistance();
			Vec3 p = feet().add(0, mob.getBbHeight() * 0.5, 0);
			level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 6, hw() * 0.6, 0.3, hw() * 0.6, 0.02);
			if (k == POUNCE_LEAP - 1) {
				land(level, r);
			}
			return;
		}
		hold();
	}

	private void land(ServerLevel level, double r) {
		Vec3 feet = feet();
		sound(level, feet, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.8F);
		sound(level, feet, SoundEvents.POWDER_SNOW_BREAK, 2.2F, 0.5F);
		sound(level, feet, SoundEvents.GLASS_BREAK, 1.4F, 0.6F);
		ElementFx.frostImpact(level, feet.add(0, 0.3, 0), 2.0);
		ElementFx.shatterRing(level, feet, r);
		ElementFx.groundRing(level, feet, TINT.secondary(), 0.5, r + 0.5, 0.6, 12);
		level.sendParticles(snow, feet.x, feet.y + 0.3, feet.z, 60, r * 0.5, 0.3, r * 0.5, 0.25);
		for (LivingEntity e : preyAt(level, feet, r, 2.5)) {
			if (first(e) && strike(level, e, POUNCE_HIT, feet, 0.9, 0.45)) {
				chill(e, 60);
				landed = true;
			}
		}
		// A pounce on empty snow leaves her sprawled; roused, only the second one does.
		if (!landed && (!roused || twice)) {
			sprawl(level, SPRAWL_TICKS);
		}
	}

	private void sprawl(ServerLevel level, int ticks) {
		pose(SPRAWL, 0, ticks);
		sound(level, feet(), SoundEvents.RAVAGER_STUNNED, 1.6F, 0.8F);
		MonsterMagic.sound(level, feet(), "monster_gloom_hurt", 1.8F, 0.6F);
	}

	/** Sprawled on the snow, open to blows. */
	private void sprawled(ServerLevel level, int t) {
		hold();
		Vec3 feet = feet();
		if (t % 4 == 0) {
			level.sendParticles(snow, feet.x, feet.y + 0.3, feet.z, 4, hw(), 0.1, hw(), 0.02);
		}
		if (t % 12 == 6) {
			sound(level, feet, SoundEvents.POWDER_SNOW_STEP, 1.0F, 0.5F);
		}
		if (t % 3 == 0) {
			double a = t * 0.5;
			Vec3 head = head();
			Motes.glow(level, head.add(Math.cos(a) * 0.6, 0.5, Math.sin(a) * 0.6), TINT.secondary(), 0.25, 12, Vec3.ZERO, 0.01);
		}
	}

	// ------------------------------------------------------------------ the swipes

	private void swipe(ServerLevel level, int tell) {
		side = -side;
		mob.giantState(WildMonster.ALT, side < 0);
		pose(SWIPES, tell, tell + 5);
	}

	/** A paw raised to one side and the arc it will rake marked; she steps in as it comes across. */
	private void swiping(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (t < tell) {
			hold();
			if (target != null && t < tell - 3) {
				turnToward(target.position().subtract(feet()), 20);
			}
			if (t % 2 == 0) {
				arc(level, feet(), dir, SWIPE_RADIUS + hw(), SWIPE_ARC, warn, 14);
				Vec3 start = feet().add(turned(side * SWIPE_ARC / 2).scale(SWIPE_RADIUS + hw()));
				level.sendParticles(bright, start.x, start.y + 0.15, start.z, 3, 0.1, 0, 0.1, 0);
			}
			Vec3 paw = paw(side);
			Motes.glows(level, paw, 2, 0.2, TINT.primary(), 0.3, 10, new Vec3(0, 0.02, 0), 0.02);
		}
		if (t >= tell - 2 && t <= tell + 1) {
			step(dir.scale(SWIPE_STEP / 4));
		}
		if (t != tell) {
			return;
		}
		Vec3 feet = feet();
		sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.6F);
		sound(level, feet, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.6F + mob.getRandom().nextFloat() * 0.3F);
		ElementFx.slash(level, feet.add(0, mob.getBbHeight() * 0.5, 0), side > 0 ? UP : DOWN, dir, TINT.secondary(),
			SWIPE_RADIUS + hw(), Math.toRadians(SWIPE_ARC), 1.0, 4, 8);
		for (int i = 0; i <= 8; i++) {
			Vec3 p = feet.add(turned(-SWIPE_ARC / 2 + SWIPE_ARC * i / 8).scale(SWIPE_RADIUS * 0.7 + hw()));
			level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.8, p.z, 3, 0.2, 0.3, 0.2, 0.03);
		}
		for (LivingEntity e : prey(level, SWIPE_RADIUS)) {
			if (!high(e) && GiantRules.inArc(across(e), angleTo(e.position()), SWIPE_RADIUS, SWIPE_ARC) && first(e)
					&& strike(level, e, SWIPE_HIT, feet, 0.4, 0.2)) {
				MonsterMagic.shove(e, right().scale(-side * 0.5));
				chill(e, 25);
			}
		}
	}

	// ------------------------------------------------------------------ the tail

	private void tail(ServerLevel level, int tell) {
		side = -side;
		mob.giantState(WildMonster.ALT, side < 0);
		pose(TAIL, tell, tell + 8);
		MonsterMagic.sound(level, feet(), "monster_gloom_growl", 1.6F, 0.75F);
		sound(level, feet(), SoundEvents.SNOW_STEP, 1.6F, 0.6F);
	}

	/** Her tail lashing behind her and the arc it will sweep marked there; then it comes round low. */
	private void tailing(ServerLevel level, int t) {
		hold();
		Vec3 feet = feet(), back = dir.reverse();
		if (t < tell) {
			if (t % 2 == 0) {
				arc(level, feet, back, TAIL_RADIUS + hw(), TAIL_ARC, warn, 14);
				arc(level, feet, back, (TAIL_RADIUS + hw()) * 0.6, TAIL_ARC, rime, 8);
				Vec3 start = feet.add(turned(side * (180 - TAIL_ARC / 2)).scale(TAIL_RADIUS + hw()));
				level.sendParticles(bright, start.x, start.y + 0.15, start.z, 3, 0.1, 0, 0.1, 0);
			}
			double lash = Math.sin(t * 0.9) * 40;
			Vec3 tip = feet.add(turned(180 + lash).scale(hw() + 1.6)).add(0, 0.6, 0);
			level.sendParticles(ParticleTypes.SNOWFLAKE, tip.x, tip.y, tip.z, 2, 0.15, 0.15, 0.15, 0.01);
			if (t == tell - 4) {
				sound(level, feet, SoundEvents.PHANTOM_FLAP, 1.6F, 0.5F);
			}
			return;
		}
		if (t != tell) {
			return;
		}
		sound(level, feet, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.45F);
		sound(level, feet, SoundEvents.POWDER_SNOW_BREAK, 2.0F, 0.6F);
		ElementFx.slash(level, feet.add(0, 0.5, 0), side > 0 ? DOWN : UP, back, TINT.secondary(),
			TAIL_RADIUS + hw(), Math.toRadians(TAIL_ARC), 1.0, 4, 8);
		Vec3 mid = feet.add(back.scale(hw() + TAIL_RADIUS * 0.5));
		level.sendParticles(snow, mid.x, mid.y + 0.3, mid.z, 30, TAIL_RADIUS * 0.4, 0.2, TAIL_RADIUS * 0.4, 0.15);
		for (LivingEntity e : prey(level, TAIL_RADIUS)) {
			if (!high(e) && across(e) <= TAIL_RADIUS && GiantRules.Matriarch.behind(dot(e.position())) && first(e)
					&& e.hurtServer(level, mob.damageSources().mobAttack(mob), TAIL_HIT)) {
				Vec3 away = flat(e.position().subtract(feet), back).scale(0.3);
				Vec3 fling = right().scale(-side * TAIL_THROW).add(away);
				MonsterMagic.shove(e, new Vec3(fling.x, 0.45, fling.z));
			}
		}
	}

	// ------------------------------------------------------------------ the howl

	/** Head thrown back and the ground about her marked as it chills; then the howl, and her pups come out of the snow. */
	private void howling(ServerLevel level, int t, @Nullable LivingEntity target) {
		hold();
		Vec3 feet = feet();
		double r = HOWL_RADIUS + hw();
		if (t < tell) {
			if (t % 2 == 0) {
				ring(level, feet, r, warn, 28);
				ring(level, feet, r * (t + 1) / (double) tell, rime, 16);
			}
			Motes.glows(level, head().add(0, 0.4, 0), 2, 0.3, TINT.secondary(), 0.3, 16, new Vec3(0, 0.06, 0), 0.02);
			if (t == 6 || t == 14) {
				MonsterMagic.sound(level, feet, "monster_gloom_ambient", 2.0F, 0.5F);
			}
			if (t == tell - 6) {
				sound(level, feet, SoundEvents.FOX_SCREECH, 2.4F, 0.4F);
			}
			return;
		}
		if (t != tell) {
			return;
		}
		sound(level, feet, SoundEvents.POWDER_SNOW_BREAK, 2.2F, 0.5F);
		sound(level, feet, SoundEvents.POLAR_BEAR_WARNING, 2.4F, 0.4F);
		ElementFx.gustRing(level, feet, r);
		ElementFx.groundRing(level, feet, TINT.primary(), 1, r, 0.8, 16);
		ElementFx.frostCreep(level, feet, r, 60);
		for (LivingEntity e : prey(level, HOWL_RADIUS)) {
			if (!e.entityTags().contains(PUP) && across(e) <= HOWL_RADIUS) {
				chill(e, 70);
			}
		}
		int count = Math.min(roused ? ROUSED_PUPS : PUPS, MAX_PUPS - pups(level));
		for (int i = 0; i < count; i++) {
			double way = (i % 2 == 0 ? 1 : -1) * (70 + i * 10);
			Vec3 at = floor(feet.add(turned(way).scale(hw() + 2)));
			Gloomstalker pup = MonsterContent.GLOOMSTALKER.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (pup == null) {
				continue;
			}
			pup.snapTo(at.x, at.y, at.z, mob.getYRot(), 0);
			pup.setVariant(MonsterVariantRules.Variant.FROST, false);
			pup.addTag(PUP);
			if (target != null) {
				pup.setTarget(target);
			}
			level.addFreshEntity(pup);
			level.sendParticles(snow, at.x, at.y + 0.4, at.z, 30, 0.5, 0.4, 0.5, 0.15);
			Motes.clouds(level, at.add(0, 0.5, 0), 6, 0.6, 0xF4FBFF, 1.2, 24, new Vec3(0, 0.03, 0), 0.03, 0.5);
		}
	}

	// ------------------------------------------------------------------ the breath

	private void breath(ServerLevel level, int tell, boolean back) {
		breathBack = back;
		mob.giantState(WildMonster.ALT, back);
		pose(BREATH, tell, tell + BREATH_TICKS + 8);
		sound(level, feet(), SoundEvents.BREEZE_INHALE, 2.0F, 0.5F);
	}

	/** Chest swelling as the cold is drawn into her mouth, the cone and the edge it starts from marked; then frost breathed across it. */
	private void breathing(ServerLevel level, int t) {
		hold();
		Vec3 feet = feet(), mouth = mouth();
		if (t < tell) {
			if (t % 2 == 0) {
				arc(level, feet, dir, BREATH_RADIUS + hw(), 2 * BREATH_SWEEP + BREATH_WIDTH, warn, 18);
				Vec3 edge = turned(GiantRules.Matriarch.breathAngle(0, breathBack));
				line(level, feet, edge, hw() + 0.5, BREATH_RADIUS + hw(), bright, 1.0);
			}
			Vec3 off = new Vec3(roll() - 0.5, roll() * 0.6, roll() - 0.5).scale(5);
			Motes.seek(level, mouth.add(off), mouth, TINT.secondary(), 0.3, 14, 0.4);
			if (t == tell - 4) {
				sound(level, feet, SoundEvents.POLAR_BEAR_WARNING, 1.8F, 0.6F);
			}
			return;
		}
		int k = t - tell;
		if (k < BREATH_TICKS) {
			double angle = GiantRules.Matriarch.breathAngle(k, breathBack);
			Vec3 way = turned(angle);
			for (double d = 1; d <= BREATH_RADIUS + hw(); d += 1.2) {
				double f = d / (BREATH_RADIUS + hw());
				Vec3 p = mouth.add(way.scale(d)).add(0, -mouth.subtract(feet).y * f, 0);
				level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y + 0.2, p.z, 2, 0.25 * f + 0.1, 0.15, 0.25 * f + 0.1, 0.02);
				if (k % 2 == 0) {
					Motes.clouds(level, p, 1, 0.3 * f, 0xE6FAFF, 0.6 + f, 14, way.scale(0.05), 0.02, 0.4);
				}
			}
			if (k % 4 == 0) {
				sound(level, mouth, k % 8 == 0 ? SoundEvents.SNOW_GOLEM_SHOOT : SoundEvents.POWDER_SNOW_STEP, 1.6F, 0.5F);
			}
			if (k % BREATH_EVERY == 0) {
				for (LivingEntity e : prey(level, BREATH_RADIUS)) {
					if (!high(e) && GiantRules.inArc(across(e), angleTo(e.position()) - angle, BREATH_RADIUS, BREATH_WIDTH)
							&& e.hurtServer(level, mob.damageSources().freeze(), BREATH_HIT)) {
						chill(e, 30);
					}
				}
			}
			return;
		}
		if (k == BREATH_TICKS) {
			Motes.clouds(level, mouth, 6, 0.4, 0xF4FBFF, 1.0, 24, dir.scale(0.04), 0.03, 0.5);
			sound(level, mouth, SoundEvents.PLAYER_HURT_FREEZE, 1.4F, 0.5F);
		}
	}

	// ------------------------------------------------------------------ vanishing and the lunge

	/** She burrows into the snow in a cloud and circles her prey unseen, only her prints and the crunch of them giving her away. */
	private void vanishing(ServerLevel level, int t, @Nullable LivingEntity target) {
		Vec3 feet = feet();
		if (t < tell) {
			hold();
			level.sendParticles(snow, feet.x, feet.y + 0.3, feet.z, 6, hw(), 0.2, hw(), 0.08);
			if (t == tell - 1) {
				mob.giantState(WildMonster.VEILED, true);
				Motes.clouds(level, feet.add(0, 1, 0), 16, hw() + 0.5, 0xF4FBFF, 2.0, 40, new Vec3(0, 0.02, 0), 0.04, 0.7);
				level.sendParticles(snow, feet.x, feet.y + 0.5, feet.z, 60, hw(), 0.6, hw(), 0.2);
				sound(level, feet, SoundEvents.POWDER_SNOW_BREAK, 2.2F, 0.5F);
			}
			return;
		}
		Vec3 centre = target != null ? target.position() : aim;
		aim = centre;
		circle += circleWay * 0.5 / CIRCLE_RADIUS;
		Vec3 spot = centre.add(Math.cos(circle) * (CIRCLE_RADIUS + hw()), 0, Math.sin(circle) * (CIRCLE_RADIUS + hw()));
		Vec3 go = flat(spot.subtract(feet), dir).scale(Math.min(0.7, spot.subtract(feet).horizontalDistance()));
		step(go);
		dir = flat(centre.subtract(feet()), dir);
		if (t % 3 == 0) {
			Vec3 now = feet();
			level.sendParticles(snow, now.x, now.y + 0.1, now.z, 4, hw() * 0.5, 0, hw() * 0.5, 0.02);
			level.sendParticles(rime, now.x, now.y + 0.1, now.z, 2, hw() * 0.4, 0, hw() * 0.4, 0);
			sound(level, now, SoundEvents.SNOW_STEP, 0.6F, 0.6F);
		}
		if (t % 10 == 5) {
			MonsterMagic.sound(level, feet(), "monster_gloom_ambient", 0.7F, 0.6F);
		}
	}

	private void lunge(ServerLevel level, @Nullable LivingEntity target, int tell) {
		mob.giantState(WildMonster.VEILED, false);
		if (target != null) {
			lungeWay = flat(target.position().subtract(feet()), dir);
			dir = lungeWay;
		}
		pose(LUNGE, tell, tell + LUNGE_TICKS + 8);
		Vec3 feet = feet();
		level.sendParticles(snow, feet.x, feet.y + 0.5, feet.z, 50, hw(), 0.5, hw(), 0.2);
		Motes.clouds(level, feet.add(0, 0.8, 0), 10, hw(), 0xF4FBFF, 1.6, 30, new Vec3(0, 0.03, 0), 0.04, 0.6);
		MonsterMagic.sound(level, feet, "monster_gloom_reveal", 2.2F, 0.6F);
		sound(level, feet, SoundEvents.POWDER_SNOW_BREAK, 2.0F, 0.6F);
	}

	/** Risen out of the snow low and coiled, the line she'll run marked; then she runs it, and a wall in the way stops her hard. */
	private void lunging(ServerLevel level, int t, @Nullable LivingEntity target) {
		Vec3 feet = feet();
		double length = LUNGE_SPEED * LUNGE_TICKS + hw();
		if (t < tell) {
			hold();
			if (target != null && t < tell - 4) {
				lungeWay = flat(target.position().subtract(feet), lungeWay);
				dir = lungeWay;
			}
			if (t == tell - 4) {
				sound(level, feet, SoundEvents.POLAR_BEAR_WARNING, 2.0F, 0.7F);
			}
			if (t % 2 == 0) {
				line(level, feet, lungeWay, hw(), length, t >= tell - 4 ? bright : warn, 1.0);
				Vec3 side = new Vec3(-lungeWay.z, 0, lungeWay.x).scale(LUNGE_CATCH + hw() * 0.5);
				line(level, feet.add(side), lungeWay, hw(), length, rime, 1.6);
				line(level, feet.subtract(side), lungeWay, hw(), length, rime, 1.6);
			}
			return;
		}
		int k = t - tell;
		if (k == 0) {
			lungeFrom = feet;
			MonsterMagic.sound(level, feet, "monster_gloom_pounce", 2.2F, 0.5F);
		}
		if (k < LUNGE_TICKS) {
			dir = lungeWay;
			step(lungeWay.scale(LUNGE_SPEED));
			Vec3 now = feet();
			level.sendParticles(snow, now.x, now.y + 0.3, now.z, 8, hw(), 0.2, hw(), 0.15);
			for (LivingEntity e : preyAt(level, now, LUNGE_CATCH + hw(), 2)) {
				if (first(e) && strike(level, e, LUNGE_HIT, now.subtract(lungeWay.scale(2)), 1.0, 0.45)) {
					chill(e, 40);
				}
			}
			if (mob.horizontalCollision && now.distanceTo(lungeFrom) > 1) {
				sound(level, now, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.0F);
				sound(level, now, SoundEvents.POWDER_SNOW_BREAK, 2.2F, 0.5F);
				level.sendParticles(snow, now.x, now.y + 1, now.z, 50, hw(), 0.6, hw(), 0.25);
				ElementFx.frostImpact(level, now.add(lungeWay.scale(hw())).add(0, 1, 0), 1.5);
				sprawl(level, TORN_TICKS);
			}
			return;
		}
		hold();
		Vec3 now = feet();
		if (k < LUNGE_TICKS + 4) {
			level.sendParticles(snow, now.x, now.y + 0.2, now.z, 10, hw(), 0.1, hw(), 0.2);
			if (k == LUNGE_TICKS) {
				sound(level, now, SoundEvents.SNOW_BREAK, 1.8F, 0.6F);
			}
		}
	}

	// ------------------------------------------------------------------ the icicles

	/** Head raised and the cold rising off her, she hangs icicles over everyone near, wave after wave. */
	private void calling(ServerLevel level, int t, @Nullable LivingEntity target) {
		hold();
		Motes.glows(level, head().add(0, 0.6, 0), 2, 0.4, TINT.primary(), 0.3, 18, new Vec3(0, 0.1, 0), 0.03);
		if (t < tell || (t - tell) % ICICLE_EVERY != 0) {
			return;
		}
		int wave = (t - tell) / ICICLE_EVERY;
		if (wave >= ICICLE_WAVES) {
			return;
		}
		sound(level, feet(), SoundEvents.AMETHYST_CLUSTER_BREAK, 1.8F, 0.6F + wave * 0.1F);
		double reach = ICICLE_REACH + hw();
		for (Player p : players(level, reach, ICICLE_MAX)) {
			boolean leadIt = p == target && (roused || wave % 2 == 1);
			hang(level, within(leadIt ? lead(p, ICICLE_FALL) : p.position(), reach));
		}
		if (wave % 2 == 0 && target != null) {
			double a = roll() * Math.PI * 2, d = 2 + roll() * 3;
			hang(level, within(target.position().add(Math.cos(a) * d, 0, Math.sin(a) * d), reach));
		}
	}

	private void hang(ServerLevel level, Vec3 at) {
		icicles.add(new Icicle(at, mob.tickCount + ICICLE_FALL));
		sound(level, at.add(0, 6, 0), SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.8F + mob.getRandom().nextFloat() * 0.4F);
	}

	private void tickIcicles(ServerLevel level) {
		for (Iterator<Icicle> it = icicles.iterator(); it.hasNext();) {
			Icicle icicle = it.next();
			Vec3 at = icicle.at();
			int due = icicle.falls() - mob.tickCount;
			if (due > 0) {
				if (due % 2 == 0) {
					ring(level, at, ICICLE_RADIUS, due <= 5 ? bright : warn, 12);
				}
				if (due % 2 == 1) {
					// Hanging high, then dropping fast in its last ticks.
					double f = due / (double) ICICLE_FALL;
					double y = 1 + 7 * f * f;
					for (int i = 0; i < 4; i++) {
						Motes.glow(level, at.add(0, y + i * 0.35, 0), i == 0 ? TINT.secondary() : TINT.primary(), 0.35 - i * 0.05, 3, Vec3.ZERO, 0);
					}
				}
				continue;
			}
			it.remove();
			ElementFx.shards(level, at.add(0, 0.3, 0), ICICLE_RADIUS, 10);
			ElementFx.frostImpact(level, at.add(0, 0.3, 0), 1.0);
			sound(level, at, SoundEvents.GLASS_BREAK, 1.6F, 0.7F + mob.getRandom().nextFloat() * 0.3F);
			for (LivingEntity e : preyAt(level, at, ICICLE_RADIUS, 2)) {
				if (e.hurtServer(level, mob.damageSources().mobAttack(mob), ICICLE_HIT)) {
					chill(e, 30);
				}
			}
		}
	}

	// ------------------------------------------------------------------ the roar

	/** Half beaten she rears and roars, the cold bursting out round her and throwing everyone near back. */
	private void roaring(ServerLevel level, int t) {
		hold();
		Vec3 feet = feet();
		if (t == 0) {
			sound(level, feet, SoundEvents.RAVAGER_ROAR, 2.4F, 0.6F);
			sound(level, feet, SoundEvents.POLAR_BEAR_WARNING, 2.4F, 0.4F);
		}
		if (t < 10) {
			if (t % 2 == 0) {
				ring(level, feet, ROAR_RADIUS + hw(), warn, 28);
			}
			Motes.glows(level, head(), 3, 0.5, TINT.secondary(), 0.4, 16, new Vec3(0, 0.08, 0), 0.04);
			return;
		}
		if (t == 10) {
			sound(level, feet, SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.7F);
			sound(level, feet, SoundEvents.GLASS_BREAK, 2.0F, 0.5F);
			ElementFx.shatterRing(level, feet, ROAR_RADIUS + hw());
			ElementFx.frostCreep(level, feet, ROAR_RADIUS + hw(), 80);
			ElementFx.gustRing(level, feet, ROAR_RADIUS + hw());
			ElementFx.frostImpact(level, feet.add(0, 1, 0), 3);
			level.sendParticles(snow, feet.x, feet.y + 0.5, feet.z, 80, hw() + 1, 0.5, hw() + 1, 0.3);
			for (LivingEntity e : prey(level, ROAR_RADIUS)) {
				if (across(e) <= ROAR_RADIUS) {
					MonsterMagic.knock(e, feet, 1.4);
					chill(e, 60);
				}
			}
			return;
		}
		if (t % 3 == 0) {
			Motes.clouds(level, mouth(), 2, 0.2, 0xF4FBFF, 0.8, 20, dir.scale(0.04).add(0, 0.02, 0), 0.02, 0.4);
		}
	}

	// ------------------------------------------------------------------ her body

	/** Whether her prey is still behind her, near enough for the tail. */
	private boolean behindHer(ServerLevel level) {
		LivingEntity target = mob.getTarget();
		return target != null && target.isAlive() && across(target) <= TAIL_REACH
			&& GiantRules.Matriarch.behind(dot(target.position()));
	}

	/** Her pups still about. */
	private int pups(ServerLevel level) {
		return level.getEntitiesOfClass(Gloomstalker.class, mob.getBoundingBox().inflate(PUP_RANGE),
			g -> g.isAlive() && g.entityTags().contains(PUP)).size();
	}

	/** Cold that frosts someone over and slows them. */
	private void chill(LivingEntity e, int ticks) {
		e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + CHILL_MOST, Math.max(e.getTicksFrozen(), 0) + ticks));
		e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.max(30, ticks), ticks >= 60 ? 1 : 0, false, true), mob);
	}

	/** A step of her own across the ground, held down on it. */
	private void step(Vec3 v) {
		mob.move(MoverType.SELF, new Vec3(v.x, -0.3, v.z));
		mob.setDeltaMovement(Vec3.ZERO);
	}

	private double hw() {
		return mob.getBbWidth() / 2;
	}

	private Vec3 head() {
		return feet().add(dir.scale(hw() + 0.2)).add(0, mob.getBbHeight() * 0.85, 0);
	}

	private Vec3 mouth() {
		return feet().add(dir.scale(hw() + 0.5)).add(0, mob.getBbHeight() * 0.65, 0);
	}

	/** A forepaw, on her right side or her left. */
	private Vec3 paw(int side) {
		return feet().add(dir.scale(hw() + 0.4)).add(right().scale(side * hw() * 0.7)).add(0, mob.getBbHeight() * 0.6, 0);
	}

	/** Where her prey will be in so many ticks, going the way it has been. */
	private Vec3 lead(LivingEntity target, int ticks) {
		return target == mob.getTarget() ? target.position().add(drift.scale(ticks)) : target.position();
	}
}
