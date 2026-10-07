package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.*;
import dev.wildercord.gametest.mixin.CounterPeerPendingAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.Supplier;

/** Test-only passive native request receipt. No check is rerun and no input, payment or source is synthesized. */
public final class CounterPeerAdmissionProbe {
    private CounterPeerAdmissionProbe(){}
    private static volatile Watch current;
    private static final ThreadLocal<Frame> REQUEST=new ThreadLocal<>();
    private record Frame(Watch watch,ServerPlayer actor,SwordStrings.Perform payload){}
    public static final class Watch implements AutoCloseable {
        final String generation,art,armedSha;
        final ServerPlayer actor;
        final net.minecraft.server.level.ServerLevel level;
        final net.minecraft.server.network.ServerGamePacketListenerImpl connection;
        final AuraGuard.Caught caught;
        final long armedTick;
        final int move,windup,recovery;
        final CounterPeerAdmissionTrace trace;
        Object clientBody,clientLevel,clientConnection,action,sourceAction,returnAction;
        String clientUuid="NONE",clientEntity="NONE",clientDimension="NONE",pendingAtEntry="NOT_REACHED";
        boolean returnCommitted,backlash;
        long sentTick=-1,requestTick=-1,returnTick=-1,paidTick=-1,rest=-1;
        double paid,before,after,cost,left;
        MastersArts.Performed source;
        boolean closed;
        String busy="NOT_REACHED",excise="NOT_REACHED",performed="NOT_REACHED",performMarks="NOT_REACHED",sentMarks="NOT_REACHED",requestMarks="NOT_REACHED";
        Watch(String generation,String armedSha,ServerPlayer actor,String art,AuraGuard.Caught caught,int move,int windup,int recovery){
            this.generation=generation;this.armedSha=armedSha;this.actor=actor;this.art=art;this.caught=caught;
            this.move=move;this.windup=windup;this.recovery=recovery;level=actor.level();connection=actor.connection;armedTick=level.getGameTime();
            trace=new CounterPeerAdmissionTrace(generation,actor,level,connection,art);
        }
        public void bindClient(Minecraft mc){
            require(current==this&&!closed&&clientBody==null&&mc.player!=null&&mc.player.getUUID().equals(actor.getUUID()),"Original local owner before the single input");
            clientBody=mc.player;clientLevel=mc.level;clientConnection=mc.getConnection();require(clientConnection!=null,"Live original client connection");
            clientUuid=mc.player.getUUID().toString();clientEntity=Integer.toString(mc.player.getId());clientDimension=mc.level.dimension().identifier().toString();
        }
        public boolean ready(){return trace.ready();}
        public void requireAccepted(){trace.requireAccepted();require(performed.equals("true")&&busy.equals("false")&&excise.equals("false"),"Actual native gates and performer accepted");require(current==this&&!closed,"Current original admission generation");}
        public Map<String,String> fields(){
            var out=new HashMap<String,String>();out.put("admissionSchemaVersion","1");out.put("admissionGeneration",generation);out.put("admissionPeerArmedSha256",armedSha);
            out.put("admissionArt",art);out.put("admissionSentMarks",sentMarks);out.put("admissionRequestMarks",requestMarks);
            out.put("admissionServerBodyUuid",actor.getUUID().toString());out.put("admissionServerBodyEntity",Integer.toString(actor.getId()));out.put("admissionServerLevel",level.dimension().identifier().toString());
            out.put("admissionServerConnectionIdentity",Integer.toUnsignedString(System.identityHashCode(connection)));out.put("admissionClientConnectionIdentity",Integer.toUnsignedString(System.identityHashCode(clientConnection)));
            out.put("admissionClientBodyUuid",clientUuid);out.put("admissionClientBodyEntity",clientEntity);out.put("admissionClientLevel",clientDimension);
            out.put("admissionBusy",busy);out.put("admissionExciseBlocking",excise);out.put("admissionPerformed",performed);out.put("admissionPerformMarks",performMarks);
            out.put("admissionTrace",trace.description());out.put("admissionCheck",trace.checkResult());out.put("admissionRefusal",trace.refusal());
            out.put("admissionObserverFailure",trace.observerFailure());out.put("admissionNativeFailure",trace.nativeFailure());
            out.put("admissionArmedTick",Long.toString(armedTick));out.put("admissionSentTick",Long.toString(sentTick));out.put("admissionRequestTick",Long.toString(requestTick));out.put("admissionReturnTick",Long.toString(returnTick));
            out.put("admissionPaidTick",Long.toString(paidTick));out.put("admissionPaid",Double.toString(paid));out.put("admissionAuraBefore",Double.toString(before));out.put("admissionAuraAfter",Double.toString(after));out.put("admissionRest",Long.toString(rest));
            out.put("admissionCost",Double.toString(cost));out.put("admissionSpendLeft",Double.toString(left));out.put("admissionBacklash",Boolean.toString(backlash));
            out.put("admissionOriginalBody",Boolean.toString(actor.level()==level&&actor.connection==connection&&connection.player==actor&&level.getServer().getPlayerList().getPlayer(actor.getUUID())==actor));
            out.put("admissionPendingAtEntry",pendingAtEntry);out.put("admissionActionIdentity",identity(action));out.put("admissionSourceActionIdentity",identity(sourceAction));out.put("admissionReturnActionIdentity",identity(returnAction));out.put("admissionReturnCommitted",Boolean.toString(returnCommitted));
            if(source!=null)out.putAll(sourceFields(source,"admissionSource"));
            return Map.copyOf(out);
        }
        public void close(){if(current==this)current=null;closed=true;clientBody=null;clientLevel=null;clientConnection=null;action=null;sourceAction=null;returnAction=null;source=null;}
    }
    public static Watch arm(String generation,String armedSha,ServerPlayer actor,String art,AuraGuard.Caught caught,int move,int windup,int recovery){
        require(current==null&&REQUEST.get()==null&&armedSha!=null&&caught!=null&&AuraGuard.caught(actor)==caught&&pending(actor)==null&&!MastersArts.committed(actor),"Fresh receipt after exact peer readiness and genuine catch");
        return current=new Watch(generation,armedSha,actor,art,caught,move,windup,recovery);
    }
    private static Object pending(ServerPlayer actor){return CounterPeerPendingAccess.counter$pending().get(actor.getUUID());}
    private static boolean live(Watch w){return w!=null&&w==current&&!w.closed;}
    private static CounterPeerAdmissionTrace.Payload fields(SwordStrings.Perform p){return new CounterPeerAdmissionTrace.Payload(p.art(),p.marks());}
    public static void sent(Minecraft mc,SwordStrings.Perform payload,Runnable original){
        Watch w=current;
        if(live(w))w.trace.observe(()->{require(mc.player==w.clientBody&&mc.level==w.clientLevel&&mc.getConnection()==w.clientConnection,"Original client body/world/connection sends request");
            require(payload.marks().size()==1&&SwordString.Token.COUNTER.fits(payload.marks().getFirst()),"Exact earned counter payload");w.sentTick=mc.level.getGameTime();w.sentMarks=payload.marks().toString();w.trace.sent(payload,fields(payload));});
        original.run();
    }
    public static void request(ServerPlayer actor,SwordStrings.Perform payload,Runnable original){
        Watch w=current;Frame previous=REQUEST.get();Frame frame=live(w)?new Frame(w,actor,payload):null;
        if(frame!=null)w.trace.observe(()->{require(previous==null,"No nested request can borrow the original receipt");
            w.requestTick=actor.level().getGameTime();w.requestMarks=payload.marks().toString();w.trace.entered(payload,fields(payload),actor,actor.level(),actor.connection);
            require(actor.connection.player==actor&&actor.level().getServer().getPlayerList().getPlayer(actor.getUUID())==actor&&actor.isAlive()&&actor.connection.hasClientLoaded(),"Original connected native server body");
            w.pendingAtEntry=identity(pending(actor));require(w.requestTick>=w.armedTick&&AuraGuard.caught(actor)==w.caught&&pending(actor)==null&&!MastersArts.committed(actor),"Original caught guard and fresh action at request entry");});
        REQUEST.set(frame);
        try{if(frame==null){original.run();return;}w.trace.invoke(original,thrown->{w.trace.observe(()->{
                w.returnTick=actor.level().getGameTime();w.rest=SwordStrings.readyAt(actor,w.art);
                w.returnAction=pending(actor);w.returnCommitted=MastersArts.committed(actor);w.trace.returned(payload,actor,actor.level(),actor.connection,w.returnAction,w.returnCommitted,thrown);
                require(w.returnTick==w.requestTick&&actor.connection.player==actor&&actor.level().getServer().getPlayerList().getPlayer(actor.getUUID())==actor,"Same synchronous native request and original resident body at return");
            });});}finally{if(previous==null)REQUEST.remove();else REQUEST.set(previous);}
    }
    private static void observe(ServerPlayer actor,java.util.function.Consumer<Frame> observation){
        Frame f=REQUEST.get();if(f!=null&&live(f.watch()))f.watch().trace.observe(()->{require(f.actor()==actor,"Foreign body cannot borrow admission scope");observation.accept(f);});
    }
    public static boolean gate(ServerPlayer actor,String gate,Supplier<Boolean> original){
        boolean result=original.get();observe(actor,f->{if(gate.equals("busy"))f.watch().busy=Boolean.toString(result);else f.watch().excise=Boolean.toString(result);});return result;
    }
    public static void performed(ServerPlayer actor,AuraApi.StringArt art,List<Integer> marks,Object counter,boolean fromClash,boolean result){observe(actor,f->{
        Watch w=f.watch();require(w.performed.equals("NOT_REACHED")&&art.id().equals(w.art)&&!fromClash&&counter!=null,"Single original earned-counter performer result");
        w.performed=Boolean.toString(result);w.performMarks=marks.toString();
    });}
    public static Optional<SwordStrings.Refusal> check(ServerPlayer actor,Supplier<Optional<SwordStrings.Refusal>> original){
        var result=original.get();observe(actor,f->f.watch().trace.checked(f.payload(),result.map(Enum::name).orElse("ACCEPTED")));return result;
    }
    public static void refused(ServerPlayer actor,String art,SwordStrings.Refusal reason){observe(actor,f->{require(f.payload().art().equals(art),"Native refusal identifies original art");f.watch().trace.refused(f.payload(),reason.name());});}
    public static AuraRules.Spend spend(ServerPlayer actor,double cost,String reason,Supplier<AuraRules.Spend> original){
        observe(actor,f->{Watch w=f.watch();require(reason.equals("art:"+w.art)&&AuraGuard.caught(actor)==w.caught,"Native payment retains original art and guard receipt");w.cost=cost;w.before=Aura.aura(actor);w.action=pending(actor);});
        var result=original.get();
        observe(actor,f->{Watch w=f.watch();w.paidTick=actor.level().getGameTime();w.paid=result.paid();w.after=Aura.aura(actor);w.left=result.left();w.backlash=result.backlash();require(w.action!=null&&pending(actor)==w.action,"Original pending token survives the native payment callback");
            w.trace.paid(f.payload(),w.action);require(!result.backlash()&&cost>0&&Math.abs(cost-w.paid)<.0001&&Math.abs(w.before-w.after-cost)<.001&&Math.abs(result.left()-w.after)<.001,"Actual native spend paid the full original price");});return result;
    }
    public static void broadcast(ServerPlayer actor,MastersArts.Performed source){observe(actor,f->{Watch w=f.watch();w.source=source;w.sourceAction=pending(actor);w.trace.source(f.payload(),w.sourceAction);
        require(source.entity()==actor.getId()&&source.move()==w.move&&source.startTick()==w.paidTick&&source.windup()==w.windup&&source.recovery()==w.recovery&&Float.isFinite(source.yaw())&&Float.isFinite(source.pitch()),"Actual paid action broadcasts its original full source");});}
    public static void requireSource(Properties receipt,MastersArts.Performed source){
        require(source!=null&&Integer.toString(source.entity()).equals(receipt.getProperty("admissionSourceEntity"))&&Integer.toString(source.move()).equals(receipt.getProperty("admissionSourceMove"))
            &&Long.toString(source.startTick()).equals(receipt.getProperty("admissionSourceStartTick"))&&Integer.toString(source.windup()).equals(receipt.getProperty("admissionSourceWindup"))
            &&Integer.toString(source.recovery()).equals(receipt.getProperty("admissionSourceRecovery"))&&Float.toString(source.yaw()).equals(receipt.getProperty("admissionSourceYaw"))&&Float.toString(source.pitch()).equals(receipt.getProperty("admissionSourcePitch")),"Client receives every field of the original server admission source");
    }
    private static String identity(Object value){return value==null?"NONE":Integer.toUnsignedString(System.identityHashCode(value));}
    public static Map<String,String> sourceFields(MastersArts.Performed source,String prefix){
        return Map.of(prefix+"Entity",Integer.toString(source.entity()),prefix+"Move",Integer.toString(source.move()),prefix+"StartTick",Long.toString(source.startTick()),
            prefix+"Windup",Integer.toString(source.windup()),prefix+"Recovery",Integer.toString(source.recovery()),prefix+"Yaw",Float.toString(source.yaw()),prefix+"Pitch",Float.toString(source.pitch()));
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError("Counter admission: "+reason);}
}
