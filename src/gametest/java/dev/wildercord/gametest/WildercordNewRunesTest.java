package dev.wildercord.gametest;

import dev.wildercord.cast.Attunement;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneSources;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The runes of the world, in a real game: a sample of them cast at husks standing on a stone
 * platform high in the air (their effects checked on the husks and on the caster), and a Blank Rune
 * attuned by meditating in a biome that holds a rune (mushroom fields, filled in around the
 * platform with /fillbiome).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY} and {@code WILDERCORD_CORDS_ONLY}.</p>
 */
public class WildercordNewRunesTest implements FabricClientGameTest {
	/** The platform: high enough that no terrain gets in the way. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);

	/** One cast: its runes, how many husks stand in front of the caster, how long to wait, and what must be true afterward. */
	private record Sample(String name, List<RuneDef> runes, int husks, int ticks, Predicate<List<Mob>> husksOk, Predicate<ServerPlayer> casterOk) {}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			for (Sample sample : samples()) {
				String failure = cast(context, world, sample);
				if (failure != null) {
					failures.add(sample.name() + ": " + failure);
				}
			}
			String attuned = attune(context, world);
			if (attuned != null) {
				failures.add("Attunement: " + attuned);
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("The runes of the world went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static List<String> ids(List<RuneDef> runes) {
		return runes.stream().map(RuneDef::id).toList();
	}

	private static boolean hurt(LivingEntity e) {
		return !e.isAlive() || e.getHealth() < e.getMaxHealth();
	}

	private static boolean has(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
		MobEffectInstance instance = e.getEffect(effect);
		return instance != null && instance.getAmplifier() >= amplifier;
	}

	/** A 21x21 stone floor with open air above it, an Echo Cord, every rune known, and plenty of mana. */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 1) + " " + (z - 10) + " " + (x + 10) + " " + (y - 1) + " " + (z + 10) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 10) + " " + y + " " + (z - 10) + " " + (x + 10) + " " + (y + 8) + " " + (z + 10) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CRYSTALS, Mana.MAX_CRYSTALS);
			stand(player);
		});
	}

	/** The caster at the middle of the platform, facing south (+Z) and a little down, toward the husks. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
	}

	private static List<Sample> samples() {
		List<Sample> samples = new ArrayList<>();
		samples.add(new Sample("Resonant Shriek", List.of(Runes.BEAM, Runes.RESONANT_SHRIEK), 1, 30,
			h -> hurt(h.getFirst()) && h.getFirst().hasEffect(MobEffects.DARKNESS), p -> true));
		samples.add(new Sample("Infest", List.of(Runes.BEAM, Runes.INFEST), 1, 40,
			h -> hurt(h.getFirst()) && h.getFirst().hasEffect(MobEffects.SLOWNESS), p -> true));
		samples.add(new Sample("Tidecall", List.of(Runes.BEAM, Runes.TIDECALL), 2, 15,
			h -> h.stream().allMatch(WildercordNewRunesTest::hurt), p -> true));
		samples.add(new Sample("Hoarfrost", List.of(Runes.BEAM, Runes.HOARFROST), 1, 70,
			h -> hurt(h.getFirst()) && h.getFirst().getTicksFrozen() > 0, p -> true));
		samples.add(new Sample("Mire", List.of(Runes.BEAM, Runes.MIRE), 1, 10,
			h -> has(h.getFirst(), MobEffects.SLOWNESS, 3), p -> true));
		samples.add(new Sample("Soulfire", List.of(Runes.BEAM, Runes.SOULFIRE), 1, 50,
			h -> hurt(h.getFirst()), p -> true));
		samples.add(new Sample("Sandstorm", List.of(Runes.BEAM, Runes.SANDSTORM), 2, 30,
			h -> h.stream().allMatch(e -> hurt(e) && e.hasEffect(MobEffects.BLINDNESS)), p -> true));
		samples.add(new Sample("Fangs", List.of(Runes.BEAM, Runes.FANGS), 1, 20,
			h -> hurt(h.getFirst()), p -> true));
		samples.add(new Sample("Eclipse", List.of(Runes.BEAM, Runes.ECLIPSE), 1, 30,
			h -> hurt(h.getFirst()) && h.getFirst().hasEffect(MobEffects.BLINDNESS), p -> true));
		samples.add(new Sample("Starmaw", List.of(Runes.BEAM, Runes.STARMAW), 1, 5,
			h -> hurt(h.getFirst()) && !h.getFirst().hasEffect(MobEffects.SPEED), p -> true));
		samples.add(new Sample("Kindled", List.of(Runes.BEAM, Runes.HARM, Runes.KINDLED), 1, 3,
			h -> hurt(h.getFirst()) && h.getFirst().getRemainingFireTicks() > 0, p -> true));
		samples.add(new Sample("Portalfall", List.of(Runes.BEAM, Runes.PORTALFALL), 1, 40,
			h -> hurt(h.getFirst()), p -> true));
		samples.add(new Sample("Constellation", List.of(Runes.CONSTELLATION, Runes.HARM), 3, 5,
			h -> h.stream().allMatch(WildercordNewRunesTest::hurt), p -> true));
		samples.add(new Sample("Vortex", List.of(Runes.VORTEX, Runes.HARM), 1, 40,
			h -> hurt(h.getFirst()), p -> true));
		samples.add(new Sample("Shulkershell", List.of(Runes.SELF, Runes.SHULKERSHELL), 0, 3,
			h -> true, p -> has(p, MobEffects.RESISTANCE, 3)));
		samples.add(new Sample("Cinderheart", List.of(Runes.SELF, Runes.CINDERHEART), 1, 30,
			h -> true, p -> has(p, MobEffects.STRENGTH, 1) && p.hasEffect(MobEffects.FIRE_RESISTANCE)));
		samples.add(new Sample("Warcry", List.of(Runes.SELF, Runes.WARCRY), 0, 3,
			h -> true, p -> p.hasEffect(MobEffects.STRENGTH) && p.hasEffect(MobEffects.SPEED)));
		return samples;
	}

	/** Casts one sample from spell 1 and checks it. Returns what went wrong, or null. */
	private static String cast(ClientGameTestContext context, TestSingleplayerContext world, Sample sample) {
		List<Integer> husks = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			stand(player);
			List<Integer> out = new ArrayList<>();
			for (int i = 0; i < sample.husks(); i++) {
				Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				if (husk == null) {
					continue;
				}
				// The first straight ahead (where a beam goes), the rest either side of it.
				double side = i == 0 ? 0 : (i % 2 == 0 ? 1.8 : -1.8);
				husk.snapTo(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 5.5, 180, 0);
				husk.setNoAi(true);
				husk.addTag("wildercord.new_runes");
				// Something good for Starmaw to swallow.
				husk.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 0));
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				husk.addTag("wildercord.rolled");
				level.addFreshEntity(husk);
				out.add(husk.getId());
			}
			return out;
		});
		context.waitTicks(3);
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(sample.runes()));
			if (!Spellbooks.get(player).spells().getFirst().equals(ids(sample.runes()))) {
				return "the spell didn't thread (has " + Spellbooks.get(player).spells().getFirst() + ")";
			}
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, Mana.max(player));
			if (sample.runes().getFirst() == Runes.VORTEX) {
				// A vortex opens on the ground where you look: look down at the husk 5 blocks ahead.
				player.setXRot(18.0F);
			}
			long now = player.level().getGameTime();
			SpellCaster.cast(player, 0);
			return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
		});
		if (cast != null) {
			cleanup(context, world);
			return cast;
		}
		context.waitTicks(sample.ticks());
		String result = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			List<Mob> found = new ArrayList<>();
			for (int id : husks) {
				if (player.level().getEntity(id) instanceof Mob mob) {
					found.add(mob);
				}
			}
			if (found.size() < husks.size()) {
				// A husk that's gone was killed by the spell: that counts as hit.
				return sample.husks() > 0 && found.isEmpty() ? null : checkHusks(sample, found);
			}
			if (!checkCaster(sample, player)) {
				return "the caster doesn't show it";
			}
			return checkHusks(sample, found);
		});
		cleanup(context, world);
		return result;
	}

	private static String checkHusks(Sample sample, List<Mob> husks) {
		if (husks.isEmpty() || sample.husksOk().test(husks)) {
			return null;
		}
		Mob first = husks.getFirst();
		return "the husks don't show it (first: " + first.getHealth() + "/" + first.getMaxHealth() + " health, effects " + first.getActiveEffectsMap().keySet() + ")";
	}

	private static boolean checkCaster(Sample sample, ServerPlayer player) {
		return sample.casterOk().test(player);
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=wildercord.new_runes]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runCommand("kill @e[type=evoker_fangs]");
		context.waitTicks(20);
	}

	/**
	 * Attunement: the platform becomes mushroom fields (which always hold Sporebloom); the player
	 * holds a Blank Rune and sneaks without moving, and twenty seconds later holds a Sporebloom rune,
	 * with the attunement in the Grimoire.
	 */
	private static String attune(ClientGameTestContext context, TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fillbiome " + (x - 12) + " " + (y - 4) + " " + (z - 12) + " " + (x + 12) + " " + (y + 8) + " " + (z + 12)
			+ " minecraft:mushroom_fields");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WildercordItems.BLANK_RUNE, 2));
		});
		context.waitTicks(10);
		String biome = world.getServer().computeOnServer(server -> Attunement.placeOf(player(server)).biome());
		if (!biome.equals("minecraft:mushroom_fields")) {
			return "the platform should be mushroom fields (it's " + biome + ")";
		}
		context.getInput().holdKey(options -> options.keyShift);
		try {
			// Meditation settles in after a second of stillness; then twenty seconds of attuning.
			context.waitTicks(60);
			boolean meditating = world.getServer().computeOnServer(server -> player(server).getAttachedOrElse(WildercordAttachments.MEDITATING, false));
			if (!meditating) {
				return "sneaking still with a Cord on should be meditating";
			}
			double halfway = world.getServer().computeOnServer(server -> Attunement.progress(player(server)));
			if (halfway <= 0) {
				return "a Blank Rune held while meditating in mushroom fields should be attuning";
			}
			context.waitTicks(Attunement.CHECKS * 5 + 20);
		} finally {
			context.getInput().releaseKey(options -> options.keyShift);
		}
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			boolean found = Heart.discovered(player, "attune:mushroom_fields");
			int runes = 0;
			int blanks = 0;
			for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (RuneItem.runeOf(stack).filter(r -> r == Runes.SPOREBLOOM).isPresent()) {
					runes += stack.getCount();
				}
				if (stack.is(WildercordItems.BLANK_RUNE)) {
					blanks += stack.getCount();
				}
			}
			if (!found) {
				return "the attunement should be written into the Grimoire";
			}
			if (runes < 1) {
				return "the Blank Rune should have become a Sporebloom rune";
			}
			if (blanks > 1) {
				return "a Blank Rune should have been used up (" + blanks + " left of 2)";
			}
			if (!RuneSources.forSource("attunement:mushroom_fields").contains(Runes.SPOREBLOOM)) {
				return "Sporebloom should come from mushroom fields";
			}
			return null;
		});
	}
}
