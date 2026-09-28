package dev.wildercord.chorus;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Sigils;
import dev.wildercord.content.SigilOption;
import dev.wildercord.duel.Duels;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.Trait;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chorus casting (rules in {@link ChorusRules}): when casters cast the same shape together, the
 * later voice's spell is cast once, bigger (+50% power and a Widen per extra voice, up to three),
 * and the earlier voices' spells fold into it (whatever of them is still in the air stops). Each
 * caster has paid for their own spell. Everyone singing sees <i>Chorus!</i>, both colours braid
 * together over the target, and each earns the Chorus feat.
 */
public final class Chorus {
	private Chorus() {}

	/** What to cast: the caster's own spell, or the chorus it became. */
	public record Sung(Cast cast, SpellPlan.Segment root, int voices) {}

	/** The last spell each voice cast and its colour, so a later voice can fold it in. */
	private record Voiced(Cast cast, int color, long time) {}

	/** Shapes centred on their caster: their "aim" is where the caster stands. */
	private static final Set<String> CENTRED = Set.of(Runes.BURST.id(), Runes.NOVA.id(), Runes.RING.id(), Runes.DOMAIN.id(), Runes.ORBIT.id(),
		Runes.TRAIL.id());

	private static final ChorusRules.Choir CHOIR = new ChorusRules.Choir();
	private static final Map<UUID, Voiced> LIVE = new HashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 == 0 && !LIVE.isEmpty()) {
				long now = server.overworld().getGameTime();
				LIVE.values().removeIf(v -> now - v.time() > ChorusRules.WINDOW * 2L);
				CHOIR.forget(now);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LIVE.clear();
			CHOIR.clear();
		});
	}

	/**
	 * Offers a cast to the choir, just before it runs. Returns what to run instead: the same cast
	 * and plan when it sings alone, or the chorus.
	 */
	public static Sung sing(Cast cast, List<RuneDef> runes, SpellPlan.Segment root) {
		if (root == null || root.groups.isEmpty()) {
			return new Sung(cast, root, 1);
		}
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		SpellPlan.Group first = root.groups.getFirst();
		String shape = first.shape.id();
		if (ChorusRules.SOLO.contains(shape)) {
			return new Sung(cast, root, 1);
		}
		long now = level.getGameTime();
		int color = first.effects.isEmpty() ? RuneColors.SHAPE : RuneColors.of(first.effects.getFirst().effect);
		Entity target = null;
		Vec3 aim;
		if (CENTRED.contains(shape)) {
			aim = caster.position();
		} else {
			Vec3 from = caster.getEyePosition();
			Vec3 to = from.add(caster.getLookAngle().scale(24.0));
			BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
			Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
			EntityHitResult entity = ProjectileUtil.getEntityHitResult(caster, from, end, new AABB(from, end).inflate(1.0),
				e -> e != caster && e instanceof LivingEntity && e.isAlive(), from.distanceToSqr(end));
			target = entity == null ? null : entity.getEntity();
			aim = entity == null ? end : entity.getLocation();
		}
		ChorusRules.Voice voice = new ChorusRules.Voice(caster.getUUID(), shape, now, level.dimension().identifier().toString(),
			caster.getX(), caster.getY(), caster.getZ(), aim.x, aim.y, aim.z, target == null ? null : target.getUUID());
		List<ChorusRules.Voice> choir = CHOIR.offer(voice);
		boolean rivals = choir.stream().anyMatch(v -> Duels.opponents(v.caster(), caster.getUUID()));
		if (choir.size() < 2 || rivals) {
			LIVE.put(caster.getUUID(), new Voiced(cast, color, now));
			return new Sung(cast, root, 1);
		}

		int voices = choir.size();
		List<Integer> colors = new ArrayList<>();
		List<LivingEntity> singers = new ArrayList<>();
		for (ChorusRules.Voice earlier : choir.subList(0, voices - 1)) {
			Voiced sung = LIVE.remove(earlier.caster());
			if (sung != null) {
				// Its spell folds into the chorus: whatever of it is still in the air stops here.
				sung.cast().cancel();
				colors.add(sung.color());
			}
			if (level.getEntity(earlier.caster()) instanceof LivingEntity singer) {
				singers.add(singer);
			}
		}
		colors.add(color);
		singers.add(caster);

		// The chorus: this voice's spell, bigger for every voice with it.
		SpellPlan.Segment chorusRoot = SpellCompiler.compile(runes).root();
		SpellPlan.Group group = chorusRoot.groups.getFirst();
		if (group.shape.has(Trait.RADIUS)) {
			for (int i = 0; i < ChorusRules.extra(voices); i++) {
				group.shapeMods.add(Runes.WIDEN);
			}
		}
		double power = ChorusRules.power(voices);
		Cast chorus = new Cast(caster, cast.castNumber, new Heart.Bonuses(cast.power * power, cast.duration, 1, 1), cast.passive, null, cast.info)
			.weigh(cast.weight() * power);
		LIVE.put(caster.getUUID(), new Voiced(chorus, color, now));

		braid(level, singers, aim, colors);
		Component message = Component.translatable("reaction.wildercord.chorus").withColor(0xFFF0C0).withStyle(ChatFormatting.BOLD);
		for (LivingEntity singer : singers) {
			if (singer instanceof ServerPlayer player) {
				player.sendOverlayMessage(message);
				Grimoire.feat(player, Feats.CHORUS);
			}
		}
		return new Sung(chorus, chorusRoot, voices);
	}

	/**
	 * The voices braided together: a ray of each colour from every singer to where they aim, a
	 * double helix of both colours along the last voice's line, and their circles turning against
	 * each other at the target.
	 */
	private static void braid(ServerLevel level, List<LivingEntity> singers, Vec3 aim, List<Integer> colors) {
		for (int i = 0; i < singers.size(); i++) {
			LivingEntity singer = singers.get(i);
			int color = colors.get(Math.min(i, colors.size() - 1));
			Vec3 hand = singer.getEyePosition().add(singer.getLookAngle().scale(0.8)).subtract(0, 0.3, 0);
			Light.ray(level, hand, aim, color, 0.08, 14);
		}
		LivingEntity last = singers.getLast();
		Vec3 from = last.getEyePosition().subtract(0, 0.3, 0);
		Vec3 line = aim.subtract(from);
		double length = line.length();
		if (length > 0.5) {
			Vec3 dir = line.normalize();
			Vec3 side = dir.cross(new Vec3(0, 1, 0));
			side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
			Vec3 up = side.cross(dir).normalize();
			int steps = (int) Math.min(48, length * 2);
			for (int s = 0; s <= steps; s++) {
				double t = (double) s / steps;
				double angle = t * Math.PI * 6;
				for (int strand = 0; strand < 2; strand++) {
					double a = angle + strand * Math.PI;
					Vec3 at = from.add(line.scale(t)).add(side.scale(Math.cos(a) * 0.35)).add(up.scale(Math.sin(a) * 0.35));
					int color = colors.get(strand % colors.size());
					Fx.send(level, Fx.dust(color, 1.1F), at, 1, 0.0, 0.0);
				}
			}
		}
		Vec3 ground = aim.add(0, 0.06, 0);
		for (int i = 0; i < colors.size(); i++) {
			float spin = (i % 2 == 0 ? 0.14F : -0.14F);
			Sigils.send(level, SigilOption.flat(i % 2 == 0 ? SigilOption.CIRCLE : SigilOption.RING, colors.get(i), 1.6F + 0.4F * i, 26, spin), ground.add(0, 0.01 * i, 0));
			Light.groundRing(level, aim, colors.get(i), 0.4, 3.0 + i, 0.08, 12 + 2 * i);
		}
		Sigils.send(level, SigilOption.glow(0xFF000000 | colors.getFirst(), 2.4F), aim.add(0, 0.8, 0));
		Fx.sound(level, aim, SoundEvents.BELL_RESONATE, 1.0F, 1.2F);
		Fx.sound(level, aim, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.8F);
	}
}
