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

/** Two real client JVMs through actual loopback TCP. Connection plus independent Unity ownership, real allied healing and cancellation; supplied progression. */
public final class WildercordTwoClientsUnityTest implements FabricClientGameTest {
 private Path directory;private String nonce;
 private int healingPrice;
 private dev.wildercord.aura.UnityRules.State peerAtDeparture;
 private net.minecraft.server.level.ServerPlayer departedPeer;
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
   aim(c,peer);c.takeScreenshot("two_clients_host_tcp");unityHost(c,server,host,peer);write("disconnect-peer",Map.of());await(c,()->exists("peer-disconnected")&&server.computeOnServer(s->s.getPlayerList().getPlayers().size()==1),"Peer disconnect removes its genuine server connection");
   server.runOnServer(s->{check(dev.wildercord.aura.Unity.state(departedPeer).equals(peerAtDeparture.stop()),"Actual disconnect stops only the departing owner while preserving spent allowances and rest");check(!dev.wildercord.aura.Unity.active(s.getPlayerList().getPlayer(host)),"Host remains independently cancelled after peer departure");});
   write("host-unity-passed",Map.of("host",host.toString(),"peer",peer.toString()));
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
   aim(c,host);c.takeScreenshot("two_clients_peer_tcp");write("peer-saw-host",Map.of());unityPeer(c,host);await(c,()->exists("disconnect-peer"),"Host admits bidirectional movement before closing peer");
  }finally{c.runOnClient(mc->mc.disconnect(new TitleScreen(),false));c.waitTicks(5);write("peer-disconnected",Map.of());}
 }

 private void unityHost(ClientGameTestContext c,TestServerContext server,UUID host,UUID peer){
  var original=server.computeOnServer(srv->dev.wildercord.config.Config.get());
  try {
   server.runOnServer(srv->{
    var aura=copy(original.aura(),Map.of("gainMultiplier",0.0));current(copy(original,Map.of("manaRegenMultiplier",0.0,"aura",aura)));
    var h=srv.getPlayerList().getPlayer(host);var p=srv.getPlayerList().getPlayer(peer);
    for(var actor:List.of(h,p))prepareUnity(actor);
    h.teleportTo(srv.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);p.teleportTo(srv.overworld(),2.5,101,.5,Set.<Relative>of(),90,0,false);
    var runes=List.of(dev.wildercord.spell.Runes.TOUCH,dev.wildercord.spell.Runes.HEAL);
    var book=dev.wildercord.player.Spellbooks.get(p).withStarterGiven();for(var r:runes)book=book.learn(r.id());dev.wildercord.player.Spellbooks.set(p,book.withSpell(0,runes.stream().map(dev.wildercord.spell.RuneDef::id).toList()));
    check(dev.wildercord.cast.SpellCaster.edit(p,0,runes.stream().map(dev.wildercord.spell.RuneDef::id).toList())==null,"Real spell edit validates supplied learned allied-healing route");
    var plan=dev.wildercord.spell.SpellCompiler.compile(runes);healingPrice=dev.wildercord.player.Heart.manaCost(p,plan,dev.wildercord.player.Heart.secretCost(p,runes)*dev.wildercord.cast.Mastery.costFactor(p,runes));check(healingPrice>0&&healingPrice<48,"Real compiled cost is finite below conversion cap");
   });
   server.runCommand("gamerule natural_health_regeneration false");server.runCommand("team add wc_unity_fixture");server.runCommand("team join wc_unity_fixture "+server.computeOnServer(srv->srv.getPlayerList().getPlayer(host).getGameProfile().name()));server.runCommand("team join wc_unity_fixture "+server.computeOnServer(srv->srv.getPlayerList().getPlayer(peer).getGameProfile().name()));
   server.runOnServer(srv->check(dev.wildercord.cast.Targets.canHelp(srv.getPlayerList().getPlayer(peer),srv.getPlayerList().getPlayer(host)),"Real scoreboard allies admit healing"));
   write("unity-peer-activate",Map.of());await(c,()->exists("unity-peer-active"),"Peer sends actual Activate payload and receives owner attachment");
   await(c,()->server.computeOnServer(srv->dev.wildercord.aura.Unity.active(srv.getPlayerList().getPlayer(peer))),"Dedicated peer receives actual packet activation");
   server.runOnServer(srv->{var p=srv.getPlayerList().getPlayer(peer);var h=srv.getPlayerList().getPlayer(host);check(dev.wildercord.aura.Unity.state(h).equals(dev.wildercord.aura.UnityRules.State.NONE),"Peer Activate cannot begin host window");activationPrice(p);check(dev.wildercord.player.Spellbooks.mana(h)==100&&dev.wildercord.aura.Aura.aura(h)==110,"Peer packet pays neither host pool: mana="+dev.wildercord.player.Spellbooks.mana(h)+" aura="+dev.wildercord.aura.Aura.aura(h)+" starter="+dev.wildercord.player.Spellbooks.get(h).starterGiven()+" max="+dev.wildercord.player.Mana.max(h));});
   c.runOnClient(mc->{mc.gui.setScreen(null);net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new dev.wildercord.aura.Unity.Activate());});
   c.waitFor(mc->dev.wildercord.aura.Unity.active(mc.player),120);
   await(c,()->server.computeOnServer(srv->dev.wildercord.aura.Unity.active(srv.getPlayerList().getPlayer(host))),"Host Activate independently begins own real window");
   server.runOnServer(srv->{var h=srv.getPlayerList().getPlayer(host);activationPrice(h);h.setHealth(8);check(h.getHealth()==8,"Supplied injured ally fixture is real health");});
   var before=server.computeOnServer(srv->dev.wildercord.aura.Unity.state(srv.getPlayerList().getPlayer(host)));
   write("unity-peer-heal",Map.of());await(c,()->exists("unity-peer-healed"),"Peer sends actual paid Touch Heal intent");
   await(c,()->server.computeOnServer(srv->srv.getPlayerList().getPlayer(host).getHealth()>8),"Actual native healing reaches real connected allied host");
   server.runOnServer(srv->{var h=srv.getPlayerList().getPlayer(host);var p=srv.getPlayerList().getPlayer(peer);float paid=88-dev.wildercord.player.Spellbooks.mana(p);check(paid==healingPrice,"Actual client packet spends the exact authoritative compiled healing price once");check(dev.wildercord.player.Spellbooks.readyAt(p,0)>srv.overworld().getGameTime(),"Actual packet creates spell cooldown");check(Math.abs(dev.wildercord.aura.Unity.state(p).auraReturned()-paid*.25)<.01,"Peer mana payment returns only its own exact quarter Aura");check(Math.abs(dev.wildercord.aura.Aura.aura(p)-(98+paid*.25))<.01,"Real owner Aura pool agrees with conversion allowance");check(dev.wildercord.aura.Unity.state(h).equals(before)&&dev.wildercord.player.Spellbooks.mana(h)==88&&dev.wildercord.aura.Aura.aura(h)==98,"Receiving allied support cannot mint or transfer host Unity resource income");});
   c.takeScreenshot("two_clients_unity_allied_heal_host");write("unity-peer-verify",Map.of());await(c,()->exists("unity-peer-verified"),"Peer verifies synced own allowance and private remote attachment");
   var hostBeforeCancel=server.computeOnServer(srv->dev.wildercord.aura.Unity.state(srv.getPlayerList().getPlayer(host)));
   c.runOnClient(mc->dev.wildercord.Wildercord.LOGGER.info("UNITY_CANCEL_BEFORE screen={} slot={} hand={} state={} clock={}",mc.gui.screen(),mc.player.getInventory().getSelectedSlot(),mc.player.getMainHandItem(),dev.wildercord.aura.Unity.state(mc.player),mc.level.getGameTime()));
   check(c.computeOnClient(mc->mc.player.getInventory().getItem(8).isEmpty()),"Actual ninth hotbar slot is empty after ordinary onboarding gifts");
   c.getInput().pressKey(o->o.keyHotbarSlots[8]);
   c.waitTicks(3);
   c.runOnClient(mc->dev.wildercord.Wildercord.LOGGER.info("UNITY_CANCEL_AFTER screen={} slot={} hand={} state={} clock={}",mc.gui.screen(),mc.player.getInventory().getSelectedSlot(),mc.player.getMainHandItem(),dev.wildercord.aura.Unity.state(mc.player),mc.level.getGameTime()));
   server.runOnServer(srv->{var h=srv.getPlayerList().getPlayer(host);dev.wildercord.Wildercord.LOGGER.info("UNITY_CANCEL_SERVER slot={} hand={} state={} clock={}",h.getInventory().getSelectedSlot(),h.getMainHandItem(),dev.wildercord.aura.Unity.state(h),srv.overworld().getGameTime());});
   await(c,()->server.computeOnServer(srv->srv.getPlayerList().getPlayer(host).getMainHandItem().isEmpty()&&!dev.wildercord.aura.Unity.active(srv.getPlayerList().getPlayer(host))),"Actual host hotbar selection packet cancels own weapon-required window");
   server.runOnServer(srv->{check(dev.wildercord.aura.Unity.state(srv.getPlayerList().getPlayer(host)).equals(hostBeforeCancel.stop()),"Cancellation retains exact paid budgets and rest");check(dev.wildercord.aura.Unity.active(srv.getPlayerList().getPlayer(peer)),"Host putting weapon away cannot cancel still-eligible peer");departedPeer=srv.getPlayerList().getPlayer(peer);peerAtDeparture=dev.wildercord.aura.Unity.state(departedPeer);});
   c.waitFor(mc->!dev.wildercord.aura.Unity.active(mc.player),120);c.takeScreenshot("two_clients_unity_owner_cancel_host");write("unity-peer-cancel-observed",Map.of());await(c,()->exists("unity-peer-still-active"),"Peer remains active before actual disconnection");
  } finally {server.runOnServer(srv->current(original));}
 }
 private void unityPeer(ClientGameTestContext c,UUID host){
  await(c,()->exists("unity-peer-activate"),"Host prepares genuine supplied progression and allies");c.waitFor(mc->Math.abs(mc.player.getX()-2.5)<.15&&Math.abs(mc.player.getZ()-.5)<.15,200);
  c.runOnClient(mc->{mc.gui.setScreen(null);net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new dev.wildercord.aura.Unity.Activate());});c.waitFor(mc->dev.wildercord.aura.Unity.active(mc.player),120);write("unity-peer-active",Map.of());
  await(c,()->exists("unity-peer-heal"),"Both real owners activate before cooperative cast");aim(c,host);c.waitTicks(3);
  c.runOnClient(mc->net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new dev.wildercord.net.WildercordNetworking.CastSpell(0)));c.waitFor(mc->dev.wildercord.aura.Unity.state(mc.player).auraReturned()>0,120);write("unity-peer-healed",Map.of());
  await(c,()->exists("unity-peer-verify"),"Server admits exact payment and actual allied health");
  check(c.computeOnClient(mc->dev.wildercord.aura.Unity.state(mc.player).auraReturned()>0),"Actual peer receives its own converted allowance");
  check(c.computeOnClient(mc->dev.wildercord.aura.Unity.state(mc.level.getPlayerByUUID(host)).equals(dev.wildercord.aura.UnityRules.State.NONE)),"Target-only Unity attachment is not broadcast to the other actual client");
  c.takeScreenshot("two_clients_unity_allied_heal_peer");write("unity-peer-verified",Map.of());await(c,()->exists("unity-peer-cancel-observed"),"Host cancels through actual client equipment selection");check(c.computeOnClient(mc->dev.wildercord.aura.Unity.active(mc.player)),"Peer client remains active after other owner cancellation");write("unity-peer-still-active",Map.of());
 }
 private static void prepareUnity(ServerPlayer p){
  p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.setHealth(20);p.removeAllEffects();
  p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
  p.setAttached(dev.wildercord.aura.AuraAttachments.AURA,new dev.wildercord.aura.AuraAttachments.Data("gale",dev.wildercord.aura.AuraRules.FORM,1800,110,0));
  p.setAttached(dev.wildercord.player.WildercordAttachments.INNATE,dev.wildercord.spell.Runes.get("wildercord:kindling").orElseThrow().id());p.setAttached(dev.wildercord.player.WildercordAttachments.CIRCLES,5);p.removeAttached(dev.wildercord.aura.Awakening.AWAKENING);
  dev.wildercord.player.Spellbooks.set(p,dev.wildercord.player.Spellbooks.get(p).withStarterGiven());
  dev.wildercord.player.Spellbooks.setCord(p,new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));dev.wildercord.player.Spellbooks.setMana(p,100);
  check(dev.wildercord.aura.Unity.state(p).equals(dev.wildercord.aura.UnityRules.State.NONE),"Fresh actual profile has no supplied activation clock");check(dev.wildercord.aura.Unity.refusal(p)==null,"Supplied progression satisfies actual runtime admission");
 }
 private static void activationPrice(ServerPlayer p){check(dev.wildercord.player.Spellbooks.mana(p)==88&&dev.wildercord.aura.Aura.aura(p)==98,"Actual owner activation spends exactly twelve of both pools");check(dev.wildercord.aura.Unity.state(p).manaReturned()==0&&dev.wildercord.aura.Unity.state(p).auraReturned()==0,"Activation never refunds itself");}
 @SuppressWarnings("unchecked") private static <T>T copy(T record,Map<String,Object> changes){try{var parts=record.getClass().getRecordComponents();var types=new Class<?>[parts.length];var args=new Object[parts.length];for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();args[i]=changes.getOrDefault(parts[i].getName(),parts[i].getAccessor().invoke(record));}return (T)record.getClass().getDeclaredConstructor(types).newInstance(args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(dev.wildercord.config.WildercordConfig config){try{var field=dev.wildercord.config.Config.class.getDeclaredField("current");field.setAccessible(true);field.set(null,config);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
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
