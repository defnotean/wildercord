package dev.wildercord.client.fx;

import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.SigilOption;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * While you charge a spell, where it will go: a reticle on the ground for spells that land where
 * you look (Zone, Rain, Pillar, Totem, Mine), a ring around you for ones centred on you (Burst,
 * Ring, Domain, Nova, and Imprint under your feet), and a faint dotted line as far as the ones that
 * fly or reach can go (Bolt, Beam, Crescent, Orb, Spark, Comet, Ray, Lance, Stream, Glaive...), a
 * Latch's with a mark on the creature it would hold. Only you see it.
 */
public final class AimPreview {
	private AimPreview() {}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.level.getGameTime() % 2 != 0) {
			return;
		}
		WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
		CordTier tier = Spellbooks.tier(player);
		if (charge == null || tier == null) {
			return;
		}
		List<RuneDef> runes = SpellCaster.activeRunes(Spellbooks.get(player), charge.spell(), tier);
		if (runes.isEmpty()) {
			return;
		}
		SpellPlan.Segment root = SpellCompiler.compile(runes).root();
		if (root.groups.isEmpty()) {
			return;
		}
		SpellPlan.Group g = root.groups.getFirst();
		int color = charge.runes().isEmpty() ? 0xFFFFFF : dev.wildercord.spell.Runes.get(charge.runes().get(Math.min(1, charge.runes().size() - 1)))
			.map(dev.wildercord.spell.RuneColors::of).orElse(0xFFFFFF);
		String shape = g.shape.path();
		switch (shape) {
			case "zone" -> reticle(mc, aim(player, CastEngine.AIM_RANGE), SpellNumbers.zoneRadius(g), color);
			case "rain" -> reticle(mc, aim(player, CastEngine.AIM_RANGE), SpellNumbers.rainRadius(g), color);
			case "pillar" -> reticle(mc, aim(player, CastEngine.AIM_RANGE), SpellNumbers.pillarRadius(g), color);
			case "totem" -> reticle(mc, aim(player, CastEngine.AIM_RANGE), SpellNumbers.totemRadius(g), color);
			case "mine" -> reticle(mc, aim(player, CastEngine.AIM_RANGE), SpellNumbers.mineRadius(g), color);
			case "burst" -> reticle(mc, player.position(), SpellNumbers.burstRadius(g), color);
			case "ring" -> reticle(mc, player.position(), SpellNumbers.ringRadius(g), color);
			case "domain" -> reticle(mc, player.position(), SpellNumbers.domainRadius(g), color);
			case "nova" -> reticle(mc, player.position(), SpellNumbers.novaRadius(g), color);
			case "bolt", "arc", "crescent", "orb", "wave", "wisp", "ricochet" -> line(mc, player, 32, color);
			case "comet", "cluster" -> line(mc, player, SpellNumbers.COMET_RANGE, color);
			case "beam" -> line(mc, player, CastEngine.BEAM_RANGE, color);
			case "spark" -> line(mc, player, SpellNumbers.SPARK_RANGE, color);
			case "ray" -> line(mc, player, SpellNumbers.RAY_RANGE, color);
			case "lance" -> line(mc, player, SpellNumbers.LANCE_RANGE, color);
			case "prism" -> line(mc, player, SpellNumbers.PRISM_RANGE, color);
			case "sweep" -> line(mc, player, SpellNumbers.sweepLength(g), color);
			case "stream" -> line(mc, player, SpellNumbers.STREAM_RANGE, color);
			// New runes (batch 2): a Glaive's reach out (it comes back along it), the ground an Imprint will
			// erupt from, and a Latch's line with a mark on the creature it would hold.
			case "glaive" -> line(mc, player, SpellNumbers.GLAIVE_RANGE, color);
			case "imprint" -> reticle(mc, feet(player), SpellNumbers.imprintRadius(g), color);
			case "latch" -> {
				line(mc, player, SpellNumbers.LATCH_RANGE, color);
				latchMark(mc, player, color);
			}
			default -> { }
		}
	}

	/** The ground under the player (an Imprint lands there, not in mid-air). */
	private static Vec3 feet(LocalPlayer player) {
		Vec3 at = player.position();
		BlockHitResult down = player.level().clip(new ClipContext(at.add(0, 0.2, 0), at.add(0, -16, 0), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		return down.getType() == HitResult.Type.MISS ? at : down.getLocation();
	}

	/** A target mark on the first creature a Latch would take hold of, if any. */
	private static void latchMark(Minecraft mc, LocalPlayer player, int color) {
		Vec3 from = player.getEyePosition();
		Vec3 dir = player.getLookAngle();
		BlockHitResult hit = player.level().clip(new ClipContext(from, from.add(dir.scale(SpellNumbers.LATCH_RANGE)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		Vec3 to = hit.getType() == HitResult.Type.MISS ? from.add(dir.scale(SpellNumbers.LATCH_RANGE)) : hit.getLocation();
		net.minecraft.world.phys.EntityHitResult entity = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, from, to,
			new net.minecraft.world.phys.AABB(from, to).inflate(1.0), e -> e instanceof net.minecraft.world.entity.LivingEntity && e.isAlive() && !e.isSpectator(),
			from.distanceToSqr(to));
		if (entity == null) {
			return;
		}
		net.minecraft.world.entity.Entity e = entity.getEntity();
		SigilOption mark = SigilOption.flat(SigilOption.TARGET, color, (float) Math.max(0.7, e.getBbWidth() + 0.4), 3, 0.08F);
		mc.particleEngine.add(SigilParticle.make(mc.level, e.position().add(0, 0.08, 0), mark, true));
	}

	private static Vec3 aim(LocalPlayer player, double range) {
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getLookAngle().scale(range));
		BlockHitResult hit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 point = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
		BlockHitResult down = player.level().clip(new ClipContext(point.add(0, 0.5, 0), point.add(0, -16, 0), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		return down.getType() == HitResult.Type.MISS ? point : down.getLocation();
	}

	private static void reticle(Minecraft mc, Vec3 at, double radius, int color) {
		SigilOption option = SigilOption.flat(SigilOption.TARGET, color, (float) radius, 3, 0.04F);
		mc.particleEngine.add(SigilParticle.make(mc.level, at.add(0, 0.08, 0), option, true));
	}

	private static void line(Minecraft mc, LocalPlayer player, double range, int color) {
		Vec3 from = player.getEyePosition();
		Vec3 dir = player.getLookAngle();
		BlockHitResult hit = player.level().clip(new ClipContext(from, from.add(dir.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double length = hit.getType() == HitResult.Type.MISS ? range : hit.getLocation().distanceTo(from);
		long phase = mc.level.getGameTime() / 2 % 3;
		for (double d = 2.5 + phase * 0.5; d < length; d += 1.5) {
			Vec3 p = from.add(dir.scale(d)).add(0, -0.15, 0);
			mc.level.addParticle(new DustParticleOptions(color, 0.45F), p.x, p.y, p.z, 0, 0, 0);
		}
		if (hit.getType() != HitResult.Type.MISS) {
			SigilOption mark = new SigilOption(SigilOption.TARGET, color, 0.5F, 0, 0, 3, 0.1F);
			Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
			SigilParticle p = SigilParticle.make(mc.level, hit.getLocation().add(n.scale(0.03)), mark, true);
			p.yaw = (float) Math.toDegrees(Math.atan2(-n.x, n.z));
			p.pitch = (float) Math.toDegrees(Math.asin(-n.y));
			mc.particleEngine.add(p);
		}
	}
}
