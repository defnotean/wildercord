package dev.wildercord.cast;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.*;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.content.*;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Three cooperation systems together, actual respawn packets and a saved-world server restart. */
public final class AuraIntegrationTest implements FabricClientGameTest {
	private static UnityRules.State expected;
	private static long etchedReady;
	private static UUID bond;
	@Override public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try(var w=c.worldBuilder().create()) {
			c.waitTicks(35);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("gamerule natural_health_regeneration false");
			w.getServer().runCommand("gamerule fall_damage false");
			w.getServer().runCommand("time set 6000");
			w.getServer().runOnServer(s -> {
				for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)s.overworld().setBlock(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState(),2);
				var p=p(s);prepare(p);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
				p.teleportTo(s.overworld(),.5,101,-3,Set.<Relative>of(),0,0,false);
				check(AuraApi.bondBlade(p,InteractionHand.MAIN_HAND,"integration"),"Native bond accepts the swordsman");
				p.getMainHandItem().set(RuneEtchings.RUNE,Runes.FIRE.id());
				bond=BondedBlades.bond(p.getMainHandItem()).id();
				check(Unity.begin(p),"Unity opens for the bonded hybrid");
				var foe=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);
				foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);foe.setHealth(200);
				foe.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);foe.setNoAi(true);
				foe.snapTo(.5,101,1,180,0);s.overworld().addFreshEntity(foe);
				int count=ResonantStrikes.reactions();
				var node=SpellCompiler.compile(List.of(Runes.TOUCH,Runes.FROST)).root().groups.getFirst().effects.getFirst();
				Effects.apply(new Cast(p),node,new Cast.Hit(List.of(foe),foe.position(),p.getLookAngle(),p.position(),null,null,false));
				float mana=Spellbooks.mana(p),health=foe.getHealth();
				float art=ArtKit.hits(p,AuraFx.art(p)).raw(foe,4,null);
				check(art>0 && health-foe.getHealth()>art,"The art, resonance and inscription cause real additional damage");
				check(ResonantStrikes.reactions()==count+1,"A rune-etched art answers the opening exactly once");
				check(Math.abs(Unity.state(p).manaReturned()-2)<.01,"Only the four-Aura resonance payment returns two mana");
				check(Math.abs(Unity.state(p).auraReturned()-RuneEtchingRules.price(Runes.FIRE)*.25)<.01,"Actual inscription price feeds the shared Aura allowance");
				check(Math.abs(Spellbooks.mana(p)-(mana-RuneEtchingRules.price(Runes.FIRE)+2))<.01,"Combined payments settle exactly once");
				check(p.getAttachedOrElse(RuneEtchings.READY,0L)>Unity.now(p),"The inscription keeps its independent rest");
				float after=Spellbooks.mana(p);ArtKit.hits(p,AuraFx.art(p)).raw(foe,4,null);
				check(ResonantStrikes.reactions()==count+1 && Spellbooks.mana(p)==after,"Repeated art hits cannot repeat either paid bonus during rest");
			});
			c.waitTicks(3);shot(c,"aura_integrated_strike");
			// Both gamerules use real death and the client's respawn request, not a codec-only copy.
			for(int round=0;round<2;round++) {
				w.getServer().runCommand("gamerule keep_inventory "+(round==1));
				w.getServer().runOnServer(s -> {
					var p=p(s);prepare(p);p.setAttached(Unity.STATE,UnityRules.State.NONE);
					check(Unity.begin(p),"Lifecycle fixture opens a real activation");
					Spellbooks.setMana(p,Spellbooks.mana(p)-8);Unity.manaSpent(p,8);Aura.spend(p,6,"integration_paid_art");
					expected=Unity.state(p);etchedReady=Unity.now(p)+1200;p.setAttached(RuneEtchings.READY,etchedReady);
					p.kill(p.level());check(!p.isAlive(),"Native death occurs");
				});
				c.waitTicks(5);
				c.runOnClient(mc -> {mc.player.respawn();mc.gui.setScreen(null);});c.waitTicks(15);
				w.getServer().runOnServer(s -> {
					var p=p(s);check(p.isAlive(),"Native respawn produces a living body");
					check(Unity.state(p).equals(expected.stop()),"Respawn ends the window and preserves exact rest and consumed budgets");
					check(p.getAttachedOrElse(RuneEtchings.READY,0L)==etchedReady,"Inscription rest survives actual death");
					checkBlade(p);prepare(p);check(!Unity.begin(p),"Death cannot buy immediate Unity with restored resources");
					check(!RuneEtchings.wake(p,foe(s),4),"Death cannot buy an immediate etched effect");
					p.teleportTo(s.overworld(),.5,101,-3,Set.<Relative>of(),0,0,false);
				});
				c.waitTicks(3);
				check(c.computeOnClient(mc -> Unity.state(mc.player).equals(expected.stop())),"Owner client sees preserved state after respawn");
				check(c.computeOnClient(mc -> Runes.FIRE.id().equals(mc.player.getMainHandItem().get(RuneEtchings.RUNE))),"Client sees its preserved inscription");
			}
			c.runOnClient(mc -> mc.gui.setScreen(new AuraScreen(null)));c.waitTicks(3);shot(c,"aura_after_respawn");c.runOnClient(mc -> mc.gui.setScreen(null));
			w.getServer().runOnServer(s -> {
				var p=p(s);prepare(p);p.setAttached(Unity.STATE,UnityRules.State.NONE);check(Unity.begin(p),"Disconnect fixture opens a fresh activation");
				Spellbooks.setMana(p,Spellbooks.mana(p)-8);Unity.manaSpent(p,8);Aura.spend(p,6,"integration_paid_art");expected=Unity.state(p);
				etchedReady=Unity.now(p)+1200;p.setAttached(RuneEtchings.READY,etchedReady);
			});
			save=w.getWorldSave();
		}
		// close() disconnects and shuts down the old integrated server; open() loads the saved world into another server.
		try(var w=save.open()) {
			c.waitTicks(20);
			w.getServer().runOnServer(s -> {
				var p=p(s);check(Unity.state(p).equals(expected.stop()),"Saved world restart retains stopped window, rest and delivered budgets: actual="+Unity.state(p)+", expected="+expected.stop());
				checkBlade(p);check(p.getAttachedOrElse(RuneEtchings.READY,0L)==etchedReady,"Saved world restart retains inscription deadline");
				prepare(p);check(!Unity.begin(p),"Reconnect cannot clear Unity rest");check(!RuneEtchings.wake(p,foe(s),4),"Reconnect cannot clear inscription rest");
			});
			check(c.computeOnClient(mc -> Unity.state(mc.player).equals(expected.stop())),"Reconnected client receives exact persisted state");
			c.runOnClient(mc -> mc.gui.setScreen(new AuraScreen(null)));c.waitTicks(3);shot(c,"aura_after_restart");
		}
	}
	private static void prepare(ServerPlayer p) {
		p.setGameMode(GameType.SURVIVAL);p.setHealth(p.getMaxHealth());p.removeAllEffects();
		p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("gale",AuraRules.FORM,1800,110,0));
		p.setAttached(WildercordAttachments.INNATE,Runes.get("wildercord:kindling").orElseThrow().id());
		p.setAttached(WildercordAttachments.CIRCLES,5);p.removeAttached(Awakening.AWAKENING);
		Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));Spellbooks.setMana(p,100);
	}
	private static void checkBlade(ServerPlayer p) {
		var blade=p.getMainHandItem();var held=BondedBlades.bond(blade);
		check(held!=null && held.id().equals(bond) && held.ownedBy(p.getUUID()),"The same owned bond returns to the main hand");
		check(Runes.FIRE.id().equals(blade.get(RuneEtchings.RUNE)),"The exact inscription survives with its blade");
		int copies=0;for(int i=0;i<p.getInventory().getContainerSize();i++) {var b=BondedBlades.bond(p.getInventory().getItem(i));if(b!=null && b.id().equals(bond))copies++;}
		check(copies==1,"Exactly one blade is present after the lifecycle transition");
	}
	private static LivingEntity foe(MinecraftServer s) {return s.overworld().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(-10,100,-10,10,105,10),e -> e.getType()==EntityTypes.HUSK && e.isAlive()).getFirst();}
	private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
	private static void check(boolean yes,String why) {if(!yes)throw new AssertionError(why);}
	private static void shot(ClientGameTestContext c,String name) {if(c.computeOnClient(mc -> mc.gui.screen()!=null)){c.getInput().setCursorPos(0,0);c.waitTicks(1);}c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
}
