package dev.wildercord.cast;

import com.mojang.math.Transformation;
import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Affinity;
import dev.wildercord.spell.Bestiary;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creature affinities at runtime: a blaze takes frost 50% harder and shrugs off half of fire's blasts,
 * an iron golem conducts storm, the undead burn under life magic. The table is entity type tags,
 * {@code wildercord:affinity/weak_to_<element>}, {@code resists_<element>} and {@code immune_to_<element>}
 * (written by tools/generate_assets.py, so a datapack can change any of it), and {@link Affinity} says how
 * they combine. On top of its kind's, a Runebound resists the element its Cord carries. Players have none.
 *
 * <p>{@link #multiplier} is the one factor {@code Effects.hurt} takes for all of this, the caster's
 * {@link Climate} included. A weakness struck shows "Weak!" over the creature in the element's colour,
 * a resistance "Resisted" in grey (at most one callout a second per caster), and the first time a player
 * finds either on a kind of creature, it goes into their Grimoire's Bestiary.</p>
 */
public final class Affinities {
	private Affinities() {}

	private static final Map<String, TagKey<EntityType<?>>> WEAK = tags("weak_to_");
	private static final Map<String, TagKey<EntityType<?>>> RESISTS = tags("resists_");
	private static final Map<String, TagKey<EntityType<?>>> IMMUNE = tags("immune_to_");

	/** At most one callout a second per caster, however many creatures a spell strikes. */
	private static final int CALLOUT_TICKS = 20;
	private static final int GREY = 0xA8A4B0;
	/** Vanilla's fivefold freezing damage on blazes, striders and magma cubes (meant for powder snow) gives way to the table. */
	private static final double VANILLA_FREEZE = 5.0;
	/** Longer Bestiary keys (an add-on creature with a very long id) aren't written: a Grimoire toast carries at most 128 characters. */
	private static final int MAX_KEY = 120;

	private static final Map<UUID, Long> LAST_CALLOUT = new ConcurrentHashMap<>();
	/** Creatures an immune hit is about to land on: the hit is dropped whole (no flinch, no sound) rather than dealt as 0. */
	private static final Set<LivingEntity> IMMUNE_HITS = Collections.newSetFromMap(new IdentityHashMap<>());

	private static Map<String, TagKey<EntityType<?>>> tags(String prefix) {
		Map<String, TagKey<EntityType<?>>> tags = new LinkedHashMap<>();
		for (String element : Affinity.ELEMENTS) {
			tags.put(element, TagKey.create(Registries.ENTITY_TYPE, Wildercord.id("affinity/" + prefix + element)));
		}
		return tags;
	}

	/** The tag of creatures weak to {@code element} (null for an element that isn't one of the ten). */
	public static TagKey<EntityType<?>> weakTo(String element) {
		return WEAK.get(element);
	}

	public static TagKey<EntityType<?>> resisting(String element) {
		return RESISTS.get(element);
	}

	public static TagKey<EntityType<?>> immuneTo(String element) {
		return IMMUNE.get(element);
	}

	/** Whether a kind of creature has any affinity at all (so it has a line in the Bestiary). */
	public static boolean hasAny(EntityType<?> type) {
		for (String element : Affinity.ELEMENTS) {
			if (type.builtInRegistryHolder().is(WEAK.get(element)) || type.builtInRegistryHolder().is(RESISTS.get(element))
					|| type.builtInRegistryHolder().is(IMMUNE.get(element))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Everything where a spell hit lands and what it lands on does to it: the caster's climate for the
	 * element, times the target's affinity to it. {@code element} is the effect's; without one (a
	 * collision's burst, a secret spell's blast) the damage says which, if it can.
	 */
	static double multiplier(Cast cast, LivingEntity target, DamageSource source, String element) {
		String e = element.isEmpty() ? elementOf(source) : element;
		if (e.isEmpty() || !WEAK.containsKey(e)) {
			return 1.0;
		}
		return Climate.factor(cast.caster, e) * affinity(cast, target, source, e, !element.isEmpty());
	}

	/** The element a damage source counts as, for a hit with no effect behind it. */
	static String elementOf(DamageSource source) {
		return Affinity.elementOf(source.is(DamageTypeTags.IS_FIRE), source.is(DamageTypeTags.IS_FREEZING), source.is(DamageTypeTags.IS_LIGHTNING),
			source.is(DamageTypes.SONIC_BOOM));
	}

	/**
	 * The target's affinity to {@code element}, with its callout, cue and Bestiary entry for a player caster.
	 *
	 * @param wetCounted whether {@code Effects.hurt} dulls this hit for a wet target too (it does for an effect's fire)
	 */
	static double affinity(Cast cast, LivingEntity target, DamageSource source, String element, boolean wetCounted) {
		if (target instanceof Player || !Config.get().creatureAffinities()) {
			return 1.0;
		}
		// Burning spells on something fire can't hurt (a blaze, a strider) do nothing already: say so.
		boolean burnproof = element.equals("fire") && source.is(DamageTypeTags.IS_FIRE) && target.fireImmune();
		boolean weak = target.is(WEAK.get(element));
		boolean resists = target.is(RESISTS.get(element)) || runeboundElement(target).equals(element);
		boolean immune = burnproof || target.is(IMMUNE.get(element));
		Affinity.Verdict verdict = Affinity.judge(weak, resists, immune, Reactions.reactedWithin(target, 0));
		double multiplier = Affinity.multiplier(verdict);
		if (source.is(DamageTypeTags.IS_FREEZING) && target.is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
			multiplier /= VANILLA_FREEZE;
		}
		if (verdict == Affinity.Verdict.RESISTED && wetCounted) {
			// Resisting fire already counts being wet: a soaked guardian takes half, not half of three quarters.
			multiplier /= WorldMagic.wetDamage(target, element);
		}
		if (verdict == Affinity.Verdict.IMMUNE) {
			IMMUNE_HITS.add(target);
		}
		if (cast.caster instanceof ServerPlayer player) {
			meet(player, target.getType());
			if (verdict != Affinity.Verdict.NONE) {
				// A Runebound's resistance comes from its Cord, not its kind: that isn't written down.
				notice(cast, player, target, element, verdict, verdict != Affinity.Verdict.RESISTED || target.is(RESISTS.get(element)));
			}
		}
		return multiplier;
	}

	/** A Runebound resists the element its Cord carries (its first effect's): empty for anything else. */
	private static String runeboundElement(LivingEntity target) {
		if (!(target instanceof Mob mob) || !mob.hasAttached(WildercordAttachments.RUNEBOUND)) {
			return "";
		}
		for (RuneDef rune : Runebound.spellOf(mob)) {
			if (rune.family() == RuneFamily.EFFECT && !rune.element().isEmpty()) {
				return rune.element();
			}
		}
		return "";
	}

	// ------------------------------------------------------------------ what the caster sees and learns

	/**
	 * A weakness, resistance or immunity struck: a cue on the creature every time, a callout and a
	 * sound at most once a second, and, if it's true of the creature's whole kind ({@code ofItsKind}),
	 * the Bestiary entry the first time.
	 */
	private static void notice(Cast cast, ServerPlayer player, LivingEntity target, String element, Affinity.Verdict verdict, boolean ofItsKind) {
		ServerLevel level = cast.level;
		Vec3 c = target.getBoundingBox().getCenter();
		int color = RuneColors.element(element);
		double w = Math.max(0.5, target.getBbWidth());
		if (verdict == Affinity.Verdict.WEAK) {
			// A crack of bright sparks in the element's colour.
			Vfx.radial(level, ParticleTypes.CRIT, c, 10, 0.4);
			Motes.burst(level, c, 8, color, 0.14, 14, 0.18 + w * 0.08);
		} else {
			// A dull grey puff: it didn't take.
			Motes.clouds(level, c, 3, 0.2 + w * 0.2, Motes.SMOKE, 0.55 + w * 0.3, 22, new Vec3(0, 0.02, 0), 0.01, 0.3);
		}
		long now = level.getGameTime();
		Long last = LAST_CALLOUT.get(player.getUUID());
		if (last == null || now - last >= CALLOUT_TICKS || last > now) {
			LAST_CALLOUT.put(player.getUUID(), now);
			switch (verdict) {
				case WEAK -> {
					callout(level, target, Component.translatable("affinity.wildercord.weak").withColor(color).withStyle(ChatFormatting.BOLD));
					Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_CRIT, 0.45F, 1.3F);
				}
				case RESISTED -> {
					callout(level, target, Component.translatable("affinity.wildercord.resisted").withColor(GREY));
					Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_WEAK, 0.5F, 0.8F);
				}
				default -> {
					callout(level, target, Component.translatable("affinity.wildercord.immune").withColor(GREY));
					Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.55F, 0.9F);
				}
			}
		}
		if (ofItsKind) {
			String type = typeId(target.getType());
			Bestiary.Kind kind = switch (verdict) {
				case WEAK -> Bestiary.Kind.WEAK;
				case RESISTED -> Bestiary.Kind.RESISTS;
				default -> Bestiary.Kind.IMMUNE;
			};
			String key = Bestiary.key(type, kind, element);
			if (key.length() <= MAX_KEY && !Heart.discovered(player, key)) {
				// Met first (a fireproof creature with no tags of its own hasn't been), so the Bestiary lists it.
				if (!Heart.discovered(player, Bestiary.metKey(type))) {
					Grimoire.unlock(player, Bestiary.metKey(type));
				}
				Grimoire.unlock(player, key);
			}
		}
	}

	/** The first spell to strike a kind of creature with any affinity writes it into the Bestiary (quietly). */
	private static void meet(ServerPlayer player, EntityType<?> type) {
		String key = Bestiary.metKey(typeId(type));
		if (key.length() <= MAX_KEY && !Heart.discovered(player, key) && hasAny(type)) {
			Grimoire.unlock(player, key);
		}
	}

	public static String typeId(EntityType<?> type) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
	}

	/** A word popping up over the creature, drifting up and gone in a second. Everyone nearby sees it. */
	private static void callout(ServerLevel level, LivingEntity target, Component text) {
		if (Fx.muted()) {
			return;
		}
		Display.TextDisplay display = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return;
		}
		Vec3 at = target.position().add((level.getRandom().nextDouble() - 0.5) * 0.4, target.getBbHeight() + 0.45, (level.getRandom().nextDouble() - 0.5) * 0.4);
		display.snapTo(at.x, at.y, at.z);
		display.setText(text);
		display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
		display.setBackgroundColor(0);
		display.setFlags((byte) (Display.TextDisplay.FLAG_SHADOW | Display.TextDisplay.FLAG_SEE_THROUGH));
		display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.9F, 0.9F, 0.9F), new Quaternionf()));
		display.setPosRotInterpolationDuration(16);
		BlockFx.fresh(display);
		level.addFreshEntity(display);
		Scheduler.later(2, () -> {
			if (!display.isRemoved()) {
				display.setPos(at.x, at.y + 0.6, at.z);
			}
		});
		Scheduler.later(20, display::discard);
	}

	public static void init() {
		// An immune hit is dropped before it lands, so the creature doesn't flinch at a hit that did nothing.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			!(amount <= 0 && Dungeons.spellLanding() && IMMUNE_HITS.remove(entity)));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			IMMUNE_HITS.clear();
			if (server.getTickCount() % 100 == 0) {
				long now = server.overworld().getGameTime();
				LAST_CALLOUT.values().removeIf(last -> now - last > 100 || last > now);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			IMMUNE_HITS.clear();
			LAST_CALLOUT.clear();
		});
	}
}
