package dev.wildercord.aura;

import com.mojang.authlib.GameProfile;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.CrimsonArts;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.client.MastersArtsClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Native server-observed full/full/full/low requests of the actual registered Final. Momentum is
 * explicit scenario setup, including a cold request and a peak that naturally ebbs during a real clash;
 * observations, charge intervals, payment, release, targets and wounds are real.
 * No direct performer, synthetic ledger stroke, cooldown reset or physical-lock clearing is used.
 * Original body/world/callback/party retirement is separately exercised by CrimsonMoonReleasedOwnerTest.
 */
public final class CrimsonMoonTimelineTest implements FabricClientGameTest {
    private static final String ID = CrimsonArts.CRIMSON_MOON;
    private static final Vec3 FEET = new Vec3(.5, 100, .5);
    private static final List<Integer> MARKS = List.of(SwordString.Token.marks(SwordString.Token.FULL),
        SwordString.Token.marks(SwordString.Token.FULL), SwordString.Token.marks(SwordString.Token.FULL),
        SwordString.Token.marks(SwordString.Token.LOW));
    private enum Case { COLD_REQUEST, CLASH_GATE_DECAY, EMPTY_GATE_CLOSED, CANCELLED, FIVE_BANDS, MOVING_WOUNDS, MOVED_FEET_STALE_STRUCK,
        VISIBLE_BEFORE_CAP, RELEASE_SNAPSHOT, LETHAL_DIRECT, LETHAL_WOUND, SHARED_MENDING, PVP_DAMAGE_AND_SINGLE_HIT_STANCE }
    private record Hit(LivingEntity target, long tick, float amount, float loss) {}
    private static final class Probe {
        final Case scenario;
        final ServerPlayer owner;
        final ServerLevel level;
        final List<LivingEntity> fixtures = new ArrayList<>();
        final List<LivingEntity> selected = new ArrayList<>(), excluded = new ArrayList<>();
        final List<Hit> hits = new ArrayList<>();
        Foe input;
        Momentum.State heldMomentum;
        long accepted = -1, released = -1, held = -1, cooldown, bucketAt, clientTurnReceived = -1;
        int spends, releases, processed;
        double cost, paid, bucket, drunk, releaseMomentum, peakStance, admittedCost, winningMomentum;
        float beforeHealth, releaseHealth;
        Throwable failure;
        boolean beforeObserved, recoveryObserved, ended, measuring;
        Probe(Case scenario, ServerPlayer owner) { this.scenario = scenario; this.owner = owner; level = owner.level(); }
    }
    private static final class Foe extends Husk {
        final Probe probe;
        Foe(Probe probe) { super(EntityTypes.HUSK, probe.level); this.probe = probe; }
        @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            float before = getHealth();
            boolean result = super.hurtServer(level, source, amount);
            record(probe, this, source, amount, Math.max(0, before - Math.max(0, getHealth())));
            return result;
        }
    }
    private static final class Guest extends FakePlayer {
        final Probe probe;
        Guest(Probe probe) { super(probe.level, new GameProfile(UUID.randomUUID(), "MoonCapGuest")); this.probe = probe; }
        @Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
        @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
            float before = getHealth();
            boolean result = super.hurtServer(level, source, amount);
            record(probe, this, source, amount, Math.max(0, before - Math.max(0, getHealth())));
            return result;
        }
    }
    private Probe current;

    @Override public void runTest(ClientGameTestContext context) {
        AuraApi.StringHook release = this::released;
        AuraApi.SpendHook spend = (owner, amount, reason, backlash) -> {
            Probe p = current;
            if (p != null && owner == p.owner && reason.equals("art:" + ID)) checked(p, () -> {
                check(!backlash, "The Final is fully funded"); p.spends++; p.paid += amount;
                if (p.scenario == Case.CLASH_GATE_DECAY) {
                    var art = AuraApi.artOf(owner, ID).orElseThrow();
                    p.accepted = p.level.getGameTime(); p.cost = SwordStrings.price(owner, art);
                    p.cooldown = p.accepted + SwordStrings.rest(owner, art);
                    check(p.accepted == p.held + ClashRules.serverLength() && !Clashes.holding(owner),
                        "The winning native clash begins its paid windup at its actual resolution tick");
                    check(!art.condition().met(owner) && Momentum.value(owner) < MomentumRules.PEAK,
                        "Natural ebb closes the Final gate before payment and before the clash winner's Momentum award");
                    check(p.cost > p.admittedCost, "Resolution uses the existing current-tier price after peak decays");
                    near(amount, p.cost, "The held Final pays exactly the existing resolution price once");
                    observePaidWindup(p);
                    Scheduler.later(1, () -> checked(p, () -> {
                        p.winningMomentum = Momentum.value(owner);
                        check(p.spends == 1 && MastersArts.committed(owner) && SwordStrings.readyAt(owner, ID) == p.cooldown,
                            "Clash resolution commits full individual rest and the ordinary physical timeline");
                        float aura = Aura.aura(owner);
                        SwordStrings.request(owner, new SwordStrings.Perform(ID, MARKS));
                        check(p.spends == 1 && Aura.aura(owner) == aura && SwordStrings.readyAt(owner, ID) == p.cooldown,
                            "The consumed held suffix cannot pay or schedule again after resolution");
                    }));
                }
            });
        };
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Probe p = current;
            if (p != null && p.level.getServer() == server && p.measuring) checked(p, () -> observeMending(p));
        });
        try (var world = context.worldBuilder().create()) {
            context.waitTicks(40);
            world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
            world.getServer().runCommand("difficulty normal");
            world.getServer().runCommand("fill -16 99 -16 32 99 32 minecraft:stone");
            world.getServer().runCommand("fill -16 100 -16 32 105 32 minecraft:air");
            world.getServer().runOnServer(server -> {
                var rules = server.overworld().getGameRules();
                rules.set(GameRules.NATURAL_HEALTH_REGENERATION, false, server);
                rules.set(GameRules.PVP, true, server);
                check(!rules.get(GameRules.NATURAL_HEALTH_REGENERATION), "Natural healing is disabled");
                AuraApi.onString(release); AuraApi.onSpend(spend);
            });
            try {
                for (Case scenario : Case.values()) {
                    waitForRealRest(context, world);
                    // Restore the actual client look between scenarios, including after the turn probe.
                    context.runOnClient(mc -> { mc.player.setYRot(0); mc.player.setYHeadRot(0); mc.player.setXRot(0); });
                    context.waitTicks(2);
                    Probe[] holder = new Probe[1];
                    world.getServer().runOnServer(server -> {
                        Probe p = new Probe(scenario, server.getPlayerList().getPlayers().getFirst());
                        near(Mth.wrapDegrees(p.owner.getYRot()), 0, "The server receives the actual client setup heading");
                        near(p.owner.getXRot(), 0, "The server receives the actual client setup pitch");
                        holder[0] = current = p; prepare(p); beginInput(p);
                    });
                    if (scenario == Case.MOVED_FEET_STALE_STRUCK) turnConnectedClient(context, world, holder[0]);
                    context.waitTicks(125 + (scenario == Case.CLASH_GATE_DECAY ? ClashRules.serverLength() : 0));
                    world.getServer().runOnServer(server -> {
                        Probe p = holder[0]; rethrow(p); verify(p); cleanup(p); current = null;
                    });
                    Wildercord.LOGGER.info("[CrimsonMoonTimeline] Native Final case passed: {}", scenario);
                }
            } finally {
                world.getServer().runOnServer(server -> {
                    if (current != null) cleanup(current);
                    current = null; AuraApi.stringHooks().remove(release); AuraApi.spendHooks().remove(spend);
                });
            }
        }
    }

    /** The connected client owns look direction; wait for its teleport before sending a real turn. */
    private static void turnConnectedClient(ClientGameTestContext context, TestSingleplayerContext world, Probe p) {
        context.waitFor(mc -> {
            var timeline = MastersArtsClient.timeline(mc.player);
            return timeline != null && timeline.move() == 19 && Math.abs(mc.player.getX() - 4.5) < .001
                && Math.abs(mc.player.getZ() - .5) < .001;
        }, 60);
        context.runOnClient(mc -> {
            var timeline = MastersArtsClient.timeline(mc.player);
            check(timeline != null && mc.level.getGameTime() < timeline.startTick() + 8,
                "The real client receives the moved origin with time left to turn during windup");
            mc.player.setYRot(180); mc.player.setYHeadRot(180); mc.player.setXRot(70);
        });
        boolean[] received = {false};
        for (int attempt = 0; attempt < 3 && !received[0]; attempt++) {
            context.waitTicks(1); // Vanilla sends the connected LocalPlayer's changed rotation.
            world.getServer().runOnServer(server -> {
                rethrow(p);
                if (Math.abs(Mth.wrapDegrees(p.owner.getYRot() - 180)) < .001 && Math.abs(p.owner.getXRot() - 70) < .001) {
                    p.clientTurnReceived = p.level.getGameTime(); received[0] = true;
                    check(p.clientTurnReceived < p.accepted + 10, "The real turn packet reaches the server before release");
                }
            });
        }
        check(received[0], "The server must observe the actual connected client's turn during paid windup");
    }

    private static void waitForRealRest(ClientGameTestContext context, TestSingleplayerContext world) {
        int[] remaining = {0};
        world.getServer().runOnServer(server -> {
            var owner = server.getPlayerList().getPlayers().getFirst();
            remaining[0] = (int) Math.max(0, SwordStrings.readyAt(owner, ID) - owner.level().getGameTime());
        });
        // Real game time expires each prior 600-tick individual rest; no attachment or clock mutation.
        context.waitTicks(Math.max(35, remaining[0] + 1));
    }
    private static void prepare(Probe p) {
        ServerPlayer owner = p.owner;
        check(!MastersArts.committed(owner) && SwordStrings.readyAt(owner, ID) <= p.level.getGameTime(), "Previous real recovery and rest expired");
        owner.setGameMode(GameType.SURVIVAL); owner.teleportTo(FEET.x, FEET.y, FEET.z);
        owner.setNoGravity(true); owner.setDeltaMovement(Vec3.ZERO); owner.setYRot(0); owner.setXRot(0);
        owner.setOnGround(true); owner.setShiftKeyDown(false); owner.setSprinting(false); owner.clearFire(); owner.removeAllEffects();
        owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); owner.setHealth(200);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND}) owner.setItemSlot(slot, ItemStack.EMPTY);
        owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        owner.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("crimson", AuraRules.SOVEREIGN, 4500, 160, 0));
        // Keep the cold request cold; the clash uses production grace/ebb without changing its meter after acceptance.
        if (p.scenario == Case.COLD_REQUEST || p.scenario == Case.CLASH_GATE_DECAY) {
            Momentum.reset(owner);
            if (p.scenario == Case.CLASH_GATE_DECAY) {
                Momentum.add(owner, 100, "moon_clash_fixture", MomentumRules.MAX);
                check(Momentum.peak(owner) && Momentum.state(owner).ebbPerTick() > 0, "The clash fixture starts at a real naturally ebbing peak");
            }
        } else owner.setAttached(Momentum.MOMENTUM, new Momentum.State(100, p.level.getGameTime() + 200, 0, 0, 0));
        SwordStrings.forget(owner.getUUID());
        var art = AuraApi.artOf(owner, ID).orElseThrow();
        check(art.stage() == AuraRules.SOVEREIGN && art.cost() == 40 && art.cooldownTicks() == 600,
            "The actual Crimson slot V retains Sovereign, 40 Aura and 600 ticks");
        float aura = Aura.aura(owner); long ready = SwordStrings.readyAt(owner, ID);
        SwordStrings.request(owner, new SwordStrings.Perform(ID, MARKS));
        check(!MastersArts.committed(owner) && Aura.aura(owner) == aura && SwordStrings.readyAt(owner, ID) == ready,
            "Unobserved client marks cannot admit or pay for the Final");
        near(ArtKit.mendRoom(owner), 10, "Previous mending naturally drained during actual rest");
        p.input = foe(p, .5, 2.2); p.input.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); p.input.setHealth(1000);
    }
    private static void beginInput(Probe p) {
        // Native attack charge gets fourteen real ticks before every full stroke. The final low is two ticks later.
        for (int i = 1; i <= 3; i++) Scheduler.later(i * 14, () -> checked(p, () -> attack(p, false)));
        Scheduler.later(44, () -> checked(p, () -> {
            attack(p, true);
            ServerPlayer owner = p.owner;
            var art = AuraApi.artOf(owner, ID).orElseThrow();
            check(SwordStrings.saw(owner, art), "The server observed FULL/FULL/FULL/LOW through real Attack and Punch handlers");
            move(p.input, .5, -3); p.excluded.add(p.input); // Preserve real stale provenance behind the accepted facing.
            owner.setYRot(0); owner.setXRot(0); owner.setShiftKeyDown(false);
            setupTargets(p);
            p.beforeHealth = owner.getHealth(); p.accepted = p.level.getGameTime();
            p.cost = SwordStrings.price(owner, art); p.cooldown = p.accepted + SwordStrings.rest(owner, art);
            if (p.scenario == Case.COLD_REQUEST) {
                refuseColdRequest(p, art); return;
            }
            if (p.scenario == Case.CLASH_GATE_DECAY) {
                holdInClash(p, art); return;
            }
            float aura = Aura.aura(owner);
            SwordStrings.request(owner, new SwordStrings.Perform(ID, MARKS));
            check(MastersArts.committed(owner) && p.spends == 1, "One checked Final request enters one paid windup");
            near(Aura.aura(owner), aura - p.cost, "Exactly the existing modified price is paid at acceptance");
            near(p.paid, p.cost, "The spend hook observes the same once-paid price");
            check(SwordStrings.readyAt(owner, ID) == p.cooldown && p.cooldown == p.accepted + 600, "Original rest is committed at acceptance");
            check(!SwordStrings.saw(owner, art), "The real observed suffix is consumed once");
            check(owner.getHealth() == p.beforeHealth && p.releases == 0, "No toll or completion at acceptance");
            SwordStrings.request(owner, new SwordStrings.Perform(ID, MARKS));
            check(p.spends == 1 && SwordStrings.readyAt(owner, ID) == p.cooldown, "A duplicate request cannot spend or schedule twice");
            afterAcceptance(p);
            observePaidWindup(p);
        }));
    }

    private static void refuseColdRequest(Probe p, AuraApi.StringArt art) {
        var owner = p.owner;
        float aura = Aura.aura(owner); p.cooldown = SwordStrings.readyAt(owner, ID);
        check(Momentum.value(owner) == 0 && !art.condition().met(owner)
            && SwordStrings.check(owner, art, MARKS).orElseThrow() == SwordStrings.Refusal.CONDITION,
            "An authentic observed Final string still refuses a cold ordinary request at the public condition gate");
        SwordStrings.request(owner, new SwordStrings.Perform(ID, MARKS));
        check(p.spends == 0 && !MastersArts.committed(owner) && !Clashes.holding(owner) && Aura.aura(owner) == aura
            && SwordStrings.readyAt(owner, ID) == p.cooldown && SwordStrings.saw(owner, art),
            "A cold request cannot reserve its observed suffix, pay, rest, clash or start a trusted continuation");
    }

    private static void holdInClash(Probe p, AuraApi.StringArt art) {
        var owner = p.owner;
        check(art.condition().met(owner) && SwordStrings.check(owner, art, MARKS).isEmpty(),
            "The observed Final legitimately meets every ordinary request check before its clash");
        var guest = new Guest(p); p.fixtures.add(guest); p.excluded.add(guest);
        guest.setGameMode(GameType.SURVIVAL); guest.setNoGravity(true); move(guest, .5, -4.5);
        p.level.addNewPlayer(guest);
        Crescents.launch(guest, owner.getEyePosition().add(0, 0, 2), new Vec3(0, 0, -1), 0x88CCFF,
            6, 0, .7, 12, 2, 6, false, entity -> false, (flight, target) -> 0);
        p.held = p.level.getGameTime(); p.admittedCost = p.cost; p.heldMomentum = Momentum.state(owner);
        float aura = Aura.aura(owner); long ready = SwordStrings.readyAt(owner, ID);
        SwordStrings.request(owner, new SwordStrings.Perform(ID, MARKS));
        check(Clashes.holding(owner) && !MastersArts.committed(owner) && p.spends == 0 && Aura.aura(owner) == aura
            && SwordStrings.readyAt(owner, ID) == ready && !SwordStrings.saw(owner, art),
            "The real oncoming crescent holds one accepted consumed Final, unpaid until resolution");
        SwordStrings.request(owner, new SwordStrings.Perform(ID, MARKS));
        check(p.spends == 0 && Aura.aura(owner) == aura && SwordStrings.readyAt(owner, ID) == ready,
            "A duplicate request cannot pay or replace the held Final");
        for (int beat = 0; beat < ClashRules.BEATS; beat++) Scheduler.later(ClashRules.beat(beat), () -> checked(p, () -> Clashes.pressFor(owner)));
        Scheduler.later(ClashRules.serverLength() - 1, () -> checked(p, () -> {
            check(Clashes.holding(owner) && p.spends == 0 && p.releases == 0 && !art.condition().met(owner),
                "The genuine Final gate naturally closes while its accepted clash entitlement remains held");
            check(Momentum.state(owner).equals(p.heldMomentum), "Only real elapsed ticks closed the held gate; its Momentum state was never rewritten");
            check(Clashes.score(owner) > Clashes.score(guest), "Actual timed native presses earn the winning clash score");
        }));
    }

    private static void observePaidWindup(Probe p) {
        Scheduler.later(9, () -> checked(p, () -> {
            p.beforeObserved = true;
            check(p.releases == 0 && p.hits.isEmpty() && p.owner.getHealth() == p.beforeHealth, "No toll, completion or direct hit before release");
            check(MastersArts.committed(p.owner), "Paid windup still owns physical recovery");
        }));
        Scheduler.later(11, () -> checked(p, () -> { p.recoveryObserved = MastersArts.committed(p.owner); }));
        Scheduler.later(31, () -> checked(p, () -> { p.ended = !MastersArts.committed(p.owner); }));
    }
    private static void attack(Probe p, boolean low) {
        var owner = p.owner;
        owner.setOnGround(true); owner.setShiftKeyDown(low); owner.setSprinting(false);
        check(low ? SwordString.Token.LOW.fits(SwordStrings.observedMarks(owner))
            : SwordString.Token.FULL.fits(SwordStrings.observedMarks(owner)), "The packet reads actual server low/charge state");
        move(p.input, .5, 2.2); Effects.readyToHurt(p.input);
        float before = p.input.getHealth();
        owner.connection.handleAttack(new ServerboundAttackPacket(p.input.getId()));
        check(p.input.getHealth() < before && owner.getLastHurtMob() == p.input, "The native attack reaches and hurts the exact observed body");
        owner.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
    }

    private static void setupTargets(Probe p) {
        switch (p.scenario) {
            case COLD_REQUEST, CLASH_GATE_DECAY, EMPTY_GATE_CLOSED, CANCELLED -> { }
            case FIVE_BANDS -> { for (double distance : new double[] {.7, 1.8, 3, 4.2, 5.4}) p.selected.add(foe(p, .5, .5 + distance)); }
            case MOVING_WOUNDS -> { p.selected.add(foe(p, -.5, 3.5)); p.selected.add(foe(p, 1.5, 3.5)); }
            case MOVED_FEET_STALE_STRUCK -> { p.selected.add(foe(p, 4.5, 5.6)); p.excluded.add(foe(p, .5, 6.3)); }
            case VISIBLE_BEFORE_CAP -> {
                wall(p, -3, -1, 1, true);
                for (int i = 0; i < 10; i++) p.excluded.add(foe(p, -2, 2 + i * .05));
                for (int i = 0; i < 11; i++) {
                    var target = foe(p, 2.5, 3.5 + i * .15);
                    (i < 10 ? p.selected : p.excluded).add(target);
                }
            }
            case PVP_DAMAGE_AND_SINGLE_HIT_STANCE -> {
                Guest guest = new Guest(p); p.fixtures.add(guest);
                guest.setGameMode(GameType.SURVIVAL); guest.setNoGravity(true); move(guest, .5, 5.6);
                guest.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); guest.setHealth(200);
                p.level.addNewPlayer(guest); p.selected.add(guest);
                check(ArtKit.harmable(p.owner, guest), "The real player body is currently admitted by PvP rules");
            }
            default -> p.selected.add(foe(p, .5, 5.6));
        }
        if (p.scenario == Case.LETHAL_DIRECT) p.selected.getFirst().setHealth(1);
    }
    private static void afterAcceptance(Probe p) {
        if (p.scenario == Case.CANCELLED) Scheduler.later(3, () -> checked(p, () -> {
            check(MastersArts.cancel(p.owner), "The actual pending windup is cancelled");
            check(MastersArts.committed(p.owner), "Cancellation retains already-paid physical recovery");
        }));
        if (p.scenario == Case.EMPTY_GATE_CLOSED) {
            p.owner.setAttached(Momentum.MOMENTUM, Momentum.State.NONE);
            check(!Momentum.FINAL_GATE.met(p.owner), "The formerly met Final condition now actually closes after payment");
        }
        if (p.scenario == Case.MOVED_FEET_STALE_STRUCK) Scheduler.later(3, () -> checked(p, () -> {
            p.owner.teleportTo(4.5, 100, .5);
        }));
        if (p.scenario == Case.SHARED_MENDING) {
            p.owner.setHealth(p.beforeHealth - 10);
            near(ArtKit.mend(p.owner, p.owner, 10), 10, "Another real art-mending call fills the shared bucket");
            p.bucket = 10; p.bucketAt = p.level.getGameTime();
            near(p.owner.getHealth(), p.beforeHealth, "The explicit shared-mending fixture returns to its pre-toll health");
        }
    }

    private void released(ServerPlayer owner, AuraApi.StringArt art, AuraApi.StringContext receipt) {
        Probe p = current;
        if (p == null || owner != p.owner || !ID.equals(art.id())) return;
        checked(p, () -> {
            p.releases++; p.released = p.level.getGameTime(); p.releaseHealth = owner.getHealth(); p.releaseMomentum = Momentum.value(owner);
            check(p.released == p.accepted + 10 && p.releases == 1, "The registered performer completes once at exactly +10");
            check(receipt.at() == p.accepted && receipt.struck() == null, "ACTIVE_CONE discards the actual stale struck victim");
            check(receipt.marks().size() == 4, "The native Final preserves all four observed strokes");
            for (int i = 0; i < 3; i++) check(SwordString.Token.FULL.fits(receipt.marks().get(i)), "Each of the first three accepted attacks was full");
            check(SwordString.Token.LOW.fits(receipt.marks().getLast()), "The accepted fourth stroke was low");
            check(Effects.applying() == owner && Effects.applyingCast() == null, "Actual release preserves owner and cast provenance");
            near(p.releaseHealth, p.beforeHealth - ArtRules.moonToll(p.beforeHealth, owner.getMaxHealth()), "Health toll occurs at release, including an empty cone");
            check(p.hits.isEmpty(), "The release frame is separate from all distance-delayed direct damage");
            if (p.scenario == Case.MOVED_FEET_STALE_STRUCK) {
                near(owner.getX(), 4.5, "Release uses current feet after accepted movement");
                near(owner.getZ(), .5, "Movement leaves the accepted horizontal origin plane");
                check(p.clientTurnReceived >= p.accepted && p.clientTurnReceived < p.released, "The connected client turn was observed before release");
                near(Mth.wrapDegrees(owner.getYRot() - 180), 0, "The owner keeps the actual received opposite heading at release");
                near(owner.getXRot(), 70, "The actual received client pitch remains in effect at release");
            }
            if (p.scenario == Case.EMPTY_GATE_CLOSED) near(p.releaseMomentum, 0, "Closed Final condition is not rechecked after payment");
            else if (p.scenario == Case.CLASH_GATE_DECAY) near(p.releaseMomentum, p.winningMomentum - MomentumRules.FINAL_SPEND,
                "The clash winner spends Final Momentum only once on its actual release");
            else near(p.releaseMomentum, 100 - MomentumRules.FINAL_SPEND, "Final completion consumes momentum once at release");
            if (p.bucketAt == 0) p.bucketAt = p.released;
            p.measuring = true;
            if (p.scenario == Case.RELEASE_SNAPSHOT) {
                move(p.selected.getFirst(), 20.5, 20.5); wall(p, -2, 2, 2, true);
                p.excluded.add(foe(p, .5, 1.2));
            }
            if (p.scenario == Case.MOVING_WOUNDS) for (int i = 1; i <= 6; i++) {
                int beat = i;
                Scheduler.later(3 + i * 10 - 1, () -> checked(p, () -> move(p.selected.get(1), 1.5 + beat * .35, 3.5)));
            }
            if (p.scenario == Case.LETHAL_WOUND) {
                // A full body admits no opening drink, preserving the shared cap for the later lethal wound.
                owner.setHealth(owner.getMaxHealth());
                Scheduler.later(6, () -> checked(p, () -> {
                // Isolate a genuinely lethal wound after the opening hit, with the same live target and wound.
                p.selected.getFirst().setHealth(.2F);
                owner.setHealth(owner.getMaxHealth() - 20);
                }));
            }
        });
    }

    private static void record(Probe p, LivingEntity target, DamageSource source, float amount, float loss) {
        if (!source.is(Aura.DAMAGE) || source.getEntity() != p.owner) return;
        checked(p, () -> {
            check(p.released >= 0 && Effects.applying() == p.owner && Effects.applyingCast() == null,
                "Every native direct/wound damage callback retains the original art actor and no borrowed spell");
            p.hits.add(new Hit(target, p.level.getGameTime(), amount, loss));
        });
    }
    private static void observeMending(Probe p) {
        for (; p.processed < p.hits.size(); p.processed++) {
            Hit hit = p.hits.get(p.processed);
            p.bucket = Math.max(0, p.bucket - (hit.tick - p.bucketAt) * .05); p.bucketAt = hit.tick;
            boolean openingAtFullHealth = p.scenario == Case.LETHAL_WOUND && p.processed == 0;
            double admitted = openingAtFullHealth ? 0 : Math.max(0, Math.min(hit.loss * .5, Math.min(10 - p.drunk, 10 - p.bucket)));
            p.bucket += admitted; p.drunk += admitted;
        }
        double room = 10 - Math.max(0, p.bucket - (p.level.getGameTime() - p.bucketAt) * .05);
        near(ArtKit.mendRoom(p.owner), room, "Each completed native tick uses 50% actual loss, the shared Moon cap and real mending bucket");
        if (p.scenario == Case.PVP_DAMAGE_AND_SINGLE_HIT_STANCE) {
            var state = Stance.state(p.selected.getFirst());
            if (state != null) p.peakStance = Math.max(p.peakStance, state.wornAt(p.level.getGameTime()));
            // This fixture spends the damage budget in its first hit, so it proves only single-hit stance wear.
            // A cumulative fifteen-point stance-budget native witness remains separate and open.
            check(p.peakStance <= StanceRules.PLAYER_POOL * StanceRules.PVP_BLOW_CAP + .001, "The observed single hit respects the player stance limit");
        }
    }
    private static void verify(Probe p) {
        rethrow(p);
        if (p.scenario == Case.COLD_REQUEST) {
            check(p.spends == 0 && p.releases == 0 && p.hits.isEmpty() && !MastersArts.committed(p.owner)
                && SwordStrings.readyAt(p.owner, ID) == p.cooldown && p.owner.getHealth() == p.beforeHealth,
                "The cold public request remains wholly refused after all possible release frames");
            return;
        }
        check(p.accepted >= 0 && p.beforeObserved && p.recoveryObserved && p.ended, "Windup, release/recovery and actual motion expiry were observed");
        check(p.spends == 1 && SwordStrings.readyAt(p.owner, ID) == p.cooldown, "All late work leaves original payment/rest untouched");
        if (p.scenario == Case.CANCELLED) {
            check(p.releases == 0 && p.hits.isEmpty() && p.owner.getHealth() == p.beforeHealth, "Pre-release cancellation has no toll, completion or Moon work");
            near(Momentum.value(p.owner), 100, "Cancellation does not consume Final momentum"); return;
        }
        check(p.releases == 1, "Every actual release completes once");
        for (LivingEntity excluded : p.excluded) check(hits(p, excluded).isEmpty(), "Cone/cover/cap excludes stale, covered, surplus and late entrants");
        for (int i = 0; i < p.selected.size(); i++) {
            LivingEntity target = p.selected.get(i); List<Hit> hits = hits(p, target);
            int delay = switch (p.scenario) {
                case FIVE_BANDS -> i + 1; case MOVING_WOUNDS -> 3;
                case VISIBLE_BEFORE_CAP -> 1 + Math.min(4, (int) (Math.hypot(2, 3 + i * .15) / 1.2));
                default -> 5;
            };
            int count = p.scenario == Case.LETHAL_DIRECT || p.scenario == Case.PVP_DAMAGE_AND_SINGLE_HIT_STANCE ? 1 : p.scenario == Case.LETHAL_WOUND ? 2 : 7;
            check(hits.size() == count, "Expected native direct/wound count for " + p.scenario + ": got " + hits.size() + ", expected " + count);
            for (int beat = 0; beat < hits.size(); beat++) {
                Hit hit = hits.get(beat);
                check(hit.tick == p.released + delay + beat * 10L && hit.amount > 0 && hit.loss > 0,
                    "Native direct distance band and six ten-tick wound beats retain exact release-relative timing");
            }
        }
        if (p.scenario == Case.FIVE_BANDS || p.scenario == Case.VISIBLE_BEFORE_CAP) near(p.drunk, 10, "All direct hits and wounds share one ten-health drink cap");
        if (p.scenario == Case.LETHAL_DIRECT) { near(p.drunk, .5, "A lawful lethal direct hit drinks half its actual one-health loss"); check(!p.selected.getFirst().isAlive(), "The direct hit was lethal"); }
        if (p.scenario == Case.LETHAL_WOUND) {
            var hits = hits(p, p.selected.getFirst()); near(hits.getLast().loss, .2, "The lethal wound records only remaining actual health");
            check(!p.selected.getFirst().isAlive(), "The wound was lethal");
            near(p.drunk, .1, "The lawful lethal wound drinks half its actual remaining health after the opening drink admitted zero");
        }
        if (p.scenario == Case.MOVING_WOUNDS) {
            var still = hits(p, p.selected.get(0)); var moving = hits(p, p.selected.get(1));
            for (int i = 1; i <= 6; i++) near(moving.get(i).amount / still.get(i).amount, 1.5, "Every moving wound preserves the original 1.5 multiplier");
        }
        if (p.scenario == Case.SHARED_MENDING) check(p.drunk > 0 && p.drunk < 4, "Previously filled common bucket limits Moon over its six wound beats");
        if (p.scenario == Case.PVP_DAMAGE_AND_SINGLE_HIT_STANCE) {
            near(p.hits.stream().mapToDouble(Hit::amount).sum(), ArtRules.PVP_ART_CAP, "The direct hit and all wounds share one exact PvP damage cap");
            check(p.peakStance > 0, "The lawful player hit actually wears stance");
        }
        check(Effects.applying() == null && Effects.applyingCast() == null, "Native callbacks restore outer source scope");
    }
    private static List<Hit> hits(Probe p, LivingEntity target) { return p.hits.stream().filter(h -> h.target == target).toList(); }
    private static Foe foe(Probe p, double x, double z) {
        Foe target = new Foe(p); p.fixtures.add(target); target.addTag("wildercord.rolled");
        target.setNoAi(true); target.setNoGravity(true); target.noPhysics = true;
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        target.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);
        move(target, x, z); p.level.addFreshEntity(target); return target;
    }
    private static void move(LivingEntity target, double x, double z) { target.snapTo(x, 100, z, 180, 0); target.setDeltaMovement(Vec3.ZERO); }
    private static void wall(Probe p, int fromX, int toX, int z, boolean solid) {
        for (int x = fromX; x <= toX; x++) for (int y = 100; y <= 103; y++)
            p.level.setBlockAndUpdate(new BlockPos(x, y, z), (solid ? Blocks.STONE : Blocks.AIR).defaultBlockState());
    }
    private static void cleanup(Probe p) {
        p.measuring = false; p.fixtures.forEach(Entity::discard);
        wall(p, -3, -1, 1, false); wall(p, -2, 2, 2, false);
    }
    private static void checked(Probe p, Runnable action) { if (p.failure == null) try { action.run(); } catch (Throwable failure) { p.failure = failure; } }
    private static void rethrow(Probe p) { if (p.failure != null) throw new AssertionError("Moon native scenario " + p.scenario, p.failure); }
    private static void near(double actual, double expected, String message) { check(Double.isFinite(actual) && Math.abs(actual - expected) <= .003, message + " (" + actual + " != " + expected + ")"); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
