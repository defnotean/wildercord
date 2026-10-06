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
import dev.wildercord.spell.WorldRules;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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
    private static int collateral, pins;
    private static String caseMode="";
    private static long pinnedSeed=-1;
    private static double pinnedRoll;
    private static boolean retiredInsideCallback;
    public static void run(ClientGameTestContext c) {
        if (!registered) {
            registered=true;
            ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,taken,blocked)->{
                if(target!=secondary || !(source instanceof RelayDamageSource relay)) return;
                collateral++;
                RelayCircleTest.check(Effects.applyingCast()!=null && Effects.applyingCast().identity()==relay.cast().identity()
                    && Effects.applying()==owner && Effects.currentElementNow().equals("storm"),"Post-effect water damage retains the original paid Cast and element");
                if(dryResidueSite) for(var pos:BlockPos.betweenClosed(5,150,5,7,151,7)) owner.level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                if(retire) {
                    Spellbooks.setCord(owner,new ItemStack(WildercordItems.ECHO_CORD));
                    retiredInsideCallback=!relay.cast().alive()&&!RelayCircles.pending(owner);
                }
                // Keep the existing residue fixture's seed schedule. Charge cases pin after
                // TrainingDummy has also finished its real wound record and floating-number draws.
                if(dryResidueSite)pinNextCharge(owner.level());
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
                        secondary=dryResidueSite?WildercordEntities.TRAINING_DUMMY.create(owner.level(),EntitySpawnReason.MOB_SUMMONED)
                            :new ChargeWitness(owner.level());
                        RelayCircleTest.check(secondary!=null,"Native water collateral creates");secondary.snapTo(6.5,150,6.5,180,0);secondary.setNoGravity(true);owner.level().addFreshEntity(secondary);
                        Shields.raise(new Cast(owner).weigh(100),primary[0],200);
                        Reactions.callout(new Cast(owner),"conduct",0xFFE650);
                        collateral=0;retire=mode.equals("retired_charge");caseMode=mode;
                        pins=0;pinnedSeed=-1;pinnedRoll=-1;retiredInsideCallback=false;
                    });c.waitTicks(12);
                    world.getServer().runOnServer(s->{RelayCircleTest.check(primary[0].isInWater()&&secondary.isInWater(),"Real connected water reaches both fixtures");RelayCircleTest.directDown(owner);});c.waitTicks(2);
                    world.getServer().runOnServer(s->{RelayCircleTest.aim(owner,primary[0].getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
                    world.getServer().runOnServer(s->{
                        RelayCircleTest.check(primary[0].getHealth()==primary[0].getMaxHealth(),"Aged Shield absorbs the primary without a counter or mastery strike");
                        RelayCircleTest.check(collateral==1&&secondary.hitSequence()==1,"First real damage comes from inherited world-water conduction beyond Shock's five-block arc");
                        int residue=0;for(var pos:BlockPos.betweenClosed(4,149,4,8,153,8)) if(ResidueBlocks.is(owner.level().getBlockState(pos)))residue++;
                        if(dryResidueSite) RelayCircleTest.check(mode.equals("residue")?residue==1:residue==0,"Inherited Lingering Mark retains the original paid block allowance");
                        if(mode.equals("charge")||retire){
                            chargeEvidence("after_release",primary[0]);
                            RelayCircleTest.check(pins==1&&pinnedSeed>=0&&pinnedRoll<WorldRules.CREEPER_CHARGE_CHANCE,"The real collateral wound pins one eligible charge roll after its native return");
                            if(retire)RelayCircleTest.check(retiredInsideCallback,"The original paid owner retires inside the actual conduction damage callback");
                            RelayCircleTest.check(primary[0].isPowered()!=retire,"A pinned native creeper charge occurs only while the original Relay admission survives its conduction callback");
                        }
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
        } finally {owner=null;secondary=null;retire=false;dryResidueSite=false;caseMode="";}
    }

    /** Runs the complete native dummy damage path; only the fixture's random pin moves past its VFX. */
    private static final class ChargeWitness extends TrainingDummy {
        ChargeWitness(ServerLevel level){super(WildercordEntities.TRAINING_DUMMY,level);}
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){
            boolean hurt=super.hurtServer(level,source,amount);
            if(this==secondary&&source instanceof RelayDamageSource&&lastDamage()>0){
                pinNextCharge(level);
                chargeEvidence("after_native_dummy_return",null);
            }
            return hurt;
        }
    }
    private static void pinNextCharge(ServerLevel level){
        var random=level.getRandom();
        for(long seed=0;seed<65_536;seed++){
            random.setSeed(seed);double roll=random.nextDouble();
            if(roll<WorldRules.CREEPER_CHARGE_CHANCE){
                random.setSeed(seed);pins++;pinnedSeed=seed;pinnedRoll=roll;return;
            }
        }
        throw new AssertionError("No eligible charge roll in bounded native RNG seed search");
    }
    private static void chargeEvidence(String phase,Creeper primary){
        var value=new com.google.gson.JsonObject();
        value.addProperty("phase",phase);value.addProperty("mode",caseMode);value.addProperty("tick",owner.level().getGameTime());
        value.addProperty("rng",owner.level().getRandom().getClass().getName());value.addProperty("pinCalls",pins);
        value.addProperty("seed",pinnedSeed);value.addProperty("pinnedNextDouble",pinnedRoll);
        value.addProperty("collateralCallbacks",collateral);value.addProperty("secondaryHits",secondary.hitSequence());value.addProperty("secondaryDamage",secondary.lastDamage());
        value.addProperty("retiredInsideCallback",retiredInsideCallback);value.addProperty("focusRegistered",RelayCircles.pending(owner));
        if(primary!=null)value.addProperty("primaryPowered",primary.isPowered());
        System.out.println("WILDERCORD_RELAY_CREEPER "+value);
    }
}
