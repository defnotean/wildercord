package dev.wildercord.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Two real client JVMs through actual loopback TCP. Connection gate only, no Aura completion claim. */
public final class WildercordTwoClientsTest implements FabricClientGameTest {
 private Path directory;private String nonce;
 @Override public void runTest(ClientGameTestContext c){
  directory=Path.of(System.getProperty("wildercord.mp.directory")).toAbsolutePath();nonce=System.getProperty("wildercord.mp.nonce");check(nonce!=null&&!nonce.isBlank(),"Explicit per-run nonce required");
  try{if("host".equals(System.getProperty("wildercord.mp.role")))host(c);else if("peer".equals(System.getProperty("wildercord.mp.role")))peer(c);else throw new AssertionError("Explicit host/peer role required");}
  catch(Throwable error){write(System.getProperty("wildercord.mp.role")+"-failure",Map.of("error",error.toString()));throw error;}
 }
 private void host(ClientGameTestContext c){
  var properties=new Properties();properties.setProperty("server-ip","127.0.0.1");properties.setProperty("server-port",Integer.toString(localPort()));properties.setProperty("max-players","2");properties.setProperty("white-list","false");properties.setProperty("online-mode","false");properties.setProperty("enforce-secure-profile","false");properties.setProperty("view-distance","5");properties.setProperty("simulation-distance","5");
  try(var server=c.worldBuilder().createServer(properties);var connection=server.connect()){
   connection.waitForChunksDownload();connection.waitForClientboundPackets();
   UUID host=c.computeOnClient(mc->mc.player.getUUID());long pid=ProcessHandle.current().pid();int port=server.computeOnServer(s->s.getPort());check(port>0,"Actual dedicated listener chooses a real TCP port");
   write("host-ready",Map.of("port",Integer.toString(port),"pid",Long.toString(pid),"uuid",host.toString()));
   await(c,()->exists("peer-connected")&&server.computeOnServer(s->s.getPlayerList().getPlayers().size()==2),"Both real TCP clients join the same dedicated server");
   var peerRecord=read("peer-connected");UUID peer=UUID.fromString(peerRecord.getProperty("uuid"));check(Long.parseLong(peerRecord.getProperty("pid"))!=pid&&!peer.equals(host),"Second profile is owned by a distinct JVM process");
   server.runOnServer(s->{
    var h=s.getPlayerList().getPlayer(host);var p=s.getPlayerList().getPlayer(peer);check(h!=null&&p!=null&&h!=p,"Dedicated player list holds both exact connected client UUIDs");
    var a=tcp(h);var b=tcp(p);check(!a.equals(b),"Two profiles have distinct actual loopback TCP remote sockets");
    for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){s.overworld().setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<=105;y++)s.overworld().setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
    for(var player:List.of(h,p)){player.setGameMode(GameType.SURVIVAL);player.teleportTo(s.overworld(),player==h?.5:4.5,101,.5,Set.<Relative>of(),0,0,false);}
   });server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule fall_damage false");
   write("move-peer",Map.of("host",host.toString()));
   await(c,()->exists("peer-moved"),"Peer completes real client movement input");
   await(c,()->server.computeOnServer(s->s.getPlayerList().getPlayer(peer).getZ()>.9),"Peer server entity moved through actual client movement packets");
   c.waitFor(mc->mc.level!=null&&mc.level.getPlayerByUUID(peer)!=null&&mc.level.getPlayerByUUID(peer).getZ()>.9,200);
   write("move-host",Map.of());c.waitFor(mc->mc.player!=null&&Math.abs(mc.player.getX()-.5)<.15&&Math.abs(mc.player.getZ()-.5)<.15,200);move(c);
   await(c,()->exists("peer-saw-host"),"Peer renderer observes actual host movement");
   await(c,()->server.computeOnServer(s->s.getPlayerList().getPlayer(host).getZ()>.9),"Host server entity moved through real client input");
   aim(c,peer);c.takeScreenshot("two_clients_host_tcp");write("disconnect-peer",Map.of());await(c,()->exists("peer-disconnected")&&server.computeOnServer(s->s.getPlayerList().getPlayers().size()==1),"Peer disconnect removes its genuine server connection");
   write("host-passed",Map.of("host",host.toString(),"peer",peer.toString(),"port",Integer.toString(port)));
  }
 }
 private void peer(ClientGameTestContext c){
  await(c,()->exists("host-ready"),"Host publishes actual dedicated TCP address");var ready=read("host-ready");String address="127.0.0.1:"+Integer.parseInt(ready.getProperty("port"));
  try{
   c.runOnClient(mc->ConnectScreen.startConnecting(mc.gui.screen(),mc,ServerAddress.parseString(address),new ServerData("Wildercord two-client fixture",address,ServerData.Type.OTHER),false,null));
   c.waitFor(mc->mc.player!=null&&mc.level!=null&&mc.getConnection()!=null,2400);
   UUID uuid=c.computeOnClient(mc->mc.player.getUUID());write("peer-connected",Map.of("uuid",uuid.toString(),"pid",Long.toString(ProcessHandle.current().pid()),"name",c.computeOnClient(mc->mc.getUser().getName())));
   await(c,()->exists("move-peer"),"Host completes real connection and socket admission");
   c.waitFor(mc->mc.player!=null&&Math.abs(mc.player.getX()-4.5)<.15&&Math.abs(mc.player.getZ()-.5)<.15,200);move(c);write("peer-moved",Map.of());
   await(c,()->exists("move-host"),"Host begins its actual client input");UUID host=UUID.fromString(read("move-peer").getProperty("host"));
   c.waitFor(mc->mc.level!=null&&mc.level.getPlayerByUUID(host)!=null&&mc.level.getPlayerByUUID(host).getZ()>.9,200);
   aim(c,host);c.takeScreenshot("two_clients_peer_tcp");write("peer-saw-host",Map.of());await(c,()->exists("disconnect-peer"),"Host admits bidirectional movement before closing peer");
  }finally{c.runOnClient(mc->mc.disconnect(new TitleScreen(),false));c.waitTicks(5);write("peer-disconnected",Map.of());}
 }
 // Fabric connect() uses the configured port; port0 does not expose the OS-assigned bound port.
 private static int localPort(){try(var socket=new java.net.ServerSocket(0,1,java.net.InetAddress.getLoopbackAddress())){return socket.getLocalPort();}catch(java.io.IOException failure){throw new AssertionError("Could not reserve local test port",failure);}}
 private static InetSocketAddress tcp(ServerPlayer p){check(p.connection.getRemoteAddress() instanceof InetSocketAddress,"Connected profile has actual network socket rather than fabricated/memory guest");var address=(InetSocketAddress)p.connection.getRemoteAddress();check(address.getAddress().isLoopbackAddress()&&address.getPort()>0,"Actual client connection is local loopback TCP");return address;}
 private static void aim(ClientGameTestContext c,UUID other){c.runOnClient(mc->{var entity=mc.level.getPlayerByUUID(other);check(entity!=null,"Other actual connected client is present in renderer");var delta=entity.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));});c.waitTicks(5);}
 private static void move(ClientGameTestContext c){c.runOnClient(mc->{mc.gui.setScreen(null);mc.player.setYRot(0);mc.player.setXRot(0);});try{c.getInput().holdKey(o->o.keyUp);c.waitFor(mc->mc.player!=null&&mc.player.getZ()>.9,200);}catch(Throwable failure){c.runOnClient(mc->dev.wildercord.Wildercord.LOGGER.info("TWO_CLIENT_MOVE role={} pos={} yaw={} screen={} key={}",System.getProperty("wildercord.mp.role"),mc.player.position(),mc.player.getYRot(),mc.gui.screen(),mc.options.keyUp.isDown()));throw failure;}finally{c.getInput().releaseKey(o->o.keyUp);}}
 private void await(ClientGameTestContext c,BooleanSupplier condition,String reason){long deadline=System.nanoTime()+120_000_000_000L;while(System.nanoTime()<deadline){check(!Files.exists(directory.resolve("host-failure.properties"))&&!Files.exists(directory.resolve("peer-failure.properties")),"Both independent client roles remain healthy: "+reason);if(condition.getAsBoolean())return;c.waitTicks(1);}throw new AssertionError(reason+" exceeded finite120s barrier");}
 private boolean exists(String name){Path file=directory.resolve(name+".properties");return Files.exists(file)&&nonce.equals(read(name).getProperty("nonce"));}
 private Properties read(String name){try(var input=Files.newInputStream(directory.resolve(name+".properties"))){var p=new Properties();p.load(input);check(nonce.equals(p.getProperty("nonce")),"IPC belongs to current run nonce");return p;}catch(java.io.IOException error){throw new AssertionError(error);}}
 private void write(String name,Map<String,String> values){try{Files.createDirectories(directory);var p=new Properties();p.setProperty("nonce",nonce);p.putAll(values);Path temp=directory.resolve(name+".tmp");try(var output=Files.newOutputStream(temp)){p.store(output,"Bounded local native handshake");}Files.move(temp,directory.resolve(name+".properties"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.io.IOException error){throw new AssertionError(error);}}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
