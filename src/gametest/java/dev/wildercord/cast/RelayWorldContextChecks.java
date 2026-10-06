package dev.wildercord.cast;

import dev.wildercord.config.Config;
import dev.wildercord.content.ResidueBlocks;
import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.WildercordBlocks;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ServerLevelData;

import java.util.List;
import java.util.Map;

/** Real inherited water damage must retain its Relay effect context through mastery and world mutations. */
public final class RelayWorldContextChecks {
    private RelayWorldContextChecks() {}
    private static boolean registered, retire, dryResidueSite;
    private static ServerPlayer owner;
    private static TrainingDummy secondary;
    private static int collateral;
    public static void run(ClientGameTestContext c) {
        if (!registered) {
            registered=true;
            ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,taken,blocked)->{
                if(target!=secondary || !(source instanceof RelayDamageSource relay)) return;
                collateral++;
                RelayCircleTest.check(Effects.applyingCast()!=null && Effects.applyingCast().identity()==relay.cast().identity()
                    && Effects.applying()==owner && Effects.currentElementNow().equals("storm"),"Post-effect water damage retains the original paid Cast and element");
                if(dryResidueSite) for(var pos:BlockPos.betweenClosed(5,150,5,7,151,7)) owner.level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                if(retire) Spellbooks.setCord(owner,new ItemStack(WildercordItems.ECHO_CORD));
                // Pin the next charge roll through the real world's RNG without assuming its implementation.
                var random=owner.level().getRandom();
                for(long seed=0;;seed++){random.setSeed(seed);if(random.nextDouble()<.25){random.setSeed(seed);break;}}
            });
        }
        try(var world=c.worldBuilder().create()) {
            c.waitTicks(40);world.getServer().runCommand("gamerule spawn_mobs false");
            var original=world.getServer().computeOnServer(s->Config.get());
            try {
                for(String mode:List.of("no_blocks","residue","charge","retired_charge")) {
                    Creeper[] primary=new Creeper[1];
                    world.getServer().runOnServer(s->{
                        owner=RelayCircleTest.player(s);RelayCircleTest.prepare(owner,Runes.SHOCK);
                        ((ServerLevelData)owner.level().getLevelData()).setGameTime(owner.level().getGameTime()+200);
                        CampConcordNative.config(CampConcordNative.copy(original,Map.of("maxBlocks",mode.equals("no_blocks")?0:32,"worldChangingMagic",true,"spellsEditBlocks",true,
                            "residues",CampConcordNative.copy(original.residues(),Map.of("enabled",true)))));
                        var entry=MasteryBook.Entry.fresh(Mastery.keyOf(List.of(Runes.RELAY,Runes.SHOCK)),1,owner.level().getGameTime());
                        dryResidueSite=mode.equals("residue")||mode.equals("no_blocks");
                        if(dryResidueSite)entry=entry.withTrait(0,MasteryTraits.LINGERING_MARK.id(),false);
                        owner.setAttached(MasteryAttachments.MASTERY,new MasteryBook(List.of(entry)));
                        for(int x=-1;x<=7;x++) for(int z=5;z<=7;z++) owner.level().setBlock(new BlockPos(x,150,z),Blocks.WATER.defaultBlockState(),2);
                        primary[0]=EntityTypes.CREEPER.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);
                        RelayCircleTest.check(primary[0]!=null,"Native primary creeper creates");primary[0].snapTo(.5,150,6.5,180,0);primary[0].setNoAi(true);primary[0].setNoGravity(true);owner.level().addFreshEntity(primary[0]);
                        secondary=WildercordEntities.TRAINING_DUMMY.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);
                        RelayCircleTest.check(secondary!=null,"Native water collateral creates");secondary.snapTo(6.5,150,6.5,180,0);secondary.setNoGravity(true);owner.level().addFreshEntity(secondary);
                        Shields.raise(new Cast(owner).weigh(100),primary[0],200);
                        Reactions.callout(new Cast(owner),"conduct",0xFFE650);
                        collateral=0;retire=mode.equals("retired_charge");
                    });c.waitTicks(12);
                    world.getServer().runOnServer(s->{RelayCircleTest.check(primary[0].isInWater()&&secondary.isInWater(),"Real connected water reaches both fixtures");RelayCircleTest.directDown(owner);});c.waitTicks(2);
                    world.getServer().runOnServer(s->{RelayCircleTest.aim(owner,primary[0].getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
                    world.getServer().runOnServer(s->{
                        RelayCircleTest.check(primary[0].getHealth()==primary[0].getMaxHealth(),"Aged Shield absorbs the primary without a counter or mastery strike");
                        RelayCircleTest.check(collateral==1&&secondary.hitSequence()==1,"First real damage comes from inherited world-water conduction beyond Shock's five-block arc");
                        int residue=0;for(var pos:BlockPos.betweenClosed(4,149,4,8,153,8)) if(ResidueBlocks.is(owner.level().getBlockState(pos)))residue++;
                        if(dryResidueSite) RelayCircleTest.check(mode.equals("residue")?residue==1:residue==0,"Inherited Lingering Mark retains the original paid block allowance");
                        if(mode.equals("charge")||retire)RelayCircleTest.check(primary[0].isPowered()!=retire,"A pinned native creeper charge occurs only while the original Relay admission survives its conduction callback");
                        RelayCircleTest.check(Effects.applyingCast()==null&&Effects.applying()==null&&Effects.currentElementNow().isEmpty(),"Inherited effect scope restores its outer context after the real release");
                        primary[0].discard();secondary.discard();secondary=null;
                    });c.waitTicks(12);
                }
                for(boolean covered:List.of(false,true)) {
                    TrainingDummy[] primary=new TrainingDummy[1];BlockPos seal=new BlockPos(1,150,6);
                    world.getServer().runOnServer(s->{
                        RelayCircleTest.prepare(owner,Runes.HARM);owner.setAttached(MasteryAttachments.MASTERY,MasteryBook.EMPTY);CampConcordNative.config(original);
                        primary[0]=WildercordEntities.TRAINING_DUMMY.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);RelayCircleTest.check(primary[0]!=null,"Native seal target creates");primary[0].snapTo(.5,150,6.5,180,0);primary[0].setNoGravity(true);owner.level().addFreshEntity(primary[0]);
                        owner.level().setBlock(seal,WildercordBlocks.RUNE_SEAL.defaultBlockState().setValue(RuneSealBlock.ELEMENT,RuneSealBlock.Element.ARCANE),2);
                        if(covered)for(int y=150;y<=152;y++)owner.level().setBlock(new BlockPos(1,y,5),Blocks.STONE.defaultBlockState(),2);
                        RelayCircleTest.directDown(owner);
                    });c.waitTicks(2);
                    world.getServer().runOnServer(s->{RelayCircleTest.aim(owner,primary[0].getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
                    world.getServer().runOnServer(s->{
                        RelayCircleTest.check(primary[0].hitSequence()==1,"The actual Relay ray hits beside the initiating seal");
                        RelayCircleTest.check(owner.level().getBlockState(seal).is(WildercordBlocks.RUNE_SEAL)==covered,"Visible solid seal surface admits the normal door response; intervening cover refuses its initiation");
                        primary[0].discard();
                    });c.waitTicks(12);
                }
                for(String mode:List.of("reveal","reveal_budget","reveal_cover","reveal_overflow")) {
                    var bodies=new java.util.ArrayList<TrainingDummy>();
                    world.getServer().runOnServer(s->{
                        RelayCircleTest.prepare(owner,Runes.HARM);owner.setAttached(MasteryAttachments.MASTERY,MasteryBook.EMPTY);
                        CampConcordNative.config(CampConcordNative.copy(original,Map.of("maxCreatures",mode.equals("reveal_budget")?1:64)));
                        for(int i=0;i<(mode.equals("reveal_overflow")?65:2);i++) {
                            var d=WildercordEntities.TRAINING_DUMMY.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);RelayCircleTest.check(d!=null,"Native shimmer dummy creates");
                            d.snapTo(i==0?.5:2.5,150,6.5,180,0);d.setNoGravity(true);owner.level().addFreshEntity(d);bodies.add(d);
                            if(i>0)d.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,2000,0,false,false));
                        }
                        if(mode.equals("reveal_cover"))for(int y=150;y<=152;y++)owner.level().setBlock(new BlockPos(1,y,6),Blocks.STONE.defaultBlockState(),2);
                        RelayCircleTest.directDown(owner);
                    });c.waitTicks(2);
                    world.getServer().runOnServer(s->{RelayCircleTest.check(bodies.get(1).isInvisible(),"Native secondary is invisible before Harm shimmer");RelayCircleTest.aim(owner,bodies.getFirst().getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
                    world.getServer().runOnServer(s->{
                        RelayCircleTest.check(bodies.getFirst().hitSequence()==1,"Actual Relay Harm reaches the inherited arcane shimmer hook");
                        long revealed=bodies.stream().skip(1).filter(d->d.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)).count();
                        RelayCircleTest.check(revealed==(mode.equals("reveal")?1:0),"Arcane reveal honors the paid creature allowance, incoming sightline and candidate overflow");
                        bodies.forEach(TrainingDummy::discard);
                    });c.waitTicks(12);
                }
            } finally {world.getServer().runOnServer(s->CampConcordNative.config(original));}
        } finally {owner=null;secondary=null;retire=false;dryResidueSite=false;}
    }
}
