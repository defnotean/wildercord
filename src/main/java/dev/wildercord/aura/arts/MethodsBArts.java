package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;

import java.util.List;

/**
 * Echo, Dawn and Venom Breath's arts, registered beside the built-in methods' ({@link MethodArts#init} calls {@link #init}). They are
 * kept apart from {@link MethodArts#METHODS}, which lists the ten built-in methods only.
 */
public final class MethodsBArts {
	private MethodsBArts() {}

	public static final List<String> METHODS = List.of(EchoArts.METHOD, DawnArts.METHOD, VenomArts.METHOD);

	/** Every sound their arts play (each an {@code aura_art_} voice in the aura feel kit). */
	public static final List<String> SOUNDS = List.of(
		"aura_art_ringing_cut", "aura_art_resonant_chord", "aura_art_counterpoint", "aura_art_reverb_step", "aura_art_grand_resonance",
		"aura_art_first_light", "aura_art_sunrise_arc", "aura_art_halo_guard", "aura_art_dawnbreak_rush", "aura_art_noon_zenith",
		"aura_art_fang_strike", "aura_art_spitting_cobra", "aura_art_shed_skin", "aura_art_serpent_slither", "aura_art_hydra_coil");

	public static void init() {
		AuraApi.registerArts(EchoArts.METHOD, EchoArts.arts());
		AuraApi.registerArts(DawnArts.METHOD, DawnArts.arts());
		AuraApi.registerArts(VenomArts.METHOD, VenomArts.arts());
		// Dawn's glint rest is per player; a player who leaves takes theirs with them.
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
			(handler, server) -> dev.wildercord.aura.MethodsBCoating.forget(handler.player.getUUID()));
	}
}
