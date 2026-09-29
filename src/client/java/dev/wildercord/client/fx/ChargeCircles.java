package dev.wildercord.client.fx;

import dev.wildercord.cast.Charging;
import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * The magic circle a caster holds out while charging a spell, drawn on every client from the synced
 * charge: the spell's own circle (see {@link SpellCircleParticle}), opening as the charge builds, so
 * its roundels appear one by one and it flares when full. For everyone else it's a small circle in
 * front of the caster's raised hands; in your own first-person view, a smaller one low and to the right.
 */
public final class ChargeCircles {
	private ChargeCircles() {}

	/** Which charges already have their circle, by entity id: the charge's start time. */
	private static final Map<Integer, Long> SHOWN = new HashMap<>();
	/** How many notes of each charging caster's melody have played, by entity id. */
	private static final Map<Integer, Integer> SUNG = new HashMap<>();

	/** The circle's radius at the hand, and in your own first-person view. */
	private static final float RADIUS = 0.42F;
	private static final float FIRST_PERSON = 0.2F;

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null) {
			SHOWN.clear();
			return;
		}
		SHOWN.keySet().removeIf(id -> {
			Entity e = level.getEntity(id);
			return !(e instanceof Player p) || !p.hasAttached(WildercordAttachments.CHARGE);
		});
		SUNG.keySet().retainAll(SHOWN.keySet());
		for (Player player : level.players()) {
			WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
			if (charge != null && SHOWN.containsKey(player.getId())) {
				sing(level, player, charge);
			}
		}
		for (Player player : level.players()) {
			WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
			if (charge == null) {
				continue;
			}
			Long shown = SHOWN.get(player.getId());
			if (shown != null && shown == charge.start()) {
				continue;
			}
			SHOWN.put(player.getId(), charge.start());
			SUNG.put(player.getId(), 0);
			mc.particleEngine.add(new Circle(level, player, charge, color(player, charge), size(charge)));
		}
	}

	/**
	 * The spell's melody: as each rune's roundel opens on the circle, its note plays (a knock for a shape, a glass pluck for an
	 * effect, a bell for a modifier, a clink for a link), on a degree of the scale that is the rune's own. Every spell has a tune.
	 */
	private static void sing(ClientLevel level, Player player, WildercordAttachments.Charge charge) {
		int n = charge.runes().size();
		if (n == 0) {
			return;
		}
		double progress = Charging.progress(player, charge, level.getGameTime());
		int sung = SUNG.getOrDefault(player.getId(), 0);
		while (sung < n && progress >= 0.35 + 0.55 * sung / n) {
			note(level, player, charge.runes().get(sung), sung);
			sung++;
		}
		SUNG.put(player.getId(), sung);
	}

	/** One rune's note, at the caster. */
	public static void note(ClientLevel level, Player player, String id, int index) {
		RuneDef rune = Runes.get(id).orElse(null);
		if (rune == null) {
			return;
		}
		String sound = switch (rune.family()) {
			case SHAPE -> "note_shape";
			case MODIFIER -> "note_mod";
			case LINK -> "note_link";
			default -> "note_effect";
		};
		net.minecraft.sounds.SoundEvent event = dev.wildercord.content.WildercordSounds.kit(sound);
		if (event == null) {
			return;
		}
		// The rune's own degree of the scale, and an octave up for the second half of a long spell.
		int degree = Math.floorMod(rune.id().hashCode(), 5) + (index >= 6 ? 5 : 0);
		level.playLocalSound(player.getX(), player.getEyeY(), player.getZ(), event, net.minecraft.sounds.SoundSource.PLAYERS, 0.35F,
			dev.wildercord.cast.feel.Feels.step(degree), false);
	}

	/** How big a charging circle is drawn: by the spell's scale (a costly, high-tier spell opens a bigger one). */
	private static float size(WildercordAttachments.Charge charge) {
		java.util.List<RuneDef> runes = new java.util.ArrayList<>();
		int tier = 1;
		for (String id : charge.runes()) {
			RuneDef rune = Runes.get(id).orElse(null);
			if (rune != null) {
				runes.add(rune);
				tier = Math.max(tier, rune.tier());
			}
		}
		double cost = runes.isEmpty() ? 0 : dev.wildercord.spell.SpellCompiler.compile(runes).cost();
		return switch (dev.wildercord.cast.feel.Band.of(dev.wildercord.cast.feel.Feel.scaleOf(cost, 0, tier))) {
			case S -> 0.85F;
			case M -> 1.0F;
			case L -> 1.2F;
			case XL -> 1.4F;
		};
	}

	/** The spell's first effect's element, or "". */
	public static String element(WildercordAttachments.Charge charge) {
		for (String id : charge.runes()) {
			RuneDef rune = Runes.get(id).orElse(null);
			if (rune != null && rune.family() == RuneFamily.EFFECT) {
				return rune.element();
			}
		}
		return "";
	}

	/** The spell's first element; with none, the caster's leaning; else gold. */
	private static int color(Player player, WildercordAttachments.Charge charge) {
		for (String id : charge.runes()) {
			RuneDef rune = Runes.get(id).orElse(null);
			if (rune != null && rune.family() == RuneFamily.EFFECT) {
				return RuneColors.of(rune);
			}
		}
		String leaning = Heart.leaning(player);
		return leaning.isEmpty() ? 0xF5D56A : RuneColors.element(leaning);
	}

	/** A charging caster's circle, following their hand. */
	private static final class Circle extends SpellCircleParticle {
		private final Player caster;
		private final long start;
		private final float scale;
		private int fading = -1;

		Circle(ClientLevel level, Player caster, WildercordAttachments.Charge charge, int color, float scale) {
			super(level, caster.getX(), caster.getEyeY(), caster.getZ(), new SpellCircleOption(charge.runes(), color, RADIUS, 0, 0, 20 * 20));
			this.caster = caster;
			this.start = charge.start();
			this.scale = scale;
			move(0);
			xo = x;
			yo = y;
			zo = z;
		}

		private boolean firstPerson() {
			Minecraft mc = Minecraft.getInstance();
			return caster == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
		}

		private double progress(float partial) {
			return Mth.clamp((caster.level().getGameTime() - start + partial) / (double) Charging.fullTicks(caster), 0, 1);
		}

		/**
		 * Out in front of the caster's hands (both raised while charging, see AvatarRendererMixin), as
		 * if they were pushing it open; in your own first person view, low and to the right so it never
		 * hides what you're aiming at.
		 */
		private Vec3 anchor(float partial) {
			Vec3 eye = caster.getEyePosition(partial);
			Vec3 look = caster.getViewVector(partial);
			Vec3 right = look.cross(new Vec3(0, 1, 0));
			right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
			Vec3 down = right.cross(look).normalize().scale(-1);
			if (firstPerson()) {
				return eye.add(look.scale(1.2)).add(right.scale(0.42)).add(down.scale(0.3));
			}
			return eye.add(look.scale(1.0)).add(right.scale(0.14)).add(down.scale(0.3));
		}

		private void move(float partial) {
			Vec3 at = anchor(partial);
			x = at.x;
			y = at.y;
			z = at.z;
		}

		@Override
		public void tick() {
			super.tick();
			age = Math.min(age, 10);
			WildercordAttachments.Charge charge = caster.getAttached(WildercordAttachments.CHARGE);
			boolean still = !caster.isRemoved() && charge != null && charge.start() == start;
			if (!still && fading < 0) {
				fading = 5;
			}
			if (fading >= 0 && fading-- == 0) {
				remove();
			}
			move(1);
		}

		@Override
		protected float opening(float partial) {
			return (float) progress(partial);
		}

		@Override
		protected float fade(float partial) {
			float leaving = fading >= 0 ? Math.max(0, (fading - partial) / 5F) : 1F;
			return Math.min(1, (age + partial) / 2F) * leaving;
		}

		@Override
		protected float size(float partial) {
			// A flare when the charge is full.
			float full = progress(partial) >= 1 ? 1.06F : 1F;
			return (firstPerson() ? FIRST_PERSON : RADIUS * scale) * full;
		}

		@Override
		protected Vec3 centre(float partial) {
			return anchor(partial);
		}

		@Override
		protected Quaternionf orientation(float partial) {
			return new Quaternionf().rotationYXZ((float) Math.toRadians(-caster.getViewYRot(partial)),
				(float) Math.toRadians(caster.getViewXRot(partial)), 0);
		}
	}
}
