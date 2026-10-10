package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.ManaSkinRules;
import dev.wildercord.player.ManaSkinDamage;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Circles;
import dev.wildercord.spell.CondenseRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Heart Circles on the server: mana spent condenses toward the next circle; once the heart is
 * ready the player is told, and meditating (sneak and stand still) for ten seconds forms it (five on a ley line).
 * Also the 3rd Circle's Mana Skin, boss kills for the 7th's breakthrough, and the rings
 * themselves: they turn around the heart while you meditate and spin up whenever you cast.
 */
public final class HeartCircles {
	private HeartCircles() {}

	/** The first eight colours stay intact; the outer master rings return through opal to white gold. */
	private static final int[] COLORS = {0x3F6BFF, 0x5A5BFF, 0x7E52FF, 0xA64FF0, 0xD35CD0, 0xF08A8A, 0xF5C46A, 0xFFF3D0,
		0xB8E5FF, 0xFFC89A, 0xDAC7FF, 0x93E8E2, 0xACBAFF, 0xE1D4FF,
		0xF8BDE6, 0xB3EDC5, 0xD5E7FF, 0xFFF09A, 0xFFE2CE, 0xFFFBE8};

	private static final Map<UUID, Integer> FORMING = new HashMap<>();
	/** Who last hurt each creature with a spell, and when: a monster dying soon after counts as a spell kill. */
	private record SpellHit(UUID caster, long time, int runes) {}
	private static final Map<UUID, SpellHit> LAST_SPELL_HIT = new HashMap<>();
	private static final Map<UUID, Integer> NOTIFIED = new HashMap<>();
	private static final Map<UUID, Float> CONDENSING = new HashMap<>();
	/** Mana spent that waits for one of the caster's spells to hurt a hostile creature (see {@link CondenseRules}). */
	private record Pending(float mana, long until) {}
	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	/** When each player's recent spell kills counted, for the farm decay. */
	private static final Map<UUID, java.util.ArrayDeque<Long>> RECENT_KILLS = new HashMap<>();
	/** Damage each player has dealt each living boss, for its breakthrough. */
	private static final Map<UUID, Map<UUID, Float>> BOSS_DAMAGE = new HashMap<>();
	/** Players whose innate rune wakes in a moment, after the 1st Circle's title has been read. */
	private static final java.util.Set<UUID> AWAKENING = new java.util.HashSet<>();
	private static final java.util.Set<UUID> VOW_REMINDED = new java.util.HashSet<>();

	/** The colour of circle {@code n} (1-based). */
	public static int color(int n) {
		return COLORS[Math.max(1, Math.min(COLORS.length, n)) - 1];
	}

	public static void init() {
		dev.wildercord.net.BreakthroughPayload.init();
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (damage > 0 && Spirits.isBoss(entity) && source.getEntity() instanceof ServerPlayer hitter) {
				BOSS_DAMAGE.computeIfAbsent(entity.getUUID(), k -> new HashMap<>()).merge(hitter.getUUID(), damage, Float::sum);
			}
			if (entity instanceof ServerPlayer player && damage > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				ManaSkinDamage.Wound wound = ManaSkinDamage.take(player, source);
				if (wound != null) manaSkin(player, wound);
				// Forming a circle takes unbroken concentration.
				if (FORMING.remove(player.getUUID()) != null) {
					player.sendOverlayMessage(Component.translatable("message.wildercord.circle_broken").withStyle(ChatFormatting.RED));
					Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 0.6F);
				}
			}
		});
		// Monsters defeated with spells: a breakthrough for the higher circles.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			SpellHit hit = LAST_SPELL_HIT.remove(entity.getUUID());
			if (hit == null || !(entity instanceof net.minecraft.world.entity.monster.Enemy) || entity.level().getGameTime() - hit.time() > 100
					|| !(entity.level() instanceof ServerLevel level)) {
				return;
			}
			ServerPlayer caster = level.getServer().getPlayerList().getPlayer(hit.caster());
			if (caster != null && farmCounts(caster, level.getGameTime())) {
				caster.setAttached(WildercordAttachments.SPELL_KILLS, Heart.spellKills(caster) + 1);
				if (hit.runes() >= 6) {
					Grimoire.feat(caster, dev.wildercord.spell.Feats.LONG_SPELL_KILL);
				}
			}
		});
		// Everyone who truly fought shares a boss kill: it's the 7th Circle's breakthrough. Standing by is not enough.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			Map<UUID, Float> dealt = BOSS_DAMAGE.remove(entity.getUUID());
			if (!Spirits.isBoss(entity) || !(entity.level() instanceof ServerLevel level) || dealt == null) {
				return;
			}
			for (ServerPlayer player : level.players()) {
				if (player.isAlive() && !player.isSpectator() && player.distanceTo(entity) <= 96 && !Heart.bossSlain(player)
						&& CondenseRules.bossCredit(dealt.getOrDefault(player.getUUID(), 0.0F), entity.getMaxHealth())) {
					player.setAttached(WildercordAttachments.BOSS_SLAIN, true);
					player.sendSystemMessage(Component.translatable("message.wildercord.boss_breakthrough").withStyle(ChatFormatting.GOLD));
				}
			}
		});
		// A spell kill only counts for a caster still here, so a leaver's marks on creatures go with them.
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			LAST_SPELL_HIT.values().removeIf(hit -> hit.caster().equals(id));
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			FORMING.clear();
			LAST_SPELL_HIT.clear();
			NOTIFIED.clear();
			CONDENSING.clear();
			PENDING.clear();
			RECENT_KILLS.clear();
			BOSS_DAMAGE.clear();
			AWAKENING.clear();
		});
	}

	/** Remembers that a spell of {@code caster}'s hurt {@code target}, so its death can count as a spell kill. */
	static void hurtBySpell(Cast cast, net.minecraft.world.entity.LivingEntity target) {
		if (!(cast.caster instanceof ServerPlayer)) {
			return;
		}
		LAST_SPELL_HIT.put(target.getUUID(), new SpellHit(cast.caster.getUUID(), target.level().getGameTime(), cast.info.runes()));
		if (target instanceof net.minecraft.world.entity.monster.Enemy && cast.caster instanceof ServerPlayer player) {
			release(player, target.level().getGameTime());
		}
		if (LAST_SPELL_HIT.size() > 4096) {
			long now = target.level().getGameTime();
			LAST_SPELL_HIT.values().removeIf(h -> now - h.time() > 100);
		}
	}

	/**
	 * Mana spent casting spells condenses toward the next circle: a tenth at once, the rest only once a spell of the caster's
	 * hurts a hostile creature soon after (see {@link CondenseRules}).
	 */
	public static void condense(ServerPlayer player, float mana) {
		if (!Float.isFinite(mana) || mana <= 0 || player.isCreative()) {
			return;
		}
		// A Condensing elixir: more of what was spent counts toward the next circle.
		mana *= (float) dev.wildercord.player.ElixirRules.condense(
			dev.wildercord.content.WildercordEffects.level(player, dev.wildercord.content.WildercordEffects.CONDENSING));
		// Fellow coven members casting close by.
		mana *= (float) dev.wildercord.guild.Guilds.bonus(player, dev.wildercord.guild.GuildRules.Kind.COVEN);
		long now = player.level().getGameTime();
		Pending waiting = PENDING.get(player.getUUID());
		float before = waiting == null || now > waiting.until() ? 0 : waiting.mana();
		PENDING.put(player.getUUID(), new Pending(CondenseRules.addPending(before, CondenseRules.pending(mana)), now + CondenseRules.PENDING_TICKS));
		add(player, CondenseRules.immediate(mana));
	}

	/** A spell of the caster's hurt a hostile creature: what was waiting condenses. */
	private static void release(ServerPlayer player, long now) {
		Pending waiting = PENDING.remove(player.getUUID());
		if (waiting != null && now <= waiting.until() && !player.isCreative()) {
			add(player, waiting.mana());
		}
	}

	/** Whether this spell kill counts: a farm's steady stream mostly doesn't. */
	private static boolean farmCounts(ServerPlayer player, long now) {
		java.util.ArrayDeque<Long> recent = RECENT_KILLS.computeIfAbsent(player.getUUID(), k -> new java.util.ArrayDeque<>());
		while (!recent.isEmpty() && now - recent.peekFirst() > CondenseRules.KILL_WINDOW_TICKS) {
			recent.pollFirst();
		}
		if (player.getRandom().nextDouble() >= CondenseRules.killChance(recent.size())) {
			return false;
		}
		recent.addLast(now);
		return true;
	}

	private static void add(ServerPlayer player, float mana) {
		if (mana <= 0) {
			return;
		}
		double total = (double) CONDENSING.getOrDefault(player.getUUID(), 0.0F) + mana;
		int whole = (int) Math.min(Integer.MAX_VALUE, Math.floor(total));
		CONDENSING.put(player.getUUID(), (float) (total - Math.floor(total)));
		if (whole > 0) {
			player.setAttached(WildercordAttachments.CONDENSED, Circles.addCondensed(Heart.condensed(player), whole));
		}
	}

	/** Every 5 ticks: tell the player when the heart is ready, and form the circle while they meditate. */
	public static void tick(ServerPlayer player, boolean meditating) {
		UUID id = player.getUUID();
		int circles = Heart.circles(player);
		boolean ready = Heart.ready(player);
		int next = circles + 1;
		if (ready && NOTIFIED.getOrDefault(id, 0) != next) {
			NOTIFIED.put(id, next);
			player.sendSystemMessage(Component.translatable("message.wildercord.circle_ready", Circles.ordinal(next)).withColor(0xF5C46A));
			if (dev.wildercord.spell.TribulationRules.tribulation(next) && player.level().getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL) {
				player.sendSystemMessage(Component.translatable("message.wildercord.tribulation.warn", Circles.ordinal(next),
					dev.wildercord.spell.TribulationRules.waves(next)).withColor(0xE8D8B0));
			}
			Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.6F);
		}
		if (meditating && circles > 0 && !ready) {
			rings(player, circles, player.level().getGameTime() * 0.05, 0.45F);
		}
		// A heart that already holds a vow circle (an older save, or a choice put off) is reminded once per session while meditating.
		if (meditating && VOW_REMINDED.add(id)) {
			java.util.List<dev.wildercord.spell.CircleVows.Vow> open = dev.wildercord.spell.CircleVows.open(Heart.vows(player), Heart.active(player));
			if (!open.isEmpty()) CircleVowCommands.offer(player, open.getFirst().circle());
			else HeartPathCommands.offer(player);
		}
		// Past the Twentieth Circle, condensed mana pays for Ascensions, formed the same way.
		if (!ready && circles >= Circles.MAX && Heart.ascensionReady(player)) {
			int rank = Heart.ascension(player) + 1;
			if (NOTIFIED.getOrDefault(id, 0) != -rank) {
				NOTIFIED.put(id, -rank);
				player.sendSystemMessage(Component.translatable("message.wildercord.ascension.ready", dev.wildercord.spell.AscensionRules.numeral(rank)).withColor(0xF5C46A));
				Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.4F);
			}
			if (meditating && !dev.wildercord.cast.events.Tribulation.active(player)) {
				int progress = FORMING.merge(id, player.getAttachedOrElse(WildercordAttachments.ON_LEY, false) ? 10 : 5, Integer::sum);
				forming(player, circles - 1, progress);
				if (progress >= Circles.FORM_TICKS) {
					FORMING.remove(id);
					ascend(player);
				}
				return;
			}
		}
		if (!ready || !meditating || dev.wildercord.cast.events.Tribulation.active(player)) {
			FORMING.remove(id);
			return;
		}
		// On a ley line the world's own mana helps: circles form twice as fast.
		int progress = FORMING.merge(id, player.getAttachedOrElse(WildercordAttachments.ON_LEY, false) ? 10 : 5, Integer::sum);
		forming(player, circles, progress);
		if (progress >= Circles.FORM_TICKS) {
			FORMING.remove(id);
			// Every fifth circle is won in a tribulation, not simply formed.
			if (!dev.wildercord.cast.events.Tribulation.begin(player, next)) form(player);
		}
	}

	/** Forms the next earned Ascension. Recheck at the mutation boundary, including the cap. */
	public static void ascend(ServerPlayer player) {
		if (!Heart.ascensionReady(player)) return;
		int rank = Heart.ascension(player) + 1;
		player.setAttached(WildercordAttachments.ASCENSION, rank);
		Spellbooks.setMana(player, Mana.max(player));
		ServerLevel level = player.level();
		Vec3 heart = heartOf(player);
		int gold = COLORS[Circles.MAX - 1];
		Sigils.flash(level, heart.add(0, 0.6, 0), 0xFF000000 | gold, 3.0F);
		Vfx.radial(level, ParticleTypes.END_ROD, heart, 32, 0.4);
		Vfx.shockwave(level, player.position(), 4.5, Vfx.theme("time"), 8);
		Sigils.ground(level, player.position(), gold, 0xFFFFFF, 2.0F, 50);
		Fx.sound(level, heart, dev.wildercord.content.WildercordSounds.CIRCLE_FORMED, 1.0F, 0.7F);
		String numeral = dev.wildercord.spell.AscensionRules.numeral(rank);
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.wildercord.ascension", numeral).withColor(gold)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.wildercord.ascension.subtitle")));
		player.sendSystemMessage(Component.translatable("message.wildercord.ascension.formed", numeral, dev.wildercord.spell.AscensionRules.MANA,
			String.format(java.util.Locale.ROOT, "%.1f", dev.wildercord.spell.AscensionRules.REGEN), Math.round(dev.wildercord.spell.AscensionRules.POWER * 100)).withColor(gold));
		Component news = Component.translatable("message.wildercord.ascension.announce", player.getDisplayName(), numeral).withColor(gold);
		for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
			if (other != player) other.sendSystemMessage(news);
		}
	}

	/** Forms the next earned circle. Recheck at the mutation boundary, including the cap. */
	public static void form(ServerPlayer player) {
		if (!Heart.ready(player)) return;
		int n = Heart.circles(player) + 1;
		player.setAttached(WildercordAttachments.CIRCLES, n);
		dev.wildercord.advancement.Advancements.circles(player);
		dev.wildercord.player.TribulationScars.apply(player);
		Spellbooks.setMana(player, Mana.max(player));
		ServerLevel level = player.level();
		Vec3 heart = heartOf(player);
		// The flash goes through Fx.send, which keeps it out of the player's own face.
		Sigils.flash(level, heart.add(0, 0.6, 0), 0xFF000000 | COLORS[n - 1], 2.6F);
		Vfx.radial(level, ParticleTypes.END_ROD, heart, 24, 0.35);
		Vfx.shockwave(level, player.position(), 3.5, Vfx.theme("time"), 6);
		// The new circle breaks out of the heart in light, a circle opening under it: for everyone, you included.
		Sigils.ground(level, player.position(), COLORS[n - 1], COLORS[Circles.MAX - 1], 1.6F, 40);
		ElementFx.groundRing(level, player.position(), COLORS[n - 1], 0.3, 4.2, 0.09, 18);
		Fx.sendOthers(level, player, ElementFx.ringOption(UP, COLORS[n - 1], Circles.ringRadius(n), 3.0, 0.04, 12), heart);
		for (int t = 0; t < 10; t++) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				if (!player.isRemoved()) {
					rings(player, n, tick * 0.6, 0.5F + (10 - tick) * 0.05F);
				}
			});
		}
		Fx.sound(level, heart, dev.wildercord.content.WildercordSounds.CIRCLE_FORMED, 1.0F, 1.0F);
		Component title = Component.translatable("title.wildercord.circle", Circles.ordinal(n)).withColor(COLORS[n - 1]);
		Component subtitle = Component.translatable("title.wildercord.circle." + n);
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.sendSystemMessage(Component.translatable("message.wildercord.circle_formed", Circles.ordinal(n), Circles.MANA_PER_CIRCLE,
			String.format(java.util.Locale.ROOT, "%.1f", Circles.REGEN_PER_CIRCLE), Math.round(Circles.POWER_PER_CIRCLE * 100)).withColor(COLORS[n - 1]));
		Component perk = switch (n) {
			case 1, 5 -> Component.translatable("message.wildercord.passive_slot", dev.wildercord.spell.Passives.slots(n));
			default -> null;
		};
		if (perk != null) {
			player.sendSystemMessage(perk.copy().withColor(0xB8A8FF));
		}
		if (n == 2 || n == Circles.MANA_SKIN || n == 4 || n == Circles.FLOW || n == Circles.OVERFLOW || n == Circles.ARCHMAGE) {
			player.sendSystemMessage(Component.translatable("message.wildercord.perk." + n).withColor(0xF5C46A));
		}
		CircleVowCommands.offer(player, n);
		if (n == dev.wildercord.spell.HeartPaths.CIRCLE) HeartPathCommands.offer(player);
		// The great breakthroughs (each tribulation circle and the Archmage's) are heard across the world.
		if (dev.wildercord.spell.TribulationRules.tribulation(n) || n == Circles.ARCHMAGE) {
			Component news = Component.translatable("message.wildercord.breakthrough.announce.circle", player.getDisplayName(), Circles.ordinal(n)).withColor(COLORS[n - 1]);
			for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
				if (other != player) other.sendSystemMessage(news);
			}
		}
		// Once the title has had its moment, the breakthrough's page: what it brought and what the next circle asks.
		boolean tribulation = dev.wildercord.spell.TribulationRules.tribulation(n) && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL;
		Scheduler.later(70, () -> {
			if (!player.isRemoved() && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, dev.wildercord.net.BreakthroughPayload.TYPE)) {
				net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new dev.wildercord.net.BreakthroughPayload(n, tribulation));
			}
		});
		if (n == Circles.ARCHMAGE) {
			player.sendSystemMessage(Component.translatable("message.wildercord.relay_lesson.invitation").withColor(0x7FDAD4));
			player.sendSystemMessage(Component.translatable("message.wildercord.masters_trials.invitation").withColor(0xE8C46A));
		}
		if (n == 1) {
			// The heart's first ring wakes something only this caster has.
			AWAKENING.add(player.getUUID());
			Scheduler.later(60, () -> {
				AWAKENING.remove(player.getUUID());
				if (!player.isRemoved()) {
					Innates.awaken(player);
				}
			});
		}
	}

	/**
	 * Your circles turn when you cast: a quick spin for everyone around to see. Not for you: it
	 * happens on every cast, and around your own chest it fills the bottom of a first-person view.
	 */
	public static void onCast(ServerPlayer player) {
		int circles = Heart.circles(player);
		if (circles <= 0) {
			return;
		}
		for (int t = 0; t < 2; t++) {
			int tick = t;
			Scheduler.later(1 + t * 3, () -> {
				if (!player.isRemoved()) {
					lightRings(player, circles, player.level().getGameTime() * 0.35 + tick, tick == 0 ? 1.6 : 1.25);
				}
			});
		}
	}

	/**
	 * The rings in light, shown to everyone but {@code player}: each settles onto its circle from
	 * {@code from} times its size, on its tilt at {@code spin}, round a small glowing heart.
	 */
	private static void lightRings(ServerPlayer player, int circles, double spin, double from) {
		ServerLevel level = player.level();
		Vec3 heart = heartOf(player);
		Fx.sendOthers(level, player, SigilOption.glow(0xFFE0A0, 0.5F), heart);
		for (int i = 0; i < Math.min(Circles.MAX, circles); i++) {
			double r = Circles.ringRadius(i + 1);
			Fx.sendOthers(level, player, ElementFx.ringOption(ringNormal(i, spin), ringColor(player, i), r * from, r, 0.02, 8), heart);
		}
	}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/**
	 * Consumes this hit's native wound once, at the original AFTER_DAMAGE callback position.
	 * A fifth of this nonlethal health wound is restored, after armour, resistance and absorption.
	 * A lethal wound receives no rebate: Reversal and totems own their later death-save recovery.
	 */
	private static void manaSkin(ServerPlayer player, ManaSkinDamage.Wound wound) {
		if (Heart.active(player) < Circles.MANA_SKIN || player.isCreative() || !player.isAlive()
				|| Spellbooks.tier(player) == null) {
			return;
		}
		float mana = Spellbooks.mana(player);
		float healthAfter = player.getHealth();
		float share = ManaSkinRules.recovery(wound.healthBefore(), wound.healthAfter(), healthAfter, mana,
			dev.wildercord.spell.HeartPaths.skinShare(Heart.path(player), Heart.active(player)));
		if (share <= 0) {
			return;
		}
		// This is the defender's passive, not healing performed by the incoming spell's caster.
		Effects.withSource(player, () -> player.heal(share));
		float cost = ManaSkinRules.payment(player.getHealth() - healthAfter, mana);
		if (cost <= 0) return;
		Spellbooks.setMana(player, mana - cost);
		Vfx.emit(player.level(), new DustParticleOptions(0x7FB0FF, 0.8F), player.getBoundingBox().getCenter(), 4, 0.35, 0.0);
		ElementFx.ring(player.level(), player.getBoundingBox().getCenter(), UP, 0x7FB0FF, 0.9, 0.45, 0.03, 7);
	}

	private static Vec3 heartOf(ServerPlayer player) {
		return player.position().add(0, player.isShiftKeyDown() ? 0.95 : 1.2, 0);
	}

	/**
	 * The rings: one per circle around the heart, each on its own tilt and turning its own way,
	 * like a gyroscope. Inner rings are deep blue; the outer ones burn toward white gold.
	 * Only onlookers receive these chest effects; in first person they overlap the camera.
	 */
	static void rings(ServerPlayer player, int circles, double spin, float size) {
		ServerLevel level = player.level();
		Vec3 heart = heartOf(player);
		Fx.sendOthers(level, player, new DustParticleOptions(0xFFE0A0, 0.8F), heart);
		for (int i = 0; i < Math.min(Circles.MAX, circles); i++) {
			double r = Circles.ringRadius(i + 1);
			// One bounded ring primitive per circle avoids quadratic per-point packet growth at twenty.
			Fx.sendOthers(level, player, ElementFx.ringOption(ringNormal(i, spin), ringColor(player, i),
				r, r, Math.max(0.008, size * 0.025), 6), heart);
		}
	}

	/** Ring {@code i}'s normal at {@code spin}: straight up, tipped over by its tilt toward a direction that turns with the spin. */
	private static Vec3 ringNormal(int i, double spin) {
		double phi = spin * (1 + 0.25 * i) + i * 0.8;
		double tilt = 0.3 + 0.14 * (i % 3);
		return new Vec3(Math.cos(phi) * Math.sin(tilt), Math.cos(tilt), Math.sin(phi) * Math.sin(tilt)).normalize();
	}

	/** A ring's colour: blue to white gold from the inside out, drawn halfway toward the element the caster leans to. */
	private static int ringColor(ServerPlayer player, int ring) {
		String leaning = Heart.leaning(player);
		int base = COLORS[ring];
		if (leaning.isEmpty()) {
			return base;
		}
		int tint = dev.wildercord.spell.RuneColors.element(leaning);
		int r = (((base >> 16) & 0xFF) + ((tint >> 16) & 0xFF)) / 2;
		int g = (((base >> 8) & 0xFF) + ((tint >> 8) & 0xFF)) / 2;
		int b = ((base & 0xFF) + (tint & 0xFF)) / 2;
		return (r << 16) | (g << 8) | b;
	}

	/** While a circle forms: the existing rings spin faster, mana streams in, and the new ring draws itself. */
	private static void forming(ServerPlayer player, int circles, int progress) {
		ServerLevel level = player.level();
		Vec3 heart = heartOf(player);
		double t = progress / (double) Circles.FORM_TICKS;
		rings(player, circles, level.getGameTime() * (0.05 + 0.25 * t), 0.45F);
		double r = Circles.ringRadius(circles + 1);
		int points = (int) Math.round((12 + 3 * circles) * t);
		DustParticleOptions dust = new DustParticleOptions(COLORS[Math.min(Circles.MAX - 1, circles)], 0.55F);
		for (int k = 0; k < points; k++) {
			double a = Math.PI * 2 * k / (12 + 3 * circles);
			Fx.sendOthers(level, player, dust, heart.add(Math.cos(a) * r, 0, Math.sin(a) * r));
		}
		for (int i = 0; i < 3; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 from = heart.add(Math.cos(a) * 1.6, (level.getRandom().nextDouble() - 0.3) * 1.2, Math.sin(a) * 1.6);
			// A safe starting point alone does not keep a trail's destination out of the camera.
			Fx.sendOthers(level, player, new net.minecraft.core.particles.TrailParticleOption(heart, COLORS[Math.min(Circles.MAX - 1, circles)], 12), from);
		}
		if (progress % 20 == 0) {
			Fx.sound(level, heart, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 0.6F + (float) t);
			// Once a second the new ring draws itself in light, as far round as it has come.
			double mid = Math.PI * t;
			Fx.sendOthers(level, player, ElementFx.slashOption(UP, new Vec3(Math.cos(mid), 0, Math.sin(mid)), COLORS[Math.min(Circles.MAX - 1, circles)], r,
				Math.PI * 2 * t, 0.02, 6, 12), heart);
			// One thin progress arc at the feet replaces the stack of rings in the owner's view.
			Vec3 feet = player.position().add(0, 0.06, 0);
			Fx.sendParticles(level, player, ElementFx.slashOption(UP, new Vec3(Math.cos(mid), 0, Math.sin(mid)),
				COLORS[Math.min(Circles.MAX - 1, circles)], 0.65, Math.PI * 2 * t, 0.012, 6, 12),
				false, false, feet.x, feet.y, feet.z, 1, 0, 0, 0, 0);
		}
	}

	/** Whether this player's innate rune is about to wake (so nothing else should wake it first). */
	public static boolean awakening(ServerPlayer player) {
		return AWAKENING.contains(player.getUUID());
	}

	public static void forget(UUID player) {
		FORMING.remove(player);
		CONDENSING.remove(player);
		PENDING.remove(player);
		RECENT_KILLS.remove(player);
		NOTIFIED.remove(player);
		AWAKENING.remove(player);
		VOW_REMINDED.remove(player);
	}
}
