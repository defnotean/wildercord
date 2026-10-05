package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Circles;
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
	/** Players whose innate rune wakes in a moment, after the 1st Circle's title has been read. */
	private static final java.util.Set<UUID> AWAKENING = new java.util.HashSet<>();

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (entity instanceof ServerPlayer player && damage > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				manaSkin(player, damage);
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
			if (caster != null) {
				caster.setAttached(WildercordAttachments.SPELL_KILLS, Heart.spellKills(caster) + 1);
				if (hit.runes() >= 6) {
					Grimoire.feat(caster, dev.wildercord.spell.Feats.LONG_SPELL_KILL);
				}
			}
		});
		// Everyone nearby shares a boss kill: it's the 7th Circle's breakthrough.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!Spirits.isBoss(entity) || !(entity.level() instanceof ServerLevel level)) {
				return;
			}
			for (ServerPlayer player : level.players()) {
				if (player.isAlive() && !player.isSpectator() && player.distanceTo(entity) <= 96 && !Heart.bossSlain(player)) {
					player.setAttached(WildercordAttachments.BOSS_SLAIN, true);
					player.sendSystemMessage(Component.translatable("message.wildercord.boss_breakthrough").withStyle(ChatFormatting.GOLD));
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			FORMING.clear();
			LAST_SPELL_HIT.clear();
			NOTIFIED.clear();
			CONDENSING.clear();
			AWAKENING.clear();
		});
	}

	/** Remembers that a spell of {@code caster}'s hurt {@code target}, so its death can count as a spell kill. */
	static void hurtBySpell(Cast cast, net.minecraft.world.entity.LivingEntity target) {
		if (!(cast.caster instanceof ServerPlayer)) {
			return;
		}
		LAST_SPELL_HIT.put(target.getUUID(), new SpellHit(cast.caster.getUUID(), target.level().getGameTime(), cast.info.runes()));
		if (LAST_SPELL_HIT.size() > 4096) {
			long now = target.level().getGameTime();
			LAST_SPELL_HIT.values().removeIf(h -> now - h.time() > 100);
		}
	}

	/** Mana spent casting spells condenses toward the next circle. */
	public static void condense(ServerPlayer player, float mana) {
		if (!Float.isFinite(mana) || mana <= 0 || player.isCreative()) {
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
			Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.6F);
		}
		if (meditating && circles > 0 && !ready) {
			rings(player, circles, player.level().getGameTime() * 0.05, 0.45F);
		}
		if (!ready || !meditating) {
			FORMING.remove(id);
			return;
		}
		// On a ley line the world's own mana helps: circles form twice as fast.
		int progress = FORMING.merge(id, player.getAttachedOrElse(WildercordAttachments.ON_LEY, false) ? 10 : 5, Integer::sum);
		forming(player, circles, progress);
		if (progress >= Circles.FORM_TICKS) {
			FORMING.remove(id);
			form(player);
		}
	}

	/** Forms the next earned circle. Recheck at the mutation boundary, including the cap. */
	public static void form(ServerPlayer player) {
		if (!Heart.ready(player)) return;
		int n = Heart.circles(player) + 1;
		player.setAttached(WildercordAttachments.CIRCLES, n);
		dev.wildercord.advancement.Advancements.circles(player);
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
		if (n == Circles.MANA_SKIN || n == Circles.FLOW || n == Circles.OVERFLOW || n == Circles.ARCHMAGE) {
			player.sendSystemMessage(Component.translatable("message.wildercord.perk." + n).withColor(0xF5C46A));
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

	/** 3rd Circle: Mana Skin. A fifth of the damage you take is paid from mana instead. */
	private static void manaSkin(ServerPlayer player, float damage) {
		if (Heart.active(player) < Circles.MANA_SKIN || player.isCreative() || !player.isAlive() || Spellbooks.tier(player) == null) {
			return;
		}
		float mana = Spellbooks.mana(player);
		float share = (float) Math.min(damage * Circles.MANA_SKIN_SHARE, mana / Circles.MANA_SKIN_COST);
		if (share < 0.25F) {
			return;
		}
		player.heal(share);
		Spellbooks.setMana(player, mana - share * Circles.MANA_SKIN_COST);
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
		NOTIFIED.remove(player);
		AWAKENING.remove(player);
	}
}
