package dev.wildercord.cast;

import dev.wildercord.aura.*;
import dev.wildercord.config.Config;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Native regressions for inherited elemental hooks, scoped resonance and bounded physical target selection. */
public final class RelayInheritedChecks {
	private RelayInheritedChecks() {}
	private static boolean registered;
	private static ServerPlayer owner;
	private static TrainingDummy watched;
	private static boolean retireBonus;
	private static int bonusAttempts;
	public static void run(ClientGameTestContext c) {
		if(!registered){registered=true;ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount)->{
			if(target==watched && source instanceof RelayDamageSource && source.is(DamageTypes.MAGIC)) {
				bonusAttempts++;
				if(retireBonus)Spellbooks.setCord(owner,new ItemStack(WildercordItems.ECHO_CORD));
			}
			return true;
		});}
		try(var world=c.worldBuilder().create()){
			c.waitTicks(40);world.getServer().runCommand("gamerule spawn_mobs false");
			var original=world.getServer().computeOnServer(s->Config.get());
			try {
				TrainingDummy[] wet=new TrainingDummy[2];
				TideScribe[] scribe=new TideScribe[1];float[] scribeHealth=new float[1];
				world.getServer().runOnServer(s->{
					owner=RelayCircleTest.player(s);RelayCircleTest.prepare(owner,Runes.SHOCK);
					CampConcordNative.config(CampConcordNative.copy(original,Map.of("maxCreatures",1)));
					for(int x=-1;x<=4;x++)for(int z=5;z<=8;z++)owner.level().setBlock(new BlockPos(x,150,z),Blocks.WATER.defaultBlockState(),2);
					wet[0]=dummy(owner,.5,6.5);wet[1]=dummy(owner,2.5,6.5);
					BlockPos altar=new BlockPos(8,150,6);
					owner.level().setBlock(altar,dev.wildercord.content.dungeons.DungeonBlocks.ALTAR.defaultBlockState()
						.setValue(dev.wildercord.content.dungeons.DungeonAltarBlock.KIND,dev.wildercord.content.dungeons.DungeonAltarBlock.Kind.TIDE)
						.setValue(dev.wildercord.content.dungeons.DungeonAltarBlock.AWAKE,true),2);
					scribe[0]=TideScribe.rise(owner.level(),altar);RelayCircleTest.check(scribe[0]!=null,"Real Tide Scribe and altar create");
					scribe[0].setNoAi(true);scribe[0].forceTide(true);scribe[0].mechanic(owner.level(),owner.level().getGameTime());
					scribeHealth[0]=scribe[0].getHealth();
				});c.waitTicks(3);
				world.getServer().runOnServer(s->{
					RelayCircleTest.check(wet[0].isInWater()&&wet[1].isInWater(),"Real water membership reaches ordinary Shock/world conduction");
					RelayCircleTest.directDown(owner);
				});c.waitTicks(2);
				world.getServer().runOnServer(s->{RelayCircleTest.aim(owner,wet[0].getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
				world.getServer().runOnServer(s->{
					RelayCircleTest.check(wet[0].hitSequence()==1,"The one-creature budget allows the actual primary hit");
					RelayCircleTest.check(wet[1].hitSequence()==0,"Ordinary Shock reactions and world-water conduction cannot exceed the paid entity budget");
					RelayCircleTest.check(scribe[0].conductions()==1,"The actual Relay release also reaches the flooded Tide Scribe arena hook");
					RelayCircleTest.check(scribe[0].getHealth()==scribeHealth[0]&&!scribe[0].stranded(),"Exhausted entity budget blocks arena bonus damage and its later stranded-state write");
					scribe[0].forceTide(false);scribe[0].discard();wet[0].discard();wet[1].discard();CampConcordNative.config(original);
				});c.waitTicks(12);

				for(int blocks:List.of(32,1)){
				// A real Frost release reaches the arena ice mechanic while its creature allowance is already spent.
				world.getServer().runOnServer(s->{
					RelayCircleTest.prepare(owner,Runes.FROST);CampConcordNative.config(CampConcordNative.copy(original,Map.of("maxCreatures",1,"maxBlocks",blocks)));
					BlockPos altar=new BlockPos(8,150,6);
					owner.level().setBlock(altar,dev.wildercord.content.dungeons.DungeonBlocks.ALTAR.defaultBlockState().setValue(dev.wildercord.content.dungeons.DungeonAltarBlock.KIND,dev.wildercord.content.dungeons.DungeonAltarBlock.Kind.TIDE).setValue(dev.wildercord.content.dungeons.DungeonAltarBlock.AWAKE,true),2);
					scribe[0]=TideScribe.rise(owner.level(),altar);RelayCircleTest.check(scribe[0]!=null,"Native frost arena creates");scribe[0].setNoAi(true);scribe[0].setNoGravity(true);scribe[0].forceTide(true);scribe[0].mechanic(owner.level(),owner.level().getGameTime());scribe[0].snapTo(2.5,150,6.5,0,0);scribe[0].setDeltaMovement(Vec3.ZERO);
					for(int x=-1;x<=4;x++)for(int z=5;z<=8;z++)for(int y=150;y<=151;y++)owner.level().setBlock(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState(),2);
					wet[0]=dummy(owner,.5,6.5);
				});c.waitTicks(3);
				world.getServer().runOnServer(s->{RelayCircleTest.check(scribe[0].isInWater()&&!scribe[0].stranded()&&scribe[0].distanceTo(wet[0])<3.5,"Scribe is an otherwise eligible nearby Frost strand victim");RelayCircleTest.directDown(owner);});c.waitTicks(2);
				world.getServer().runOnServer(s->{RelayCircleTest.aim(owner,wet[0].getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
				world.getServer().runOnServer(s->{RelayCircleTest.check(wet[0].hitSequence()==1 && (blocks==1 || scribe[0].icedCells()>0),"The actual Relay Frost reaches primary damage and the admitted native arena ice mechanic"); int ice=0; for(var pos:BlockPos.betweenClosed(-4,150,2,6,154,11)) if(owner.level().getBlockState(pos).is(Blocks.ICE)||owner.level().getBlockState(pos).is(Blocks.FROSTED_ICE))ice++; RelayCircleTest.check(ice>0&&ice<=blocks,"World frost and arena frost share one paid block allowance");RelayCircleTest.check(!scribe[0].stranded(),"Arena Frost cannot spend a second creature after the paid primary exhausted its allowance");scribe[0].forceTide(false);scribe[0].discard();wet[0].discard();CampConcordNative.config(original);});c.waitTicks(12);

				}

				for(boolean retire:List.of(false,true)){
					world.getServer().runOnServer(s->{
						RelayCircleTest.prepare(owner,Runes.FROST);
						owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
						owner.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("starlit",AuraRules.FLOW,150,100,0));
						watched=dummy(owner,.5,6.5);retireBonus=retire;bonusAttempts=0;
						float blade=AuraCombat.projected(owner,watched,4,1,true);
						RelayCircleTest.check(blade>0,"A real native projected blade wound primes the normal resonance ledger");
						RelayCircleTest.directDown(owner);
					});c.waitTicks(2);
					world.getServer().runOnServer(s->{RelayCircleTest.aim(owner,watched.getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
					world.getServer().runOnServer(s->{
						RelayCircleTest.check(bonusAttempts==1,"The actual resonance bonus keeps RelayDamageSource and the original paid receipt");
						RelayCircleTest.check(watched.hitSequence()==(retire?2:3),"Pre-damage callback can reject only the resonance bonus while retaining the earlier actual blade and frost hits");
						if(retire)RelayCircleTest.check(!watched.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)&&watched.getTicksFrozen()==0,"Retired resonance cannot resume utility or Frost follow-up writes");
						watched.discard();watched=null;retireBonus=false;
					});c.waitTicks(12);
				}

				List<TrainingDummy> crowd=new ArrayList<>();
				for(int count:List.of(64,65)){
					world.getServer().runOnServer(s->{RelayCircleTest.prepare(owner,Runes.HARM);owner.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);for(int i=0;i<count;i++)crowd.add(dummy(owner,.5,6.5));RelayCircleTest.directDown(owner);});c.waitTicks(2);
					world.getServer().runOnServer(s->{RelayCircleTest.aim(owner,crowd.getFirst().getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
					world.getServer().runOnServer(s->{
						long hits=crowd.stream().mapToLong(TrainingDummy::hitSequence).sum();
						RelayCircleTest.check(hits==(count==64?1:0),"Candidate-bound control hits one nearest body; overflow refuses the entire paid ray");
						if(count==64)RelayCircleTest.check(crowd.getFirst().hitSequence()==1,"Equal-distance primary selection has deterministic entity-id order");
						crowd.forEach(LivingEntity::discard);crowd.clear();
					});c.waitTicks(12);
				}

				LivingEntity[] pet=new LivingEntity[1];TrainingDummy[] behind=new TrainingDummy[1];
				world.getServer().runOnServer(s->{
					RelayCircleTest.prepare(owner,Runes.HARM);behind[0]=dummy(owner,.5,6.5);
					var wolf=EntityTypes.WOLF.create(owner.level(),EntitySpawnReason.MOB_SUMMONED);RelayCircleTest.check(wolf!=null,"Protected physical blocker creates");wolf.tame(owner);wolf.setNoAi(true);wolf.setNoGravity(true);wolf.snapTo(.5,150,4.5,0,0);owner.level().addFreshEntity(wolf);pet[0]=wolf;
					RelayCircleTest.directDown(owner);owner.teleportTo(owner.level(),-1.5,150,.5,Set.of(),0,0,false);
				});c.waitTicks(2);
				world.getServer().runOnServer(s->{RelayCircleTest.aim(owner,behind[0].getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
				world.getServer().runOnServer(s->RelayCircleTest.check(behind[0].hitSequence()==0&&pet[0].getHealth()==pet[0].getMaxHealth(),"Protected nearest focus-lane body stops the unpiercing ray without receiving harm"));
			} finally {world.getServer().runOnServer(s->CampConcordNative.config(original));}
		} finally {owner=null;watched=null;retireBonus=false;}
	}
	private static TrainingDummy dummy(ServerPlayer player,double x,double z){
		var dummy=WildercordEntities.TRAINING_DUMMY.create(player.level(),EntitySpawnReason.MOB_SUMMONED);RelayCircleTest.check(dummy!=null,"Native dummy creates");dummy.snapTo(x,150,z,180,0);dummy.setNoGravity(true);player.level().addFreshEntity(dummy);return dummy;
	}
}
