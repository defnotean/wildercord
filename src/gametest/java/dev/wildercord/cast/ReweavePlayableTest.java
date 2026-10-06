package dev.wildercord.cast;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.content.ReweaveLessonChecks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.net.ReweaveInput;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Heart;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.MasterStudyRules;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.ReweaveRules;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Ordinary Grimoire study, Cord editor and rebound physical input over a real connection.
 * Only XII/Low Tide and arena/resources are declared fixtures; the study and first equipped row
 * are earned through their native UI. Later cases retain that same learned character.
 */
public final class ReweavePlayableTest implements FabricClientGameTest {
    private static final class Probe {
        ServerPlayer owner;
        Witness dummy;
        ReweaveFields.View paid;
        long field, rest;
        int payments, spent, expectedCost;
        Runnable beforeAdmission;
        float before, after;
        final List<Long> hitAges = new ArrayList<>();
    }
    private static Probe probe;
    private static boolean listening;
    // Adversarial packets run after physical-key checks, so this never invalidates later real keys.
    private static long packetNonce = 1_000_000;

    @Override public void runTest(ClientGameTestContext context) {
        ReweaveLessonChecks.run(context);
        listen();
        KeyMapping[] keys = new KeyMapping[3];
        InputConstants.Key[] original = new InputConstants.Key[3];
        try (var server = context.worldBuilder().createServer(properties())) {
            server.runOnServer(s -> dev.wildercord.Wildercord.LOGGER.info(
                "WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.cast.ReweavePlayableTest#input\",\"seed\":\"{}\"}", s.overworld().getSeed()));
            var connection = server.connect();
            try {
                connection.waitForChunksDownload(); connection.waitForClientboundPackets();
                server.runCommand("gamerule spawn_mobs false");
                server.runCommand("gamerule fall_damage false");
                server.runCommand("gamerule natural_health_regeneration false");
                server.runCommand("gamerule keep_inventory true");
                server.runOnServer(s -> {
                    probe = new Probe(); probe.owner = player(s);
                    RelayCircleTest.prepare(probe.owner, Runes.HARM);
                    // A pre-lesson save: the permanent victory exists, but no study or rune does.
                    probe.owner.setAttached(WildercordAttachments.CIRCLES, 12);
                    probe.owner.setAttached(WildercordAttachments.CONDENSED, 300000);
                    probe.owner.setAttached(WildercordAttachments.RUNEBOUND_SLAIN, 20);
                    probe.owner.setAttached(WildercordAttachments.GRIMOIRE, List.of("feat:tide_scribe"));
                    probe.owner.setAttached(WildercordAttachments.AFFINITY, Map.of());
                    probe.owner.setAttached(WildercordAttachments.RUNE_RANKS, Map.of());
                    probe.owner.setAttached(MasteryAttachments.MASTERY, MasteryBook.EMPTY);
                    Spellbooks.set(probe.owner, new Spellbook(List.of(Runes.HARM.id(), Runes.BOLT.id(), Runes.AMPLIFY.id()), List.of(), 0, true));
                    check(MasterStudies.hasReweaveLesson(probe.owner) && !MasterStudies.knowsReweave(probe.owner)
                        && !Spellbooks.knows(probe.owner, ReweaveRules.ID), "Old Low Tide has recoverable entitlement but no forged learned shape");
                });
                context.waitTicks(10);
                ReweaveLessonChecks.learnFromGrimoire(context);
                server.runOnServer(s -> check(MasterStudies.knowsReweave(probe.owner) && Spellbooks.knows(probe.owner, ReweaveRules.ID),
                    "This connected caster learned Reweave through the actual three pages"));
                equipInEditor(context);
                server.runOnServer(s -> {
                    check(Spellbooks.get(probe.owner).spells().getFirst().equals(ReweaveRules.IDS), "Real Codex clicks equip exactly Reweave then Harm");
                    check(SpellCaster.edit(probe.owner, 1, ReweaveRules.IDS) == null, "Second ordinary Echo row accepts the learned lesson");
                    probe.dummy = new Witness(probe.owner.level());
                    probe.dummy.snapTo(.5, 150, 6.5, 180, 0); probe.dummy.setNoGravity(true);
                    probe.owner.level().addFreshEntity(probe.dummy);
                    // Preserve the exact UI-equipped first row for its first real cast.
                    Spellbooks.setMana(probe.owner, 200); aimFloor(probe.owner);
                });
                context.runOnClient(mc -> {
                    keys[0] = WildercordKeys.castMapping();
                    for (int i = 1; i < keys.length; i++) {
                        String name = "key.wildercord.cast_" + i;
                        keys[i] = java.util.Arrays.stream(mc.options.keyMappings).filter(k -> k.getName().equals(name)).findFirst().orElseThrow();
                    }
                    int[] rebound = {InputConstants.KEY_F8, InputConstants.KEY_F9, InputConstants.KEY_F10};
                    for (int i = 0; i < keys.length; i++) { original[i] = KeyMappingHelper.getBoundKeyOf(keys[i]); keys[i].setKey(InputConstants.Type.KEYBOARD.getOrCreate(rebound[i])); }
                    KeyMapping.resetMapping();
                    check(WildercordKeys.castKey().getString().contains("F8"), "Help resolves the rebound Cast control");
                });
                context.waitTicks(4);

                context.getInput().holdKey(keys[0]); context.waitTicks(10);
                server.runOnServer(s -> {
                    rememberField();
                    check(state().phase() == ReweaveState.DISC && probe.payments == 1, "One real held Cast places one paid disc");
                    check(probe.spent == probe.expectedCost && close(probe.before - probe.after, probe.spent), "The compiled 32+8 price receives normal discounts exactly once");
                    check(SpellCompiler.compile(ReweaveRules.RUNES).cost() == 40, "Undiscounted Reweave and Harm cost 40");
                    check(probe.dummy.hitSequence() == 0 && !probe.owner.hasAttached(WildercordAttachments.CHARGE), "Held placement neither reaches the distant dummy nor enters charging");
                });
                context.takeScreenshot(TestScreenshotOptions.of("reweave_paid_disc_rebound_cast").disableCounterPrefix());
                context.getInput().releaseKey(keys[0]); context.waitTicks(2);
                server.runOnServer(s -> RelayCircleTest.aim(probe.owner, probe.dummy.getBoundingBox().getCenter()));
                context.waitTicks(3);
                context.getInput().holdKey(keys[0]); context.waitTicks(2);
                server.runOnServer(s -> {
                    check(state().phase() == ReweaveState.WARNING && state().warningUntil() - ReweaveFields.view(probe.owner).convertedAt() == 8,
                        "A released then freshly pressed Cast commits the eight-tick warning");
                    check(probe.dummy.hitSequence() == 0, "The warning has not hurt the dummy");
                    samePayment("Conversion");
                });
                context.takeScreenshot(TestScreenshotOptions.of("reweave_paid_lane_warning").disableCounterPrefix());
                server.waitFor(s -> probe.dummy.hitSequence() > 0, 45);
                context.getInput().releaseKey(keys[0]); context.waitTicks(2);
                server.runOnServer(s -> {
                    check(probe.dummy.lastDamage() > 0 && !probe.hitAges.isEmpty(), "A native Harm wound reaches the dummy beyond the original disc");
                    check(MasterStudies.practicedReweave(probe.owner), "A paid rewritten lane wound records the optional practice");
                    samePayment("Actual lane impact");
                });
                context.takeScreenshot(TestScreenshotOptions.of("reweave_paid_lane_dummy_hit").disableCounterPrefix());

                context.setScreen(CordScreen::new); context.waitTicks(2);
                server.runOnServer(s -> {
                    check(SpellCaster.edit(probe.owner, 0, List.of(Runes.BOLT.id(), Runes.HARM.id())) == null, "Normal editor accepts replacing the source row");
                    SpellCaster.select(probe.owner, 1);
                    Spellbooks.setCord(probe.owner, new ItemStack(WildercordItems.COPPER_CORD));
                    samePayment("Open Cord menu, edited row, selection and lower gear");
                    Spellbooks.setCord(probe.owner, new ItemStack(WildercordItems.ECHO_CORD));
                });
                context.waitTicks(3);
                server.runOnServer(s -> samePayment("Several ordinary menu ticks"));
                context.setScreen(() -> null); context.waitTicks(2);
                server.waitFor(s -> probe.owner.getAttached(ReweaveState.VIEW) == null, 85);
                server.runOnServer(s -> {
                    check(ReweaveFields.view(probe.owner) == null && ReweaveFields.rest(probe.owner) == probe.rest, "Original expiry retires the field without renewing shared rest");
                    check(probe.payments == 1 && probe.hitAges.stream().allMatch(age -> ReweaveRules.BEATS.stream().anyMatch(beat -> age == beat.longValue())), "All wounds retain the original absolute beat schedule and single payment");
                    check(Spellbooks.readyAt(probe.owner, 1) > probe.owner.level().getGameTime(), "Another Reweave row displays the still-running shared rest");
                });
                press(context, keys[2], 3);
                server.runOnServer(s -> check(probe.owner.getAttached(ReweaveState.VIEW) == null && probe.payments == 1, "A real direct-slot key cannot escape shared rest"));
                server.waitFor(s -> ReweaveFields.now(probe.owner) >= probe.rest, ReweaveRules.REST_TICKS);
                server.runOnServer(s -> {
                    SpellCaster.select(probe.owner, 0); aimFloor(probe.owner); Spellbooks.setMana(probe.owner, 200);
                });
                context.waitTicks(3);
                press(context, keys[2], 3);
                server.runOnServer(s -> {
                    rememberField();
                    check(state().slot() == 1 && Spellbooks.get(probe.owner).selected() == 0 && probe.payments == 2,
                        "Rebound Cast spell 2 places its own ordinary row while another row remains selected");
                });
                context.setScreen(CordScreen::new); context.waitTicks(2);
                context.getInput().holdKey(keys[2]); context.waitTicks(2);
                context.setScreen(() -> null); context.waitTicks(3);
                server.runOnServer(s -> { samePayment("Holding a direct key across menu return"); check(state().phase() == ReweaveState.DISC, "Menu return cannot invent a fresh hidden rewrite"); });
                context.getInput().releaseKey(keys[2]); context.waitTicks(2);
                server.runOnServer(s -> SpellCaster.select(probe.owner, 1)); context.waitTicks(2);
                var sneak = context.computeOnClient(mc -> mc.options.keyShift);
                context.getInput().holdKey(sneak); context.waitTicks(2);
                context.getInput().holdKey(keys[0]); context.waitTicks(2);
                server.runOnServer(s -> check(probe.owner.getAttached(ReweaveState.VIEW) == null && ReweaveFields.rest(probe.owner) == probe.rest
                    && probe.payments == 2, "Explicit sneak plus fresh Cast cancels without refund, another payment or rest reset"));
                context.getInput().releaseKey(keys[0]); context.getInput().releaseKey(sneak); context.waitTicks(2);
                press(context, keys[2], 2);
                server.runOnServer(s -> check(probe.owner.getAttached(ReweaveState.VIEW) == null && probe.payments == 2, "Cancellation cannot buy an immediate replacement from another row"));

                // A separate resource/rest fixture isolates physical key rearming from the real cooldown proof above.
                server.runOnServer(s -> readyFixture(probe.owner)); context.waitTicks(3);
                context.getInput().holdKey(keys[0]); context.waitTicks(3);
                server.runOnServer(s -> rememberField());
                context.setScreen(CordScreen::new); context.waitTicks(2);
                // Screen changes clear mapped keys. Keep the physical control held while the menu owns focus.
                context.getInput().holdKey(keys[0]); context.waitTicks(2);
                context.setScreen(() -> null); context.waitTicks(3);
                server.runOnServer(s -> {
                    samePayment("Rebound Cast held through opening and closing the menu");
                    check(state().phase() == ReweaveState.DISC && probe.payments == 3, "A still-held rebound Cast cannot produce a hidden press on menu return");
                });
                context.getInput().releaseKey(keys[0]); context.waitTicks(2);
                server.runOnServer(s -> RelayCircleTest.aim(probe.owner, probe.dummy.getBoundingBox().getCenter())); context.waitTicks(2);
                context.getInput().holdKey(keys[0]); context.waitTicks(2);
                server.runOnServer(s -> {
                    check(state().phase() == ReweaveState.WARNING && probe.payments == 3, "Actual release followed by a fresh rebound Cast rearms one legitimate rewrite");
                    samePayment("Fresh post-menu rewrite");
                });
                context.getInput().releaseKey(keys[0]); context.waitTicks(2);

                Consumer<Consumer<MinecraftServer>> onServer = action -> server.runOnServer(s -> action.accept(s));
                packetChecks(context, onServer);
                admissionChecks(context, onServer);
                for (String transition : List.of("spectator", "spectator round trip", "dimension", "respawn", "disconnect")) {
                    server.runOnServer(s -> readyFixture(probe.owner));
                    edge(context, 0, 0);
                    server.runOnServer(s -> rememberField());
                    ServerPlayer originalBody = server.computeOnServer(s -> probe.owner);
                    long paidRest = server.computeOnServer(s -> probe.rest);
                    if (transition.equals("disconnect")) {
                        connection.close(); context.waitFor(mc -> mc.level == null, 200); context.setScreen(TitleScreen::new);
                        connection = server.connect(); connection.waitForChunksDownload(); connection.waitForClientboundPackets();
                        server.runOnServer(s -> { probe.owner = player(s); check(probe.owner != originalBody, "Reconnect creates a new connected body"); });
                    } else {
                        server.runOnServer(s -> {
                            if (transition.equals("spectator")) probe.owner.setGameMode(GameType.SPECTATOR);
                            else if (transition.equals("spectator round trip")) {
                                check(probe.owner.setGameMode(GameType.SPECTATOR), "Real spectator entry succeeds");
                                check(probe.owner.setGameMode(GameType.SURVIVAL), "Real same-callback survival return succeeds without a field validity poll");
                            }
                            else if (transition.equals("dimension")) {
                                check(probe.owner.teleportTo(s.getLevel(Level.NETHER), .5, 150, .5, Set.of(), 0, 0, false), "Actual dimension departure succeeds");
                                check(probe.owner.teleportTo(s.overworld(), .5, 150, .5, Set.of(), 0, 55, false), "Actual same-tick dimension return succeeds");
                            } else {
                                probe.owner.kill(probe.owner.level());
                                probe.owner.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                                probe.owner = player(s); check(probe.owner != originalBody, "Death and respawn replace the caster body");
                            }
                        });
                        context.waitTicks(5);
                    }
                    server.runOnServer(s -> {
                        check(ReweaveFields.view(originalBody) == null && probe.owner.getAttached(ReweaveState.VIEW) == null,
                            "Released field retires across " + transition);
                        check(ReweaveFields.rest(probe.owner) == paidRest, "Paid shared rest survives " + transition);
                        check(MasterStudies.knowsReweave(probe.owner), "The actually learned lesson survives " + transition);
                    });
                }
            } finally {
                for (KeyMapping key : keys) if (key != null) context.getInput().releaseKey(key);
                context.getInput().releaseKey(options -> options.keyShift);
                context.runOnClient(mc -> { for (int i = 0; i < keys.length; i++) if (keys[i] != null && original[i] != null) keys[i].setKey(original[i]); KeyMapping.resetMapping(); });
                if (context.computeOnClient(mc -> mc.level != null)) connection.close();
            }
        } finally { probe = null; }
    }

    private static void packetChecks(ClientGameTestContext c, Consumer<Consumer<MinecraftServer>> onServer) {
        long retired = probe.field;
        onServer.accept(s -> readyFixture(probe.owner)); edge(c, 0, 0);
        onServer.accept(s -> rememberField());
        long accepted = packetNonce;
        send(c, RelayInputRules.DOWN, 0, accepted, probe.field);
        send(c, RelayInputRules.DOWN, 0, ++packetNonce, probe.field);
        onServer.accept(s -> { samePayment("Exact duplicate and held-repeat packets"); check(state().phase() == ReweaveState.DISC, "Held packets cannot rewrite"); });
        edge(c, 0, retired);
        send(c, RelayInputRules.CANCEL, 0, ++packetNonce, retired);
        onServer.accept(s -> { samePayment("Old field identity"); check(state().phase() == ReweaveState.DISC, "Old identity cannot rewrite or cancel a later paid field"); });
        send(c, RelayInputRules.DOWN, 0, accepted - 1, probe.field);
        onServer.accept(s -> check(state().phase() == ReweaveState.DISC, "Stale nonce cannot rewrite"));
        send(c, RelayInputRules.CANCEL, 0, ++packetNonce, probe.field);
        onServer.accept(s -> check(probe.owner.getAttached(ReweaveState.VIEW) == null && ReweaveFields.rest(probe.owner) == probe.rest, "Correct explicit cancellation preserves the original rest"));
    }

    private static void admissionChecks(ClientGameTestContext c, Consumer<Consumer<MinecraftServer>> onServer) {
        List<String> learned = new ArrayList<>(Heart.grimoire(probe.owner));
        for (String gate : List.of("active XI", "missing Low Tide", "missing study", "Copper Cord", "invalid row", "extra slot", "no mana", "free recast", "spectator callback")) {
            int payments = probe.payments;
            onServer.accept(s -> {
                readyFixture(probe.owner);
                probe.owner.setAttached(WildercordAttachments.GRIMOIRE, learned);
                switch (gate) {
                    case "active XI" -> probe.owner.setAttached(WildercordAttachments.CIRCLES, 11);
                    case "missing Low Tide" -> probe.owner.setAttached(WildercordAttachments.GRIMOIRE, learned.stream().filter(x -> !x.equals("feat:tide_scribe")).toList());
                    case "missing study" -> probe.owner.setAttached(WildercordAttachments.GRIMOIRE, learned.stream().filter(x -> !x.equals(MasterStudyRules.REWEAVE)).toList());
                    case "Copper Cord" -> Spellbooks.setCord(probe.owner, new ItemStack(WildercordItems.COPPER_CORD));
                    case "invalid row" -> check(SpellCaster.edit(probe.owner, 0, List.of(ReweaveRules.ID, Runes.HARM.id(), Runes.AMPLIFY.id())) != null, "Invalid editor draft is explicitly refused for casting");
                    case "extra slot" -> Spellbooks.set(probe.owner, Spellbooks.get(probe.owner).withSpell(4, ReweaveRules.IDS));
                    case "spectator callback" -> probe.beforeAdmission = () -> {
                        check(probe.owner.setGameMode(GameType.SPECTATOR), "Before-cast callback enters spectator");
                        check(probe.owner.setGameMode(GameType.SURVIVAL), "Before-cast callback returns in the same call");
                    };
                    case "no mana" -> Spellbooks.setMana(probe.owner, 0);
                    case "free recast" -> {
                        // Explicit bonus-token fixture, generated by the production surge path.
                        WildSurge.force(probe.owner, dev.wildercord.spell.WildMagic.Surge.FREE_RECAST);
                        WildSurge.roll(new Cast(probe.owner), List.of(Runes.BOLT, Runes.HARM), false, 1, ignored -> {});
                        check(WildSurge.freeRecast(probe.owner, probe.owner.level().getGameTime()), "A real unspent free-recast token exists before the refusal");
                    }
                }
            });
            edge(c, gate.equals("extra slot") ? 4 : 0, 0);
            onServer.accept(s -> {
                check(probe.owner.getAttached(ReweaveState.VIEW) == null && probe.payments == payments, "Ordinary packet refuses " + gate + " without paying or dispatching a partial spell");
                check(!probe.owner.hasAttached(WildercordAttachments.CHARGE), "Refusal does not strand charging");
                if (gate.equals("no mana")) check(Heart.active(probe.owner) == 12 && probe.owner.getHealth() == probe.owner.getMaxHealth(), "Insufficient mana cannot crack or overcast the Heart");
                if (gate.equals("free recast")) check(WildSurge.freeRecast(probe.owner, probe.owner.level().getGameTime()), "Reweave neither consumes a free recast nor uses it to copy the paid field");
            });
        }
        onServer.accept(s -> { readyFixture(probe.owner); probe.owner.setAttached(WildercordAttachments.GRIMOIRE, learned); });
        int payments = probe.payments;
        c.runOnClient(mc -> { ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)); ClientPlayNetworking.send(new WildercordNetworking.ChargeSpell(0, true)); });
        c.waitTicks(4);
        onServer.accept(s -> check(probe.owner.getAttached(ReweaveState.VIEW) == null && !probe.owner.hasAttached(WildercordAttachments.CHARGE)
            && probe.payments == payments, "Legacy ordinary cast and charge packets cannot bypass fresh Reweave edges"));
    }

    /** Independent control cases reset only resource/position/rest fixtures, retaining the UI-learned study. */
    private static void readyFixture(ServerPlayer p) {
        ReweaveFields.cancel(p); RelayCircles.cancel(p); WildSurge.forget(p.getUUID());
        p.setGameMode(GameType.SURVIVAL); p.setHealth(p.getMaxHealth());
        p.setAttached(WildercordAttachments.CIRCLES, 12); p.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
        Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
        Spellbooks.set(p, Spellbooks.get(p).withSelected(0).withSpell(0, ReweaveRules.IDS).withSpell(1, ReweaveRules.IDS));
        p.setAttached(ReweaveState.REST, 0L);
        for (int slot = 0; slot < dev.wildercord.gear.SpellSlots.ALL; slot++) Spellbooks.setReadyAt(p, slot, 0);
        Spellbooks.setMana(p, 200); aimFloor(p); p.setShiftKeyDown(false);
    }
    private static void aimFloor(ServerPlayer p) {
        p.teleportTo(p.level().getServer().overworld(), .5, 150, .5, Set.<Relative>of(), 0, 55, false);
        p.setDeltaMovement(Vec3.ZERO); p.setOnGround(true);
    }
    private static void rememberField() {
        check(state() != null && ReweaveFields.view(probe.owner) != null, "Ordinary connected input accepted a paid field");
        probe.paid = ReweaveFields.view(probe.owner); probe.field = state().fieldId(); probe.rest = ReweaveFields.rest(probe.owner);
        check(probe.rest == probe.paid.created() + 160 && probe.paid.expires() == probe.paid.created() + 80, "Placement fixes its original eight-second rest and four-second expiry");
    }
    private static void samePayment(String operation) {
        var current = ReweaveFields.view(probe.owner);
        check(current != null && state().fieldId() == probe.field && current.payment() == probe.paid.payment() && current.identity() == probe.paid.identity()
            && current.created() == probe.paid.created() && current.expires() == probe.paid.expires() && current.cost() == probe.paid.cost()
            && ReweaveFields.rest(probe.owner) == probe.rest, operation + " keeps the original paid Cast and its deadlines");
    }
    private static void equipInEditor(ClientGameTestContext c) {
        c.setScreen(CordScreen::new); c.waitTicks(3);
        click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).pagePoint(0)));
        click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).rowPoint(0)));
        for (String id : ReweaveRules.IDS) {
            c.runOnClient(mc -> ((CordScreen) mc.gui.screen()).searchFor(id.equals(ReweaveRules.ID) ? "Reweave" : "Harm")); c.waitTicks(2);
            click(c, c.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).codexPoint(id)));
        }
        c.runOnClient(mc -> check(((CordScreen) mc.gui.screen()).rowRunes(0).equals(ReweaveRules.IDS), "Visible editor row matches the learned exact pair"));
        c.takeScreenshot(TestScreenshotOptions.of("reweave_learned_equipped_row").disableCounterPrefix());
        c.setScreen(() -> null); c.waitTicks(3);
    }
    private static void click(ClientGameTestContext c, double[] point) {
        check(point != null, "Requested native editor control is visible");
        double scale = c.computeOnClient(mc -> mc.getWindow().getGuiScale());
        c.getInput().setCursorPos(point[0] * scale, point[1] * scale); c.waitTicks(1);
        c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); c.waitTicks(4);
    }
    private static void press(ClientGameTestContext c, KeyMapping key, int ticks) {
        c.getInput().holdKey(key); c.waitTicks(ticks); c.getInput().releaseKey(key); c.waitTicks(2);
    }
    private static void edge(ClientGameTestContext c, int slot, long field) {
        send(c, RelayInputRules.UP, slot, ++packetNonce, field);
        send(c, RelayInputRules.DOWN, slot, ++packetNonce, field);
    }
    private static void send(ClientGameTestContext c, int action, int slot, long nonce, long field) {
        c.runOnClient(mc -> ClientPlayNetworking.send(new ReweaveInput(action, slot, nonce, field))); c.waitTicks(2);
    }
    private static void listen() {
        if (listening) return; listening = true;
        WildercordEvents.BEFORE_CAST.register((p, slot, runes, cost) -> {
            if (probe != null && p == probe.owner && ReweaveRules.valid(runes)) { probe.before = Spellbooks.mana(p); probe.expectedCost = Heart.manaCost(p, SpellCompiler.compile(runes), Mastery.costFactor(p, runes));
                if (probe.beforeAdmission != null) { Runnable mutation = probe.beforeAdmission; probe.beforeAdmission = null; mutation.run(); }
            }
            return true;
        });
        WildercordEvents.AFTER_CAST.register((p, slot, runes, cost) -> {
            if (probe != null && p == probe.owner && ReweaveRules.valid(runes)) { probe.payments++; probe.spent = cost; probe.after = Spellbooks.mana(p); }
        });
    }
    private static final class Witness extends TrainingDummy {
        Witness(ServerLevel level) { super(WildercordEntities.TRAINING_DUMMY, level); }
        @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            boolean hurt = super.hurtServer(level, source, amount);
            if (hurt && lastDamage() > 0 && probe != null && this == probe.dummy && source instanceof RelayDamageSource receipt && probe.paid != null) {
                check(receipt.cast().payment() == probe.paid.payment() && receipt.cast().identity() == probe.paid.identity(), "Real damage source keeps the original paid ledger and Cast identity");
                probe.hitAges.add(ReweaveFields.now(probe.owner) - probe.paid.created());
            }
            return hurt;
        }
    }
    private static Properties properties() {
        Properties p = new Properties();
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) { p.setProperty("server-port", Integer.toString(socket.getLocalPort())); }
        catch (java.io.IOException exception) { throw new RuntimeException(exception); }
        p.setProperty("server-ip", "127.0.0.1"); p.setProperty("online-mode", "false"); p.setProperty("enforce-secure-profile", "false");
        p.setProperty("pause-when-empty-seconds", "-1"); p.setProperty("view-distance", "3"); p.setProperty("simulation-distance", "3");
        return p;
    }
    private static ServerPlayer player(MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
    private static ReweaveState state() { return probe.owner.getAttached(ReweaveState.VIEW); }
    private static boolean close(double a, double b) { return Math.abs(a - b) < .01; }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
