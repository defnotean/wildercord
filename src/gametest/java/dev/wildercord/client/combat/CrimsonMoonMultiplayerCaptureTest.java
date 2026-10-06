package dev.wildercord.client.combat;

import com.google.gson.Gson;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.*;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.SwordStringsClient;
import dev.wildercord.client.fx.HitStop;
import dev.wildercord.gametest.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

/** One fresh world and accepted Final, two real TCP clients, two causally bounded native images. */
public final class CrimsonMoonMultiplayerCaptureTest implements FabricClientGameTest {
    public static final String CASE="articulated_netherite_funded_right_release";
    private static final String ART="crimson_moon";
    private static final List<String> IDENTITY=List.of("nonce","suite","sourceHead","checkoutSha","prHeadSha","descriptorSha256","hostUuid","peerUuid","runIdentity");
    private static final String[] SETTINGS={ArticulatedCombat.ENABLE_PROPERTY,ArticulatedCombat.STABLE_CAMERA_PROPERTY,ArticulatedArmorRenderer.ENABLE_PROPERTY,ArticulatedArmorRenderer.VIEW_PROPERTY,ArticulatedAuraShellRenderer.ENABLE_PROPERTY};
    private static final long MAX_HOLD=Arrays.stream(AuraFxRules.Weight.values()).mapToInt(w->AuraFxRules.hitStop(w,1)).max().orElseThrow()*1_000_000L;
    private final Properties identity=new Properties();
    private Path directory;
    private String role,skin,angle;
    private UUID hostId,peerId;
    private long deadline,otherPid;
    private int actorEntity,observerEntity;
    private volatile long acceptedTick=Long.MIN_VALUE;
    private final ClockObservations clocks=new ClockObservations();
    private static volatile Audit current;
    private static boolean damageHooked;
    private record Spend(long tick,double paid,double expected,int rest,double momentum,boolean committed,boolean backlash){}
    private record Completion(long tick,long accepted,List<Integer> marks){}
    private record Damage(long tick,float amount,boolean hostImage,boolean peerImage,String hostWitnessSha256,String peerWitnessSha256){}
    /** Bounded observations of existing handoffs, never a clock adjustment or image receipt. */
    private final class ClockObservations {
        private static final int LIMIT=96;
        private final List<Map<String,Object>> points=new ArrayList<>();
        private final Gson gson=new Gson();
        private long started=System.nanoTime(),sequence;
        private int dropped,observationErrors;
        private String lastWait;
        private boolean firstAge;
        synchronized void clear(){points.clear();started=System.nanoTime();sequence=0;dropped=0;observationErrors=0;lastWait=null;firstAge=false;}
        private Map<String,Object> point(String phase,long tick){
            var p=new LinkedHashMap<String,Object>();p.put("sequence",++sequence);p.put("elapsedNanos",System.nanoTime()-started);
            p.put("phase",phase);p.put("thread",Thread.currentThread().getName());p.put("acceptedTick",acceptedTick);p.put("gameTick",tick);
            if(acceptedTick!=Long.MIN_VALUE)p.put("ageTicks",tick-acceptedTick);
            return p;
        }
        private void add(Map<String,Object> p){if(points.size()<LIMIT)points.add(p);else dropped++;}
        synchronized void server(String phase,long tick){
            try {
            var p=point(phase,tick);p.put("clockOwner","server");add(p);
            // Record both phases, then log after the actual write so logging cannot delay its publication.
            if(phase.endsWith("after-write"))emit("server-write-complete",List.copyOf(points),null);
            }catch(Throwable ignored){observationErrors++;}
        }
        synchronized void client(String phase,Minecraft mc,String release,Boolean held,boolean wait){
            try {
            long tick=mc.level.getGameTime();var actor=mc.level.getPlayerByUUID(hostId);var timeline=MastersArtsClient.timeline(actor);
            String source=timeline==null?"absent":timeline.toString();String key=tick+"/"+release+"/"+source;
            boolean first=wait&&!firstAge&&tick>=acceptedTick+10;
            if(wait&&key.equals(lastWait)&&!first)return;
            if(wait){phase=lastWait==null?"capture-entry":"capture-wait-change";lastWait=key;if(first)firstAge=true;}
            var p=point(phase,tick);p.put("clockOwner","client");p.put("releaseReceipt",release);p.put("timeline",source);
            p.put("actorUuid",actor==null?"absent":actor.getUUID().toString());p.put("actorEntity",actor==null?-1:actor.getId());
            p.put("observerEntity",mc.player==null?-1:mc.player.getId());p.put("firstAgeAtLeast10",first);
            if(held!=null)p.put("hitStopHolding",held);add(p);
            }catch(Throwable ignored){observationErrors++;}
        }
        synchronized void report(String phase,Throwable failure){try{emit(phase,List.copyOf(points),failure);}catch(Throwable ignored){observationErrors++;}}
        private void emit(String phase,List<Map<String,Object>> values,Throwable failure){
            var report=new LinkedHashMap<String,Object>();report.put("schemaVersion",1);report.put("phase",phase);report.put("role",role);
            report.put("pid",ProcessHandle.current().pid());report.put("nonce",identity.getProperty("nonce"));report.put("runIdentity",identity.getProperty("runIdentity"));
            report.put("sourceHead",identity.getProperty("sourceHead"));report.put("case",CASE);report.put("clockBasis","process_local_monotonic_observation");
            report.put("sourceBasis","client_game_time_and_accepted_timeline");report.put("screenshotEvidence",false);
            report.put("serverReleaseFrameCorrespondenceVerified",false);report.put("droppedObservations",dropped);report.put("observationErrors",observationErrors);report.put("observations",values);
            if(failure!=null)report.put("error",failure.toString());
            System.out.println("CRIMSON_MOON_CLOCK_DIAGNOSTIC "+gson.toJson(report));
        }
    }
    private final class Audit implements AutoCloseable {
        Mob target;
        final List<Spend> spends=new CopyOnWriteArrayList<>();
        final List<Completion> completions=new CopyOnWriteArrayList<>();
        final List<Damage> damage=new CopyOnWriteArrayList<>();
        volatile Throwable failure;
        final AuraApi.SpendHook spend=(p,paid,reason,backlash)->{
            if(p.getUUID().equals(hostId)&&reason.equals("art:"+ART)){
                var art=AuraApi.artOf(p,ART).orElseThrow();
                spends.add(new Spend(p.level().getGameTime(),paid,SwordStrings.price(p,art),SwordStrings.rest(p,art),Momentum.value(p),MastersArts.committed(p),backlash));
                check(spends.size()==1,"Only one real Final is admitted");acceptedTick=p.level().getGameTime();
                clocks.server("accepted-before-write",p.level().getGameTime());write("case-00-accepted",action());clocks.server("accepted-after-write",p.level().getGameTime());
            }
        };
        final AuraApi.StringHook done=(p,art,receipt)->{
            if(p.getUUID().equals(hostId)&&art.id().equals(ART)){
                var completion=new Completion(p.level().getGameTime(),receipt.at(),List.copyOf(receipt.marks()));completions.add(completion);
                check(completions.size()==1&&completion.accepted()==acceptedTick&&CrimsonMoonRenderMath.releaseAt(acceptedTick,completion.tick()),"Actual completion is exactly accepted+10");
                check(completion.marks().size()==4&&completion.marks().subList(0,3).stream().allMatch(SwordString.Token.FULL::fits)&&SwordString.Token.LOW.fits(completion.marks().getLast()),"Actual FULL/FULL/FULL/LOW completion");
                var fields=action();fields.put("releaseTick",Long.toString(completion.tick()));fields.put("completionOffset","10");fields.put("marks","FULL,FULL,FULL,LOW");
                clocks.server("release-before-write",p.level().getGameTime());write("case-00-release",fields);clocks.server("release-after-write",p.level().getGameTime());
            }
        };
        Audit(){check(current==null,"Moon audits do not overlap");current=this;
            if(!damageHooked){ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked)->{
                var audit=current;if(audit!=null&&entity==audit.target&&!audit.spends.isEmpty()&&source.is(Aura.DAMAGE)&&source.getEntity() instanceof ServerPlayer p&&p.getUUID().equals(audit.owner()))audit.hit(p.level().getGameTime(),taken);
            });damageHooked=true;}
            AuraApi.onSpend(spend);AuraApi.onString(done);
        }
        UUID owner(){return hostId;}
        void hit(long tick,float amount){
            try {
                boolean host=exists("host-case-00-observed"),peer=exists("peer-case-00-observed");
                String hostHash="missing",peerHash="missing";
                if(host){var value=read("host-case-00-observed","host");checkAction(value);hostHash=fileSha("host-case-00-observed");}
                if(peer){var value=read("peer-case-00-observed","peer");checkAction(value);peerHash=fileSha("peer-case-00-observed");}
                damage.add(new Damage(tick,amount,host,peer,hostHash,peerHash));
            }catch(Throwable problem){failure=problem;}
        }
        @Override public void close(){current=null;AuraApi.spendHooks().remove(spend);AuraApi.stringHooks().remove(done);}
    }
    @Override public void runTest(ClientGameTestContext context){OpeningCaptureWait.withCleanup(()->run(context));}
    private void run(ClientGameTestContext c){
        configure();
        clocks.clear();
        String[] previous=Arrays.stream(SETTINGS).map(System::getProperty).toArray(String[]::new);
        var camera=c.computeOnClient(mc->mc.options.getCameraType());var hand=c.computeOnClient(mc->mc.options.mainHand().get());
        int width=c.computeOnClient(mc->mc.getWindow().getScreenWidth()),height=c.computeOnClient(mc->mc.getWindow().getScreenHeight()),gui=c.computeOnClient(mc->mc.options.guiScale().get());
        boolean hidden=c.computeOnClient(mc->mc.gui.hud.isHidden()),fullscreen=c.computeOnClient(mc->mc.options.fullscreen().get()),toggle=c.computeOnClient(mc->mc.options.toggleCrouch().get());
        try {
            for(String setting:SETTINGS)System.setProperty(setting,"true");
            c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(3);mc.resizeGui();mc.options.toggleCrouch().set(false);mc.options.mainHand().set(HumanoidArm.RIGHT);mc.options.setCameraType(CameraType.FIRST_PERSON);if(mc.gui.hud.isHidden())mc.gui.hud.toggle();});
            if(role.equals("host"))host(c);else peer(c);
        }catch(Throwable failure){clocks.report("failure",failure);if(!exists(role+"-failure"))write(role+"-failure",Map.of("error",failure.toString()));throw failure;}
        finally {
            try {
            c.getInput().releaseKey(o->o.keyAttack);c.getInput().releaseKey(o->o.keyShift);CrimsonMoonRenderProbe.unwatch();HitStop.clear();
            for(int i=0;i<SETTINGS.length;i++){if(previous[i]==null)System.clearProperty(SETTINGS[i]);else System.setProperty(SETTINGS[i],previous[i]);}
            c.runOnClient(mc->{mc.options.setCameraType(camera);mc.options.mainHand().set(hand);mc.options.broadcastOptions();mc.options.toggleCrouch().set(toggle);mc.getWindow().setWindowed(width,height);mc.getWindow().setFullscreen(fullscreen);mc.options.guiScale().set(gui);mc.resizeGui();if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();});
            }finally{clocks.clear();}
        }
    }
    private void configure(){
        role=required("role");check(role.equals("host")||role.equals("peer"),"Explicit supervised role");
        directory=Path.of(required("directory")).toAbsolutePath().normalize();check(Files.isDirectory(directory),"Supervisor creates a fresh bounded IPC directory");
        for(String key:IDENTITY){String value=key.equals("prHeadSha")?System.getProperty("wildercord.mp.prHeadSha"):required(key);check(value!=null,"Explicit provenance: "+key);identity.setProperty(key,value);}
        check("moon".equals(identity.getProperty("suite")),"Only the supervised Moon suite admits this entrypoint");
        check(identity.getProperty("sourceHead").matches("[a-f0-9]{40}")&&identity.getProperty("checkoutSha").equals(identity.getProperty("sourceHead"))&&identity.getProperty("descriptorSha256").matches("[a-f0-9]{64}"),"Exact checkout and descriptor provenance");
        check(identity.getProperty("prHeadSha").isEmpty()||identity.getProperty("prHeadSha").matches("[a-f0-9]{40}"),"Optional separate PR-head association");
        String nonce=identity.getProperty("nonce");check(UUID.fromString(nonce).toString().equals(nonce)&&nonce.equals(System.getenv("WILDERCORD_MOON_RECEIPT_NONCE")),"Both native images use the supervisor nonce");
        skin=moon("expectedSkin");angle=moon("observerAngle");check(CASE.equals(moon("case")),"Only the fixed release slice is accepted");
        check(List.of("wide","slim").contains(skin)&&List.of("front_oblique","reverse_oblique").contains(angle),"Bounded skin/angle matrix");
        hostId=UUID.fromString(identity.getProperty("hostUuid"));peerId=UUID.fromString(identity.getProperty("peerUuid"));
        check(hostId.equals(UUID.fromString(skin.equals("wide")?"efc39378-3f97-37a3-bf28-b62b82f0bafa":"0167ff94-ec08-3235-8727-7c1376ad1e7b"))&&peerId.equals(UUID.fromString("315e5fc2-3894-3190-83b4-ab47825b72ac")),"Fixed original offline profile UUIDs");
        identity.setProperty("serverReleaseFrameCorrespondenceVerified","false");
        identity.setProperty("case",CASE);identity.setProperty("cases",CASE);identity.setProperty("expectedSkin",skin);identity.setProperty("observerAngle",angle);
        int seconds=Integer.parseInt(required("timeoutSeconds"));check(CrimsonMoonMultiplayerProof.boundedTimeout(seconds),"Moon supervisor budget must be 60–180 seconds");deadline=System.nanoTime()+(seconds-10L)*1_000_000_000L;
    }
    private void host(ClientGameTestContext c){
        var props=new Properties();props.setProperty("server-ip","127.0.0.1");props.setProperty("server-port",Integer.toString(localPort()));props.setProperty("max-players","2");props.setProperty("white-list","false");props.setProperty("online-mode","false");props.setProperty("enforce-secure-profile","false");props.setProperty("view-distance","5");props.setProperty("simulation-distance","5");
        try(var server=c.worldBuilder().createServer(props);var connection=server.connect();var audit=new Audit()){
            connection.waitForChunksDownload();connection.waitForClientboundPackets();
            check(c.computeOnClient(mc->mc.player.getUUID().equals(hostId)&&mc.player.getGameProfile().name().equals(skin.equals("wide")?"WCMoonWide2":"WCMoonSlim")),"Actual host fixed profile");
            write("host-ready",Map.of("port",Integer.toString(server.computeOnServer(s->s.getPort()))));
            await(c,()->exists("peer-connected")&&server.computeOnServer(s->s.getPlayerList().getPlayers().size()==2&&s.getPlayerList().getPlayers().stream().allMatch(p->p.connection.hasClientLoaded())),"Both actual TCP clients load");
            otherPid=Long.parseLong(read("peer-connected","peer").getProperty("pid"));check(otherPid>0&&otherPid!=ProcessHandle.current().pid(),"Distinct actual JVM/PID");
            for(String command:List.of("gamerule spawn_mobs false","gamerule advance_time false","time set 3000","weather clear","fill -10 99 -10 10 99 10 minecraft:stone_bricks","fill -10 100 -10 10 108 10 minecraft:air"))server.runCommand(command);
            server.runOnServer(s->{
                var host=s.getPlayerList().getPlayer(hostId);var peer=s.getPlayerList().getPlayer(peerId);
                check(host!=null&&peer!=null&&host.connection.player==host&&peer.connection.player==peer,"Exact current PlayerList identities");check(!tcp(host).equals(tcp(peer)),"Distinct real loopback TCP endpoints");
                actorEntity=host.getId();observerEntity=peer.getId();prepare(host,peer,audit);
            });
            write("case-00-ready",identities());
            c.runOnClient(mc->{mc.gui.setScreen(null);mc.options.broadcastOptions();mc.player.setYRot(0);mc.player.setXRot(3);CrimsonMoonRenderProbe.watch(hostId);});
            await(c,()->exists("peer-capture-ready"),"Actual observer takes its ordinary first-person position");read("peer-capture-ready","peer");c.waitTicks(15);
            int requests=c.computeOnClient(mc->SwordStringsClient.counts()[0]);
            for(int i=0;i<3;i++){
                c.waitFor(mc->mc.player.getAttackStrengthScale(0)>=.999F,40);c.getInput().pressKey(o->o.keyAttack);c.waitTicks(2);boolean last=i==2;
                server.runOnServer(s->{audit.target.snapTo(.5,100,last?5.6:3.1,180,0);audit.target.setDeltaMovement(Vec3.ZERO);if(last){var host=s.getPlayerList().getPlayer(hostId);host.setAttached(AuraAttachments.AURA,Aura.data(host).withAura(AuraRules.capacity(AuraRules.SOVEREIGN)));}});c.waitTicks(12);
            }
            check(c.computeOnClient(mc->SwordStringsClient.chain().size()>=3),"Three actual fully charged attacks precede LOW");
            server.runOnServer(s->{var host=s.getPlayerList().getPlayer(hostId);check(Momentum.peak(host)&&AuraApi.artOf(host,ART).orElseThrow().condition().met(host),"Ordinary server Final admission remains open");});
            c.getInput().holdKey(o->o.keyShift);c.waitTicks(2);check(c.computeOnClient(mc->Momentum.peak(mc.player)&&AuraApi.artOf(mc.player,ART).orElseThrow().condition().met(mc.player)),"Actual synced peak at LOW");
            c.getInput().pressKey(o->o.keyAttack);c.getInput().releaseKey(o->o.keyShift);
            c.waitFor(mc->MastersArtsClient.timeline(mc.player)!=null&&MastersArtsClient.timeline(mc.player).move()==19,35);
            c.runOnClient(mc->{var accepted=MastersArtsClient.timeline(mc.player);clocks.client("timeline-ready",mc,"not_checked",null,false);check(accepted.startTick()==acceptedTick&&accepted.entity()==actorEntity&&accepted.windup()==10&&accepted.recovery()==20,"Client received this same actual accepted timeline");check(SwordStringsClient.counts()[0]==requests+1&&SwordStringsClient.lastAsked().equals(ART),"Exactly one real reader request names Moon");});
            capture(c);
            c.waitFor(mc->mc.level.getGameTime()>=acceptedTick+34,45);
            server.runOnServer(s->{
                var host=s.getPlayerList().getPlayer(hostId);check(audit.failure==null,"Damage-order callback remains healthy: "+audit.failure);
                check(audit.spends.size()==1&&audit.completions.size()==1&&!audit.damage.isEmpty(),"One paid accepted Final releases and damages its real target");
                var spend=audit.spends.getFirst();var done=audit.completions.getFirst();var damage=audit.damage.getFirst();
                check(spend.tick()==acceptedTick&&spend.paid()>0&&Math.abs(spend.paid()-spend.expected())<.0001&&spend.committed()&&!spend.backlash()&&spend.momentum()>=95,"Actual paid admission retains the declared legitimate peak gate");
                check(spend.rest()==600&&SwordStrings.readyAt(host,ART)==acceptedTick+600&&!MastersArts.committed(host),"Original 600-tick rest and 30-tick recovery are untouched");
                check(damage.amount()>0&&CrimsonMoonMultiplayerProof.damageAfterBothImages(acceptedTick,done.tick(),damage.tick(),damage.hostImage(),damage.peerImage()),"Unproved release/image/damage order: both completed native images must precede the genuine first direct damage at ages11–15");
                var hostImage=read("host-case-00-observed","host");var peerImage=read("peer-case-00-observed","peer");checkAction(hostImage);checkAction(peerImage);
                check(damage.hostWitnessSha256().equals(fileSha("host-case-00-observed"))&&damage.peerWitnessSha256().equals(fileSha("peer-case-00-observed")),"Before-damage witnesses are the unchanged final image receipts");
                var passed=action();passed.put("releaseTick",Long.toString(done.tick()));passed.put("completionOffset","10");passed.put("firstDamageTick",Long.toString(damage.tick()));passed.put("firstDamageAgeTicks",Long.toString(damage.tick()-acceptedTick));passed.put("firstDamageDelayTicks",Long.toString(damage.tick()-done.tick()));
                passed.put("hostObservedBeforeFirstDamage","true");passed.put("peerObservedBeforeFirstDamage","true");passed.put("hostObservedWitnessSha256",damage.hostWitnessSha256());passed.put("peerObservedWitnessSha256",damage.peerWitnessSha256());passed.put("releaseImageDamageOrderVerified","true");passed.put("serverReleaseFrameCorrespondenceVerified","false");
                for(String key:List.of("pngSha256","callbackPixelSha256","receiptSha256")){passed.put("host"+Character.toUpperCase(key.charAt(0))+key.substring(1),hostImage.getProperty(key));passed.put("peer"+Character.toUpperCase(key.charAt(0))+key.substring(1),peerImage.getProperty(key));}
                write("case-00-passed",passed);audit.target.discard();audit.target=null;
            });
            write("disconnect-peer",action());
            await(c,()->exists("peer-disconnected")&&exists("peer-moon-passed")&&server.computeOnServer(s->s.getPlayerList().getPlayer(peerId)==null),"Real peer departs after complete image/action proof");
            var peerPassed=read("peer-moon-passed","peer");checkAction(peerPassed);checkOrder(peerPassed);checkOrder(read("peer-disconnected","peer"));write("host-moon-passed",terminal(read("case-00-passed","host")));
        }
    }
    private void peer(ClientGameTestContext c){
        await(c,()->exists("host-ready"),"Host publishes bounded TCP server");var ready=read("host-ready","host");otherPid=Long.parseLong(ready.getProperty("pid"));check(otherPid>0&&otherPid!=ProcessHandle.current().pid(),"Distinct host JVM/PID");
        int port=Integer.parseInt(ready.getProperty("port"));check(port>0&&port<=65535,"Actual loopback port");String address="127.0.0.1:"+port;
        try {
            c.runOnClient(mc->ConnectScreen.startConnecting(mc.gui.screen(),mc,ServerAddress.parseString(address),new ServerData("Connected Crimson Moon capture",address,ServerData.Type.OTHER),false,null));
            c.waitFor(mc->mc.player!=null&&mc.level!=null&&mc.getConnection()!=null,2400);check(c.computeOnClient(mc->mc.player.getUUID().equals(peerId)&&mc.player.getGameProfile().name().equals("WCAuraPeer")),"Actual distinct fixed peer profile");write("peer-connected",Map.of());
            await(c,()->exists("case-00-ready"),"Server positions the actual observer");var command=read("case-00-ready","host");actorEntity=Integer.parseInt(command.getProperty("actorEntity"));observerEntity=Integer.parseInt(command.getProperty("observerEntity"));
            c.waitFor(mc->mc.player.getId()==observerEntity&&mc.level.getPlayerByUUID(hostId)!=null&&mc.level.getPlayerByUUID(hostId).getId()==actorEntity&&Math.abs(mc.player.getX()-(angle.equals("front_oblique")?4.5:-3.5))<.01&&Math.abs(mc.player.getZ()-(angle.equals("front_oblique")?5.5:-4.5))<.01,240);
            c.runOnClient(mc->{mc.gui.setScreen(null);mc.options.broadcastOptions();mc.player.setYRot(observerYaw());mc.player.setXRot(7);CrimsonMoonRenderProbe.watch(hostId);});write("peer-capture-ready",identities());
            await(c,()->exists("case-00-accepted"),"Actual paid accepted Final");var accepted=read("case-00-accepted","host");acceptedTick=Long.parseLong(accepted.getProperty("acceptedTick"));checkAction(accepted);
            c.waitFor(mc->{var actor=mc.level.getPlayerByUUID(hostId);var timeline=MastersArtsClient.timeline(actor);boolean timelineReady=timeline!=null&&timeline.entity()==actorEntity&&timeline.move()==19&&timeline.startTick()==acceptedTick&&timeline.windup()==10&&timeline.recovery()==20;if(timelineReady)clocks.client("timeline-ready",mc,"not_checked",null,false);return timelineReady;},35);
            capture(c);
            await(c,()->exists("case-00-passed"),"Host proves both images precede first direct damage");var passed=read("case-00-passed","host");checkAction(passed);checkOrder(passed);
            await(c,()->exists("disconnect-peer"),"Native assertions complete before departure");checkAction(read("disconnect-peer","host"));
            c.runOnClient(mc->mc.disconnect(new TitleScreen(),false));c.waitFor(mc->mc.player==null&&mc.level==null,300);
            var terminal=terminal(passed);write("peer-disconnected",terminal);write("peer-moon-passed",terminal);
            await(c,()->exists("host-moon-passed"),"Host observes actual connection removal");var hostPassed=read("host-moon-passed","host");checkAction(hostPassed);checkOrder(hostPassed);
        }finally{c.runOnClient(mc->{if(mc.level!=null)mc.disconnect(new TitleScreen(),false);});}
    }
    private void capture(ClientGameTestContext c){
        try {
        c.waitFor(mc->{boolean age=mc.level.getGameTime()>=acceptedTick+10;boolean released=age&&exists("case-00-release");clocks.client("capture-wait",mc,age?Boolean.toString(released):"not_checked",null,true);return age&&released;},45);
        var snapshot=c.computeOnClient(mc->{boolean held=HitStop.holding();var value=new OpeningCaptureWait.Snapshot(new OpeningCaptureWait.Identity(hostId,actorEntity,actorEntity,19,acceptedTick,mc.level.getGameTime()),held,System.nanoTime()+(held?MAX_HOLD:0));clocks.client("capture-snapshot",mc,"true",held,false);return value;});OpeningCaptureWait.await(snapshot);
        String view=role.equals("host")?"fp":"remote";
        String name=CrimsonMoonRenderProbe.PREFIX+view+"_"+skin+"_"+angle+"_"+CASE;
        var release=read("case-00-release","host");checkAction(release);
        var expectedRelease=new HashMap<String,String>();for(String key:identity.stringPropertyNames())expectedRelease.put(key,identity.getProperty(key));expectedRelease.putAll(action());expectedRelease.put("role","host");expectedRelease.put("pid",Long.toString(role.equals("host")?ProcessHandle.current().pid():otherPid));
        c.runOnClient(mc->{
            var actor=mc.level.getPlayerByUUID(hostId);check(actor instanceof net.minecraft.client.player.AbstractClientPlayer connectedActor&&connectedActor.getSkin().model().getSerializedName().equals(skin),"Actual unchanged connected skin width");
            var readyIdentity=new OpeningCaptureWait.Identity(hostId,actorEntity,actorEntity,19,acceptedTick,mc.level.getGameTime());boolean held=HitStop.holding();
            try{OpeningCaptureWait.requireReady(snapshot,readyIdentity,held);}finally{clocks.client("capture-ready",mc,"true",held,false);}
            check(!HitStop.holding()&&CrimsonMoonRenderMath.age(10,mc.level.getGameTime()-acceptedTick),"Unretimed release source remains in [10,11)");
            clocks.client("capture-arm",mc,"true",false,false);
            CrimsonMoonRenderProbe.armPaired(new CrimsonMoonRenderProbe.Expected(name,actorEntity,hostId.toString(),acceptedTick,"articulated",view,"RIGHT",skin,true,true,"ACTIVE",10),
                new CrimsonMoonRenderProbe.Paired(role,peerId.toString(),observerEntity,angle,directory.resolve("case-00-release.properties"),expectedRelease));
        });
        CrimsonMoonRenderProbe.Artifact artifact;
        try{c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDeltaTicks(0));artifact=CrimsonMoonRenderProbe.requireArtifact(name);}
        finally{CrimsonMoonRenderProbe.disarm(name);}
        var report=artifact.report();check(report.scopeCleanupVerified()&&report.releaseObservedBeforeSource(),"Actual extraction follows release receipt; screenshot scope fully restores");
        var fields=action();fields.put("view",view);fields.put("screenshotName",name);fields.put("pngSha256",report.image().pngSha256());fields.put("callbackPixelSha256",report.callbackPixels().sha256());fields.put("receiptSha256",artifact.sha256());
        Path game=FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize(),receipt=artifact.receipt().toAbsolutePath().normalize();check(receipt.startsWith(game),"Report is under this role's actual game directory");
        fields.put("receiptRelativePath",game.relativize(receipt).toString());fields.put("pngRelativePath",report.image().relativeImagePath());fields.put("actualSourceAge",report.copy().observations().get("actualSourceAge"));fields.put("releaseReceiptSha256",report.copy().observations().get("releaseReceiptSha256"));
        fields.put("releaseImageDamageOrderVerified","false");fields.put("releaseObservedBeforeSource","true");fields.put("scopeCleanupVerified","true");fields.put("serverReleaseFrameCorrespondenceVerified","false");fields.put("pixelQualityReviewed","false");
        write(role+"-case-00-observed",fields);
        }finally{clocks.report("capture-exit",null);}
    }
    private void prepare(ServerPlayer host,ServerPlayer peer,Audit audit){
        check(host.level().getGameTime()>=SwordStrings.readyAt(host,ART),"Fresh action respects real individual rest");
        host.setGameMode(GameType.SURVIVAL);host.teleportTo(host.level(),.5,100,.5,Set.<Relative>of(),0,3,false);host.setDeltaMovement(Vec3.ZERO);host.removeAllEffects();host.setHealth(host.getMaxHealth());host.getFoodData().setFoodLevel(20);
        host.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));host.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
            var armor=new ItemStack(switch(slot){case HEAD->Items.NETHERITE_HELMET;case CHEST->Items.NETHERITE_CHESTPLATE;case LEGS->Items.NETHERITE_LEGGINGS;default->Items.NETHERITE_BOOTS;});
            armor.enchant(host.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),4);host.setItemSlot(slot,armor);
        }
        host.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("crimson",AuraRules.SOVEREIGN,AuraRules.threshold(AuraRules.SOVEREIGN),AuraRules.capacity(AuraRules.SOVEREIGN),0));
        // Declared fixture peak; admission, reader inputs, costs, recovery and rest remain production-owned.
        host.setAttached(Momentum.MOMENTUM,new Momentum.State(100,host.level().getGameTime()+300,0,0,0));host.inventoryMenu.broadcastChanges();
        var art=AuraApi.artOf(host,ART).orElseThrow();var style=MastersStyleRules.of(ART);check(AuraApi.ArtSlot.of(art).orElseThrow()==AuraApi.ArtSlot.FINAL&&art.string().text().equals("full full full low")&&art.condition().met(host)&&style.animation()==19&&style.windup()==10&&style.recovery()==20,"Actual Crimson Sovereign Final and ordinary gate");
        peer.setGameMode(GameType.SURVIVAL);peer.teleportTo(peer.level(),angle.equals("front_oblique")?4.5:-3.5,100,angle.equals("front_oblique")?5.5:-4.5,Set.<Relative>of(),observerYaw(),7,false);peer.setDeltaMovement(Vec3.ZERO);peer.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);peer.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);peer.inventoryMenu.broadcastChanges();
        var target=EntityTypes.HUSK.create(host.level(),EntitySpawnReason.COMMAND);check(target!=null,"Actual live target exists");target.addTag("wildercord.rolled");target.setNoAi(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);target.setHealth(200);target.snapTo(.5,100,3.1,180,0);host.level().addFreshEntity(target);audit.target=target;
    }
    private float observerYaw(){return angle.equals("front_oblique")?141.3402F:-38.6598F;}
    private Map<String,String> identities(){return new HashMap<>(Map.of("actorEntity",Integer.toString(actorEntity),"observerEntity",Integer.toString(observerEntity),"actorUuid",hostId.toString(),"observerUuid",peerId.toString()));}
    private Map<String,String> action(){var fields=identities();fields.put("acceptedTick",Long.toString(acceptedTick));fields.put("move","19");fields.put("windup","10");fields.put("recovery","20");return fields;}
    private Map<String,String> terminal(Properties passed){
        checkAction(passed);checkOrder(passed);var fields=action();
        for(String key:List.of("releaseTick","firstDamageTick","firstDamageAgeTicks","firstDamageDelayTicks","releaseImageDamageOrderVerified","serverReleaseFrameCorrespondenceVerified"))fields.put(key,passed.getProperty(key));
        return fields;
    }
    private void checkOrder(Properties value){
        check("true".equals(value.getProperty("releaseImageDamageOrderVerified"))&&"false".equals(value.getProperty("serverReleaseFrameCorrespondenceVerified")),
            "Causal release/image/damage order is proven; exact server release-frame correspondence remains unproved");
        check(CrimsonMoonMultiplayerProof.orderClaim(Long.parseLong(value.getProperty("acceptedTick")),Long.parseLong(value.getProperty("releaseTick")),
            Long.parseLong(value.getProperty("firstDamageTick")),Long.parseLong(value.getProperty("firstDamageAgeTicks")),Long.parseLong(value.getProperty("firstDamageDelayTicks")),
            Boolean.parseBoolean(value.getProperty("releaseImageDamageOrderVerified")),Boolean.parseBoolean(value.getProperty("serverReleaseFrameCorrespondenceVerified"))),
            "Acceptance-relative age and release-relative delay match the actual server ticks");
    }
    private void checkAction(Properties value){for(var field:action().entrySet())check(field.getValue().equals(value.getProperty(field.getKey())),"Same accepted action: "+field.getKey());}
    private void await(ClientGameTestContext c,BooleanSupplier condition,String reason){while(System.nanoTime()<deadline){check(!exists("host-failure")&&!exists("peer-failure"),"Both native processes remain healthy: "+reason);if(condition.getAsBoolean())return;c.waitTicks(1);}throw new AssertionError("Finite native handshake expired: "+reason);}
    private boolean exists(String name){return Files.isRegularFile(directory.resolve(name+".properties"));}
    private Properties read(String name,String expectedRole){try(var input=Files.newInputStream(directory.resolve(name+".properties"))){var value=new Properties();value.load(input);for(String key:identity.stringPropertyNames())check(identity.getProperty(key).equals(value.getProperty(key)),"Native witness identity: "+key);check(expectedRole.equals(value.getProperty("role")),"Expected witness role");long pid=Long.parseLong(value.getProperty("pid"));check(pid>0&&(expectedRole.equals(role)?pid==ProcessHandle.current().pid():otherPid==0||pid==otherPid),"Exact native process identity");return value;}catch(java.io.IOException failure){throw new AssertionError(failure);}}
    private void write(String name,Map<String,String> fields){try{var value=new Properties();value.putAll(identity);for(var field:fields.entrySet()){check(!value.containsKey(field.getKey())||value.getProperty(field.getKey()).equals(field.getValue()),"No identity replacement");value.setProperty(field.getKey(),field.getValue());}value.setProperty("role",role);value.setProperty("pid",Long.toString(ProcessHandle.current().pid()));Path target=directory.resolve(name+".properties"),temporary=directory.resolve(name+".tmp");check(!Files.exists(target),"Witness written exactly once: "+name);try(var out=Files.newOutputStream(temporary,StandardOpenOption.CREATE_NEW)){value.store(out,"Bounded genuine Moon capture witness");}Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);}catch(java.io.IOException failure){throw new AssertionError(failure);}}
    private String fileSha(String name){try{return ArticulatedRenderReceipt.sha256(Files.readAllBytes(directory.resolve(name+".properties")));}catch(java.io.IOException failure){throw new AssertionError(failure);}}
    private static String required(String key){String value=System.getProperty("wildercord.mp."+key);check(value!=null&&!value.isBlank(),"Explicit supervised property: "+key);return value;}
    private static String moon(String key){String value=System.getProperty("wildercord.moon."+key);check(value!=null&&!value.isBlank(),"Explicit bounded Moon property: "+key);return value;}
    private static int localPort(){try(var socket=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){return socket.getLocalPort();}catch(java.io.IOException failure){throw new AssertionError(failure);}}
    private static InetSocketAddress tcp(ServerPlayer player){check(player.connection.getRemoteAddress() instanceof InetSocketAddress,"Actual TCP connection");var value=(InetSocketAddress)player.connection.getRemoteAddress();check(value.getAddress().isLoopbackAddress()&&value.getPort()>0,"Actual loopback endpoint");return value;}
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
