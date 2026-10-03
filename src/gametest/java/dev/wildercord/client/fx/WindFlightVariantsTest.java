package dev.wildercord.client.fx;

import dev.wildercord.cast.*;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Set;

/** Real paid alternate deliveries/modifiers and covered/uncovered mixed groups. */
public final class WindFlightVariantsTest implements FabricClientGameTest {
    private record Case(String name, List<String> spell, String effects, int style, boolean arc, boolean covered) {}
    @Override public void runTest(ClientGameTestContext c) {
        var previous = c.computeOnClient(mc -> MagicQuality.own);
        try (var world = c.worldBuilder().create()) {
            c.waitTicks(40);
            var server = world.getServer();
            server.runCommand("gamerule spawn_mobs false");server.runCommand("time set 6000");server.runCommand("weather clear");
            server.runCommand("fill -16 100 -12 16 100 40 polished_deepslate");
            server.runCommand("fill -12 101 32 12 109 32 gray_concrete");
            server.runOnServer(s -> {
                var p = s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
                Spellbooks.setCord(p, new ItemStack(WildercordItems.ECHO_CORD));
                var book = Spellbooks.get(p).withStarterGiven();for (var r : Runes.all()) book = book.learn(r.id());Spellbooks.set(p, book);
            });
            c.waitTicks(15);
            c.runOnClient(mc -> {mc.getWindow().setWindowed(1280,720);mc.resizeGui();if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();MagicQuality.own = MagicQuality.Level.FULL;});
            for (var sample : List.of(
                new Case("arc_cyclone", List.of("arc","cyclone"), "wildercord:cyclone", 0, true, true),
                new Case("arc_feather_fall", List.of("arc","feather_fall"), "wildercord:feather_fall", 0, true, true),
                new Case("pierce_windcut", List.of("bolt","windcut","pierce"), "wildercord:windcut", RuneBolt.STYLE_PIERCE, false, true),
                new Case("frugal_swift", List.of("bolt","swift","frugal"), "wildercord:swift", RuneBolt.STYLE_FRUGAL, false, true),
                new Case("mixed_windcut_shock", List.of("bolt","windcut","shock"), "wildercord:windcut,wildercord:shock", 0, false, true),
                new Case("mixed_windcut_umbra", List.of("bolt","windcut","umbra"), "wildercord:windcut,wildercord:umbra", 0, false, true),
                new Case("mixed_windcut_harm", List.of("bolt","windcut","harm"), "wildercord:windcut,wildercord:harm", 0, false, false))) {
                server.runOnServer(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst();
                    p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
                    p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,sample.arc ? -15 : 0,false);
                });
                c.waitTicks(12);c.runOnClient(mc -> {mc.particleEngine.clearParticles();mc.gui.toastManager().clear();});
                var empty = c.computeOnClient(mc -> WindFlightTest.snapshot(mc,"wind_variant_"+sample.name+"_background"));c.waitFor(mc -> empty.isDone());empty.join();
                server.runOnServer(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst();
                    check(SpellCaster.edit(p,0,sample.spell.stream().map(id -> "wildercord:"+id).toList()) == null,"Accepted "+sample.name);
                    Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);float before = Spellbooks.mana(p);
                    SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Paid "+sample.name);
                });
                c.waitTicks(4);
                Vec3[] first = new Vec3[1];
                server.runOnServer(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst();
                    first[0] = p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).getFirst().getDeltaMovement();
                });
                c.waitTicks(4);
                server.runOnServer(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst();
                    var bolt = p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).getFirst();
                    if (sample.arc) check(bolt.getDeltaMovement().y < first[0].y-.1,"Native lob falls: "+sample.name);
                    else check(bolt.getDeltaMovement().distanceTo(first[0])<.001,"Straight motion retained");
                });
                c.runOnClient(mc -> {
                    RuneBolt bolt = null;for (var e : mc.level.entitiesForRendering()) if (e instanceof RuneBolt b) bolt = b;
                    check(bolt != null,"Live client "+sample.name);
                    check(bolt.getEntityData().get(RuneBolt.DATA_EFFECTS).equals(sample.effects),"Exact group metadata");
                    check((bolt.getEntityData().get(RuneBolt.DATA_STYLE)&sample.style)==sample.style,"Modifier style retained");
                    check(WindFlightTest.authoredNear(mc,bolt),"Authored body in alternate route");
                    var particles = WindFlightTest.particles(mc.particleEngine);
                    check(particles.stream().anyMatch(p -> p.getClass().getSimpleName().equals("Comet")
                        && (Boolean)WindFlightTest.field(p,p.getClass(),"authored")==sample.covered),"Correct fallback coverage");
                    if (sample.name.equals("mixed_windcut_shock")) {
                        for (int ingredient : new int[]{3,2}) check(particles.stream().anyMatch(p -> p instanceof MaterialParticle
                            && ((Number)WindFlightTest.field(p,Particle.class,"lifetime")).intValue()==5
                            && ((Number)WindFlightTest.field(p,MaterialParticle.class,"style")).intValue()==ingredient),"Both authored schools present");
                    }
                });
                var shot = c.computeOnClient(mc -> WindFlightTest.snapshot(mc,"wind_variant_"+sample.name));c.waitFor(mc -> shot.isDone());shot.join();
                c.waitTicks(2);
                c.runOnClient(mc -> {
                    RuneBolt live = null;for (var e : mc.level.entitiesForRendering()) if (e instanceof RuneBolt b) live = b;
                    check(live != null && WindFlightTest.authoredNear(mc,live),"Later alternate flight remains visible: "+sample.name);
                });
                var later = c.computeOnClient(mc -> WindFlightTest.snapshot(mc,"wind_variant_travel_"+sample.name));c.waitFor(mc -> later.isDone());later.join();
            }
        } finally {c.runOnClient(mc -> MagicQuality.own = previous);}
    }
    private static void check(boolean yes,String why) {if (!yes) throw new AssertionError(why);}
}
