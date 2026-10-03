package dev.wildercord.cast;

import dev.wildercord.aura.*;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real spell damage, coated weapon attacks and projected arts; ten presentations, shields and cooperative permissions. */
public final class ResonantStrikesTest implements FabricClientGameTest {
	private static ServerPlayer ally;
	private static Cast continuing;
	@Override public void runTest(ClientGameTestContext c) {
		try(var w=c.worldBuilder().create()) {
			c.waitTicks(40);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");w.getServer().runCommand("gamerule fall_damage false");w.getServer().runCommand("time set 6000");
			w.getServer().runOnServer(s -> {
				for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++)s.overworld().setBlock(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState(),2);
				var p=p(s);teach(p,"gale");p.teleportTo(s.overworld(),.5,101,-3,Set.<Relative>of(),0,0,false);
				ally=new FakePlayer(s.overworld(),new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("resonant-ally".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"ResonantAlly")){
					@Override public net.minecraft.world.scores.PlayerTeam getTeam(){return level().getScoreboard().getPlayersTeam(getScoreboardName());}
					@Override public boolean isInvulnerableTo(ServerLevel level,net.minecraft.world.damagesource.DamageSource source){return false;}
				};
				teach(ally,"stone");ally.snapTo(3,101,-6,0,0);p.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(ally)));s.overworld().addNewPlayer(ally);
			});c.waitTicks(15);c.runOnClient(mc -> {mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(2);mc.resizeGui();});
			// All ten spell families meet a complementary blade. The callback records only landed health damage.
			for(int i=0;i<ResonantRules.ELEMENTS.size();i++) {
				c.waitTicks(82);String element=ResonantRules.ELEMENTS.get(i);
				w.getServer().runOnServer(s -> {
					var p=p(s);teach(p,partner(element));var foe=foe(s,0,3);int count=ResonantStrikes.reactions();var cast=new Cast(p);
					spell(cast,foe,element,8);float before=foe.getHealth(),energy=Aura.aura(p);
					float original=AuraCombat.projected(p,foe,8,true);
					check(ResonantStrikes.reactions()==count+1,"Actual "+element+" damage and projected blade trigger once");
					check(before-foe.getHealth()<=original+3.01,"Reaction adds no more than three health damage");check(Aura.aura(p)<energy,"Reaction pays Aura");
					check(Heart.discovered(p,"aura:resonant_strike"),"Actual resonance records discovery");
					continuing=cast;
				});c.waitTicks(4);shot(c,"resonant_"+element);c.waitTicks(10);w.getServer().runOnServer(s -> {var f=target(s);int n=ResonantStrikes.reactions();for(int j=0;j<6;j++){spell(continuing,f,element,1);AuraCombat.projected(p(s),f,1,true);}check(ResonantStrikes.reactions()==n,"Rapid spell/art hits cannot retrigger "+element);clear(s);});
			}
			// The native effect dispatcher participates, with blade first as well as spell first.
			c.waitTicks(82);w.getServer().runOnServer(s -> {
				var p=p(s);teach(p,"ember");var f=foe(s,0,2);int n=ResonantStrikes.reactions();AuraCombat.projected(p,f,5,true);
				var node=SpellCompiler.compile(List.of(Runes.BEAM,Runes.FROST)).root().groups.getFirst().effects.getFirst();
				Effects.apply(new Cast(p),node,new Cast.Hit(List.of(f),f.position(),new Vec3(0,0,1),p.position(),null,null,false));
				check(ResonantStrikes.reactions()==n+1,"Native Frost effect answers a blade already landed");
			});c.waitTicks(12);w.getServer().runOnServer(s -> clear(s));
			c.waitTicks(82);w.getServer().runOnServer(s -> {
				var p=p(s);teach(p,"gale");var f=foe(s,0,2);int n=ResonantStrikes.reactions();spell(new Cast(p),f,"fire",5);
				AuraCombat.swing(p);p.attack(f);check(ResonantStrikes.reactions()==n+1,"A real ordinary fully charged coated weapon attack resonates");
			});c.waitTicks(12);w.getServer().runOnServer(s -> clear(s));
			// Failed spell damage, dead targets, unsupported elements and unpaid Aura cannot prime/trigger.
			c.waitTicks(82);w.getServer().runOnServer(s -> {
				var p=p(s);teach(p,"gale");int n=ResonantStrikes.reactions();var immune=foe(s,0,2);immune.setPermanentlyInvulnerable(true);spell(new Cast(p),immune,"fire",5);immune.setPermanentlyInvulnerable(false);AuraCombat.projected(p,immune,4,true);check(ResonantStrikes.reactions()==n,"Immune spell cannot leave an opening");
				var shielded=foe(s,4,2);Shields.give(shielded,24,200,List.of());var shieldCast=new Cast(p);spell(shieldCast,shielded,"fire",5);check(Shields.blocked(shieldCast,shielded),"Fixture shield actually stops the spell");AuraCombat.projected(p,shielded,4,true);check(ResonantStrikes.reactions()==n,"A stopped spell cannot leave an opening");
				var unknown=foe(s,-4,2);spell(new Cast(p),unknown,"unknown",5);AuraCombat.projected(p,unknown,4,true);check(ResonantStrikes.reactions()==n,"Unknown element remains dormant");
				var unpaid=foe(s,0,5);spell(new Cast(p),unpaid,"fire",5);p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("gale",AuraRules.GLOW,0,0,0));AuraCombat.projected(p,unpaid,4,true);check(ResonantStrikes.reactions()==n,"Insufficient Aura cannot pay a reaction");
			});c.waitTicks(12);w.getServer().runOnServer(s -> clear(s));
			// A long-lived paid cast cannot be milked once the rest expires.
			c.waitTicks(82);w.getServer().runOnServer(s -> {var p=p(s);teach(p,"gale");var f=foe(s,0,2);continuing=new Cast(p);spell(continuing,f,"fire",5);AuraCombat.projected(p,f,4,true);});
			c.waitTicks(82);w.getServer().runOnServer(s -> {var f=target(s);int n=ResonantStrikes.reactions();spell(continuing,f,"fire",5);AuraCombat.projected(p(s),f,4,true);check(ResonantStrikes.reactions()==n,"A repeated pulse of the same payment cannot prime the same target twice");});c.waitTicks(10);w.getServer().runOnServer(s -> clear(s));
			// Delayed spell work keeps its element while the thread's outer context has returned.
			c.waitTicks(82);int delayed=w.getServer().computeOnServer(s -> {var p=p(s);teach(p,"gale");var f=foe(s,0,2);var cast=new Cast(p);AuraCombat.projected(p,f,4,true);int n=ResonantStrikes.reactions();Effects.asElement("fire",() -> Scheduler.later(4,() -> Effects.hurt(cast,f,p.level().damageSources().magic(),5)));return n;});c.waitTicks(8);check(w.getServer().computeOnServer(s -> ResonantStrikes.reactions()==delayed+1),"Scheduled spell damage keeps its elemental context");w.getServer().runOnServer(s -> clear(s));
			// Distinct players must be allies. A failed hostile attempt cannot steal the opening.
			c.waitTicks(82);w.getServer().runOnServer(s -> {
				var p=p(s);teach(p,"gale");teach(ally,"stone");var f=foe(s,0,2);int n=ResonantStrikes.reactions();spell(new Cast(p),f,"storm",5);AuraCombat.projected(ally,f,4,true);check(ResonantStrikes.reactions()==n,"Unallied players cannot cooperate for resonance");
				var team=s.getScoreboard().addPlayerTeam("resonant_test");team.setAllowFriendlyFire(false);s.getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);s.getScoreboard().addPlayerToTeam(ally.getScoreboardName(),team);
				check(s.overworld().players().contains(ally),"Cooperative swordsman is present in the level");check(WayBanner.ally(ally,p),"Scoreboard team makes the partners allies");check(Targets.canHarm(p,f) && Targets.canHarm(ally,f),"Both partners can harm the foe");check(Aura.enabled(ally) && Aura.holdsWeapon(ally) && !Awakening.spent(ally),"Partner can use Aura");
				float energy=Aura.aura(ally);float hit=AuraCombat.projected(ally,f,4,true);check(hit>0,"Partner actually lands projected damage");check(ResonantStrikes.reactions()==n+1,"Allied mage and swordsman react; count="+ResonantStrikes.reactions()+" expected="+(n+1));check(Aura.aura(ally)<energy,"Only the swordsman pays the reaction");
				check(Heart.discovered(ally,"aura:resonant_strike"),"Partner discovery is recorded");
			});c.waitTicks(4);shot(c,"resonant_cooperation");c.waitTicks(10);w.getServer().runOnServer(s -> clear(s));
			// First-person view remains open; a new target cannot bypass the swordsman's global rest.
			w.getServer().runOnServer(s -> {var p=p(s);teach(p,"gale");var f=foe(s,0,2);int n=ResonantStrikes.reactions();spell(new Cast(p),f,"fire",4);AuraCombat.projected(ally,f,4,true);check(ResonantStrikes.reactions()==n,"Switching foes cannot reset the swordsman's rest");});
			c.waitTicks(82);w.getServer().runOnServer(s -> {clear(s);var p=p(s);teach(p,"gale");var f=foe(s,0,2);spell(new Cast(p),f,"fire",5);AuraCombat.projected(p,f,4,true);});c.waitTicks(3);shot(c,"resonant_first_person");
			c.waitTicks(82);w.getServer().runOnServer(s -> {
				clear(s);var p=p(s);teach(p,"gale");p.setHealth(15);var dummy=WildercordEntities.TRAINING_DUMMY.create(s.overworld(),EntitySpawnReason.COMMAND);dummy.snapTo(.5,101,2,180,0);dummy.addTag("resonant_foe");s.overworld().addFreshEntity(dummy);int n=ResonantStrikes.reactions();
				spell(new Cast(p),dummy,"life",5);check(dummy.lastDamage()>0 && dummy.getHealth()==dummy.getMaxHealth(),"Dummy retains measured damage while restoring health");float hit=AuraCombat.projected(p,dummy,4,true);
				check(hit>0 && ResonantStrikes.reactions()==n+1,"Real self-restoring dummy lets a player practice resonance");check(p.getHealth()==15,"Life resonance cannot farm dummy healing");
				dummy.setPermanentlyInvulnerable(true);dummy.hurtServer(s.overworld(),s.overworld().damageSources().magic(),5);check(dummy.lastDamage()==0,"Rejected dummy hit clears its prior damage sample");
			});
			c.waitTicks(82);w.getServer().runOnServer(s -> {
				clear(s);var p=p(s);teach(p,"gale");teach(ally,"stone");s.getScoreboard().removePlayerFromTeam(ally.getScoreboardName());ally.damageCooldownTime=0;
				int n=ResonantStrikes.reactions();spell(new Cast(p),ally,"fire",2);float before=ally.getHealth();float original=AuraCombat.projected(p,ally,2,true);
				check(ResonantStrikes.reactions()==n+1,"Actual competitive player receives a resonant strike");check(before-ally.getHealth()<=original+.751F,"Player bonus remains below three quarters of a health point through defenses");
			});
		}
	}
	private static String partner(String e) {return switch(e){case "fire"->"gale";case "frost"->"ember";case "storm"->"stone";case "wind","arcane"->"hourglass";case "earth"->"rime";case "life"->"crimson";case "void"->"starlit";case "time"->"gale";default->"hollow";};}
	private static ServerPlayer p(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
	private static void teach(ServerPlayer p,String method){p.setGameMode(GameType.SURVIVAL);p.setHealth(p.getMaxHealth());p.removeAllEffects();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data(method,AuraRules.GLOW,0,100,0));p.removeAttached(AuraAttachments.STATE);}
	private static LivingEntity foe(MinecraftServer s,double x,double z){var f=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);f.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);f.setHealth(200);f.setNoAi(true);f.addTag("resonant_foe");f.snapTo(x+.5,101,z,180,0);s.overworld().addFreshEntity(f);return f;}
	private static LivingEntity target(MinecraftServer s){for(Entity e:s.overworld().getAllEntities())if(e instanceof LivingEntity f && e.entityTags().contains("resonant_foe"))return f;throw new AssertionError("Test foe missing");}
	private static void clear(MinecraftServer s){var copy=new ArrayList<Entity>();s.overworld().getAllEntities().forEach(copy::add);copy.stream().filter(e->e.entityTags().contains("resonant_foe")).forEach(Entity::discard);}
	private static void spell(Cast cast,LivingEntity f,String element,float amount){Effects.asElement(element,() -> Effects.hurt(cast,f,cast.level.damageSources().source(net.minecraft.world.damagesource.DamageTypes.MAGIC,cast.caster,cast.caster),amount));}
	private static void shot(ClientGameTestContext c,String n){c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(n).disableCounterPrefix());}
	private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
}
