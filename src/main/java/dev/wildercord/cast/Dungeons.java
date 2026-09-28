package dev.wildercord.cast;

import dev.wildercord.content.dungeons.DungeonBlocks;
import dev.wildercord.content.dungeons.DungeonItems;
import dev.wildercord.content.dungeons.DungeonSounds;
import dev.wildercord.world.dungeons.DungeonWorldgen;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * The three dimension dungeons (the Ember Sanctum, the Astral Observatory and the Drowned
 * Scriptorium) and their bosses, and the few places the spell engine tells them about a spell:
 * whether damage landing right now is a spell's (the Cinder Warden shrugs off anything else) and
 * where each effect lands (the Tide Scribe's flooded arena conducts storms and freezes under
 * frost). Everything else lives in the bosses themselves.
 */
public final class Dungeons {
	private Dungeons() {}

	/** How many spell hits are landing right now (nested: a hit can set off another). */
	private static int landing;

	public static void init() {
		DungeonBlocks.init();
		DungeonItems.init();
		DungeonSounds.init();
		DungeonEntities.init();
		DungeonWorldgen.init();
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			landing = 0;
			TideScribe.clearArenas();
		});
	}

	/** Runs a spell's damage: while it runs, {@link #spellLanding()} is true. */
	static void spellHit(Runnable hurt) {
		landing++;
		try {
			hurt.run();
		} finally {
			landing--;
		}
	}

	/** Whether the damage being dealt right now comes from a spell (not a blade, an arrow, lava or a fall). */
	public static boolean spellLanding() {
		return landing > 0;
	}

	/** Every effect of every spell, where it landed: cheap unless a Tide Scribe's arena is awake. */
	static void onSpell(Cast cast, Cast.Hit hit, String element) {
		if (TideScribe.anyAwake() && (element.equals("storm") || element.equals("frost"))) {
			TideScribe.onSpell(cast, hit, element);
		}
	}
}
