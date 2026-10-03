package dev.wildercord.aura;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.api.AuraApi;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.AuraSocialClient;
import dev.wildercord.duel.Duels;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Real client/server packets and controls with a fake second swordsman; screenshots social_*. */
public final class AuraSocialTest implements FabricClientGameTest {
    private Rival rival;
    private Crescents.Flight mine, theirs;
    private static final class Rival extends FakePlayer {
        Rival(ServerLevel level) { super(level, new GameProfile(UUID.nameUUIDFromBytes("aura-social-rival".getBytes(StandardCharsets.UTF_8)), "Rowan")); }
        @Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
    }
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> action) {
        return world.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
    }
    private static void teach(ServerPlayer p, int stage) {
        p.setGameMode(GameType.SURVIVAL); p.setHealth(p.getMaxHealth()); p.removeAllEffects(); p.damageCooldownTime = 0;
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", stage, 0, AuraRules.capacity(stage), 0));
        p.removeAttached(AuraAttachments.STATE); Duels.rested(p); Momentum.reset(p); Ways.set(p, WayRules.BLADE);
    }
    private static void shot(ClientGameTestContext c, String name) {
        c.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); });
        c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
    }
    private static void click(ClientGameTestContext c, double[] point) {
        check(point != null, "lineage control must be visible");
        double scale = c.computeOnClient(mc -> mc.getWindow().getGuiScale());
        c.getInput().setCursorPos(point[0] * scale, point[1] * scale);
        c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); c.waitTicks(2);
    }
    @Override public void runTest(ClientGameTestContext c) {
        c.runOnClient(mc -> { mc.getWindow().setWindowed(1600, 900); mc.options.guiScale().set(2); mc.resizeGui(); });
        try (TestSingleplayerContext world = c.worldBuilder().create()) {
            c.waitTicks(40);
            world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
            world.getServer().runCommand("gamerule minecraft:natural_regeneration false");
            world.getServer().runCommand("fill -16 189 -16 16 189 20 minecraft:stone");
            world.getServer().runCommand("fill -16 190 -16 16 199 20 minecraft:air");
            on(world, p -> {
                teach(p, AuraRules.FORM);
                p.teleportTo(p.level(), 0.5, 190, 0.5, Set.<Relative>of(), 0, 0, false);
                rival = new Rival(p.level()); teach(rival, AuraRules.GLOW);
                rival.snapTo(0.5, 190, 2.5, 180, 0); rival.setYHeadRot(180);
                p.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(rival)));
                p.level().addNewPlayer(rival);
                return null;
            });
            c.waitTicks(10);
            // Neither a salute nor kneeling alone forces an agreement.
            on(world, p -> { rival.setShiftKeyDown(true); return null; }); c.waitTicks(30);
            check(!on(world, p -> Lineage.isDisciple(p, rival)), "kneeling alone cannot force a master");
            c.getInput().holdKey(o -> o.keyShift); c.waitTicks(65);
            check(on(world, Lineage::underWay), "the settled stance and kneeling must begin a ceremony");
            shot(c, "social_lineage_ceremony");
            on(world, p -> { rival.setShiftKeyDown(false); return null; }); c.waitTicks(3);
            check(!on(world, Lineage::underWay), "standing cancels the ceremony");
            check(!on(world, p -> Lineage.isDisciple(p, rival)), "interrupted ceremony makes no bond");
            on(world, p -> { rival.setShiftKeyDown(true); return null; }); c.waitTicks(175);
            c.getInput().releaseKey(o -> o.keyShift); c.waitTicks(5);
            check(on(world, p -> Lineage.isDisciple(p, rival)), "the completed ceremony records both sides");
            check(on(world, p -> AuraApi.mayPassBlade(p, rival)), "an active disciple qualifies for blade passing");
            check(on(world, p -> Lineage.masterNear(rival)), "the nearby master is recognised");
            check(on(world, p -> Math.abs(Lineage.near(rival, 8) - 10) < .001), "nearby disciple experience uses the configured bonus");
            String taughtPart = on(world, p -> {
                String part = TechniqueRules.allParts().stream().filter(TechniqueRules::scrollable).filter(id -> !Techniques.learned(rival, id)).findFirst().orElseThrow();
                check(Techniques.teach(p, part, "test"), "the master learns one teachable part");
                return part;
            });
            // A lesson produces a known part and its day timer; ordinary practice cannot bypass the timer.
            c.getInput().holdKey(o -> o.keyShift); c.waitTicks(150);
            c.getInput().releaseKey(o -> o.keyShift); c.waitTicks(4);
            check(on(world, p -> LineageRegistry.of(p.level().getServer()).masterOf(rival.getUUID()).orElseThrow().lessonAt() != Long.MIN_VALUE), "a completed lesson records its rest timer");
            check(on(world, p -> Techniques.learned(rival, taughtPart)), "the lesson teaches the disciple an actual technique part");
            on(world, p -> { rival.setShiftKeyDown(false); Lineage.refresh(p); return null; }); c.waitTicks(4);
            AuraScreen.showLineage(true); c.setScreen(() -> new AuraScreen(null)); c.waitTicks(6);
            shot(c, "social_lineage_record");
            UUID other = rival.getUUID();
            click(c, c.computeOnClient(mc -> ((AuraScreen) mc.gui.screen()).lineagePoint("release:" + other)));
            check(on(world, p -> Lineage.isDisciple(p, rival)), "first release click only confirms");
            shot(c, "social_lineage_release_confirmation");
            click(c, c.computeOnClient(mc -> ((AuraScreen) mc.gui.screen()).lineagePoint("release:" + other)));
            c.waitTicks(5); check(!on(world, p -> Lineage.isDisciple(p, rival)), "second release click sends the server-authorised end packet");
            c.setScreen(() -> null); AuraScreen.showLineage(false);
            on(world,p -> {
                check(!AuraApi.mayPassBlade(p,rival), "a released disciple no longer qualifies for blade passing");
                check(Lineage.take(p,rival,false), "the bond can be renewed");
                rival.setShiftKeyDown(true);
                long now=p.level().getGameTime();
                p.setAttached(AuraAttachments.STATE,AuraAttachments.State.NONE.settled(now-40));
                Lineage.breathing(p,Aura.state(p),now);
                check(Lineage.underWay(p), "a renewed bond can begin a lesson");
                check(Lineage.end(p,rival.getUUID()), "a participant can release a bond mid-lesson");
                check(!Lineage.underWay(p) && !p.hasAttached(Lineage.RITE) && !rival.hasAttached(Lineage.RITE), "release cancels the lesson and both progress attachments");
                rival.setShiftKeyDown(false); return null;
            });
            // Consent, count-in and restricted spar damage.
            on(world, p -> { teach(p, AuraRules.FORM); teach(rival, AuraRules.GLOW); Spars.salute(p, rival); return null; }); c.waitTicks(4);
            check(!on(world, Spars::sparring), "one salute never starts a spar"); shot(c, "social_spar_offer");
            on(world, p -> { Spars.salute(rival, p); return null; }); c.waitTicks(5);
            check(on(world, Spars::sparring), "a reciprocal salute starts the count-in"); shot(c, "social_spar_countdown");
            c.runOnClient(mc -> { mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK); mc.player.setXRot(20); });
            c.waitTicks(3); shot(c, "social_spar_standards");
            c.runOnClient(mc -> { mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); mc.player.setXRot(0); });
            c.waitTicks(SparRules.COUNT_TICKS + 3);
            on(world, p -> {
                check(Boolean.TRUE.equals(Duels.canHarm(p, rival)), "sparring permits blade combat");
                float health = rival.getHealth();
                var spell = dev.wildercord.spell.SpellCompiler.compile(List.of(dev.wildercord.spell.Runes.TOUCH, dev.wildercord.spell.Runes.HARM));
                var effect = spell.root().groups.getFirst().effects.getFirst();
                dev.wildercord.cast.Effects.apply(new dev.wildercord.cast.Cast(p), effect,
                    new dev.wildercord.cast.Cast.Hit(List.of(rival), rival.position(), new Vec3(0,0,1), p.position(), null, null, false));
                check(rival.getHealth() == health, "an actual Harm spell cannot hurt a sparring partner");
                rival.hurtServer(p.level(), p.level().damageSources().arrow(null, p), 5);
                check(rival.getHealth() == health, "a projectile cannot hurt a sparring partner");
                p.setAttached(AuraAttachments.AURA, Aura.data(p).withAura(5));
                rival.hurtServer(p.level(), p.level().damageSources().playerAttack(p), 1000);
                check(rival.isAlive(), "the knockout cannot kill the loser");
                return null;
            }); c.waitTicks(4);
            check(!on(world, Spars::sparring), "knockout ends the spar");
            check(on(world, p -> rival.getHealth() == rival.getMaxHealth() && p.getHealth() == p.getMaxHealth()), "both health pools are restored");
            check(on(world, p -> Math.abs(Aura.data(p).aura() - Aura.capacity(p)) < .1), "spent Aura is restored");
            check(on(world, p -> Spars.log(p).wins() == 0 && Spars.log(rival).losses() == 0 && Aura.data(p).xp() == 0), "a brief one-sided spar grants no record or experience");
            shot(c, "social_spar_result");
            on(world, p -> { Duels.rested(p); Duels.rested(rival); check(Spars.start(p, rival), "another rested spar can begin"); return null; });
            c.waitTicks(SparRules.COUNT_TICKS + 2);
            on(world, p -> { var outsider = net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                p.hurtServer(p.level(), p.level().damageSources().mobAttack(outsider), 3); return null; }); c.waitTicks(2);
            check(!on(world, Spars::sparring), "outside harm calls off the spar");
            // A sustained two-sided fight updates records and teaches experience.
            on(world, p -> { teach(p, AuraRules.FORM); teach(rival, AuraRules.GLOW); Spars.forgetBout(p); Spars.forgetBout(rival); check(Spars.start(p, rival), "a rested practice bout starts"); return null; });
            c.waitTicks(SparRules.COUNT_TICKS + 3);
            on(world, p -> {
                p.hurtServer(p.level(), p.level().damageSources().playerAttack(rival), 3);
                rival.hurtServer(p.level(), p.level().damageSources().playerAttack(p), 3);
                return null;
            }); c.waitTicks(SparRules.MIN_FIGHT_TICKS + 2);
            on(world, p -> { rival.damageCooldownTime = 0; boolean hit = rival.hurtServer(p.level(), p.level().damageSources().playerAttack(p), 1000); check(!Spars.sparring(p), "a lethal blow must end a real spar (hit=" + hit + ", health=" + rival.getHealth() + ", bout=" + Spars.bout(p) + ")"); return null; }); c.waitTicks(4);
            check(on(world, p -> Spars.log(p).wins() == 1 && Spars.log(rival).losses() == 1), "a real spar updates both records");
            check(on(world, p -> Aura.data(p).xp() > 0 && Spars.log(p).counted(rival.getUUID(), SparRules.day(p.level().getGameTime())) == 1), "a real spar teaches and counts toward the per-pair cap");
            // Two real crescents lock; the client's actual attack key becomes a judged press.
            on(world, p -> {
                teach(p, AuraRules.EDGE); teach(rival, AuraRules.GLOW); Clashes.forgetRest();
                rival.snapTo(0.5, 190, 10.5, 180, 0);
                mine = Crescents.launch(p, new Vec3(.5,191.3,2), new Vec3(0,0,1), 0xF26E42, 6, 0, .7, 24, 2, 6, false, e -> false, (f,t) -> 0).pierce();
                theirs = Crescents.launch(rival, new Vec3(.5,191.3,9), new Vec3(0,0,-1), 0x8CDDFF, 6, 0, .7, 24, 2, 6, false, e -> false, (f,t) -> 0);
                return null;
            }); c.waitTicks(5);
            check(on(world, Clashes::clashing), "the crescents lock into a timed clash");
            check(c.computeOnClient(mc -> AuraSocialClient.ownId() >= 0), "the Begin packet reaches the client HUD");
            shot(c, "social_clash_rhythm");
            on(world, p -> {
                // Use a different identity: a matching UUID would be the same participant.
                var outsider = new FakePlayer(p.level(), new GameProfile(UUID.randomUUID(), "Observer")) {};
                int score = Clashes.score(p); Clashes.press(outsider, Clashes.idOf(p));
                check(Clashes.score(p) == score && Clashes.score(outsider) == 0, "an outsider cannot press for a participant");
                return null;
            });
            for (int i = 0; i < ClashRules.serverLength() + 3; i++) {
                int age = on(world, Clashes::ageOf);
                if (age == ClashRules.beat(0) || age == ClashRules.beat(1) || age == ClashRules.beat(2)) c.getInput().pressKey(o -> o.keyAttack);
                c.waitTicks(1);
            }
            check(!on(world, Clashes::clashing), "the timed clash resolves");
            check(on(world, p -> !mine.done() && theirs.done()), "client presses beat an opponent who misses");
            check(Math.abs(mine.damage() - 6 * ClashRules.CARRY) < .001, "winner carries the configured reduced damage");
            shot(c, "social_clash_result");
            c.waitTicks(35); check(c.computeOnClient(mc -> AuraSocialClient.ownId() < 0), "finished HUD expires");
            // Held arts are forfeited or released once; answered art damage is restored only after a win.
            var performed = new java.util.concurrent.atomic.AtomicInteger();
            var heldArt = AuraApi.StringArt.of("test:social_held", "swing swing low", AuraRules.GLOW, 5, 0, (player, marks) -> { performed.incrementAndGet(); return true; });
            float before = on(world, p -> {
                teach(p, AuraRules.FORM); teach(rival, AuraRules.GLOW); Clashes.forgetRest(); mine.done = true; theirs.done = true;
                p.teleportTo(p.level(), .5,190,.5,Set.<Relative>of(),0,0,false); p.setDeltaMovement(Vec3.ZERO);
                rival.snapTo(.5,190,3.5,180,0);
                theirs = Crescents.launch(rival, new Vec3(.5,191.3,2.5), new Vec3(0,0,-1), 0x8CDDFF, 6,0,.7,12,2,6,false,e -> false,(f,t) -> 0);
                check(Clashes.meets(p, heldArt, List.of(1,1,0)), "an art meets an oncoming crescent");
                check(Clashes.holding(p), "the art waits for the clash result");
                return Aura.data(p).aura();
            });
            boolean[] pressed = new boolean[ClashRules.BEATS];
            for (int i=0; i<ClashRules.serverLength()+2; i++) {
                on(world,p -> { int age=Clashes.ageOf(p); for(int beat=0; beat<pressed.length; beat++) if(!pressed[beat] && Math.abs(age-ClashRules.beat(beat)) <= 1) { Clashes.pressFor(rival); pressed[beat]=true; } return null; });
                c.waitTicks(1);
            }
            check(performed.get() == 0 && !on(world,Clashes::holding), "losing an art clash forfeits the art without performing it");
            check(on(world,p -> Aura.data(p).aura()) < before, "the forfeited art is still paid for");
            on(world,p -> {
                teach(p,AuraRules.FORM); rival.damageCooldownTime=0; Clashes.forgetRest(); theirs.done=true;
                p.teleportTo(p.level(), .5,190,.5,Set.<Relative>of(),0,0,false); p.setDeltaMovement(Vec3.ZERO);
                var firstArt=AuraApi.StringArt.of("test:social_first", "swing swing low", AuraRules.GLOW,0,0,(attacker,marks) -> {
                    float health=p.getHealth(); p.damageCooldownTime=0;
                    p.hurtServer(p.level(),p.level().damageSources().playerAttack(rival),4);
                    Clashes.artStruck(rival,p,health-p.getHealth(),4);
                    return true;
                });
                check(SwordStrings.release(rival,firstArt,List.of(1,1,0)), "the first art lands");
                check(p.getHealth()<p.getMaxHealth(), "there is real damage to turn aside");
                check(Clashes.meets(p,heldArt,List.of(1,1,0)), "an answering art locks with the first strike");
                return null;
            });
            java.util.Arrays.fill(pressed,false);
            for(int i=0;i<ClashRules.serverLength()+2;i++) {
                on(world,p -> {int age=Clashes.ageOf(p);for(int beat=0;beat<pressed.length;beat++)if(!pressed[beat] && Math.abs(age-ClashRules.beat(beat))<=1){Clashes.pressFor(p);pressed[beat]=true;}return null;});c.waitTicks(1);
            }
            check(performed.get()==1, "a winning held art releases exactly once");
            check(on(world,p -> p.getHealth()==p.getMaxHealth()), "winning the answering clash restores the first art's damage");
            // Offline masters keep earned shares, graduation and voluntary release through saved-data serialization.
            on(world, p -> {
                teach(p, AuraRules.GLOW); teach(rival, AuraRules.FORM);
                check(Lineage.take(rival, p, false), "the reciprocal role can form a valid bond");
                UUID mentor = rival.getUUID(); rival.discard();
                Lineage.brokeThrough(p, AuraRules.FLOW);
                var registry = LineageRegistry.of(p.level().getServer());
                double owed = registry.masterOf(p.getUUID()).orElseThrow().owed();
                check(owed > 0, "an offline master retains their breakthrough share");
                var encoded = LineageRegistry.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, registry).getOrThrow();
                var restored = LineageRegistry.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow();
                check(restored.bonded(mentor, p.getUUID()) && restored.masterOf(p.getUUID()).orElseThrow().owed() == owed, "saved lineage preserves bonds and owed experience");
                Lineage.brokeThrough(p, AuraRules.FORM);
                check(registry.masterOf(p.getUUID()).isEmpty() && registry.honoured(mentor).size() == 1, "reaching the absent master's stage graduates with honour");
                check(registry.collect(mentor) > owed && registry.collect(mentor) == 0, "graduated offline shares are paid once");
                var absent = new Rival(p.level()); teach(absent, AuraRules.FORM);
                check(Lineage.take(absent, p, false), "an absent master can be represented in the saved record");
                check(Lineage.end(p, absent.getUUID()), "either side may release an offline bond");
                check(registry.masterOf(p.getUUID()).isEmpty(), "offline release is authoritative");
                return null;
            });
            c.runOnClient(mc -> { mc.getWindow().setWindowed(960,540); mc.options.guiScale().set(2); mc.resizeGui(); });
            c.waitTicks(4); shot(c,"social_compact_hud");
        } finally {
            c.getInput().releaseKey(o -> o.keyShift); AuraScreen.showLineage(false); rival = null;
        }
    }
}
