package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.DuneRules;
import dev.wildercord.aura.IronRules;
import dev.wildercord.aura.TideRules;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tide, Iron and Dune's finishers (what one does is decided in {@code Stance}; here is how each looks and sounds, and the small
 * touch of its element it leaves).
 * <ul>
 * <li><b>Tide, Undertow</b>: a wave breaks over the foe and drags it out, soaked.</li>
 * <li><b>Iron, Quench</b>: a hammer of sparks falls on the foe; its armour cracks wide.</li>
 * <li><b>Dune, Dust Devil</b>: a spinning column of sand round the foe; it comes out blinded.</li>
 * </ul>
 */
public final class MethodsAFinishers {
	private MethodsAFinishers() {}

	public static final String UNDERTOW = "undertow";
	public static final String QUENCH = "quench";
	public static final String DUST_DEVIL = "dust_devil";

	public static final List<String> IDS = List.of(UNDERTOW, QUENCH, DUST_DEVIL);
	public static final List<String> SOUNDS = List.of("aura_finisher_tide", "aura_finisher_iron", "aura_finisher_dune");

	private static final int WHITE = 0xFFFFFF;

	static void init() {
		AuraApi.registerFinisher(TideArts.METHOD, new AuraApi.Finisher(UNDERTOW, MethodsAFinishers::undertow));
		AuraApi.registerFinisher(IronArts.METHOD, new AuraApi.Finisher(QUENCH, MethodsAFinishers::quench));
		AuraApi.registerFinisher(DuneArts.METHOD, new AuraApi.Finisher(DUST_DEVIL, MethodsAFinishers::dustDevil));
	}

	/** The frame: its trail, a grand impact, the stinger and its voice, and a seal at the foe's feet. Returns the foe's heart. */
	private static Vec3 frame(ServerPlayer player, LivingEntity foe, AuraFxRules.Stroke stroke, int color, String voice) {
		ServerLevel level = player.level();
		Vec3 heart = foe.getBoundingBox().getCenter();
		AuraFx.art(player).color(color).trail(stroke, false, 1.7F).impact(foe, AuraFxRules.Weight.GRAND).flare(40, 1.0F);
		Feels.sound(level, heart, "aura_finisher", 1.1F, 1.0F);
		Feels.sound(level, heart, voice, 1.0F, 1.0F);
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.0F, 0.72F);
		ArtLight world = ArtLight.world(player);
		world.groundRing(foe.position(), color, 0.3, 2.0, 0.12, 12);
		world.bare().groundRing(foe.position(), WHITE, 0.2, 1.4, 0.04, 9);
		return heart;
	}

	private static Vec3 away(ServerPlayer player, LivingEntity foe) {
		Vec3 d = foe.position().subtract(player.position());
		d = new Vec3(d.x, 0, d.z);
		return d.lengthSqr() < 1.0E-4 ? ArtKit.flat(player) : d.normalize();
	}

	/** Tide: a wave breaks over the foe and drags it out, soaked. */
	static void undertow(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		int color = Aura.color(player);
		Vec3 heart = frame(player, foe, AuraFxRules.Stroke.SWEEP, color, "aura_finisher_tide");
		ArtLight show = ArtLight.spectacle(player);
		Vec3 dir = away(player, foe);
		show.slash(heart.add(0, 0.5, 0), ArtKit.UP.add(dir.scale(0.4)).normalize(), dir, color, 1.6, 2.6, 0.3, 1, 10);
		show.bare().slash(heart.add(0, 0.6, 0), ArtKit.UP.add(dir.scale(0.4)).normalize(), dir, MethodsAFlavours.FOAM, 1.5, 2.2, 0.1, 1, 9);
		Vfx.emit(player.level(), ParticleTypes.SPLASH, heart, 40, 0.8, 0.3);
		Scheduler.later(3, () -> ArtLight.world(player).ground(foe.position(), SigilOption.RING, MethodsAFlavours.FOAM, 1.8, 40, 0.05));
		if (foe.isAlive()) {
			MethodsAFlavours.soak(player, foe, 80);
			MethodsAFlavours.current(foe, dir.scale(TideRules.FINISHER_THROW).add(0, 0.2, 0));
		}
	}

	/** Iron: a hammer of sparks falls on the foe; its armour cracks wide. */
	static void quench(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		int color = Aura.color(player);
		Vec3 heart = frame(player, foe, AuraFxRules.Stroke.FALLING, color, "aura_finisher_iron");
		ArtLight show = ArtLight.spectacle(player);
		show.bare().ray(heart.add(0, 4, 0), heart, MethodsAFlavours.SPARK, 0.35, 8);
		show.shards(heart, 2.2, 14, color, MethodsAFlavours.SPARK);
		show.flash(heart, MethodsAFlavours.SPARK, 1.8F);
		Vfx.emit(player.level(), ParticleTypes.LAVA, heart, 6, 0.4, 0.2);
		Vfx.emit(player.level(), ParticleTypes.CRIT, heart, 24, 0.6, 0.4);
		ArtLight.world(player).ground(foe.position(), SigilOption.TARGET, color | ArtLight.DARK, 1.8, 50, 0);
		if (foe.isAlive()) {
			MethodsAFlavours.sunder(player, foe, IronRules.FINISHER_ARMOUR, 100);
		}
	}

	/** Dune: a spinning column of sand round the foe; it comes out blinded. */
	static void dustDevil(ServerPlayer player, LivingEntity foe, AuraApi.FinisherContext context) {
		int color = Aura.color(player);
		Vec3 heart = frame(player, foe, AuraFxRules.Stroke.SPIN, color, "aura_finisher_dune");
		ArtLight.spectacle(player).swirl(foe.position(), Math.max(0.8, foe.getBbWidth()) + 0.4, foe.getBbHeight() + 2.0, 6, color, MethodsAFlavours.GOLD);
		Vfx.emit(player.level(), new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), heart.add(0, 1, 0), 40, 0.9, 0.05);
		ArtLight.world(player).ground(foe.position(), SigilOption.CRACKED, color, 1.8, 50, 0.04);
		if (foe.isAlive()) {
			MethodsAFlavours.blind(player, foe, DuneRules.FINISHER_BLIND);
		}
	}
}
