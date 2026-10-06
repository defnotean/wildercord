package dev.wildercord.cast;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/** Caster and focus are on opposite sides: actual guard direction and normal defensive counter both matter. */
public final class RelayDefenceTest implements FabricClientGameTest {
	private static final class Defender extends FakePlayer {
		Defender(ServerLevel level) {super(level,new GameProfile(UUID.randomUUID(),"RelayGuard"));}
		@Override public boolean isInvulnerableTo(ServerLevel level,DamageSource source){return false;}
	}
	private ServerPlayer owner;
	private Defender defender;
	private Throwable failure;
	private static boolean observing;
	private static ServerPlayer watchedOwner;
	private static int blocked;
	private static boolean cancelSecond;
	private static Defender mirrorTarget;
	private static int mirrorMode, mirrorAdmissions, fragments;
	private static Vec3 fragmentOrigin;
	private static Defender reprieveTarget;
	private static boolean retireDebt;

	@Override public void runTest(ClientGameTestContext c){
		if(!observing){observing=true;
			net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount)->{
				if(source instanceof RelayDamageSource && target==mirrorTarget){
					RelayCircleTest.check(ArmorResponses.mirrorReady(mirrorTarget),"Timed native mantle fixture is active before the actual Relay damage");mirrorAdmissions++;
					if(mirrorMode==2)dev.wildercord.player.Spellbooks.setCord(watchedOwner,new ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
				}else if(source instanceof RelayDamageSource && target==watchedOwner && mirrorTarget!=null){fragments++;fragmentOrigin=source.getSourcePosition();}
				return true;
			});
			net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,taken,blockedDamage)->{
				if(target==reprieveTarget && source instanceof RelayDamageSource && retireDebt)
					dev.wildercord.player.Spellbooks.setCord(watchedOwner,new ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
				if(target==mirrorTarget && source instanceof RelayDamageSource && mirrorMode==3)
					dev.wildercord.player.Spellbooks.setCord(watchedOwner,new ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
			});
			dev.wildercord.api.WildercordEvents.SPELL_BLOCKED.register((caster,target,cost,strength)->{
			if(cancelSecond && (caster==watchedOwner || target==watchedOwner)) {
				blocked++;
				if(blocked==2)dev.wildercord.player.Spellbooks.setCord(watchedOwner,new ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
			}
		});}
		try(var world=c.worldBuilder().create()){
			c.waitTicks(40);world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(s->{
				owner=RelayCircleTest.player(s);RelayCircleTest.prepare(owner,Runes.HARM);owner.level().getGameRules().set(GameRules.PVP,true,s);
				defender=new Defender(owner.level());defender.setGameMode(GameType.SURVIVAL);defender.setNoGravity(true);owner.level().addNewPlayer(defender);
			});c.waitTicks(3);
			for(int mode=0;mode<4;mode++){
				int facing=mode;failure=null;blocked=0;cancelSecond=facing==3;watchedOwner=owner;
				world.getServer().runOnServer(s->{
					owner.setHealth(owner.getMaxHealth());owner.removeAllEffects();Effects.readyToHurt(owner);
					defender.setHealth(defender.getMaxHealth());defender.removeAllEffects();Effects.readyToHurt(defender);
					defender.removeAttached(dev.wildercord.player.WildercordAttachments.SPELL_SHIELD);
					defender.snapTo(.5,150,6.5,facing==1?0:180,0);defender.setShiftKeyDown(true);
					defender.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
					defender.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("starlit",AuraRules.FLOW,150,100,0));
					defender.setAttached(AuraAttachments.STATE,AuraAttachments.State.NONE);
					RelayCircleTest.reset(owner,Runes.HARM);RelayCircleTest.directDown(owner);
					RelayCircleTest.check(RelayCircles.pending(owner),"Directional fixture really places a paid focus");
					owner.teleportTo(owner.level(),-3.5,150,8.5,Set.of(),0,0,false);
				});c.waitTicks(2);
				world.getServer().runOnServer(s->{
					RelayCircleTest.aim(owner,defender.getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);
					RelayCircleTest.check(owner.getAttached(RelayState.VIEW).phase()==RelayState.WARNING,"Opposite-side caster retains two clear bounded legs");
					Scheduler.later(4,()->{
						try {
							if(facing>=2){
								Shields.raise(new Cast(defender).weigh(60),defender,80);
								if(facing==3)Shields.raise(new Cast(owner).weigh(60),owner,80);
							}else RelayCircleTest.check(AuraGuard.raise(defender),"Defender pays to raise a real guard immediately before impact");
						}catch(RuntimeException|AssertionError e){failure=e;}
					});
				});c.waitTicks(8);
				world.getServer().runOnServer(s->{
					if(failure!=null)throw new AssertionError("Guard setup failed",failure);
					if(facing==1){
						RelayCircleTest.check(defender.getHealth()<defender.getMaxHealth(),"Facing the caster with the focus behind does not stop the ray");
						RelayCircleTest.check(AuraGuard.perfectNow(defender),"A rear Relay does not consume the unused perfect guard");
						RelayCircleTest.check(owner.getHealth()==owner.getMaxHealth(),"Rear arrival creates no false defensive counter");
					}else if(facing==3){
						RelayCircleTest.check(blocked==2,"Both real defensive parries execute before the second callback retires the original focus");
						RelayCircleTest.check(owner.getHealth()==owner.getMaxHealth() && defender.getHealth()==defender.getMaxHealth(),"No third counter survives cancellation through a second reflected Cast");
						RelayCircleTest.check(!RelayCircles.pending(owner),"Second-parry callback retires the original paid focus permanently");
					}else{
						RelayCircleTest.check(defender.getHealth()==defender.getMaxHealth(),"Facing the real focus or raising Shield stops Relay");
						RelayCircleTest.check(owner.getHealth()<owner.getMaxHealth(),"Ordinary defensive counter reaches the exposed original caster");
					}
				});c.waitTicks(12);
			}
			cancelSecond=false;
			var original=world.getServer().computeOnServer(server->dev.wildercord.config.Config.get());
			try {
				for(int mode=0;mode<4;mode++){
					mirrorMode=mode;mirrorAdmissions=0;fragments=0;fragmentOrigin=null;failure=null;
					world.getServer().runOnServer(server->{
						dev.wildercord.cast.CampConcordNative.config(dev.wildercord.cast.CampConcordNative.copy(original,java.util.Map.of("maxCreatures",mirrorMode==1?1:64)));
						RelayCircleTest.prepare(owner,Runes.HARM);Effects.readyToHurt(owner);owner.removeAllEffects();owner.removeAttached(dev.wildercord.player.WildercordAttachments.SPELL_SHIELD);
						defender.snapTo(.5,150,6.5,180,0);defender.setHealth(defender.getMaxHealth());Effects.readyToHurt(defender);defender.removeAllEffects();defender.setShiftKeyDown(false);
						defender.removeAttached(dev.wildercord.player.WildercordAttachments.SPELL_SHIELD);defender.removeAttached(AuraAttachments.STATE);defender.removeAttached(AuraAttachments.AURA);defender.removeAttached(ArmorResponses.STATE);
						defender.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
						defender.setItemSlot(EquipmentSlot.CHEST,new ItemStack(dev.wildercord.gear.ElementalArmor.ALL.stream().filter(a->a.kind==dev.wildercord.gear.ElementalArmor.Kind.MIRROR_THREAD).findFirst().orElseThrow()));
						mirrorTarget=defender;RelayCircleTest.directDown(owner);
					});c.waitTicks(2);
					world.getServer().runOnServer(server->{RelayCircleTest.aim(owner,defender.getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);Scheduler.later(4,()->defender.setAttached(ArmorResponses.STATE,new ArmorResponses.State(0,12,0,100,true)));});c.waitTicks(8);
					world.getServer().runOnServer(server->{
						RelayCircleTest.check(mirrorAdmissions==1,"Native damage genuinely reaches the mantle branch");
						RelayCircleTest.check(fragments==(mirrorMode==0?1:0),"Mirror-thread retains payment, shared creature cap and original lifetime after ALLOW/AFTER_DAMAGE callbacks");
						RelayCircleTest.check((owner.getHealth()<owner.getMaxHealth())==(mirrorMode==0),"Only live, budgeted mantle retaliation wounds the original body");
						if(mirrorMode==0)RelayCircleTest.check(fragmentOrigin.distanceToSqr(defender.getEyePosition())<.0001,"Native mantle retaliation reports defender as actual incoming direction");
						mirrorTarget=null;
					});c.waitTicks(12);
				}
				for(boolean cancel:java.util.List.of(false,true)){
					retireDebt=cancel;
					world.getServer().runOnServer(server->{
						dev.wildercord.cast.CampConcordNative.config(original);RelayCircleTest.prepare(owner,Runes.HARM);
						defender.snapTo(.5,150,6.5,180,0);defender.setHealth(defender.getMaxHealth());Effects.readyToHurt(defender);defender.removeAllEffects();
						defender.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);defender.removeAttached(ArmorResponses.STATE);defender.removeAttached(DefensiveFoci.STATE);
						dev.wildercord.gear.GearSlots.set(defender,dev.wildercord.gear.GearSlot.FOCUS,new ItemStack(dev.wildercord.gear.GearItems.get(dev.wildercord.gear.GearDef.REPRIEVE)));
						reprieveTarget=defender;RelayCircleTest.directDown(owner);
					});c.waitTicks(2);
					world.getServer().runOnServer(server->{RelayCircleTest.aim(owner,defender.getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});c.waitTicks(8);
					world.getServer().runOnServer(server->{
						float owed=defender.getAttachedOrElse(DefensiveFoci.STATE,DefensiveFoci.State.EMPTY).owed();
						RelayCircleTest.check(defender.getHealth()<defender.getMaxHealth(),"Actual native Reprieve branch accepts the immediate partial wound");
						RelayCircleTest.check(!RelayCircles.pending(owner),"Original focus has retired before checking accepted debt");
						RelayCircleTest.check(cancel?owed==0:owed>0,"Callback retirement prevents new debt; a debt accepted before normal focus retirement remains an already-admitted wound");
						reprieveTarget=null;dev.wildercord.gear.GearSlots.clear(defender,dev.wildercord.gear.GearSlot.FOCUS);
					});c.waitTicks(12);
				}
			}finally{world.getServer().runOnServer(server->dev.wildercord.cast.CampConcordNative.config(original));}
		} finally {cancelSecond=false;watchedOwner=null;mirrorTarget=null;reprieveTarget=null;}
	}
}
