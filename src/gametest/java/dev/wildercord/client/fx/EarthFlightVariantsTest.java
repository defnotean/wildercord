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
public final class EarthFlightVariantsTest implements FabricClientGameTest {
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
            c.runOnClient(mc -> {
                var material=new dev.wildercord.content.EarthOption(dev.wildercord.content.EarthOption.ROCK,0xC7B897,.2F,8,new Vec3(.01,-.01,0),.15F);
                var rock=new EarthParticle(mc.level,0,80,0,material);
                check(rock.getLightCoords(0)!=net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,"Physical stone uses sampled world light below the platform");
                boolean previousFlash=MagicQuality.reducedFlash;
                try {
                    MagicQuality.reducedFlash=false;rock.tick();float normal=((Number)EarthFlightTest.field(rock,net.minecraft.client.particle.SingleQuadParticle.class,"alpha")).floatValue();
                    var softened=new EarthParticle(mc.level,0,80,0,material);MagicQuality.reducedFlash=true;softened.tick();
                    float reduced=((Number)EarthFlightTest.field(softened,net.minecraft.client.particle.SingleQuadParticle.class,"alpha")).floatValue();
                    check(reduced<normal && reduced>0,"Reduced Flash softens physical debris without removing it");
                }finally{MagicQuality.reducedFlash=previousFlash;}
                for(var velocity:List.of(new Vec3(0,1,0),new Vec3(0,-1,0),Vec3.ZERO)) {
                    EarthFlights.draw("wildercord:pelt",4,2,2,Vec3.ZERO,velocity,true,(option,point)->check(Double.isFinite(point.lengthSqr()) && point.length()<2,"Finite bounded vertical or stationary body"));
                }
            });
            for (var sample : List.of(
                new Case("arc_pelt", List.of("arc","pelt"), "wildercord:pelt", 0, true, true),
                new Case("arc_stalactite", List.of("arc","stalactite"), "wildercord:stalactite", 0, true, true),
                new Case("pierce_chisel", List.of("bolt","chisel","pierce"), "wildercord:chisel", RuneBolt.STYLE_PIERCE, false, true),
                new Case("frugal_prospect", List.of("bolt","prospect","frugal"), "wildercord:prospect", RuneBolt.STYLE_FRUGAL, false, true),
                new Case("mixed_chisel_shock", List.of("bolt","chisel","shock"), "wildercord:chisel,wildercord:shock", 0, false, true),
                new Case("mixed_chisel_umbra", List.of("bolt","chisel","umbra"), "wildercord:chisel,wildercord:umbra", 0, false, true),
                new Case("mixed_chisel_harm", List.of("bolt","chisel","harm"), "wildercord:chisel,wildercord:harm", 0, false, false))) {
                server.runOnServer(s -> {
                    var p = s.getPlayerList().getPlayers().getFirst();
                    p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
                    p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,sample.arc ? -15 : 0,false);
                });
                c.waitTicks(12);c.runOnClient(mc -> {mc.particleEngine.clearParticles();mc.gui.toastManager().clear();});
                var empty = c.computeOnClient(mc -> EarthFlightTest.snapshot(mc,"earth_variant_"+sample.name+"_background"));c.waitFor(mc -> empty.isDone());empty.join();
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
                    check(EarthFlightTest.authoredNear(mc,bolt),"Authored body in alternate route");
                    var particles = EarthFlightTest.particles(mc.particleEngine);
                    check(particles.stream().anyMatch(p -> p.getClass().getSimpleName().equals("Comet")
                        && (Boolean)EarthFlightTest.field(p,p.getClass(),"authored")==sample.covered),"Correct fallback coverage");
                    if (sample.name.equals("mixed_chisel_shock")) {
                        check(particles.stream().anyMatch(p -> p instanceof EarthParticle),"Physical earth ingredient retained in mixed spell");
                        check(particles.stream().anyMatch(p -> p instanceof MaterialParticle
                            && ((Number)EarthFlightTest.field(p,Particle.class,"lifetime")).intValue()==5
                            && ((Number)EarthFlightTest.field(p,MaterialParticle.class,"style")).intValue()==2),"Authored storm ingredient retained in mixed spell");
                    }

                });
                var shot = c.computeOnClient(mc -> EarthFlightTest.snapshot(mc,"earth_variant_"+sample.name));c.waitFor(mc -> shot.isDone());shot.join();
                c.waitTicks(2);
                c.runOnClient(mc -> {
                    RuneBolt live = null;for (var e : mc.level.entitiesForRendering()) if (e instanceof RuneBolt b) live = b;
                    check(live != null && EarthFlightTest.authoredNear(mc,live),"Later alternate flight remains visible: "+sample.name);
                });
                var later = c.computeOnClient(mc -> EarthFlightTest.snapshot(mc,"earth_variant_travel_"+sample.name));c.waitFor(mc -> later.isDone());later.join();
            }
        } finally {c.runOnClient(mc -> MagicQuality.own = previous);}
    }
    private static void check(boolean yes,String why) {if (!yes) throw new AssertionError(why);}
}
