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
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			fail(player, Component.translatable("message.wildercord.no_cord"));
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
		List<RuneDef> runes = activeRunes(book, spell, tier);
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
		if (runes.isEmpty() || compiled.isEmpty()) {
			fail(player, Component.translatable("message.wildercord.spell_empty", spell + 1));
			return;
		}
		Optional<Secrets.Secret> secret = Secrets.match(runes);
		long now = player.level().getGameTime();
		long readyAt = Spellbooks.readyAt(player, spell);
		// A wild surge's Free Recast: this one costs nothing and waits for no cooldown.
		boolean free = WildSurge.freeRecast(player, now);
		if (now < readyAt && !free) {
			Rhythm.early(player, now);
			fail(player, Component.translatable("message.wildercord.cooldown", String.format(java.util.Locale.ROOT, "%.1f", (readyAt - now) / 20.0)));
			return;
		}
		// Add-ons may stop a cast here, before anything is spent.
		int asked = compiled.paysInHealth() ? 0 : (int) Math.ceil(Heart.manaCost(player, compiled) * secret.map(Secrets.Secret::power).orElse(1.0) - 1e-9);
		if (!dev.wildercord.api.WildercordEvents.BEFORE_CAST.invoker().allow(player, spell, List.copyOf(runes), asked)) {
			return;
		}
		float manaNow = Spellbooks.mana(player);
		boolean overflow = manaNow >= Mana.max(player) - 0.5F;
		Heart.Bonuses bonuses = Heart.bonuses(player, overflow);
		int spent;
		// Set when this cast is an overcast: the mana there was and what it cost, for wild magic.
		float overcastMana = -1;
		int overcastCost = -1;
		if (free) {
			spent = 0;
			WildSurge.useFreeRecast(player);
		} else if (compiled.paysInHealth()) {
			// Blood Price: paid in health, and never enough to kill you.
			int blood = Heart.healthCost(player, compiled);
			spent = blood * 5;
			if (!player.isCreative() && player.getHealth() <= blood) {
				fail(player, Component.translatable("message.wildercord.no_health", blood));
				return;
			}
			if (!player.isCreative()) {
				player.setHealth(player.getHealth() - blood);
				Fx.sound(player.level(), player.position(), SoundEvents.PLAYER_HURT, 0.6F, 0.7F);
				Vfx.emit(player.level(), net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, player.getBoundingBox().getCenter(), 4, 0.3, 0.1);
			}
		} else {
			int cost = (int) Math.ceil(Heart.manaCost(player, compiled) * secret.map(Secrets.Secret::power).orElse(1.0) - 1e-9);
			spent = cost;
			float mana = Spellbooks.mana(player);
			if (!player.isCreative() && mana < cost) {
				// Not enough: a second press within two seconds overcasts, cracking a circle to pay.
				if (!Overcast.confirm(player, spell, (int) mana, cost)) {
					return;
				}
				spent = (int) mana;
				overcastMana = mana;
				overcastCost = cost;
				Spellbooks.setMana(player, 0);
			} else if (!player.isCreative()) {
				Spellbooks.setMana(player, mana - cost);
			}
		}
		int cooldown = Heart.cooldownTicks(player, compiled);
		if (secret.isPresent()) {
			cooldown = cooldown * 3 / 2;
		}
		Spellbooks.setReadyAt(player, spell, now + cooldown);
		HeartCircles.condense(player, spent);
		double rhythm = Rhythm.onCast(player, now, cooldown);
		double charged = 1 + Charging.POWER * Math.max(0, Math.min(1, charge));
		bonuses = bonuses.withPower(bonuses.power() * rhythm * charged);
		if (charge >= 1.0) {
			Grimoire.feat(player, dev.wildercord.spell.Feats.CHARGED);
		}
		dev.wildercord.advancement.Advancements.cast(player, runes.size());
		String leaning = countElements(player, runes);
		int castNumber = COMBO.computeIfAbsent(player.getUUID(), k -> new int[dev.wildercord.gear.SpellSlots.ALL])[spell] += 1;
		player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		Vfx.Theme theme = secret.map(s -> Vfx.themeOf(s.color())).orElse(compiled.root().groups.isEmpty() ? Vfx.theme("") : Vfx.theme(compiled.root().groups.getFirst()));
		Vfx.castCircle(player, theme, runes);
		// Casting gear, read from the hands now: a staff's flourish on a charged cast, and its power on every part of the spell.
		dev.wildercord.gear.GearBonuses gear = dev.wildercord.gear.Gear.of(player);
		dev.wildercord.gear.Gear.flourish(player, gear, dev.wildercord.gear.GearBonuses.elements(compiled.root()), charge);
		// Everyone around sees the casting pose for this spell's shape.
		player.setAttached(dev.wildercord.player.WildercordAttachments.CAST_POSE, new dev.wildercord.player.WildercordAttachments.CastPose(runes.getFirst().id(), now));
		HeartCircles.onCast(player);
		// Under a mana storm, a spell may surge (see cast.events.ManaStorm).
		dev.wildercord.cast.events.EventRules.Surge surge = dev.wildercord.cast.events.ManaStorm.surge(player);
		bonuses = bonuses.withPower(bonuses.power() * dev.wildercord.cast.events.EventRules.surgePower(surge));
		Cast.Info info = new Cast.Info(compiled.root(), runes.size(), leaning, List.copyOf(runes));
		Cast cast = new Cast(player, castNumber, bonuses, false, null, info).weigh(compiled.cost() * secret.map(Secrets.Secret::power).orElse(1.0)).gear(gear);
		if (secret.isPresent()) {
			SecretSpells.discover(player, secret.get());
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
		// Wild magic: an overcast spell may twist into something else.
		if (overcastCost < 0 || !WildSurge.overcast(cast, runes, secret.isPresent(), overcastMana, overcastCost, release)) {
			release.accept(cast);
		}
		dev.wildercord.runesmith.Contracts.onCast(player, runes);
		Heart.Bonuses surged = bonuses;
		dev.wildercord.cast.events.ManaStorm.afterCast(player, surge, runes, () -> {
			Cast echo = new Cast(player, castNumber, surged, false, null, info);
			if (secret.isPresent()) {
				SecretSpells.cast(echo, secret.get());
			} else {
				CastEngine.cast(echo, compiled.root());
			}
		});
		// Twin Star: the next spell goes off a second time, a moment later.
		if (Innates.consumeTwin(player)) {
			Heart.Bonuses twin = bonuses;
			Scheduler.later(8, () -> {
				if (!player.isRemoved() && player.isAlive()) {
					TechniqueVfx.twinStar(player.level(), player);
					Cast again = new Cast(player, castNumber, twin, false, null, info).gear(gear);
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
			Heart.Bonuses echoed = bonuses;
			Scheduler.later(10, () -> {
				if (!player.isRemoved() && player.isAlive()) {
					HeartCircles.onCast(player);
					Cast again = new Cast(player, castNumber, echoed, false, null, info).gear(gear);
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

	/** Why a spell slot can't be used: the Cord has too few spells, or the tome's slot without the tome in hand. */
	private static Component locked(int spell) {
		if (spell == dev.wildercord.gear.SpellSlots.TOME) {
			return Component.translatable("message.wildercord.tome_needed");
		}
		return Component.translatable("message.wildercord.spell_needs", spell + 1, Component.translatable(CordTier.forSpells(spell + 1).itemKey()));
	}

	/**
	 * Counts this cast toward each element in it, for elemental leaning, and returns the element
	 * the caster now leans toward ("" for none). Tells the player when a leaning first appears.
	 */
	private static String countElements(ServerPlayer player, List<RuneDef> runes) {
		java.util.Set<String> elements = new java.util.HashSet<>();
		// A Knot's runes count as if they were threaded one by one.
		for (RuneDef rune : dev.wildercord.spell.Knots.flatten(runes)) {
			if (rune.family() == dev.wildercord.spell.RuneFamily.EFFECT && !rune.element().isEmpty()) {
				elements.add(rune.element());
			}
		}
		String before = Heart.leaning(player);
		if (elements.isEmpty()) {
			return before;
		}
		java.util.Map<String, Integer> counts = new java.util.HashMap<>(Heart.elementCasts(player));
		for (String element : elements) {
			counts.merge(element, 1, Integer::sum);
		}
		player.setAttached(dev.wildercord.player.WildercordAttachments.ELEMENT_CASTS, java.util.Map.copyOf(counts));
		String after = dev.wildercord.spell.Leaning.of(counts);
		if (!after.isEmpty() && !after.equals(before)) {
			player.sendSystemMessage(Component.translatable("message.wildercord.leaning", Component.translatable("element.wildercord." + after))
				.withColor(dev.wildercord.spell.RuneColors.element(after)));
			Grimoire.feat(player, dev.wildercord.spell.Feats.LEANING);
		}
		return after;
	}

	/** Gives a spell a custom name, or clears it back to the automatic one. */
	public static void rename(ServerPlayer player, int spell, String name) {
		if (spell < 0 || spell >= dev.wildercord.gear.SpellSlots.ALL) {
			return;
		}
		Spellbooks.set(player, Spellbooks.get(player).withName(spell, dev.wildercord.spell.SpellNames.clean(name)));
	}

	/** A spell's name: the custom one, or one made from its runes. */
	public static String nameOf(Spellbook book, int spell, List<RuneDef> runes) {
		String custom = book.name(spell);
		if (!custom.isEmpty()) {
			return custom;
		}
		Optional<Secrets.Secret> secret = Secrets.match(runes);
		return secret.map(Secrets.Secret::name).orElseGet(() -> dev.wildercord.spell.SpellNames.auto(runes));
	}

	/** Casts of each spell so far this session, per player, for Combo. */
	private static final java.util.Map<java.util.UUID, int[]> COMBO = new java.util.HashMap<>();

	public static void select(ServerPlayer player, int spell) {
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
			boolean alreadyThreaded = old.contains(id);
			Optional<RuneDef> rune = Runes.get(id);
			if (alreadyThreaded) {
				kept.add(id);
			} else if (rune.isEmpty() || !book.knows(id)) {
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
			Optional<RuneDef> rune = Runes.get(id);
			if (old.contains(id)) {
				kept.add(id);
			} else if (rune.isEmpty() || !book.knows(id)) {
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
		float next = Math.min(stats.max(), mana + stats.regen() / 4.0F);
		if (mana > stats.max()) {
			next = stats.max();
		}
		if (next != mana) {
			Spellbooks.setMana(player, next);
		}
	}
}
