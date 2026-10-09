package dev.wildercord.cast;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.*;
import dev.wildercord.client.*;
import dev.wildercord.client.combat.*;
import dev.wildercord.client.fx.HitStop;
import dev.wildercord.gametest.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/** Eight additions after the frozen 46-case ledger. Two actual clients, one original accepted action per case. */
public final class CounterPeerPairedCases {
    public static final List<String> CASES=List.of("COUNTER_UNMOVED_CLASSIC_RIGHT","COUNTER_UNMOVED_CLASSIC_LEFT",
        "COUNTER_UNMOVED_ARTICULATED_RIGHT","COUNTER_UNMOVED_ARTICULATED_LEFT","COUNTER_NULL_PARRY_CLASSIC_RIGHT",
        "COUNTER_NULL_PARRY_CLASSIC_LEFT","COUNTER_NULL_PARRY_ARTICULATED_RIGHT","COUNTER_NULL_PARRY_ARTICULATED_LEFT");
    private static final String[] SETTINGS={ArticulatedCombat.ENABLE_PROPERTY,ArticulatedCombat.STABLE_CAMERA_PROPERTY,
        ArticulatedArmorRenderer.ENABLE_PROPERTY,ArticulatedArmorRenderer.VIEW_PROPERTY,ArticulatedAuraShellRenderer.ENABLE_PROPERTY};
    private static Session clockOwner;
    private static boolean clockRegistered;

    public void runConnectedPair(ClientGameTestContext c,TestServerContext server,UUID host,UUID peer,
        BiConsumer<String,Map<String,String>> observed,Consumer<String> completed){
        OpeningCaptureWait.withCleanup(()->{
            for(int i=0;i<CASES.size();i++){
                String id=CASES.get(i);
                c.getInput().releaseKey(o->o.keyShift);c.waitTicks(105);
                try(var session=new Session(c,id,46+i,true)){
                    check(session.host.equals(host)&&session.peer.equals(peer),"Actual supervised owner and observer");
                    var modes=server.computeOnServer(s->List.of(body(s,host).gameMode.getGameModeForPlayer(),body(s,peer).gameMode.getGameModeForPlayer()));
                    var lighting=server.computeOnServer(LifeCaptureStage.Lighting::new);
                    Audit audit=server.computeOnServer(s->{lighting.noon();LifeCaptureStage.build(body(s,host).level());
                        var actor=body(s,host);var viewer=body(s,peer);
                        actor.setGameMode(GameType.SURVIVAL);viewer.setGameMode(GameType.SURVIVAL);
                        check(actor.teleportTo(actor.level(),.5,221,.5,Set.of(),0,0,false),"Native owner fixture position before input");
                        check(viewer.teleportTo(viewer.level(),3.5,221,3.5,Set.of(),135,10,false),"Actual second player's unobstructed view before input");
                        for(var p:List.of(actor,viewer)){p.removeAllEffects();p.setAbsorptionAmount(0);p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);
                            for(var slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.HUMANOID_ARMOR)p.setItemSlot(slot,ItemStack.EMPTY);
                            p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);}
                        actor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));viewer.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                        actor.setAttached(AuraAttachments.AURA,new AuraAttachments.Data(ArtRules.art(session.art).method(),4,1800,19,0));
                        actor.inventoryMenu.broadcastChanges();viewer.inventoryMenu.broadcastChanges();
                        return new Audit(session,actor);
                    });
                    try{
                        c.waitTicks(15);session.arm();
                        session.write("prepare",session.fields());session.alignHost(server);
                        session.await(()->session.exists("armed"),"Independent peer is armed before real guard input");
                        var armed=session.read("armed","peer");session.requireFields(armed,session.fields());
                        check("true".equals(armed.getProperty("cameraNative"))&&session.skin.equals(armed.getProperty("skin")),"Peer binds same original skin and own camera");
                        check(session.readySha.equals(armed.getProperty("clockReadySha256")),"This exact natural clock barrier precedes peer arming");
                        session.armedSha=session.sha("armed");
                        c.getInput().holdKey(o->o.keyShift);c.waitTicks(2);c.getInput().pressKey(WildercordKeys.auraMapping());
                        for(int t=0;t<4&&!server.computeOnServer(s->AuraGuard.perfectNow(body(s,host)));t++)c.waitTicks(1);
                        server.runOnServer(s->audit.catchBlow());
                        c.waitFor(mc->SwordString.Token.COUNTER.fits(SwordStringsClient.cueMarks(mc.level.getGameTime())),8);
                        server.runOnServer(s->audit.armAdmission());
                        c.runOnClient(mc->audit.admission.bindClient(mc));
                        int requests=c.computeOnClient(mc->SwordStringsClient.counts()[0]);
                        c.getInput().pressKey(o->o.keyAttack);c.getInput().releaseKey(o->o.keyShift);
                        c.runOnClient(mc->check(SwordStringsClient.counts()[0]==requests+1&&session.art.equals(SwordStringsClient.lastAsked()),"One genuine ordinary attack requests the earned counter"));
                        try{session.waitInitial(()->server.computeOnServer(s->audit.admission.ready()));}
                        finally{server.runOnServer(s->audit.publishAdmission());}
                        server.runOnServer(s->audit.publishAccepted());
                        session.captureAll();
                        for(String phase:CounterPeerPhaseContract.PHASES){session.await(()->session.existsRole("peer",phase),"Peer captures actual "+phase);session.verifyObserved(session.readRole("peer",phase),phase);}
                        c.waitTicks(30);server.runOnServer(s->audit.verify());
                        var proof=session.fields();proof.remove("case");proof.remove("actorEntity");proof.put("acceptedTick",Long.toString(session.accepted));
                        proof.putAll(server.computeOnServer(s->audit.outcome()));
                        observed.accept(id,Map.copyOf(proof));completed.accept(id);
                    }finally{server.runOnServer(s->{audit.close();lighting.close();body(s,host).setGameMode(modes.get(0));body(s,peer).setGameMode(modes.get(1));});}
                }
            }
        });
    }

    /** Invoked in the separate connected peer before the old post-outcome ready/seen exchange. */
    public static Map<String,String> capturePeer(ClientGameTestContext c,String id,int index){
        var result=new HashMap<String,String>();
        OpeningCaptureWait.withCleanup(()->{
            try(var session=new Session(c,id,index,false)){
                session.waitUnpaced(()->session.exists("prepare"),"Owner prepares the actual actor and stage");
                var prepare=session.read("prepare","host");
                c.waitFor(mc->mc.player!=null&&mc.level.getPlayerByUUID(session.host)!=null
                    &&Integer.toString(mc.player.getId()).equals(prepare.getProperty("observerEntity"))
                    &&Integer.toString(mc.level.getPlayerByUUID(session.host).getId()).equals(prepare.getProperty("actorEntity")),120);
                session.arm();session.requireFields(prepare,session.fields());session.alignPeer();
                var armed=session.fields();armed.put("clockReadySha256",session.readySha);session.write("armed",armed);session.armedSha=session.sha("armed");
                session.waitSource(()->session.exists("accepted"),40);
                var accepted=session.read("accepted","host");session.accepted=Long.parseLong(accepted.getProperty("acceptedTick"));
                session.requireFields(accepted,session.fields());check(session.armedSha.equals(accepted.getProperty("armedSha256")),"The real acceptance follows this exact pre-input peer arm");
                session.captureAll();result.putAll(session.fields());result.put("acceptedTick",Long.toString(session.accepted));
            }
        });return Map.copyOf(result);
    }

    public static void finishPeerSource(){CounterPeerRenderProbe.assertHealthy();CounterPeerRenderProbe.unwatch();}

    private static final class Audit implements AutoCloseable {
        final Session session;final ServerPlayer actor;final Mob attacker;final AuraApi.SpendHook spend;final AuraApi.StringHook complete;
        CounterPeerAdmissionProbe.Watch admission;
        AuraGuard.Caught caught;int payments,completions,hits;long paidTick,released,hitTick;double paid;long rest;Object hitAction;
        final class Foe extends net.minecraft.world.entity.monster.zombie.Husk {
            Foe(net.minecraft.server.level.ServerLevel level){super(EntityTypes.HUSK,level);}
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){
                float before=getHealth();boolean result=super.hurtServer(level,source,amount);var art=SwordStrings.performing();
                if(source.is(Aura.DAMAGE)&&source.getEntity()==actor&&art!=null&&art.id().equals(session.art)&&CounterHitCapture.direct(actor,session.art,this)){
                    check(result&&getHealth()<before,"Original hostile takes actual direct counter damage");
                    hitAction=CounterHitCapture.action(actor,session.art,this,hitAction);check(hitAction!=null,"Exact native accepted Hits action");hits++;hitTick=level.getGameTime();
                }return result;
            }
        }
        Audit(Session session,ServerPlayer actor){this.session=session;this.actor=actor;
            attacker=new Foe(actor.level());
            attacker.addTag("wildercord.rolled");attacker.setNoAi(true);attacker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);attacker.setHealth(200);
            attacker.snapTo(.5,221,2.1,180,0);check(actor.level().addFreshEntity(attacker),"Original hostile body is resident before all input");
            spend=(p,amount,reason,backlash)->{if(p!=actor||!reason.equals("art:"+session.art))return;
                check(caught!=null&&AuraGuard.caught(actor)==caught&&caught.attacker()==attacker&&!backlash&&++payments==1,"One payment retains the authentic original catch");
                var art=AuraApi.artOf(actor,session.art).orElseThrow();check(Math.abs(amount-SwordStrings.price(actor,art))<.0001,"Normal counter price");
                paidTick=actor.level().getGameTime();paid=amount;rest=paidTick+SwordStrings.rest(actor,art);
            };
            complete=(p,art,receipt)->{if(p!=actor||!art.id().equals(session.art))return;
                var release=MastersArts.earnedCounter(actor);check(release!=null&&release.target()==attacker&&release.caughtDamage()==caught.damage(),"Release retains the same caught hostile without repositioning it");
                check(receipt.at()==paidTick&&receipt.marks().size()==1&&SwordString.Token.COUNTER.fits(receipt.marks().getFirst()),"Completion is the real accepted counter receipt");
                released=actor.level().getGameTime();check(++completions==1&&released==paidTick+session.windup,"One ordinary fixed-tick release");};
            AuraApi.onSpend(spend);AuraApi.onString(complete);
        }
        void catchBlow(){check(AuraGuard.perfectNow(actor)&&AuraGuard.caught(actor)==null,"Actual Aura-key perfect guard precedes genuine hostile melee");
            float health=actor.getHealth();attacker.doHurtTarget(actor.level(),actor);caught=AuraGuard.caught(actor);
            check(caught!=null&&caught.attacker()==attacker&&caught.damage()>0&&actor.getHealth()==health,"Native damage path earns the original catch");
            // Deliberately retain natural stagger, position and velocity. No correction after gameplay starts.
        }
        void armAdmission(){admission=CounterPeerAdmissionProbe.arm(session.identity.getProperty("nonce")+":"+session.stem,session.armedSha,actor,session.art,caught,session.move,session.windup,session.recovery);}
        void publishAdmission(){var fields=session.fields();fields.putAll(admission.fields());session.write("admission",fields);}
        void publishAccepted(){admission.requireAccepted();check(actor.level().getGameTime()>=paidTick&&actor.level().getGameTime()-paidTick<session.windup,"Original WINDUP cannot be missed while awaiting native admission");check(payments==1&&MastersArts.committed(actor)&&session.armedSha!=null,"Actual paid commitment follows peer readiness");
            session.accepted=paidTick;var fields=session.fields();fields.putAll(admission.fields());fields.put("admissionReceiptSha256",session.sha("admission"));fields.put("acceptedTick",Long.toString(paidTick));fields.put("caughtTick",Long.toString(caught.at()));
            fields.put("caughtAttackerUuid",attacker.getUUID().toString());fields.put("paid",Double.toString(paid));fields.put("payments","1");fields.put("restUntil",Long.toString(rest));fields.put("clockReadySha256",session.readySha);fields.put("clockRendezvousTick",Long.toString(session.rendezvous));fields.put("armedSha256",session.armedSha);session.write("accepted",fields);
        }
        void verify(){admission.requireAccepted();CounterHitCapture.assertIdle();check(hits==1&&hitTick==released,"One real primary counter hit uses the fixed release tick");check(payments==1&&completions==1&&!MastersArts.committed(actor)&&SwordStrings.readyAt(actor,session.art)==rest,"Original paid rest and natural recovery are unchanged");
            check(actor.connection.player==actor&&body(actor.level().getServer(),session.host)==actor&&attacker.isAlive()&&attacker.getHealth()<200,"Original connected owner and caught hostile survive a genuine damaging release");}
        Map<String,String> outcome(){var fields=new HashMap<>(Map.of("directPrimaryHits",Integer.toString(hits),"primaryHitTick",Long.toString(hitTick),"releaseTick",Long.toString(released),"payments",Integer.toString(payments),"completions",Integer.toString(completions),
            "counterTargetUuid",attacker.getUUID().toString(),"counterTargetEntity",Integer.toString(attacker.getId()),"counterTargetHealthBefore","200.0","counterTargetHealth",Float.toString(attacker.getHealth())));fields.put("restUntil",Long.toString(SwordStrings.readyAt(actor,session.art)));return Map.copyOf(fields);}
        public void close(){if(admission!=null)admission.close();AuraApi.spendHooks().remove(spend);AuraApi.stringHooks().remove(complete);attacker.discard();}
    }

    private static final class Session implements AutoCloseable {
        final ClientGameTestContext c;final String id,stem,role,art,mode,hand;final int move,windup,recovery;final boolean owner;
        final UUID host,peer;final Path directory;final long deadline,otherPid;final Properties identity=new Properties();
        final String[] prior=Arrays.stream(SETTINGS).map(System::getProperty).toArray(String[]::new);
        final CameraType camera;final HumanoidArm previousHand;final boolean toggle;final CrimsonMoonClockPacing.Series clocks=new CrimsonMoonClockPacing.Series();
        int initialSourceSteps;int actorEntity,observerEntity;String skin,armedSha,readySha;long accepted=Long.MIN_VALUE,rendezvous=-1,clockSequence,lastClock=-1;boolean pacing;
        MinecraftServer server;
        Session(ClientGameTestContext c,String id,int index,boolean owner){this.c=c;this.id=id;this.owner=owner;role=owner?"host":"peer";
            check(index>=46&&index<54&&CASES.get(index-46).equals(id),"Only the eight appended counter cases");stem=String.format(Locale.ROOT,"case-%02d",index);
            art=id.contains("UNMOVED")?"unmoved":"null_parry";move=art.equals("unmoved")?24:25;windup=move==24?6:4;recovery=move==24?16:14;
            mode=id.contains("ARTICULATED")?"articulated":"classic";hand=id.endsWith("LEFT")?"LEFT":"RIGHT";
            directory=Path.of(required("directory"));for(String key:List.of("nonce","suite","sourceHead","checkoutSha","prHeadSha","descriptorSha256","hostUuid","peerUuid","runIdentity")){
                String value=System.getProperty("wildercord.mp."+key);check(value!=null,"Exact IPC provenance "+key);identity.setProperty(key,value);}
            host=UUID.fromString(required("hostUuid"));peer=UUID.fromString(required("peerUuid"));check(!host.equals(peer),"Independent profiles");
            deadline=System.nanoTime()+Math.min(120,Integer.parseInt(required("timeoutSeconds"))-10L)*1_000_000_000L;
            otherPid=Long.parseLong(readGlobal(owner?"peer-connected":"host-ready",owner?"peer":"host").getProperty("pid"));
            camera=c.computeOnClient(mc->mc.options.getCameraType());previousHand=c.computeOnClient(mc->mc.options.mainHand().get());toggle=c.computeOnClient(mc->mc.options.toggleCrouch().get());
            for(String setting:SETTINGS)System.setProperty(setting,Boolean.toString(mode.equals("articulated")));
            c.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.toggleCrouch().set(false);
                if(owner){mc.options.mainHand().set(HumanoidArm.valueOf(hand));mc.options.broadcastOptions();}
                mc.setCameraEntity(mc.player);mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});
        }
        void arm(){LifeCaptureStage.await(c,host,owner);c.runOnClient(mc->{var actor=mc.level.getPlayerByUUID(host);actorEntity=actor.getId();observerEntity=mc.level.getPlayerByUUID(peer).getId();
                check(mc.player.getUUID().equals(owner?host:peer)&&mc.getCameraEntity()==mc.player,"Same original real local camera");
                skin=((net.minecraft.client.player.AbstractClientPlayer)actor).getSkin().model().getSerializedName();
                String expected=System.getProperty("wildercord.counter.expectedSkin");check(expected==null||expected.equals(skin),"Actual original profile width must match selected fixed variant");
                check(List.of("wide","slim").contains(skin),"Supported original skin width");CounterPeerRenderProbe.watch(host,move,windup,recovery);});}
        HashMap<String,String> fields(){var fields=new HashMap<String,String>();fields.put("case",id);fields.put("actorUuid",host.toString());fields.put("actorEntity",Integer.toString(actorEntity));
            fields.put("observerUuid",peer.toString());fields.put("observerEntity",Integer.toString(observerEntity));fields.put("move",Integer.toString(move));fields.put("windup",Integer.toString(windup));
            fields.put("recovery",Integer.toString(recovery));fields.put("mode",mode);fields.put("hand",hand);if(skin!=null)fields.put("skin",skin);fields.put("cameraNative","true");return fields;}
        void alignHost(TestServerContext s){waitUnpaced(()->exists("clock-initial"),"Peer reports original pre-input clock");var initial=read("clock-initial","peer");requireFields(initial,fields());
            long peerTick=CrimsonMoonClockPacing.tick(initial.getProperty("clockClientTick"));long initialServer=s.computeOnServer(x->x.overworld().getGameTime());rendezvous=Math.max(peerTick,initialServer);
            while(!s.computeOnServer(x->CrimsonMoonClockPacing.reached(x.overworld().getGameTime(),rendezvous))){healthy();c.waitTicks(1);}
            var offer=fields();offer.put("clockRendezvousTick",Long.toString(rendezvous));offer.put("clockInitialServerTick",Long.toString(initialServer));offer.put("clockServerTick",Long.toString(rendezvous));offer.put("clockInitialSha256",sha("clock-initial"));write("clock-rendezvous",offer);offer.put("clockRendezvousSha256",sha("clock-rendezvous"));
            await(()->exists("clock-ack"),"Peer reaches actual pre-input clock");var ack=read("clock-ack","peer");requireFields(ack,offer);check(Long.parseLong(ack.getProperty("clockClientTick"))==rendezvous,"Peer matches natural server clock");
            s.runOnServer(x->{check(x.overworld().getGameTime()==rendezvous,"Server clock remains unchanged through pre-input barrier");offer.put("clockAckSha256",sha("clock-ack"));offer.put("clockClientTick",ack.getProperty("clockClientTick"));write("clock-ready",offer);readySha=sha("clock-ready");server=x;
                if(!clockRegistered){ServerTickEvents.END_SERVER_TICK.register(t->{if(clockOwner!=null&&clockOwner.server==t)clockOwner.publishClock(t.overworld().getGameTime());});clockRegistered=true;}clockOwner=this;});}
        void alignPeer(){var initial=fields();initial.put("clockClientTick",Long.toString(c.computeOnClient(mc->mc.level.getGameTime())));write("clock-initial",initial);
            await(()->exists("clock-rendezvous"),"Host holds original natural rendezvous");var offer=read("clock-rendezvous","host");requireFields(offer,fields());rendezvous=Long.parseLong(offer.getProperty("clockRendezvousTick"));
            check(sha("clock-initial").equals(offer.getProperty("clockInitialSha256")),"Initial clock receipt unchanged");while(!c.computeOnClient(mc->CrimsonMoonClockPacing.reached(mc.level.getGameTime(),rendezvous))){healthy();c.waitTicks(1);}
            var ack=fields();ack.put("clockRendezvousSha256",sha("clock-rendezvous"));ack.put("clockInitialServerTick",offer.getProperty("clockInitialServerTick"));ack.put("clockServerTick",offer.getProperty("clockServerTick"));ack.put("clockRendezvousTick",Long.toString(rendezvous));ack.put("clockInitialSha256",sha("clock-initial"));ack.put("clockClientTick",Long.toString(rendezvous));write("clock-ack",ack);
            await(()->exists("clock-ready"),"Host rechecks real clocks before inputs");var ready=read("clock-ready","host");requireFields(ready,fields());check(sha("clock-ack").equals(ready.getProperty("clockAckSha256"))&&Long.toString(rendezvous).equals(ready.getProperty("clockServerTick"))
                &&Long.toString(rendezvous).equals(ready.getProperty("clockClientTick"))&&c.computeOnClient(mc->mc.level.getGameTime())==rendezvous,"Exact unchanged native clock ack chain");readySha=sha("clock-ready");pacing=true;}
        void publishClock(long tick){check(tick>=rendezvous&&tick>=lastClock,"Actual server clock cannot roll back");lastClock=tick;
            var f=fields();f.put("clockSequence",Long.toString(++clockSequence));f.put("clockServerTick",Long.toString(tick));f.put("clockReadySha256",readySha);writeReplace("clock-latest",f);}
        void tick(){if(!pacing){c.waitTicks(1);return;}long now=c.computeOnClient(mc->mc.level.getGameTime());await(()->{
                if(!exists("clock-latest"))return false;
                var expected=fields();for(String key:identity.stringPropertyNames())expected.put(key,identity.getProperty(key));expected.put("role","host");expected.put("pid",Long.toString(otherPid));expected.put("clockReadySha256",readySha);
                try{var sample=clocks.observe(CrimsonMoonClockPacing.read(Files.readAllBytes(path("clock-latest")),expected));return now<sample.serverTick();}
                catch(java.io.IOException failure){throw new AssertionError("Original atomic server clock receipt",failure);}},"Actual server clock permits one ordinary peer tick");c.waitTicks(1);}
        void waitSource(BooleanSupplier predicate,int budget){CrimsonMoonClockPacing.waitFor(()->{healthy();return predicate.getAsBoolean();},budget,this::tick);}
        // Admission and first source share the original 12 native ticks; this never refreshes a phase/case budget.
        void waitInitial(BooleanSupplier ready){while(!ready.getAsBoolean()){healthy();check(initialSourceSteps<12,"Original admission/source native-tick budget exhausted");tick();initialSourceSteps++;}healthy();}
        void captureAll(){
            var admissionReceipt=read("admission","host");requireFields(admissionReceipt,fields());
            var acceptedReceipt=read("accepted","host");String admissionSha=sha("admission");check(admissionSha.equals(acceptedReceipt.getProperty("admissionReceiptSha256")),"Immutable original native admission receipt");
            for(String key:admissionReceipt.stringPropertyNames())check(admissionReceipt.getProperty(key).equals(acceptedReceipt.getProperty(key)),"Accepted retains original admission field "+key);
            waitInitial(()->c.computeOnClient(mc->{var actor=mc.level.getPlayerByUUID(host);var timeline=MastersArtsClient.timeline(actor);if(timeline==null)return false;
                check(timeline.entity()==actorEntity&&timeline.move()==move&&timeline.startTick()==accepted&&timeline.windup()==windup&&timeline.recovery()==recovery,"First accepted source must be this exact action; no later-packet search");CounterPeerRenderProbe.requireSource(timeline,accepted);CounterPeerAdmissionProbe.requireSource(admissionReceipt,timeline);check(CounterPeerPhaseContract.phase(move,mc.level.getGameTime()-accepted).equals("WINDUP"),"Original WINDUP remains available after native admission/source");return true;}));
            var captures=new LinkedHashMap<String,java.util.concurrent.CompletableFuture<CounterPeerRenderProbe.Artifact>>();
            for(String phase:CounterPeerPhaseContract.PHASES){int threshold=switch(phase){case "WINDUP"->Math.max(1,windup/2);case "ACTIVE"->windup;case "FOLLOW"->windup+2;default->windup+recovery/2;};
                waitSource(()->c.computeOnClient(mc->{CounterPeerRenderProbe.requireSource(MastersArtsClient.timeline(mc.level.getPlayerByUUID(host)),accepted);long age=mc.level.getGameTime()-accepted;check(!CounterPeerPhaseContract.phase(move,age).equals("NONE"),"Action cannot expire before native phases");return age>=threshold;}),30);
                var snapshot=c.computeOnClient(mc->{boolean held=HitStop.holding();return new OpeningCaptureWait.Snapshot(new OpeningCaptureWait.Identity(host,actorEntity,actorEntity,move,accepted,mc.level.getGameTime()),held,System.nanoTime()+(held?300_000_000L:0));});OpeningCaptureWait.await(snapshot);
                String name="counter_peer_"+art+"_"+mode+"_"+hand.toLowerCase(Locale.ROOT)+"_"+role+"_"+phase.toLowerCase(Locale.ROOT);
                var acceptance=read("accepted","host");requireFields(acceptance,fields());check(Long.toString(accepted).equals(acceptance.getProperty("acceptedTick")),"Single accepted action throughout capture");
                var receiptIdentity=new HashMap<String,String>();for(String key:identity.stringPropertyNames())receiptIdentity.put(key,identity.getProperty(key));receiptIdentity.putAll(fields());receiptIdentity.put("role","host");receiptIdentity.put("pid",Long.toString(owner?ProcessHandle.current().pid():otherPid));receiptIdentity.put("acceptedTick",Long.toString(accepted));receiptIdentity.put("armedSha256",armedSha);
                for(String key:admissionReceipt.stringPropertyNames())if(key.startsWith("admission"))receiptIdentity.put(key,admissionReceipt.getProperty(key));receiptIdentity.put("admissionReceiptSha256",admissionSha);
                c.runOnClient(mc->{OpeningCaptureWait.requireReady(snapshot,new OpeningCaptureWait.Identity(host,actorEntity,actorEntity,move,accepted,mc.level.getGameTime()),HitStop.holding());
                    check(CounterPeerPhaseContract.phase(move,mc.level.getGameTime()-accepted).equals(phase),"No requested-phase substitution or late capture");
                    CounterPeerRenderProbe.armPaired(new CounterPeerRenderProbe.Expected(name,actorEntity,host.toString(),accepted,mode,owner?"fp":"remote",hand,skin,false,false,phase,move,windup,recovery),
                        new CounterPeerRenderProbe.Paired(role,peer.toString(),observerEntity,path("accepted"),receiptIdentity));});
                var future=c.computeOnClient(mc->CounterPeerRenderProbe.capture(mc,name));captures.put(phase,future);
                var queued=fields();queued.put("acceptedTick",Long.toString(accepted));queued.put("phase",phase);queued.put("renderedBeforeReadback","true");
                writeGlobal(role+"-"+stem+"-"+phase+"-rendered",queued,false);
                if(owner){await(()->Files.isRegularFile(directory.resolve("peer-"+stem+"-"+phase+"-rendered.properties")),"Peer's actual native render before advancing host");
                    var rendered=readGlobal("peer-"+stem+"-"+phase+"-rendered","peer");requireFields(rendered,queued);}
            }
            var all=java.util.concurrent.CompletableFuture.allOf(captures.values().toArray(java.util.concurrent.CompletableFuture[]::new));
            c.waitFor(mc->all.isDone(),60);all.join();CounterPeerRenderProbe.assertHealthy();
            for(String phase:CounterPeerPhaseContract.PHASES){
                var artifact=captures.get(phase).join();String name=artifact.report().expected().name();
                var report=artifact.report();var f=fields();f.put("acceptedTick",Long.toString(accepted));f.put("acceptedReceiptSha256",sha("accepted"));f.put("phase",phase);f.put("view",owner?"fp":"remote");f.put("screenshotName",name);
                f.put("admissionReceiptSha256",admissionSha);f.put("admissionGeneration",admissionReceipt.getProperty("admissionGeneration"));
                f.put("actualSourceAge",report.copy().observations().get("actualSourceAge"));f.put("pngSha256",report.image().pngSha256());f.put("callbackPixelSha256",report.callbackPixels().sha256());f.put("receiptSha256",artifact.sha256());
                Path game=FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();check(artifact.receipt().toAbsolutePath().normalize().startsWith(game),"Receipt belongs to this native process game directory");
                f.put("receiptRelativePath",game.relativize(artifact.receipt().toAbsolutePath().normalize()).toString());f.put("pngRelativePath",report.image().relativeImagePath());f.put("scopeCleanupVerified",Boolean.toString(report.scopeCleanupVerified()));
                f.put("acceptanceObservedBeforeSource",Boolean.toString(report.acceptanceObservedBeforeSource()));f.put("pixelQualityReviewed","false");writeGlobal(role+"-"+stem+"-"+phase+"-observed",f,false);
            }
        }
        void verifyObserved(Properties p,String phase){requireFields(p,fields());check(Long.toString(accepted).equals(p.getProperty("acceptedTick"))&&phase.equals(p.getProperty("phase"))
            &&sha("accepted").equals(p.getProperty("acceptedReceiptSha256"))&&CounterPeerPhaseContract.phase(move,Float.parseFloat(p.getProperty("actualSourceAge"))).equals(phase)
            &&"true".equals(p.getProperty("acceptanceObservedBeforeSource"))&&"true".equals(p.getProperty("scopeCleanupVerified")),"Exact independent phase/action evidence");}
        void requireFields(Properties p,Map<String,String> expected){for(var e:expected.entrySet())check(e.getValue().equals(p.getProperty(e.getKey())),"Exact case identity "+e.getKey());}
        void healthy(){CounterPeerRenderProbe.assertHealthy();check(System.nanoTime()<deadline&&!Files.exists(directory.resolve("host-failure.properties"))&&!Files.exists(directory.resolve("peer-failure.properties")),"Bounded healthy two-client counter fixture");}
        void await(BooleanSupplier ready,String reason){OpeningCaptureWait.awaitSignal(()->{healthy();return ready.getAsBoolean();},deadline,reason);}
        void waitUnpaced(BooleanSupplier ready,String reason){while(!ready.getAsBoolean()){healthy();c.waitTicks(1);}}
        boolean exists(String suffix){return Files.isRegularFile(path(suffix));}Path path(String suffix){return directory.resolve(stem+"-"+suffix+".properties");}
        boolean existsRole(String who,String phase){return Files.isRegularFile(directory.resolve(who+"-"+stem+"-"+phase+"-observed.properties"));}
        Properties readRole(String who,String phase){return readGlobal(who+"-"+stem+"-"+phase+"-observed",who);}
        Properties read(String suffix,String who){return readGlobal(stem+"-"+suffix,who);}
        Properties readGlobal(String name,String who){try(var stream=Files.newInputStream(directory.resolve(name+".properties"))){var value=new Properties();value.load(stream);
                for(String key:identity.stringPropertyNames())check(identity.getProperty(key).equals(value.getProperty(key)),"Same run/source/session "+key);
                check(who.equals(value.getProperty("role")),"Expected IPC role");long pid=Long.parseLong(value.getProperty("pid"));check(pid>0&&(who.equals(role)?pid==ProcessHandle.current().pid():otherPid==0||pid==otherPid),"Same native process");return value;
            }catch(java.io.IOException failure){throw new AssertionError(failure);}}
        void write(String suffix,Map<String,String> f){writeGlobal(stem+"-"+suffix,f,false);}void writeReplace(String suffix,Map<String,String> f){writeGlobal(stem+"-"+suffix,f,true);}
        void writeGlobal(String name,Map<String,String> fields,boolean replace){try{var value=new Properties();value.putAll(identity);for(var e:fields.entrySet()){check(!value.containsKey(e.getKey())||e.getValue().equals(value.getProperty(e.getKey())),"No identity overwrite");value.put(e.getKey(),e.getValue());}
                value.put("role",role);value.put("pid",Long.toString(ProcessHandle.current().pid()));Path target=directory.resolve(name+".properties"),temp=directory.resolve(name+".tmp");check(replace||!Files.exists(target),"Immutable one-shot counter witness");
                try(var out=Files.newOutputStream(temp,StandardOpenOption.CREATE_NEW)){value.store(out,"Actual connected counter capture");}if(replace)Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);else Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);
            }catch(java.io.IOException failure){throw new AssertionError(failure);}}
        String sha(String suffix){try{return ArticulatedRenderReceipt.sha256(Files.readAllBytes(path(suffix)));}catch(java.io.IOException failure){throw new AssertionError(failure);}}
        public void close(){if(clockOwner==this)clockOwner=null;pacing=false;if(owner)CounterPeerRenderProbe.unwatch();c.getInput().releaseKey(o->o.keyShift);
            for(int i=0;i<SETTINGS.length;i++){if(prior[i]==null)System.clearProperty(SETTINGS[i]);else System.setProperty(SETTINGS[i],prior[i]);}
            c.runOnClient(mc->{mc.options.setCameraType(camera);mc.options.mainHand().set(previousHand);mc.options.toggleCrouch().set(toggle);mc.options.broadcastOptions();mc.setCameraEntity(mc.player);});}
    }
    private static ServerPlayer body(MinecraftServer s,UUID id){var player=s.getPlayerList().getPlayer(id);check(player!=null&&player.isAlive()&&player.connection.player==player&&player.connection.hasClientLoaded(),"Exact current connected body");return player;}
    private static String required(String key){String value=System.getProperty("wildercord.mp."+key);check(value!=null&&!value.isBlank(),"Explicit supervised property "+key);return value;}
    private static void check(boolean yes,String reason){if(!yes)throw new AssertionError("Counter paired fixture: "+reason);}
}
