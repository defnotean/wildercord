package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.content.CordTier;
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
	/** A charge held this long fizzles. */
	private static final int MAX_HOLD = 20 * 12;
	private static final Identifier SLOW = Wildercord.id("charging");

	public static double progress(WildercordAttachments.Charge charge, long now) {
		return Math.max(0, Math.min(1, (now - charge.start()) / (double) FULL));
	}

	public static void request(ServerPlayer player, int requested, boolean start) {
		if (start) {
			begin(player, requested);
			return;
		}
		WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
		if (charge == null) {
			// The charge never started (cooling down, an empty spell...): cast normally, which says why.
			SpellCaster.cast(player, requested, 0);
			return;
		}
		double progress = progress(charge, player.level().getGameTime());
		stop(player);
		SpellCaster.cast(player, charge.spell(), progress);
	}

	private static void begin(ServerPlayer player, int requested) {
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			return;
		}
		Spellbook book = Spellbooks.get(player);
		int spell = requested < 0 ? book.selected() : requested;
		if (spell >= tier.spells) {
			return;
		}
		List<RuneDef> runes = SpellCaster.activeRunes(book, spell, tier);
		if (runes.isEmpty()) {
			return;
		}
		long now = player.level().getGameTime();
		if (now < Spellbooks.readyAt(player, spell)) {
			// Still cooling down: the release will say so.
			return;
		}
		List<String> ids = new ArrayList<>();
		for (RuneDef rune : runes.subList(0, Math.min(runes.size(), dev.wildercord.spell.SpellSigil.MAX_RUNES))) {
			ids.add(rune.id());
		}
		player.setAttached(WildercordAttachments.CHARGE, new WildercordAttachments.Charge(spell, now, List.copyOf(ids)));
		player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(
			new AttributeModifier(SLOW, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.5F);
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
		if (held >= FULL && held < FULL + 5) {
			Fx.sound(player.level(), player.position(), SoundEvents.BEACON_POWER_SELECT, 0.5F, 1.8F);
			Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.9F);
		}
		if (held > MAX_HOLD || Spellbooks.tier(player) == null || !player.isAlive()) {
			stop(player);
			player.sendOverlayMessage(Component.translatable("message.wildercord.charge_fizzled").withStyle(ChatFormatting.GRAY));
			Fx.sound(player.level(), player.position(), SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.4F);
		}
	}

	public static void forget(ServerPlayer player) {
		if (player.hasAttached(WildercordAttachments.CHARGE)) {
			stop(player);
		}
	}
}
