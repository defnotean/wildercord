package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * What each effect rune does to a hit. Numbers match the rune descriptions in {@link Runes}
 * and docs/DESIGN.md; Amplify multiplies power by 1.5 and Extend doubles durations.
 */
public final class Effects {
	private Effects() {}

	/** Damage to players from other players' spells is scaled down so PvP stays fair. */
	public static final float PVP_DAMAGE = 0.6F;
	private static final int MAX_STRIKES_PER_HIT = 8;

	public static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit) {
		apply(cast, node, hit, 1.0);
	}

	/** Execute on the effect being applied: extra power against targets under half health (1 = none). */
	private static double executeBonus = 1.0;
	/** The element of the effect being applied, for Unison. */
	private static String currentElement = "";

	/** @param groupPower extra power from the shape (Focus on a shape) */
	public static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower) {
		double outerBonus = executeBonus;
		String outerElement = currentElement;
		executeBonus = SpellNumbers.executeBonus(node);
		currentElement = node.effect.element();
		try {
			applyEffect(cast, node, hit, groupPower);
		} finally {
			executeBonus = outerBonus;
			currentElement = outerElement;
		}
		RuneSeals.onSpell(cast, hit, node.effect.element());
		dev.wildercord.familiar.Familiars.onSpell(cast, hit, node.effect.element());
	}

	/**
	 * Wraps a task scheduled while an effect is being applied, so the damage it deals later (a
	 * meteor landing, a countdown going off, the next slash) still gets that effect's Execute and
	 * Unison.
	 */
	static Runnable carryContext(Runnable task) {
		if (executeBonus == 1.0 && currentElement.isEmpty()) {
			return task;
		}
		double bonus = executeBonus;
		String element = currentElement;
		return () -> {
			double outerBonus = executeBonus;
			String outerElement = currentElement;
			executeBonus = bonus;
			currentElement = element;
			try {
				task.run();
			} finally {
				executeBonus = outerBonus;
				currentElement = outerElement;
			}
		};
	}

	private static void applyEffect(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower) {
		RuneDef rune = node.effect;
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		// Elemental leaning: the element you cast most hits a little harder. Innate runes grow with the heart.
		double leaning = !rune.element().isEmpty() && rune.element().equals(cast.info.leaning()) ? 1 + dev.wildercord.spell.Leaning.POWER : 1.0;
		double innate = Runes.innate(rune) ? Innates.scale(caster) : 1.0;
		double power = SpellNumbers.power(node) * groupPower * cast.power * leaning * innate;
		double duration = SpellNumbers.duration(node) * cast.duration;
		int amplify = node.count(Runes.AMPLIFY);
		List<LivingEntity> helped = filter(hit.entities(), e -> Targets.canHelp(caster, e));
		List<LivingEntity> harmed = filter(hit.entities(), e -> Targets.canHarm(caster, e));
		if (!harmed.isEmpty() && (rune.kind() == dev.wildercord.spell.EffectKind.HARMFUL || rune.kind() == dev.wildercord.spell.EffectKind.MOVEMENT && !hit.self())) {
			// A Shield stops a spell that costs no more than the one that raised it; a costlier one breaks it and goes through.
			harmed = Shields.screen(cast, harmed, hit);
		}
		// Self always means you: movement effects move you even though they are "harmful" to others.
		List<LivingEntity> moved = hit.self() ? List.of(caster) : harmed;

		switch (rune.path()) {
			case "feather_fall" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks(12, duration), 0, false, true));
				t.resetFallDistance();
				Vfx.featherFall(level, t);
			});
			case "swift" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks(10, duration), Math.min(4, 2 + amplify), false, true));
				Vfx.swift(level, t);
			});
			case "night_eye" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks(60, duration), 0, false, true));
				Vfx.nightEye(level, t);
			});
			case "heal" -> helped.forEach(t -> {
				t.heal((float) (8 * power));
				Vfx.heal(level, t);
			});
			case "shield" -> {
				int ticks = (int) Math.round(SpellNumbers.shieldTicks(node) * cast.duration);
				helped.forEach(t -> Shields.raise(cast, t, ticks));
			}
			case "harm" -> harmed.forEach(t -> {
				hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 7 * power);
				Vfx.harm(level, t);
			});
			case "push" -> harmed.forEach(t -> {
				Vec3 away = horizontal(t.position().subtract(hit.origin()), hit.dir());
				push(t, away.scale(2.2 * power).add(0, 0.45, 0));
				Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				Vfx.push(level, t, away);
			});
			case "pull" -> harmed.forEach(t -> {
				Vec3 towards = hit.origin().subtract(t.position());
				double distance = towards.length();
				Vfx.pull(level, t, hit.origin());
				Reactions.mark(t, Reactions.Mark.PULLED);
				push(t, towards.normalize().scale(Math.min(2.6, 0.5 + distance * 0.22) * power).add(0, 0.3, 0));
			});
			case "launch" -> moved.forEach(t -> {
				Vec3 v = t.getDeltaMovement();
				Vec3 kick = new Vec3(0, 1.5 * power, 0);
				if (t == caster) {
					kick = kick.add(horizontal(caster.getLookAngle(), caster.getLookAngle()).scale(0.7));
					caster.resetFallDistance();
				}
				push(t, new Vec3(v.x, Math.max(0, v.y), v.z).add(kick).subtract(v));
				if (t != caster) {
					Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				}
				Vfx.launch(level, t);
			});
			case "dash" -> moved.forEach(t -> {
				Vec3 look = caster.getLookAngle();
				Vec3 dir = new Vec3(look.x, Math.max(-0.2, Math.min(0.45, look.y)) + 0.12, look.z).normalize();
				push(t, dir.scale(2.6 * power));
				if (t == caster) {
					caster.resetFallDistance();
				} else {
					Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				}
				Vfx.dash(level, t, dir);
			});
			case "fire" -> harmed.forEach(t -> {
				double react = Reactions.fire(cast, t);
				t.igniteForSeconds((float) (6 * duration));
				hurt(cast, t, level.damageSources().source(DamageTypes.IN_FIRE, caster), 5 * power * react);
				Vfx.fire(level, t);
			});
			case "frost" -> harmed.forEach(t -> {
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 5 * power);
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(4, duration), 2, false, true));
				t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + ticks(4, duration)));
				Reactions.mark(t, Reactions.Mark.FROZEN);
				Vfx.frost(level, t);
			});
			case "lightning" -> {
				List<Vec3> strikes = new ArrayList<>();
				harmed.forEach(t -> strikes.add(t.position()));
				if (strikes.isEmpty()) {
					strikes.add(hit.point());
				}
				for (int i = 0; i < Math.min(MAX_STRIKES_PER_HIT, strikes.size()); i++) {
					lightning(cast, strikes.get(i), power);
				}
			}
			case "explode" -> {
				double radius = SpellNumbers.explodeRadius(node);
				List<Vec3> blasts = new ArrayList<>();
				harmed.forEach(t -> blasts.add(t.getBoundingBox().getCenter()));
				if (blasts.isEmpty()) {
					blasts.add(hit.point());
				}
				for (int i = 0; i < Math.min(4, blasts.size()); i++) {
					explode(cast, blasts.get(i), radius, power);
				}
			}
			case "sonic_boom" -> harmed.forEach(t -> {
				Vfx.sonicBoom(level, hit.origin(), t.getBoundingBox().getCenter());
				hurt(cast, t, level.damageSources().sonicBoom(caster), 16 * power);
			});
			case "wither" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.WITHER, ticks(8, duration), 2, false, true), caster);
				Vfx.wither(level, t);
			});
			case "dragon_breath" -> dragonBreath(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power, duration);
			case "shock" -> harmed.forEach(t -> shock(cast, t, power));
			case "haste" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks(30, duration), Math.min(3, 1 + amplify), false, true));
				Vfx.haste(level, t);
			});
			case "reveal" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks(15, duration), 0, false, false));
				Vfx.reveal(level, t);
			});
			case "regrowth" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks(8, duration), Math.min(3, 1 + amplify), false, true));
				Vfx.regrowth(level, t);
			});
			case "cleanse" -> helped.forEach(t -> {
				List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> bad = new ArrayList<>();
				for (MobEffectInstance effect : t.getActiveEffects()) {
					if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
						bad.add(effect.getEffect());
					}
				}
				bad.forEach(t::removeEffect);
				t.clearFire();
				t.setTicksFrozen(0);
				Reactions.clear(t, Reactions.Mark.FROZEN);
				Vfx.cleanse(level, t);
			});
			case "stoneskin" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks(10, duration), Math.min(3, 1 + amplify), false, true));
				Vfx.stoneskin(level, t);
			});
			case "root" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(3, duration), 6, false, false));
				t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
				Vfx.root(level, t);
			});
			case "veil" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks(12, duration), 0, false, true));
				for (Entity e : level.getEntities(t, t.getBoundingBox().inflate(16.0), e -> e instanceof net.minecraft.world.entity.Mob)) {
					net.minecraft.world.entity.Mob mob = (net.minecraft.world.entity.Mob) e;
					if (mob.getTarget() == t) {
						mob.setTarget(null);
					}
				}
				Vfx.veil(level, t);
			});
			case "empower" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks(10, duration), Math.min(3, 1 + amplify), false, true));
				Vfx.empower(level, t);
			});
			case "levitate" -> moved.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks(3, duration), 1, false, true));
				if (t != caster) {
					Reactions.mark(t, Reactions.Mark.WINDSWEPT, ticks(3, duration) + 20);
				}
				Vfx.levitate(level, t);
			});
			case "freeze" -> harmed.forEach(t -> {
				Spirits.freeze(t, ticks(2.5, duration));
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 3 * power);
				Vfx.freeze(level, t);
			});
			case "meteor" -> {
				List<Vec3> targets = new ArrayList<>();
				harmed.forEach(t -> targets.add(t.position()));
				if (targets.isEmpty()) {
					targets.add(hit.point());
				}
				double radius = 3.0 * SpellNumbers.effectRadius(node);
				for (int i = 0; i < Math.min(4, targets.size()); i++) {
					meteor(cast, targets.get(i), radius, power);
				}
			}
			case "tremor" -> tremor(cast, hit.point(), 4.0 * SpellNumbers.effectRadius(node), power);
			case "gravity_well" -> gravityWell(cast, hit.point(), 7.0 * SpellNumbers.effectRadius(node), power, duration);
			case "summon" -> Spirits.summonWolves(cast, caster.position(), 3, power, duration);
			case "venom" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.POISON, ticks(6, duration), Math.min(3, 1 + amplify), false, true), caster);
				hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 2 * power);
				Vfx.venom(level, t);
			});
			case "smite" -> harmed.forEach(t -> {
				double undead = t.isInvertedHealAndHarm() ? 2.0 : 1.0;
				hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 10 * power * undead);
				Vfx.smite(level, t);
			});
			case "inferno" -> inferno(cast, hit.point(), 4.0 * SpellNumbers.effectRadius(node), power, duration);
			case "thunderclap" -> thunderclap(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power);
			case "starfall" -> starfall(cast, hit.point(), 4.0 * SpellNumbers.effectRadius(node), power);
			case "blind" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks(5, duration), 0, false, true));
				t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks(5, duration), 0, false, true));
				if (t instanceof net.minecraft.world.entity.Mob mob) {
					mob.setTarget(null);
				}
				Vfx.blind(level, t);
			});
			case "chill" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(6, duration), 1, false, true));
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 1 * power);
				Reactions.mark(t, Reactions.Mark.FROZEN, 40);
				Vfx.chill(level, t);
			});
			case "silence" -> harmed.forEach(t -> {
				if (t instanceof net.minecraft.world.entity.Mob mob) {
					mob.setTarget(null);
				}
				t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks(6, duration), 1, false, true));
				Vfx.silence(level, t);
			});
			case "fireward" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks(30, duration), 0, false, true));
				t.clearFire();
				Vfx.fireward(level, t);
			});
			case "nourish" -> helped.forEach(t -> {
				if (t instanceof Player player) {
					player.getFoodData().eat((int) Math.round(6 * power), 0.6F);
				}
				Vfx.nourish(level, t);
			});
			case "tidebreath" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ticks(30, duration), 0, false, true));
				t.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, ticks(30, duration), 0, false, true));
				Vfx.tidebreath(level, t);
			});
			case "leap" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks(15, duration), Math.min(4, 2 + amplify), false, true));
				Vfx.leap(level, t);
			});
			case "grapple" -> grapple(cast, hit, power);
			case "harvest" -> harvest(cast, hit, SpellNumbers.effectRadius(node));
			case "icepath" -> icepath(cast, hit, 3.0 * SpellNumbers.effectRadius(node));
			case "collect" -> collect(cast, hit.point(), 8.0 * SpellNumbers.effectRadius(node));
			case "excavate" -> excavate(cast, hit, amplify > 0);
			case "blink" -> blink(cast, hit);
			case "light" -> light(cast, hit, duration);
			case "grow" -> grow(cast, hit, power);
			case "break" -> breakBlock(cast, hit, amplify > 0);
			case "cleave" -> harmed.forEach(t -> Techniques.cleave(cast, t, power));
			case "dismantle" -> harmed.forEach(t -> Techniques.dismantle(cast, t, power));
			case "blackspark" -> harmed.forEach(t -> Techniques.blackspark(cast, t, power));
			case "aftershock" -> harmed.forEach(t -> Techniques.aftershock(cast, t, power));
			case "resonance" -> harmed.forEach(t -> Techniques.resonance(cast, t, power, ticks(10, duration)));
			case "ripple" -> harmed.forEach(t -> Techniques.ripple(cast, t, power));
			case "primer" -> {
				for (int i = 0; i < Math.min(MAX_STRIKES_PER_HIT, harmed.size()); i++) {
					Techniques.primer(cast, harmed.get(i), 3.0 * SpellNumbers.effectRadius(node), power);
				}
			}
			case "blackflame" -> harmed.forEach(t -> Techniques.blackflame(cast, t, power, (int) Math.round(6 * duration), true));
			case "hollow" -> Techniques.hollow(cast, hit, harmed, 4.0 * SpellNumbers.effectRadius(node), power);
			case "repel" -> Techniques.repel(cast, hit, 3.0 * SpellNumbers.effectRadius(node), power);
			case "decree" -> Techniques.decree(cast, harmed, ticks(2, duration));
			case "weigh" -> harmed.forEach(t -> Techniques.weigh(cast, t, ticks(5, duration)));
			case "shackle" -> harmed.forEach(t -> Techniques.shackle(cast, t, ticks(5, duration)));
			case "bubble" -> harmed.forEach(t -> Techniques.bubble(cast, t, ticks(3, duration), power));
			case "infinity" -> helped.forEach(t -> Wards.infinity(cast, t, ticks(6, duration)));
			case "reversal" -> helped.forEach(t -> Wards.reversal(cast, t, ticks(30, duration)));
			case "reflect" -> helped.forEach(t -> Wards.reflect(cast, t, ticks(10, duration), Math.min(1.5, 0.6 * power)));
			case "overdrive" -> helped.forEach(t -> Techniques.overdrive(cast, t, ticks(10, duration), amplify));
			case "foresight" -> helped.forEach(t -> Wards.foresight(cast, t, ticks(15, duration), (int) Math.max(1, Math.round(2 * power))));
			case "restore" -> helped.forEach(t -> Techniques.restore(cast, t, power));
			case "swap" -> Techniques.swap(cast, hit);
			case "zipper" -> Techniques.zipper(cast);
			case "shadowstep" -> Techniques.shadowstep(cast, hit);
			case "stasis" -> harmed.forEach(t -> Wards.stasis(cast, t, ticks(5, duration)));
			case "rewind" -> helped.forEach(t -> Wards.rewind(cast, t));
			case "accelerate" -> helped.forEach(t -> Techniques.accelerate(cast, t, ticks(10, duration), amplify));
			case "time_skip" -> Techniques.timeSkip(cast);
			case "rampart" -> Techniques.rampart(cast, hit, SpellNumbers.effectRadius(node), ticks(10, duration));
			case "shades" -> Spirits.summonShades(cast, caster.position(), 2, power, duration);
			case "thunderbird" -> Techniques.thunderbird(cast, power, ticks(15, duration));
			// Batch 6: protection.
			case "barrier" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks(20, duration), Math.min(4, amplify), false, true));
				ExpansionVfx.barrier(level, t, Vfx.theme(rune));
			});
			case "brace" -> helped.forEach(t -> brace(cast, t, ticks(2, duration)));
			case "anchor" -> helped.forEach(t -> anchor(cast, t, ticks(15, duration)));
			case "bramble" -> helped.forEach(t -> bramble(cast, t, ticks(10, duration), power));
			case "frostward" -> helped.forEach(t -> frostward(cast, t, ticks(60, duration)));
			case "cushion" -> helped.forEach(t -> cushion(cast, t, ticks(30, duration), power));
			case "deflect" -> helped.forEach(t -> deflect(cast, t, ticks(8, duration)));
			case "haven" -> haven(cast, hit.self() ? caster.position() : CastEngine.ground(level, hit.point().add(0, 0.5, 0)),
				4.0 * SpellNumbers.effectRadius(node), ticks(8, duration));
			// Batch 6: mining and building.
			case "chisel" -> chisel(cast, hit, amplify > 0 ? 2 : 1);
			case "glimmer" -> glimmer(cast, hit, SpellNumbers.effectRadius(node));
			case "prune" -> prune(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node));
			case "tunnel" -> tunnel(cast, hit, amplify > 0 ? 3 : 2);
			case "vein" -> vein(cast, hit, amplify > 0 ? 3 : 2);
			case "smelt" -> smelt(cast, hit, amplify > 0 ? 3 : 2);
			case "fell" -> fell(cast, hit);
			case "span" -> span(cast, hit, SpellNumbers.effectRadius(node), ticks(30, duration));
			// Batch 6: a simple spell for every element.
			case "ember" -> harmed.forEach(t -> {
				double react = Reactions.fire(cast, t);
				t.igniteForSeconds((float) (3 * duration));
				hurt(cast, t, level.damageSources().source(DamageTypes.IN_FIRE, caster), 3 * power * react);
				ExpansionVfx.ember(level, t);
			});
			case "icicle" -> harmed.forEach(t -> {
				boolean slowed = t.hasEffect(MobEffects.SLOWNESS) || Reactions.has(t, Reactions.Mark.FROZEN);
				ExpansionVfx.icicle(level, t, slowed);
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), (slowed ? 6 : 4) * power);
				// A touch of frost on the skin, well short of frozen solid.
				t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() - 1, t.getTicksFrozen() + 40));
			});
			case "pelt" -> harmed.forEach(t -> {
				Vec3 away = horizontal(t.position().subtract(hit.origin()), hit.dir());
				ExpansionVfx.pelt(level, t, away);
				hurt(cast, t, level.damageSources().source(DamageTypes.FALLING_BLOCK, caster), 4 * power);
				push(t, away.scale(0.6 * Math.sqrt(power)).add(0, 0.25, 0));
			});
			case "windcut" -> harmed.forEach(t -> {
				Vec3 away = horizontal(t.position().subtract(hit.origin()), hit.dir());
				hurt(cast, t, level.damageSources().source(DamageTypes.WIND_CHARGE, caster), 4 * power);
				push(t, away.scale(0.7).add(0, 0.2, 0));
				Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				ExpansionVfx.windcut(level, t, away);
			});
			case "leech" -> harmed.forEach(t -> {
				float before = t.getHealth();
				hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 3 * power);
				float taken = Math.max(0.0F, before - t.getHealth());
				if (taken > 0 && caster.isAlive()) {
					caster.heal(taken);
				}
				ExpansionVfx.leech(level, t, caster);
			});
			case "hex" -> harmed.forEach(t -> hex(cast, t, ticks(8, duration)));
			case "rend" -> harmed.forEach(t -> rend(cast, t, ticks(10, duration)));
			case "countdown" -> harmed.forEach(t -> countdown(cast, t, power));
			case "jolt" -> harmed.forEach(t -> {
				ExpansionVfx.jolt(level, t);
				hurt(cast, t, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), 4 * power * Reactions.storm(cast, t));
				Spirits.hold(t, ticks(1, duration));
			});
			case "bleed" -> harmed.forEach(t -> bleed(cast, t, power, (int) Math.round(8 * duration)));
			case "coldsnap" -> coldsnap(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power, duration);
			case "flashfire" -> flashfire(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power);
			case "banish" -> harmed.forEach(t -> banish(cast, t, Math.min(16.0, 8.0 * power)));
			case "cyclone" -> cyclone(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power, ticks(2, duration));
			case "blood_thread", "kindling", "twin_star", "borrowed_time", "gale_mantle", "stoneform", "mirrorfrost", "fortune", "phantom", "stormheart" ->
				Innates.apply(cast, rune, helped, harmed, power, duration);
			default -> { }
		}
		List<LivingEntity> touched = rune.kind() == EffectKind.HELPFUL ? helped : harmed;
		if (!hit.self()) {
			Vfx.Theme theme = Vfx.theme(rune);
			touched.forEach(t -> Vfx.touched(level, t, theme));
		}
	}

	// ------------------------------------------------------------------ helpers

	private static List<LivingEntity> filter(List<Entity> entities, Predicate<Entity> test) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : entities) {
			if (e instanceof LivingEntity living && test.test(e)) {
				out.add(living);
			}
		}
		return out;
	}

	static int ticks(double seconds, double duration) {
		return (int) Math.round(seconds * 20 * duration);
	}

	/** A horizontal unit vector, falling back to {@code fallback} when the input is (nearly) vertical. */
	static Vec3 horizontal(Vec3 v, Vec3 fallback) {
		Vec3 flat = new Vec3(v.x, 0, v.z);
		if (flat.lengthSqr() < 1.0E-4) {
			flat = new Vec3(fallback.x, 0, fallback.z);
		}
		return flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
	}

	static void push(LivingEntity target, Vec3 impulse) {
		double resist = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		Vec3 scaled = target instanceof Player ? impulse : impulse.scale(Math.max(0.0, 1.0 - resist));
		target.setDeltaMovement(target.getDeltaMovement().add(scaled));
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	/**
	 * Damages with invulnerability frames skipped, so stacked effects in one spell all land.
	 * Execute on the current effect doubles it against targets under half health.
	 */
	static void hurt(Cast cast, LivingEntity target, DamageSource source, double amount) {
		// Damage that didn't come through a shape's hit (a meteor landing, a secret spell's blast) meets a Shield here.
		if (Shields.stops(cast, target, cast.caster.getEyePosition())) {
			return;
		}
		if (executeBonus > 1.0 && target.getHealth() < target.getMaxHealth() * 0.5F) {
			amount *= executeBonus;
			Vfx.emit(cast.level, net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, target.getBoundingBox().getCenter(), 4, 0.3, 0.1);
		}
		amount *= Innates.fortune(cast, target);
		amount *= Unison.onHit(cast, target, currentElement);
		amount *= hexBonus(cast, target);
		float damage = (float) amount;
		// PvP only: a monster's spell already has its power set by difficulty.
		if (target instanceof Player && cast.caster instanceof Player) {
			damage *= PVP_DAMAGE;
		}
		HeartCircles.hurtBySpell(cast, target);
		Innates.spellHit(cast, target);
		target.setInvulnerableTime(0);
		target.hurtServer(cast.level, source, damage);
		// A heavy hit lands with a punch for whoever cast it.
		if (damage >= 8) {
			ScreenFx.punch(cast.caster, Math.min(1, damage / 20F));
		}
	}

	private static void lightning(Cast cast, Vec3 at, double power) {
		ServerLevel level = cast.level;
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.snapTo(at.x, at.y, at.z);
			level.addFreshEntity(bolt);
		}
		Vfx.lightning(level, at);
		for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(2.0, 3.0, 2.0), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity target = (LivingEntity) e;
			hurt(cast, target, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 12 * power * Reactions.storm(cast, target));
			target.igniteForSeconds(4);
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 3, false, false));
		}
	}

	static void explode(Cast cast, Vec3 center, double radius, double power) {
		ServerLevel level = cast.level;
		double implode = Reactions.blast(cast, center, radius);
		radius *= implode;
		power *= implode > 1 ? 1.3 : 1.0;
		Vfx.explosion(level, center, radius);
		DamageSource source = level.damageSources().explosion(cast.caster, cast.caster);
		for (Entity e : level.getEntities((Entity) null, new AABB(center, center).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity target = (LivingEntity) e;
			double distance = target.getBoundingBox().getCenter().distanceTo(center);
			if (distance > radius) {
				continue;
			}
			double falloff = 1.0 - 0.4 * (distance / radius);
			hurt(cast, target, source, 12 * power * falloff * Reactions.fire(cast, target));
			Vec3 away = target.position().subtract(center);
			push(target, (away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize()).scale(1.3 * falloff).add(0, 0.45, 0));
		}
	}

	private static void dragonBreath(Cast cast, Vec3 center, double radius, double power, double duration) {
		int pulses = (int) Math.round(5 * duration);
		Fx.sound(cast.level, center, net.minecraft.sounds.SoundEvents.ENDER_DRAGON_SHOOT, 1.0F, 1.0F);
		for (int i = 0; i < pulses; i++) {
			Scheduler.later(1 + i * 20, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.dragonBreath(cast.level, center, radius);
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(center, center).inflate(radius, 2.0, radius), e -> Targets.canHarm(cast.caster, e))) {
					hurt(cast, (LivingEntity) e, cast.level.damageSources().source(DamageTypes.DRAGON_BREATH, cast.caster), 5 * power);
				}
			});
		}
	}

	private static void blink(Cast cast, Cast.Hit hit) {
		LivingEntity caster = cast.caster;
		Vec3 target = hit.point();
		Vec3 back = hit.dir().lengthSqr() > 1.0E-4 ? hit.dir().normalize().scale(-0.6) : Vec3.ZERO;
		if (target.distanceTo(caster.position()) > 40) {
			Casters.tell(caster, Component.translatable("message.wildercord.blink_far"));
			return;
		}
		for (int attempt = 0; attempt < 6; attempt++) {
			Vec3 spot = target.add(back.scale(1 + attempt * 0.5)).add(0, attempt % 2 == 0 ? 0 : 1, 0);
			spot = CastEngine.ground(cast.level, spot);
			AABB box = caster.getDimensions(caster.getPose()).makeBoundingBox(spot);
			if (cast.level.noCollision(caster, box)) {
				Vec3 from = caster.position();
				caster.teleportTo(cast.level, spot.x, spot.y, spot.z, Set.<Relative>of(), caster.getYRot(), caster.getXRot(), false);
				caster.resetFallDistance();
				Vfx.blink(cast.level, from, spot);
				return;
			}
		}
	}

	/** Inferno: everything around the point burns for a few seconds. */
	private static void inferno(Cast cast, Vec3 point, double radius, double power, double duration) {
		int pulses = (int) Math.round(4 * duration);
		Fx.sound(cast.level, point, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
		for (int i = 0; i < pulses; i++) {
			Scheduler.later(1 + i * 20, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.inferno(cast.level, point, radius);
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius, 2.0, radius), e -> Targets.canHarm(cast.caster, e))) {
					LivingEntity t = (LivingEntity) e;
					t.igniteForSeconds(3);
					hurt(cast, t, cast.level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 3 * power * Reactions.fire(cast, t));
				}
			});
		}
	}

	/** Thunderclap: a crack of thunder that hurts and hurls everything around the point. */
	private static void thunderclap(Cast cast, Vec3 point, double radius, double power) {
		Vfx.thunderclap(cast.level, point, radius);
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity t = (LivingEntity) e;
			hurt(cast, t, cast.level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 5 * power * Reactions.storm(cast, t));
			Vec3 away = horizontal(t.position().subtract(point), cast.caster.getLookAngle());
			push(t, away.scale(2.0 * power).add(0, 0.5, 0));
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 3, false, false));
			Reactions.mark(t, Reactions.Mark.WINDSWEPT);
		}
	}

	/** Starfall: stars rain down around the point over two seconds. */
	private static void starfall(Cast cast, Vec3 point, double radius, double power) {
		for (int i = 0; i < 8; i++) {
			double a = cast.level.getRandom().nextDouble() * Math.PI * 2;
			double r = Math.sqrt(cast.level.getRandom().nextDouble()) * radius;
			Vec3 target = CastEngine.ground(cast.level, point.add(Math.cos(a) * r, 2, Math.sin(a) * r));
			Scheduler.later(1 + i * 5, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.star(cast.level, target);
				Scheduler.later(6, () -> {
					for (Entity e : cast.level.getEntities((Entity) null, new AABB(target, target).inflate(1.6, 2.0, 1.6), e -> Targets.canHarm(cast.caster, e))) {
						hurt(cast, (LivingEntity) e, cast.level.damageSources().indirectMagic(cast.caster, cast.caster), 6 * power);
					}
				});
			});
		}
	}

	/** Grapple: pulls the caster toward the point the spell hit. */
	private static void grapple(Cast cast, Cast.Hit hit, double power) {
		LivingEntity caster = cast.caster;
		Vec3 to = hit.point().subtract(caster.position());
		double distance = to.length();
		if (distance < 1.0 || distance > 48) {
			return;
		}
		Vec3 pull = to.normalize().scale(Math.min(3.2, 0.8 + distance * 0.12) * Math.sqrt(power)).add(0, 0.35, 0);
		caster.setDeltaMovement(pull);
		caster.needsSync = true;
		if (caster instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
		caster.resetFallDistance();
		Scheduler.later(30, caster::resetFallDistance);
		Vfx.grapple(cast.level, caster.getEyePosition().subtract(0, 0.4, 0), hit.point());
	}

	/** Harvest: breaks grown crops around the block hit and replants them from their drops. */
	private static void harvest(Cast cast, Cast.Hit hit, double radiusScale) {
		BlockPos center = targetBlock(hit);
		int r = (int) Math.round(1 * radiusScale) + 1;
		int harvested = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
			BlockPos p = pos.immutable();
			BlockState state = cast.level.getBlockState(p);
			if (!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) || !crop.isMaxAge(state)) {
				continue;
			}
			if (!Casters.mayBuild(cast.caster) || !Casters.mayEdit(cast.caster, cast.level, p) || !cast.takeBlock()) {
				break;
			}
			cast.level.destroyBlock(p, true, cast.caster);
			cast.level.setBlockAndUpdate(p, crop.getStateForAge(0));
			harvested++;
		}
		if (harvested > 0) {
			Vfx.grow(cast.level, Vec3.atCenterOf(center).add(0, 0.6, 0));
			Fx.sound(cast.level, Vec3.atCenterOf(center), net.minecraft.sounds.SoundEvents.CROP_BREAK, 0.8F, 1.2F);
		}
	}

	/** Icepath: water near the point freezes into frosted ice that melts on its own. */
	private static void icepath(Cast cast, Cast.Hit hit, double radius) {
		BlockPos center = BlockPos.containing(hit.point().x, hit.point().y - 0.5, hit.point().z);
		int r = (int) Math.ceil(radius);
		BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
		int frozen = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 1, r))) {
			BlockPos p = pos.immutable();
			if (p.distSqr(center) > radius * radius) {
				continue;
			}
			BlockState state = cast.level.getBlockState(p);
			if (state.is(Blocks.WATER) && state.getFluidState().isSource() && cast.level.getBlockState(p.above()).isAir()
					&& Casters.mayEdit(cast.caster, cast.level, p)) {
				if (!cast.takeBlock()) {
					break;
				}
				cast.level.setBlockAndUpdate(p, ice);
				cast.level.scheduleTick(p, Blocks.FROSTED_ICE, 60 + cast.level.getRandom().nextInt(60));
				frozen++;
			}
		}
		if (frozen > 0) {
			Vfx.icepath(cast.level, Vec3.atCenterOf(center), radius);
		}
	}

	/** Collect reaches this far at most, however widened. */
	private static final double MAX_COLLECT = 24.0;

	/** Collect: items and experience orbs around the point fly to the caster (not off ground they couldn't build on: a claim, spawn). */
	private static void collect(Cast cast, Vec3 point, double radius) {
		LivingEntity caster = cast.caster;
		double reach = Math.min(MAX_COLLECT, radius);
		int moved = 0;
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(reach),
				e -> (e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof net.minecraft.world.entity.ExperienceOrb)
					&& e.distanceToSqr(point) <= reach * reach && Casters.mayEdit(caster, cast.level, BlockPos.containing(e.position()).below()))) {
			Vfx.stream(cast.level, e.position(), caster.position().add(0, 1, 0), Vfx.theme("void"), 1);
			e.teleportTo(caster.getX(), caster.getY() + 0.5, caster.getZ());
			if (e instanceof net.minecraft.world.entity.item.ItemEntity item) {
				item.setNoPickUpDelay();
			}
			moved++;
		}
		if (moved > 0) {
			Fx.sound(cast.level, caster.position(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.8F, 0.8F);
		}
	}

	/** Excavate: mines a 3x3 face of blocks around the block hit. */
	private static void excavate(Cast cast, Cast.Hit hit, boolean amplified) {
		if (hit.block() == null) {
			return;
		}
		net.minecraft.core.Direction face = hit.face() == null ? net.minecraft.core.Direction.UP : hit.face();
		BlockPos center = hit.block();
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				BlockPos p = switch (face.getAxis()) {
					case X -> center.offset(0, a, b);
					case Y -> center.offset(a, 0, b);
					case Z -> center.offset(a, b, 0);
				};
				BlockState state = cast.level.getBlockState(p);
				if (state.isAir() || state.getDestroySpeed(cast.level, p) < 0
						|| state.is(amplified ? BlockTags.INCORRECT_FOR_DIAMOND_TOOL : BlockTags.INCORRECT_FOR_IRON_TOOL)) {
					continue;
				}
				if (mayEdit(cast, p)) {
					cast.level.destroyBlock(p, true, cast.caster);
					magicBreak(cast.level, p);
				}
			}
		}
		Vfx.tremor(cast.level, Vec3.atCenterOf(center), 1.5);
	}

	/** Shock: a small zap that arcs on to the nearest other enemy. */
	private static void shock(Cast cast, LivingEntity target, double power) {
		ServerLevel level = cast.level;
		Vfx.shockArc(level, target.getBoundingBox().getCenter().add(0, 1.2, 0), target.getBoundingBox().getCenter());
		hurt(cast, target, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 4 * power * Reactions.storm(cast, target));
		Entity next = level.getEntities(target, target.getBoundingBox().inflate(4.0), e -> Targets.canHarm(cast.caster, e))
			.stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(target))).orElse(null);
		if (next != null) {
			Vfx.shockArc(level, target.getBoundingBox().getCenter(), next.getBoundingBox().getCenter());
			hurt(cast, (LivingEntity) next, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 3 * power);
		}
	}

	/** Meteor: a burning rock falls for half a second, then bursts. */
	private static void meteor(Cast cast, Vec3 target, double radius, double power) {
		Vec3 ground = CastEngine.ground(cast.level, target.add(0, 1, 0));
		Vfx.meteorFall(cast.level, ground);
		Scheduler.later(12, () -> {
			if (!cast.alive()) {
				return;
			}
			double implode = Reactions.blast(cast, ground, radius);
			double r = radius * implode;
			Vfx.explosion(cast.level, ground.add(0, 0.5, 0), r);
			DamageSource source = cast.level.damageSources().explosion(cast.caster, cast.caster);
			for (Entity e : cast.level.getEntities((Entity) null, new AABB(ground, ground).inflate(r), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity t = (LivingEntity) e;
				double distance = t.position().distanceTo(ground);
				if (distance > r) {
					continue;
				}
				double falloff = 1.0 - 0.4 * (distance / r);
				hurt(cast, t, source, 10 * power * falloff * (implode > 1 ? 1.3 : 1.0) * Reactions.fire(cast, t));
				t.igniteForSeconds(4);
				Vec3 away = t.position().subtract(ground);
				push(t, (away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize()).scale(0.8 * falloff).add(0, 0.5, 0));
			}
		});
	}

	/** Tremor: the ground erupts around a point, hurting and throwing enemies standing on it. */
	private static void tremor(Cast cast, Vec3 point, double radius, double power) {
		Vec3 ground = CastEngine.ground(cast.level, point.add(0, 0.5, 0));
		Vfx.tremor(cast.level, ground, radius);
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(ground, ground).inflate(radius, 2.0, radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity t = (LivingEntity) e;
			if (t.position().distanceTo(ground) > radius || (!t.onGround() && t.position().y - ground.y > 1.5)) {
				continue;
			}
			hurt(cast, t, cast.level.damageSources().source(DamageTypes.FALLING_BLOCK, cast.caster), 8 * power);
			push(t, new Vec3(0, 0.75, 0));
			Reactions.mark(t, Reactions.Mark.WINDSWEPT);
		}
	}

	/** Gravity Well: for a while, enemies around the point are dragged into it, then crushed a little. */
	private static void gravityWell(Cast cast, Vec3 point, double radius, double power, double duration) {
		int total = (int) Math.round(40 * duration);
		Fx.sound(cast.level, point, net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, 0.8F, 1.4F);
		for (int t = 0; t <= total; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.gravityWell(cast.level, point, radius, tick);
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
					LivingEntity victim = (LivingEntity) e;
					Vec3 towards = point.subtract(victim.position());
					double distance = towards.length();
					if (distance > radius || distance < 0.4) {
						continue;
					}
					victim.setDeltaMovement(victim.getDeltaMovement().scale(0.5).add(towards.normalize().scale(Math.min(0.6, 0.12 + distance * 0.05))));
					victim.needsSync = true;
					if (victim instanceof ServerPlayer player) {
						player.connection.send(new ClientboundSetEntityMotionPacket(player));
					}
					Reactions.mark(victim, Reactions.Mark.PULLED);
					if (tick >= total - 1) {
						hurt(cast, victim, cast.level.damageSources().indirectMagic(cast.caster, cast.caster), 4 * power);
					}
				}
			});
		}
	}

	/** The block a world effect should act on: the one hit, or the one under a creature. */
	private static BlockPos targetBlock(Cast.Hit hit) {
		if (hit.block() != null) {
			return hit.block();
		}
		return BlockPos.containing(hit.point().x, hit.point().y - 0.5, hit.point().z);
	}

	private static boolean mayEdit(Cast cast, BlockPos pos) {
		// A Rampart's wall is only there for a while: spells don't mine it (it would drop packed mud).
		return Casters.mayBuild(cast.caster) && !Techniques.isRampart(cast.level, pos) && Casters.mayEdit(cast.caster, cast.level, pos)
			&& cast.takeBlock();
	}

	/** Hooks the effects need from the start (Span's rules). */
	public static void init() {
		SpanRules.ready();
	}

	private static void light(Cast cast, Cast.Hit hit, double duration) {
		BlockPos pos = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		ServerLevel level = cast.level;
		if (!level.getBlockState(pos).isAir() || !mayEdit(cast, pos)) {
			return;
		}
		level.setBlockAndUpdate(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
		GlobalPos lit = GlobalPos.of(level.dimension(), pos.immutable());
		LIGHTS.add(lit);
		Vfx.light(level, Vec3.atCenterOf(pos));
		Scheduler.later(ticks(60, duration), () -> {
			LIGHTS.remove(lit);
			if (level.getBlockState(pos).is(Blocks.LIGHT)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
		});
	}

	/** Bone-meals the block that was hit and the ones around it. */
	private static void grow(Cast cast, Cast.Hit hit, double power) {
		BlockPos center = targetBlock(hit);
		int times = (int) Math.round(2 * power);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
			BlockPos p = pos.immutable();
			if (!Casters.mayBuild(cast.caster) || !Casters.mayEdit(cast.caster, cast.level, p)) {
				continue;
			}
			boolean grew = false;
			for (int i = 0; i < times; i++) {
				grew |= BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), cast.level, p);
			}
			if (grew) {
				cast.level.levelEvent(null, 1505, p, 15);
			}
		}
		Vfx.grow(cast.level, Vec3.atCenterOf(center).add(0, 0.6, 0));
	}

	private static void breakBlock(Cast cast, Cast.Hit hit, boolean amplified) {
		if (hit.block() == null) {
			return;
		}
		BlockPos pos = hit.block();
		BlockState state = cast.level.getBlockState(pos);
		if (state.isAir() || state.getDestroySpeed(cast.level, pos) < 0) {
			return;
		}
		if (state.is(amplified ? BlockTags.INCORRECT_FOR_DIAMOND_TOOL : BlockTags.INCORRECT_FOR_IRON_TOOL)) {
			return;
		}
		if (mayEdit(cast, pos)) {
			cast.level.destroyBlock(pos, true, cast.caster);
			magicBreak(cast.level, pos);
		}
	}

	/** The crunch of a block broken by magic: once a tick at most, however many blocks a spell takes. */
	private static long lastBreakSound = Long.MIN_VALUE;

	private static void magicBreak(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
		if (level.getGameTime() != lastBreakSound) {
			lastBreakSound = level.getGameTime();
			Fx.sound(level, Vec3.atCenterOf(pos), dev.wildercord.content.WildercordSounds.MAGIC_BREAK, 0.8F, 1.0F);
		}
	}

	// ------------------------------------------------------------------ batch 6: wards

	/**
	 * A lasting ward on one creature, ticked by a single task. Casting it again (or a passive
	 * renewing it) only pushes back its end and takes the newer cast's power, so wards never stack.
	 */
	private static final class Ward {
		Cast cast;
		double power;
		long until;
		/** The last game tick its task ran: a ward whose task was lost (the server stopped) is started afresh. */
		long beat;
		/** What the ward remembers between ticks. */
		double memory;
	}

	private static final Map<String, Ward> WARDS = new HashMap<>();

	static void clearWards() {
		WARDS.clear();
	}

	/**
	 * Puts a ward on {@code t}, or renews the one it has. {@code tick} runs every {@code every} ticks
	 * while it lasts, {@code end} once it's over (or the cast is). Returns the new ward, or null when
	 * an existing one was only renewed.
	 */
	private static Ward ward(Cast cast, LivingEntity t, String kind, int ticks, double power, int every, Consumer<Ward> tick, Runnable end) {
		String key = kind + ":" + t.getUUID();
		long now = cast.level.getGameTime();
		Ward old = WARDS.get(key);
		if (old != null && old.beat <= now && now - old.beat <= 2) {
			old.until = Math.max(old.until, now + ticks);
			old.power = power;
			old.cast = cast;
			return null;
		}
		Ward ward = new Ward();
		ward.cast = cast;
		ward.power = power;
		ward.until = now + ticks;
		ward.beat = now;
		WARDS.put(key, ward);
		int[] age = {0};
		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			long time = ward.cast.level.getGameTime();
			if (WARDS.get(key) != ward || !ward.cast.alive() || !t.isAlive() || t.level() != ward.cast.level || time > ward.until) {
				WARDS.remove(key, ward);
				end.run();
				return;
			}
			ward.beat = time;
			if (age[0]++ % every == 0) {
				tick.accept(ward);
			}
			Scheduler.later(1, next[0]);
		};
		Scheduler.later(1, next[0]);
		return ward;
	}

	private static void modifier(LivingEntity t, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = t.getAttribute(attribute);
		if (instance != null) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}

	private static void unmodify(LivingEntity t, Identifier id, List<Holder<Attribute>> attributes) {
		for (Holder<Attribute> attribute : attributes) {
			AttributeInstance instance = t.getAttribute(attribute);
			if (instance != null) {
				instance.removeModifier(id);
			}
		}
	}

	private static void setMotion(LivingEntity target, Vec3 motion) {
		target.setDeltaMovement(motion);
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	/** When each creature may brace again. */
	private static final Map<UUID, Long> BRACED = new HashMap<>();

	/** Brace: 80% less damage for a moment, and a while before it can be done again. */
	private static void brace(Cast cast, LivingEntity t, int ticks) {
		long now = cast.level.getGameTime();
		Long ready = BRACED.get(t.getUUID());
		if (ready != null && now < ready && ready - now < 1200) {
			ExpansionVfx.braceSpent(cast.level, t);
			return;
		}
		BRACED.put(t.getUUID(), now + ticks + 120);
		if (BRACED.size() > 256) {
			BRACED.values().removeIf(until -> until < now);
		}
		t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 3, false, true));
		ExpansionVfx.brace(cast.level, t, Vfx.theme("earth"));
	}

	private static final Identifier ANCHOR_ID = Wildercord.id("anchor");
	private static final List<Holder<Attribute>> ANCHORED = List.of(Attributes.KNOCKBACK_RESISTANCE, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, Attributes.ARMOR);

	/** Anchor: no knockback from blows or blasts, and a little armour, for a while. */
	private static void anchor(Cast cast, LivingEntity t, int ticks) {
		modifier(t, Attributes.KNOCKBACK_RESISTANCE, ANCHOR_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);
		modifier(t, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, ANCHOR_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);
		modifier(t, Attributes.ARMOR, ANCHOR_ID, 4.0, AttributeModifier.Operation.ADD_VALUE);
		Ward fresh = ward(cast, t, "anchor", ticks, 1.0, 20, w -> { }, () -> unmodify(t, ANCHOR_ID, ANCHORED));
		if (fresh != null || !cast.passive) {
			ExpansionVfx.anchor(cast.level, t, Vfx.theme("void"));
		}
	}

	/** Bramble: whatever hurts the target from close by takes damage back and is shoved away. */
	private static void bramble(Cast cast, LivingEntity t, int ticks, double power) {
		Ward fresh = ward(cast, t, "bramble", ticks, power, 2, w -> {
			int stamp = t.getLastHurtByMobTimestamp();
			if (stamp == (int) w.memory) {
				return;
			}
			w.memory = stamp;
			LivingEntity attacker = t.getLastHurtByMob();
			DamageSource last = t.getLastDamageSource();
			// Thorns never answer thorns, so two brambled casters can't trade blows forever.
			if (attacker == null || attacker == t || !attacker.isAlive() || attacker.distanceTo(t) > 4.5
					|| (last != null && last.is(DamageTypes.THORNS)) || !Targets.canHarm(w.cast.caster, attacker)) {
				return;
			}
			ExpansionVfx.brambleStrike(w.cast.level, t, attacker);
			hurt(w.cast, attacker, w.cast.level.damageSources().thorns(t), 3 * w.power);
			Vec3 away = horizontal(attacker.position().subtract(t.position()), t.getLookAngle());
			push(attacker, away.scale(0.9).add(0, 0.3, 0));
		}, () -> { });
		if (fresh != null) {
			// Only hits from now on count.
			fresh.memory = t.getLastHurtByMobTimestamp();
		}
		ExpansionVfx.bramble(cast.level, t);
	}

	/** Frostward: the target can't freeze, and frost can't leave it brittle for Shatter. */
	private static void frostward(Cast cast, LivingEntity t, int ticks) {
		t.setTicksFrozen(0);
		Reactions.clear(t, Reactions.Mark.FROZEN);
		Ward fresh = ward(cast, t, "frostward", ticks, 1.0, 5, w -> {
			if (t.getTicksFrozen() > 0) {
				t.setTicksFrozen(0);
			}
			Reactions.clear(t, Reactions.Mark.FROZEN);
		}, () -> { });
		if (fresh != null || !cast.passive) {
			ExpansionVfx.frostward(cast.level, t, Vfx.theme("frost"));
		}
	}

	private static final Identifier CUSHION_ID = Wildercord.id("cushion");

	/** Cushion: no fall damage, and every hard landing throws a gust at the enemies around. */
	private static void cushion(Cast cast, LivingEntity t, int ticks, double power) {
		modifier(t, Attributes.FALL_DAMAGE_MULTIPLIER, CUSHION_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		t.resetFallDistance();
		Ward fresh = ward(cast, t, "cushion", ticks, power, 1, w -> {
			// Remembers how far this fall has come; it lands on the tick the ground is found again.
			if (!t.onGround()) {
				w.memory = Math.max(w.memory, t.fallDistance);
				return;
			}
			if (w.memory > 4) {
				cushionLanding(w.cast, t, w.power);
			}
			w.memory = 0;
		}, () -> unmodify(t, CUSHION_ID, List.of(Attributes.FALL_DAMAGE_MULTIPLIER)));
		if (fresh != null || !cast.passive) {
			ExpansionVfx.cushion(cast.level, t, Vfx.theme("wind"));
		}
	}

	private static void cushionLanding(Cast cast, LivingEntity t, double power) {
		ExpansionVfx.cushionLand(cast.level, t.position(), 3.0, Vfx.theme("wind"));
		for (Entity e : cast.level.getEntities(t, t.getBoundingBox().inflate(3.0, 1.0, 3.0), e -> Targets.canHarm(cast.caster, e))) {
			Vec3 away = horizontal(e.position().subtract(t.position()), t.getLookAngle());
			push((LivingEntity) e, away.scale(1.1 * Math.sqrt(power)).add(0, 0.35, 0));
			Reactions.mark(e, Reactions.Mark.WINDSWEPT);
		}
	}

	/** Deflect: projectiles coming at the target are turned aside by the wind around it. */
	private static void deflect(Cast cast, LivingEntity t, int ticks) {
		Vfx.Theme theme = Vfx.theme("wind");
		Ward fresh = ward(cast, t, "deflect", ticks, 1.0, 1, w -> {
			ServerLevel level = w.cast.level;
			Vec3 centre = t.getBoundingBox().getCenter();
			for (Projectile p : level.getEntitiesOfClass(Projectile.class, t.getBoundingBox().inflate(3.0))) {
				Entity owner = p.getOwner();
				if (owner == t || (owner != null && Targets.isAlly(w.cast.caster, owner))) {
					continue;
				}
				Vec3 v = p.getDeltaMovement();
				if (v.lengthSqr() < 0.01 || v.dot(centre.subtract(p.position())) <= 0) {
					continue;
				}
				Vec3 away = horizontal(p.position().subtract(centre), v.scale(-1));
				p.setDeltaMovement(away.scale(Math.max(0.4, v.length() * 0.6)).add(0, 0.2, 0));
				p.needsSync = true;
				ExpansionVfx.deflectHit(level, p.position(), away, theme);
			}
			if (level.getGameTime() % 5 == 0) {
				ExpansionVfx.deflectSpin(level, t, theme, level.getGameTime());
			}
		}, () -> { });
		if (fresh != null || !cast.passive) {
			ExpansionVfx.deflect(cast.level, t, theme);
		}
	}

	/**
	 * Haven: a dome over the point for a while. Allies inside are kept under Resistance, and
	 * projectiles fired from outside by anyone but an ally glance off its shell.
	 */
	private static void haven(Cast cast, Vec3 centre, double radius, int ticks) {
		ServerLevel level = cast.level;
		Vfx.Theme theme = Vfx.theme("life");
		ExpansionVfx.havenOpen(level, centre, radius, theme, ticks);
		ShapeRunners.each(cast, ticks, tick -> {
			for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(centre, centre).inflate(radius + 1.5))) {
				Entity owner = p.getOwner();
				if (p.position().distanceTo(centre) > radius + 1.0
						|| (owner != null && (Targets.isAlly(cast.caster, owner) || owner.position().distanceTo(centre) <= radius))) {
					continue;
				}
				Vec3 v = p.getDeltaMovement();
				Vec3 normal = p.position().subtract(centre).normalize();
				if (v.lengthSqr() < 0.01 || v.dot(normal) >= 0) {
					continue;
				}
				p.setDeltaMovement(v.subtract(normal.scale(2 * v.dot(normal))).scale(0.6));
				p.needsSync = true;
				ExpansionVfx.havenGlance(level, p.position(), normal, theme);
			}
			if (tick % 10 == 0) {
				for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius),
						e -> Targets.canHelp(cast.caster, e) && e.position().distanceTo(centre) <= radius)) {
					((LivingEntity) e).addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 25, 0, false, true));
				}
			}
			if (tick % 20 == 0) {
				ExpansionVfx.havenShell(level, centre, radius, theme, tick);
			}
			if (tick == ticks - 1) {
				ExpansionVfx.havenClose(level, centre, radius, theme);
			}
			return true;
		});
	}

	// ------------------------------------------------------------------ batch 6: mining and building

	/** The blocks a mining spell of each tier can't break: I stone, II iron, III diamond. */
	private static TagKey<Block> tooHard(int tier) {
		return switch (tier) {
			case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
			case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
			default -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
		};
	}

	/** The pickaxe a mining spell of each tier breaks blocks with, so drops come out as they would for a player holding it. */
	private static ItemStack pickaxe(int tier) {
		return new ItemStack(tier <= 1 ? Items.STONE_PICKAXE : tier == 2 ? Items.IRON_PICKAXE : Items.DIAMOND_PICKAXE);
	}

	private static Consumer<ItemStack> dropAt(ServerLevel level, BlockPos pos) {
		return stack -> Block.popResource(level, pos, stack);
	}

	/**
	 * Mines one block as a player holding {@code tool} would: never an unbreakable block, a fluid or
	 * one in {@code tooHard}, only where the caster may build and within the cast's block budget. It
	 * drops what that tool would (a container spills its contents too), each drop going to
	 * {@code drops}. Returns whether the block was mined.
	 */
	private static boolean mine(Cast cast, BlockPos pos, ItemStack tool, TagKey<Block> tooHard, Consumer<ItemStack> drops) {
		ServerLevel level = cast.level;
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getDestroySpeed(level, pos) < 0 || state.is(tooHard)) {
			return false;
		}
		if (!mayEdit(cast, pos)) {
			return false;
		}
		if (unspan(level, pos)) {
			// A Span's glass just shatters.
			return true;
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		boolean harvest = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
		List<ItemStack> loot = harvest ? Block.getDrops(state, level, pos, blockEntity, cast.caster, tool) : List.of();
		level.destroyBlock(pos, false, cast.caster);
		magicBreak(level, pos);
		if (harvest) {
			state.spawnAfterBreak(level, pos, tool, true);
		}
		loot.forEach(drops);
		return true;
	}

	/** Chisel: one block, at stone-pickaxe strength. */
	private static void chisel(Cast cast, Cast.Hit hit, int tier) {
		if (hit.block() == null) {
			return;
		}
		BlockPos pos = hit.block();
		BlockState state = cast.level.getBlockState(pos);
		if (mine(cast, pos, pickaxe(tier), tooHard(tier), dropAt(cast.level, pos))) {
			ExpansionVfx.chisel(cast.level, pos, state, hit.face());
		}
	}

	/** Tunnel: a walkable passage bored into a wall (or a shaft into a floor or ceiling), a little deeper each tick. */
	private static void tunnel(Cast cast, Cast.Hit hit, int tier) {
		if (hit.block() == null || hit.face() == null) {
			return;
		}
		ServerLevel level = cast.level;
		Direction into = hit.face().getOpposite();
		BlockPos start = hit.block();
		List<BlockPos> cells = new ArrayList<>();
		if (into.getAxis().isHorizontal()) {
			// Two high, starting at the caster's feet when they aim at the block at head height.
			BlockPos base = start.getY() > cast.caster.getBlockY() ? start.below() : start;
			for (int d = 0; d < 4; d++) {
				cells.add(base.relative(into, d));
				cells.add(base.relative(into, d).above());
			}
		} else {
			for (int d = 0; d < 4; d++) {
				cells.add(start.relative(into, d));
			}
		}
		ExpansionVfx.tunnel(level, Vec3.atCenterOf(start), into, Vfx.theme("earth"));
		for (int i = 0; i < cells.size(); i++) {
			BlockPos p = cells.get(i);
			Scheduler.later(1 + i / 2 * 2, () -> {
				if (!cast.alive()) {
					return;
				}
				BlockState state = level.getBlockState(p);
				if (mine(cast, p, pickaxe(tier), tooHard(tier), dropAt(level, p))) {
					ExpansionVfx.bore(level, p, state, into);
				}
			});
		}
	}

	private static boolean isOre(BlockState state) {
		return state.is(BlockTags.ORES) || state.is(ConventionalBlockTags.ORES);
	}

	/** The same ore: the same block, its deepslate twin, or a metal sharing one of vanilla's ore tags. */
	private static boolean sameOre(BlockState a, BlockState b) {
		if (!isOre(b)) {
			return false;
		}
		if (a.getBlock() == b.getBlock()) {
			return true;
		}
		for (TagKey<Block> tag : List.of(BlockTags.IRON_ORES, BlockTags.GOLD_ORES, BlockTags.COPPER_ORES)) {
			if (a.is(tag) && b.is(tag)) {
				return true;
			}
		}
		return oreName(a).equals(oreName(b));
	}

	private static String oreName(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().replace("deepslate_", "");
	}

	/** Vein: the block hit and, for an ore, every matching ore touching it, one after another. */
	private static void vein(Cast cast, Cast.Hit hit, int tier) {
		if (hit.block() == null) {
			return;
		}
		ServerLevel level = cast.level;
		BlockPos start = hit.block();
		BlockState first = level.getBlockState(start);
		List<BlockPos> ores = new ArrayList<>(List.of(start));
		if (isOre(first)) {
			Set<BlockPos> seen = new HashSet<>(ores);
			Deque<BlockPos> queue = new ArrayDeque<>(ores);
			while (!queue.isEmpty() && ores.size() < 16) {
				BlockPos p = queue.poll();
				for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
					BlockPos q = n.immutable();
					if (ores.size() < 16 && seen.add(q) && sameOre(first, level.getBlockState(q))) {
						ores.add(q);
						queue.add(q);
					}
				}
			}
		}
		for (int i = 0; i < ores.size(); i++) {
			BlockPos p = ores.get(i);
			BlockPos from = ores.get(Math.max(0, i - 1));
			Scheduler.later(1 + i, () -> {
				if (!cast.alive()) {
					return;
				}
				BlockState state = level.getBlockState(p);
				if (mine(cast, p, pickaxe(tier), tooHard(tier), dropAt(level, p))) {
					ExpansionVfx.vein(level, from, p, state);
				}
			});
		}
	}

	/** Smelt: mines the block and drops what a furnace would make of it, with the furnace's experience. */
	private static void smelt(Cast cast, Cast.Hit hit, int tier) {
		if (hit.block() == null) {
			return;
		}
		ServerLevel level = cast.level;
		BlockPos pos = hit.block();
		BlockState state = level.getBlockState(pos);
		double[] xp = {0};
		if (!mine(cast, pos, pickaxe(tier), tooHard(tier), stack -> Block.popResource(level, pos, smelted(level, stack, xp)))) {
			return;
		}
		int whole = (int) xp[0];
		if (level.getRandom().nextDouble() < xp[0] - whole) {
			whole++;
		}
		if (whole > 0) {
			ExperienceOrb.award(level, Vec3.atCenterOf(pos), whole);
		}
		ExpansionVfx.smelt(level, pos, state);
	}

	/** What a furnace would make of a stack (or the stack itself), adding the experience it gives to {@code xp}. */
	private static ItemStack smelted(ServerLevel level, ItemStack stack, double[] xp) {
		SingleRecipeInput input = new SingleRecipeInput(stack);
		var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
		if (recipe.isEmpty()) {
			return stack;
		}
		ItemStack out = recipe.get().value().assemble(input);
		if (out.isEmpty()) {
			return stack;
		}
		out.setCount(Math.min(out.getMaxStackSize(), out.getCount() * stack.getCount()));
		xp[0] += recipe.get().value().experience() * stack.getCount();
		return out;
	}

	/**
	 * Fell: the log hit and every log joined to it at its level or above. A log without living
	 * leaves around it is part of a build, not a tree, so only that one comes down.
	 */
	private static void fell(Cast cast, Cast.Hit hit) {
		if (hit.block() == null) {
			return;
		}
		ServerLevel level = cast.level;
		BlockPos start = hit.block();
		if (!level.getBlockState(start).is(BlockTags.LOGS)) {
			return;
		}
		List<BlockPos> logs = new ArrayList<>(List.of(start));
		Set<BlockPos> seen = new HashSet<>(logs);
		boolean tree = false;
		for (int i = 0; i < logs.size(); i++) {
			BlockPos p = logs.get(i);
			for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
				BlockPos q = n.immutable();
				BlockState state = level.getBlockState(q);
				if (state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT)) {
					tree = true;
				}
				if (logs.size() < Cast.MAX_BLOCKS && seen.add(q) && state.is(BlockTags.LOGS)) {
					logs.add(q);
				}
			}
		}
		if (!tree) {
			logs = List.of(start);
		}
		ItemStack axe = new ItemStack(Items.IRON_AXE);
		ExpansionVfx.fellStart(level, start, Vfx.theme("earth"));
		for (int i = 0; i < logs.size(); i++) {
			BlockPos p = logs.get(i);
			Scheduler.later(1 + i / 2, () -> {
				if (!cast.alive()) {
					return;
				}
				BlockState state = level.getBlockState(p);
				if (mine(cast, p, axe, BlockTags.INCORRECT_FOR_IRON_TOOL, dropAt(level, p))) {
					ExpansionVfx.fell(level, p, state);
				}
			});
		}
	}

	/** Glimmer: glow lichen over the face that was hit and the faces around it, nearest first. */
	private static void glimmer(Cast cast, Cast.Hit hit, double radiusScale) {
		ServerLevel level = cast.level;
		if (!Casters.mayBuild(cast.caster)) {
			return;
		}
		BlockPos support = targetBlock(hit);
		Direction face = hit.block() != null && hit.face() != null ? hit.face() : Direction.UP;
		Direction back = face.getOpposite();
		BooleanProperty side = MultifaceBlock.getFaceProperty(back);
		int want = (int) Math.round(5 * radiusScale);
		List<BlockPos> around = new ArrayList<>();
		for (int a = -2; a <= 2; a++) {
			for (int b = -2; b <= 2; b++) {
				around.add(switch (face.getAxis()) {
					case X -> support.offset(0, a, b);
					case Y -> support.offset(a, 0, b);
					case Z -> support.offset(a, b, 0);
				});
			}
		}
		around.sort(Comparator.comparingDouble(p -> p.distSqr(support)));
		List<BlockPos> grown = new ArrayList<>();
		for (BlockPos s : around) {
			if (grown.size() >= want) {
				break;
			}
			BlockPos cell = s.relative(face);
			BlockState there = level.getBlockState(cell);
			boolean lichen = there.is(Blocks.GLOW_LICHEN);
			if (!(there.isAir() || (lichen && !there.getValue(side))) || !MultifaceBlock.canAttachTo(level, back, s, level.getBlockState(s))) {
				continue;
			}
			if (!Casters.mayEdit(cast.caster, level, cell)) {
				continue;
			}
			if (!cast.takeBlock()) {
				break;
			}
			level.setBlockAndUpdate(cell, (lichen ? there : Blocks.GLOW_LICHEN.defaultBlockState()).setValue(side, true));
			grown.add(cell);
		}
		ExpansionVfx.glimmer(level, grown, face);
	}

	private static boolean prunable(BlockState state) {
		if (state.is(BlockTags.LEAVES)) {
			// Leaves placed by hand are part of a build.
			return state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
		}
		if (!state.getFluidState().isEmpty() || state.is(Blocks.GLOW_LICHEN)) {
			return false;
		}
		return state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.SMALL_FLOWERS) || state.is(Blocks.COBWEB);
	}

	/** Prune: leaves, grass, flowers, vines and cobwebs around the point are cleared, nearest first, dropping what they would by hand. */
	private static void prune(Cast cast, Vec3 point, double radius) {
		ServerLevel level = cast.level;
		if (!Casters.mayBuild(cast.caster)) {
			return;
		}
		BlockPos centre = BlockPos.containing(point);
		int r = (int) Math.ceil(radius);
		List<BlockPos> plants = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
			if (pos.distSqr(centre) <= radius * radius && prunable(level.getBlockState(pos))) {
				plants.add(pos.immutable());
			}
		}
		plants.sort(Comparator.comparingDouble(p -> p.distSqr(centre)));
		int cleared = 0;
		for (BlockPos p : plants) {
			// The other half of a tall plant may already be gone.
			if (!prunable(level.getBlockState(p)) || !Casters.mayEdit(cast.caster, level, p)) {
				continue;
			}
			if (!cast.takeBlock()) {
				break;
			}
			level.destroyBlock(p, true, cast.caster);
			cleared++;
		}
		ExpansionVfx.prune(level, point, radius, cleared > 0);
	}

	/** Light's invisible light blocks still lit, so they go out when the server stops. */
	private static final java.util.Set<GlobalPos> LIGHTS = new java.util.HashSet<>();

	/** Whether the block at {@code pos} is only there for a while (a Span's glass, a Rampart's wall): pistons can't move it. */
	public static boolean isTemporary(ServerLevel level, BlockPos pos) {
		return !SPAN.isEmpty() && SPAN.containsKey(GlobalPos.of(level.dimension(), pos)) || Techniques.isRampart(level, pos);
	}

	/** Span bridges still standing, and what each of their blocks replaced. */
	private static final Map<GlobalPos, BlockState> SPAN = new HashMap<>();
	private static final BlockState SPAN_BLOCK = Blocks.STAINED_GLASS.magenta().defaultBlockState();

	/**
	 * Registered when the mod starts (see {@link #init()}): a Span's glass broken by hand drops
	 * nothing and puts back what it replaced, and every bridge still standing is taken down when the
	 * server stops.
	 */
	private static final class SpanRules {
		static {
			PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
				!(world instanceof ServerLevel server) || !state.is(SPAN_BLOCK.getBlock()) || !unspan(server, pos));
			ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
				for (Map.Entry<GlobalPos, BlockState> entry : new ArrayList<>(SPAN.entrySet())) {
					ServerLevel level = server.getLevel(entry.getKey().dimension());
					BlockPos pos = entry.getKey().pos();
					if (level != null && level.getBlockState(pos).is(SPAN_BLOCK.getBlock())) {
						level.setBlockAndUpdate(pos, entry.getValue());
					}
				}
				SPAN.clear();
				for (GlobalPos lit : LIGHTS) {
					ServerLevel level = server.getLevel(lit.dimension());
					if (level != null && level.getBlockState(lit.pos()).is(Blocks.LIGHT)) {
						level.setBlockAndUpdate(lit.pos(), Blocks.AIR.defaultBlockState());
					}
				}
				LIGHTS.clear();
			});
		}

		private SpanRules() {}

		static void ready() {
		}
	}

	/** Takes a Span's glass away (if it stands there), putting back what it replaced. Returns whether it was a Span's. */
	private static boolean unspan(ServerLevel level, BlockPos pos) {
		if (SPAN.isEmpty()) {
			return false;
		}
		BlockState replaced = SPAN.remove(GlobalPos.of(level.dimension(), pos.immutable()));
		if (replaced == null) {
			return false;
		}
		if (level.getBlockState(pos).is(SPAN_BLOCK.getBlock())) {
			level.levelEvent(2001, pos, Block.getId(SPAN_BLOCK));
			level.setBlockAndUpdate(pos, replaced);
		}
		return true;
	}

	/** Span: a bridge of glass grows out from under the caster's feet toward the point, then shatters. */
	private static void span(Cast cast, Cast.Hit hit, double radiusScale, int ticks) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		if (!Casters.mayBuild(caster)) {
			return;
		}
		Vec3 feet = caster.position();
		Vec3 toward = hit.point().subtract(feet);
		double reach = Math.hypot(toward.x, toward.z);
		boolean aimed = !hit.self() && reach >= 2;
		Vec3 dir = horizontal(aimed ? toward : caster.getLookAngle(), caster.getLookAngle());
		double length = aimed ? Math.min(16, reach + 1) : 10;
		Vec3 side = new Vec3(-dir.z, 0, dir.x);
		int half = Math.max(0, (int) Math.round((radiusScale - 1) * 2));
		int y = BlockPos.containing(feet.x, feet.y - 0.2, feet.z).getY();
		Set<BlockPos> cells = new LinkedHashSet<>();
		BlockPos prev = null;
		for (double d = 0.8; d <= length; d += 0.25) {
			Vec3 c = feet.add(dir.scale(d));
			BlockPos p = BlockPos.containing(c.x, y, c.z);
			if (prev != null && p.getX() != prev.getX() && p.getZ() != prev.getZ()) {
				// Never a diagonal gap to fall through.
				cells.add(new BlockPos(prev.getX(), y, p.getZ()));
			}
			for (int w = -half; w <= half; w++) {
				cells.add(BlockPos.containing(c.x + side.x * w, y, c.z + side.z * w));
			}
			prev = p;
		}
		List<BlockPos> order = new ArrayList<>(cells);
		Vfx.Theme theme = Vfx.theme("arcane");
		ExpansionVfx.spanStart(level, feet, dir, theme);
		for (int i = 0; i < order.size(); i++) {
			BlockPos p = order.get(i);
			Scheduler.later(1 + i / 3, () -> {
				if (!cast.alive()) {
					return;
				}
				BlockState state = level.getBlockState(p);
				if (!state.canBeReplaced() || !level.getEntities((Entity) null, new AABB(p), e -> e instanceof LivingEntity).isEmpty()
						|| !Casters.mayEdit(caster, level, p) || !cast.takeBlock()) {
					return;
				}
				SPAN.put(GlobalPos.of(level.dimension(), p.immutable()), state);
				level.setBlockAndUpdate(p, SPAN_BLOCK);
				ExpansionVfx.spanBlock(level, p, theme);
			});
		}
		// It always comes down, even if its caster is gone.
		Scheduler.later(ticks, () -> order.forEach(p -> {
			if (unspan(level, p)) {
				ExpansionVfx.spanShatter(level, p, theme);
			}
		}));
	}

	// ------------------------------------------------------------------ batch 6: damage and control

	private record Hexed(UUID caster, long until) {}

	private static final Map<UUID, Hexed> HEXED = new HashMap<>();
	/** How much harder a hexer's spells hit what they hexed. */
	public static final double HEX_BONUS = 1.25;

	private static void hex(Cast cast, LivingEntity t, int ticks) {
		long now = cast.level.getGameTime();
		HEXED.put(t.getUUID(), new Hexed(cast.caster.getUUID(), now + ticks));
		if (HEXED.size() > 256) {
			HEXED.values().removeIf(h -> h.until() < now);
		}
		ExpansionVfx.hex(cast.level, t, ticks);
	}

	/** Hex: its caster's spells hit the hexed creature harder. */
	private static double hexBonus(Cast cast, LivingEntity target) {
		if (HEXED.isEmpty()) {
			return 1.0;
		}
		Hexed hexed = HEXED.get(target.getUUID());
		if (hexed == null) {
			return 1.0;
		}
		if (hexed.until() < cast.level.getGameTime()) {
			HEXED.remove(target.getUUID());
			return 1.0;
		}
		if (!hexed.caster().equals(cast.caster.getUUID())) {
			return 1.0;
		}
		ExpansionVfx.hexBite(cast.level, target);
		return HEX_BONUS;
	}

	private static final Identifier REND_ID = Wildercord.id("rend");

	/** Rend: less armour for a while. */
	private static void rend(Cast cast, LivingEntity t, int ticks) {
		if (t.getAttribute(Attributes.ARMOR) == null) {
			return;
		}
		modifier(t, Attributes.ARMOR, REND_ID, -4.0, AttributeModifier.Operation.ADD_VALUE);
		ward(cast, t, "rend", ticks, 1.0, 20, w -> { }, () -> unmodify(t, REND_ID, List.of(Attributes.ARMOR)));
		ExpansionVfx.rend(cast.level, t);
	}

	/** Countdown: a mark that ticks twice, then strikes. */
	private static void countdown(Cast cast, LivingEntity t, double power) {
		ExpansionVfx.countdown(cast.level, t, 0);
		for (int beat = 1; beat <= 2; beat++) {
			int b = beat;
			Scheduler.later(beat * 10, () -> {
				if (cast.alive() && t.isAlive()) {
					ExpansionVfx.countdown(cast.level, t, b);
				}
			});
		}
		Scheduler.later(30, () -> {
			if (!cast.alive() || !t.isAlive() || t.level() != cast.level) {
				return;
			}
			ExpansionVfx.countdownStrike(cast.level, t);
			hurt(cast, t, cast.level.damageSources().indirectMagic(cast.caster, cast.caster), 6 * power);
		});
	}

	/** Bleed: a cut, then more damage every half second. */
	private static void bleed(Cast cast, LivingEntity t, double power, int wounds) {
		DamageSource source = cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
		ExpansionVfx.bleed(cast.level, t, true);
		hurt(cast, t, source, 2 * power);
		for (int i = 1; i <= wounds; i++) {
			Scheduler.later(i * 10, () -> {
				if (!cast.alive() || !t.isAlive() || t.level() != cast.level) {
					return;
				}
				ExpansionVfx.bleed(cast.level, t, false);
				hurt(cast, t, source, power);
			});
		}
	}

	/** Every enemy within {@code radius} of the point (by the middle of its body). */
	private static List<LivingEntity> enemiesAround(Cast cast, Vec3 point, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius + 1), e -> Targets.canHarm(cast.caster, e))) {
			if (e.getBoundingBox().getCenter().distanceTo(point) <= radius + e.getBbWidth() / 2) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	/** Coldsnap: frost bites everything around the point, slowing it and leaving it brittle for Shatter. */
	private static void coldsnap(Cast cast, Vec3 point, double radius, double power, double duration) {
		ExpansionVfx.coldsnap(cast.level, point, radius);
		for (LivingEntity t : enemiesAround(cast, point, radius)) {
			hurt(cast, t, cast.level.damageSources().source(DamageTypes.FREEZE, cast.caster), 3 * power);
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(4, duration), 1, false, true));
			Reactions.mark(t, Reactions.Mark.FROZEN, 40);
			ExpansionVfx.chilled(cast.level, t);
		}
	}

	/** Flashfire: a flash of heat that burns everything around the point. */
	private static void flashfire(Cast cast, Vec3 point, double radius, double power) {
		ExpansionVfx.flashfire(cast.level, point, radius);
		for (LivingEntity t : enemiesAround(cast, point, radius)) {
			double react = Reactions.fire(cast, t);
			t.igniteForSeconds(3);
			hurt(cast, t, cast.level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 4 * power * react);
			ExpansionVfx.ember(cast.level, t);
		}
	}

	/** Banish: the target reappears further away from the caster, somewhere it fits and can see back to. Bosses stay put. */
	private static void banish(Cast cast, LivingEntity t, double distance) {
		ServerLevel level = cast.level;
		if (Spirits.isBoss(t)) {
			ExpansionVfx.banishResisted(level, t);
			return;
		}
		Vec3 away = horizontal(t.position().subtract(cast.caster.position()), cast.caster.getLookAngle());
		Vec3 from = t.position();
		for (double d = distance; d >= 2; d -= 1) {
			Vec3 spot = CastEngine.ground(level, from.add(away.scale(d)).add(0, 1.0, 0));
			if (Math.abs(spot.y - from.y) > 4 || !level.noCollision(t, t.getDimensions(t.getPose()).makeBoundingBox(spot))) {
				continue;
			}
			if (level.clip(new ClipContext(from.add(0, 1, 0), spot.add(0, 1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, t)).getType()
					!= HitResult.Type.MISS) {
				continue;
			}
			ExpansionVfx.banish(level, t, from, spot);
			t.teleportTo(level, spot.x, spot.y, spot.z, Set.<Relative>of(), t.getYRot(), t.getXRot(), false);
			t.resetFallDistance();
			if (t instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			return;
		}
		ExpansionVfx.banishResisted(level, t);
	}

	/** Cyclone: enemies around the point are whirled around it for a while, then flung out. Bosses are struck but never moved. */
	private static void cyclone(Cast cast, Vec3 point, double radius, double power, int ticks) {
		ServerLevel level = cast.level;
		Vec3 centre = CastEngine.ground(level, point.add(0, 0.5, 0));
		Set<LivingEntity> caught = new LinkedHashSet<>();
		ExpansionVfx.cycloneRise(level, centre, radius);
		ShapeRunners.each(cast, ticks + 1, tick -> {
			if (tick < ticks) {
				ExpansionVfx.cyclone(level, centre, radius, tick);
				if (tick % 2 != 0) {
					return true;
				}
				for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius, 3.0, radius), e -> Targets.canHarm(cast.caster, e))) {
					LivingEntity v = (LivingEntity) e;
					Vec3 rel = new Vec3(v.getX() - centre.x, 0, v.getZ() - centre.z);
					double d = rel.length();
					if (d > radius + 0.5) {
						continue;
					}
					caught.add(v);
					Reactions.mark(v, Reactions.Mark.WINDSWEPT);
					if (Spirits.isBoss(v)) {
						continue;
					}
					Vec3 around = d < 0.3 ? new Vec3(1, 0, 0) : new Vec3(-rel.z, 0, rel.x).normalize();
					Vec3 in = d < 0.3 ? Vec3.ZERO : rel.normalize().scale(-(d - 1.2) * 0.15);
					double lift = v.getY() - centre.y < 1.5 ? 0.14 : -0.02;
					setMotion(v, around.scale(0.45).add(in).add(0, lift, 0));
					v.resetFallDistance();
				}
				return true;
			}
			ExpansionVfx.cycloneFling(level, centre, radius);
			for (LivingEntity v : caught) {
				if (!v.isAlive() || v.level() != level) {
					continue;
				}
				if (!Spirits.isBoss(v)) {
					Vec3 away = horizontal(v.position().subtract(centre), cast.caster.getLookAngle());
					push(v, away.scale(1.4 * Math.sqrt(power)).add(0, 0.5, 0));
				}
				hurt(cast, v, level.damageSources().source(DamageTypes.WIND_CHARGE, cast.caster), 3 * power);
			}
			return false;
		});
	}
}
