package dev.wildercord.cast;

import dev.wildercord.api.WildercordEvents;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.ReweaveRules;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Native mechanism proof only: declares XII/Low Tide/study eligibility, then enters the real paid
 * server admission. It does not claim teaching, editor, network/key-input, rendering or progression proof.
 * Registered for diagnostic selection and the full descriptor, not the focused release rosters.
 */
public final class ReweaveFeasibilityTest implements FabricClientGameTest {
    private enum Mode { DISC, LANE, WARNING, LATE, BLOCKED, REENTER, RETIRE, ROUND_TRIP, BUDGET, UNLOADED_START, ECHO_EMPTY, LIVE_DISC, LIVE_LANE }
    private static final class Probe {
        final ServerPlayer owner;
        final Mode mode;
        TrainingDummy target;
        ReweaveFields.View initial;
        int payments, spent;
        float before, after;
        boolean changed, done;
        Throwable failure;
        UnloadedStart unresolved;
        Cast receipt;
        dev.wildercord.spell.RuneQuirks.Quirk echo;
        final List<Long> hitTicks = new ArrayList<>();
        final List<Float> damage = new ArrayList<>();
        Probe(ServerPlayer owner, Mode mode) { this.owner = owner; this.mode = mode; }
    }
    private static Probe probe;
    private static boolean registered;
    private static long nonce = 90_000;

    @Override public void runTest(ClientGameTestContext context) {
        register();
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> dev.wildercord.Wildercord.LOGGER.info(
                "WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.cast.ReweaveFeasibilityTest\",\"seed\":\"{}\"}", server.overworld().getSeed()));
            context.waitTicks(40);
            world.getServer().runCommand("gamerule spawn_mobs false");
            world.getServer().runCommand("gamerule fall_damage false");
            world.getServer().runCommand("gamerule natural_health_regeneration false");
            world.getServer().runCommand("time set midnight");
            List<Float> liveDisc = new ArrayList<>(), liveLane = new ArrayList<>();
            var config = world.getServer().computeOnServer(server -> dev.wildercord.config.Config.get());
            try {
                for (Mode mode : Mode.values()) {
                    world.getServer().runOnServer(server -> {
                        CampConcordNative.config(mode == Mode.BUDGET ? CampConcordNative.copy(config, Map.of("maxCreatures", 1)) : config);
                        var echo = mode == Mode.ECHO_EMPTY ? configureEcho(config, server) : null;
                        WorldQuirks.forget();
                        begin(RelayCircleTest.player(server), mode);
                        probe.echo = echo;
                    });
                    world.getServer().waitFor(server -> probe.done, 110);
                    world.getServer().runOnServer(server -> {
                        if (probe.failure != null) throw new AssertionError("Reweave " + mode, probe.failure);
                        List<Long> expected = switch (mode) {
                            case WARNING, UNLOADED_START -> List.of(8L, 48L, 68L);
                            case RETIRE, ROUND_TRIP -> List.of();
                            case BUDGET -> List.of(8L);
                            default -> List.of(8L, 28L, 48L, 68L);
                        };
                        check(probe.hitTicks.equals(expected), "Absolute native beat schedule: " + mode + " " + probe.hitTicks);
                        check(probe.payments == 1 && probe.spent > 0, "Exactly one paid AFTER_CAST across all conversion attempts");
                        check(Math.abs(probe.before - probe.after - probe.spent) < .01, "Real survival mana payment");
                        check(ReweaveFields.view(probe.owner) == null, "Original expiry retires every field");
                        check(ReweaveFields.rest(probe.owner) == probe.initial.created() + ReweaveRules.REST_TICKS, "Rest never renews");
                        if (mode == Mode.LIVE_DISC) liveDisc.addAll(probe.damage);
                        if (mode == Mode.LIVE_LANE) liveLane.addAll(probe.damage);
                        check(probe.owner.level().getBlockState(BlockPos.containing(probe.initial.center().add(1, -1, 1))).is(dev.wildercord.content.WildercordBlocks.RUNE_SEAL), "Harm never initiates a Rune-Seal write");
                        if (probe.unresolved != null) probe.unresolved.close();
                        probe.target.discard();
                        ReweaveFields.cancel(probe.owner);
                    });
                }
                check(liveDisc.size() == 4 && liveLane.size() == 4, "Both native A/B treatments ran four real beats");
                for (int i = 0; i < 4; i++) check(Math.abs(liveDisc.get(i) - liveLane.get(i)) < .01,
                    "Live rank change affects corresponding disc/lane beat equally: " + i + " " + liveDisc + " / " + liveLane);
                check(liveDisc.get(1) > liveDisc.getFirst() * 1.4, "A/B actually changes live rank I to III after release");
                world.getServer().runOnServer(server -> { CampConcordNative.config(config); admissionChecks(RelayCircleTest.player(server)); });
            } finally {
                world.getServer().runOnServer(server -> {
                    CampConcordNative.config(config); WorldQuirks.forget();
                    if (probe != null) {
                        if (probe.unresolved != null) { probe.unresolved.close(); probe.unresolved = null; }
                        ReweaveFields.cancel(probe.owner);
                        if (probe.target != null) probe.target.discard();
                    }
                    probe = null;
                });
            }
        } finally { probe = null; }
    }
    private static void prepare(ServerPlayer player) {
        ReweaveFields.cancel(player);
        RelayCircleTest.prepare(player, Runes.HARM);
        // Explicit synthetic eligibility setup, not a claimed Tide Scribe kill or completed lesson.
        player.setAttached(WildercordAttachments.CIRCLES, 12);
        player.setAttached(WildercordAttachments.CONDENSED, 300000);
        player.setAttached(WildercordAttachments.RUNEBOUND_SLAIN, 20);
        player.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:tide_scribe", ReweaveRules.STUDY));
        player.setAttached(WildercordAttachments.AFFINITY, Map.of());
        player.setAttached(WildercordAttachments.RUNE_RANKS, Map.of());
        player.setAttached(MasteryAttachments.MASTERY, MasteryBook.EMPTY);
        Spellbooks.set(player, new Spellbook(ReweaveRules.IDS, List.of(ReweaveRules.IDS), 0, true));
        ((ServerLevelData) player.level().getLevelData()).setGameTime(player.level().getGameTime() + 200);
        for (int slot = 0; slot < dev.wildercord.gear.SpellSlots.ALL; slot++) Spellbooks.setReadyAt(player, slot, 0);
        Spellbooks.setMana(player, 200);
    }
    private static void begin(ServerPlayer player, Mode mode) {
        prepare(player);
        probe = new Probe(player, mode);
        down(player);
        probe.initial = ReweaveFields.view(player);
        check(probe.initial != null, "Paid native placement accepted");
        Vec3 center = probe.initial.center();
        player.level().setBlock(BlockPos.containing(center.add(1, -1, 1)), dev.wildercord.content.WildercordBlocks.RUNE_SEAL.defaultBlockState()
            .setValue(dev.wildercord.content.RuneSealBlock.ELEMENT, dev.wildercord.content.RuneSealBlock.Element.ARCANE), 2);
        probe.target = new Witness(player.level());
        probe.target.snapTo(center.x, center.y - .12, center.z + 1, 180, 0);
        probe.target.setNoGravity(true);
        player.level().addFreshEntity(probe.target);
        check(!Exposed.has(probe.target), "Fresh target starts without Harm mark");
    }
    private static void drive(Probe p) {
        long age = ReweaveFields.now(p.owner) - p.initial.created();
        if (!p.changed && age == (p.mode == Mode.WARNING ? 24 : p.mode == Mode.LATE ? 64 : 12)) {
            p.changed = true;
            switch (p.mode) {
                case DISC, RETIRE, ROUND_TRIP, REENTER -> {}
                case ECHO_EMPTY -> {
                    check(p.receipt != null && !p.receipt.alive(), "Actual paid Cast is closed between beats, independently of field expiry");
                    check(p.echo != null && !p.receipt.once("quirk_echo:" + p.echo.id()), "The real Harm ECHO hook already scheduled its delayed callback");
                    p.target.discard();
                    p.unresolved = new UnloadedStart(p.owner, BlockPos.containing(p.initial.center()));
                }
                case UNLOADED_START -> {
                    p.unresolved = new UnloadedStart(p.owner, BlockPos.containing(p.initial.center()));
                    var before = ReweaveFields.view(p.owner);
                    convert(p);
                    check(ReweaveFields.view(p.owner).equals(before), "Unknown ward refuses conversion without altering the paid field");
                    p.unresolved.assertNotLoaded();
                }
                case LIVE_DISC -> rankAndLoadout(p);
                case LIVE_LANE -> { rankAndLoadout(p); convert(p); }
                case BLOCKED -> {
                    var before = ReweaveFields.view(p.owner);
                    BlockPos wall = BlockPos.containing(p.initial.center().add(0, 0, 4));
                    for (int y = 150; y <= 153; y++) p.owner.level().setBlock(new BlockPos(wall.getX(), y, wall.getZ()), Blocks.STONE.defaultBlockState(), 2);
                    convert(p);
                    check(ReweaveFields.view(p.owner).equals(before), "Rejected blocked lane preserves exact original paid state");
                    for (int y = 150; y <= 153; y++) p.owner.level().setBlock(new BlockPos(wall.getX(), y, wall.getZ()), Blocks.AIR.defaultBlockState(), 2);
                }
                case LATE -> {
                    var before = ReweaveFields.view(p.owner); convert(p);
                    check(ReweaveFields.view(p.owner).equals(before), "Useless conversion preserves original footprint and remaining beat");
                }
                default -> convert(p);
            }
        }
        if (p.mode == Mode.UNLOADED_START && p.unresolved != null) {
            p.unresolved.assertNotLoaded();
            if (age == 35) {
                var after = ReweaveFields.view(p.owner);
                check(after != null && after.nextBeat() == 2 && p.hitTicks.equals(List.of(8L)),
                    "Unknown ward consumes the due beat without retiring, replaying or damaging");
                p.unresolved.close(); p.unresolved = null;
            }
        }
        if (p.mode == Mode.ECHO_EMPTY && p.unresolved != null) {
            p.unresolved.assertNotLoaded();
            if (age == 20) {
                check(p.target.isRemoved() && !p.receipt.alive() && p.hitTicks.equals(List.of(8L)),
                    "Actual delayed ECHO with an absent original victim cannot reopen impact or read unknown ward metadata");
                // Explicitly exercise the formerly unsafe empty-list Effects.apply boundary with the real paid receipt.
                Effects.apply(p.receipt, p.receipt.info.root().groups.getFirst().effects.getFirst(),
                    new Cast.Hit(List.of(), p.initial.center().add(0, 1, 1), Vec3.ZERO, p.initial.center(), null, null, false));
                p.unresolved.assertNotLoaded();
            }
            if (age == 22) {
                p.unresolved.close(); p.unresolved = null;
                Vec3 center = p.initial.center();
                p.target = new Witness(p.owner.level());
                p.target.snapTo(center.x, center.y - .12, center.z + 1, 180, 0);
                p.target.setNoGravity(true); p.owner.level().addFreshEntity(p.target);
                check(ReweaveFields.view(p.owner) != null, "Closing an echo window did not retire the remaining original field");
            }
        }
        if (age >= 81) p.done = true;
    }
    private static void rankAndLoadout(Probe p) {
        p.owner.setAttached(WildercordAttachments.RUNE_RANKS, Map.of(Runes.HARM.id(), 3));
        // Same-tier cord replacement and changed Heart are deliberate policy probes: the release keeps its captured power.
        Spellbooks.setCord(p.owner, new net.minecraft.world.item.ItemStack(WildercordItems.ECHO_CORD));
        p.owner.setAttached(WildercordAttachments.CIRCLES, 13);
        p.owner.setInvulnerableTime(0);
        p.owner.hurtServer(p.owner.level(), p.owner.level().damageSources().generic(), 1);
        check(ReweaveFields.view(p.owner).power() == p.initial.power(), "Ordinary damage/loadout/Heart changes do not resnapshot release power");
    }
    private static void convert(Probe p) {
        var before = ReweaveFields.view(p.owner);
        RelayCircleTest.aim(p.owner, p.initial.center().add(0, 1, 7));
        down(p.owner);
        var after = ReweaveFields.view(p.owner);
        check(after != null && after.payment() == before.payment() && after.identity() == before.identity(), "Conversion keeps exact Cast and paid ledger identities");
        check(after.center().equals(before.center()) && after.created() == before.created() && after.expires() == before.expires()
            && after.cost() == before.cost() && after.power() == before.power(), "Conversion preserves original center/time/price/Heart snapshot");
        if (p.mode != Mode.LATE && p.mode != Mode.BLOCKED && p.mode != Mode.UNLOADED_START) {
            check(after.direction() != null && after.convertedAt() == ReweaveFields.now(p.owner), "One fixed lane accepted from server aim");
            ReweaveFields.input(p.owner, RelayInputRules.DOWN, 0, nonce); // Duplicate and held edges do not convert again.
            check(ReweaveFields.view(p.owner).equals(after), "Duplicate cannot reset warning");
        }
    }
    private static void admissionChecks(ServerPlayer player) {
        prepare(player); probe = null;
        float mana = Spellbooks.mana(player);
        var chunk = player.level().getChunkSource().getChunkNow(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);
        check(chunk != null, "Constructor-cleanup control has a resident body chunk");
        var beforeReferences = UnloadedStart.references(chunk);
        AssertionError injected = new AssertionError("Injected post-reference constructor failure");
        try {
            new UnloadedStart(player, player.blockPosition(), injected);
            throw new AssertionError("The constructor-failure negative control did not fail");
        } catch (AssertionError failure) {
            if (failure != injected) throw failure;
        }
        check(chunk.getAllReferences().equals(beforeReferences), "A constructor assertion restores all original structure references without close()");
        try (var unresolved = new UnloadedStart(player, player.blockPosition())) {
            down(player);
            check(ReweaveFields.view(player) == null && Spellbooks.mana(player) == mana,
                "Loaded footprint with unresolved start refuses placement before real payment");
            unresolved.assertNotLoaded();
        }
        // Advancing only the admission fixture clock permits a genuinely fresh edge for the next refusal.
        ((ServerLevelData) player.level().getLevelData()).setGameTime(player.level().getGameTime() + 1);
        player.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:tide_scribe"));
        down(player); check(ReweaveFields.view(player) == null && Spellbooks.mana(player) == mana, "Forged learned rune/book cannot supply a completed study");
        player.setAttached(WildercordAttachments.GRIMOIRE, List.of(ReweaveRules.STUDY));
        // Fresh input must be on a new tick, so test entitlement directly here; native paid path rechecks the same predicate.
        check(!ReweaveFields.entitled(player), "Study marker cannot replace permanent Low Tide entitlement");
        player.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:tide_scribe", ReweaveRules.STUDY));
        player.setAttached(WildercordAttachments.CIRCLES, 11);
        check(!ReweaveFields.entitled(player), "Eleven active circles cannot use XII");
    }
    private static dev.wildercord.spell.RuneQuirks.Quirk configureEcho(dev.wildercord.config.WildercordConfig config, net.minecraft.server.MinecraftServer server) {
        for (int candidate = 0; candidate < 10000; candidate++) {
            String salt = "reweave-echo-regression-" + candidate;
            var found = dev.wildercord.spell.RuneQuirks.forge(server.overworld().getSeed(), salt, 8).stream()
                .filter(q -> q.rune().equals(Runes.HARM.id()) && q.kind() == dev.wildercord.spell.RuneQuirks.Kind.ECHO
                    && q.when() == dev.wildercord.spell.RuneQuirks.When.NIGHT).findFirst();
            if (found.isEmpty()) continue;
            CampConcordNative.config(CampConcordNative.copy(config, Map.of("resonances",
                CampConcordNative.copy(config.resonances(), Map.of("enabled", true, "quirks", 8, "rerollSalt", salt)))));
            WorldQuirks.forget();
            check(WorldResonances.quirks(server).contains(found.get()), "Native ledger actually installs the deterministic Harm ECHO fixture");
            return found.get();
        }
        throw new AssertionError("Could not find a bounded deterministic Harm ECHO fixture");
    }
    /** A real resident chunk references a distant absent start; no artificial ward result is substituted. */
    private static final class UnloadedStart implements AutoCloseable {
        private final net.minecraft.server.level.ServerLevel level;
        private final net.minecraft.world.level.chunk.LevelChunk chunk;
        private final Map<net.minecraft.world.level.levelgen.structure.Structure, it.unimi.dsi.fastutil.longs.LongSet> original;
        private static final int FAR = 10000;
        UnloadedStart(ServerPlayer player, BlockPos footprint) { this(player, footprint, null); }
        private UnloadedStart(ServerPlayer player, BlockPos footprint, AssertionError failAfterInsert) {
            level = player.level();
            chunk = level.getChunkSource().getChunkNow(footprint.getX() >> 4, footprint.getZ() >> 4);
            check(chunk != null, "Reweave regression uses an already resident FULL footprint chunk");
            original = references(chunk);
            assertNotLoaded();
            var structure = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                    net.minecraft.resources.Identifier.parse("wildercord:sword_tomb"))).value();
            try {
                chunk.addReferenceForStructure(structure, net.minecraft.world.level.ChunkPos.pack(FAR, FAR));
                if (failAfterInsert != null) throw failAfterInsert;
                check(!dev.wildercord.world.dungeons.DungeonWards.movementWard(level, footprint).known(),
                    "Unloaded referenced start produces explicit UNKNOWN for the loaded footprint");
            } catch (RuntimeException | Error failure) {
                // A failed resource constructor is never closed by try-with-resources.
                chunk.setAllReferences(original);
                throw failure;
            }
        }
        private static Map<net.minecraft.world.level.levelgen.structure.Structure, it.unimi.dsi.fastutil.longs.LongSet> references(net.minecraft.world.level.chunk.LevelChunk chunk) {
            Map<net.minecraft.world.level.levelgen.structure.Structure, it.unimi.dsi.fastutil.longs.LongSet> copy = new java.util.HashMap<>();
            chunk.getAllReferences().forEach((key, value) -> copy.put(key, new it.unimi.dsi.fastutil.longs.LongOpenHashSet(value)));
            return copy;
        }
        void assertNotLoaded() {
            check(level.getChunkSource().getChunk(FAR, FAR, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS, false) == null,
                "Reweave must not request even STRUCTURE_STARTS for an unloaded referenced chunk");
        }
        @Override public void close() { chunk.setAllReferences(original); }
    }
    private static void down(ServerPlayer p) {
        ReweaveFields.input(p, RelayInputRules.UP, 0, ++nonce);
        ReweaveFields.input(p, RelayInputRules.DOWN, 0, ++nonce);
    }
    private static void register() {
        if (registered) return;
        registered = true;
        ReweaveFields.initFeasibility();
        WildercordEvents.BEFORE_CAST.register((owner, slot, runes, cost) -> {
            if (probe != null && owner == probe.owner && ReweaveRules.valid(runes)) {
                probe.before = Spellbooks.mana(owner);
                ReweaveFields.input(owner, RelayInputRules.DOWN, slot, ++nonce);
                check(ReweaveFields.view(owner) == null, "ActionAdmission refuses payment-callback reentry");
            }
            return true;
        });
        WildercordEvents.AFTER_CAST.register((owner, slot, runes, cost) -> {
            if (probe != null && owner == probe.owner && ReweaveRules.valid(runes)) {
                probe.payments++; probe.spent = cost; probe.after = Spellbooks.mana(owner);
            }
        });
        WildercordEvents.SPELL_HIT.register((owner, targets, at, effects) -> {
            Probe p = probe;
            if (p == null || owner != p.owner || p.target == null || !targets.contains(p.target)) return;
            if (p.mode == Mode.ROUND_TRIP) {
                var level = p.owner.level();
                check(p.owner.teleportTo(level.getServer().getLevel(net.minecraft.world.level.Level.NETHER), .5, 150, .5, java.util.Set.of(), 0, 0, false), "Native owner departs its original world");
                check(p.owner.teleportTo(level, .5, 150, .5, java.util.Set.of(), 0, 55, false), "Native owner returns in same callback without a validity poll");
            } else if (p.mode == Mode.RETIRE) {
                ReweaveFields.cancel(p.owner);
                check(!Exposed.has(p.target), "Retiring inside target callback precedes all Harm mutations");
            } else if (p.mode == Mode.REENTER) {
                var before = ReweaveFields.view(p.owner);
                down(p.owner);
                check(ReweaveFields.view(p.owner).equals(before), "Impact callback cannot rewrite an admitted beat");
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Probe p = probe;
            if (p == null || p.done || p.owner.level().getServer() != server) return;
            try { drive(p); } catch (RuntimeException | AssertionError failure) { p.failure = failure; p.done = true; }
        });
    }
    private static final class Witness extends TrainingDummy {
        Witness(net.minecraft.server.level.ServerLevel level) { super(WildercordEntities.TRAINING_DUMMY, level); }
        @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
            boolean result = super.hurtServer(level, source, amount);
            Probe p = probe;
            if (p != null && this == p.target && source instanceof RelayDamageSource relay && result) {
                p.receipt = relay.cast();
                check(relay.cast().payment() == p.initial.payment(), "Native damage keeps original payment");
                p.hitTicks.add(ReweaveFields.now(p.owner) - p.initial.created());
                p.damage.add(lastDamage());
            }
            return result;
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
