package dev.wildercord.aura.world;

import dev.wildercord.aura.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import java.util.*;

/** Real registration packets, hosted duel protection, three methods, finite prize selection and saved village provenance. */
public final class VillageTournamentTest implements FabricClientGameTest {
	private static final BlockPos BOARD=new BlockPos(0,101,6),BELL=new BlockPos(32,101,0);
	@Override public void runTest(ClientGameTestContext c){
		try(var w=c.worldBuilder().create()){
			c.waitTicks(40);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule mob_griefing true");w.getServer().runCommand("gamerule fall_damage false");w.getServer().runCommand("time set 6000");
			w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-32;x<=65;x++)for(int z=-24;z<=24;z++)l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);l.setBlockAndUpdate(BELL,Blocks.BELL.defaultBlockState());
				check(VillageTournaments.install(l,new BlockPos(0,101,0),BELL)==null,"A lone bell cannot host a tournament");
				for(int i=0;i<3;i++){var v=EntityTypes.VILLAGER.create(l,EntitySpawnReason.COMMAND);v.snapTo(30+i*2,101,4,0,0);v.setNoAi(true);l.addFreshEntity(v);}
				l.setBlock(new BlockPos(0,101,0),Blocks.CHEST.defaultBlockState(),2);check(VillageTournaments.install(l,new BlockPos(0,101,0),BELL)==null,"Occupied ground cannot be overwritten");l.setBlock(new BlockPos(0,101,0),Blocks.AIR.defaultBlockState(),2);
				check(VillageTournaments.inhabited(l,BELL),"Actual residents populate the bell query");
				var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("stone",AuraRules.EDGE,0,100,0));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));p.teleportTo(l,.5,101,4.5,Set.<Relative>of(),180,0,false);
			});c.waitTicks(15);
			w.getServer().runOnServer(s->{var l=s.overworld();check(l.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING),"Test permits event placement");
				for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){var at=new BlockPos(x,101,z);check(l.hasChunkAt(at),"Arena chunk loaded at "+at);check(l.canSeeSky(at),"Arena sky exposed at "+at);check(l.getBlockState(at.below()).is(Blocks.GRASS_BLOCK),"Arena preserves its ordinary grass ground");for(int y=0;y<3;y++)check(l.getBlockState(at.above(y)).isAir(),"Arena unobstructed at "+at.above(y));}
				var b=VillageTournaments.install(l,new BlockPos(0,101,0),BELL);check(b!=null && b.authentic(),"Three residents and a clear flat square host a gathering");check(VillageTournaments.install(l,new BlockPos(0,101,0),BELL)==null,"One nearby gathering per village");});c.waitTicks(30);
			c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(2);mc.resizeGui();});
			check(w.getServer().computeOnServer(s->board(s).open()),"A nearby daytime village opens its actual event");
			w.getServer().runOnServer(s->{var b=board(s);var saved=(TournamentBoardEntity)BlockEntity.loadStatic(BOARD,b.getBlockState(),b.saveWithFullMetadata(s.registryAccess()),s.registryAccess());saved.setLevel(s.overworld());check(saved.authentic() && saved.open(),"Village provenance and event deadline survive serialization");});
			w.getServer().runOnServer(s->{var p=player(s);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.teleportTo(s.overworld(),-3,101,10,Set.<Relative>of(),-135,13,false);});c.waitTicks(120);c.runOnClient(mc->mc.gui.hud.getChat().clearMessages(false));shot(c,"village_tournament_stand");
			w.getServer().runOnServer(s->{var p=player(s);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));p.teleportTo(s.overworld(),.5,101,4.5,Set.<Relative>of(),0,22,false);});c.waitTicks(10);use(c);c.waitTicks(8);check(w.getServer().computeOnServer(s->!DuelistDuels.inDuel(player(s))),"First real interaction only offers a challenge");use(c);c.waitTicks(8);
			check(w.getServer().computeOnServer(s->DuelistDuels.phase(player(s))==dev.wildercord.duel.DuelRules.Phase.COUNTDOWN),"Second real interaction starts the hosted countdown");
			w.getServer().runOnServer(s->{var d=duelist(s);check(!d.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(player(s)),999),"Countdown rejects damage to a steward");check(d.stage()==AuraRules.EDGE,"Bout scales to the challenger's current Aura stage");});
			c.runOnClient(mc->{mc.player.setYRot(180);mc.player.setXRot(0);});c.waitTicks(70);shot(c,"village_tournament_bout");
			// An actual lethal opponent blow becomes a loss and restores only the health it took.
			w.getServer().runOnServer(s->{var p=player(s);var d=duelist(s);d.setNoAi(true);p.setHealth(18);p.damageCooldownTime=0;p.hurtServer(s.overworld(),s.overworld().damageSources().mobAttack(d),999);check(p.isAlive() && !DuelistDuels.inDuel(p) && p.getHealth()>=18 && p.getHealth()<=20,"Opponent knockout is safe and restores duel harm");check(!board(s).pending(p.getUUID()),"A loss cannot award a tournament prize");});
			// A complete three-bout run uses actual server hurt calls; AI is paused for deterministic outcome checks.
			start(w,c);Set<String> methods=new HashSet<>();
			for(int round=0;round<3;round++){
				w.getServer().runOnServer(s->{var d=duelist(s);d.setNoAi(true);methods.add(d.method().id());});c.waitTicks(65);
				w.getServer().runOnServer(s->{var d=duelist(s);d.damageCooldownTime=0;check(d.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(player(s)),999),"Player knockout reaches only their steward");});
				if(round<2){check(w.getServer().computeOnServer(s->board(s).wins()>0 && !board(s).pending(player(s).getUUID())),"Intermediate wins give no final prize");use(c);c.waitTicks(8);}
			}
			check(methods.size()==3,"Three distinct breathing methods form the circuit");
			w.getServer().runOnServer(s->{var p=player(s);var b=board(s);check(b.pending(p.getUUID()),"Three clean knockouts earn one claim");var saved=(TournamentBoardEntity)BlockEntity.loadStatic(BOARD,b.getBlockState(),b.saveWithFullMetadata(s.registryAccess()),s.registryAccess());check(saved.pending(p.getUUID()) && saved.wins()==0,"Unclaimed prize survives save/load and active bracket does not resume");});
			c.runOnClient(mc->mc.options.keyShift.setDown(true));c.waitTicks(5);use(c);c.waitTicks(8);c.runOnClient(mc->mc.options.keyShift.setDown(false));c.waitTicks(5);
			int before=w.getServer().computeOnServer(s->scrolls(player(s)));use(c);c.waitTicks(8);shot(c,"village_tournament_prize");
			w.getServer().runOnServer(s->{var p=player(s);check(scrolls(p)==before+1 && !board(s).pending(p.getUUID()),"Actual claim gives one technique scroll and clears its ledger");var b=board(s);var saved=(TournamentBoardEntity)BlockEntity.loadStatic(BOARD,b.getBlockState(),b.saveWithFullMetadata(s.registryAccess()),s.registryAccess());check(!saved.pending(p.getUUID()),"Claimed reward remains claimed on reload");});use(c);c.waitTicks(8);check(w.getServer().computeOnServer(s->scrolls(player(s))==before+1 && !DuelistDuels.inDuel(player(s))),"Repeated use grants nothing and cannot repeat a won gathering");
			// A fresh village circuit tests the eight-block boundary and magic disqualification.
			w.getServer().runOnServer(s->{var p=player(s);for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).has(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT)){var book=p.getInventory().getItem(i);p.getInventory().setItem(i,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,book);break;}});c.waitTicks(10);c.runOnClient(mc->mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(10);c.runOnClient(mc->check(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen,"Actual awarded lore opens for reading"));shot(c,"village_tournament_three_bows");c.runOnClient(mc->mc.gui.setScreen(null));
			w.getServer().runOnServer(s->{var l=s.overworld();l.setBlockAndUpdate(BOARD,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(BOARD,VillageTournaments.BOARD.defaultBlockState());board(s).host(BELL);player(s).setHealth(20);});c.waitTicks(30);start(w,c);c.waitTicks(65);
			w.getServer().runOnServer(s->{var p=player(s);p.teleportTo(s.overworld(),12,101,0,Set.<Relative>of(),0,0,false);});c.waitTicks(15);check(w.getServer().computeOnServer(s->!DuelistDuels.inDuel(player(s)) && !board(s).pending(player(s).getUUID())),"Leaving the hosted radius ends the run without a prize");
			start(w,c);w.getServer().runOnServer(s->duelist(s).setNoAi(true));c.waitTicks(65);w.getServer().runOnServer(s->{var d=duelist(s);d.damageCooldownTime=0;d.hurtServer(s.overworld(),s.overworld().damageSources().source(net.minecraft.world.damagesource.DamageTypes.MAGIC,player(s)),999);check(!DuelistDuels.inDuel(player(s)) && board(s).wins()==0 && !board(s).pending(player(s).getUUID()),"A spell win disqualifies the entire Aura run");});
			start(w,c);w.getServer().runOnServer(s->duelist(s).setNoAi(true));c.waitTicks(65);w.getServer().runOnServer(s->{var p=player(s);p.damageCooldownTime=0;var rival=new Rival(s.overworld());p.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(rival),2);check(!DuelistDuels.inDuel(p) && board(s).wins()==0 && !board(s).pending(p.getUUID()),"A third-party blow interrupts without awarding a round");});
			// Ordinary wandering duels retain their lesson and farewell, independently of hosted outcomes.
			var ordinary=w.getServer().computeOnServer(s->{var d=AuraWorld.DUELIST.create(s.overworld(),EntitySpawnReason.COMMAND);d.setMethod(BreathingMethods.EMBER);d.snapTo(-3,101,0,0,0);d.setNoAi(true);s.overworld().addFreshEntity(d);DuelistDuels.start(player(s),d);return d.getUUID();});c.waitTicks(65);w.getServer().runOnServer(s->{var d=(Duelist)s.overworld().getEntity(ordinary);d.damageCooldownTime=0;d.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(player(s)),999);check(!DuelistDuels.inDuel(player(s)) && d.leaving() && dev.wildercord.player.Heart.discovered(player(s),"aura:duelist"),"Ordinary duel still teaches and sends its defeated wanderer away");});
		}
	}
	private static ServerPlayer player(net.minecraft.server.MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
	private static TournamentBoardEntity board(net.minecraft.server.MinecraftServer s){return (TournamentBoardEntity)s.overworld().getBlockEntity(BOARD);}
	private static Duelist duelist(net.minecraft.server.MinecraftServer s){return (Duelist)s.overworld().getEntity(board(s).opponent());}
	private static int scrolls(ServerPlayer p){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(TechniqueScrollItem.SCROLL))n+=p.getInventory().getItem(i).getCount();return n;}
	private static void start(TestSingleplayerContext w,ClientGameTestContext c){w.getServer().runOnServer(s->{var p=player(s);p.teleportTo(s.overworld(),.5,101,4.5,Set.<Relative>of(),180,0,false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));});c.waitTicks(10);use(c);c.waitTicks(8);use(c);c.waitTicks(8);check(w.getServer().computeOnServer(s->DuelistDuels.inDuel(player(s))),"Registration starts a real hosted duel");}
	private static void use(ClientGameTestContext c){c.runOnClient(mc->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(BOARD),Direction.NORTH,BOARD,false)));}
	private static void shot(ClientGameTestContext c,String name){c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
	private static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
	private static final class Rival extends net.fabricmc.fabric.api.entity.FakePlayer {Rival(ServerLevel l){super(l,new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("tournament-rival".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"Rival"));}}
}
