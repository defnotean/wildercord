package dev.wildercord.gear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One piece of casting gear: a staff, the Tome of the Fifth Page or a focus. Pure data with no
 * Minecraft types (like {@link dev.wildercord.spell.RuneDef}), so the bonuses can be unit-tested and
 * the Cord screen, the HUD and the server all read the same numbers.
 *
 * <p>Each goes in its inventory slot ({@link GearSlot}) and works from there. Held instead, staffs work in
 * either hand and the tome and the foci only in the off-hand; a piece in its slot takes the place of
 * held pieces of its kind (see {@link GearBonuses}).</p>
 *
 * @param path         item path, e.g. {@code fire_staff}
 * @param element      staffs: the element they favour; otherwise ""
 * @param elementPower staffs: power multiplier on effects of their element
 * @param elementCost  staffs: cost multiplier on spells with an effect of their element
 * @param power        power multiplier on every effect (Focus of Thrift)
 * @param cost         cost multiplier on every spell (Focus of Thrift)
 * @param chargeSpeed  how much faster charged casts fill (Focus of Haste)
 * @param mana         extra max mana while it counts (Focus of the Deep Well)
 * @param echo         chance a spell goes off a second time (Focus of Echoes)
 * @param fifthSpell   opens a fifth spell (Tome of the Fifth Page)
 */
public record GearDef(String path, GearKind kind, String element, double elementPower, double elementCost, double power, double cost,
		double chargeSpeed, int mana, double echo, boolean fifthSpell) {

	public enum GearKind {
		STAFF,
		/** A staff from a boss: stronger. */
		GREATER_STAFF,
		TOME,
		FOCUS,
		/** The Breath Sash: nothing for spells, but it steadies the breath of aura (see {@code aura.world.ForgedGear}). */
		SASH;

		public boolean staff() {
			return this == STAFF || this == GREATER_STAFF;
		}
	}

	/** The elements a staff can favour, in Codex order. */
	public static final List<String> ELEMENTS = List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood");

	public static final double STAFF_POWER = 1.20;
	public static final double GREATER_STAFF_POWER = 1.35;
	public static final double STAFF_COST = 0.90;
	public static final double HASTE_SPEED = 1.40;
	public static final double THRIFT_COST = 0.85;
	public static final double THRIFT_POWER = 0.90;
	public static final int DEEP_WELL_MANA = 50;
	public static final double ECHO_CHANCE = 0.10;
	public static final double RESOLVE_PROTECTION = 0.20;
	public static final double RESOLVE_POWER = 0.85;

	private static final Map<String, GearDef> ALL = new LinkedHashMap<>();

	public static final GearDef TOME = register(new GearDef("tome_of_the_fifth_page", GearKind.TOME, "", 1, 1, 1, 1, 1, 0, 0, true));
	public static final GearDef HASTE = register(focus("focus_of_haste", 1, 1, HASTE_SPEED, 0, 0));
	public static final GearDef THRIFT = register(focus("focus_of_thrift", THRIFT_POWER, THRIFT_COST, 1, 0, 0));
	public static final GearDef DEEP_WELL = register(focus("focus_of_the_deep_well", 1, 1, 1, DEEP_WELL_MANA, 0));
	public static final GearDef ECHOES = register(focus("focus_of_echoes", 1, 1, 1, 0, ECHO_CHANCE));
	/** A defensive focus trades spell power for protection from incoming spell hits. */
	public static final GearDef RESOLVE = register(focus("focus_of_resolve", RESOLVE_POWER, 1, 1, 0, 0));
	public static final GearDef REPRIEVE = register(focus("focus_of_reprieve", 0.90, 1, 1, 0, 0));
	public static final GearDef GROUNDING = register(focus("focus_of_grounding", 0.90, 1, 1, 0, 0));
	/** Worn where the tome goes (a swordsman's choice against a fifth spell): more aura, and a steadier breathing stance. */
	public static final GearDef BREATH_SASH = register(new GearDef("breath_sash", GearKind.SASH, "", 1, 1, 1, 1, 1, 0, 0, false));

	static {
		for (String element : ELEMENTS) {
			register(new GearDef(element + "_staff", GearKind.STAFF, element, STAFF_POWER, STAFF_COST, 1, 1, 1, 0, 0, false));
		}
		for (String element : ELEMENTS) {
			register(new GearDef("greater_" + element + "_staff", GearKind.GREATER_STAFF, element, GREATER_STAFF_POWER, STAFF_COST, 1, 1, 1, 0, 0, false));
		}
	}

	private static GearDef focus(String path, double power, double cost, double chargeSpeed, int mana, double echo) {
		return new GearDef(path, GearKind.FOCUS, "", 1, 1, power, cost, chargeSpeed, mana, echo, false);
	}

	private static GearDef register(GearDef def) {
		if (ALL.put(def.path(), def) != null) {
			throw new IllegalStateException("Duplicate gear " + def.path());
		}
		return def;
	}

	public static Optional<GearDef> get(String path) {
		return Optional.ofNullable(ALL.get(path));
	}

	public static List<GearDef> all() {
		return Collections.unmodifiableList(new ArrayList<>(ALL.values()));
	}

	public static GearDef staff(String element) {
		return ALL.get(element + "_staff");
	}

	public static GearDef greaterStaff(String element) {
		return ALL.get("greater_" + element + "_staff");
	}

	/** Whether it works held in this hand: staffs from either, everything else only from the off-hand (a slotted piece needs no hand). */
	public boolean worksIn(boolean mainHand) {
		return !mainHand || kind.staff();
	}
}
