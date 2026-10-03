package dev.wildercord.aura;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.JsonOps;
import dev.wildercord.cast.*;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.content.*;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Native GUI/packet activation, real mana and Aura payments, denied synthetic income and persistent budgets. */
public final class UnityTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext c) {
		try(var w=c.worldBuilder().create()) {
			c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");w.getServer().runCommand("time set 6000");
			w.getServer().runOnServer(s -> {
				for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)s.overworld().setBlock(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState(),2);
				var p=p(s);setup(p);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);
				p.setAttached(WildercordAttachments.CIRCLES,4);check(!Unity.begin(p),"Four circles cannot activate");
				check(Spellbooks.mana(p)==100 && Aura.aura(p)==110,"Refusal pays nothing");
				p.setAttached(WildercordAttachments.CIRCLES,5);Spellbooks.setMana(p,5);check(!Unity.begin(p),"Both prices required");
				check(Aura.aura(p)==110,"No partial Aura payment");Spellbooks.setMana(p,100);
				check(Unity.begin(p),"Eligible paths activate");check(Spellbooks.mana(p)==88 && Aura.aura(p)==98,"Both activation prices paid exactly");
				check(Unity.state(p).manaReturned()==0 && Unity.state(p).auraReturned()==0,"Activation cannot refund itself");
				Unity.stop(p);check(!Unity.begin(p),"Early end keeps rest");p.setAttached(Unity.STATE,UnityRules.State.NONE);
				setup(p);
			});
			c.runOnClient(mc -> mc.gui.setScreen(new AuraScreen(null)));c.waitTicks(5);shot(c,"unity_ready_page");
			var point=c.computeOnClient(mc -> ((AuraScreen)mc.gui.screen()).unityPoint());
			double scale=c.computeOnClient(mc -> mc.getWindow().getGuiScale());
			c.getInput().setCursorPos(point[0]*scale,point[1]*scale);c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);c.waitTicks(2);
			w.getServer().runOnServer(s -> {check(Unity.active(p(s)),"Actual page mouse click reaches activation packet");check(Unity.state(p(s)).manaReturned()==0,"Packet activation cannot self-refund");});
			c.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));c.waitTicks(1);shot(c,"unity_shared_breath");
			c.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));c.waitTicks(1);shot(c,"unity_first_person");
			w.getServer().runOnServer(s -> {
				var p=p(s);p.setAttached(AuraAttachments.AURA,Aura.data(p).withAura(60));Spellbooks.setMana(p,100);
				spell(p,List.of(Runes.SELF,Runes.SWIFT));float before=Spellbooks.mana(p),aura=Aura.aura(p);
				SpellCaster.cast(p,0);float paid=before-Spellbooks.mana(p);
				check(paid>0 && Math.abs(Aura.aura(p)-aura-paid*.25)<.01,"Real Cord mana payment feeds Aura");
				float gained=Unity.state(p).auraReturned();float mana=Spellbooks.mana(p);
				Aura.spend(p,20,"slash");check(Math.abs(Spellbooks.mana(p)-mana-10)<.01,"Real Aura spend feeds mana");
				check(Unity.state(p).auraReturned()==gained,"Mana restoration cannot return more Aura recursively");
				dev.wildercord.api.WildercordEvents.AFTER_CAST.invoker().afterCast(p,0,List.of(Runes.SWIFT),999);
				check(Unity.state(p).auraReturned()==gained,"Synthetic spent event cannot mint Aura");
				var plan=SpellCompiler.compile(List.of(Runes.SELF,Runes.SWIFT));CastEngine.cast(new Cast(p).again(1),plan.root());
				check(Unity.state(p).auraReturned()==gained,"Unpaid echo cannot mint Aura");
				spell(p,List.of(Runes.SELF,Runes.SWIFT,Runes.BLOOD_PRICE_MOD));float hp=p.getHealth();mana=Spellbooks.mana(p);
				SpellCaster.cast(p,0);check(p.getHealth()<hp && Spellbooks.mana(p)==mana && Unity.state(p).auraReturned()==gained,"Native Blood Price is health, never mana income");
				p.setGameMode(GameType.CREATIVE);spell(p,List.of(Runes.SELF,Runes.SWIFT));SpellCaster.cast(p,0);
				check(Unity.state(p).auraReturned()==gained,"Native creative casting cannot feed Unity");p.setGameMode(GameType.SURVIVAL);
				var target=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);target.setNoAi(true);target.snapTo(3,101,3,0,0);s.overworld().addFreshEntity(target);
				p.getMainHandItem().set(RuneEtchings.RUNE,Runes.HARM.id());p.setAttached(RuneEtchings.READY,0L);mana=Spellbooks.mana(p);
				check(RuneEtchings.wake(p,target,4),"Paid inscription participates");
				check(Math.abs(Unity.state(p).auraReturned()-gained-(mana-Spellbooks.mana(p))*.25)<.01,"Inscription mana is actual payment");
				p.getMainHandItem().remove(RuneEtchings.RUNE);
			});
			c.waitTicks(2);shot(c,"unity_paid_effects");
			w.getServer().runOnServer(s -> {
				var p=p(s);var old=Unity.state(p);p.setAttached(Unity.STATE,new UnityRules.State(old.since(),old.until(),old.readyAt(),0,0));
				p.setAttached(AuraAttachments.AURA,Aura.data(p).withAura(110));Spellbooks.setMana(p,100);
				for(int i=0;i<100;i++) {Spellbooks.setMana(p,Spellbooks.mana(p)-1);Unity.manaSpent(p,1);Aura.spend(p,1,"test_paid_art");}
				var bounded=Unity.state(p);check(Math.abs(bounded.manaReturned()-24)<.01 && Math.abs(bounded.auraReturned()-12)<.01,"Both lifetime budgets cap under alternating payments");
				check(Math.abs(Spellbooks.mana(p)-24)<.01 && Math.abs(Aura.aura(p)-22)<.01,"Circulating payments lose resources");
				var saved=Unity.CODEC.encodeStart(JsonOps.INSTANCE,bounded).getOrThrow();var loaded=Unity.CODEC.parse(JsonOps.INSTANCE,saved).getOrThrow();
				check(loaded.equals(bounded),"Rest and consumed allowances persist together");p.setAttached(Unity.STATE,loaded);
				var net=io.netty.buffer.Unpooled.buffer();Unity.STREAM.encode(net,loaded);check(Unity.STREAM.decode(net).equals(loaded),"Client state stream preserves deadlines and budgets");net.release();
				p.teleportTo(s.getLevel(PracticeRoom.DIMENSION),.5,101,.5,Set.<Relative>of(),0,0,false);
				check(Unity.state(p).equals(loaded) && Unity.active(p),"Dimension change retains same window and budgets");
				p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);
				// Give both tests fresh allowance so refusal cannot pass merely because a budget was already exhausted.
				p.setAttached(Unity.STATE,new UnityRules.State(loaded.since(),loaded.until(),loaded.readyAt(),0,0));
				p.setAttached(AuraAttachments.AURA,Aura.data(p).withAura(2));float manaBefore=Spellbooks.mana(p);
				Aura.spend(p,10000,"test_overdraw");check(Spellbooks.mana(p)==manaBefore,"Backlash payment cannot become mana");
				p.setAttached(AuraAttachments.AURA,Aura.data(p).withAura(22));
				long now=p.level().getGameTime();p.setAttached(Awakening.AWAKENING,new Awakening.State(AwakeningRules.Phase.SPENT.ordinal(),now-10,now-5,now+100,now+300,1,0));
				float aura=Aura.aura(p);Spellbooks.setMana(p,Spellbooks.mana(p)-1);Unity.manaSpent(p,1);check(Aura.aura(p)==aura,"Spent awakening cannot gain Aura");
				manaBefore=Spellbooks.mana(p);Aura.spend(p,1,"test_spent_art");check(Spellbooks.mana(p)==manaBefore,"Spent awakening cannot return mana either");
			});
			c.waitTicks(3);w.getServer().runOnServer(s -> {var p=p(s);check(!Unity.active(p) && Unity.state(p).readyAt()>Unity.now(p),"Spent state ends window without removing rest");});
			c.runOnClient(mc -> mc.gui.setScreen(new AuraScreen(null)));c.waitTicks(3);shot(c,"unity_rest_page");
			c.runOnClient(mc -> mc.gui.setScreen(null));
			w.getServer().runOnServer(s -> {var p=p(s);p.removeAttached(Awakening.AWAKENING);p.setAttached(Unity.STATE,UnityRules.State.NONE);setup(p);check(Unity.begin(p),"Fresh fixture opens expiry test");});
			c.waitTicks(UnityRules.DURATION+1);
			w.getServer().runOnServer(s -> {var p=p(s);check(!Unity.active(p) && Unity.state(p).readyAt()>Unity.now(p),"Native window expires with rest retained");check(!Unity.begin(p),"Expired window cannot immediately reactivate");});
		}
	}
	private static void setup(ServerPlayer p) {
		p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.removeAllEffects();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
		p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("gale",AuraRules.FORM,1800,110,0));
		p.setAttached(WildercordAttachments.INNATE,Runes.get("wildercord:kindling").orElseThrow().id());
		p.setAttached(WildercordAttachments.CIRCLES,5);
		p.removeAttached(Awakening.AWAKENING);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));Spellbooks.setMana(p,100);
	}
	private static void spell(ServerPlayer p,List<RuneDef> runes) {var b=Spellbooks.get(p);for(var r:runes)b=b.learn(r.id());Spellbooks.set(p,b.withSpell(0,runes.stream().map(RuneDef::id).toList()));Spellbooks.setReadyAt(p,0,0);}
	private static ServerPlayer p(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
	private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
	private static void shot(ClientGameTestContext c,String name){if(c.computeOnClient(mc -> mc.gui.screen()!=null)){c.getInput().setCursorPos(0,0);c.waitTicks(1);}c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
}
