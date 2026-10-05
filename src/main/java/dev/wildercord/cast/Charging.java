package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Overchannel;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.SpellCompiler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Charged casting: hold the cast key and a magic circle grows in front of you, one ring for
 * every rune in the spell. Release to cast; a full charge (1.5 seconds) adds 40% power. A tap
 * still casts instantly, so charging is never required. You walk slower while you charge.
 *
 * <p>Holding on past full is a performance with a risk in it (see {@link Overchannel} for the rules
 * and numbers): the charge overchannels, climbing a stage a little over every second while it drains
 * spare mana, each stage stronger and wilder; letting go on the beat (as it fills, or as a stage
 * lands) adds a little more; held too long past the last stage it tears loose, harmlessly but not
 * kindly. Holding sneak while charging steadies the hands to trace the spell's glyph, which the
 * client scores and reports ({@link #trace}); the server believes it only as far as the time spent
 * steadying allows. The stage is checked every tick, so its beats land where the HUD says they will.</p>
 *
 * <p>The circle is drawn by every nearby client from the synced {@link WildercordAttachments#CHARGE}
 * attachment, so it follows the caster smoothly instead of being re-sent every tick.</p>
 */
public final class Charging {
	private Charging() {}

	/** Ticks to a full charge. */
	public static final int FULL = 30;
	/** Extra power at a full charge. */
	public static final double POWER = 0.4;
	/** A release from at least this far into the charge is a charged spell, and leaves with a rush of air. */
	private static final double RELEASE_FROM = 0.2;
	/** A charge held this long fizzles. */
	private static final int MAX_HOLD = 20 * 12;
	private static final Identifier SLOW = Wildercord.id("charging");
	/** A charge can start at most this often (a modified client could otherwise flood everyone nearby with circles). */
	private static final int MIN_BEGIN_GAP = 4;

	/** When each player last started a charge. */
	private static final java.util.Map<java.util.UUID, Long> LAST_BEGIN = new java.util.HashMap<>();
	/** Players whose charge fizzled: the release that follows does nothing. */
	private static final java.util.Set<java.util.UUID> FIZZLED = new java.util.HashSet<>();
	/** What the server keeps about each charge in hand that the client never decides: its price, the steadying, the trace reported. */
	private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
	/** How each player's last charged release was performed (for tests and commands). */
	private static final Map<UUID, Performance> LAST = new HashMap<>();

	/** One charge's channel: the spell's mana price (what the channel may never drain), ticks spent steadying, and the reported trace. */
	private static final class Channel {
		final long start;
		final double cost;
		int steady;
		double reported;

		Channel(long start, double cost) {
			this.start = start;
			this.cost = cost;
		}
	}

	/**
	 * How a release was performed: the overchannel stage it reached, whether it fell on the beat, the
	 * trace accuracy the server believed, the power all that adds, and the chance it surges.
	 */
	public record Performance(int stage, boolean onBeat, double trace, double power, double surgeChance) {
		public static final Performance NONE = new Performance(0, false, 0, 1.0, 0);

		/** Whether there is anything to tell the caster about. */
		public boolean shown() {
			return stage > 0 || onBeat || trace > 0;
		}
	}

	public static double progress(WildercordAttachments.Charge charge, long now) {
		return Math.max(0, Math.min(1, (now - charge.start()) / (double) FULL));
	}

	/** Ticks to a full charge for this caster: a Focus of Haste in the off-hand fills it faster. */
	public static int fullTicks(net.minecraft.world.entity.Entity caster) {
		double speed = caster instanceof net.minecraft.world.entity.LivingEntity living ? dev.wildercord.gear.Gear.chargeSpeed(living) : 1.0;
		// A spell grown Ready Breath charges a little faster (see Mastery).
		speed *= Mastery.chargeSpeed(caster);
		boolean hurried = VoidTime.hurried(caster);
		// Quick hands: Haste fills the charge 30% sooner (hurried time, Accelerate's Haste III, fills it 40% sooner and does not stack with that).
		if (!hurried && caster instanceof net.minecraft.world.entity.LivingEntity hasty && hasty.hasEffect(net.minecraft.world.effect.MobEffects.HASTE)) {
			speed *= 1.3;
		}
		return Math.max(1, (int) Math.round(FULL / (speed * (hurried ? VoidTime.HURRY_CHARGE : 1.0))));
	}

	/** How far along a caster's charge is, 0 to 1, with their casting gear. */
	public static double progress(net.minecraft.world.entity.Entity caster, WildercordAttachments.Charge charge, long now) {
		return Math.max(0, Math.min(1, (now - charge.start()) / (double) fullTicks(caster)));
	}

	/** The server's overchannel tuning (the {@code channeling} section of its config). */
	public static Overchannel.Tuning tuning() {
		return dev.wildercord.config.Config.get().channeling().tuning();
	}

	public static void request(ServerPlayer player, int requested, boolean start) {
		if (dev.wildercord.aura.MastersArts.committed(player)) return;
		if (start) {
			begin(player, requested);
			return;
		}
		WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
		if (charge == null) {
			if (FIZZLED.remove(player.getUUID())) {
				// It fizzled while held: letting go does nothing.
				return;
			}
			// The charge never started (cooling down, an empty spell...): cast normally, which says why.
			SpellCaster.cast(player, requested, 0);
			return;
		}
		long now = player.level().getGameTime();
		double progress = progress(player, charge, now);
		Performance performance = performance(player, charge, now);
		LAST.put(player.getUUID(), performance);
		stop(player);
		long readyAt = Spellbooks.readyAt(player, charge.spell());
		SpellCaster.cast(player, charge.spell(), progress, performance);
		// The spell went off if its cooldown started (not if it lacked mana, or is waiting to overcast).
		if (Spellbooks.readyAt(player, charge.spell()) != readyAt) {
			if (progress >= RELEASE_FROM) {
				Fx.sound(player.level(), player.position(), WildercordSounds.RELEASE, 0.5F + 0.5F * (float) progress, 1.0F);
				ScreenFx.kick(player, (float) progress);
			}
			if (performance.onBeat()) {
				// A bright chime, a step up the scale for every stage it was let go at.
				dev.wildercord.cast.feel.Feels.sound(player.level(), player.position(), "beat_release", 0.8F,
					dev.wildercord.cast.feel.Feels.step(performance.stage()));
			}
			if (performance.shown()) {
				player.sendOverlayMessage(describe(performance));
			}
		}
	}

	/**
	 * How the release of {@code charge} at {@code now} was performed, as far as the server can tell: the
	 * stage the channel reached, whether it's on the beat, and what it believes of the reported trace.
	 */
	public static Performance performance(ServerPlayer player, WildercordAttachments.Charge charge, long now) {
		Overchannel.Tuning tuning = tuning();
		Channel channel = CHANNELS.get(player.getUUID());
		int stage = Math.max(0, Math.min(charge.stages(), charge.stage()));
		boolean onBeat = Overchannel.onBeat(now, Overchannel.beatTime(charge.start(), charge.full(), stage, charge.stageTime()), charge.stages());
		double trace = channel == null || channel.start != charge.start() ? 0
			: Overchannel.validTrace(channel.reported, channel.steady, tuning.tracing() && charge.traceable());
		return new Performance(stage, onBeat, trace, Overchannel.power(stage, onBeat, trace, tuning),
			Overchannel.surgeChance(stage, tuning.surgePerStage(), trace));
	}

	/** How {@code player}'s last charged release was performed ({@link Performance#NONE} before any). */
	public static Performance last(ServerPlayer player) {
		return LAST.getOrDefault(player.getUUID(), Performance.NONE);
	}

	/** The line above the hotbar for a performed release: "Overchannel III · on the beat · steadied 92% (+90% power)". */
	private static Component describe(Performance performance) {
		List<Component> parts = new ArrayList<>();
		if (performance.stage() > 0) {
			parts.add(Component.translatable("message.wildercord.overchannel.stage", roman(performance.stage())));
		}
		if (performance.onBeat()) {
			parts.add(Component.translatable("message.wildercord.overchannel.beat"));
		}
		if (performance.trace() > 0) {
			parts.add(Component.translatable("message.wildercord.overchannel.steady", Math.round(performance.trace() * 100)));
		}
		MutableComponent line = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				line.append(" · ");
			}
			line.append(parts.get(i));
		}
		long percent = Math.round((performance.power() - 1) * 100);
		if (percent > 0) {
			line.append(Component.translatable("message.wildercord.overchannel.power", percent));
		}
		int color = switch (performance.stage()) {
			case 0 -> 0xF5D56A;
			case 1 -> 0xFFC266;
			case 2 -> 0xFF9A4A;
			default -> 0xFF6A4A;
		};
		return line.withColor(color);
	}

	/** I, II or III. */
	public static String roman(int stage) {
		return switch (stage) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			default -> "";
		};
	}

	/**
	 * The client's report of how well the glyph of the charge that began at {@code chargeStart} was
	 * traced, sent just before it's let go. Only remembered for the charge in hand; how much of it counts
	 * is decided at the release ({@link Overchannel#validTrace}).
	 */
	public static void trace(ServerPlayer player, long chargeStart, float accuracy) {
		WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
		Channel channel = CHANNELS.get(player.getUUID());
		if (charge == null || channel == null || charge.start() != chargeStart || channel.start != chargeStart) {
			return;
		}
		channel.reported = Float.isNaN(accuracy) ? 0 : Math.max(0, Math.min(1, accuracy));
	}

	/**
	 * Breaks a player's charge (a Windcut, a silence, Manaburn): the circle closes and the release that follows does nothing.
	 * Returns whether there was a charge or an interruptible Aura performance.
	 */
	public static boolean interrupt(ServerPlayer player) {
		boolean artInterrupted = dev.wildercord.aura.MastersArts.cancel(player);
		if (!player.hasAttached(WildercordAttachments.CHARGE)) {
			return artInterrupted;
		}
		stop(player);
		FIZZLED.add(player.getUUID());
		player.sendOverlayMessage(Component.translatable("message.wildercord.charge_interrupted").withStyle(ChatFormatting.GRAY));
		Fx.sound(player.level(), player.position(), SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.4F);
		return true;
	}

	private static void begin(ServerPlayer player, int requested) {
		FIZZLED.remove(player.getUUID());
		CordTier tier = Spellbooks.tier(player);
		if (tier == null || !player.isAlive() || player.isSpectator() || player.hasAttached(WildercordAttachments.CHARGE) || CastLock.locked(player)) {
			return;
		}
		Spellbook book = Spellbooks.get(player);
		int spell = requested < 0 ? book.selected() : requested;
		if (requested < 0 && !dev.wildercord.gear.Gear.spellOpen(player, tier, spell)) {
			// As a tap does (SpellCaster.cast): the selected spell went quiet with the Tome put away, so the next open one charges.
			spell = dev.wildercord.gear.SpellSlots.resolve(tier.spells, dev.wildercord.gear.Gear.tome(player), spell);
		}
		if (!dev.wildercord.gear.Gear.spellOpen(player, tier, spell)) {
			return;
		}
		List<RuneDef> runes = SpellCaster.activeRunes(book, spell, tier);
		if (runes.isEmpty()) {
			return;
		}
		long now = player.level().getGameTime();
		if (now < Spellbooks.readyAt(player, spell)) {
			// Still cooling down: the release will say so. Early, so any rhythm starts over.
			Rhythm.early(player, now);
			return;
		}
		Long last = LAST_BEGIN.get(player.getUUID());
		if (last != null && now >= last && now - last < MIN_BEGIN_GAP) {
			return;
		}
		LAST_BEGIN.put(player.getUUID(), now);
		List<String> ids = new ArrayList<>();
		for (RuneDef rune : runes.subList(0, Math.min(runes.size(), dev.wildercord.spell.SpellSigil.MAX_RUNES))) {
			ids.add(rune.id());
		}
		Overchannel.Tuning tuning = tuning();
		// How far it can be pushed is fixed as it begins: the working Heart Circles now (a cracked one holds nothing).
		int stages = Overchannel.stagesFor(Heart.active(player), tuning.enabled());
		int full = fullTicks(player);
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
		double cost = compiled.isEmpty() ? 0 : Heart.manaCost(player, compiled, Heart.secretCost(player, runes));
		CHANNELS.put(player.getUUID(), new Channel(now, cost));
		player.setAttached(WildercordAttachments.CHARGE,
			new WildercordAttachments.Charge(spell, now, List.copyOf(ids), full, stages, 0, now + full, tuning.tracing()));
		player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(
			new AttributeModifier(SLOW, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		Fx.sound(player.level(), player.position(), WildercordSounds.CIRCLE_OPEN, 0.5F, 1.0F);
	}

	private static void stop(ServerPlayer player) {
		player.removeAttached(WildercordAttachments.CHARGE);
		player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SLOW);
		CHANNELS.remove(player.getUUID());
	}

	/**
	 * Every tick, for a player charging: counts the steadying (sneak held), and runs the overchannel:
	 * the drain from stage I on, the climb to the next stage when it's due and there's mana to feed it,
	 * and tearing loose once held too long past the last stage.
	 */
	public static void everyTick(ServerPlayer player) {
		WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
		if (charge == null) {
			return;
		}
		Channel channel = CHANNELS.get(player.getUUID());
		if (channel == null || channel.start != charge.start()) {
			// A charge set some other way (a test, an old save of the attachment): it's priced now.
			channel = new Channel(charge.start(), 0);
			CHANNELS.put(player.getUUID(), channel);
		}
		if (player.isShiftKeyDown()) {
			channel.steady++;
		}
		if (charge.stages() <= 0) {
			return;
		}
		long now = player.level().getGameTime();
		long filled = charge.start() + charge.full();
		if (now < filled) {
			return;
		}
		Overchannel.Tuning tuning = tuning();
		int stage = charge.stage();
		long since = now - Overchannel.beatTime(charge.start(), charge.full(), stage, charge.stageTime());
		boolean creative = player.isCreative();
		float mana = Spellbooks.mana(player);
		double drain = Overchannel.drainPerTick(channel.cost, tuning.drainPerSecond());
		boolean fed = creative || Overchannel.fed(mana, channel.cost, drain);
		if (Overchannel.tears(stage, charge.stages(), since)) {
			backfire(player, charge, tuning);
			return;
		}
		boolean climbs = fed && Overchannel.due(stage, charge.stages(), since);
		// The channel takes its mana from stage I on, and as each stage lands; it never touches the spell's own price.
		if (fed && !creative && (stage > 0 || climbs)) {
			Spellbooks.setMana(player, (float) (mana - drain));
		}
		if (climbs) {
			int next = stage + 1;
			player.setAttached(WildercordAttachments.CHARGE, charge.withStage(next, now));
			// A crack through the circle, higher with each stage.
			dev.wildercord.cast.feel.Feels.sound(player.level(), player.position().add(0, 1.2, 0), "overchannel_crack", 0.55F + 0.15F * next,
				dev.wildercord.cast.feel.Feels.step(next - 1));
		}
	}

	/**
	 * A channel held too long past its last stage tears loose: the circle bursts, the spell fizzles (the
	 * release that follows does nothing), some mana scatters, the caster is dazed for a moment, and the
	 * loose magic surges harmlessly ({@link WildSurge#fizzle}). It never deals damage.
	 */
	private static void backfire(ServerPlayer player, WildercordAttachments.Charge charge, Overchannel.Tuning tuning) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		// Where the circle was: behind the shoulders.
		Vec3 at = eye.subtract(look.scale(1.55)).add(0, -0.3, 0);
		int color = color(charge);
		stop(player);
		FIZZLED.add(player.getUUID());
		Overchannel.Backfire backfire = Overchannel.backfire(Spellbooks.mana(player), Mana.max(player), player.isCreative(), tuning);
		if (!player.isCreative()) {
			Spellbooks.setMana(player, backfire.manaAfter());
		}
		if (backfire.stunTicks() > 0) {
			CastLock.daze(player, backfire.stunTicks());
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, backfire.stunTicks(), 2, false, false, true));
		}
		Sigils.flash(level, at, color, 1.8F);
		Sigils.flash(level, at, 0xFFFFFF, 1.1F);
		ElementFx.ring(level, at, look, color, 0.2, 2.4, 0.07, 10);
		ElementFx.ring(level, at, look, 0xFFFFFF, 0.1, 1.6, 0.035, 8);
		Motes.burst(level, at, 18, color, 0.14, 18, 0.28);
		Motes.burst(level, at, 8, 0xFFFFFF, 0.1, 12, 0.36);
		dev.wildercord.cast.feel.Feels.sound(level, at, "overchannel_backfire", 0.9F, 1.0F);
		Fx.sound(level, at, WildercordSounds.MAGIC_BREAK, 0.6F, 0.8F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.overchannel.backfire").withColor(0xFF6A5A));
		WildSurge.fizzle(player);
	}

	/** A charge's colour: its first effect's, or the shape colour. */
	private static int color(WildercordAttachments.Charge charge) {
		for (String id : charge.runes()) {
			RuneDef rune = dev.wildercord.spell.Runes.get(id).orElse(null);
			if (rune != null && rune.family() == RuneFamily.EFFECT) {
				return RuneColors.of(rune);
			}
		}
		return RuneColors.SHAPE;
	}

	/** Every 5 ticks: a chime at full charge, and a charge held too long fizzles. */
	public static void tick(ServerPlayer player) {
		WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
		if (charge == null) {
			return;
		}
		long held = player.level().getGameTime() - charge.start();
		int full = fullTicks(player);
		if (held >= full && held < full + 5) {
			Fx.sound(player.level(), player.position(), WildercordSounds.CHARGE_FULL, 0.8F, 1.0F);
		}
		if (held > MAX_HOLD || Spellbooks.tier(player) == null || !player.isAlive() || player.isSpectator()) {
			stop(player);
			FIZZLED.add(player.getUUID());
			player.sendOverlayMessage(Component.translatable("message.wildercord.charge_fizzled").withStyle(ChatFormatting.GRAY));
			Fx.sound(player.level(), player.position(), SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.4F);
		}
	}

	public static void forget(ServerPlayer player) {
		if (player.hasAttached(WildercordAttachments.CHARGE)) {
			stop(player);
		}
		LAST_BEGIN.remove(player.getUUID());
		LAST.remove(player.getUUID());
		FIZZLED.remove(player.getUUID());
		CHANNELS.remove(player.getUUID());
	}

	static void clear() {
		CastLock.clear();
		LAST_BEGIN.clear();
		LAST.clear();
		FIZZLED.clear();
		CHANNELS.clear();
	}
}
