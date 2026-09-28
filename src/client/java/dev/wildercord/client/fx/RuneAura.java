package dev.wildercord.client.fx;

import dev.wildercord.player.WildercordAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A Runebound's aura, drawn on every client from its synced rune marks: a faint haze of its
 * spell's colour around it, breathing slowly, and specks of light drifting up off its body, more of
 * them and brighter while it telegraphs a cast. The marks on its body are {@code RuneMarksLayer}.
 */
public final class RuneAura {
	private RuneAura() {}

	private static final double RANGE = 36;
	private static final Glimmer.Budget MOTES = new Glimmer.Budget(90);
	private static final Glimmer.Budget HAZES = new Glimmer.Budget(24);

	public static void tick(Minecraft mc) {
		MOTES.tick();
		HAZES.tick();
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null) {
			return;
		}
		long now = level.getGameTime();
		RandomSource random = player.getRandom();
		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isInvisible()) {
				continue;
			}
			WildercordAttachments.RuneMarks marks = living.getAttached(WildercordAttachments.RUNE_MARKS);
			if (marks == null || living.distanceToSqr(player) > RANGE * RANGE) {
				continue;
			}
			long left = marks.castAt() - now;
			boolean telegraphing = marks.castAt() > 0 && left >= 0 && left <= 24;
			float width = living.getBbWidth();
			float height = living.getBbHeight();
			// The haze: one soft glow at a time around the body, renewed before the last one fades.
			if ((now + living.getId()) % 40 == 0 && HAZES.hasRoom()) {
				mc.particleEngine.add(Glimmer.haze(level, living, height * 0.55, marks.color(), height * 1.35F, marks.adept() ? 0.13F : 0.09F, 48));
				HAZES.spend(48);
			}
			float chance = telegraphing ? 0.9F : marks.adept() ? 0.2F : 0.12F;
			int count = telegraphing ? 2 : 1;
			for (int i = 0; i < count && MOTES.hasRoom() && random.nextFloat() < chance; i++) {
				double a = random.nextDouble() * Mth.TWO_PI;
				double r = width * 0.5 + 0.08 + random.nextDouble() * 0.12;
				Vec3 at = new Vec3(living.getX() + Math.cos(a) * r, living.getY() + height * (0.1 + random.nextDouble() * 0.85),
					living.getZ() + Math.sin(a) * r);
				double rise = telegraphing ? 0.03 + random.nextDouble() * 0.02 : 0.008 + random.nextDouble() * 0.012;
				int color = random.nextInt(3) == 0 ? hot(marks.color()) : marks.color();
				int life = telegraphing ? 16 + random.nextInt(10) : 26 + random.nextInt(18);
				mc.particleEngine.add(Glimmer.mote(level, at, color, 0.08F + random.nextFloat() * 0.06F, telegraphing ? 0.9F : 0.6F, life,
					Math.cos(a) * 0.004, rise, Math.sin(a) * 0.004, 0.003F));
				MOTES.spend(life);
			}
		}
	}

	private static int hot(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + (255 - r) / 2) << 16) | ((g + (255 - g) / 2) << 8) | (b + (255 - b) / 2);
	}
}
