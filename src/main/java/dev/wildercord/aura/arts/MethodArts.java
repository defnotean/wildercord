package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.cast.Scheduler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The breathing methods' own arts: each method answers the five art strings ({@link AuraApi.ArtSlot}) its own way. All ten built-in
 * methods have theirs: {@link EmberArts}, {@link RimeArts}, {@link ThunderArts}, {@link GaleArts}, {@link StoneArts},
 * {@link VerdantArts}, {@link HollowArts}, {@link StarlitArts}, {@link HourglassArts} and {@link CrimsonArts}; an add-on's method
 * without arts of its own plays the common ones.
 *
 * <p>To give a method its arts: write a class like {@link EmberArts} whose {@code arts()} lists one art a slot, built with
 * {@link #art} (its price and rest come from {@link ArtRules}, by id), and register the list here with
 * {@link AuraApi#registerArts}. An art's performer decides what happens and draws it ({@link ArtKit} for who it touches and what it
 * does to them, {@link ArtLight} for its shaped light, {@link ArtFields} for what it leaves on the ground, {@link ArtBlocks} for
 * stone and ice that rise and sink, {@code aura.AuraFx} for its trail and impacts); the framework pays its price, rests it, names
 * it in a banner, flares the body's aura, plays the method's technique sound under its own voice, and writes it in the Grimoire
 * the first time.</p>
 */
public final class MethodArts {
	private MethodArts() {}

	/** The methods with arts of their own, in element order. */
	public static final List<String> METHODS = List.of(EmberArts.METHOD, RimeArts.METHOD, ThunderArts.METHOD, GaleArts.METHOD, StoneArts.METHOD,
		VerdantArts.METHOD, HollowArts.METHOD, StarlitArts.METHOD, HourglassArts.METHOD, CrimsonArts.METHOD);

	/** Each art's own voice (tools/feel/aura_arts.py), for the tests: every one must exist. */
	public static final List<String> SOUNDS = List.of("aura_art_kindling_draw", "aura_art_rising_cinders", "aura_art_backdraft",
		"aura_art_wildfire_rush", "aura_art_sunfall", "aura_art_sunfall_impact", "aura_art_frostbite", "aura_art_hailfall", "aura_art_glacier_mirror",
		"aura_art_skate", "aura_art_winters_hush", "aura_art_winters_hush_shatter", "aura_art_crackle", "aura_art_skyfall", "aura_art_static_riposte",
		"aura_art_bolt_step", "aura_art_heavens_spear", "aura_art_cutting_breeze", "aura_art_updraft", "aura_art_eye_of_the_storm",
		"aura_art_tailwind", "aura_art_hundred_winds", "aura_art_rockbreaker", "aura_art_avalanche", "aura_art_unmoved", "aura_art_landslide",
		"aura_art_mountain_splitter",
		"aura_art_thorn_lash", "aura_art_blossom_fall", "aura_art_rooted_parry", "aura_art_wild_growth", "aura_art_groves_heart",
		"aura_art_void_cut", "aura_art_collapse", "aura_art_null_parry", "aura_art_rift_step", "aura_art_event_horizon", "aura_art_event_horizon_crush",
		"aura_art_star_needle", "aura_art_meteor_shower", "aura_art_constellation_guard", "aura_art_comet_dash", "aura_art_comet_dash_burst",
		"aura_art_nova", "aura_art_echo_cut", "aura_art_rewind_leap", "aura_art_stopped_moment", "aura_art_blur", "aura_art_thousand_moments",
		"aura_art_thousand_moments_release", "aura_art_bloodletting", "aura_art_red_rain", "aura_art_sanguine_parry", "aura_art_frenzy",
		"aura_art_crimson_moon");

	public static void init() {
		ArtFields.init();
		ArtWards.init();
		AuraApi.registerArts(EmberArts.METHOD, EmberArts.arts());
		AuraApi.registerArts(RimeArts.METHOD, RimeArts.arts());
		AuraApi.registerArts(ThunderArts.METHOD, ThunderArts.arts());
		AuraApi.registerArts(GaleArts.METHOD, GaleArts.arts());
		AuraApi.registerArts(StoneArts.METHOD, StoneArts.arts());
		AuraApi.registerArts(VerdantArts.METHOD, VerdantArts.arts());
		AuraApi.registerArts(HollowArts.METHOD, HollowArts.arts());
		AuraApi.registerArts(StarlitArts.METHOD, StarlitArts.arts());
		AuraApi.registerArts(HourglassArts.METHOD, HourglassArts.arts());
		AuraApi.registerArts(CrimsonArts.METHOD, CrimsonArts.arts());
	}

	/**
	 * An art in {@code slot} with id {@code id}, priced and rested by {@link ArtRules#art} (and held to the slot it's written
	 * for there).
	 */
	static AuraApi.StringArt art(AuraApi.ArtSlot slot, String id, AuraApi.ArtPerformer performer) {
		ArtRules.Art numbers = ArtRules.art(id);
		if (numbers.slot() != slot.ordinal()) {
			throw new IllegalStateException("art " + id + " is priced for slot " + numbers.slot() + " but written for " + slot);
		}
		return slot.art(id, numbers.cost(), numbers.cooldown(), performer);
	}

	/** An art with nowhere to go (a rush against a wall, a blink with nobody near): it doesn't go, and says why. */
	static void blocked(ServerPlayer player, String id) {
		player.sendOverlayMessage(Component.translatable("message.wildercord.aura.art.blocked", Component.translatable("aura.wildercord.art." + id))
			.withColor(0xA89CC8));
	}

	/**
	 * Runs {@code action} where the swordsman comes down: once they've left the ground and touched it again, or straight away if
	 * they never leave it (a ceiling), or when {@code timeout} runs out (a long fall, water, wings), whichever is first. Their fall
	 * is forgotten all the way down: an art's leap never hurts its swordsman.
	 */
	static void whenLanded(ServerPlayer player, int timeout, Consumer<Vec3> action) {
		ServerLevel level = player.level();
		boolean[] airborne = {!player.onGround()};
		int[] waited = {0};
		Runnable[] step = new Runnable[1];
		step[0] = () -> {
			if (!player.isAlive() || player.level() != level) {
				return;
			}
			player.resetFallDistance();
			waited[0]++;
			if (!player.onGround()) {
				airborne[0] = true;
			}
			boolean landed = airborne[0] && player.onGround();
			boolean stuck = !airborne[0] && waited[0] >= 6;
			if (landed || stuck || waited[0] >= timeout || player.isInWater() || player.isFallFlying() || player.getAbilities().flying) {
				action.accept(player.position());
				return;
			}
			Scheduler.later(1, step[0]);
		};
		Scheduler.later(1, step[0]);
	}

	// ------------------------------------------------------------------ lifecycle

	public static void forget(UUID id) {
		ArtKit.forget(id);
		ArtFields.forget(id);
		ArtWards.forget(id);
	}

	public static void clear() {
		ArtKit.clear();
		ArtFields.clear();
		ArtWards.clear();
		ArtBlocks.clear();
	}
}
