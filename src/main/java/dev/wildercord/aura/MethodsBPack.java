package dev.wildercord.aura;

import java.util.List;

/**
 * Echo, Dawn and Venom Breath beside the built-in ten: the small pure facts the shared catalogs read from their one
 * {@code methods-b pack} block each (where their manuals are found, the byte a fighter carries for them).
 */
public final class MethodsBPack {
	private MethodsBPack() {}

	public static final List<String> METHODS = List.of(MethodsBArtRules.ECHO, MethodsBArtRules.DAWN, MethodsBArtRules.VENOM);

	/** A chest that may hold one of their manuals, and the chance out of 100. */
	public record Find(String id, String lootTable, int chance, String method) {}

	public static final List<Find> FINDS = List.of(
		new Find("echo_ancient_city", "minecraft:chests/ancient_city", 8, MethodsBArtRules.ECHO),
		new Find("dawn_desert_pyramid", "minecraft:chests/desert_pyramid", 8, MethodsBArtRules.DAWN),
		new Find("venom_jungle_temple", "minecraft:chests/jungle_temple", 10, MethodsBArtRules.VENOM));

	/** The synced byte a fighter of one of these methods carries: well clear of the built-in ten's indices. */
	public static final int FIGHTER_BASE = 20;

	/** The byte for {@code method}, or -1 when it isn't one of these. */
	public static int fighterIndex(String method) {
		int i = METHODS.indexOf(method);
		return i < 0 ? -1 : FIGHTER_BASE + i;
	}

	/** The method a fighter's byte names, or null when it isn't one of these. */
	public static String fighterMethod(int index) {
		int i = index - FIGHTER_BASE;
		return i >= 0 && i < METHODS.size() ? METHODS.get(i) : null;
	}
}
