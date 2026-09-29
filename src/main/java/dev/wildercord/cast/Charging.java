package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Charged casting: hold the cast key and a magic circle grows in front of you, one ring for
 * every rune in the spell. Release to cast; a full charge (1.5 seconds) adds 40% power. A tap
 * still casts instantly, so charging is never required. You walk slower while you charge.
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

	public static double progress(WildercordAttachments.Charge charge, long now) {
		return Math.max(0, Math.min(1, (now - charge.start()) / (double) FULL));
	}

	/** Ticks to a full charge for this caster: a Focus of Haste in the off-hand fills it faster. */
	public static int fullTicks(net.minecraft.world.entity.Entity caster) {
		double speed = caster instanceof net.minecraft.world.entity.LivingEntity living ? dev.wildercord.gear.Gear.chargeSpeed(living) : 1.0;
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

	public static void request(ServerPlayer player, int requested, boolean start) {
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
		double progress = progress(player, charge, player.level().getGameTime());
		stop(player);
		long readyAt = Spellbooks.readyAt(player, charge.spell());
		SpellCaster.cast(player, charge.spell(), progress);
		// The spell went off if its cooldown started (not if it lacked mana, or is waiting to overcast).
		if (progress >= RELEASE_FROM && Spellbooks.readyAt(player, charge.spell()) != readyAt) {
			Fx.sound(player.level(), player.position(), WildercordSounds.RELEASE, 0.5F + 0.5F * (float) progress, 1.0F);
			ScreenFx.kick(player, (float) progress);
		}
	}

	/**
	 * Breaks a player's charge (a Windcut, a silence, Manaburn): the circle closes and the release that follows does nothing.
	 * Returns whether there was a charge.
	 */
	public static boolean interrupt(ServerPlayer player) {
		if (!player.hasAttached(WildercordAttachments.CHARGE)) {
			return false;
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
		player.setAttached(WildercordAttachments.CHARGE, new WildercordAttachments.Charge(spell, now, List.copyOf(ids)));
		player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(
			new AttributeModifier(SLOW, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		Fx.sound(player.level(), player.position(), WildercordSounds.CIRCLE_OPEN, 0.5F, 1.0F);
	}

	private static void stop(ServerPlayer player) {
		player.removeAttached(WildercordAttachments.CHARGE);
		player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SLOW);
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
		FIZZLED.remove(player.getUUID());
	}

	static void clear() {
		CastLock.clear();
		LAST_BEGIN.clear();
		FIZZLED.clear();
	}
}
