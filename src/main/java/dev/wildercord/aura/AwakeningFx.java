package dev.wildercord.aura;

import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.ArtLight;
import dev.wildercord.aura.arts.Awakenings;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * How an awakening looks and sounds on the server's side (the body's aura itself, burning in its awakened form, is drawn by every
 * client from the synced state: {@code client.render.AuraBodyLayer} and {@code client.AuraFxClient}).
 * <ul>
 * <li>{@link #transform}: the moment. The shared rising stinger and the method's own voice; the body's aura drawn in for a breath
 *     (each client draws the gathering) and then, at {@link AwakeningRules#BURST_AT}, the burst: a flash, rings and an echo at the
 *     heart, a shockwave over the ground, a column of light (seen by everyone else, and by the swordsman only in third person), the
 *     method's own flourish ({@link Awakenings#flourish}), foes close by thrown back a step, and the ground shaking a little. Its
 *     name, grand, by the side of the swordsman's own screen and over their head for everyone else.</li>
 * <li>{@link #burning}: while it lasts, the aura breathing out over the ground now and then.</li>
 * <li>{@link #fed}: a finisher feeding it, a flash at the heart.</li>
 * <li>{@link #spent} and {@link #recovered}: the aura guttering out into ash, and stirring again.</li>
 * </ul>
 */
public final class AwakeningFx {
	private AwakeningFx() {}

	/** Every sound an awakening plays (the tests check they're all in the feel kit). */
	public static final List<String> SOUNDS = List.of("aura_awaken", "aura_awaken_fed", "aura_spent", "aura_recovered", "aura_awaken_ember",
		"aura_awaken_rime", "aura_awaken_thunder", "aura_awaken_gale", "aura_awaken_stone", "aura_awaken_verdant", "aura_awaken_hollow",
		"aura_awaken_starlit", "aura_awaken_hourglass", "aura_awaken_crimson", "aura_awaken_steel",
		// ---- methods-a pack
		"aura_awaken_tide", "aura_awaken_iron", "aura_awaken_dune");

	private static final int WHITE = 0xFFFFFF;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** The method's own voice for its awakening ({@code aura_awaken_<method>}, plain steel for a method without one). */
	public static String voice(String methodId) {
		AwakeningRules.Flavour flavour = AwakeningRules.Flavour.of(methodId);
		return flavour == AwakeningRules.Flavour.PLAIN ? "aura_awaken_steel" : "aura_awaken_" + flavour.method;
	}

	/** The transformation, as {@code player} awakens for {@code ticks}. */
	static void transform(ServerPlayer player, int ticks) {
		ServerLevel level = player.level();
		Vec3 heart = player.position().add(0, 1.0, 0);
		Feels.sound(level, heart, "aura_awaken", 1.3F, 1.0F);
		Feels.sound(level, heart, voice(Aura.data(player).method()), 1.15F, 1.0F);
		AuraFx.bodyAuraFlare(player, 60, 1.0F);
		// Its name, grand: "Awakening", and over it the method and the stage it was reached at.
		Component method = Aura.method(player).<Component>map(m -> Component.translatable(m.nameKey())).orElse(Component.empty());
		Component stage = Component.translatable("aura.wildercord.stage." + AuraStages.id(Aura.stage(player)));
		AuraFx.banner(player, Component.translatable("aura.wildercord.awakening"), Component.translatable("aura.wildercord.banner.kicker", method, stage),
			AuraFxRules.BannerKind.GRAND);
		// The gathering: rings of its light closing in on the body, over the ground and round the waist, as the burst draws breath (for
		// everyone watching, and the swordsman in third person; each client draws the motes drawn in).
		int color = Aura.color(player);
		ArtLight show = ArtLight.spectacle(player);
		show.groundRing(player.position(), color, 3.4, 0.3, 0.1, AwakeningRules.BURST_AT + 1);
		show.bare().groundRing(player.position(), ArtKit.hot(color, 0.5), 2.4, 0.2, 0.05, AwakeningRules.BURST_AT);
		show.bare().ring(heart, UP, ArtKit.hot(color, 0.3), 2.0, 0.2, 0.05, AwakeningRules.BURST_AT);
		Scheduler.later(AwakeningRules.BURST_AT, () -> {
			if (player.isAlive() && !player.isRemoved() && Awakening.awakened(player)) {
				burst(player);
			}
		});
	}

	/** The burst, a few ticks into it. */
	private static void burst(ServerPlayer player) {
		ServerLevel level = player.level();
		int color = Aura.color(player);
		int hot = ArtKit.hot(color, 0.45);
		Vec3 feet = player.position();
		Vec3 heart = feet.add(0, 1.0, 0);
		// A flash and a ring at the heart (a whisper low in the swordsman's own view): kept modest, so the method's own flourish carries it.
		AuraFx.burst(level, player, heart, Vec3.ZERO, color, 1.9F, AuraFx.Burst.FLASH | AuraFx.Burst.RING | AuraFx.Burst.SPARKS);
		// A shockwave racing out over the ground, in two rings, and a column of light through them: for everyone else, and the swordsman
		// only in third person (through their own eyes rings racing out from under them would sweep across the view).
		ArtLight show = ArtLight.spectacle(player);
		show.groundRing(feet, color, 0.5, 5.0, 0.16, 14);
		Scheduler.later(3, () -> ArtLight.spectacle(player).groundRing(feet, hot, 0.6, 6.0, 0.06, 12));
		show.ray(feet, feet.add(0, 8.0, 0), color, 0.4, 16);
		show.bare().ray(feet, feet.add(0, 7.0, 0), hot, 0.18, 14);
		show.bare().ray(feet, feet.add(0, 6.0, 0), WHITE, 0.07, 12);
		Awakenings.flourish(player);
		// Foes standing close are thrown back a step (no harm: room to fight in).
		for (LivingEntity foe : ArtKit.around(player, feet, 3.5, 1.0, 2.5, 16)) {
			ArtKit.knock(foe, feet, 0.75, 0.3);
		}
		AuraFx.sound(player, AuraFx.Sound.IMPACT, 1.1F, 0.6F);
		ScreenFx.shake(level, heart, 0.3F, 10);
	}

	/** Each tick of an awakening, {@code age} ticks in with {@code left} to go: the aura breathing out over the ground now and then. */
	static void burning(ServerPlayer player, long age, long left) {
		if (age < AwakeningRules.RISE || age % 30 != 0) {
			return;
		}
		int color = Aura.color(player);
		ArtLight.spectacle(player).groundRing(player.position(), color, 0.4, 1.8, 0.08, 12);
		if (left <= AwakeningRules.GUTTER) {
			// Ending: motes falling away from it.
			Motes.burst(player.level(), player.position().add(0, 1.0, 0), 4, color, 0.06, 16, 0.05);
		}
	}

	/** A finisher fed the awakening a moment more. */
	static void fed(ServerPlayer player) {
		int color = Aura.color(player);
		Vec3 heart = player.position().add(0, 1.0, 0);
		AuraFx.burst(player.level(), player, heart, Vec3.ZERO, ArtKit.hot(color, 0.3), 1.4F, AuraFx.Burst.FLASH | AuraFx.Burst.RING);
		AuraFx.bodyAuraFlare(player, 24, 0.8F);
		Feels.sound(player.level(), heart, "aura_awaken_fed", 0.7F, 1.0F);
	}

	/** The awakening burned out: the aura guttering into ash, a long breath out. */
	static void spent(ServerPlayer player) {
		ServerLevel level = player.level();
		int color = Aura.color(player);
		Vec3 heart = player.position().add(0, 1.0, 0);
		Feels.sound(level, heart, "aura_spent", 1.0F, 1.0F);
		Motes.clouds(level, heart, 8, 0.45, 0x6A6478, 0.6, 34, new Vec3(0, 0.015, 0), 0.02, 0.4);
		Motes.burst(level, heart, 8, color, 0.06, 18, 0.06);
		ArtLight.world(player).groundRing(player.position(), 0x6A6478, 1.6, 0.4, 0.08, 14);
		player.sendOverlayMessage(Component.translatable("message.wildercord.aura.spent").withColor(0xC8A0A0));
	}

	/** Spent no longer: the aura stirring again (only the swordsman hears it). */
	static void recovered(ServerPlayer player) {
		Feels.sound(player.level(), player.position().add(0, 1.0, 0), "aura_recovered", 0.6F, 1.0F);
		Motes.burst(player.level(), player.position().add(0, 0.6, 0), 5, Aura.color(player), 0.06, 14, 0.04);
		player.sendOverlayMessage(Component.translatable("message.wildercord.aura.recovered").withColor(0xFF000000 | Aura.color(player)));
	}
}
