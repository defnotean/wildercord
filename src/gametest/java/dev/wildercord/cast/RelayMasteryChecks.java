package dev.wildercord.cast;

import dev.wildercord.api.SpellMasteryApi;
import dev.wildercord.config.Config;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Uses the actual onCast -> afterDamage -> CHAIN hook, with a deterministic test trait (no production RNG change). */
public final class RelayMasteryChecks {
    private RelayMasteryChecks() {}
    private static final String CHAIN = "wildercord_test:relay_chain";
    private static boolean registered;
    private static ServerPlayer owner;
    private static TrainingDummy secondary;
    private static Vec3 incoming;
    private static int attempts;
    private static boolean retire;

    public static void run(ClientGameTestContext c) {
        if (!registered) {
            registered = true;
            SpellMasteryApi.registerTrait(MasteryTraits.custom(CHAIN,"Deterministic Relay chain","Native hook fixture",MasteryTraits.Hook.CHAIN,1).harmful());
            ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount) -> {
                if (target == secondary && source instanceof RelayDamageSource) {
                    attempts++; incoming = source.getSourcePosition();
                    if (retire) Spellbooks.setCord(owner,new ItemStack(WildercordItems.ECHO_CORD));
                }
                return true;
            });
        }
        try (var world = c.worldBuilder().create()) {
            c.waitTicks(40); world.getServer().runCommand("gamerule spawn_mobs false");
            var original = world.getServer().computeOnServer(s -> Config.get());
            List<TrainingDummy> bodies = new ArrayList<>();
            try {
                for (String mode : List.of("control","budget","cover","retire","overflow")) {
                    TrainingDummy[] primary = new TrainingDummy[1];
                    world.getServer().runOnServer(s -> {
                        owner = RelayCircleTest.player(s); RelayCircleTest.prepare(owner,Runes.HARM);
                        CampConcordNative.config(CampConcordNative.copy(original,Map.of("maxCreatures",mode.equals("budget") ? 1 : 64)));
                        var entry = MasteryBook.Entry.fresh(Mastery.keyOf(List.of(Runes.RELAY,Runes.HARM)),1,owner.level().getGameTime()).withTrait(0,CHAIN,false);
                        owner.setAttached(MasteryAttachments.MASTERY,new MasteryBook(List.of(entry)));
                        primary[0] = dummy(.5,6.5); bodies.add(primary[0]); secondary = dummy(2.5,6.5); bodies.add(secondary);
                        if (mode.equals("overflow")) for (int i=0;i<63;i++) bodies.add(dummy(2.5,6.5));
                        if (mode.equals("cover")) for (int y=150;y<=152;y++) owner.level().setBlock(new BlockPos(1,y,6),Blocks.STONE.defaultBlockState(),2);
                        attempts=0;incoming=null;retire=mode.equals("retire");RelayCircleTest.directDown(owner);
                    }); c.waitTicks(2);
                    world.getServer().runOnServer(s -> {RelayCircleTest.aim(owner,primary[0].getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);}); c.waitTicks(8);
                    world.getServer().runOnServer(s -> {
                        RelayCircleTest.check(primary[0].hitSequence()==1,"Actual paid Relay primary reaches Mastery hook: "+mode);
                        boolean reached=mode.equals("control")||mode.equals("retire");
                        RelayCircleTest.check(attempts==(reached?1:0),"Inherited chain respects shared allowance, secondary sightline and bounded candidate overflow: "+mode);
                        RelayCircleTest.check(secondary.hitSequence()==(mode.equals("control")?1:0),"Real native chain damage cannot outlive original focus: "+mode);
                        if(reached) RelayCircleTest.check(incoming.distanceToSqr(primary[0].getBoundingBox().getCenter())<.0001,"Secondary native damage retains primary victim as its true incoming origin");
                        bodies.forEach(TrainingDummy::discard);bodies.clear();secondary=null;
                    });c.waitTicks(12);
                }
            } finally {world.getServer().runOnServer(s -> CampConcordNative.config(original));}
        } finally {owner=null;secondary=null;retire=false;}
    }
    private static TrainingDummy dummy(double x,double z) {
        var d=WildercordEntities.TRAINING_DUMMY.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);
        RelayCircleTest.check(d!=null,"Native Mastery dummy creates");d.snapTo(x,150,z,180,0);d.setNoGravity(true);owner.level().addFreshEntity(d);return d;
    }
}
