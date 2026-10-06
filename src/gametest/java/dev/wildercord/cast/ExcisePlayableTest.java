package dev.wildercord.cast;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.content.ExciseLessonChecks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.net.ExciseInput;
import dev.wildercord.player.Heart;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.ExciseRules;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Connected ordinary study/equip/rebound hold; real Witch native Zones are the counterproof. */
public final class ExcisePlayableTest implements FabricClientGameTest {
    private record Pulse(long tick, boolean excised, int remaining) {}
    private record Cut(long tick, long began, int pulses, int remaining) {}
    private static final class Probe {
        ServerPlayer player; Witch witch; TrainingDummy target;
        List<NativeZoneEmitters.Emitter> emitters = List.of();
        NativeZoneEmitters.Emitter selected;
        final Map<Long, List<Pulse>> pulses = new HashMap<>();
        Cut cut;
        int payments, cost; float before, after;
        long rest, poisonHits, recovery, cancelledAt;
        double masteryBefore;
        Runnable mutation;
    }
    private static Probe probe;
    private static boolean listening;
    private static long nonce = 100000;

    @Override public void runTest(ClientGameTestContext context) {
        ExciseLessonChecks.run(context);
        listen();
        KeyMapping[] key = new KeyMapping[2]; InputConstants.Key[] original = new InputConstants.Key[2];
        try (var server = context.worldBuilder().createServer(properties())) {
            server.runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.cast.ExcisePlayableTest\",\"seed\":\"{}\"}", s.overworld().getSeed()));
            var connection = server.connect();
            try {
                connection.waitForChunksDownload(); connection.waitForClientboundPackets();
                server.runCommand("gamerule spawn_mobs false"); server.runCommand("gamerule fall_damage false");
                server.runCommand("gamerule natural_health_regeneration false"); server.runCommand("gamerule keep_inventory true");
                server.runOnServer(s -> {
                    probe = new Probe(); probe.player = player(s);
                    RelayCircleTest.prepare(probe.player, Runes.HARM);
                    var p = probe.player;
                    p.setAttached(WildercordAttachments.CIRCLES, 16); p.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
                    p.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:root_guardian"));
                    p.setAttached(MasteryAttachments.MASTERY, MasteryBook.EMPTY);
                    p.setAttached(WildercordAttachments.AFFINITY, Map.of());
                    Spellbooks.set(p, new Spellbook(List.of(Runes.BEAM.id(), Runes.HARM.id(), Runes.AMPLIFY.id()), List.of(), 0, true));
                    check(!MasterStudies.knowsExcise(p) && MasterStudies.hasExciseLesson(p), "Old saved Heartwood grants retrieval, never study");
                    var prior=Spellbooks.get(p).learn(Runes.SELF.id()).learn(Runes.SWIFT.id()).learn(Runes.NIGHT_EYE.id())
                        .withPassive(0,List.of(Runes.SELF.id(),Runes.NIGHT_EYE.id()));Spellbooks.set(p,prior);
                    check(SpellCaster.editPassive(p,0,List.of(Runes.SELF.id(),Runes.SWIFT.id(),ExciseRules.ID))!=null && Spellbooks.get(p).equals(prior),
                        "Excise-bearing passive edit is refused as a whole before truncation or saved-book mutation");
                });
                context.waitTicks(6); ExciseLessonChecks.learnFromGrimoire(context); equip(context);
                server.runOnServer(s -> {
                    check(MasterStudies.knowsExcise(probe.player) && ExciseRules.IDS.equals(Spellbooks.get(probe.player).spells().getFirst()), "Three real pages and Codex clicks equip the ordinary pair");
                    newField(true);
                });
                context.runOnClient(mc -> {
                    key[0] = WildercordKeys.castMapping(); original[0] = KeyMappingHelper.getBoundKeyOf(key[0]);
                    key[0].setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F8));
                    key[1] = java.util.Arrays.stream(mc.options.keyMappings).filter(k -> k.getName().equals("key.wildercord.cast_1")).findFirst().orElseThrow();
                    original[1] = KeyMappingHelper.getBoundKeyOf(key[1]); key[1].setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F9)); KeyMapping.resetMapping();
                    check(WildercordKeys.castKey().getString().contains("F8"), "Lesson/HUD read the rebound Cast control");
                });
                context.waitTicks(4);
                server.runOnServer(s -> {
                    check(probe.emitters.size() == 3 && probe.emitters.stream().allMatch(e -> count(e) == 1), "Real Witch Split emits one first native pulse from each center");
                    check(probe.target.hasEffect(MobEffects.POISON), "The original native Witch Zone applied real venom before Excise");
                    probe.poisonHits = probe.target.hitSequence();
                    probe.target.setRemainingFireTicks(160);
                });
                context.getInput().holdKey(key[0]); context.waitTicks(4);
                server.runOnServer(s -> {
                    check(ExciseCasting.pending(probe.player) && probe.payments == 1, "One physical held edge owns one paid action");
                    var state = state(); probe.rest = ExciseCasting.rest(probe.player);
                    check(state.emitter() == probe.selected.id && state.until() - state.began() == 16, "The exact core and 16-tick deadline are locked");
                    check(probe.rest == state.began() + 240 && close(probe.before - probe.after, probe.cost), "One discounted payment owns the 240-tick shared rest");
                    check(SpellCompiler.compile(ExciseRules.RUNES).cost() == 36 && !probe.player.hasAttached(WildercordAttachments.CHARGE), "No overchannel or second casting route");
                });
                context.getInput().holdKey(key[1]); context.waitTicks(1); context.getInput().releaseKey(key[1]); context.waitTicks(1);
                server.runOnServer(s -> { check(ExciseCasting.pending(probe.player) && probe.payments == 1, "Refused quick-key release cannot cancel the original held Cast on the same row"); otherActionsUnspent(); });
                context.takeScreenshot(TestScreenshotOptions.of("excise_rebound_hold_native_witch").disableCounterPrefix());
                context.waitTicks(16);
                server.runOnServer(s -> {
                    check(probe.selected.excised && !probe.selected.allowsPulse() && probe.selected.cast.alive(), "Only the selected local emitter is cut; its shared Cast remains alive");
                    check(probe.cut != null && probe.cut.tick() - probe.cut.began() == 16,
                        "The actual native cut callback occurs at the paid sixteen-tick deadline");
                    check(probe.cut.pulses() >= 1 && probe.cut.pulses() < 6 && probe.cut.remaining() == 6 - probe.cut.pulses(),
                        "The cut preserves completed pulses and owns only the original remaining pulses");
                    check(MasterStudies.practicedExcise(probe.player), "Only the real completed cut records practice");
                    Mastery.flush(probe.player);
                    check(mastery() > probe.masteryBefore && mastery() - probe.masteryBefore <= dev.wildercord.spell.MasteryRules.MAX_PER_CAST,
                        "A real completed cut earns utility mastery within the existing per-cast cap");
                    check(MasteryAttachments.book(probe.player).entry(Mastery.keyOf(ExciseRules.RUNES)).orElseThrow().casts()==1,
                        "One completed cut counts exactly one useful cast");
                    check(probe.emitters.stream().filter(e -> e != probe.selected).allMatch(e -> !e.excised && e.allowsPulse()), "Split siblings keep their receipts");
                    check(probe.target.hasEffect(MobEffects.POISON) && probe.target.getRemainingFireTicks() > 0, "Existing poison and fire are not cleansed");
                    check(ExciseCasting.rest(probe.player) == probe.rest && probe.payments == 1, "Holding after completion does not pay or retarget again");
                });
                context.getInput().releaseKey(key[0]); context.waitTicks(104);
                server.runOnServer(s -> {
                    pulseReceipt("expired");
                    check(probe.emitters.stream().allMatch(e -> ExciseCasting.now(probe.player) >= e.expires),
                        "The post-cut observation covers every original emitter's expiry");
                    check(count(probe.selected) == probe.cut.pulses() && probe.selected.remaining == probe.cut.remaining()
                        && probe.pulses.get(probe.selected.id).stream().noneMatch(Pulse::excised),
                        "The selected emitter emits no pulse after the actual cut; its count and remaining work stay fixed");
                    check(probe.emitters.stream().filter(e -> e != probe.selected).allMatch(e -> count(e) == 6), "Both Split siblings finish all six original pulses");
                    check(probe.target.hitSequence() > probe.poisonHits, "Previously applied venom continues its own real damage after the cut");
                    check(probe.emitters.stream().noneMatch(e -> NativeZoneEmitters.snapshot().contains(e)), "Expiry cleans the bounded target registry");
                });

                // Ordinary focus/menu cancellation retains payment and recovery.
                server.runOnServer(s -> newField(false)); context.waitTicks(3);
                context.getInput().holdKey(key[0]); context.waitTicks(3);
                context.setScreen(CordScreen::new); context.waitTicks(3);
                server.runOnServer(s -> check(!ExciseCasting.pending(probe.player) && state().phase() == ExciseState.CANCELLED && ExciseCasting.committed(probe.player)
                    && !probe.selected.excised, "A native menu transition cancels without cutting or refunding"));
                server.runOnServer(s -> otherActionsUnspent());
                context.getInput().releaseKey(key[0]); context.setScreen(() -> null); context.waitTicks(15);
                server.runOnServer(s -> ordinaryActionsAfterRecovery());

                // Adversarial packets use fresh nonces only after ordinary physical input coverage.
                for (String reason : List.of("damage", "movement", "sight", "row", "cord", "eligibility", "spectator", "target death")) {
                    server.runOnServer(s -> { newField(false); directDown(); }); context.waitTicks(2);
                    server.runOnServer(s -> {
                        check(ExciseCasting.pending(probe.player), "Fixture accepted before " + reason);
                        int payments = probe.payments; float mana = Spellbooks.mana(probe.player); long rest = ExciseCasting.rest(probe.player);
                        switch (reason) {
                            case "damage" -> { Effects.readyToHurt(probe.player); probe.player.hurtServer(probe.player.level(), probe.player.damageSources().magic(), 1); }
                            case "movement" -> probe.player.teleportTo(probe.player.level(), 2.0, 150, .5, Set.<Relative>of(), 0, 0, false);
                            case "sight" -> { for (int y = 150; y <= 152; y++) probe.player.level().setBlock(new BlockPos(0, y, 3), Blocks.STONE.defaultBlockState(), 2); }
                            case "row" -> Spellbooks.set(probe.player, Spellbooks.get(probe.player).withSelected(1));
                            case "cord" -> Spellbooks.setCord(probe.player, new ItemStack(WildercordItems.ECHO_CORD));
                            case "eligibility" -> probe.player.setAttached(WildercordAttachments.CIRCLES, 15);
                            case "spectator" -> { probe.player.setGameMode(GameType.SPECTATOR); probe.player.setGameMode(GameType.SURVIVAL); }
                            case "target death" -> probe.witch.kill(probe.player.level());
                        }
                        check(probe.payments == payments && ExciseCasting.rest(probe.player) == rest && Spellbooks.mana(probe.player) <= mana, reason + " does not undo the paid ledger");
                    });
                    context.waitTicks(2);
                    server.runOnServer(s -> { check(!ExciseCasting.pending(probe.player) && !probe.selected.excised && ExciseCasting.committed(probe.player), reason + " cancels into paid recovery");
                        Mastery.flush(probe.player); check(mastery() == probe.masteryBefore, "Cancellation awards no utility mastery: " + reason); });
                    context.waitTicks(14);
                }
                server.runOnServer(s -> {
                    newField(false);
                    int payments = probe.payments;
                    Spellbooks.setMana(probe.player, 0); directDown();
                    check(!ExciseCasting.pending(probe.player) && probe.payments == payments && Heart.active(probe.player) == 16, "Unaffordable input cannot pay, crack or overcast");
                }); context.waitTicks(2);
                server.runOnServer(s -> {
                    newField(false); int payments = probe.payments;
                    Spellbooks.set(probe.player, Spellbooks.get(probe.player).withSpell(0, List.of(Runes.BEAM.id(), ExciseRules.ID, Runes.AMPLIFY.id())));
                    directDown(); check(probe.payments == payments && !ExciseCasting.pending(probe.player), "Unsupported grammar rejects before payment");
                }); context.waitTicks(2);
                server.runOnServer(s -> {
                    newField(false); int payments = probe.payments;
                    probe.mutation = () -> { ExciseCasting.input(probe.player, RelayInputRules.DOWN, 0, ++nonce); SpellCaster.cast(probe.player, 1); Charging.request(probe.player, 1, true); check(!dev.wildercord.aura.Unity.begin(probe.player), "Unity cannot reenter Excise admission"); };
                    directDown(); check(probe.payments == payments + 1, "Reentrant callbacks cannot acquire another paid action");
                    long used = nonce; ExciseCasting.input(probe.player, RelayInputRules.DOWN, 0, used); ExciseCasting.input(probe.player, RelayInputRules.DOWN, 0, ++nonce);
                    check(probe.payments == payments + 1, "Duplicate and held-repeat packets cannot pay again");
                    ExciseCasting.cancel(probe.player);
                }); context.waitTicks(14);
                server.runOnServer(s -> {
                    newField(false); int payments = probe.payments;
                    probe.mutation = () -> Spellbooks.setCord(probe.player, new ItemStack(WildercordItems.ECHO_CORD));
                    directDown(); check(probe.payments == payments && !ExciseCasting.pending(probe.player), "BEFORE_CAST Cord mutation refuses before payment");
                }); context.waitTicks(2);
                server.runOnServer(s -> {
                    newField(false); int payments = probe.payments;
                    try (var unresolved = new UnloadedStart(probe.player)) {
                        directDown(); check(!ExciseCasting.pending(probe.player) && probe.payments == payments, "Unknown resident ward references refuse before payment");
                        unresolved.assertNotLoaded();
                    }
                });
                context.waitTicks(2); server.runOnServer(s -> {
                    newField(false);var p=probe.player;
                    var entry=MasteryBook.Entry.fresh(Mastery.keyOf(ExciseRules.RUNES),1,p.level().getGameTime()).withXp(10000,0).withTrait(0,"second_wind",false);
                    p.setAttached(MasteryAttachments.MASTERY,new MasteryBook(List.of(entry)));p.setHealth(3);
                    float before=Spellbooks.mana(p);int cost=Heart.manaCost(p,SpellCompiler.compile(ExciseRules.RUNES),Mastery.costFactor(p,ExciseRules.RUNES));
                    directDown();check(ExciseCasting.pending(p) && Spellbooks.mana(p)==before-cost,"Learned Second Wind cannot refund this field-cut payment");
                    ExciseCasting.cancel(p);
                });context.waitTicks(14);
                // The last scheduled native pulse, rather than a cosmetic tail, ends targetability.
                context.waitTicks(2); server.runOnServer(s -> newField(false)); context.waitTicks(92);
                server.runOnServer(s -> { check(probe.selected.remaining == 1, "One real final native pulse remains"); directDown(); check(ExciseCasting.pending(probe.player), "Late commitment starts while its locked core still lives"); });
                context.waitTicks(12);
                server.runOnServer(s -> check(probe.selected.remaining == 0 && count(probe.selected) == 6 && !probe.selected.excised && !ExciseCasting.pending(probe.player), "A completed final pulse cannot be cut afterward or award a false success"));
                context.waitTicks(14);
                server.runOnServer(s -> {
                    newField(false); int before=NativeZoneEmitters.snapshot().size();
                    var helpful=SpellCompiler.compile(List.of(Runes.ZONE,Runes.HEAL));
                    CastEngine.cast(new Cast(probe.witch),helpful.root());
                    var linked=SpellCompiler.compile(List.of(Runes.ZONE,Runes.VENOM,Runes.ON_HIT,Runes.BOLT,Runes.HARM));
                    CastEngine.cast(new Cast(probe.witch),linked.root());
                    var harmful=SpellCompiler.compile(List.of(Runes.ZONE,Runes.VENOM));
                    CastEngine.cast(new Cast(probe.witch,1,Heart.Bonuses.NONE,true,()->true,new Cast.Info(harmful.root(),2,"")),harmful.root());
                    check(NativeZoneEmitters.snapshot().size()==before,"Helpful, passive and anchored native Zones never enter the Excise target registry");
                });
                // Actual replacement at the same clock preserves only the remaining paid deadline.
                for (int elapsed : List.of(0, 13, -1)) {
                    context.waitTicks(2);
                    server.runOnServer(s -> {
                        newField(false); directDown();
                        probe.cancelledAt = ExciseCasting.now(probe.player); probe.rest = ExciseCasting.rest(probe.player);
                        if (elapsed > 0) {
                            ExciseCasting.cancel(probe.player); probe.recovery = ExciseCasting.recoveryUntil(probe.player);
                            check(probe.recovery == probe.cancelledAt + 12, "Cancellation starts exactly twelve ticks of recovery");
                        } else {
                            probe.recovery = probe.cancelledAt + 12;
                            check(ExciseCasting.pending(probe.player), "Death scenario owns a real pending paid target before lifecycle cancellation");
                        }
                    });
                    if (elapsed > 0) context.waitTicks(elapsed);
                    server.runOnServer(s -> {
                        var old = probe.player; long clock = ExciseCasting.now(old);
                        if (elapsed <= 0) {
                            check(ExciseCasting.pending(old), "The body still owns its paid target immediately before lifecycle probe");
                            probe.cancelledAt = clock; probe.recovery = clock + 12;
                            if (elapsed == 0) ExciseCasting.cancel(old); // explicit cancellation and respawn share this clock
                        }
                        old.kill(old.level());
                        old.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                        var fresh = player(s); probe.player = fresh;
                        fresh.teleportTo(s.overworld(), .5, 150, .5, Set.<Relative>of(), 0, 0, false);
                        check(old != fresh && ExciseCasting.now(fresh) == clock, "Real respawn replaces the body at the same authoritative clock");
                        check(ExciseCasting.rest(fresh) == probe.rest && !ExciseCasting.pending(fresh) && !fresh.hasAttached(ExciseState.VIEW) && !probe.selected.excised,
                            "Respawn keeps rest while clearing every pending target and view");
                        check(ExciseCasting.recoveryUntil(fresh) == (elapsed <= 0 ? probe.recovery : 0), "Respawn copies remaining deadline without renewal, and expired recovery stays expired");
                    });
                    context.waitTicks(20);
                    server.runOnServer(s -> check(!ExciseCasting.committed(probe.player), "Copied recovery expires normally"));
                }
                // Freeze only this lifecycle clock probe, never a gameplay or visual-success proof.
                context.waitTicks(2);
                server.runOnServer(s -> {
                    newField(false); directDown(); ExciseCasting.cancel(probe.player);
                    probe.rest = ExciseCasting.rest(probe.player); probe.recovery = ExciseCasting.recoveryUntil(probe.player);
                    probe.cancelledAt = ExciseCasting.now(probe.player); s.tickRateManager().setFrozen(true);
                });
                try {
                    connection.close(); context.waitFor(mc -> mc.level == null, 200); context.setScreen(TitleScreen::new);
                    connection = server.connect(); connection.waitForChunksDownload(); connection.waitForClientboundPackets();
                    server.runOnServer(s -> {
                        var fresh = player(s);
                        check(fresh != probe.player && ExciseCasting.now(fresh) == probe.cancelledAt, "Actual reconnect replaces the connected body at the frozen lifecycle clock");
                        check(ExciseCasting.recoveryUntil(fresh) == probe.recovery && ExciseCasting.committed(fresh), "Reconnect retains only the same remaining recovery deadline");
                        check(!ExciseCasting.pending(fresh) && !fresh.hasAttached(ExciseState.VIEW) && ExciseCasting.rest(fresh) == probe.rest && MasterStudies.knowsExcise(fresh),
                            "Reconnect restores study/rest but no pending core, held input or old-body view");
                        check(!probe.selected.excised, "Reconnect never completes the cancelled cut"); probe.player = fresh;
                    });
                } finally { server.runOnServer(s -> s.tickRateManager().setFrozen(false)); }
                context.waitTicks(14);
                server.runOnServer(s -> check(!ExciseCasting.committed(probe.player) && ExciseCasting.recoveryUntil(probe.player) == 0, "Recovery expires against advancing server time"));
                connection.close(); context.waitFor(mc -> mc.level == null, 200); context.setScreen(TitleScreen::new);
                connection = server.connect(); connection.waitForChunksDownload(); connection.waitForClientboundPackets();
                server.runOnServer(s -> check(!ExciseCasting.committed(player(s)) && !player(s).hasAttached(ExciseState.VIEW) && ExciseCasting.rest(player(s)) == probe.rest,
                    "Reconnect after the deadline cannot renew recovery or restore an old target"));
                server.runOnServer(s -> probe.player = player(s)); context.waitTicks(2);
                server.runOnServer(s -> {
                    newField(false); directDown(); check(ExciseCasting.pending(probe.player), "Disconnect starts with an actual pending paid hold");
                    probe.cancelledAt = ExciseCasting.now(probe.player); probe.recovery = probe.cancelledAt + 12; probe.rest = ExciseCasting.rest(probe.player);
                    s.tickRateManager().setFrozen(true);
                });
                try {
                    connection.close(); context.waitFor(mc -> mc.level == null, 200); context.setScreen(TitleScreen::new);
                    connection = server.connect(); connection.waitForChunksDownload(); connection.waitForClientboundPackets();
                    server.runOnServer(s -> {
                        var fresh=player(s);
                        check(fresh != probe.player && ExciseCasting.now(fresh)==probe.cancelledAt && ExciseCasting.recoveryUntil(fresh)==probe.recovery,
                            "Disconnect itself cancels the paid hold before save and keeps its one recovery deadline");
                        check(!ExciseCasting.pending(fresh) && !fresh.hasAttached(ExciseState.VIEW) && !probe.selected.excised && ExciseCasting.rest(fresh)==probe.rest,
                            "Active disconnect cannot resurrect or complete the locked target");
                    });
                } finally { server.runOnServer(s -> s.tickRateManager().setFrozen(false)); }


            } finally {
                context.runOnClient(mc -> { for (int i=0;i<key.length;i++) if(key[i]!=null) key[i].setKey(original[i]); KeyMapping.resetMapping(); });
                if (context.computeOnClient(mc -> mc.level != null)) connection.close();
                probe = null;
            }
        }
    }
    private static void newField(boolean split) {
        var p = probe.player;
        probe.cut = null;
        if (probe.witch != null) probe.witch.discard(); if (probe.target != null) probe.target.discard();
        ExciseCasting.cancel(p); p.setGameMode(GameType.SURVIVAL); p.setHealth(p.getMaxHealth());
        for (int y = 150; y <= 152; y++) p.level().setBlock(new BlockPos(0,y,3), Blocks.AIR.defaultBlockState(), 2);
        p.teleportTo(p.level(), .5, 150, .5, Set.<Relative>of(), 0, 0, false); p.setDeltaMovement(Vec3.ZERO);
        p.setAttached(WildercordAttachments.CIRCLES, 16); p.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
        p.setAttached(ExciseState.REST, 0L); for (int slot = 0; slot < 4; slot++) Spellbooks.setReadyAt(p,slot,0);
        Spellbooks.set(p, Spellbooks.get(p).withSelected(0).withSpell(0,ExciseRules.IDS).withSpell(1,List.of(Runes.BEAM.id(),Runes.HARM.id())));
        Spellbooks.setMana(p,200); Mastery.flush(p); probe.masteryBefore = mastery();
        probe.target = new TrainingDummy(WildercordEntities.TRAINING_DUMMY,p.level()); probe.target.snapTo(.5,150,6.5,0,0); probe.target.setNoGravity(true); p.level().addFreshEntity(probe.target);
        probe.witch = EntityTypes.WITCH.create(p.level(), EntitySpawnReason.COMMAND); check(probe.witch != null,"Native Witch exists");
        probe.witch.setNoAi(true); probe.witch.setNoGravity(true); probe.witch.addTag("wildercord.rolled"); probe.witch.snapTo(8.5,150,6.5,90,0);
        probe.witch.setTarget(probe.target); p.level().addFreshEntity(probe.witch);
        Vec3 delta = probe.target.position().subtract(probe.witch.getEyePosition());
        probe.witch.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
        probe.witch.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));
        Runebound.cast(p.level(),probe.witch,split ? List.of(Runes.ZONE,Runes.SPLIT_MOD,Runes.VENOM) : List.of(Runes.ZONE,Runes.VENOM),1);
        probe.emitters = NativeZoneEmitters.snapshot().stream().filter(e -> e.cast.caster == probe.witch).toList();
        check(!probe.emitters.isEmpty(),"Native Zone branch registered actual centers"); probe.selected = probe.emitters.getFirst();
        RelayCircleTest.aim(p,probe.selected.core);
        check(NativeZoneEmitters.select(p) == probe.selected,"Server aim selects one exact hostile core");
    }
    private static void otherActionsUnspent() {
        var p=probe.player; float mana=Spellbooks.mana(p), aura=dev.wildercord.aura.Aura.aura(p); long rest=ExciseCasting.rest(p);
        ItemStack old=p.getMainHandItem();
        ItemStack scroll=new ItemStack(WildercordItems.SPELL_SCROLL);
        scroll.set(dev.wildercord.content.WildercordComponents.SCROLL,new dev.wildercord.content.ScrollSpell(List.of(Runes.SELF.id(),Runes.HEAL.id()),"fixture","fixture"));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,scroll);
        ((dev.wildercord.content.SpellScrollItem)WildercordItems.SPELL_SCROLL).use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        check(scroll.getCount()==1,"A committed cut refuses ordinary scroll consumption");
        ItemStack stored=new ItemStack(net.minecraft.world.item.Items.STICK);
        var imbued=new dev.wildercord.content.Imbued(List.of(Runes.SELF.id(),Runes.HEAL.id()),2,0xAACCAA,false,new java.util.UUID(0,0),0);
        stored.set(dev.wildercord.content.WildercordComponents.IMBUED,imbued);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stored);
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,p.level(),net.minecraft.world.InteractionHand.MAIN_HAND);
        check(stored.get(dev.wildercord.content.WildercordComponents.IMBUED).charges()==2,"A committed cut refuses ordinary Imbue before charge consumption");
        ItemStack paper=p.getInventory().getItem(20), ink=p.getInventory().getItem(21);
        p.getInventory().setItem(20,new ItemStack(net.minecraft.world.item.Items.PAPER,3)); p.getInventory().setItem(21,new ItemStack(net.minecraft.world.item.Items.INK_SAC,3));
        dev.wildercord.content.SpellScrollItem.inscribe(p,1);
        check(p.getInventory().getItem(20).getCount()==3 && p.getInventory().getItem(21).getCount()==3,"Otherwise funded inscription keeps its paper and ink");
        var oldAura=dev.wildercord.aura.Aura.data(p); var oldUnity=dev.wildercord.aura.Unity.state(p);
        p.setAttached(dev.wildercord.aura.AuraAttachments.AURA,new dev.wildercord.aura.AuraAttachments.Data("gale",dev.wildercord.aura.AuraRules.FORM,1800,110,0));
        p.setAttached(dev.wildercord.aura.Unity.STATE,dev.wildercord.aura.UnityRules.State.NONE);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        check(dev.wildercord.aura.Unity.refusal(p)==null,"Unity has real stage, weapon, mana, Aura and no independent refusal");
        check(!dev.wildercord.aura.Unity.begin(p) && dev.wildercord.aura.Aura.aura(p)==110,"A committed cut refuses otherwise eligible Unity activation");
        p.setAttached(dev.wildercord.aura.AuraAttachments.AURA,oldAura);p.setAttached(dev.wildercord.aura.Unity.STATE,oldUnity);
        check(Spellbooks.mana(p)==mana && dev.wildercord.aura.Aura.aura(p)==aura && ExciseCasting.rest(p)==rest,"Conflicting attempts preserve both balances and the exact paid rest");
        p.getInventory().setItem(20,paper);p.getInventory().setItem(21,ink);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,old);
    }
    private static void ordinaryActionsAfterRecovery() {
        var p=probe.player; check(!ExciseCasting.committed(p),"Paid recovery has actually ended");
        p.getInventory().setItem(20,new ItemStack(net.minecraft.world.item.Items.PAPER,3));p.getInventory().setItem(21,new ItemStack(net.minecraft.world.item.Items.INK_SAC,3));
        float mana=Spellbooks.mana(p);dev.wildercord.content.SpellScrollItem.inscribe(p,1);
        check(Spellbooks.mana(p)<mana && p.getInventory().getItem(20).getCount()==2 && p.getInventory().getItem(21).getCount()==2,"Same ordinary inscription succeeds once recovery ends");
        p.setAttached(dev.wildercord.aura.AuraAttachments.AURA,new dev.wildercord.aura.AuraAttachments.Data("gale",dev.wildercord.aura.AuraRules.FORM,1800,110,0));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        p.setAttached(dev.wildercord.aura.Unity.STATE,dev.wildercord.aura.UnityRules.State.NONE);
        check(dev.wildercord.aura.Unity.refusal(p)==null && dev.wildercord.aura.Unity.begin(p),"Same funded Unity fixture activates once recovery ends");
        dev.wildercord.aura.Unity.stop(p);p.setAttached(dev.wildercord.aura.Unity.STATE,dev.wildercord.aura.UnityRules.State.NONE);
    }
    private static double mastery() { return MasteryAttachments.book(probe.player).entry(Mastery.keyOf(ExciseRules.RUNES)).map(MasteryBook.Entry::xp).orElse(0.0); }
    private static void directDown() { ExciseCasting.input(probe.player,RelayInputRules.UP,0,++nonce); ExciseCasting.input(probe.player,RelayInputRules.DOWN,0,++nonce); }
    private static ExciseState state() { return probe.player.getAttached(ExciseState.VIEW); }
    private static int count(NativeZoneEmitters.Emitter emitter) { return probe.pulses.getOrDefault(emitter.id,List.of()).size(); }
    /** Called only by the test mod's read-only injection at NativeZoneEmitters.cut's successful return. */
    public static void observeNativeCut(ServerPlayer player, Object value) {
        if (probe == null || player != probe.player || value != probe.selected) return;
        var held = state();
        check(probe.cut == null && held != null && held.phase() == ExciseState.HOLDING && held.emitter() == probe.selected.id,
            "A native cut is observed once for the exact paid held emitter");
        probe.cut = new Cut(ExciseCasting.now(player), held.began(), count(probe.selected), probe.selected.remaining);
        pulseReceipt("cut");
    }
    private static void pulseReceipt(String phase) {
        var emitters = probe.emitters.stream().map(e -> Map.of(
            "id", e.id, "selected", e == probe.selected, "created", e.created, "expires", e.expires,
            "excised", e.excised, "remaining", e.remaining, "sharedCast", e.cast == probe.selected.cast,
            "count", count(e), "center", List.of(e.center.x, e.center.y, e.center.z),
            "pulses", probe.pulses.getOrDefault(e.id,List.of()))).toList();
        dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_EXCISE_PULSES {}", new com.google.gson.Gson().toJson(Map.of(
            "phase", phase, "tick", ExciseCasting.now(probe.player), "cut", probe.cut, "emitters", emitters)));
    }
    private static void listen() {
        if (listening) return; listening = true;
        WildercordEvents.BEFORE_CAST.register((p,slot,runes,cost) -> {
            if (probe != null && p == probe.player && ExciseRules.valid(runes)) { probe.before = Spellbooks.mana(p); if (probe.mutation != null) { var action = probe.mutation; probe.mutation = null; action.run(); } }
            return true;
        });
        WildercordEvents.AFTER_CAST.register((p,slot,runes,cost) -> { if (probe != null && p == probe.player && ExciseRules.valid(runes)) { probe.payments++; probe.cost = cost; probe.after = Spellbooks.mana(p); } });
        WildercordEvents.SPELL_HIT.register((caster,entities,point,runes) -> {
            if (probe == null || caster != probe.witch || !runes.contains(Runes.VENOM) || point == null) return;
            for (var emitter : probe.emitters) if (emitter.center.distanceToSqr(point) < .0001) {
                probe.pulses.computeIfAbsent(emitter.id, ignored -> new ArrayList<>())
                    .add(new Pulse(ExciseCasting.now(probe.player), emitter.excised, emitter.remaining));
                // This also rejects a forbidden same-clock pulse after cut; clock comparisons alone cannot.
                if (emitter == probe.selected && emitter.excised) {
                    pulseReceipt("post-cut-pulse");
                    check(false, "The selected native emitter produced SPELL_HIT after its cut");
                }
            }
        });
    }
    private static void equip(ClientGameTestContext c) {
        c.setScreen(CordScreen::new); c.waitTicks(3); click(c,c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).pagePoint(0)));
        click(c,c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).rowPoint(0)));
        for (String id : ExciseRules.IDS) {
            c.runOnClient(mc -> ((CordScreen)mc.gui.screen()).searchFor(id.equals(ExciseRules.ID) ? "Excise" : "Beam")); c.waitTicks(2);
            click(c,c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).codexPoint(id)));
        }
        c.takeScreenshot(TestScreenshotOptions.of("excise_learned_equipped_row").disableCounterPrefix()); c.setScreen(() -> null); c.waitTicks(3);
    }
    private static void click(ClientGameTestContext c,double[] point) {
        check(point != null,"Native control is visible"); double scale=c.computeOnClient(mc -> mc.getWindow().getGuiScale());
        c.getInput().setCursorPos(point[0]*scale,point[1]*scale); c.waitTicks(1); c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); c.waitTicks(4);
    }
    private static Properties properties() {
        Properties p=new Properties();
        try(ServerSocket socket=new ServerSocket(0,0,InetAddress.getLoopbackAddress())) { p.setProperty("server-port",Integer.toString(socket.getLocalPort())); }
        catch(java.io.IOException error) { throw new RuntimeException(error); }
        p.setProperty("server-ip","127.0.0.1"); p.setProperty("online-mode","false"); p.setProperty("enforce-secure-profile","false"); p.setProperty("pause-when-empty-seconds","-1"); p.setProperty("view-distance","3"); p.setProperty("simulation-distance","3"); return p;
    }
    private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
    private static boolean close(float a,float b) { return Math.abs(a-b)<.01; }
    private static void check(boolean value,String text) { if(!value)throw new AssertionError(text); }
    private static final class UnloadedStart implements AutoCloseable {
        private final net.minecraft.server.level.ServerLevel level;
        private final net.minecraft.world.level.chunk.LevelChunk chunk;
        private final Map<net.minecraft.world.level.levelgen.structure.Structure, it.unimi.dsi.fastutil.longs.LongSet> original;
        private static final int FAR = 10000;
        UnloadedStart(ServerPlayer player) { this(player, player.blockPosition(), null); }
        private UnloadedStart(ServerPlayer player, BlockPos footprint, AssertionError failAfterInsert) {
            level = player.level();
            chunk = level.getChunkSource().getChunkNow(footprint.getX() >> 4, footprint.getZ() >> 4);
            check(chunk != null, "Excise regression uses an already resident FULL footprint chunk");
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
                "Excise must not request even STRUCTURE_STARTS for an unloaded referenced chunk");
        }
        @Override public void close() { chunk.setAllReferences(original); }
    }
}
