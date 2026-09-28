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
			mc.particleEngine.add(new Circle(level, player, charge, color(player, charge)));
		}
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
		private int fading = -1;

		Circle(ClientLevel level, Player caster, WildercordAttachments.Charge charge, int color) {
			super(level, caster.getX(), caster.getEyeY(), caster.getZ(), new SpellCircleOption(charge.runes(), color, RADIUS, 0, 0, 20 * 20));
			this.caster = caster;
			this.start = charge.start();
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
			return Mth.clamp((caster.level().getGameTime() - start + partial) / (double) Charging.FULL, 0, 1);
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
			return (firstPerson() ? FIRST_PERSON : RADIUS) * full;
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
