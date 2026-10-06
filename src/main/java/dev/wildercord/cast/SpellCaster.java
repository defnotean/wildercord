package dev.wildercord.cast;

import dev.wildercord.content.CordTier;
import dev.wildercord.content.RuneItem;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The server side of pressing R: checks everything (Cord worn, spell exists, runes learned,
 * cooldown, mana) and then runs the spell. The client never decides anything that matters.
 */
public final class SpellCaster {
	private SpellCaster() {}

	/** The runes of one spell that will actually fire. */
	public static List<RuneDef> activeRunes(Spellbook book, int spell, CordTier tier) {
		List<RuneDef> runes = new ArrayList<>();
		if (spell < 0 || spell >= dev.wildercord.gear.SpellSlots.ALL) {
			return runes;
		}
		List<String> ids = book.spells().get(spell);
		for (int socket : activeSockets(ids, book, spell, tier)) {
			runes.add(Runes.get(ids.get(socket)).orElseThrow());
		}
		return runes;
	}

	/**
	 * Socket positions whose runes will fire: inside the Cord's sockets, learned, loaded, and
	 * no stronger than the Cord can hold. Everything else stays threaded but quiet: a Silent
	 * Rune from a missing add-on, a rune past the last socket, or one too strong for this Cord. The
	 * tome's slot counts whatever the Cord (casting it also needs the tome in hand: see {@link dev.wildercord.gear.Gear#spellOpen}).
	 */
	public static List<Integer> activeSockets(List<String> ids, Spellbook book, int spell, CordTier tier) {
		List<Integer> sockets = new ArrayList<>();
		if (dev.wildercord.spell.RelayRules.containsIds(ids)) {
			var raw = ids.stream().map(Runes::get).flatMap(Optional::stream).toList();
			if (tier != CordTier.ECHO || raw.size() != ids.size() || !dev.wildercord.spell.RelayRules.valid(raw)
				|| ids.stream().anyMatch(id -> !book.knows(id))) return sockets;
		}
		if (tier == null || spell < 0 || spell >= tier.spells && spell != dev.wildercord.gear.SpellSlots.TOME) {
			return sockets;
		}
		for (int i = 0; i < Math.min(ids.size(), tier.sockets); i++) {
			Optional<RuneDef> rune = Runes.get(ids.get(i));
			if (rune.isPresent() && book.knows(rune.get().id()) && tier.holds(rune.get().tier())) {
				sockets.add(i);
			}
		}
		return sockets;
	}

	public static void cast(ServerPlayer player, int requested) {
		cast(player, requested, 0.0);
	}

	/**
	 * Casts a spell.
	 *
	 * @param charge how charged it was when released, 0 (a tap) to 1 (full): up to
	 *               {@link Charging#POWER} more power
	 */
	public static void cast(ServerPlayer player, int requested, double charge) {
		cast(player, requested, charge, Charging.Performance.NONE);
	}

	/**
	 * Casts a spell that was charged and let go with some performance: an overchannel stage, the beat, a
	 * traced glyph ({@link Charging.Performance}). Its power is in the cast's (and noted, so it counts
	 * inside the bonus cap against players), and its surge chance is rolled as the spell leaves.
	 */
	public static void cast(ServerPlayer player, int requested, double charge, Charging.Performance performance) {
		if (dev.wildercord.aura.MastersArts.committed(player) || RelayCircles.committed(player)) return;
		if (RelayCircles.contains(player, requested)) {
			int slot = requested < 0 ? Spellbooks.get(player).selected() : requested;
			String problem = RelayCircles.problem(player, slot);
			fail(player, Component.literal(problem == null ? "Relay Circle uses fresh cast-key presses: place, release the key, then press again." : problem));
			return;
		}
		if (RelayCircles.pending(player)) RelayCircles.cancel(player);
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			fail(player, Component.translatable("message.wildercord.no_cord"));
			return;
		}
		if (CastLock.locked(player)) {
			fail(player, Component.translatable("message.wildercord.silenced"));
			return;
		}
		if (VoidTime.hushed(player)) {
			// A Hush lies over them: no word they say makes a sound.
			fail(player, Component.translatable("message.wildercord.hushed"));
			return;
		}
		if (FusedFrostWards.sealed(player)) {
			// Untouchable in the ice, so it can't be a fortress to cast from.
			fail(player, Component.translatable("message.wildercord.cryostasis_sealed"));
			return;
		}
		Spellbook book = Spellbooks.get(player);
		int spell = requested < 0 ? book.selected() : requested;
		if (requested < 0 && !dev.wildercord.gear.Gear.spellOpen(player, tier, spell)) {
			// The selected spell went quiet (the Tome put away): the cast key takes the next open one.
			spell = dev.wildercord.gear.SpellSlots.resolve(tier.spells, dev.wildercord.gear.Gear.tome(player), spell);
		}
		if (!dev.wildercord.gear.Gear.spellOpen(player, tier, spell)) {
			fail(player, locked(spell));
			return;
		}
		long now = player.level().getGameTime();
		long readyAt = Spellbooks.readyAt(player, spell);
		// A wild surge's Free Recast: this one costs nothing and waits for no cooldown.
		boolean free = WildSurge.freeRecast(player, now);
		// The cooldown first: a press while it runs costs the server nothing (no compiling, no secrets).
		if (now < readyAt && !free) {
			Rhythm.early(player, now);
			fail(player, Component.translatable("message.wildercord.cooldown", String.format(java.util.Locale.ROOT, "%.1f", (readyAt - now) / 20.0)));
			return;
		}
		List<RuneDef> runes = activeRunes(book, spell, tier);
		if(!SoulWeaving.owns(player,runes)){
			fail(player,Component.literal("This soul weave contains an innate rune that does not belong to your heart."));return;
		}
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
		if (runes.isEmpty() || compiled.isEmpty()) {
			fail(player, Component.translatable("message.wildercord.spell_empty", spell + 1));
			return;
		}
		Optional<Secrets.Secret> secret = Secrets.match(runes);
		// A secret costs and recharges as one only once it's been found, as every readout shows it: the
		// first cast, which finds it, costs what the ordinary spell does. Read before it's found below.
		double secretPower = Heart.secretCost(player, runes);
		double secretCooldown = Heart.secretCooldown(player, runes);
		// The traits its caster chose as it grew with them (see Mastery): a little cheaper, a little quicker.
		double traitCost = Mastery.costFactor(player, runes);
		// What it costs, and whether it can be paid, before anything is spent.
		int blood = 0;
		int cost = 0;
		boolean overcast = false;
		float mana = Spellbooks.mana(player);
		if (!free && compiled.paysInHealth()) {
			// Blood Price: paid in health, and never enough to kill you.
			blood = Heart.healthCost(player, compiled, secretPower * traitCost);
			if (!player.isCreative() && player.getHealth() <= blood) {
				fail(player, Component.translatable("message.wildercord.no_health", blood));
				return;
			}
		} else if (!free) {
			cost = Heart.manaCost(player, compiled, secretPower * traitCost);
			if (!player.isCreative() && mana < cost) {
				// Not enough: a second press within two seconds overcasts, cracking a circle to pay.
				if (!Overcast.confirm(player, spell, (int) mana, cost)) {
					return;
				}
				overcast = true;
			}
		}
		// Add-ons may stop a cast here, before anything is spent (once per cast: an overcast's first press never gets this far).
		if (!dev.wildercord.api.WildercordEvents.BEFORE_CAST.invoker().allow(player, spell, List.copyOf(runes), cost)) {
			return;
		}
		boolean overflow = mana >= Mana.max(player) - 0.5F;
		Heart.Bonuses bonuses = Heart.bonuses(player, overflow);
		int spent;
		// Set when this cast is an overcast: the mana there was and what it cost, for wild magic.
		float overcastMana = -1;
		int overcastCost = -1;
		if (free) {
			spent = 0;
			WildSurge.useFreeRecast(player);
		} else if (compiled.paysInHealth()) {
			spent = blood * 5;
			if (!player.isCreative()) {
				player.setHealth(player.getHealth() - blood);
				Fx.sound(player.level(), player.position(), SoundEvents.PLAYER_HURT, 0.6F, 0.7F);
				Vfx.emit(player.level(), net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, player.getBoundingBox().getCenter(), 4, 0.3, 0.1);
			}
		} else if (overcast) {
			Overcast.crack(player);
			spent = (int) mana;
			overcastMana = mana;
			overcastCost = cost;
			Spellbooks.setMana(player, 0);
		} else {
			spent = cost;
			if (!player.isCreative()) {
				Spellbooks.setMana(player, mana - cost);
			}
		}
		if (!player.isCreative() && !free && !compiled.paysInHealth())
			dev.wildercord.aura.Unity.manaSpent(player, Math.max(0, mana - Spellbooks.mana(player)));
		int cooldown = Heart.cooldownTicks(player, compiled, secretCooldown * Mastery.cooldownFactor(player, runes));
		Spellbooks.setReadyAt(player, spell, now + cooldown);
		HeartCircles.condense(player, spent);
		VoidTime.spent(player, spent);
		ExplorerEffects.manatideRefund(player, spent);
		// Each element in the spell grows its affinity by the mana it made up; a Blood Price's health feeds blood.
		PlayerAffinities.onCast(player, compiled.root(), spent, blood);
		double rhythm = Rhythm.onCast(player, now, cooldown);
		double charged = 1 + Charging.POWER * Math.max(0, Math.min(1, charge));
		bonuses = bonuses.withPower(bonuses.power() * rhythm * charged * Mastery.powerFactor(player, runes) * performance.power());
		if (charge >= 1.0) {
			Grimoire.feat(player, dev.wildercord.spell.Feats.CHARGED);
		}
		dev.wildercord.advancement.Advancements.cast(player, runes.size());
		// The element the caster's magic leans toward (their deepest affinity), for the record of what was cast.
		String leaning = Heart.leaning(player);
		int castNumber = COMBO.computeIfAbsent(player.getUUID(), k -> new int[dev.wildercord.gear.SpellSlots.ALL])[spell] += 1;
		player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		Vfx.Theme theme = secret.map(s -> Vfx.themeOf(s.color())).orElse(compiled.root().groups.isEmpty() ? Vfx.theme("") : Vfx.theme(compiled.root().groups.getFirst()));
		if (!compiled.root().groups.isEmpty()) {
			// The spell's feel (its scale sizes the circle), worked out as the cast will.
			theme = theme.with(dev.wildercord.cast.feel.Signatures.adjust(dev.wildercord.cast.feel.Feel.of(compiled.root().groups.getFirst(), compiled.cost(), charge)));
		}
		dev.wildercord.player.RuneResearch.cast(player,runes);
		SpellTrials.cast(player,runes,cost);
		// Casting gear, read from the hands now: a staff's flourish on a charged cast, and its power on every part of the spell.
		dev.wildercord.gear.GearBonuses gear = dev.wildercord.gear.Gear.of(player);
		dev.wildercord.gear.Gear.flourish(player, gear, dev.wildercord.gear.GearBonuses.elements(compiled.root()), charge);
		// Everyone around sees the casting pose for this spell's shape.
		player.setAttached(dev.wildercord.player.WildercordAttachments.CAST_POSE, new dev.wildercord.player.WildercordAttachments.CastPose(poseShape(runes).id(), now));
		HeartCircles.onCast(player);
		// Under a mana storm, a spell may surge (see cast.events.ManaStorm).
		dev.wildercord.cast.events.EventRules.Surge surge = dev.wildercord.cast.events.ManaStorm.surge(player);
		bonuses = bonuses.withPower(bonuses.power() * dev.wildercord.cast.events.EventRules.surgePower(surge));
		Cast.Info info = new Cast.Info(compiled.root(), runes.size(), leaning, List.copyOf(runes));
		// Against a Shield a secret always weighs its full price, found or not.
		Cast cast = new Cast(player, castNumber, bonuses, false, null, info).weigh(compiled.cost() * secret.map(Secrets.Secret::power).orElse(1.0)).gear(gear)
			.withAffinity();
		cast.damagePrice(Heart.manaCost(player, compiled, secretPower * traitCost));
        cast.charge(charge);
		cast.performance(performance.power());
		// What this spell learns from the cast, and the traits it has grown (see Mastery).
		Mastery.onCast(player, spell, runes, cast, spent);
		if(secret.isPresent())Vfx.castCircle(player,theme,runes);
		else FormationVfx.send(cast,runes);
		if (overcast) {
			// Magic paid for with a cracked circle marks the world where it lands (see Residues).
			cast.markOvercast();
		}
		if (!cast.info.root().groups.isEmpty()) {
			var first = cast.info.root().groups.getFirst();
			dev.wildercord.cast.feel.Feels.cue(cast, cast.feel(first), cast.theme(first));
		}
		if (charge < 0.2) {
			// A tap has no release sound of its own (the charged release has): a short snap so it doesn't leave in silence.
			dev.wildercord.cast.feel.Feels.sound(player.level(), player.position(), "tap_release", 0.45F, 1.0F);
		}
		if (secret.isPresent()) {
			SecretSpells.discover(player, secret.get());
		} else {
			// This world's own magic: if these runes are one of its resonances, it wakes, and its twist rides the cast.
			WorldResonances.wake(player, runes, cast);
		}
		java.util.function.Consumer<Cast> release = c -> {
			if (secret.isPresent()) {
				SecretSpells.cast(c, secret.get());
			} else {
				// Cast together with others, the same shape at the same place: one bigger chorus spell.
				dev.wildercord.chorus.Chorus.Sung sung = dev.wildercord.chorus.Chorus.sing(c, runes, compiled.root());
				CastEngine.cast(sung.cast(), sung.root());
			}
		};
		// Cast sneaking with an aura weapon in hand (Edge and up), the spell flows into the blade instead of leaving, and rides
		// the next Aura Slash (see aura.Spellblade); if the blade can't take it by then, it leaves as usual. An overchanneled spell
		// (sneak there steadies the charge) always leaves as usual.
		final java.util.function.Consumer<Cast> leave = performance.stage() == 0
			&& dev.wildercord.aura.Spellblade.wants(player, compiled.root(), secret.isPresent())
			? c -> {
				if (!dev.wildercord.aura.Spellblade.draw(player, c, compiled.root(), release)) {
					release.accept(c);
				}
			}
			: release;
		// Wild magic: an overcast spell, or an overchanneled one, may twist into something else (both at once roll as one chance).
		final double surgeChance = overcastCost >= 0
			? dev.wildercord.spell.Overchannel.combined(WildSurge.overcastChance(player, overcastMana, overcastCost), performance.surgeChance())
			: performance.surgeChance();
		// The formation completes before any ordinary or wild release leaves the caster.
		Scheduler.later(3, () -> {
			if (!cast.alive()) return;
			if (surgeChance <= 0 || !WildSurge.roll(cast, runes, secret.isPresent(), surgeChance, leave)) {
				leave.accept(cast);
			}
		});
		dev.wildercord.runesmith.Contracts.onCast(player, runes);
		// The copies below go off for the same payment: they share its Siphon cap and once-per-cast
		// things (a second Imbue would store the spell twice for one price), and its casting gear.
		dev.wildercord.cast.events.ManaStorm.afterCast(player, surge, runes, () -> {
			Cast echo = cast.again(1.0);
			if (secret.isPresent()) {
				SecretSpells.cast(echo, secret.get());
			} else {
				CastEngine.cast(echo, compiled.root());
			}
		});
		// Twin Star: the next spell goes off a second time, a moment later.
		if (Innates.consumeTwin(player)) {
			Scheduler.later(8, () -> {
				if (cast.alive()) {
					TechniqueVfx.twinStar(player.level(), player);
					// The twin is a shade weaker than the caster (75%): a free second cast is not a free double.
					Cast again = cast.again(Innates.TWIN_POWER);
					if (secret.isPresent()) {
						SecretSpells.cast(again, secret.get());
					} else {
						CastEngine.cast(again, compiled.root());
					}
				}
			});
		}
		// Focus of Echoes: now and then the spell goes off again, a moment later, at no cost.
		if (dev.wildercord.gear.Gear.echoes(player, gear)) {
			Scheduler.later(10, () -> {
				if (cast.alive()) {
					HeartCircles.onCast(player);
					Cast again = cast.again(1.0);
					if (secret.isPresent()) {
						SecretSpells.cast(again, secret.get());
					} else {
						CastEngine.cast(again, compiled.root());
					}
				}
			});
		}
		dev.wildercord.api.WildercordEvents.AFTER_CAST.invoker().afterCast(player, spell, List.copyOf(runes), spent);
	}

	/** The shape a spell's casting pose is taken from: its first shape rune, or Self when it has none (effects come first in some spells). */
	static RuneDef poseShape(List<RuneDef> runes) {
		for (RuneDef rune : runes) {
			if (rune.family() == dev.wildercord.spell.RuneFamily.SHAPE) {
				return rune;
			}
		}
		return Runes.SELF;
	}

	/** Whether {@code entity} is sealed in a Cryostasis right now: nothing is cast, and no loadout loaded, from inside the ice. */
	public static boolean sealed(net.minecraft.world.entity.Entity entity) {
		return FusedFrostWards.sealed(entity);
	}

	/** Why a spell slot can't be used: the Cord has too few spells, or the tome's slot without the tome in hand. */
	private static Component locked(int spell) {
		if (spell == dev.wildercord.gear.SpellSlots.TOME) {
			return Component.translatable("message.wildercord.tome_needed");
		}
		return Component.translatable("message.wildercord.spell_needs", spell + 1, Component.translatable(CordTier.forSpells(spell + 1).itemKey()));
	}

	/** Gives a spell a custom name, or clears it back to the automatic one. */
	public static void rename(ServerPlayer player, int spell, String name) {
		if (spell < 0 || spell >= dev.wildercord.gear.SpellSlots.ALL) {
			return;
		}
		Spellbooks.set(player, Spellbooks.get(player).withName(spell, dev.wildercord.spell.SpellNames.clean(name)));
	}

	/**
	 * A spell's name, as {@code player} knows it: the custom one, a secret spell's once they've found it
	 * (never before: the name would give it away), or one made from its runes.
	 */
	public static String nameOf(net.minecraft.world.entity.player.Player player, Spellbook book, int spell, List<RuneDef> runes) {
		return nameOf(player, book, spell, runes, () -> dev.wildercord.spell.SpellNames.auto(runes));
	}

	/**
	 * As {@link #nameOf(net.minecraft.world.entity.player.Player, Spellbook, int, List)}, for {@code runes}
	 * already read as {@code compiled}: the HUD and the wheel draw the name every frame, and a name made
	 * from the runes would otherwise read the whole spell again each time.
	 */
	public static String nameOf(net.minecraft.world.entity.player.Player player, Spellbook book, int spell, List<RuneDef> runes,
			SpellCompiler.Compiled compiled) {
		return nameOf(player, book, spell, runes, () -> dev.wildercord.spell.SpellNames.auto(compiled.root()));
	}

	private static String nameOf(net.minecraft.world.entity.player.Player player, Spellbook book, int spell, List<RuneDef> runes,
			java.util.function.Supplier<String> auto) {
		String custom = book.name(spell);
		if (!custom.isEmpty()) {
			return custom;
		}
		return Heart.foundSecret(player, runes).map(Secrets.Secret::name)
			.or(() -> Heart.foundResonance(player, runes).map(resonance -> dev.wildercord.spell.SpellNames.clean(capital(resonance.name()))))
			.orElseGet(auto);
	}

	private static String capital(String text) {
		return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	/** Casts of each spell so far this session, per player, for Combo. */
	private static final java.util.Map<java.util.UUID, int[]> COMBO = new java.util.HashMap<>();

	public static void select(ServerPlayer player, int spell) {
		RelayCircles.cancel(player);
		CordTier tier = Spellbooks.tier(player);
		// Steps through the Cord's spells, and on to the tome's while it's in the off-hand.
		int index = dev.wildercord.gear.SpellSlots.resolve(tier == null ? 1 : tier.spells, dev.wildercord.gear.Gear.tome(player), spell);
		Spellbooks.set(player, Spellbooks.get(player).withSelected(index));
		List<RuneDef> runes = activeRunes(Spellbooks.get(player), index, tier);
		MutableComponent line = Component.translatable("message.wildercord.selected", index + 1).withStyle(ChatFormatting.AQUA);
		if (!runes.isEmpty()) {
			line.append(Component.literal("  "));
			for (int i = 0; i < runes.size(); i++) {
				if (i > 0) {
					line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
				}
				line.append(RuneItem.runeName(runes.get(i)).withColor(dev.wildercord.spell.RuneColors.of(runes.get(i))));
			}
		}
		player.sendOverlayMessage(line);
	}

	/**
	 * Saves an edited spell after checking it against the worn Cord. New runes must be learned,
	 * fit in the Cord's sockets, and be a tier the Cord can hold. Runes already threaded are
	 * never deleted by a smaller Cord: they may be reordered or removed, and stay quiet.
	 *
	 * @return null if everything was accepted, otherwise why something was left out
	 */
	public static Component edit(ServerPlayer player, int spell, List<String> runeIds) {
		RelayCircles.cancel(player);
		if (dev.wildercord.spell.RelayRules.containsIds(runeIds)) {
			if (runeIds.size() > CordTier.MAX_SOCKETS)
				return Component.literal("This Relay draft contains too many runes; the accepted row is restored.");
			if (!dev.wildercord.player.MasterStudies.knowsRelay(player) || !dev.wildercord.player.MasterStudies.eligibleRelay(player))
				return Component.literal("Study Relay Circle at the quiet Archive lectern with eight active Heart Circles.");
			if (Spellbooks.tier(player) != CordTier.ECHO || !dev.wildercord.gear.Gear.spellOpen(player, CordTier.ECHO, spell))
				return Component.literal("Relay needs an Echo Cord and an open ordinary spell slot.");
			if (runeIds.stream().anyMatch(id -> !Spellbooks.knows(player, id)))
				return Component.literal("Learn both runes before threading this Relay spell.");
			var runes = runeIds.stream().map(Runes::get).flatMap(Optional::stream).toList();
			if (runes.size() != runeIds.size()) return Component.literal("This Relay draft contains unreadable runes; the accepted row is restored.");
			// An entitled in-progress draft stays exactly as written and cannot cast until the whole grammar is valid.
			Spellbooks.set(player, Spellbooks.get(player).withSpell(spell, List.copyOf(runeIds)));
			return dev.wildercord.spell.RelayRules.valid(runes) ? null : Component.literal(dev.wildercord.spell.RelayRules.GRAMMAR_PROBLEM);
		}
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			return Component.translatable("message.wildercord.no_cord");
		}
		if (!dev.wildercord.gear.Gear.spellOpen(player, tier, spell)) {
			return locked(spell);
		}
		Spellbook book = Spellbooks.get(player);
		List<String> old = book.spells().get(spell);
		int limit = Math.min(CordTier.MAX_SOCKETS, Math.max(tier.sockets, old.size()));
		List<String> kept = new ArrayList<>();
		Component problem = null;
		for (String id : runeIds) {
			if (kept.size() >= limit) {
				problem = Component.translatable("message.wildercord.sockets_full", Component.translatable(tier.itemKey()), tier.sockets);
				break;
			}
			if (old.contains(id)) {
				kept.add(id);
				continue;
			}
			// Known first: only then is the id looked up (a Knot's id is a whole spell to decode).
			Optional<RuneDef> rune = book.knows(id) ? Runes.get(id) : Optional.empty();
			if (rune.isEmpty()) {
				problem = Component.translatable("message.wildercord.not_learned", id);
			} else if (!tier.holds(rune.get().tier())) {
				problem = Component.translatable("message.wildercord.too_strong", RuneItem.runeName(rune.get()),
					Component.translatable(CordTier.forRuneTier(rune.get().tier()).itemKey()));
			} else {
				kept.add(id);
			}
		}
		Spellbooks.set(player, book.withSpell(spell, kept));
		return problem;
	}

	/**
	 * Saves an edited passive. New runes must be learned, held by the Cord, allowed in passives
	 * and fit its passive sockets; the slot must be open (the 1st Circle opens one, the 5th a second).
	 *
	 * @return null if everything was accepted, otherwise why something was left out
	 */
	public static Component editPassive(ServerPlayer player, int slot, List<String> runeIds) {
		if (dev.wildercord.spell.RelayRules.containsIds(runeIds)) return Component.literal("Relay Circle cannot be sustained as a passive.");
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			return Component.translatable("message.wildercord.no_cord");
		}
		int slots = dev.wildercord.spell.Passives.slots(dev.wildercord.player.Heart.active(player));
		if (slot < 0 || slot >= dev.wildercord.spell.Passives.MAX || slot >= slots) {
			return Component.translatable("message.wildercord.passive_locked", dev.wildercord.spell.Circles.ordinal(
				dev.wildercord.spell.Passives.circleFor(Math.max(0, Math.min(slot, dev.wildercord.spell.Passives.MAX - 1)))));
		}
		Spellbook book = Spellbooks.get(player);
		List<String> old = book.passives().get(slot);
		int limit = Math.min(dev.wildercord.spell.Passives.SOCKETS, Math.max(PassiveCaster.sockets(tier), old.size()));
		List<String> kept = new ArrayList<>();
		Component problem = null;
		for (String id : runeIds) {
			if (kept.size() >= limit) {
				problem = Component.translatable("message.wildercord.passive_full", PassiveCaster.sockets(tier));
				break;
			}
			if (old.contains(id)) {
				kept.add(id);
				continue;
			}
			Optional<RuneDef> rune = book.knows(id) ? Runes.get(id) : Optional.empty();
			if (rune.isEmpty()) {
				problem = Component.translatable("message.wildercord.not_learned", id);
			} else if (!tier.holds(rune.get().tier())) {
				problem = Component.translatable("message.wildercord.too_strong", RuneItem.runeName(rune.get()),
					Component.translatable(CordTier.forRuneTier(rune.get().tier()).itemKey()));
			} else if (!dev.wildercord.spell.Passives.allowed(rune.get())) {
				problem = Component.translatable("message.wildercord.not_sustainable", RuneItem.runeName(rune.get()));
			} else {
				kept.add(id);
			}
		}
		Spellbooks.set(player, book.withPassive(slot, kept));
		return problem;
	}

	/** Switches a passive on or off. */
	public static void togglePassive(ServerPlayer player, int slot) {
		if (slot < 0 || slot >= dev.wildercord.spell.Passives.MAX) {
			return;
		}
		Spellbook book = Spellbooks.get(player);
		boolean on = !book.passiveOn(slot);
		Spellbooks.set(player, book.withPassiveOn(slot, on));
		player.sendOverlayMessage(Component.translatable(on ? "message.wildercord.passive_on" : "message.wildercord.passive_off", slot + 1)
			.withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GRAY));
	}

	private static void fail(ServerPlayer player, Component message) {
		player.sendOverlayMessage(message.copy().withStyle(ChatFormatting.RED));
		// A quiet fizzle, so a cast that didn't go off is heard as well as read.
		dev.wildercord.cast.feel.Feels.sound(player.level(), player.position(), "fizzle", 0.4F, 1.0F);
	}

	/** Mana regeneration and the starter runes, checked every few ticks. */
	public static void init() {
		// What the server remembers about a player goes when they leave, and about anything when it stops.
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.player;
			java.util.UUID id = player.getUUID();
			Charging.forget(player);
			PassiveCaster.forget(id);
			Overcast.forget(id);
			WildSurge.forget(id);
			Meditation.forget(id);
			Attunement.forget(id);
			HeartCircles.forget(id);
			SecretSpells.forget(id);
			COMBO.remove(id);
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			COMBO.clear();
			Charging.clear();
			Overcast.clear();
			WildSurge.clear();
			Meditation.clear();
			Reactions.clear();
			RuneBolt.clearLive();
			Effects.clearWards();
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// A charge in hand is looked after every tick: its overchannel's beats must land where the HUD says.
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE)) {
					Charging.everyTick(player);
				}
			}
			if (server.getTickCount() % 5 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player, server.getTickCount());
			}
			if (server.getTickCount() % 200 == 0) {
				Reactions.sweep(server.overworld().getGameTime());
				COMBO.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
			}
		});
	}

	private static void tickPlayer(ServerPlayer player, int tickCount) {
		Meditation.tick(player);
		// Before the Cord check: a charge whose Cord came off fizzles.
		Charging.tick(player);
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			return;
		}
		HeartCircles.tick(player, player.getAttachedOrElse(dev.wildercord.player.WildercordAttachments.MEDITATING, false));
		// A Blank Rune held while meditating in the right place attunes to the land's rune.
		Attunement.tick(player);
		PassiveCaster.tick(player, tickCount);
		Overcast.tick(player);
		Rhythm.tick(player);
		LeyWalker.tick(player);
		if (Heart.circles(player) > 0 && Heart.innate(player).isEmpty() && !HeartCircles.awakening(player)) {
			// Casters who formed their 1st Circle before innate runes existed get theirs now.
			Innates.awaken(player);
		}
		Spellbook book = Spellbooks.get(player);
		if (!book.starterGiven()) {
			Spellbook next = book;
			for (String id : Runes.STARTER) {
				next = next.learn(id);
			}
			if (next.spells().getFirst().isEmpty()) {
				next = next.withSpell(0, List.of(Runes.BOLT.id(), Runes.PUSH.id()));
			}
			Spellbooks.set(player, next.withStarterGiven());
			Spellbooks.setMana(player, Mana.max(player));
			player.sendSystemMessage(Component.translatable("message.wildercord.first_cord").withStyle(ChatFormatting.AQUA));
			return;
		}
		Mana.Stats stats = Mana.of(player);
		float mana = Spellbooks.mana(player);
		float next = Math.min(stats.max(), mana + stats.regen() * (float) Spirits.upkeep(player) / 4.0F);
		if (mana > stats.max()) {
			next = stats.max();
		}
		if (next != mana) {
			Spellbooks.setMana(player, next);
		}
	}
}
