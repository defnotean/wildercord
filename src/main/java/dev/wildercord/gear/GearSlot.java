package dev.wildercord.gear;

import dev.wildercord.gear.GearDef.GearKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A place casting gear is worn: one inventory slot for one family of gear. Pure data with no
 * Minecraft types (like {@link GearDef}), so the rule, the menus, the screens and the renderer all
 * read the same list, and a new kind of gear gets its slot with one line here: its id, how it shows
 * on the wearer and which {@link GearKind}s fit.
 *
 * <p>The slot's place in the inventory follows from its position in the list (see
 * {@code menu.GearInventorySlot}), its empty-slot icon is {@code container/slot/gear_<id>}, and its
 * name and hint are {@code gear_slot.wildercord.<id>} and {@code gear_slot.wildercord.<id>.hint}.</p>
 */
public final class GearSlot {
	/** How a piece in the slot is drawn on the character. */
	public enum Look {
		/** Strapped diagonally across the back, its head over a shoulder. */
		BACK,
		/** Hovering just off a shoulder, bobbing and turning. */
		SHOULDER,
		/** Hanging at the hip from a belt. */
		HIP
	}

	private static final Map<String, GearSlot> ALL = new LinkedHashMap<>();

	/** Any staff, greater ones too. */
	public static final GearSlot STAFF = register("staff", Look.BACK, GearKind.STAFF, GearKind.GREATER_STAFF);
	/** Any focus. */
	public static final GearSlot FOCUS = register("focus", Look.SHOULDER, GearKind.FOCUS);
	/** The Tome of the Fifth Page. */
	public static final GearSlot TOME = register("tome", Look.HIP, GearKind.TOME);

	private final String id;
	private final int index;
	private final Look look;
	private final Set<GearKind> kinds;

	private GearSlot(String id, int index, Look look, Set<GearKind> kinds) {
		this.id = id;
		this.index = index;
		this.look = look;
		this.kinds = kinds;
	}

	/**
	 * Adds a slot for these kinds of gear. Call it while the mod is loading (before any menu is built):
	 * the inventory, the creative tab and the wearer's look all follow this list.
	 */
	public static GearSlot register(String id, Look look, GearKind first, GearKind... rest) {
		Set<GearKind> kinds = EnumSet.of(first, rest);
		for (GearSlot other : ALL.values()) {
			for (GearKind kind : kinds) {
				if (other.kinds.contains(kind)) {
					throw new IllegalStateException("Gear kind " + kind + " already has the " + other.id + " slot");
				}
			}
		}
		GearSlot slot = new GearSlot(id, ALL.size(), look, Collections.unmodifiableSet(kinds));
		if (ALL.put(id, slot) != null) {
			throw new IllegalStateException("Duplicate gear slot " + id);
		}
		return slot;
	}

	/** Every slot, in inventory order. */
	public static List<GearSlot> all() {
		return Collections.unmodifiableList(new ArrayList<>(ALL.values()));
	}

	/** The slot with this id, or null. */
	public static GearSlot get(String id) {
		return ALL.get(id);
	}

	/** The slot a kind of gear goes in, or null if it has none (it then works held, as before). */
	public static GearSlot of(GearKind kind) {
		for (GearSlot slot : ALL.values()) {
			if (slot.kinds.contains(kind)) {
				return slot;
			}
		}
		return null;
	}

	/** The slot a piece of gear goes in, or null. */
	public static GearSlot of(GearDef def) {
		return def == null ? null : of(def.kind());
	}

	public String id() {
		return id;
	}

	/** Where it sits in the list: its place in the inventory, and in the menu after the Cord slot. */
	public int index() {
		return index;
	}

	public Look look() {
		return look;
	}

	public Set<GearKind> kinds() {
		return kinds;
	}

	/** Whether this piece of gear goes in this slot. */
	public boolean accepts(GearDef def) {
		return def != null && kinds.contains(def.kind());
	}

	/** The empty slot's icon, a sprite in the GUI atlas. */
	public String iconPath() {
		return "container/slot/gear_" + id;
	}

	public String nameKey() {
		return "gear_slot.wildercord." + id;
	}

	public String hintKey() {
		return "gear_slot.wildercord." + id + ".hint";
	}

	@Override
	public String toString() {
		return "GearSlot[" + id + "]";
	}
}
