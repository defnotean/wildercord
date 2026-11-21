package dev.wildercord.content.dungeons;

import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The dungeon bosses' own sounds, synthesised by {@code tools/sound_art.py} in the same key as the
 * rest (D major pentatonic), so a boss's cry and the spells flying around it never clash.
 */
public final class DungeonSounds {
	private DungeonSounds() {}

	/** A boss gathering itself between phases: a swelling, straining chord that breaks into a boom. Heard from 48 blocks. */
	public static final SoundEvent BOSS_PHASE = register("boss_phase");
	/** A boss waking over its altar: a deep toll, rising. Heard from 48 blocks. */
	public static final SoundEvent BOSS_RISE = register("boss_rise");

	/** The Cinder Warden: a low rumble of banked coals and a creak of chain. */
	public static final SoundEvent WARDEN_AMBIENT = register("warden_ambient");
	/** Struck through its armour: a cracked-bell clang and a hiss of steam. */
	public static final SoundEvent WARDEN_HURT = register("warden_hurt");
	/** Its fires going out: a long hiss, plates slumping, a last ember of a note. */
	public static final SoundEvent WARDEN_DEATH = register("warden_death");
	/** Its fist meeting the floor: a heavy thud and a roar of flame. */
	public static final SoundEvent WARDEN_SLAM = register("warden_slam");
	/** A spell glancing off its armour: a dull, dead anvil knock. */
	public static final SoundEvent WARDEN_IMMUNE = register("warden_immune");

	/** The Star-Eater: a slow, hollow hum under a glitter of far-off glass. */
	public static final SoundEvent STAR_EATER_AMBIENT = register("star_eater_ambient");
	/** Struck: a glassy crack over a low groan. */
	public static final SoundEvent STAR_EATER_HURT = register("star_eater_hurt");
	/** Falling in on itself: everything rushes inward, then a soft, bottomless boom. */
	public static final SoundEvent STAR_EATER_DEATH = register("star_eater_death");
	/** A spell turned back: a ping run backwards into a bright snap. */
	public static final SoundEvent STAR_EATER_REFLECT = register("star_eater_reflect");
	/** A star shard knocked back where it came from: a bright, ringing clang. */
	public static final SoundEvent SHARD_PARRY = register("shard_parry");

	/** The Tide Scribe: murmuring under water, bubbles and a scratch of a quill. */
	public static final SoundEvent TIDE_SCRIBE_AMBIENT = register("tide_scribe_ambient");
	/** Struck: a gurgling cry. */
	public static final SoundEvent TIDE_SCRIBE_HURT = register("tide_scribe_hurt");
	/** Sinking: a long, bubbling sigh as the ink runs out of it. */
	public static final SoundEvent TIDE_SCRIBE_DEATH = register("tide_scribe_death");
	/** The tide coming in: a swelling rush of water. Heard from 40 blocks. */
	public static final SoundEvent TIDE_RISE = register("tide_rise");
	/** The tide going out: water draining away with a long gurgle. Heard from 40 blocks. */
	public static final SoundEvent TIDE_EBB = register("tide_ebb");

	private static SoundEvent register(String path) {
		Identifier id = Wildercord.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {}
}
