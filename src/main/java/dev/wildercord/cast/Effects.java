package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.mixin.ItemEntityAccessor;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.WorldRules;
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
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.phys.shapes.CollisionContext;

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

	/** Damage to players from other players' spells is scaled down so PvP stays fair: the default for casting.pvp_damage_scale. */
	public static final float PVP_DAMAGE = 0.6F;
	private static final int MAX_STRIKES_PER_HIT = 8;

	public static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit) {
		apply(cast, node, hit, 1.0);
	}

	/** Execute on the effect being applied: extra power against targets under half health (1 = none). */
	private static double executeBonus = 1.0;
	/** The element of the effect being applied, for Unison. */
	private static String currentElement = "";
	/** Trial Key on the effect being applied: extra power against targets at full health (1 = none). */
	private static double openingBonus = 1.0;
	/** Whether the effect being applied is a passive renewing itself: {@link #ticks} caps what it sets (see {@link dev.wildercord.spell.Passives#effectTicks}). */
	private static boolean passiveEffect;
	/** Whose spell is being applied right now (null outside one): a duel undoes only what the opponent's spells did. */
	private static LivingEntity applying;
	/** The cast being applied right now (null outside one): health it restores counts toward the spell's mastery. */
	private static Cast applyingCast;
	/** Opaque identity of the innermost non-spell source scope, retained only while its action is on the stack. */
	private static Object sourceScope;
	/** Thirst on the effect being applied: the share of the damage it deals that heals its caster (0 = none). */
	private static double thirst;

	/** The element of the effect being applied right now (empty outside one): the monsters of the wilds ask, as a spell lands on them. */
	public static String currentElementNow() {
		return currentElement;
	}

	/** Whose spell is being applied right now, or null: harm landing meanwhile is that caster's doing. */
	public static LivingEntity applying() {
		return applying;
	}

	/** The cast being applied right now, or null (see {@link Mastery#healed}). */
	public static Cast applyingCast() {
		return applyingCast;
	}

	/** Identity of the current {@link #withSource} scope, or null outside one. Callers may compare, never replace it. */
	public static Object sourceScope() {
		return sourceScope;
	}

	/**
	 * Attribute non-spell actions (a sword art or a projectile's impact) without borrowing an
	 * unrelated spell's cast. Nested calls and failures always restore their caller's context.
	 */
	public static <T> T withSource(LivingEntity source, java.util.function.Supplier<T> action) {
		LivingEntity outerApplying = applying;
		Cast outerCast = applyingCast;
		Object outerScope = sourceScope;
		applying = source;
		applyingCast = null;
		sourceScope = new Object();
		try {
			return action.get();
		} finally {
			applying = outerApplying;
			applyingCast = outerCast;
			sourceScope = outerScope;
		}
	}

	public static void withSource(LivingEntity source, Runnable action) {
		withSource(source, () -> { action.run(); return null; });
	}

	/** @param groupPower extra power from the shape (Focus on a shape) */
	public static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower) {
		if ((cast.guardedImpact() || cast.hasConsequences()) && !cast.alive()) return;
		if(Runes.innate(node.effect) && cast.caster instanceof ServerPlayer owner && !owner.isCreative()
			&& !dev.wildercord.player.Heart.innate(owner).equals(node.effect.id()))return;
		if(PhysicalMagic.interact(cast,node.effect,hit))return;
		if(hit.block()!=null && cast.level.getBlockEntity(hit.block()) instanceof dev.wildercord.content.RunicHearthEntity hearth && hearth.onSpell(cast,node))return;
		if(hit.block()!=null && cast.level.getBlockEntity(hit.block()) instanceof dev.wildercord.content.dungeons.ExpeditionMechanismEntity mechanism
			&& mechanism.onSpell(cast,node))return;
		double outerBonus = executeBonus;
		String outerElement = currentElement;
		double outerOpening = openingBonus;
		boolean outerPassive = passiveEffect;
		LivingEntity outerApplying = applying;
		Cast outerCast = applyingCast;
		double outerThirst = thirst;
		executeBonus = SpellNumbers.executeBonus(node);
		currentElement = node.effect.element();
		openingBonus = SpellNumbers.trialKeyBonus(node);
		passiveEffect = cast.passive;
		applying = cast.caster;
		applyingCast = cast;
		thirst = SpellNumbers.thirstShare(node);
		try {
			applyEffect(cast, node, hit, groupPower);
			// A Relay's inherited hooks are still part of its original paid effect. Keep their
			// mastery, damage and residue callbacks in this exact scope, including on exceptions.
			if (cast.guardedImpact()) afterEffect(cast, node, hit, groupPower);
		} finally {
			executeBonus = outerBonus;
			currentElement = outerElement;
			openingBonus = outerOpening;
			passiveEffect = outerPassive;
			applying = outerApplying;
			applyingCast = outerCast;
			thirst = outerThirst;
		}
		// Ordinary spells retain their existing post-effect outer context.
		if (!cast.guardedImpact()) afterEffect(cast, node, hit, groupPower);
	}

	private static boolean lostConsequence(Cast cast, Cast.Hit hit) {
		return cast.hasConsequences() && (!cast.alive() || hit.entities().stream()
			.anyMatch(entity -> entity instanceof LivingEntity living && !cast.consequencesValid(living)));
	}

	private static void afterEffect(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower) {
		if (lostConsequence(cast, hit) || cast.guardedImpact() && (!cast.alive() || hit.entities().stream().anyMatch(e -> !cast.admits(e)))) return;
		RuneSeals.onSpell(cast, hit, node.effect.element());
		if (lostConsequence(cast, hit) || cast.guardedImpact() && (!cast.alive() || hit.entities().stream().anyMatch(e -> !cast.admits(e)))) return;
		WorldQuirks.after(cast, node, hit, groupPower);
		if (lostConsequence(cast, hit) || cast.guardedImpact() && (!cast.alive() || hit.entities().stream().anyMatch(e -> !cast.admits(e)))) return;
		WorldMagic.onSpell(cast, node, hit, groupPower);
		// Strong magic leaves a lasting mark of its element where it lands.
		if (lostConsequence(cast, hit) || cast.guardedImpact() && (!cast.alive() || hit.entities().stream().anyMatch(e -> !cast.admits(e)))) return;
		Residues.onSpell(cast, node, hit);
		if (lostConsequence(cast, hit) || cast.guardedImpact() && (!cast.alive() || hit.entities().stream().anyMatch(e -> !cast.admits(e)))) return;
		dev.wildercord.cast.events.WorldEvents.onSpell(cast, hit, node.effect.element());
		if (lostConsequence(cast, hit) || cast.guardedImpact() && (!cast.alive() || hit.entities().stream().anyMatch(e -> !cast.admits(e)))) return;
		dev.wildercord.familiar.Familiars.onSpell(cast, hit, node.effect.element());
		if (lostConsequence(cast, hit) || cast.guardedImpact() && (!cast.alive() || hit.entities().stream().anyMatch(e -> !cast.admits(e)))) return;
		Dungeons.onSpell(cast, hit, node.effect.element());
		// Monsters that answer magic: a Gloomstalker shown by light, a harpy dragged down by earth.
		if (lostConsequence(cast, hit) || cast.guardedImpact() && (!cast.alive() || hit.entities().stream().anyMatch(e -> !cast.admits(e)))) return;
		dev.wildercord.monster.Monsters.onSpell(cast, hit, node.effect);
	}

	/**
	 * Wraps a task scheduled while an effect is being applied, so the damage it deals later (a
	 * meteor landing, a countdown going off, the next slash) still gets that effect's Execute and
	 * Unison.
	 */
	static Runnable carryContext(Runnable task) {
		if (executeBonus == 1.0 && openingBonus == 1.0 && currentElement.isEmpty() && thirst == 0
				&& applying == null && applyingCast == null) {
			return task;
		}
		double bonus = executeBonus;
		double opening = openingBonus;
		String element = currentElement;
		double drinks = thirst;
		LivingEntity source = applying;
		Cast sourceCast = applyingCast;
		return () -> {
			double outerBonus = executeBonus;
			double outerOpening = openingBonus;
			String outerElement = currentElement;
			double outerThirst = thirst;
			LivingEntity outerApplying = applying;
			Cast outerCast = applyingCast;
			executeBonus = bonus;
			openingBonus = opening;
			currentElement = element;
			thirst = drinks;
			applying = source;
			applyingCast = sourceCast;
			try {
				task.run();
			} finally {
				executeBonus = outerBonus;
				openingBonus = outerOpening;
				currentElement = outerElement;
				thirst = outerThirst;
				applying = outerApplying;
				applyingCast = outerCast;
			}
		};
	}

	/**
	 * Runs {@code task} as damage of {@code element} alone, without the Execute, Trial Key or element of
	 * whatever effect is being applied right now: for damage dealt in answer to someone else's spell
	 * (Stoneform's aftershock answering a blow), which would otherwise borrow that spell's, and set off
	 * that element's reactions for the wrong caster.
	 */
	static void asElement(String element, Runnable task) {
		double outerBonus = executeBonus;
		double outerOpening = openingBonus;
		String outerElement = currentElement;
		double outerThirst = thirst;
		executeBonus = 1.0;
		openingBonus = 1.0;
		currentElement = element;
		thirst = 0;
		try {
			task.run();
		} finally {
			executeBonus = outerBonus;
			openingBonus = outerOpening;
			currentElement = outerElement;
			thirst = outerThirst;
		}
	}

	private static void applyEffect(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower) {
		RuneDef rune = node.effect;
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		// The caster's affinity with this element (+3% a level: heals, shields and pushes grow with it too). Innate runes grow with the heart.
		double affinity = PlayerAffinities.power(cast, rune.element());
		double innate = Runes.innate(rune) ? Innates.scale(caster) : 1.0;
		// A rune ranked up at the Fusion Altar hits harder wherever it's threaded; rank III counts as one Amplify for levels.
		int rank = dev.wildercord.player.RuneRanks.rank(caster, rune.id());
		// Casting gear (a staff of this element, a Focus of Thrift): its own factor, set when the spell was cast.
		double gear = cast.gearPower(rune.element());
		double power = SpellNumbers.power(node) * groupPower * cast.power * affinity * innate * dev.wildercord.spell.Ranks.power(rank) * gear
			* ExplorerEffects.swing(cast, node, hit) * WorldQuirks.power(cast, rune, hit);
		// This world's quirks may make it stronger or last longer where it lands (see WorldQuirks).
		double duration = SpellNumbers.duration(node) * cast.duration * WorldQuirks.duration(cast, rune, hit);
		int amplify = node.count(Runes.AMPLIFY) + dev.wildercord.spell.Ranks.levels(rank);
		List<LivingEntity> helped = filter(hit.entities(), e -> Targets.canHelp(caster, e));
		List<LivingEntity> harmed = filter(hit.entities(), e -> Targets.canHarm(caster, e) && cast.admits(e));
		if (!harmed.isEmpty() && (rune.kind() == dev.wildercord.spell.EffectKind.HARMFUL || rune.kind() == dev.wildercord.spell.EffectKind.MOVEMENT && !hit.self())) {
			// A Shield stops a spell that costs no more than the one that raised it; a costlier one breaks it and goes through.
			harmed = Shields.screen(cast, harmed, hit);
		}
		// Self always means you: movement effects move you even though they are "harmful" to others.
		List<LivingEntity> moved = hit.self() ? List.of(caster) : harmed;
		List<LivingEntity> targetsHit = harmed;
		RunicAnimations.land(cast, rune, hit);
		if (PhysicalMagic.apply(cast,rune,hit,power,duration)) return;
		if (FieldFusions.apply(cast,node,hit,helped,harmed)) return;
		if (CounterSignatures.apply(cast,node,helped,harmed)) return;
		if (SupportSignatures.apply(cast,node,hit,helped)) return;
		if (CampConcordMagic.apply(cast,node,hit,helped)) return;
		if (TrailSignatures.apply(cast,node,hit)) return;

		// Wildercord's own runes by name; an add-on's (another namespace) never, even one called example:bleed.
		switch (builtIn(rune) ? rune.path() : "") {
			case "feather_fall" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks(12, duration), 0, false, true));
				t.resetFallDistance();
				Vfx.featherFall(level, t);
				if (!cast.passive) {
					featherglide(cast, t, ticks(12, duration));
				}
			});
			case "soar" -> Soar.lift(cast, node, helped, SoarRules.flightTicks(duration));
			case "swift" -> helped.forEach(t -> {
				shakeOffCold(t);
				t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks(10, duration), Math.min(4, 2 + amplify), false, true));
				Vfx.swift(level, t);
			});
			case "night_eye" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks(60, duration), 0, false, true));
				Vfx.nightEye(level, t);
			});
			case "heal" -> helped.forEach(t -> {
				// Repeats inside one cast (a Zone's pulses, Linger, Echo) heal 100%, then 60%, then 40% of it: a heal over time is its own runes' job.
				double share = cast.once("heal0:" + t.getUUID()) ? 1.0 : cast.once("heal1:" + t.getUUID()) ? 0.6 : 0.4;
				var observedHeal=LifeOwnerEvents.before(t);
				float before=t.getHealth();
				t.heal((float)(8*power*share));
				// Whatever the heal could not use becomes a shield of up to 2 hearts that fades in 10 s (never stacking past that).
				float over = (float) (8 * power * share) - (t.getHealth() - before);
				if (over >= 0.5F && t.getAbsorptionAmount() < HEAL_SHIELD_MAX) {
					float keep = Math.max(t.getAbsorptionAmount(), Math.min(HEAL_SHIELD_MAX, over));
					t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, true));
					t.setAbsorptionAmount(keep);
				}
				LifeOwnerEvents.changed(cast,"heal",t,observedHeal,null,LifeOwnerEvents.Moment.APPLY);
			});
			case "shield" -> {
				int ticks = (int) Math.round(SpellNumbers.shieldTicks(node) * cast.duration);
				helped.forEach(t -> Shields.raise(cast, t, ticks));
			}
			case "harm" -> harmed.forEach(t -> {
				hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 7 * power);
				if (!cast.admits(t)) return;
				Exposed.mark(t, Exposed.HARM_TICKS);
				Vfx.harm(level, t);
			});
			case "push" -> harmed.forEach(t -> {
				Vec3 away = horizontal(t.position().subtract(hit.origin()), hit.dir());
				Statuses.windPush(t, away.scale(2.2 * power).add(0, 0.45, 0));
				Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				Vfx.push(level, t, away);
				wallSlam(cast, t, power);
			});
			case "pull" -> harmed.forEach(t -> {
				Vec3 towards = hit.origin().subtract(t.position());
				double distance = towards.length();
				Vfx.pull(level, t, hit.origin());
				// Left staggered where it lands, and marked long enough to follow with a blast.
				Reactions.mark(t, Reactions.Mark.PULLED, 80);
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2, false, false));
				push(t, towards.normalize().scale(Math.min(2.6, 0.5 + distance * 0.22) * power).add(0, 0.3, 0));
			});
			case "launch" -> moved.forEach(t -> {
				Vec3 v = t.getDeltaMovement();
				Vec3 kick = new Vec3(0, 1.5 * power, 0);
				if (t == caster) {
					kick = kick.add(horizontal(caster.getLookAngle(), caster.getLookAngle()).scale(0.7));
					caster.resetFallDistance();
				}
				Vec3 lift = new Vec3(v.x, Math.max(0, v.y), v.z).add(kick).subtract(v);
				if (t == caster) {
					push(t, lift);
				} else {
					Statuses.windPush(t, lift);
					Reactions.mark(t, Reactions.Mark.WINDSWEPT);
					Statuses.airborne(t, AIRBORNE_LAUNCH_TICKS);
				}
				Vfx.launch(level, t);
			});
			case "dash" -> moved.forEach(t -> {
				Vec3 flat = horizontal(caster.getLookAngle(), caster.getLookAngle());
				Vec3 dir = flat.add(0, 0.12, 0).normalize();
				if (t == caster) {
					dashSelf(cast, caster, flat, power);
				} else {
					Statuses.windPush(t, flat.scale(DASH_SHOVE * power).add(0, 0.2, 0));
					Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				}
				Vfx.dash(level, t, dir);
			});
			case "fire" -> {
    dev.wildercord.wildlife.EmberContent.affectFern(cast,targetBlock(hit),"fire");
    harmed.forEach(t -> {
				double react = Reactions.fire(cast, t);
				if (!cast.admitsConsequence(t)) return;
				t.igniteForSeconds((float) (6 * duration));
				hurt(cast, t, level.damageSources().source(DamageTypes.IN_FIRE, caster), 5 * power * react);
				if (!cast.consequencesValid(t)) return;
				FireBloodVfx.fire(level, t);
			});
   }
			case "frost" -> {
    dev.wildercord.wildlife.EmberContent.affectFern(cast,targetBlock(hit),"water");
    harmed.forEach(t -> {
    int priorFrost=t.getTicksFrozen();
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 5 * power);
				if (!cast.admits(t)) return;
				boolean freshSlow=t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(4, duration), 2, false, true));
				if (!cast.admits(t)) return;
				t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + ticks(4, duration)));
				if (!cast.admits(t)) return;
				Reactions.mark(t, Reactions.Mark.FROZEN);
				if(freshSlow || t.getTicksFrozen()>priorFrost)dev.wildercord.wildlife.EmberContent.cool(cast,t);
				Vfx.frost(level, t);
			});
   }
			case "lightning" -> {
				List<Vec3> strikes = new ArrayList<>();
				harmed.forEach(t -> strikes.add(t.position()));
				if (strikes.isEmpty()) {
					strikes.add(hit.point());
				}
				// Set alight once every strike has landed: fire from one strike would let the next set off Overload again.
				Set<LivingEntity> struck = new LinkedHashSet<>();
				for (int i = 0; i < Math.min(MAX_STRIKES_PER_HIT, strikes.size()); i++) {
					lightning(cast, strikes.get(i), power, struck, new HashSet<>(harmed), i < 3);
				}
				struck.forEach(t -> t.igniteForSeconds(4));
			}
			case "explode" -> {
				double radius = SpellNumbers.explodeRadius(node);
				List<Vec3> blasts = new ArrayList<>();
				harmed.forEach(t -> blasts.add(t.getBoundingBox().getCenter()));
				if (blasts.isEmpty()) {
					blasts.add(hit.point());
				}
				// One blast per cluster of enemies, and an enemy takes only the strongest blast that reaches it.
				Map<UUID, Double> landed = new HashMap<>();
				for (Vec3 at : clusterCentres(blasts, radius * BLAST_MERGE, MAX_BLASTS)) {
					explode(cast, at, radius, power, landed);
				}
			}
			case "sonic_boom" -> harmed.forEach(t -> {
				Vfx.sonicBoom(level, hit.origin(), t.getBoundingBox().getCenter());
				hurt(cast, t, level.damageSources().sonicBoom(caster), 16 * power);
				sonicLine(cast, hit.origin(), t, targetsHit, level.damageSources().sonicBoom(caster), 8 * power);
			});
			case "wither" -> harmed.forEach(t -> {
				// Wither IV for 6 s: the rot spreads to whoever strikes it and the wound will not close.
				t.addEffect(new MobEffectInstance(MobEffects.WITHER, ticks(6, duration), 3, false, true), caster);
				Reactions.mark(t, Reactions.Mark.SHADOWED, ticks(6, duration));
				CraftedRunes.noHeal(t, ticks(6, duration));
				VoidTime.withered(t, ticks(6, duration));
				Vfx.wither(level, t);
			});
			case "dragon_breath" -> {
				// A breath laid again by a repeating shape on the same place does not stack.
				if (VoidTime.onceAt(cast, "dragon_breath", hit.point(), (int) Math.round(100 * duration) - 5)) {
					dragonBreath(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power, duration, hit.self() ? caster.getLookAngle() : hit.dir());
				}
			}
			case "shock" -> harmed.forEach(t -> shock(cast, t, power));
			case "haste" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks(30, duration), Math.min(3, 1 + amplify), false, true));
				Vfx.haste(level, t);
			});
			case "reveal" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks(15, duration), 0, false, false));
				t.removeEffect(MobEffects.INVISIBILITY);
				Exposed.mark(t, ticks(15, duration));
				LifeArcaneFx.reveal(level, t, ticks(15, duration));
			});
			case "regrowth" -> helped.forEach(t -> {
				if (passiveEffect) {
					if(t.addEffect(new MobEffectInstance(MobEffects.REGENERATION,ticks(8,duration),Math.min(3,1+amplify),false,true)))LifeOwnerEvents.admitted(cast,"regrowth",t,LifeOwnerEvents.Moment.RENEW,Math.min(3,1+amplify)+1,null);
				} else {
					// The vines take hold: Regeneration I for 3 s, II for 3, III for 2 (about 7 health in all).
					int[] length = {ticks(3, duration), ticks(3, duration), ticks(2, duration)};
					int at = 0;
					for (int stage = 0; stage < 3; stage++) {
						int level2 = Math.min(3, stage + amplify);
						int span = length[stage];
						if(stage==0){
							if(t.addEffect(new MobEffectInstance(MobEffects.REGENERATION,span,level2,false,true)))LifeOwnerEvents.admitted(cast,"regrowth",t,LifeOwnerEvents.Moment.PULSE,level2+1,null);
						} else {
							Scheduler.later(at, () -> {
								if (t.isAlive() && t.level() == level) {
									if(t.addEffect(new MobEffectInstance(MobEffects.REGENERATION,span,level2,false,true)))LifeOwnerEvents.admitted(cast,"regrowth",t,LifeOwnerEvents.Moment.PULSE,level2+1,null);
								}
							});
						}
						at += span;
					}
				}
			});
			case "cleanse" -> helped.forEach(t -> {
				var observedCleanse=LifeOwnerEvents.before(t);
				List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> bad = new ArrayList<>();
				for (MobEffectInstance effect : t.getActiveEffects()) {
					if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
						bad.add(effect.getEffect());
					}
				}
				bad.forEach(t::removeEffect);
				t.clearFire();
				t.setTicksFrozen(0);
				// It washes the elemental marks off too, so a reaction can't be set off on someone just cleansed.
				for (Reactions.Mark mark : Reactions.Mark.values()) {
					Reactions.clear(t, mark);
				}
				LifeOwnerEvents.changed(cast,"cleanse",t,observedCleanse,null,LifeOwnerEvents.Moment.APPLY);
			});
			case "stoneskin" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks(10, duration), Math.min(3, 1 + amplify), false, true));
				// Stone is heavy: the price of the best long ward is a step slower.
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(10, duration), 0, false, false));
				Vfx.stoneskin(level, t);
				if (!cast.passive) {
					StormEarthFx.stoneskinHold(level, t, ticks(10, duration));
				}
			});
			case "root" -> harmed.forEach(t -> {
				boolean rooted = t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(3, duration), 6, false, false));
				if (rooted && t instanceof ServerPlayer player) dev.wildercord.aura.MasterForms.cancel(player);
				t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
				Vfx.root(level, t, ticks(3, duration));
				StormEarthFx.rootHold(level, t, ticks(3, duration));
			});
			case "veil" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks(12, duration), 0, false, true));
				VoidTime.veiled(t, ticks(12, duration));
				for (Entity e : level.getEntities(t, t.getBoundingBox().inflate(16.0), e -> e instanceof net.minecraft.world.entity.Mob)) {
					net.minecraft.world.entity.Mob mob = (net.minecraft.world.entity.Mob) e;
					if (mob.getTarget() == t) {
						mob.setTarget(null);
					}
				}
				Vfx.veil(level, t);
				TimeFx.endingLater(level, t, ticks(12, duration), ElementFx.VOID.secondary(), "void_step_tick", 0.6F);
			});
			case "empower" -> helped.forEach(t -> {
				int strength = ticks(10, duration);
				// A passive carries only Strength I; a cast's borrowed strength is paid back as a comedown when it runs out.
				t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, strength, passiveEffect ? 0 : Math.min(3, 1 + amplify), false, true));
				if (!passiveEffect) {
					Scheduler.later(strength, () -> {
						if (t.isAlive() && t.level() == level) {
							t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EMPOWER_COMEDOWN, 0, false, true));
							LifeArcaneFx.comedown(level, t);
						}
					});
				}
				Vfx.empower(level, t);
			});
			case "levitate" -> moved.forEach(t -> {
				// Bosses are only ever slowed: lifted out of reach, a fight could be won by the fall.
				boolean boss = t != caster && Spirits.isBoss(t);
				t.addEffect(new MobEffectInstance(boss ? MobEffects.SLOWNESS : MobEffects.LEVITATION, ticks(3, duration), boss ? 1 : 0, false, true));
				if (t != caster) {
					Reactions.mark(t, Reactions.Mark.WINDSWEPT, ticks(3, duration) + 20);
					if (!boss) {
						// Suspended: it hangs where it was lifted, and every spell hits it harder while it's off the ground.
						Statuses.airborne(t, ticks(3, duration) + 10);
						suspend(cast, t, ticks(3, duration));
					}
				}
				Vfx.levitate(level, t);
			});
			case "freeze" -> {
    dev.wildercord.wildlife.EmberContent.affectFern(cast,targetBlock(hit),"water");
    harmed.forEach(t -> {
    int priorFreeze=t.getTicksFrozen();
				Spirits.freeze(t, ticks(2.5, duration));
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 3 * power);
				if(t.getTicksFrozen()>priorFreeze)dev.wildercord.wildlife.EmberContent.cool(cast,t);
				Vfx.freeze(level, t);
			});
   }
			case "meteor" -> {
				List<Vec3> targets = new ArrayList<>();
				harmed.forEach(t -> targets.add(t.position()));
				if (targets.isEmpty()) {
					targets.add(hit.point());
				}
				double radius = METEOR_RADIUS * SpellNumbers.effectRadius(node);
				Map<UUID, Double> landed = new HashMap<>();
				for (Vec3 at : clusterCentres(targets, radius * BLAST_MERGE, MAX_METEORS)) {
					meteor(cast, at, radius, power, landed);
				}
			}
			case "tremor" -> tremor(cast, hit.point(), 4.0 * SpellNumbers.effectRadius(node), power);
			case "gravity_well" -> {
				if (VoidTime.onceAt(cast, "gravity_well", hit.point(), (int) Math.round(40 * duration))) {
					gravityWell(cast, hit.point(), 7.0 * SpellNumbers.effectRadius(node), power, duration);
				}
			}
			case "summon" -> Spirits.summonWolves(cast, caster.position(), 3, power, duration);
			case "venom" -> harmed.forEach(t -> {
				// Poison I stays as the marker (cures, Blight, Elapse); the damage is the venom's own, so undead and spiders feel it and it can kill.
				int seconds = (int) Math.max(1, Math.round(VENOM_SECONDS * duration));
				t.addEffect(new MobEffectInstance(MobEffects.POISON, seconds * 20, Math.min(3, amplify), false, true), caster);
				LifeOwnerEvents.mutation(cast,"venom",t,LifeOwnerEvents.Moment.APPLY,null,()->hurt(cast,t,level.damageSources().indirectMagic(caster,caster),2*power));
				venomDot(cast, t, power, seconds);
				if (cast.once("venom-spread:" + t.getUUID())) {
					venomSpread(cast, t, power, seconds);
				}
			});
			case "smite" -> harmed.forEach(t -> {
				// A verdict, not a flick: a ring closes at its feet for 0.7 s (it can step out), then the column falls on where it stands.
				Sigils.target(level, t.position(), 0xFFF0B0, 1.6F, SMITE_DELAY);
				LifeArcaneFx.smiteWindUp(level, t);
				Scheduler.later(SMITE_DELAY, carryContext(() -> {
					if (!cast.alive() || !t.isAlive() || t.level() != level || !Targets.canHarm(caster, t)) {
						return;
					}
					double undead = t.isInvertedHealAndHarm() ? 2.0 : 1.0;
					t.setAbsorptionAmount(0);
					hurt(cast, t, level.damageSources().indirectMagic(caster, caster), SMITE_DAMAGE * power * undead);
					Vfx.smite(level, t);
				}));
			});
			case "inferno" -> inferno(cast, hit.point(), 4.0 * SpellNumbers.effectRadius(node), power, duration);
			case "thunderclap" -> thunderclap(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power);
			case "starfall" -> starfall(cast, hit.point(), 4.0 * SpellNumbers.effectRadius(node), power);
			case "blind" -> harmed.forEach(t -> {
				// Players are blacked out for 3 s, not 5; a monster is blind for 5 and lashes out at what stands next to it.
				int dark = t instanceof Player ? ticks(3, duration) : ticks(5, duration);
				t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dark, 0, false, true));
				t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, dark, 0, false, true));
				if (t instanceof net.minecraft.world.entity.Mob mob) {
					VoidTime.lashOut(cast, mob, dark);
				}
				Reactions.mark(t, Reactions.Mark.SHADOWED, dark);
				Vfx.blind(level, t);
			});
			case "chill" -> harmed.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks(6, duration), chillLevel(t, ticks(6, duration)), false, true));
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), 1 * power);
				Reactions.mark(t, Reactions.Mark.FROZEN, 40);
				Vfx.chill(level, t);
			});
			case "silence" -> harmed.forEach(t -> {
				if (t instanceof net.minecraft.world.entity.Mob mob) {
					mob.setTarget(null);
				}
				t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks(6, duration), 0, false, true));
				// What it says: a caster can't cast for a few seconds (a player 3, a monster 4), and a cast in hand is cut short.
				CastLock.lock(t, ticks(t instanceof Player ? 3 : 4, duration));
				Vfx.silence(level, t);
			});
			case "fireward" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks(30, duration), 0, false, true));
				t.clearFire();
				FireBloodVfx.ward(level, t);
			});
			case "nourish" -> helped.forEach(t -> {
				var observedNourish=LifeOwnerEvents.before(t);
				if (t instanceof Player player) {
					player.getFoodData().eat((int) Math.round(6 * power), 0.6F);
					player.removeEffect(MobEffects.HUNGER);
				} else if (t instanceof net.minecraft.world.entity.animal.Animal animal) {
					// A pet is fed too: it heals, and a grown one is ready to breed.
					animal.heal((float) (6 * power));
					if (animal.getAge() == 0 && caster instanceof ServerPlayer feeder) {
						boolean alreadyInLove=animal.isInLove();animal.setInLove(feeder);
						if(!alreadyInLove&&animal.isInLove())LifeOwnerEvents.admitted(cast,"nourish",animal,LifeOwnerEvents.Moment.APPLY,1,null);
					}
				}
				LifeOwnerEvents.changed(cast,"nourish",t,observedNourish,null,LifeOwnerEvents.Moment.APPLY);
			});
			case "tidebreath" -> {
    dev.wildercord.wildlife.EmberContent.affectFern(cast,targetBlock(hit),"water");
    helped.forEach(t -> {
    boolean priorBurn=t.isOnFire();
				boolean freshBreath=t.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, ticks(30, duration), 0, false, true));
				boolean freshGrace=t.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, ticks(30, duration), 0, false, true));
				// It douses: the fire on them goes out (and fire hits are dulled while they drip).
				t.clearFire();
				if(freshBreath || freshGrace || priorBurn && !t.isOnFire())dev.wildercord.wildlife.EmberContent.cool(cast,t);
    if((freshBreath || freshGrace || priorBurn && !t.isOnFire()) && t instanceof dev.wildercord.wildlife.SiltcrestBittern bird)bird.answerWater(cast);
				Vfx.tidebreath(level, t);
			});
   }
			case "leap" -> helped.forEach(t -> {
				t.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks(15, duration), Math.min(4, 2 + amplify), false, true));
				Vfx.leap(level, t);
			});
			case "grapple" -> grapple(cast, hit, power);
			case "root_carry" -> dev.wildercord.wildlife.RootCarry.apply(cast, hit);
			case "harvest" -> harvest(cast, hit, SpellNumbers.effectRadius(node));
			case "basinfill" -> Basinfill.fill(cast, hit);
			case "icepath" -> {
				if (hit.self()) {
					icepathStrip(cast, 1.5 * SpellNumbers.effectRadius(node));
				} else {
					icepath(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node));
				}
			}
			case "collect" -> collect(cast, hit.point(), 8.0 * SpellNumbers.effectRadius(node));
			case "excavate" -> excavate(cast, hit, amplify > 0);
			case "blink" -> blink(cast, hit);
			case "light" -> light(cast, hit, duration);
			case "grow" -> grow(cast, hit, power);
			case "break" -> breakBlock(cast, hit, amplify > 0);
			case "cleave" -> {
				Set<UUID> struck = new HashSet<>();
				harmed.forEach(t -> struck.add(t.getUUID()));
				int[] budget = {3};
				harmed.forEach(t -> Techniques.cleave(cast, t, power, struck, budget));
			}
			case "dismantle" -> harmed.forEach(t -> Techniques.dismantle(cast, t, power));
			case "blackspark" -> harmed.forEach(t -> Techniques.blackspark(cast, t, power));
			case "aftershock" -> harmed.forEach(t -> Techniques.aftershock(cast, t, power));
			case "resonance" -> harmed.forEach(t -> Techniques.resonance(cast, t, power, ticks(10, duration)));
			case "ripple" -> harmed.forEach(t -> Techniques.ripple(cast, t, power));
			case "primer" -> {
				Map<UUID, Double> landed = new HashMap<>();
				for (int i = 0; i < Math.min(MAX_STRIKES_PER_HIT, harmed.size()); i++) {
					Techniques.primer(cast, harmed.get(i), 3.0 * SpellNumbers.effectRadius(node), power, landed);
				}
			}
			case "blackflame" -> harmed.forEach(t -> Techniques.blackflame(cast, t, power, (int) Math.round(6 * duration), true));
			case "hollow" -> Techniques.hollow(cast, hit, harmed, 4.0 * SpellNumbers.effectRadius(node), power);
			case "repel" -> Techniques.repel(cast, hit, 3.0 * SpellNumbers.effectRadius(node), power);
			case "decree" -> Techniques.decree(cast, harmed, ticks(2, duration));
			case "weigh" -> harmed.forEach(t -> Techniques.weigh(cast, t, ticks(5, duration)));
			case "shackle" -> harmed.forEach(t -> Techniques.shackle(cast, t, ticks(5, duration)));
			case "bubble" -> harmed.forEach(t -> Techniques.bubble(cast, t, ticks(bubbleSeconds(t), duration), power));
			case "infinity" -> helped.forEach(t -> Wards.infinity(cast, t, ticks(6, duration)));
			case "reversal" -> helped.forEach(t -> Wards.reversal(cast, t, ticks(30, duration)));
			case "reflect" -> helped.forEach(t -> Wards.reflect(cast, t, ticks(10, duration), passiveEffect ? Math.min(0.3, 0.6 * power) : Math.min(1.5, 0.6 * power)));
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
			case "thunderbird" -> Techniques.thunderbird(cast, power, ticks(12, duration));
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
				boolean stoked = t.isOnFire();
				feedOrIgnite(t, (int) Math.round(60 * duration));
				hurt(cast, t, level.damageSources().source(DamageTypes.IN_FIRE, caster), 3 * power * react);
				FireBloodVfx.ember(level, t, stoked);
			});
			case "icicle" -> harmed.forEach(t -> {
				boolean slowed = t.hasEffect(MobEffects.SLOWNESS) || Reactions.has(t, Reactions.Mark.FROZEN);
				ExpansionVfx.icicle(level, t, slowed);
				hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, caster), (slowed ? 6 : 4) * power);
				// A touch of frost on the skin, well short of frozen solid.
				t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() - 1, t.getTicksFrozen() + 40));
				melt(cast, t);
			});
			case "pelt" -> harmed.forEach(t -> {
				Vec3 away = horizontal(t.position().subtract(hit.origin()), hit.dir());
				ExpansionVfx.pelt(level, t, away);
				hurt(cast, t, level.damageSources().source(DamageTypes.FALLING_BLOCK, caster), 4 * power);
				push(t, away.scale(1.1 * Math.sqrt(power)).add(0, 0.25, 0));
			});
			case "windcut" -> harmed.forEach(t -> {
				Vec3 away = horizontal(t.position().subtract(hit.origin()), hit.dir());
				hurt(cast, t, level.damageSources().source(DamageTypes.WIND_CHARGE, caster), 4 * power);
				Statuses.windPush(t, away.scale(0.7).add(0, 0.2, 0));
				Reactions.mark(t, Reactions.Mark.WINDSWEPT);
				ExpansionVfx.windcut(level, t, away);
				// The cut breaks what it was winding up: a charge, a draw, a fuse, a telegraphed spell.
				if (t.isAlive()) {
					Statuses.interrupt(t);
				}
			});
			case "leech" -> harmed.forEach(t -> {
				float before = t.getHealth();
				hurt(cast, t, level.damageSources().indirectMagic(caster, caster), 3 * power);
				float taken = Math.max(0.0F, before - t.getHealth());
				boolean shielded = false;
				if (taken > 0 && caster.isAlive()) {
					float room = caster.getMaxHealth() - caster.getHealth();
					caster.heal(taken);
					// What a full heart can't take becomes a shield (up to 4).
					float over = taken - room;
					if (over > 0.25F) {
						shielded = true;
						float shield = Math.min(4.0F, caster.getAbsorptionAmount() + over);
						caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, true));
						caster.setAbsorptionAmount(shield);
					}
				}
				FireBloodVfx.leech(level, t, caster, shielded);
			});
			case "hex" -> harmed.forEach(t -> {
				hex(cast, t, ticks(6, duration));
				// The price of the curse: the hexed creature fixes on whoever hexed it.
				if (t instanceof net.minecraft.world.entity.Mob mob) {
					VoidTime.fixate(cast, mob, ticks(6, duration));
				}
			});
			case "rend" -> harmed.forEach(t -> rend(cast, t, ticks(10, duration)));
			case "countdown" -> harmed.forEach(t -> countdown(cast, t, power));
			case "jolt" -> harmed.forEach(t -> {
				ExpansionVfx.jolt(level, t);
				StormEarthFx.stunRing(level, t, ticks(1, duration));
				hurt(cast, t, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), 4 * power * Reactions.storm(cast, t));
				Spirits.hold(t, ticks(1, duration));
				// The counter-spell: a caster caught mid-charge loses the spell.
				if (t instanceof ServerPlayer charging && charging.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE)) {
					Charging.forget(charging);
					Casters.tell(charging, Component.translatable("message.wildercord.interrupted"));
				}
			});
			case "bleed" -> harmed.forEach(t -> bleed(cast, t, power, (int) Math.round(8 * duration)));
			case "coldsnap" -> coldsnap(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power, duration);
			case "flashfire" -> flashfire(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power);
			case "banish" -> harmed.forEach(t -> banish(cast, t, Math.min(16.0, 8.0 * power)));
			case "cyclone" -> cyclone(cast, hit.point(), 3.0 * SpellNumbers.effectRadius(node), power, ticks(2, duration));
			// New runes (batch 2): their own class.
			case "spellbrand", "gash", "prospect", "searing_edge", "flash_freeze", "drowse", "galvanize", "prolong", "umbra", "disarm" ->
				CraftedRunes.apply(cast, node, hit, helped, harmed, power, duration);
			case "blood_thread", "kindling", "twin_star", "borrowed_time", "gale_mantle", "stoneform", "mirrorfrost", "fortune", "phantom", "stormheart" ->
				Innates.apply(cast, rune, helped, harmed, power, duration);
			default -> {
				if (Runes.fused(rune)) {
					// The fused effects, made only at the Fusion Altar.
					FusedEffects.apply(cast, node, hit, helped, harmed, power, duration, amplify);
				} else {
					// The runes of the world (found, never crafted) live in their own class; a rune from an add-on (dev.wildercord.api) does what it registered.
					ExplorerEffects.apply(cast, node, hit, helped, harmed, power, duration);
					AddonRunes.effect(cast, node, hit, harmed, helped, power, duration);
				}
			}
		}
		// Kindled: whatever the effect struck is set alight too.
		ExplorerEffects.kindle(cast, node, harmed, duration);
		// Kindred: a helpful effect lands again, at half power, on its caster and the nearest ally it missed.
		if (node.count(Runes.KINDRED) > 0) {
			CraftedRunes.share(cast, node, hit, helped, groupPower);
		}
		List<LivingEntity> touched = rune.kind() == EffectKind.HELPFUL ? helped : harmed;
		if (!hit.self()) {
			Vfx.Theme theme = Vfx.theme(rune);
			touched.forEach(t -> dev.wildercord.cast.feel.Feels.touched(level, t, theme, rune, cast));
		}
	}

	// ------------------------------------------------------------------ helpers

	/** Whether a rune is one of Wildercord's own, run by name here; an add-on's does only what it registered. */
	static boolean builtIn(RuneDef rune) {
		return rune.id().startsWith("wildercord:");
	}

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
		int ticks = (int) Math.round(seconds * 20 * duration);
		// A passive's buffs last a little past its next renewal, so switching it off ends them (a potion's own are untouched).
		return passiveEffect ? dev.wildercord.spell.Passives.effectTicks(ticks) : ticks;
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
		if (applyingCast != null && !applyingCast.admits(target)) return;
		if (dev.wildercord.party.Parties.blocksCurrentHarm(target)) return;
		// Anchor: nothing a spell does moves it.
		if (VoidTime.anchored(target)) {
			if (impulse.lengthSqr() > 0.09 && target.level() instanceof ServerLevel level) {
				VoidFx.clank(level, target);
			}
			return;
		}
		double resist = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		impulse = DefensiveFoci.resist(target, impulse);
		if(target instanceof Player) impulse=impulse.scale(1-.10*dev.wildercord.gear.ElementalArmor.count(target,dev.wildercord.gear.ElementalArmor.Kind.STONEBOUND));
		Vec3 scaled = target instanceof Player ? impulse : impulse.scale(Math.max(0.0, 1.0 - resist));
		if (target instanceof ServerPlayer player && scaled.lengthSqr() > 0) dev.wildercord.aura.MasterFormMovement.begin(player, 2);
		target.setDeltaMovement(target.getDeltaMovement().add(scaled));
		target.needsSync = true;
		if (target instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	/**
	 * Lets the next hit land in full: clears the target's hurt cooldown, so several spells striking it at
	 * once (Split, Volley, Hail, an echo) each deal their own damage. In 26.3 the cooldown a hit checks is
	 * the living entity's {@code damageCooldownTime}; {@code invulnerableTime} alone no longer covers it.
	 */
	public static void readyToHurt(net.minecraft.world.entity.Entity target) {
		target.setInvulnerableTime(0);
		if (target instanceof LivingEntity living) {
			living.damageCooldownTime = 0;
		}
	}

	/** Set while soul fire burns: water can't dull it (see {@link #hurt}). */
	private static boolean soulBurn;

	static boolean soulBurning() {
		return soulBurn;
	}

	/** Creatures whose natural resistances a Rend has torn, until a game time. */
	private static final Map<UUID, Long> RENT = new HashMap<>();

	/** Whether {@code t} has been rent (Rend): what it resists it takes at full strength. */
	static boolean isRent(LivingEntity t) {
		if (RENT.isEmpty()) {
			return false;
		}
		Long until = RENT.get(t.getUUID());
		return until != null && until >= t.level().getGameTime();
	}

	/** Runs {@code task} as soul fire: fire damage that being wet doesn't weaken. */
	static void soulFire(Runnable task) {
		boolean outer = soulBurn;
		soulBurn = true;
		try {
			task.run();
		} finally {
			soulBurn = outer;
		}
	}

	/** Set while lingering damage (a burn's later ticks, a zone's pulses) is dealt: a Shield blocks it but can't parry it. */
	private static boolean lingering;

	/** Runs {@code task} as lingering damage over time: see {@link Shields#stops}. */
	static void lingering(Runnable task) {
		boolean outer = lingering;
		lingering = true;
		try {
			task.run();
		} finally {
			lingering = outer;
		}
	}

	/** Whether the damage being dealt right now lingers on from an earlier hit, rather than a spell arriving. */
	static boolean isLingering() {
		return lingering;
	}

	/**
	 * Damages with invulnerability frames skipped, so stacked effects in one spell all land.
	 * Execute on the current effect doubles it against targets under half health.
	 */
	static void hurt(Cast cast, LivingEntity target, DamageSource source, double amount) {
		hurtCapped(cast,target,source,amount,java.util.function.DoubleUnaryOperator.identity());
	}

	/** Finite effect admission runs after bonuses and before shared payment and defence. */
	static void hurtCapped(Cast cast, LivingEntity target, DamageSource source, double amount,
			java.util.function.DoubleUnaryOperator finalAdmission) {
		// A delayed hit rechecks relationships before shields, reactions or shared budgets are paid.
		if (!Targets.canHarm(cast.caster, target) || !cast.admits(target)) return;
		// Damage that didn't come through a shape's hit (a meteor landing, a secret spell's blast) meets a Shield here.
		if (Shields.stops(cast, target, cast.incoming())) {
			return;
		}
		// What the moment adds to the hit (a setup paying off, a weakness, a reaction), multiplied together.
		double bonus = 1.0;
		if (executeBonus > 1.0 && target.getHealth() < target.getMaxHealth() * 0.5F) {
			bonus *= executeBonus;
			Vfx.emit(cast.level, net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, target.getBoundingBox().getCenter(), 4, 0.3, 0.1);
			dev.wildercord.cast.feel.Feels.sound(cast.level, target.getBoundingBox().getCenter(), "tell_crack", 0.5F, 1.0F);
		}
		double fortune = Innates.fortune(cast, target);
		bonus *= fortune;
		bonus *= Unison.onHit(cast, target, currentElement);
		bonus *= hexBonus(cast, target);
		// Veil's ambush and Shadowstep's backstab: the first blow from the dark lands half again as hard.
		bonus *= VoidTime.opener(cast, target);
		bonus *= Techniques.condemned(cast, target);
		// A sleeper struck takes a backstab from the blow that wakes it (Drowse).
		bonus *= CraftedRunes.backstab(target);
		// What damage of this element sets off on the marks it meets (Fracture, Blight, Unweave, Rupture, Elapse), and Cracked.
		// Before the affinity, so a reaction this hit sets off breaks through a resistance, as Shatter's does.
		bonus *= Reactions.hit(cast, target, currentElement);
		if (!cast.consequencesValid(target)) return;
		bonus *= Affinities.multiplier(cast, target, source, currentElement);
		// Fire is weaker on the wet (unless the spell has grown Undying Flame).
		if (!soulBurn && !Mastery.wetFire(cast)) {
			bonus *= WorldMagic.wetDamage(target, currentElement);
		}
		// The damage traits its caster chose for it as it grew (see Mastery): held to their own cap, and to the one below.
		bonus *= Mastery.damageBonus(cast, target);
		bonus *= AddonRunes.react(cast, target, currentElement);
		if (!cast.consequencesValid(target)) return;
		bonus *= ExplorerEffects.bonus(cast, target, currentElement);
		// Trial Key: the opening blow on a target still at full health.
		if (openingBonus > 1.0 && target.getHealth() >= target.getMaxHealth() - 0.01F) {
			bonus *= openingBonus;
		}
		// Against a player all that together is held to the server's cap (defence.max_bonus): a setup still pays off, but
		// never ten times over, which is what took players from full health to dead in one blow. What the cast's performance
		// added (overchannel, the beat, a traced glyph) is in the hit's power already, and counts toward the same cap.
		float damage = (float) (amount * SpellDefenceRules.capBonus(bonus, cast.performance(), SpellDefence.maxBonus(target)));
		// PvP only: a monster's spell already has its power set by difficulty. The server can change the scale.
		if (target instanceof Player && cast.caster instanceof Player) {
			damage *= (float) dev.wildercord.config.Config.get().pvpDamageScale();
		}
		if (!Float.isFinite(damage) || damage<=0 || !cast.admits(target)) return;
		double allowed=finalAdmission.applyAsDouble(damage);
		if (!Double.isFinite(allowed) || allowed<=0) return;
		damage=(float)Math.min(damage,allowed);
        damage=cast.admitDamage(target,damage);
        if(damage<=0)return;
		HeartCircles.hurtBySpell(cast, target);
		Innates.spellHit(cast, target);
		// A hit that can't hurt (an immune snow golem under frost) isn't one a contract counts.
		if (damage > 0) {
			dev.wildercord.runesmith.Contracts.onSpellHit(cast.caster, target, currentElement);
		}
		if (!cast.admits(target)) return;
		readyToHurt(target);
		float dealt = damage;
		float before = target.getHealth();
		// A player's defences against spells (armour, Warding, Warded, the spellguard) are met there.
		DamageSource admittedSource = cast.guardedImpact() ? new RelayDamageSource(source, cast) : source;
		Dungeons.spellHit(() -> SpellDefence.hurtAdmitted(cast.level, target, admittedSource, dealt, cast));
		if (!cast.consequencesValid(target) || cast.guardedImpact() && (!cast.alive() || target.isAlive() && !cast.admits(target))) return;
		// A heavy hit lands with a punch for whoever cast it.
		if (damage >= 8) {
			ScreenFx.punch(cast.caster, Math.min(1, damage / 20F));
		}
		// Thirst: its caster drinks a share of what the hit really took.
		float taken = target instanceof TrainingDummy dummy ? dummy.lastDamage() : before - Math.max(0.0F, target.getHealth());
		if (fortune > 1 && taken > 0) LifeOwnerEvents.transition(cast.level,"fortune",target,LifeOwnerEvents.Moment.TRIGGER,1,-taken,cast.caster.getBoundingBox().getCenter(),cast.caster.getUUID());
		if (thirst > 0 && taken > 0 && cast.caster.isAlive() && cast.caster != target
				&& !(target instanceof TrainingDummy) && target.level().dimension() != PracticeRoom.DIMENSION) {
			cast.caster.heal((float) (taken * thirst));
			if (!cast.consequencesValid(target)) return;
			CraftedVfx.thirst(cast.level, target, cast.caster);
		}
		// Spellbrand: a brand this caster left on the target bursts.
		CraftedRunes.afterSpellHit(cast, target);
		if (!cast.consequencesValid(target)) return;
		// What the spell learns from the blow, and the traits that answer one (see Mastery).
		Mastery.afterDamage(cast, target, dealt, taken);
		if (!cast.consequencesValid(target)) return;
		dev.wildercord.aura.ResonantStrikes.spell(cast, target, currentElement, taken);
	}

	/** One strike of Lightning at {@code at}; whatever it hits is added to {@code struck}, to be set alight after the last strike. */
	private static void lightning(Cast cast, Vec3 at, double power, Set<LivingEntity> struck, Set<LivingEntity> aimedAt, boolean full) {
	ServerLevel level = cast.level;
	// The first three strikes of a cast are the whole show; the rest a lighter bolt, so a crowd doesn't fill the sky.
	if (full) {
		Vfx.lightning(level, at);
	} else {
		ElementFx.bolt(level, at.add(0, 10, 0), at, 0.08, 1, 2);
		ElementFx.groundRing(level, at, ElementFx.STORM.primary(), 0.3, 1.8, 0.05, 7);
	}
		for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(2.0, 3.0, 2.0), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity target = (LivingEntity) e;
			// A creature takes its strongest strike once per cast (its own 12, or half of that from a strike aimed at a neighbour),
			// however many strikes land near it: a crowd is hit once each, not once for every neighbour.
			double dealt = FusedEffects.unstacked(cast, target, "lightning", 2, 12 * power * (aimedAt.contains(target) ? 1.0 : 0.5));
			if (dealt > 0) {
				hurt(cast, target, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), dealt * Reactions.storm(cast, target));
			}
			struck.add(target);
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 3, false, false));
		}
	}

	/** Blasts one cast may set off at once, and how close (in radii) two blast centres must be to count as one. */
	static final int MAX_BLASTS = FireBloodRules.MAX_BLASTS;
	static final int MAX_METEORS = FireBloodRules.MAX_METEORS;
	static final double BLAST_MERGE = FireBloodRules.BLAST_MERGE;
	/** Meteor: how far it hits, and how long it falls (ticks). */
	static final double METEOR_RADIUS = 3.5;
	static final int METEOR_FALL = 24;

	static List<Vec3> clusterCentres(List<Vec3> points, double gap, int max) {
		return FireBloodRules.clusterCentres(points, gap, max);
	}

	/** Sets {@code t} alight for {@code ticks}; one already burning is topped up by that much instead (10 s at most). */
	static void feedOrIgnite(LivingEntity t, int ticks) {
		if (t.fireImmune()) {
			return;
		}
		if (t.isOnFire()) {
			t.setRemainingFireTicks(Math.min(200, t.getRemainingFireTicks() + ticks));
		} else {
			t.igniteForTicks(ticks);
		}
	}

	static void explode(Cast cast, Vec3 center, double radius, double power) {
		explode(cast, center, radius, power, new HashMap<>());
	}

	/**
	 * A blast. {@code landed} is what each enemy has already taken from this cast's blasts (before reactions): a blast only adds
	 * what it has over the strongest that landed, so overlapping blasts never stack on one enemy.
	 */
	static void explode(Cast cast, Vec3 center, double radius, double power, Map<UUID, Double> landed) {
		ServerLevel level = cast.level;
		double implode = Reactions.blast(cast, center, radius);
		radius *= implode;
		power *= implode > 1 ? 1.3 : 1.0;
		FireBloodVfx.blast(level, center, radius, landed.isEmpty(), "fire_blast", 1.0F);
		DamageSource source = level.damageSources().explosion(cast.caster, cast.caster);
		for (Entity e : level.getEntities((Entity) null, new AABB(center, center).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			LivingEntity target = (LivingEntity) e;
			double distance = target.getBoundingBox().getCenter().distanceTo(center);
			if (distance > radius) {
				continue;
			}
			double falloff = 1.0 - 0.4 * (distance / radius);
			double amount = 12 * power * falloff;
			double extra = FireBloodRules.unstacked(landed, target.getUUID(), amount);
			if (extra > 0) {
				hurt(cast, target, source, extra * Reactions.fire(cast, target));
			}
			Vec3 away = target.position().subtract(center);
			push(target, (away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize()).scale(1.3 * falloff).add(0, 0.45, 0));
			// Thrown by the blast: fire again soon (an Ember, say) sets off Wildfire.
			// (a tick later, so a second blast of this same cast doesn't set it off)
			Scheduler.later(2, () -> {
				if (target.isAlive()) {
					Reactions.mark(target, Reactions.Mark.WINDSWEPT);
				}
			});
		}
	}

	private static void dragonBreath(Cast cast, Vec3 start, double radius, double power, double duration, Vec3 along) {
		int pulses = (int) Math.round(5 * duration);
		dev.wildercord.cast.feel.Feels.sound(cast.level, start, "void_breath_roar", 1.0F, 1.0F);
		// The breath rolls on along the way it was blown, a block and a bit each second: a lane, not a spot.
		Vec3 drift = horizontal(along, along).scale(DRAGON_DRIFT);
		for (int i = 0; i < pulses; i++) {
			boolean later = i > 0;
			Vec3 center = i == 0 ? start : CastEngine.ground(cast.level, start.add(drift.scale(i)).add(0, 1.0, 0));
			Scheduler.later(1 + i * 20, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.dragonBreath(cast.level, center, radius);
				// After the first, the pulses linger: a Shield blocks them but can't parry them.
				Runnable pulse = () -> {
					for (Entity e : cast.level.getEntities((Entity) null, new AABB(center, center).inflate(radius, 2.0, radius), e -> Targets.canHarm(cast.caster, e))) {
						hurt(cast, (LivingEntity) e, cast.level.damageSources().source(DamageTypes.DRAGON_BREATH, cast.caster), 5 * power);
					}
				};
				if (later) {
					lingering(pulse);
				} else {
					pulse.run();
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
			dev.wildercord.cast.feel.Feels.sound(cast.level, caster.position(), "void_blink_fizzle", 0.9F, 1.0F);
			return;
		}
		for (int attempt = 0; attempt < 6; attempt++) {
			Vec3 spot = target.add(back.scale(1 + attempt * 0.5)).add(0, attempt % 2 == 0 ? 0 : 1, 0);
			spot = CastEngine.ground(cast.level, spot);
			AABB box = caster.getDimensions(caster.getPose()).makeBoundingBox(spot);
			// Always somewhere safe: ground to stand on (never over a chasm or the void), room, inside the world border,
			// and never into lava or fire (the ground under a lava lake is still "where the spell landed").
			if (footing(cast.level, spot) && cast.level.noCollision(caster, box) && cast.level.getWorldBorder().isWithinBounds(spot.x, spot.z)
					&& cast.level.getBlockStates(box.inflate(0, 0.5, 0)).noneMatch(s -> s.getFluidState().is(FluidTags.LAVA) || s.is(BlockTags.FIRE))) {
				Vec3 from = caster.position();
				caster.teleportTo(cast.level, spot.x, spot.y, spot.z, Set.<Relative>of(), caster.getYRot(), caster.getXRot(), false);
				caster.resetFallDistance();
				Vfx.blink(cast.level, from, spot);
				return;
			}
		}
		// Nowhere safe to land: the blink's ping cut short.
		dev.wildercord.cast.feel.Feels.sound(cast.level, caster.position(), "void_blink_fizzle", 0.9F, 1.0F);
		Vfx.emit(cast.level, net.minecraft.core.particles.ParticleTypes.SMOKE, caster.position().add(0, 1, 0), 4, 0.25, 0.02);
	}

	/**
	 * Whether there's ground right under {@code feet}: a spot {@link CastEngine#ground} found, rather than the point
	 * itself, which it hands back when there's nothing within reach below (a chasm, the void).
	 */
	static boolean footing(ServerLevel level, Vec3 feet) {
		return !level.noCollision(new AABB(feet.x - 0.2, feet.y - 0.25, feet.z - 0.2, feet.x + 0.2, feet.y - 0.01, feet.z + 0.2));
	}

	/** Whether there's lava or fire in {@code box} or just under it. */
	static boolean scorching(ServerLevel level, AABB box) {
		return level.getBlockStates(box.inflate(0, 0.5, 0)).anyMatch(s -> s.getFluidState().is(FluidTags.LAVA) || s.is(BlockTags.FIRE));
	}

	/**
	 * Somewhere a spell may set {@code entity} down, as Blink does: room for it, ground right under it (never over a
	 * chasm or the void), inside the world border, and no lava or fire.
	 */
	static boolean safeSpot(ServerLevel level, Entity entity, Vec3 feet) {
		AABB box = entity.getDimensions(entity.getPose()).makeBoundingBox(feet);
		return footing(level, feet) && level.noCollision(entity, box) && level.getWorldBorder().isWithinBounds(feet.x, feet.z)
			&& !scorching(level, box);
	}

	/** How far Dragon Breath's cloud rolls on each second. */
	private static final double DRAGON_DRIFT = 1.2;

	/** Inferno: everything around the point burns for a few seconds. */
	private static void inferno(Cast cast, Vec3 point, double radius, double power, double duration) {
		inferno(cast, point, radius, power, duration, false);
	}

	/** {@code crater}: the pulses of a Meteor's crater (a few low flames, not the standing ring of an Inferno). */
	private static void inferno(Cast cast, Vec3 point, double radius, double power, double duration, boolean crater) {
		int pulses = (int) Math.round(4 * duration);
		for (int i = 0; i < pulses; i++) {
			boolean later = i > 0;
			int index = i;
			Scheduler.later(1 + i * 20, () -> {
				if (!cast.alive()) {
					return;
				}
				if (crater) {
					FireBloodVfx.craterPulse(cast.level, point, radius, index == pulses - 1);
				} else {
					FireBloodVfx.infernoPulse(cast.level, point, radius, index, pulses);
				}
				// After the first, the pulses linger: a Shield blocks them but can't parry them.
				Runnable pulse = () -> {
					for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius, 2.0, radius), e -> Targets.canHarm(cast.caster, e))) {
						LivingEntity t = (LivingEntity) e;
						// A circle, as drawn (the search box is square).
						if (Math.hypot(t.getX() - point.x, t.getZ() - point.z) > radius + t.getBbWidth() / 2) {
							continue;
						}
						t.igniteForSeconds(3);
						hurt(cast, t, cast.level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 3 * power * Reactions.fire(cast, t));
					}
				};
				if (later) {
					lingering(pulse);
				} else {
					pulse.run();
				}
			});
		}
	}

	/** Ticks between Thunderclap's flash and its crack. */
	static final int CLAP_DELAY = 5;

	/**
	 * Thunderclap: a flash, then (a fifth of a second later) the crack: 5 damage to everything around the point, which
	 * is staggered for half a second (a real stun on mobs) and, if a mob, forgets who it was hunting; the throw is short
	 * (Repel throws, this one stops). The delay is the warning.
	 */
	private static void thunderclap(Cast cast, Vec3 point, double radius, double power) {
		ElementFx.groundRing(cast.level, point, ElementFx.STORM.secondary(), radius * 1.2, 0.3, 0.06, CLAP_DELAY);
		Sigils.flash(cast.level, point.add(0, 1, 0), ElementFx.STORM.secondary(), 1.6F);
		dev.wildercord.cast.feel.Feels.sound(cast.level, point, "storm_flash", 0.9F, 1.0F);
		Scheduler.later(CLAP_DELAY, carryContext(() -> {
			if (!cast.alive()) {
				return;
			}
			List<LivingEntity> caught = enemiesAround(cast, point, radius);
			Vfx.thunderclap(cast.level, point, radius, !caught.isEmpty());
			for (LivingEntity t : caught) {
				hurt(cast, t, cast.level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 5 * power * Reactions.storm(cast, t));
				Vec3 away = horizontal(t.position().subtract(point), cast.caster.getLookAngle());
				push(t, away.scale(0.8 * Math.sqrt(power)).add(0, 0.3, 0));
				Spirits.hold(t, 10);
				if (t instanceof Mob mob) {
					mob.setTarget(null);
				}
				Reactions.mark(t, Reactions.Mark.WINDSWEPT);
			}
		}));
	}

	/** Starfall: stars rain down around the point over two seconds. */
	/** Venom: seconds it lasts, damage a second, and how far and to how many it spreads. */
	public static final int VENOM_SECONDS = 4;
	public static final double VENOM_PER_SECOND = 0.75;
	public static final double VENOM_SPREAD_REACH = 2.5;
	public static final int VENOM_SPREAD_MAX = 3;

	/** Until when (game time) each creature's venom runs: a second dose only extends it, so repeaters never stack tickers. */
	private static final Map<UUID, long[]> VENOM = new HashMap<>();

	static void venomDot(Cast cast,LivingEntity t,double power,int seconds){venomDot(cast,t,power,seconds,"venom");}
	static void venomDot(Cast cast,LivingEntity t,double power,int seconds,String outcomeRune){
		long now = cast.level.getGameTime();
		long[] running = VENOM.get(t.getUUID());
		if (running != null && running[0] > now) {
			running[0]=Math.max(running[0],now+seconds*20L);
			LifeOwnerEvents.renewDot(running,t);
			return;
		}
		long[] state = {now + seconds * 20L};
		VENOM.put(t.getUUID(),state);
		LifeOwnerEvents.ownDot(cast,outcomeRune,t,state);
		LifeOwnerEvents.admitted(cast,outcomeRune,t,LifeOwnerEvents.Moment.APPLY,1,null);
		Runnable[] next = new Runnable[1];
		next[0] = carryContext(() -> {
			if (!cast.alive() || !t.isAlive() || t.level() != cast.level || !cast.damageAvailable(t) || cast.level.getGameTime() > state[0]) {
				VENOM.remove(t.getUUID(),state);LifeOwnerEvents.forgetDot(state);
				return;
			}
			lingering(()->LifeOwnerEvents.mutation(cast,outcomeRune,t,LifeOwnerEvents.Moment.PULSE,null,()->hurt(cast,t,cast.level.damageSources().indirectMagic(cast.caster,cast.caster),VENOM_PER_SECOND*power)));
			Scheduler.later(20, next[0]);
		});
		Scheduler.later(20, next[0]);
	}

	/** The venomed pass it on, once: to up to 3 enemies nearby, at half strength. */
	static void venomSpread(Cast cast, LivingEntity from, double power, int seconds) {
		int passed = 0;
		for (Entity e : cast.level.getEntities(from, from.getBoundingBox().inflate(VENOM_SPREAD_REACH), x -> Targets.canHarm(cast.caster, x))) {
			if (passed >= VENOM_SPREAD_MAX) {
				break;
			}
			if (e instanceof LivingEntity other && other.distanceTo(from) <= VENOM_SPREAD_REACH && cast.once("venom-spread:" + other.getUUID())) {
				other.addEffect(new MobEffectInstance(MobEffects.POISON, seconds * 20, 0, false, true), cast.caster);
				venomDot(cast,other,power*.5,seconds);
				LifeOwnerEvents.admitted(cast,"venom",other,LifeOwnerEvents.Moment.PULSE,1,from.getBoundingBox().getCenter());
				passed++;
			}
		}
	}

	/** The most Absorption an overheal turns into: 2 hearts. */
	public static final float HEAL_SHIELD_MAX = 4.0F;

	/** Weakness I for 4 s once an Empower runs out. */
	public static final int EMPOWER_COMEDOWN = 80;

	/** Smite's wind-up in ticks (0.7 s) and its damage (13, x2 on undead). */
	public static final int SMITE_DELAY = 14;
	public static final double SMITE_DAMAGE = 13.0;

	private static void starfall(Cast cast, Vec3 point, double radius, double power) {
		// The first stars go to exposed enemies in the rain; the rest fall where they will.
		List<LivingEntity> exposed = enemiesAround(cast, point, radius).stream().filter(Exposed::has).limit(4).toList();
		List<Vec3> marks = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			double a = cast.level.getRandom().nextDouble() * Math.PI * 2;
			double r = Math.sqrt(cast.level.getRandom().nextDouble()) * radius;
			Vec3 target = i < exposed.size() ? CastEngine.ground(cast.level, exposed.get(i).position().add(0, 2, 0))
				: CastEngine.ground(cast.level, point.add(Math.cos(a) * r, 2, Math.sin(a) * r));
			marks.add(target);
			if (i == 7) {
				LifeArcaneFx.starfallPattern(cast.level, marks);
			}
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
		if (caster instanceof ServerPlayer player) dev.wildercord.aura.MasterFormMovement.begin(player, 31);
		Vec3 pull = to.normalize().scale(Math.min(3.2, 0.8 + distance * 0.12) * Math.sqrt(power)).add(0, 0.35, 0);
		caster.setDeltaMovement(pull);
		caster.needsSync = true;
		if (caster instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
		caster.resetFallDistance();
		Scheduler.later(30, caster::resetFallDistance);
		// It reels you in, not past: within 2 blocks of the point the momentum dies (you arrive, you do not overshoot).
		Vec3 point = hit.point();
		ShapeRunners.each(cast, 30, tick -> {
			if (tick > 2 && caster.position().add(0, 1, 0).distanceTo(point) < 2.0) {
				caster.setDeltaMovement(caster.getDeltaMovement().scale(0.2));
				caster.needsSync = true;
				if (caster instanceof ServerPlayer player) {
					player.connection.send(new ClientboundSetEntityMotionPacket(player));
				}
				return false;
			}
			return true;
		});
		Vfx.grapple(cast.level, caster.getEyePosition().subtract(0, 0.4, 0), hit.point());
	}

	/** Harvest: breaks grown crops around the block hit and replants them from their drops (a seed each, never a free one). */
	private static void harvest(Cast cast, Cast.Hit hit, double radiusScale) {
		if (!Casters.mayBuild(cast.caster)) {
			return;
		}
		BlockPos center = targetBlock(hit);
		int r = (int) Math.round(1 * radiusScale) + 1;
		int harvested = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
			BlockPos p = pos.immutable();
			BlockState state = cast.level.getBlockState(p);
			if (!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) || !crop.isMaxAge(state)) {
				continue;
			}
			// A crop in a claim is left alone; the rest of the field is still harvested.
			if (!Casters.mayEdit(cast.caster, cast.level, p)) {
				continue;
			}
			if (!cast.takeBlock()) {
				break;
			}
			// Replanted from its own drops, as a farmer would: one seed goes back into the ground (none, and it isn't).
			List<ItemStack> drops = Block.getDrops(state, cast.level, p, null, cast.caster, ItemStack.EMPTY);
			ItemStack seed = state.getCloneItemStack(cast.level, p, false);
			boolean replant = false;
			for (ItemStack drop : drops) {
				if (!replant && !seed.isEmpty() && ItemStack.isSameItem(drop, seed)) {
					drop.shrink(1);
					replant = true;
				}
			}
			cast.level.destroyBlock(p, false, cast.caster);
			for (ItemStack drop : drops) {
				if (!drop.isEmpty()) {
					Block.popResource(cast.level, p, drop);
				}
			}
			state.spawnAfterBreak(cast.level, p, ItemStack.EMPTY, true);
			if (replant) {
				cast.level.setBlockAndUpdate(p, crop.getStateForAge(0));
			}
			LifeOwnerEvents.cell(cast,"harvest",p,state,cast.level.getBlockState(p),LifeOwnerEvents.Moment.APPLY);
			harvested++;
		}
	}

	/**
	 * Icepath: water near the point freezes into frosted ice that melts on its own in the light, and
	 * is thawed after {@link WorldRules#THAW_TICKS} wherever it is (frosted ice never melts in the dark).
	 */
	private static void icepath(Cast cast, Vec3 point, double radius) {
		BlockPos center = BlockPos.containing(point.x, point.y - 0.5, point.z);
		int r = (int) Math.ceil(radius);
		BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
		List<BlockPos> frozen = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 1, r))) {
			BlockPos p = pos.immutable();
			if (p.distSqr(center) > radius * radius) {
				continue;
			}
			BlockState state = cast.level.getBlockState(p);
			// Never around a creature swimming in it (frost walker's rule): it would be stuck in the ice, and choke.
			if (state.is(Blocks.WATER) && state.getFluidState().isSource() && cast.level.getBlockState(p.above()).isAir()
					&& cast.level.isUnobstructed(ice, p, CollisionContext.empty()) && Casters.mayEdit(cast.caster, cast.level, p)) {
				if (!cast.takeBlock()) {
					break;
				}
				cast.level.setBlockAndUpdate(p, ice);
				cast.level.scheduleTick(p, Blocks.FROSTED_ICE, 60 + cast.level.getRandom().nextInt(60));
				frozen.add(p);
			}
		}
		if (!frozen.isEmpty()) {
			Thaws.schedule(cast.level, frozen, cast.level.getGameTime() + WorldRules.THAW_TICKS + cast.level.getRandom().nextInt(60));
			Vfx.icepath(cast.level, Vec3.atCenterOf(center), radius);
		}
	}

	/** How far an Icepath strip runs from the caster (blocks). */
	private static final int ICEPATH_LENGTH = 10;

	/** A path, literally: on Self it freezes a strip {@code halfWidth} either side of the way you look, an ice front growing a block a tick. */
	private static void icepathStrip(Cast cast, double halfWidth) {
		LivingEntity caster = cast.caster;
		Vec3 flat = horizontal(caster.getLookAngle(), caster.getLookAngle());
		Vec3 from = caster.position();
		for (int i = 1; i <= ICEPATH_LENGTH; i++) {
			Vec3 at = from.add(flat.scale(i)).add(0, 0.5, 0);
			Scheduler.later(i, () -> {
				if (cast.alive()) {
					icepath(cast, at, halfWidth);
				}
			});
		}
	}

	/** Collect reaches this far at most, however widened. */
	private static final double MAX_COLLECT = 24.0;
	/** Collect takes at most this many things at once (each is a streak of light). */
	private static final int MAX_COLLECT_ITEMS = 48;

	/** Collect: items and experience orbs around the point fly to the caster (not off ground they couldn't build on: a claim, spawn). */
	private static void collect(Cast cast, Vec3 point, double radius) {
		LivingEntity caster = cast.caster;
		double reach = Math.min(MAX_COLLECT, radius);
		int moved = 0;
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(reach),
				e -> (e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof net.minecraft.world.entity.ExperienceOrb)
						&& (!(e instanceof net.minecraft.world.entity.item.ItemEntity item) || collectableItem(caster, item))
					&& e.distanceToSqr(point) <= reach * reach && onOpenGround(cast, BlockPos.containing(e.position()).below()))) {
			if (moved >= MAX_COLLECT_ITEMS) {
				break;
			}
			Vfx.stream(cast.level, e.position(), caster.position().add(0, 1, 0), Vfx.theme("void"), 1);
			e.teleportTo(caster.getX(), caster.getY() + 0.5, caster.getZ());
			if (e instanceof net.minecraft.world.entity.item.ItemEntity item) {
				item.setNoPickUpDelay();
			}
			moved++;
		}
		// A ring drawing in on the point, then the sound of everything arriving; a quiet fizzle when there was nothing to fetch.
		ElementFx.groundRing(cast.level, CastEngine.ground(cast.level, point.add(0, 0.5, 0)), ElementFx.VOID.primary(), reach, 0.4, 0.05, 10);
		dev.wildercord.cast.feel.Feels.sound(cast.level, caster.position(), moved > 0 ? "void_collect_suck" : "fizzle", 0.9F, 1.0F);
	}

	/** Remote collection leaves another owner's drops and active pickup reservations in place. */
	private static boolean collectableItem(LivingEntity caster, net.minecraft.world.entity.item.ItemEntity item) {
		if (!item.isAlive() || item.hasPickUpDelay()) return false;
		var ownership = (ItemEntityAccessor) item;
		return (ownership.wildercord$target() == null || ownership.wildercord$target().equals(caster.getUUID()))
			&& (ownership.wildercord$thrower() == null || ownership.wildercord$thrower().getUUID().equals(caster.getUUID()));
	}

	/** Excavate: mines a 3x3 face of blocks around the block hit. */
	private static void excavate(Cast cast, Cast.Hit hit, boolean amplified) {
		if (hit.block() == null) {
			return;
		}
		net.minecraft.core.Direction face = hit.face() == null ? net.minecraft.core.Direction.UP : hit.face();
		BlockPos center = hit.block();
		int mined = 0;
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
					mined++;
				}
			}
		}
		// Digging is not an earthquake: a cracked seal where it dug (nothing at all if nothing gave), and no shake.
		if (mined > 0) {
			ElementFx.crack(cast.level, Vec3.atCenterOf(center), 1.2, 20);
		}
	}

	/** Shock: a small zap that arcs on to the nearest other enemy. */
	private static void shock(Cast cast, LivingEntity target, double power) {
		if (!cast.admits(target)) return;
		ServerLevel level = cast.level;
		Vfx.shockArc(level, target.getBoundingBox().getCenter().add(0, 1.2, 0), target.getBoundingBox().getCenter());
		hurt(cast, target, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 4 * power * Reactions.storm(cast, target));
		if (!cast.consequencesValid(target) || cast.guardedImpact() && (!cast.alive() || target.isAlive() && !cast.admits(target))) return;
		// The arc looks for a conductor first (a wet enemy, or one in metal armour) within 5; else the nearest within 4.
		LivingEntity next = null;
		boolean conductor = false;
		double best = Double.MAX_VALUE;
		List<Entity> nearby;
		if (cast.guardedImpact()) {
			nearby = new ArrayList<>();
			level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class), target.getBoundingBox().inflate(5.0),
				e -> e != target && e != cast.caster && e.isAlive(), nearby, Cast.MAX_ENTITIES + 1);
			if (nearby.size() > Cast.MAX_ENTITIES) return;
			nearby.sort(java.util.Comparator.comparingDouble((Entity e) -> e.distanceToSqr(target)).thenComparingInt(Entity::getId));
		} else nearby = level.getEntities(target, target.getBoundingBox().inflate(5.0), e -> e instanceof LivingEntity && Targets.canHarm(cast.caster, e));
		for (Entity e : nearby) {
			if (!Targets.canHarm(cast.caster, e) || !cast.admits(e)) continue;
			LivingEntity other = (LivingEntity) e;
			double d = other.distanceToSqr(target);
			if (cast.guardedImpact() && cast.caster instanceof ServerPlayer owner
				&& !RelayCircles.clear(level, owner, target.getBoundingBox().getCenter(), other.getBoundingBox().getCenter())) continue;
			boolean conducts = conducts(other);
			if (d > 25 || (!conducts && d > 16) || (conducts != conductor ? !conducts : d >= best)) {
				continue;
			}
			next = other;
			conductor = conducts;
			best = d;
		}
		if (next != null) {
			Vfx.shockArc(level, target.getBoundingBox().getCenter(), next.getBoundingBox().getCenter());
			StormEarthFx.tether(level, target.getBoundingBox().getCenter(), next.getBoundingBox().getCenter());
			if (cast.guardedImpact() && cast.takeEntities(1) < 1) return;
			Vec3 previousOrigin = cast.incoming();
			if (cast.guardedImpact()) cast.incoming(target.getBoundingBox().getCenter());
			try {
				hurt(cast, next, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), (conductor ? 4 : 3) * power * Reactions.storm(cast, next));
			} finally {
				if (cast.guardedImpact()) cast.incoming(previousOrigin);
			}
		}
	}

	/** Whether something carries a shock on: standing wet, or in two or more pieces of metal armour. */
	private static boolean conducts(LivingEntity e) {
		if (WorldMagic.wet(e)) {
			return true;
		}
		int metal = 0;
		for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
				net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
			String path = BuiltInRegistries.ITEM.getKey(e.getItemBySlot(slot).getItem()).getPath();
			if (path.startsWith("iron_") || path.startsWith("chainmail_") || path.startsWith("golden_") || path.startsWith("copper_")) {
				metal++;
			}
		}
		return metal >= 2;
	}

	/** Meteor: a burning rock falls for 1.2 seconds (a reticle shows where), bursts, and leaves a crater that burns on a moment. */
	private static void meteor(Cast cast, Vec3 target, double radius, double power, Map<UUID, Double> landed) {
		Vec3 ground = CastEngine.ground(cast.level, target.add(0, 1, 0));
		FireBloodVfx.meteorFall(cast.level, ground, METEOR_FALL);
		Scheduler.later(METEOR_FALL, () -> {
			if (!cast.alive()) {
				return;
			}
			double implode = Reactions.blast(cast, ground, radius);
			double r = radius * implode;
			FireBloodVfx.meteorLand(cast.level, ground, r);
			DamageSource source = cast.level.damageSources().explosion(cast.caster, cast.caster);
			for (Entity e : cast.level.getEntities((Entity) null, new AABB(ground, ground).inflate(r), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity t = (LivingEntity) e;
				double distance = t.position().distanceTo(ground);
				if (distance > r) {
					continue;
				}
				double falloff = 1.0 - 0.4 * (distance / r);
				double amount = 12 * power * falloff * (implode > 1 ? 1.3 : 1.0);
				double extra = FireBloodRules.unstacked(landed, t.getUUID(), amount);
				if (extra > 0) {
					hurt(cast, t, source, extra * Reactions.fire(cast, t));
				}
				t.igniteForSeconds(4);
				Vec3 away = t.position().subtract(ground);
				push(t, (away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize()).scale(0.8 * falloff).add(0, 0.9, 0));
			}
			// The crater: the ground where it fell burns on for a moment.
			inferno(cast, ground, r * 0.6, power * 0.33, 0.5, true);
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
		dev.wildercord.cast.feel.Feels.sound(cast.level, point, "void_corral", 0.9F, 1.0F);
		for (int t = 0; t <= total; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!cast.alive()) {
					return;
				}
				Vfx.gravityWell(cast.level, point, radius, tick);
				if (tick >= total - 1) {
					VoidFx.discSnap(cast.level, point, radius);
				}
				for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
					LivingEntity victim = (LivingEntity) e;
					Vec3 towards = point.subtract(victim.position());
					double distance = towards.length();
					if (distance > radius || distance < 0.4) {
						continue;
					}
					// Bosses feel the pull (and the crush) but are never dragged: a boss held in the well is out of its fight.
					if (!Spirits.isBoss(victim) && !VoidTime.anchored(victim)) {
						push(victim,towards.normalize().scale(Math.min(0.6, 0.12 + distance * 0.05)).subtract(victim.getDeltaMovement().scale(.5)));
					}
					// It has weight: whatever hangs above the point is dragged down with the rest, and the mark outlasts the well.
					Reactions.mark(victim, Reactions.Mark.PULLED, tick >= total - 1 ? 60 : 50);
					if (!Spirits.isBoss(victim) && !VoidTime.anchored(victim) && victim.getY() > point.y + 0.6 && !victim.onGround()) {
						victim.setDeltaMovement(victim.getDeltaMovement().x, Math.min(victim.getDeltaMovement().y, -0.35), victim.getDeltaMovement().z);
						victim.needsSync = true;
					}
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
		LifeOwnerEvents.init();
	}

	private static void light(Cast cast, Cast.Hit hit, double duration) {
		BlockPos pos = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		ServerLevel level = cast.level;
		if (!level.getBlockState(pos).isAir() || !mayEdit(cast, pos)) {
			Casters.tell(cast.caster, Component.translatable("message.wildercord.light_blocked"));
			return;
		}
		BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
		level.setBlockAndUpdate(pos, light);
		GlobalPos lit = GlobalPos.of(level.dimension(), pos.immutable());
		LIGHTS.add(lit);
		int ticks = ticks(60, duration);
		TemporaryBlocks.put(level, pos, light, Blocks.AIR.defaultBlockState(), level.getGameTime() + ticks);
		Vfx.light(level, Vec3.atCenterOf(pos));
		Scheduler.later(ticks, () -> {
			LIGHTS.remove(lit);
			// Out of loaded ground now: it goes out as its chunk loads (see TemporaryBlocks), never loaded just for this.
			if (level.isLoaded(pos)) {
				if (level.getBlockState(pos).is(Blocks.LIGHT)) {
					level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				}
				TemporaryBlocks.remove(level, pos);
			}
		});
	}

	/** Whether Collect may take what lies on {@code ground}: ground the caster could build on, or a spell's passing Span or Rampart. */
	private static boolean onOpenGround(Cast cast, BlockPos ground) {
		if (isTemporary(cast.level, ground)) {
			return cast.caster instanceof ServerPlayer player && Casters.mayBuild(player) && cast.level.mayInteract(player, ground);
		}
		return Casters.mayEdit(cast.caster, cast.level, ground);
	}

	/** Bone-meals the block that was hit and the ones around it. */
	private static void grow(Cast cast, Cast.Hit hit, double power) {
		BlockPos center = targetBlock(hit);
		int times = (int) Math.round(2 * power);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
			BlockPos p = pos.immutable();
			var source = cast.level.getBlockState(p);
   if(source.is(dev.wildercord.wildlife.EmberContent.FERN)){if(dev.wildercord.wildlife.EmberContent.affectFern(cast,p,"life"))LifeOwnerEvents.cell(cast,"grow",p,source,cast.level.getBlockState(p),LifeOwnerEvents.Moment.APPLY);continue;}
			if (!(source.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock)) continue;
			// Known adjacent mutations reserve every destination before vanilla writes the first cell.
			if (GrowDoublePlantPreflight.handles(source)) {
				if (!GrowDoublePlantPreflight.reserve(cast,p,source)) continue;
			} else if (GrowSeagrassPreflight.handles(source)) {
				if (!GrowSeagrassPreflight.reserve(cast,p,source)) continue;
			} else if (GrowMossCarpetPreflight.handles(source)) {
				if (!GrowMossCarpetPreflight.reserve(cast,p,source)) continue;
			} else {
				// Other bonemeal features retain their original source admission and native behavior.
				if (!Casters.mayBuild(cast.caster) || !Casters.mayEdit(cast.caster,cast.level,p)) continue;
				if (!cast.takeBlock()) break;
			}
			boolean grew=false;
			try(var observedWrites=LifeGrowthWrites.open(cast,"grow")){
				for(int i=0;i<times;i++)grew|=BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL),cast.level,p);
			}
			if (grew) {
				cast.level.levelEvent(null,1505,p,15);
			}
		}
		// Whatever is young there grows up.
		for (net.minecraft.world.entity.AgeableMob baby : cast.level.getEntitiesOfClass(net.minecraft.world.entity.AgeableMob.class, new AABB(center).inflate(1.5), m -> m.getAge() < 0)) {
			baby.setAge(0);
			LifeOwnerEvents.admitted(cast,"grow",baby,LifeOwnerEvents.Moment.APPLY,1,null);
		}
		// Retained block writes and actual age changes own Life outcome presentation.
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
		/** Uses left (Bramble's thorns) and what a renewal refills them to; 0 refill means unlimited. */
		int charges;
		int refill;
	}

	private static final Map<String, Ward> WARDS = new HashMap<>();

	static void clearWards() {
		WARDS.clear();
		VENOM.clear();
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
			old.charges = Math.max(old.charges, old.refill);
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
		if (target instanceof ServerPlayer player) dev.wildercord.aura.MasterFormMovement.begin(player, 2);
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
		VoidTime.anchor(t, ticks);
		// Dug in: standing still for a second doubles the armour it gives.
		Vec3[] last = {t.position()};
		Ward fresh = ward(cast, t, "anchor", ticks, 1.0, 10, w -> {
			boolean still = t.position().distanceToSqr(last[0]) < 0.04;
			last[0] = t.position();
			w.memory = still ? Math.min(2, w.memory + 1) : 0;
			modifier(t, Attributes.ARMOR, ANCHOR_ID, w.memory >= 1 ? 8.0 : 4.0, AttributeModifier.Operation.ADD_VALUE);
		}, () -> {
			unmodify(t, ANCHOR_ID, ANCHORED);
			if (t.isAlive() && !cast.passive && t.level() instanceof ServerLevel level) {
				// The chains go slack.
				TimeFx.ending(level, t.getBoundingBox().getCenter(), ElementFx.VOID.secondary(), "void_step_tick", 0.6F);
			}
		});
		if (fresh != null || !cast.passive) {
			ExpansionVfx.anchor(cast.level, t, Vfx.theme("void"));
		}
	}

	/** Bramble: whatever hurts the target from close by takes damage back and is shoved away. */
	/** Bramble's thorns: each hit taken from within reach spends one, so a swarm can only be punished so far. */
	public static final int BRAMBLE_THORNS = 4;

	private static void bramble(Cast cast, LivingEntity t, int ticks, double power) {
		Ward fresh = ward(cast, t, "bramble", ticks, power, 2, w -> {
			int stamp = t.getLastHurtByMobTimestamp();
			if (stamp == (int) w.memory) {
				return;
			}
			w.memory = stamp;
			if (w.refill > 0 && w.charges <= 0) {
				return;
			}
			LivingEntity attacker = t.getLastHurtByMob();
			DamageSource last = t.getLastDamageSource();
			// Thorns never answer thorns, so two brambled casters can't trade blows forever.
			if (attacker == null || attacker == t || !attacker.isAlive() || attacker.distanceTo(t) > 4.5
					|| (last != null && last.is(DamageTypes.THORNS)) || !Targets.canHarm(w.cast.caster, attacker)) {
				return;
			}
			w.charges--;
			LifeOwnerEvents.mutation(w.cast,"bramble",attacker,LifeOwnerEvents.Moment.TRIGGER,t.getBoundingBox().getCenter(),()->hurt(w.cast,attacker,w.cast.level.damageSources().thorns(t),3*w.power));
			Vec3 away = horizontal(attacker.position().subtract(t.position()), t.getLookAngle());
			push(attacker, away.scale(0.9).add(0, 0.3, 0));
		}, () -> { });
		if (fresh != null) {
			fresh.charges = fresh.refill = BRAMBLE_THORNS;
			// Only hits from now on count.
			fresh.memory = t.getLastHurtByMobTimestamp();
		}
		LifeOwnerEvents.admitted(cast,"bramble",t,fresh!=null?LifeOwnerEvents.Moment.APPLY:LifeOwnerEvents.Moment.RENEW,fresh!=null?fresh.charges:0,null);
	}

	// ------------------------------------------------------------------ frost and wind mechanics

	/** How long an Airborne mark from Launch lasts (the flight of a throw). */
	private static final int AIRBORNE_LAUNCH_TICKS = 40;
	/** Dash: the impulse it shoves others with, and the speed (blocks a tick) it holds you at for five ticks. */
	private static final double DASH_SHOVE = 1.4;
	private static final double DASH_SPEED = 2.0;
	private static final int DASH_TICKS = 5;

	/** Whether {@code t} carries a ward of this kind right now. */
	static boolean warded(Entity t, String kind) {
		Ward ward = WARDS.get(kind + ":" + t.getUUID());
		return ward != null && ward.until >= t.level().getGameTime();
	}

	/** Push: a creature thrown hard into a block takes 2 and a flinch, once. Watches for about 0.7 s. */
	private static void wallSlam(Cast cast, LivingEntity t, double power) {
		if (Spirits.isBoss(t) || !Statuses.claim(t, "slam", 40)) {
			return;
		}
		Vec3[] last = {t.getDeltaMovement()};
		for (int i = 1; i <= 14; i++) {
			Scheduler.later(i, () -> {
				if (last[0] == null || !t.isAlive() || t.level() != cast.level) {
					return;
				}
				double before = Math.sqrt(last[0].x * last[0].x + last[0].z * last[0].z);
				if (t.horizontalCollision && before > 0.5) {
					last[0] = null;
					hurt(cast, t, cast.level.damageSources().source(DamageTypes.WIND_CHARGE, cast.caster), 2 * power);
					Statuses.stagger(t, 10);
					Vfx.emit(cast.level, net.minecraft.core.particles.ParticleTypes.CRIT, t.getBoundingBox().getCenter(), 6, 0.3, 0.2);
					return;
				}
				last[0] = t.getDeltaMovement();
			});
		}
	}

	/** Dash on yourself: held level at speed for five ticks, then braked, so it goes about ten blocks and never up or off anything. */
	private static void dashSelf(Cast cast, LivingEntity caster, Vec3 flat, double power) {
		if (caster instanceof ServerPlayer player) dev.wildercord.aura.MasterFormMovement.begin(player, DASH_TICKS + 1);
		double speed = DASH_SPEED * Math.sqrt(Math.max(0.25, power));
		caster.resetFallDistance();
		for (int i = 0; i < DASH_TICKS; i++) {
			Runnable step = () -> {
				if (caster.isAlive() && caster.level() == cast.level) {
					Vec3 v = caster.getDeltaMovement();
					setMotion(caster, new Vec3(flat.x * speed, Math.min(0, v.y), flat.z * speed));
					caster.resetFallDistance();
				}
			};
			if (i == 0) {
				step.run();
			} else {
				Scheduler.later(i, step);
			}
		}
		Scheduler.later(DASH_TICKS, () -> {
			if (caster.isAlive() && caster.level() == cast.level) {
				Vec3 v = caster.getDeltaMovement();
				setMotion(caster, new Vec3(flat.x * 0.25, v.y, flat.z * 0.25));
			}
		});
	}

	/** Levitate: the lifted creature hangs where it is, its drift stopped, for {@code ticks}. */
	private static void suspend(Cast cast, LivingEntity t, int ticks) {
		if (t instanceof ServerPlayer player) dev.wildercord.aura.MasterFormMovement.begin(player, ticks + 1);
		for (int i = 2; i < ticks; i += 2) {
			Scheduler.later(i, () -> {
				if (t.isAlive() && t.level() == cast.level && !t.onGround()) {
					Vec3 v = t.getDeltaMovement();
					setMotion(t, new Vec3(0, v.y, 0));
				}
			});
		}
	}

	/** Feather Fall: while it lasts, a creature that's falling drifts the way it looks (sneaking stops the drift). */
	private static void featherglide(Cast cast, LivingEntity t, int ticks) {
		String key = "featherglide:" + t.getUUID();
		Object token = new Object();
		GLIDES.put(key, token);
		for (int i = 2; i < ticks; i += 2) {
			Scheduler.later(i, () -> {
				if (GLIDES.get(key) != token || !t.isAlive() || t.level() != cast.level) {
					return;
				}
				glide(t);
			});
		}
	}

	/**
	 * One step of Feather Fall's drift, meant for every other tick: a creature falling slowly drifts the way it
	 * looks, up to a gentle speed, unless it's on the ground, sneaking (which stops it), swimming or gliding.
	 * Soar's descent drifts the same way.
	 */
	static void glide(LivingEntity t) {
		if (t.onGround() || t.isShiftKeyDown() || t.isInWater() || t.isFallFlying()) {
			return;
		}
		Vec3 v = t.getDeltaMovement();
		Vec3 look = horizontal(t.getLookAngle(), t.getLookAngle());
		double sx = v.x + look.x * GLIDE_PUSH * 2;
		double sz = v.z + look.z * GLIDE_PUSH * 2;
		double speed = Math.sqrt(sx * sx + sz * sz);
		if (speed > GLIDE_MAX) {
			sx *= GLIDE_MAX / speed;
			sz *= GLIDE_MAX / speed;
		}
		setMotion(t, new Vec3(sx, v.y, sz));
	}

	private static final Map<String, Object> GLIDES = new HashMap<>();
	private static final double GLIDE_PUSH = 0.03;
	private static final double GLIDE_MAX = 0.35;

	/** Swift: it shakes off the cold: Slowness (not a hold) and frozen skin are gone. */
	private static void shakeOffCold(LivingEntity t) {
		MobEffectInstance slow = t.getEffect(MobEffects.SLOWNESS);
		if (slow != null && slow.getAmplifier() < 6) {
			t.removeEffect(MobEffects.SLOWNESS);
		}
		t.setTicksFrozen(0);
		Reactions.clear(t, Reactions.Mark.FROZEN);
	}

	// ---- Chill stacks: each Chill within 6 s of the last deepens the slow one level (II, III, IV)

	/** By creature: when its chill runs out, its stacks so far, and when the last stack was added. */
	private static final Map<UUID, long[]> CHILLS = new HashMap<>();
	private static final int CHILL_MEMORY = 120;
	/** The soonest one creature takes another stack (a Zone's every pulse can't run it up to IV). */
	private static final int CHILL_GAP = 20;

	/** The Slowness amplifier a chill of {@code ticks} gives {@code t} now: 1 (II), 2 (III) or 3 (IV). */
	static int chillLevel(LivingEntity t, int ticks) {
		long now = t.level().getGameTime();
		if (CHILLS.size() > 256) {
			CHILLS.values().removeIf(c -> c[0] < now);
		}
		long[] c = CHILLS.get(t.getUUID());
		if (c == null || c[0] < now) {
			c = new long[] {now + ticks, 0, now};
			CHILLS.put(t.getUUID(), c);
		} else {
			c[0] = Math.max(c[0], now + ticks);
			if (now - c[2] >= CHILL_GAP && c[1] < 2) {
				c[1]++;
				c[2] = now;
			}
		}
		return 1 + (int) c[1];
	}

	// ---- Icicle melts into a soak

	/** The icicle stays lodged for 2 s, then melts and leaves its target soaked for 5 s. One melt at a time on a creature. */
	private static void melt(Cast cast, LivingEntity t) {
		if (!Statuses.claim(t, "melt", 40)) {
			return;
		}
		Scheduler.later(40, () -> {
			if (t.isAlive() && t.level() == cast.level) {
				Reactions.mark(t, Reactions.Mark.SOAKED, 100);
				Vfx.emit(cast.level, net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER, t.getBoundingBox().getCenter(), 6, 0.3, 0.0);
			}
		});
	}

	// ---- Bubble: a bubble holds small things longest

	/** Seconds a bubble holds a creature of this size: 2.5 up to 0.8 wide, 2 up to 1.4, 1.5 above. */
	static double bubbleSeconds(Entity t) {
		double w = t.getBbWidth();
		return w <= 0.8 ? 2.5 : w <= 1.4 ? 2.0 : 1.5;
	}

	/** Frostward: the target can't freeze, and frost can't leave it brittle for Shatter. */
	private static void frostward(Cast cast, LivingEntity t, int ticks) {
		t.setTicksFrozen(0);
		Reactions.clear(t, Reactions.Mark.FROZEN);
		int[] beats = {0};
		Ward fresh = ward(cast, t, "frostward", ticks, 1.0, 5, w -> {
			if (t.getTicksFrozen() > 0) {
				t.setTicksFrozen(0);
			}
			Reactions.clear(t, Reactions.Mark.FROZEN);
			if (++beats[0] % 8 == 0) {
				ExpansionVfx.frostwardIdle(cast.level, t);
			}
		}, () -> {
			if (t.isAlive() && t.level() == cast.level) {
				ExpansionVfx.frostwardEnd(cast.level, t);
			}
		});
		if (fresh != null || !cast.passive) {
			ExpansionVfx.frostward(cast.level, t, Vfx.theme("frost"));
		}
	}

	private static final Identifier CUSHION_ID = Wildercord.id("cushion");

	/** Cushion: no fall damage, and every hard landing throws a gust at the enemies around. */
	private static void cushion(Cast cast, LivingEntity t, int ticks, double power) {
		modifier(t, Attributes.FALL_DAMAGE_MULTIPLIER, CUSHION_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		t.resetFallDistance();
		int[] beats = {0};
		Ward fresh = ward(cast, t, "cushion", ticks, power, 1, w -> {
			if (++beats[0] % 50 == 0 && t.onGround()) {
				ExpansionVfx.cushionIdle(cast.level, t, Vfx.theme("wind"));
			}
			// Remembers how far this fall has come; it lands on the tick the ground is found again.
			if (!t.onGround()) {
				w.memory = Math.max(w.memory, t.fallDistance);
				return;
			}
			if (w.memory > 4) {
				cushionLanding(w.cast, t, w.power, w.memory);
			}
			w.memory = 0;
		}, () -> {
			unmodify(t, CUSHION_ID, List.of(Attributes.FALL_DAMAGE_MULTIPLIER));
			if (t.isAlive() && t.level() == cast.level) {
				ExpansionVfx.cushionEnd(cast.level, t, Vfx.theme("wind"));
			}
		});
		if (fresh != null || !cast.passive) {
			ExpansionVfx.cushion(cast.level, t, Vfx.theme("wind"));
		}
	}

	/** Cushion's landing: the gust deals this much per block fallen past 4, up to 6. */
	private static final double CUSHION_PER_BLOCK = 0.75;
	private static final double CUSHION_MAX = 6.0;

	private static void cushionLanding(Cast cast, LivingEntity t, double power, double fallen) {
		ExpansionVfx.cushionLand(cast.level, t.position(), 3.0, Vfx.theme("wind"), fallen);
		double damage = Math.min(CUSHION_MAX, CUSHION_PER_BLOCK * Math.max(0, fallen - 4)) * power;
		int struck = 0;
		for (Entity e : cast.level.getEntities(t, t.getBoundingBox().inflate(3.0, 1.0, 3.0), e -> Targets.canHarm(cast.caster, e))) {
			if (struck++ >= 16) {
				break;
			}
			LivingEntity v = (LivingEntity) e;
			Vec3 away = horizontal(e.position().subtract(t.position()), t.getLookAngle());
			Statuses.windPush(v, away.scale(1.1 * Math.sqrt(power)).add(0, 0.35, 0));
			Reactions.mark(e, Reactions.Mark.WINDSWEPT);
			if (damage > 0.5 && Statuses.claim(v, "cushion", 20)) {
				hurt(cast, v, cast.level.damageSources().source(DamageTypes.WIND_CHARGE, cast.caster), damage);
			}
		}
	}

	/** Tags a projectile Deflect has already sent back (a second deflection only turns it aside). */
	private static final String DEFLECTED_TAG = "wildercord.deflected";

	/** Deflect: projectiles coming at the target are sent back at their shooter, or turned aside if they've been already. */
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
				if (owner instanceof LivingEntity shooter && shooter.isAlive() && !p.entityTags().contains(DEFLECTED_TAG)) {
					// Returned to sender, once: it flies back at its shooter, now the warded creature's.
					p.addTag(DEFLECTED_TAG);
					Vec3 back = shooter.getBoundingBox().getCenter().subtract(p.position());
					p.setOwner(t);
					p.setDeltaMovement(back.normalize().scale(Math.max(0.6, v.length() * 0.8)));
					p.needsSync = true;
					ExpansionVfx.deflectHit(level, p.position(), back.normalize(), theme);
					continue;
				}
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
	/** How hard Haven's dome pushes the hostile out, once a second. */
	public static final double HAVEN_SHOVE = 0.9;

	private static void haven(Cast cast, Vec3 centre, double radius, int ticks) {
		ServerLevel level = cast.level;
		Vfx.Theme theme = Vfx.theme("life");
		// The first live shelter runner tick owns opening presentation.
		ShapeRunners.each(cast,ticks,tick->{
			if(tick==0)LifeOwnerEvents.point(cast,"haven",LifeOwnerEvents.Moment.APPLY,centre,null,1,0);
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
				LifeOwnerEvents.point(cast,"haven",LifeOwnerEvents.Moment.TRIGGER,p.position(),centre,1,0);
			}
			if (tick % 10 == 0) {
				for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius),
						e -> Targets.canHelp(cast.caster, e) && e.position().distanceTo(centre) <= radius)) {
					// (No Resistance: the dome keeps the fight out instead, below.)
				}
			}
			if (tick % 20 == 10) {
				for (Entity e : level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius), e -> Targets.canHarm(cast.caster, e) && e instanceof LivingEntity)) {
					LivingEntity foe = (LivingEntity) e;
					if (foe.position().distanceTo(centre) <= radius && !Spirits.isBoss(foe)) {
						var havenBefore=foe.getDeltaMovement();
						push(foe, horizontal(foe.position().subtract(centre), foe.getLookAngle()).scale(HAVEN_SHOVE));
						if(!foe.getDeltaMovement().equals(havenBefore))LifeOwnerEvents.point(cast,"haven",LifeOwnerEvents.Moment.TRIGGER,foe.position().add(0,1,0),centre,1,foe.getDeltaMovement().subtract(havenBefore).length());
					}
				}
			}
			if (tick % 20 == 0) {
				if(tick>0)LifeOwnerEvents.point(cast,"haven",LifeOwnerEvents.Moment.PULSE,centre,null,1,0);
			}
			if (tick == ticks - 1) {
				LifeOwnerEvents.point(cast,"haven",LifeOwnerEvents.Moment.END,centre,null,1,0);
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
			var observedLichen=(lichen?there:Blocks.GLOW_LICHEN.defaultBlockState()).setValue(side,true);
			level.setBlockAndUpdate(cell,observedLichen);
			if(level.getBlockState(cell).equals(observedLichen))LifeOwnerEvents.point(cast,"glimmer",LifeOwnerEvents.Moment.APPLY,Vec3.atCenterOf(cell),Vec3.atCenterOf(cell).add(face.getStepX(),face.getStepY(),face.getStepZ()),1,0);
			grown.add(cell);
		}
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
			// As shears would: leaves, webs and vines drop as themselves.
			BlockState state = level.getBlockState(p);
			List<ItemStack> drops = Block.getDrops(state, level, p, level.getBlockEntity(p), cast.caster, new ItemStack(Items.SHEARS));
			level.destroyBlock(p, false, cast.caster);
			for (ItemStack drop : drops) {
				if (!drop.isEmpty()) {
					Block.popResource(level, p, drop);
				}
			}
			cleared++;
		}
		ExpansionVfx.prune(level, point, radius, cleared > 0);
	}

	/** Light's invisible light blocks still lit, so they go out when the server stops (and, saved in {@link TemporaryBlocks}, even if it doesn't stop cleanly). */
	private static final java.util.Set<GlobalPos> LIGHTS = new java.util.HashSet<>();

	/**
	 * Whether the block at {@code pos} is only there for a while (a Span's glass, a Rampart's wall, frost's crust on lava, a Galvanize spark,
	 * a residue): pistons can't move it, and other spells leave it alone.
	 */
	public static boolean isTemporary(ServerLevel level, BlockPos pos) {
		return !SPAN.isEmpty() && SPAN.containsKey(GlobalPos.of(level.dimension(), pos)) || Techniques.isRampart(level, pos) || WorldMagic.isCrust(level, pos)
			|| CraftedRunes.isSpark(level, pos) || TemporaryBlocks.recorded(level,pos)
			|| dev.wildercord.content.PhysicalBlocks.isConstruct(level.getBlockState(pos)) || Residues.isResidue(level, pos);
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
			// Only in loaded ground: the rest are saved in TemporaryBlocks, and go as their chunks load.
			ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
				for (Map.Entry<GlobalPos, BlockState> entry : new ArrayList<>(SPAN.entrySet())) {
					ServerLevel level = server.getLevel(entry.getKey().dimension());
					BlockPos pos = entry.getKey().pos();
					if (level == null || !level.isLoaded(pos)) {
						continue;
					}
					if (level.getBlockState(pos).is(SPAN_BLOCK.getBlock())) {
						level.setBlockAndUpdate(pos, entry.getValue());
					}
					TemporaryBlocks.remove(level, pos);
				}
				SPAN.clear();
				for (GlobalPos lit : LIGHTS) {
					ServerLevel level = server.getLevel(lit.dimension());
					if (level == null || !level.isLoaded(lit.pos())) {
						continue;
					}
					if (level.getBlockState(lit.pos()).is(Blocks.LIGHT)) {
						level.setBlockAndUpdate(lit.pos(), Blocks.AIR.defaultBlockState());
					}
					TemporaryBlocks.remove(level, lit.pos());
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
		// Out of loaded ground now: it's put back as its chunk loads (see TemporaryBlocks), never loaded just for this.
		if (level.isLoaded(pos)) {
			if (level.getBlockState(pos).is(SPAN_BLOCK.getBlock())) {
				Vfx.emit(level, SpellMaterials.of("arcane", 0xB4E6F0, .12F), Vec3.atCenterOf(pos), 10, .35, .04);
				level.setBlockAndUpdate(pos, replaced);
			}
			TemporaryBlocks.remove(level, pos);
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
		long due = level.getGameTime() + ticks;
		for (int i = 0; i < order.size(); i++) {
			BlockPos p = order.get(i);
			Scheduler.later(1 + i / 3, () -> {
				if (!cast.alive()) {
					return;
				}
				BlockState state = level.getBlockState(p);
				// Never over a Light spell's light: put back when the bridge shattered, it would stay lit for good.
				if (!state.canBeReplaced() || state.is(Blocks.LIGHT) || !level.getEntities((Entity) null, new AABB(p), e -> e instanceof LivingEntity).isEmpty()
						|| !Casters.mayEdit(caster, level, p) || !cast.takeBlock()) {
					return;
				}
				SPAN.put(GlobalPos.of(level.dimension(), p.immutable()), state);
				TemporaryBlocks.put(level, p, SPAN_BLOCK, state, due);
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

	private record Hexed(UUID caster, long until, double bonus) {}

	private static final Map<UUID, Hexed> HEXED = new HashMap<>();
	/** How much harder a hexer's spells hit what they hexed. */
	public static final double HEX_BONUS = 1.25;

	/** Hex (and Malison's curse, see {@link SignatureFusions}): its caster's spells hit {@code t} harder for {@code ticks}, and it's shadowed. */
	static void hex(Cast cast, LivingEntity t, int ticks) {
		hex(cast, t, ticks, HEX_BONUS, true);
	}

	/** A curse of {@code bonus} (1.25 = +25%); {@code sound} false when the caller has its own cast sound (Malison). */
	static void hex(Cast cast, LivingEntity t, int ticks, double bonus, boolean sound) {
		long now = cast.level.getGameTime();
		HEXED.put(t.getUUID(), new Hexed(cast.caster.getUUID(), now + ticks, bonus));
		if (HEXED.size() > 256) {
			HEXED.values().removeIf(h -> h.until() < now);
		}
		// A curse leaves it shadowed as long as it lasts: life damage then sets off Blight.
		Reactions.mark(t, Reactions.Mark.SHADOWED, ticks);
		ExpansionVfx.hex(cast.level, t, ticks, sound);
		// The curse lifting, so the +25% is never a guess: a ring closes on the head and a small tick.
		TimeFx.endingLater(cast.level, t, ticks, ElementFx.VOID.primary(), "void_step_tick", 0.7F);
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
		return hexed.bonus();
	}

	private static final Identifier REND_ID = Wildercord.id("rend");

	/** Rend: less armour for a while, and torn open (bleeding: wind damage on it sets off Rupture). */
	private static void rend(Cast cast, LivingEntity t, int ticks) {
		Reactions.mark(t, Reactions.Mark.BLEEDING);
		// The tear goes deeper than armour: what the creature naturally resists, it takes at full strength meanwhile.
		RENT.put(t.getUUID(), cast.level.getGameTime() + ticks);
		if (RENT.size() > 256) {
			long now = cast.level.getGameTime();
			RENT.values().removeIf(until -> until < now);
		}
		FireBloodVfx.rend(cast.level, t);
		if (t.getAttribute(Attributes.ARMOR) == null) {
			return;
		}
		modifier(t, Attributes.ARMOR, REND_ID, -4.0, AttributeModifier.Operation.ADD_VALUE);
		ward(cast, t, "rend", ticks, 1.0, 20, w -> { }, () -> {
			unmodify(t, REND_ID, List.of(Attributes.ARMOR));
			if (t.isAlive() && t.level() == cast.level) {
				FireBloodVfx.rendMend(cast.level, t);
			}
		});
	}

	/** Countdown: a mark that ticks twice, then strikes. */
	private static void countdown(Cast cast, LivingEntity t, double power) {
		ExpansionVfx.countdown(cast.level, t, 0);
		Vec3[] last = {t.getBoundingBox().getCenter()};
		for (int beat = 1; beat <= 2; beat++) {
			int b = beat;
			Scheduler.later(beat * 10, () -> {
				if (cast.alive() && t.isAlive()) {
					last[0] = t.getBoundingBox().getCenter();
					ExpansionVfx.countdown(cast.level, t, b);
				}
			});
		}
		Scheduler.later(30, () -> {
			if (!cast.alive() || t.level() != cast.level) {
				return;
			}
			LivingEntity mark = t;
			if (!t.isAlive()) {
				// The mark died first: the moment finds whoever stands nearest where it fell.
				mark = ShapeRunners.nearestEnemy(cast, last[0], 6.0, null);
				if (mark == null) {
					return;
				}
			}
			ExpansionVfx.countdownStrike(cast.level, mark);
			hurt(cast, mark, cast.level.damageSources().indirectMagic(cast.caster, cast.caster), 6 * power);
		});
	}

	/** Sonic Boom passes through everything on its line, walls and armour alike: half as hard as the blow itself. */
	private static void sonicLine(Cast cast, Vec3 from, LivingEntity target, List<LivingEntity> already, DamageSource source, double amount) {
		Vec3 to = target.getBoundingBox().getCenter();
		int struck = 0;
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(from, to).inflate(1.5), e -> e != target && e instanceof LivingEntity && Targets.canHarm(cast.caster, e))) {
			LivingEntity v = (LivingEntity) e;
			if (already.contains(v) || struck >= 8 || !VoidTime.once(cast, "sonic_line", v, 20)) {
				continue;
			}
			Vec3 c = v.getBoundingBox().getCenter();
			Vec3 ab = to.subtract(from);
			double t = Math.max(0, Math.min(1, c.subtract(from).dot(ab) / Math.max(1.0E-4, ab.lengthSqr())));
			if (from.add(ab.scale(t)).distanceTo(c) <= 1.2 + v.getBbWidth() / 2) {
				struck++;
				hurt(cast, v, source, amount);
			}
		}
	}

	/** Bleed: blocks a creature must have moved in half a second for its wound to tear wider. */
	static final double BLEED_MOVING = FireBloodRules.BLEED_MOVING;

	/** Bleed: a cut, then more damage every half second (half as much again while the bearer moves). */
	private static void bleed(Cast cast, LivingEntity t, double power, int wounds) {
		DamageSource source = cast.level.damageSources().indirectMagic(cast.caster, cast.caster);
		FireBloodVfx.bleedCut(cast.level, t);
		// Bleeding for as long as the wound runs: wind damage on it sets off Rupture.
		Reactions.mark(t, Reactions.Mark.BLEEDING, wounds * 10 + 10);
		hurt(cast, t, source, 2 * power);
		Vec3[] last = {t.position()};
		for (int i = 1; i <= wounds; i++) {
			Scheduler.later(i * 10, () -> {
				if (!cast.alive() || !t.isAlive() || t.level() != cast.level) {
					return;
				}
				// The wound tears wider while its bearer is on the move.
				boolean moving = t.position().distanceToSqr(last[0]) > BLEED_MOVING * BLEED_MOVING;
				last[0] = t.position();
				FireBloodVfx.bleedDrip(cast.level, t, moving);
				hurt(cast, t, source, power * (moving ? 1.5 : 1.0));
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

	/** How long Coldsnap leaves everything it strikes brittle for Shatter and Fracture (4 s: the whole crowd primed). */
	private static final int COLDSNAP_WINDOW = 80;

	/** Coldsnap: frost bites everything around the point, slowing it and leaving it brittle for Shatter. */
	private static void coldsnap(Cast cast, Vec3 point, double radius, double power, double duration) {
		ExpansionVfx.coldsnap(cast.level, point, radius);
		int slow = ticks(4, duration);
		for (LivingEntity t : enemiesAround(cast, point, radius)) {
			// The ring travels: the nearest are struck at once, the farthest six ticks later.
			int delay = (int) Math.round(6 * Math.min(1.0, t.getBoundingBox().getCenter().distanceTo(point) / Math.max(0.5, radius)));
			Runnable strike = () -> {
				if (!cast.alive() || !t.isAlive() || t.level() != cast.level || !Targets.canHarm(cast.caster, t)) {
					return;
				}
				hurt(cast, t, cast.level.damageSources().source(DamageTypes.FREEZE, cast.caster), 4 * power);
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slow, 1, false, true));
				Reactions.mark(t, Reactions.Mark.FROZEN, COLDSNAP_WINDOW);
				ExpansionVfx.chilled(cast.level, t);
			};
			if (delay <= 0) {
				strike.run();
			} else {
				Scheduler.later(delay, strike);
			}
		}
	}

	/** Flashfire: a flash of heat that burns everything around the point. */
	private static void flashfire(Cast cast, Vec3 point, double radius, double power) {
		FireBloodVfx.flashfire(cast.level, point, radius);
		for (LivingEntity t : enemiesAround(cast, point, radius)) {
			double react = Reactions.fire(cast, t);
			t.igniteForSeconds(4);
			hurt(cast, t, cast.level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 5 * power * react);
			FireBloodVfx.flashSpark(cast.level, t);
		}
		// The heat is a friend to the cold-struck: allies in the flash are thawed and dried.
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(point, point).inflate(radius + 1), e -> e instanceof LivingEntity && e.isAlive() && Targets.canHelp(cast.caster, e))) {
			LivingEntity ally = (LivingEntity) e;
			if (ally.getBoundingBox().getCenter().distanceTo(point) > radius + ally.getBbWidth() / 2) {
				continue;
			}
			ally.setTicksFrozen(0);
			ally.removeEffect(MobEffects.SLOWNESS);
			Reactions.clear(ally, Reactions.Mark.FROZEN);
			Reactions.clear(ally, Reactions.Mark.WET);
			Reactions.clear(ally, Reactions.Mark.SOAKED);
		}
	}

	/** Banish: the target reappears further away from the caster, somewhere it fits and can see back to. Bosses stay put. */
	private static void banish(Cast cast, LivingEntity t, double distance) {
		ServerLevel level = cast.level;
		if (Spirits.isBoss(t) || VoidTime.anchored(t)) {
			ExpansionVfx.banishResisted(level, t);
			return;
		}
		Vec3 away = horizontal(t.position().subtract(cast.caster.position()), cast.caster.getLookAngle());
		Vec3 from = t.position();
		for (double d = distance; d >= 2; d -= 1) {
			Vec3 spot = CastEngine.ground(level, from.add(away.scale(d)).add(0, 1.0, 0));
			// Near its own height, and for a creature on the ground, on ground again: never over a chasm or the void
			// (where no ground is found, and the height check alone would pass a spot in mid-air). A flier may stay aloft.
			if (t.onGround() && !footing(level, spot) || Math.abs(spot.y - from.y) > 4
					|| !level.noCollision(t, t.getDimensions(t.getPose()).makeBoundingBox(spot))) {
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
				mob.setTarget(null);
			}
			// It comes out of the void dazed.
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, true));
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
					Statuses.airborne(v, 12);
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
					// Bowling: the crowd is flung the way the caster faced, as one, not out in every direction.
					Vec3 away = horizontal(cast.caster.getLookAngle(), cast.caster.getLookAngle());
					Statuses.windPush(v, away.scale(1.4 * Math.sqrt(power)).add(0, 0.5, 0));
				}
				hurt(cast, v, level.damageSources().source(DamageTypes.WIND_CHARGE, cast.caster), 3 * power);
			}
			return false;
		});
	}
}
