package dev.wildercord.gametest.perf;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraCombat;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.ElementalMasters;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Priority 8 performance harness: eight fake players join, cast a farming / delving / warding mix with hearth passives on,
 * and fight two Sword Masters with swings and Aura arts for a sustained {@value #MEASURE}-tick window. It records server tick
 * time (mean, p95, max), entities, particles and packets per tick, and the size of every static per-player store before,
 * during and after; then the players leave and nothing of theirs may stay behind. Budgets are deliberately generous: this
 * catches regressions and leaks, it is not a frame-time pass/fail. Numbers land in build/perf/perf-harness.json (see
 * docs/testing/performance.md).
 */
public class PerformanceHarnessTest implements FabricClientGameTest {
	static final int PLAYERS = 8, WARMUP = 100, MEASURE = 600, SAMPLE = 20, GRACE = 260;
	/** Regression budgets, milliseconds of server work per tick (a healthy dev machine sits far below them). */
	static final double MEAN_BUDGET = 40, P95_BUDGET = 120, MAX_BUDGET = 2500;
	static final float HEALTH = 200;

	/** What may name a departed player on purpose, and why. */
	static final Map<String, String> KEPT = Map.of(
		"dev.wildercord.duel.Duels.LAST_HURT", "a relog never skips the duel readiness rest (swept on leave once rested, past 256)",
		"dev.wildercord.duel.Duels.LAST_PVP", "as LAST_HURT",
		"dev.wildercord.cast.Statuses.CLAIMS", "a per-creature stacking guard a relog must not reset; swept 600 ticks after its last grant",
		"dev.wildercord.cast.events.WorldEvents.STORMS", "a live mana storm's per-player cast tally and rune-given set, so a relog under it never resets toward or repeats its rune; gone with the storm (at most EventRules.MAX_STORMS, minutes long)",
		"dev.wildercord.cast.Mastery.MEMORY", "mastery's repetition memory outlives a relog so relogging never resets diminishing returns; one small entry per player per server run",
		"dev.wildercord.cast.packs.WardState.GRACE_SPENT", "Grace's once-per-10-minutes save rest outlives a relog by design (swept once rested)",
		"dev.wildercord.cast.packs.WardState.FAITHFUL_SPENT", "Faithful's save rest, as Grace (swept once rested)",
		"dev.wildercord.cast.packs.WardState.AEGIS_SPENT", "Aegis' 2-minute rest, as Grace (swept once rested)",
		"dev.wildercord.cast.WayfarerEffects.LOCATED", "the structure-search rest still running; dropped at leave once rested, so a relog can't skip it");

	private static final class Bench extends FakePlayer {
		Bench(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}

	/** One loadout per bench: four spells (shape + rune) for the Echo Cord's four slots. */
	static final String[][][] LOADOUTS = {
		{{"bolt", "tillage"}, {"bolt", "dewfall"}, {"bolt", "ripen"}, {"bolt", "sow"}},
		{{"self", "mending_mist"}, {"self", "hearthglow"}, {"self", "sanctuary"}, {"self", "arrowveil"}},
		{{"self", "deepsound"}, {"self", "oretally"}, {"self", "hollowsense"}, {"self", "headlamp"}},
		{{"self", "aegis"}, {"self", "citadel"}, {"self", "corral"}, {"bolt", "keepsafe"}},
		{{"bolt", "tillage"}, {"bolt", "dewfall"}, {"bolt", "ripen"}, {"bolt", "sow"}},
		{{"bolt", "gloomsight"}, {"bolt", "lumenpath"}, {"bolt", "torchfall"}, {"self", "caveward"}},
		{{"self", "mending_mist"}, {"self", "hearthglow"}, {"self", "sanctuary"}, {"self", "arrowveil"}},
		{{"bolt", "tillage"}, {"self", "mending_mist"}, {"self", "deepsound"}, {"self", "caveward"}}};
	/** Two hearth passives (a passive holds two runes; five circles open two slots). */
	static final List<List<String>> PASSIVES = List.of(List.of("self", "slowburn"), List.of("self", "warm_cloak"));

	private static volatile Consumer<MinecraftServer> drive;
	private static volatile boolean timing;
	private static long tickStart;
	private static final long[] TICKS = new long[MEASURE * 2];
	private static int recorded;
	private static boolean hooked;

	private ServerLevel level;
	private Vec3 origin;
	private final List<Bench> bench = new ArrayList<>();
	private final SwordMaster[] masters = new SwordMaster[2];
	private final Map<String, int[]> casts = new TreeMap<>();
	private final int[] arts = new int[2], swings = new int[1], respawns = new int[1];
	private final List<String> passiveProblems = new ArrayList<>();
	private int clock;

	private static synchronized void hook() {
		if (hooked) return;
		hooked = true;
		ServerTickEvents.START_SERVER_TICK.register(server -> tickStart = System.nanoTime());
		ServerTickEvents.START_SERVER_TICK.register(server -> { Consumer<MinecraftServer> d = drive; if (d != null) d.accept(server); });
		ServerTickEvents.END_SERVER_TICK.register(server -> { if (timing && recorded < TICKS.length) TICKS[recorded++] = System.nanoTime() - tickStart; });
	}

	@Override public void runTest(ClientGameTestContext context) {
		hook();
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "time set 6000", "gamerule advance_time false"))
				world.getServer().runCommand(command);
			Map<String, Integer> sizesBefore = sorted(world.getServer().computeOnServer(server -> RetainedScan.sizes(false)));
			sizesBefore.putAll(context.computeOnClient(mc -> RetainedScan.sizes(true)));
			world.getServer().runOnServer(server -> {
				ServerPlayer observer = server.getPlayerList().getPlayers().getFirst();
				level = observer.level(); origin = new Vec3(observer.getBlockX() + .5, 181, observer.getBlockZ() + .5);
				for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
					BlockPos floor = BlockPos.containing(origin).offset(x, -1, z);
					level.setBlockAndUpdate(floor, Blocks.GRASS_BLOCK.defaultBlockState());
					for (int y = 1; y <= 5; y++) level.setBlockAndUpdate(floor.above(y), Blocks.AIR.defaultBlockState());
				}
				observer.setGameMode(GameType.SPECTATOR);
				observer.teleportTo(level, origin.x, origin.y + 9, origin.z - 18, Set.of(), 0, 25, false);
				for (int i = 0; i < PLAYERS; i++) bench.add(join(server, i));
				for (int k = 0; k < masters.length; k++) masters[k] = master(k);
			});
			drive = this::tick;
			context.waitTicks(WARMUP);
			PerfCounters.reset(); recorded = 0; PerfCounters.on = true; timing = true;
			List<int[]> serverEntities = new ArrayList<>(), client = new ArrayList<>();
			for (int t = 0; t < MEASURE; t += SAMPLE) {
				context.waitTicks(SAMPLE);
				serverEntities.add(world.getServer().computeOnServer(server -> {
					int n = 0; for (ServerLevel l : server.getAllLevels()) for (Entity ignored : l.getAllEntities()) n++;
					return new int[] {n};
				}));
				client.add(context.computeOnClient(mc -> new int[] {mc.level == null ? -1 : mc.level.getEntityCount(), particles(mc.particleEngine.countParticles())}));
			}
			timing = false; PerfCounters.on = false;
			int measured = recorded;
			long[] ticks = Arrays.copyOf(TICKS, measured);
			long realPackets = PerfCounters.PACKETS.get(), fakePackets = PerfCounters.FAKE_PACKETS.get();
			long particlePackets = PerfCounters.PARTICLE_PACKETS.get(), particles = PerfCounters.PARTICLES.get();
			Set<UUID> ids = bench.stream().map(Entity::getUUID).collect(Collectors.toSet());
			Map<String, Integer> sizesLoad = sorted(world.getServer().computeOnServer(server -> RetainedScan.sizes(false)));
			sizesLoad.putAll(context.computeOnClient(mc -> RetainedScan.sizes(true)));
			Map<String, Integer> heldLoad = sorted(world.getServer().computeOnServer(server -> RetainedScan.holding(false, ids)));
			heldLoad.putAll(context.computeOnClient(mc -> RetainedScan.holding(true, ids)));

			// ---- leave: the real disconnect path (Fabric's DISCONNECT, then the player list's own removal)
			drive = null;
			world.getServer().runOnServer(server -> {
				for (SwordMaster m : masters) if (m != null) m.discard();
				for (Bench p : bench) {
					MastersArts.cancel(p);
					ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(p.connection, server);
					server.getPlayerList().remove(p);
				}
				bench.clear(); Arrays.fill(masters, null);
			});
			context.waitTicks(GRACE);
			for (int i = 0; i < 3; i++) { System.gc(); context.waitTicks(1); }
			Map<String, Integer> sizesAfter = sorted(world.getServer().computeOnServer(server -> RetainedScan.sizes(false)));
			sizesAfter.putAll(context.computeOnClient(mc -> RetainedScan.sizes(true)));
			Map<String, Integer> heldAfter = sorted(world.getServer().computeOnServer(server -> RetainedScan.holding(false, ids)));
			heldAfter.putAll(context.computeOnClient(mc -> RetainedScan.holding(true, ids)));
			boolean listed = world.getServer().computeOnServer(server -> ids.stream().anyMatch(id -> server.getPlayerList().getPlayer(id) != null));
			Map<String, Integer> leaked = new TreeMap<>(heldAfter); leaked.keySet().removeAll(KEPT.keySet());

			// ---- numbers
			long[] sorted = ticks.clone(); Arrays.sort(sorted);
			double mean = Arrays.stream(ticks).average().orElse(0) / 1e6, p95 = pct(sorted, .95) / 1e6, max = sorted.length == 0 ? 0 : sorted[sorted.length - 1] / 1e6;
			Map<String, Object> json = new LinkedHashMap<>();
			json.put("suite", getClass().getName());
			json.put("players", PLAYERS); json.put("warmupTicks", WARMUP); json.put("measuredTicks", measured);
			json.put("tickMs", Map.of("mean", round(mean), "p50", round(pct(sorted, .5) / 1e6), "p95", round(p95), "max", round(max)));
			json.put("budgetsMs", Map.of("mean", MEAN_BUDGET, "p95", P95_BUDGET, "max", MAX_BUDGET));
			json.put("serverEntities", stats(serverEntities, 0)); json.put("clientEntities", stats(client, 0)); json.put("clientParticles", stats(client, 1));
			json.put("packetsPerTick", Map.of("observer", round((double) realPackets / Math.max(1, measured)), "allFakePlayers", round((double) fakePackets / Math.max(1, measured)),
				"perFakePlayer", round((double) fakePackets / Math.max(1, measured) / PLAYERS)));
			json.put("particlePacketsPerTick", round((double) particlePackets / Math.max(1, measured)));
			json.put("particlesSentPerTick", round((double) particles / Math.max(1, measured)));
			json.put("casts", casts.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[1] + "/" + e.getValue()[0], (a, b) -> a, TreeMap::new)));
			json.put("arts", arts[1] + "/" + arts[0]); json.put("swings", swings[0]); json.put("masterRespawns", respawns[0]);
			json.put("passiveProblems", passiveProblems);
			json.put("retainedGrowthDuringLoad", growth(sizesBefore, sizesLoad));
			json.put("retainedGrowthAfterLeave", growth(sizesBefore, sizesAfter));
			json.put("namingPlayersDuringLoad", heldLoad);
			json.put("namingPlayersAfterLeave", heldAfter);
			json.put("keptByDesign", KEPT.entrySet().stream().filter(e -> heldAfter.containsKey(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new)));
			json.put("leaked", leaked);
			String text = write(json);
			dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_PERF {}", text.replace('\n', ' '));

			check(measured >= MEASURE - SAMPLE, "The harness measured a sustained window: " + measured + " ticks");
			check(casts.values().stream().mapToInt(c -> c[1]).sum() > 0 && arts[0] > 0 && swings[0] > 0, "The load actually ran: casts=" + casts + " arts=" + Arrays.toString(arts));
			check(!heldLoad.isEmpty(), "The scan sees per-player state while the players are online (else it proves nothing)");
			check(!listed, "Departed players are gone from the player list");
			check(leaked.isEmpty(), "Retained per-player state is released on disconnect: " + leaked);
			check(mean <= MEAN_BUDGET && p95 <= P95_BUDGET && max <= MAX_BUDGET,
				String.format(Locale.ROOT, "Server tick within generous budgets: mean %.2f p95 %.2f max %.2f ms", mean, p95, max));
		} finally {
			drive = null; timing = false; PerfCounters.on = false;
		}
	}

	// ------------------------------------------------------------------ the load

	private Bench join(MinecraftServer server, int i) {
		Bench p = new Bench(level, "PerfBench" + i);
		p.setGameMode(GameType.SURVIVAL);
		p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); p.setHealth(HEALTH);
		Vec3 at = seat(i);
		p.snapTo(at.x, at.y, at.z, 0, 0);
		level.addNewPlayer(p);
		server.getPlayerList().getPlayers().add(p);
		server.getPlayerList().getPlayersByUUID().put(p.getUUID(), p);
		server.getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(p)));
		p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
		p.setAttached(WildercordAttachments.CIRCLES, 5);
		Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
		var book = Spellbooks.get(p);
		for (List<String> passive : PASSIVES) for (String id : passive) book = book.learn(rune(id).id());
		for (int s = 0; s < 4; s++) {
			List<String> spell = Arrays.stream(LOADOUTS[i][s]).map(id -> rune(id).id()).toList();
			for (String id : spell) book = book.learn(id);
			book = book.withSpell(s, spell);
		}
		Spellbooks.set(p, book);
		for (int slot = 0; slot < PASSIVES.size(); slot++) {
			var problem = SpellCaster.editPassive(p, slot, PASSIVES.get(slot).stream().map(id -> rune(id).id()).toList());
			if (problem != null) passiveProblems.add(p.getGameProfile().name() + ": " + problem.getString());
			if (!Spellbooks.get(p).passiveOn(slot)) SpellCaster.togglePassive(p, slot);
		}
		refill(p);
		return p;
	}

	private SwordMaster master(int k) {
		SwordMaster m = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		check(m != null, "The registered Master exists");
		Vec3 home = home(k);
		m.snapTo(home.x, home.y, home.z, 0, 0); level.addFreshEntity(m);
		m.setDiscipline(ElementalMasters.SCHOOL_IDS.get(k % ElementalMasters.SCHOOL_IDS.size()));
		for (int i = k; i < PLAYERS; i += masters.length) { Bench p = bench.get(i); m.interact(p, InteractionHand.MAIN_HAND, m.position()); m.interact(p, InteractionHand.MAIN_HAND, m.position()); }
		m.setTarget(bench.get(k));
		return m;
	}

	private Vec3 home(int k) { return origin.add(k == 0 ? -7 : 7, 0, 0); }

	private Vec3 seat(int i) {
		double a = Math.PI * 2 * (i / masters.length) / (PLAYERS / masters.length);
		return home(i % masters.length).add(Math.cos(a) * 2.6, 0, Math.sin(a) * 2.6);
	}

	private void refill(Bench p) {
		p.setHealth(p.getMaxHealth());
		Spellbooks.setMana(p, 300);
		p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.FORM, 1800, 110, 0));
	}

	private void tick(MinecraftServer server) {
		int t = clock++;
		for (int k = 0; k < masters.length; k++) if (t % 20 == 0 && (masters[k] == null || !masters[k].isAlive())) { masters[k] = master(k); respawns[0]++; }
		for (int i = 0; i < bench.size(); i++) {
			Bench p = bench.get(i);
			if (!p.isAlive()) continue;
			SwordMaster m = masters[i % masters.length];
			if (t % 20 == i % 20) refill(p);
			else if (t % 5 == 0) p.setHealth(p.getMaxHealth());
			if (t % 20 == (i * 7) % 20) {
				Vec3 at = seat(i);
				Vec3 look = m.position().subtract(at);
				float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
				boolean farming = LOADOUTS[i][0][1].equals("tillage");
				p.teleportTo(level, at.x, at.y, at.z, Set.of(), yaw, farming ? 50 : 5, false);
				p.setDeltaMovement(Vec3.ZERO);
			}
			if ((t + i * 3) % 15 == 0) cast(p, i, (t / 15 + i) % 4);
			if ((t + i) % 8 == 0) { AuraCombat.swing(p); swings[0]++; if (p.distanceTo(m) < 4) p.attack(m); }
			if ((t + i * 5) % 40 == 0) { arts[0]++; if (MastersArts.activate(p, (t / 40 + i) % 4)) arts[1]++; }
		}
	}

	private void cast(Bench p, int i, int slot) {
		String key = String.join("+", LOADOUTS[i][slot]);
		int[] tally = casts.computeIfAbsent(key, k -> new int[2]);
		Spellbooks.setReadyAt(p, slot, 0);
		Spellbooks.setMana(p, 300);
		tally[0]++;
		SpellCaster.cast(p, slot);
		if (Spellbooks.readyAt(p, slot) > level.getGameTime()) tally[1]++;
	}

	// ------------------------------------------------------------------ helpers

	private static RuneDef rune(String id) { return Runes.get("wildercord:" + id).orElseThrow(() -> new AssertionError("Missing rune " + id)); }
	private static Map<String, Integer> sorted(Map<String, Integer> m) { return new TreeMap<>(m); }

	private static int particles(String counted) {
		var m = java.util.regex.Pattern.compile("\\d+").matcher(counted == null ? "" : counted);
		return m.find() ? Integer.parseInt(m.group()) : -1;
	}

	private static long pct(long[] sorted, double q) { return sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, (int) Math.ceil(q * sorted.length) - 1)]; }

	private static double round(double v) { return Math.round(v * 1000) / 1000.0; }

	private static Map<String, Object> stats(List<int[]> samples, int at) {
		int[] v = samples.stream().mapToInt(s -> s[at]).toArray();
		return Map.of("mean", round(Arrays.stream(v).average().orElse(0)), "max", Arrays.stream(v).max().orElse(0), "samples", v.length);
	}

	private static Map<String, String> growth(Map<String, Integer> before, Map<String, Integer> after) {
		Map<String, String> out = new TreeMap<>();
		Set<String> keys = new HashSet<>(before.keySet()); keys.addAll(after.keySet());
		for (String k : keys) {
			int a = before.getOrDefault(k, 0), b = after.getOrDefault(k, 0);
			if (a != b) out.put(k, a + " -> " + b);
		}
		return out;
	}

	private static String write(Map<String, Object> json) {
		String text = json(json, "");
		try {
			Path root = Path.of("").toAbsolutePath();
			while (root != null && !Files.exists(root.resolve("build.gradle"))) root = root.getParent();
			Path dir = (root == null ? Path.of("build") : root.resolve("build")).resolve("perf");
			Files.createDirectories(dir);
			Files.writeString(dir.resolve("perf-harness.json"), text);
		} catch (Exception e) {
			dev.wildercord.Wildercord.LOGGER.warn("WILDERCORD_PERF could not write build/perf/perf-harness.json", e);
		}
		return text;
	}

	private static String json(Object v, String pad) {
		if (v instanceof Map<?, ?> m) {
			if (m.isEmpty()) return "{}";
			String in = pad + "  ";
			return m.entrySet().stream().map(e -> in + quote(String.valueOf(e.getKey())) + ": " + json(e.getValue(), in))
				.collect(Collectors.joining(",\n", "{\n", "\n" + pad + "}"));
		}
		if (v instanceof List<?> l) return l.stream().map(x -> json(x, pad)).collect(Collectors.joining(", ", "[", "]"));
		if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
		return quote(String.valueOf(v));
	}

	private static String quote(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""; }

	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
