package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The displays of the life and arcane runes that were rebuilt so no two read alike. Two rules keep the elements from
 * turning into templates: <b>arcane's star seal marks someone</b> (a lasting state: Exposed, a brand, a reflecting shell),
 * and instant strikes are needles, columns and comets; <b>life's bloom means restoration</b>, and combat life is thorns and
 * vines, crowd life spores, world life ripples and lamps. Every method sends through {@link Fx}/{@link ElementFx} (so
 * nothing lands in the caster's own eyes) and plays its own kit sound (tools/feel/life.py, arcane.py).
 */
final class LifeArcaneFx {
	private LifeArcaneFx() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	static final int HOLY = 0xFFF0B0;
	private static final int AMBER = 0xFFC04A;
	private static final int LIME = 0xA8F060;
	/** The pentatonic ratios: one ping, a different step of the scale for each star. */
	private static final float[] STAR_PITCH = {1.0F, 1.122F, 1.26F, 1.498F, 1.682F, 2.0F};

	// ------------------------------------------------------------------ shared: how Exposed looks

	/** A small eye-shaped seal turning over the head: the weak point showing (Exposed). */
	static void exposedMark(ServerLevel level, Entity t, int ticks) {
		Vec3 head = t.position().add(0, t.getBbHeight() + 0.55, 0);
		ElementFx.sigil(level, head, UP, SigilOption.TARGET, ElementFx.ARCANE.secondary(), 0.3, Math.min(ticks, 60), 0.12);
	}

	// ------------------------------------------------------------------ life

	/** Heal: a bloom at the feet, a column of petals rising, two hearts; a warm flash at the chest. */
	static void heal(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		double h = target.getBbHeight();
		Vec3 chest = target.getBoundingBox().getCenter();
		Sigils.flash(level, chest, ElementFx.LIFE.secondary(), 1.3F);
		ElementFx.bloom(level, chest, base, 1.0 + Math.max(0.45, target.getBbWidth() * 0.75) * 0.5);
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(1 + i * 2, () -> ElementFx.petals(level, base.add(0, 0.3 + k * h * 0.4, 0), 0.35, 3));
		}
		Vfx.emit(level, ParticleTypes.HEART, base.add(0, h + 0.3, 0), 2, 0.3, 0.0);
		Feels.sound(level, base, "life_heal", 0.9F, 1.0F);
	}

	/** Regrowth: a vine winding up the body in three stages, leaves shedding at each step. */
	static void regrowth(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		double r = Math.max(0.45, target.getBbWidth() * 0.75);
		double h = target.getBbHeight();
		int[] colors = {0x3E7A34, ElementFx.LIFE.primary(), LIME};
		for (int stage = 0; stage < 3; stage++) {
			int k = stage;
			Scheduler.later(stage * 60 + 1, () -> {
				if (!target.isAlive()) {
					return;
				}
				ElementFx.swirl(level, target.position(), r, h * (0.4 + 0.3 * k), 3, colors[k], ElementFx.LIFE.secondary());
				ElementFx.groundRing(level, target.position(), colors[k], 0.2, 1.0 + 0.2 * k, 0.04, 12);
				Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, target.getBoundingBox().getCenter(), 3, 0.35, 0.0);
			});
		}
		Feels.sound(level, base, "life_regrow", 0.9F, 1.0F);
	}

	/** Nourish: a warm amber ring at the mouth and a few crumbs. */
	static void nourish(ServerLevel level, Entity target) {
		Vec3 mouth = target.getEyePosition().subtract(0, 0.3, 0);
		ElementFx.ring(level, mouth, UP, AMBER, 0.7, 0.2, 0.04, 10);
		Vfx.emit(level, new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.Items.BREAD), mouth, 5, 0.2, 0.05);
		Feels.sound(level, target.position(), "life_munch", 0.8F, 1.0F);
	}

	/** Harvest: a flat golden crescent sweeping across the field, wheat-gold motes lifting off it. */
	static void reap(ServerLevel level, Vec3 centre) {
		Vec3 flat = centre.add(0, 0.25, 0);
		ElementFx.slash(level, flat, UP, new Vec3(1, 0, 0), 0xF0D060, 2.6, Math.PI * 1.3, 0.09, 2, 9);
		Motes.glows(level, flat, 8, 1.6, 0xF6E08A, 0.12, 24, new Vec3(0, 0.03, 0), 0.01);
		Feels.sound(level, centre, "life_reap", 0.8F, 1.0F);
	}

	/** Grow: a green ripple racing out from the block, stems popping up ring by ring. */
	static void growRipple(ServerLevel level, Vec3 at) {
		for (int i = 0; i < 3; i++) {
			int k = i;
			Scheduler.later(1 + i * 2, () -> {
				ElementFx.groundRing(level, at.subtract(0, 0.1, 0), LIME, 0.3 + k * 0.7, 0.9 + k * 0.8, 0.05, 8);
				Vfx.radial(level, ParticleTypes.HAPPY_VILLAGER, at.add(0, 0.1, 0), 4, 0.05 + 0.03 * k);
			});
		}
		Feels.sound(level, at, "life_grow", 0.8F, 1.0F);
	}

	/** A thread of venom jumping from one victim to a neighbour. */
	static void venomHop(ServerLevel level, Entity from, Entity to) {
		ElementFx.ray(level, from.getBoundingBox().getCenter(), to.getBoundingBox().getCenter(), 0x86D23A, 0.03, 8);
		Motes.glows(level, to.getBoundingBox().getCenter(), 3, 0.3, 0x86D23A, 0.1, 16, new Vec3(0, 0.02, 0), 0.01);
		Feels.sound(level, to.position(), "life_fang", 0.35F, 1.4F);
	}

	/** Ancient Seed's growth pulse: a gold-teal ring rising through the field. */
	static void seedPulse(ServerLevel level, BlockPos ground, int reach) {
		Vec3 at = Vec3.atBottomCenterOf(ground.above());
		ElementFx.groundRing(level, at, 0xC8F0A0, 0.3, reach + 0.5, 0.06, 14);
		Feels.sound(level, at, "life_crack", 0.4F, 1.6F);
	}

	/** Restore: a golden thread stitching the body from head to toe, and an anvil ring at the end. */
	static void restore(ServerLevel level, Entity target, boolean mended) {
		Vec3 base = target.position();
		double h = target.getBbHeight();
		for (int i = 0; i < 4; i++) {
			int k = i;
			Scheduler.later(1 + i * 2, () -> ElementFx.ring(level, base.add(0, h - k * h / 3.5, 0), UP, 0xF5D86A, Math.max(0.6, target.getBbWidth()), Math.max(0.5, target.getBbWidth() * 0.8), 0.03, 6));
		}
		if (mended) {
			Vfx.emit(level, ParticleTypes.WAX_ON, target.getBoundingBox().getCenter(), 8, 0.4, 0.0);
		}
		Vfx.emit(level, ParticleTypes.HEART, base.add(0, h + 0.3, 0), 1, 0.2, 0.0);
		Feels.sound(level, base, "life_mend", 0.9F, mended ? 1.0F : 1.15F);
	}

	/** A killing blow turned: a golden sun-disc rising behind the creature and a door of light closing round it. */
	static void reversalPayoff(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 base = target.position();
		Vec3 back = target.getLookAngle().multiply(-1, 0, -1);
		if (back.lengthSqr() < 1.0E-4) {
			back = new Vec3(0, 0, -1);
		}
		back = back.normalize();
		Vec3 sun = c.add(back.scale(1.4)).add(0, 0.8, 0);
		Light.orb(level, sun, 0xF5D86A, 0.9, 20);
		ElementFx.ring(level, sun, back, 0xFFF0B0, 0.4, 1.8, 0.07, 16);
		ElementFx.ring(level, sun, back, 0xF5D86A, 0.2, 1.2, 0.05, 20);
		for (int i = 0; i < 3; i++) {
			ElementFx.ring(level, base.add(0, 0.2 + i * 0.7, 0), UP, i == 1 ? ElementFx.LIFE.secondary() : 0xF5D86A, 0.3, 1.6 - i * 0.3, 0.05, 10 + i * 2);
		}
		ElementFx.leafSpiral(level, base, 0.8, target.getBbHeight() + 0.8, 6);
		Vfx.radial(level, ParticleTypes.TOTEM_OF_UNDYING, c, 14, 0.4);
		Sigils.flash(level, c, 0xFFF0B0, 2.4F);
		Feels.sound(level, c, "life_door", 1.0F, 1.0F);
	}

	/** Spore-sick: a small ring of spores turning over a confused monster's head. */
	static void confused(ServerLevel level, Entity mob) {
		Vec3 head = mob.position().add(0, mob.getBbHeight() + 0.4, 0);
		ElementFx.ring(level, head, UP, 0xC8506A, 0.5, 0.5, 0.03, 10);
		Vfx.emit(level, ParticleTypes.SPORE_BLOSSOM_AIR, head, 3, 0.2, 0.0);
	}

	// ------------------------------------------------------------------ arcane

	/** Harm: a thin needle of pink light from the hand to the target, a small starburst, and the eye seal for Exposed. */
	static void harm(ServerLevel level, Entity target, LivingEntity caster) {
		Vec3 c = target.getBoundingBox().getCenter();
		if (caster != null && caster != target) {
			Vec3 hand = caster.getEyePosition().add(caster.getLookAngle().scale(0.8)).subtract(0, 0.3, 0);
			ElementFx.ray(level, hand, c, ElementFx.ARCANE.primary(), 0.045, 5);
			ElementFx.ray(level, hand, c, 0xFFFFFF, 0.015, 4);
		}
		Sigils.flash(level, c, ElementFx.ARCANE.primary(), 1.4F);
		Vfx.radial(level, ParticleTypes.ENCHANTED_HIT, c, 8, 0.28);
		exposedMark(level, target, Exposed.HARM_TICKS);
		Feels.sound(level, c, "arcane_needle", 0.9F, 1.0F);
	}

	/** Haste: afterimage streaks trailing off the hands, no seal. */
	static void haste(ServerLevel level, Entity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 back = target.getLookAngle().multiply(-1, 0, -1);
		back = back.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, -1) : back.normalize();
		Vec3 side = back.cross(UP).normalize();
		for (int i = -1; i <= 1; i++) {
			Vec3 from = c.add(side.scale(i * 0.35)).add(0, 0.2 * i, 0);
			ElementFx.ray(level, from, from.add(back.scale(1.6)), i == 0 ? 0xFFD8FA : ElementFx.ARCANE.primary(), 0.035, 7);
		}
		Vfx.emit(level, ParticleTypes.CRIT, c, 4, 0.3, 0.1);
		Feels.sound(level, target.position(), "arcane_flutter", 0.7F, 1.0F);
	}

	/** Reveal: a sonar ring sweeping out from the target, an eye seal above the head for as long as it glows. */
	static void reveal(ServerLevel level, Entity target, int ticks) {
		Vec3 feet = target.position().add(0, 0.1, 0);
		ElementFx.groundRing(level, feet, ElementFx.ARCANE.secondary(), 0.4, 4.5, 0.05, 14);
		ElementFx.groundRing(level, feet, ElementFx.ARCANE.primary(), 0.3, 3.0, 0.03, 10);
		exposedMark(level, target, ticks);
		Feels.sound(level, target.position(), "arcane_ping", 0.7F, 1.0F);
	}

	/** Empower: a shockwave ring off the fists and rising tongues of power, no seal. */
	static void empower(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		Vec3 c = target.getBoundingBox().getCenter();
		ElementFx.ring(level, c, UP, 0xE04040, 0.3, 1.8, 0.07, 9);
		ElementFx.groundRing(level, base, 0xE04040, 0.3, 1.6, 0.06, 8);
		ElementFx.tongues(level, base, Math.max(0.4, target.getBbWidth() * 0.6), target.getBbHeight(), 4, 0xE04040, 0xFFB060, 2, 8);
		Vfx.radial(level, ParticleTypes.CRIT, c, 8, 0.3);
		Feels.sound(level, base, "arcane_thrum", 0.9F, 1.0F);
	}

	/** The comedown: a soft exhale and a dull ring at the feet. */
	static void comedown(ServerLevel level, Entity target) {
		ElementFx.groundRing(level, target.position(), 0x8A5050, 1.2, 0.3, 0.04, 10);
		Feels.sound(level, target.position(), "arcane_thrum", 0.5F, 1.0F);
	}

	/** Smite's column: a tall white-gold pillar down onto the target, a scorch ring, holy sparks. */
	static void smite(ServerLevel level, Entity target) {
		Vec3 base = target.position();
		Vec3 c = target.getBoundingBox().getCenter();
		Vec3 top = base.add(0, target.getBbHeight() + 12, 0);
		ElementFx.ray(level, top, base, HOLY, 0.4, 10);
		ElementFx.ray(level, top, base, 0xFFFFFF, 0.12, 8);
		Sigils.flash(level, c, HOLY, 2.6F);
		ElementFx.groundRing(level, base, HOLY, 0.2, 2.6, 0.09, 10);
		ElementFx.groundRing(level, base, 0xB08040, 0.1, 1.4, 0.06, 20);
		Vfx.radial(level, ParticleTypes.END_ROD, c, 12, 0.22);
		ScreenFx.shake(level, base, 0.25F, 10);
	}

	/** Smite's wind-up begins: the toll starts to rise. */
	static void smiteWindUp(ServerLevel level, Entity target) {
		Feels.sound(level, target.position(), "arcane_toll", 1.0F, 1.0F);
	}

	/** Starfall's telegraph: the falling stars' landing marks joined by threads of light into a star polygon. */
	static void starfallPattern(ServerLevel level, java.util.List<Vec3> marks) {
		for (int i = 0; i < marks.size(); i++) {
			Vec3 a = marks.get(i).add(0, 0.1, 0);
			Vec3 b = marks.get((i + 2) % marks.size()).add(0, 0.1, 0);
			ElementFx.ray(level, a, b, ElementFx.ARCANE.accent(), 0.02, 14);
		}
	}

	/** One star landing: its own pitch on the scale. */
	static void starLand(ServerLevel level, Vec3 at, int index) {
		Feels.sound(level, at, "arcane_starfall", 0.7F, STAR_PITCH[Math.floorMod(index, STAR_PITCH.length)]);
		ScreenFx.shake(level, at, 0.1F, 6);
	}

	/** Silence: a ring closes over the head and a muzzle-band seals the lower face; nothing can be said. */
	static void silence(ServerLevel level, Entity target) {
		Vec3 head = target.position().add(0, target.getBbHeight() + 0.3, 0);
		Vec3 mouth = target.getEyePosition().subtract(0, 0.25, 0);
		ElementFx.ring(level, head, UP, ElementFx.ARCANE.primary(), 0.8, 0.35, 0.04, 8);
		ElementFx.ring(level, mouth, UP, ElementFx.ARCANE.accent(), 0.5, 0.25, 0.05, 40);
		ElementFx.sigil(level, mouth, target.getLookAngle(), SigilOption.BAND, ElementFx.ARCANE.accent(), 0.25, 40, 0.0);
		Feels.sound(level, target.position(), "arcane_hush", 0.8F, 1.0F);
	}

	/** Swap: two arcs crossing in mid-air between the two places, no seals. */
	static void swap(ServerLevel level, Vec3 a, Vec3 b) {
		Vec3 pa = a.add(0, 1, 0);
		Vec3 pb = b.add(0, 1, 0);
		Vec3 mid = pa.add(pb).scale(0.5).add(0, 0.6, 0);
		ElementFx.ray(level, pa, mid, ElementFx.ARCANE.primary(), 0.06, 8);
		ElementFx.ray(level, mid, pb, ElementFx.ARCANE.primary(), 0.06, 8);
		ElementFx.ray(level, pb, mid.subtract(0, 1.2, 0), ElementFx.ARCANE.secondary(), 0.05, 8);
		ElementFx.ray(level, mid.subtract(0, 1.2, 0), pa, ElementFx.ARCANE.secondary(), 0.05, 8);
		for (Vec3 p : new Vec3[] {pa, pb}) {
			Sigils.flash(level, p, ElementFx.ARCANE.primary(), 1.5F);
			ElementFx.ring(level, p, UP, ElementFx.ARCANE.secondary(), 1.0, 0.3, 0.04, 8);
		}
		Feels.sound(level, a, "arcane_swap", 0.9F, 1.0F);
		Feels.sound(level, b, "arcane_swap", 0.5F, 1.25F);
	}

	/** Summon: one great circle on the ground and a howl; each wolf then steps out with only a ring and a ray. */
	static void summonCircle(ServerLevel level, Vec3 at) {
		Sigils.ground(level, at, ElementFx.ARCANE.primary(), ElementFx.ARCANE.secondary(), 3.2F, 50);
		ElementFx.groundRing(level, at, ElementFx.ARCANE.accent(), 0.5, 3.4, 0.08, 16);
		Sigils.flash(level, at.add(0, 0.8, 0), ElementFx.ARCANE.primary(), 2.6F);
		ScreenFx.shake(level, at, 0.2F, 8);
		Feels.sound(level, at, "arcane_howl", 1.0F, 1.0F);
	}

	static void summonWolf(ServerLevel level, Vec3 at) {
		ElementFx.groundRing(level, at, ElementFx.ARCANE.accent(), 0.2, 1.4, 0.05, 10);
		ElementFx.ray(level, at, at.add(0, 1.8, 0), ElementFx.ARCANE.primary(), 0.16, 10);
		Vfx.emit(level, ParticleTypes.SOUL, at.add(0, 0.5, 0), 5, 0.3, 0.05);
		Feels.sound(level, at, "arcane_yip", 0.5F, 1.0F);
	}
}
