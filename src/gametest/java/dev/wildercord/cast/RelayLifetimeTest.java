package dev.wildercord.cast;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Set;

/** Real tick/time/body transitions, including same-callback dimension round trip and TCP disconnect/rejoin. */
public final class RelayLifetimeTest implements FabricClientGameTest {
	private enum Change { CONTROL, CRACK, CORD, SLOT_EDIT, COVER, TETHER_COVER, WARD, SILENCE, SHARED_INTERRUPT, DAMAGE, DEATH, RESPAWN, DIMENSION, ROUND_TRIP, DISCONNECT }
	private static final class Probe {
		ServerPlayer original;
		ServerLevel level;
		TrainingDummy target;
		long rest, began, committed, mutated;
		float paidMana;
		boolean done;
		Throwable failure;
	}
	private Probe probe;
	@Override public void runTest(ClientGameTestContext context) {
		Properties properties = new Properties();
		properties.setProperty("server-ip", "127.0.0.1"); properties.setProperty("server-port", Integer.toString(port()));
		properties.setProperty("online-mode", "false"); properties.setProperty("enforce-secure-profile", "false");
		properties.setProperty("pause-when-empty-seconds", "-1"); properties.setProperty("view-distance", "3"); properties.setProperty("simulation-distance", "3");
		try (var server = context.worldBuilder().createServer(properties)) {
			TestDedicatedServerConnection connection = server.connect();
			try {
				connection.waitForChunksDownload(); connection.waitForClientboundPackets();
				server.runCommand("gamerule spawn_mobs false"); server.runCommand("gamerule fall_damage false");
				server.runOnServer(s -> {
					for (var level : new ServerLevel[]{s.overworld(),s.getLevel(Level.NETHER)}) {
						for(int x=-1;x<=0;x++)for(int z=-1;z<=1;z++)level.setChunkForced(x,z,true);
						for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++) {level.setBlock(new BlockPos(x,149,z),Blocks.STONE.defaultBlockState(),2);for(int y=150;y<=154;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
					}
				});
				for (Change change : Change.values()) {
					server.runOnServer(s -> begin(s,change));
					server.waitFor(s -> probe.done,100);
					server.runOnServer(s -> {
						if(probe.failure!=null)throw new AssertionError(change+" Relay lifecycle failed",probe.failure);
						RelayCircleTest.check(probe.mutated==probe.committed+2,"Mutation occurs two real ticks into the six-tick warning");
						if(change==Change.CONTROL)RelayCircleTest.check(probe.target.hitSequence()==1,"Control timeline actually fires exactly once");
						else RelayCircleTest.check(probe.target.hitSequence()==0 && !probe.target.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) && probe.target.getTicksFrozen()==0,"Retired focus never wounds or freezes its original target: "+change);
						RelayCircleTest.check(!RelayCircles.pending(probe.original),"No pending focus remains after terminal outcome");
						if(change!=Change.DISCONNECT)restore(s);
					});
					if(change!=Change.DISCONNECT){context.runOnClient(mc->mc.gui.setScreen(null));context.waitTicks(12);}
				}
				context.waitFor(mc -> mc.level==null,200);context.setScreen(TitleScreen::new);
				connection=server.connect();connection.waitForChunksDownload();
				server.runOnServer(s->{
					ServerPlayer joined=s.getPlayerList().getPlayer(probe.original.getUUID());
					RelayCircleTest.check(joined!=null&&joined!=probe.original,"Rejoin creates a distinct original-UUID body");
					RelayCircleTest.check(!RelayCircles.pending(joined)&&!joined.hasAttached(RelayState.VIEW),"Reconnect cannot revive transient focus or warning");
					RelayCircleTest.check(joined.getAttachedOrElse(RelayState.REST,0L)==probe.rest,"Shared rest persists across actual disconnect/rejoin");
					RelayCircleTest.check(MasterStudies.knowsRelay(joined),"Learned lesson survives actual reconnect");
					RelayCircleTest.check(probe.target.hitSequence()==0,"Original target is still untouched after rejoin");probe.target.discard();
				});
			} finally {if(context.computeOnClient(mc -> mc.level!=null))connection.close();}
		}
	}
	private void begin(MinecraftServer server,Change change){
		probe=new Probe();probe.original=RelayCircleTest.player(server);probe.level=server.overworld();
		if(probe.original.level()!=probe.level)probe.original.teleportTo(probe.level,.5,150,.5,Set.of(),0,55,false);
		RelayCircleTest.prepare(probe.original,Runes.FROST);
		if(change==Change.WARD){
			for(int x=17;x<=23;x++)for(int z=-2;z<=9;z++){probe.level.setBlock(new BlockPos(x,149,z),Blocks.STONE.defaultBlockState(),2);for(int y=150;y<=154;y++)probe.level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
			probe.original.teleportTo(probe.level,20.5,150,.5,Set.of(),0,55,false);
		}
		probe.target=WildercordEntities.TRAINING_DUMMY.create(probe.level,EntitySpawnReason.MOB_SUMMONED);RelayCircleTest.check(probe.target!=null,"Lifecycle dummy creates");
		probe.target.snapTo(change==Change.WARD?20.5:.5,150,6.5,180,0);probe.level.addFreshEntity(probe.target);
		RelayCircleTest.directDown(probe.original);
		RelayCircleTest.check(RelayCircles.pending(probe.original),"Production input accepted and placed lifecycle focus");
		probe.began=probe.level.getGameTime();probe.paidMana=Spellbooks.mana(probe.original);probe.rest=probe.original.getAttachedOrElse(RelayState.REST,0L);
		Probe current=probe;
		Scheduler.later(2,()->checked(current,()->{
			if(change==Change.TETHER_COVER)current.original.teleportTo(current.level,-3.5,150,.5,Set.of(),0,0,false);
			RelayCircleTest.aim(current.original,current.target.getBoundingBox().getCenter());RelayCircleTest.downAfterUp(current.original);
			RelayState state=current.original.getAttached(RelayState.VIEW);RelayCircleTest.check(state!=null&&state.phase()==RelayState.WARNING,"Production second edge commits lifecycle warning");
			current.committed=current.level.getGameTime();
			Scheduler.later(2,()->checked(current,()->mutate(server,current,change)));
			Scheduler.later(9,()->checked(current,()->current.done=true));
		}));
	}
	private static void mutate(MinecraftServer server,Probe p,Change change){
		ServerPlayer player=p.original;p.mutated=p.level.getGameTime();
		switch(change){
			case CONTROL -> {}
			case CRACK -> player.setAttached(WildercordAttachments.CRACKS,new WildercordAttachments.Cracks(1,p.level.getGameTime()+400));
			case CORD -> Spellbooks.setCord(player,new ItemStack(WildercordItems.ECHO_CORD));
			case SLOT_EDIT -> SpellCaster.edit(player,0,ListOfRelayHarm());
			case COVER -> {for(int y=150;y<=153;y++)p.level.setBlock(new BlockPos(0,y,3),Blocks.STONE.defaultBlockState(),2);}
			case TETHER_COVER -> p.level.setBlock(new BlockPos(-1,150,1),Blocks.STONE.defaultBlockState(),2);
			case WARD -> dev.wildercord.world.dungeons.DungeonWards.remember(p.level,()->java.util.List.of(new net.minecraft.world.level.levelgen.structure.BoundingBox(20,149,3,20,154,3)));
			case SILENCE -> CastLock.interrupt(player,10);
			case SHARED_INTERRUPT -> {
				RelayCircleTest.check(Statuses.interrupt(player),"Ordinary shared interruption reaches a Relay without a Charging attachment");
				RelayCircleTest.check(!Statuses.interrupt(player),"A second interrupt keeps the existing shared immunity semantics");
			}
			case DAMAGE -> {player.setInvulnerableTime(0);player.hurtServer(p.level,p.level.damageSources().generic(),1);}
			case DEATH -> {player.kill(p.level);RelayCircleTest.check(!player.isAlive(),"Native death really occurs");}
			case RESPAWN -> {player.kill(p.level);ServerPlayer replacement=respawn(server,player);RelayCircleTest.check(replacement!=player&&replacement.isAlive(),"Native respawn replaces the body");}
			case DIMENSION,ROUND_TRIP -> {
				RelayCircleTest.check(player.teleportTo(server.getLevel(Level.NETHER),.5,150,.5,Set.of(),0,0,false),"Native dimension departure succeeds");
				if(change==Change.ROUND_TRIP)RelayCircleTest.check(player.teleportTo(p.level,.5,150,.5,Set.of(),0,0,false),"Native same-callback return succeeds without polling pending validity");
			}
			case DISCONNECT -> {
				Connection socket=server.getConnection().getConnections().stream().filter(c->c.getPacketListener()==player.connection).findFirst().orElseThrow();
				RelayCircleTest.check(socket.isConnected()&&!socket.isMemoryConnection(),"Fixture disconnects the genuine TCP owner");
				socket.disconnect(Component.literal("Relay original-body lifecycle test"));socket.handleDisconnection();
				RelayCircleTest.check(server.getPlayerList().getPlayer(player.getUUID())==null&&player.isRemoved(),"Native disconnect removes original body");
			}
		}
		// No pending-state poll here: the production tick/impact must discover and retire the lifetime itself.
	}
	private static java.util.List<String> ListOfRelayHarm(){return java.util.List.of(Runes.RELAY.id(),Runes.HARM.id());}
	private void restore(MinecraftServer server){
		ServerPlayer current=server.getPlayerList().getPlayer(probe.original.getUUID());
		if(!current.isAlive())current=respawn(server,current);
		if(current.level()!=probe.level)current.teleportTo(probe.level,.5,150,.5,Set.of(),0,55,false);
		RelayCircleTest.check(current.getAttachedOrElse(RelayState.REST,0L)==probe.rest,"Cancellation/death/respawn keeps paid shared rest");
		RelayCircleTest.check(MasterStudies.knowsRelay(current),"Known lesson survives lifecycle transition");
		probe.target.discard();
	}
	private static ServerPlayer respawn(MinecraftServer server,ServerPlayer dead){
		dead.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));return server.getPlayerList().getPlayer(dead.getUUID());
	}
	private static void checked(Probe p,Runnable action){if(p.failure!=null)return;try{action.run();}catch(RuntimeException|AssertionError e){p.failure=e;p.done=true;}}
	private static int port(){try(ServerSocket socket=new ServerSocket(0,0,InetAddress.getLoopbackAddress())){return socket.getLocalPort();}catch(java.io.IOException e){throw new RuntimeException(e);}}
}
