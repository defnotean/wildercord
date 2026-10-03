package dev.wildercord.aura.world;

import java.util.List;

/** Three traditions, one discovery per player per tradition; no per-site or repeatable reward. */
public final class BattlefieldRules {
	private BattlefieldRules() {}
	public static final int REMEMBER_TICKS = 160;
	public static final double REACH = 4;
	public record Memory(String id, String title, String part, int colour) {
		public String key() { return "aura:battlefield_" + id; }
	}
	public static final List<Memory> MEMORIES = List.of(
		new Memory("broken_line", "The Line That Broke", "sunder", 0xC38A54),
		new Memory("last_shelter", "The Last Shelter", "bind", 0x78AD86),
		new Memory("returned_step", "The Returned Step", "echo", 0xB5A6D4));
	public static Memory memory(int kind) { return MEMORIES.get(Math.floorMod(kind, MEMORIES.size())); }
	public static boolean footing(int surface, int oceanFloor, int[] neighbours, int sea) {
		if (surface < sea || surface - oceanFloor > 1 || neighbours.length != 4) return false;
		for (int height : neighbours) if (Math.abs(height - surface) > 5) return false;
		return true;
	}
	public static boolean holding(boolean alive, boolean breathing, boolean weapon, boolean sameWorld, double distanceSquared) {
		return alive && breathing && weapon && sameWorld && distanceSquared <= REACH * REACH;
	}
}
