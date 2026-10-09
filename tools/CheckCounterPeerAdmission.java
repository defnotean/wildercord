package dev.wildercord.gametest;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** CPU mutation checks for the one-shot admission boundary and transparent native invocation. */
public final class CheckCounterPeerAdmission {
    private static int checks;
    private static final class Run {
        final Object body=new Object(),level=new Object(),connection=new Object(),decoded=new Object(),action=new Object();
        final CounterPeerAdmissionTrace.Payload payload=new CounterPeerAdmissionTrace.Payload("unmoved",List.of(64));
        final CounterPeerAdmissionTrace trace=new CounterPeerAdmissionTrace("nonce:case-46",body,level,connection,"unmoved");
        void send(){trace.sent(new Object(),payload);}
        void enter(){trace.entered(decoded,payload,body,level,connection);}
        void pay(){trace.paid(decoded,action);}
        void source(){trace.source(decoded,action);}
        void returned(){trace.returned(decoded,body,level,connection,action,true,null);}
        void normal(){send();enter();trace.checked(decoded,"ACCEPTED");pay();source();returned();}
    }
    public static void main(String[] ignored){
        Run good=new Run();good.normal();good.trace.requireAccepted();check(good.trace.ready(),"Native return is ready");
        Run pending=new Run();pending.send();check(!pending.trace.ready(),"Client send alone never means server admission");
        for(String reason:List.of("CLOSED","NO_WEAPON","SILENCED","NOT_READY","CONDITION","NO_AURA","UNSEEN")){
            Run r=new Run();r.send();r.enter();r.trace.checked(r.decoded,reason);r.trace.refused(r.decoded,reason);r.returned();
            rejected(r.trace::requireAccepted,"Real native refusal "+reason);check(r.trace.refusal().equals(reason),"Exact refusal remains available");
        }
        Run silent=new Run();silent.send();silent.enter();silent.trace.returned(silent.decoded,silent.body,silent.level,silent.connection,null,false,null);
        rejected(silent.trace::requireAccepted,"Early native return is not success");
        mutation(r->r.trace.entered(r.decoded,r.payload,r.body,r.level,r.connection),"Request before send");
        mutation(r->{r.send();r.send();},"Duplicate send");
        mutation(r->{r.send();r.enter();r.enter();},"Duplicate decoded request");
        mutation(r->{r.send();r.trace.entered(r.decoded,new CounterPeerAdmissionTrace.Payload("null_parry",List.of(64)),r.body,r.level,r.connection);},"Foreign art");
        mutation(r->{r.send();r.trace.entered(r.decoded,new CounterPeerAdmissionTrace.Payload("unmoved",List.of(65)),r.body,r.level,r.connection);},"Foreign marks");
        mutation(r->{r.send();r.trace.entered(r.decoded,r.payload,new Object(),r.level,r.connection);},"Replaced server body");
        mutation(r->{r.send();r.trace.entered(r.decoded,r.payload,r.body,new Object(),r.connection);},"Foreign world");
        mutation(r->{r.send();r.trace.entered(r.decoded,r.payload,r.body,r.level,new Object());},"Foreign connection");
        mutation(r->{r.send();r.enter();r.trace.checked(new Object(),"ACCEPTED");},"Foreign nested scope");
        mutation(r->{r.send();r.enter();r.source();},"Source before payment");
        mutation(r->{r.send();r.enter();r.pay();r.pay();},"Duplicate payment");
        mutation(r->{r.send();r.enter();r.pay();r.trace.source(r.decoded,new Object());},"Source borrowed another paid action");
        mutation(r->{r.send();r.enter();r.trace.checked(r.decoded,"ACCEPTED");r.pay();r.source();r.trace.returned(r.decoded,r.body,r.level,r.connection,new Object(),true,null);},"Token replacement at return");
        mutation(r->{r.normal();r.enter();},"Late replay after accepted return");
        mutation(r->{r.normal();r.send();},"Late resend after accepted return");
        Run transparent=new Run();var calls=new AtomicInteger();var observed=new AtomicInteger();
        transparent.trace.invoke(calls::incrementAndGet,t->{check(t==null,"Native success retained");observed.incrementAndGet();throw new AssertionError("observer failure");});
        check(calls.get()==1&&observed.get()==1,"Original and final observation each called once");rejected(transparent.trace::requireAccepted,"Observer failure latched outside gameplay");
        for(Throwable original:List.of(new IllegalStateException("native exception"),new AssertionError("native error"))){
            Run r=new Run();var nativeCalls=new AtomicInteger();Throwable[] seen={null};
            try{r.trace.invoke(()->{nativeCalls.incrementAndGet();if(original instanceof Error error)throw error;throw (RuntimeException)original;},t->{seen[0]=t;throw new AssertionError("secondary observer");});throw new AssertionError("Missing original throw");}
            catch(Throwable actual){check(actual==original&&seen[0]==original&&nativeCalls.get()==1,"Exact original throwable survives final observer failure");}
            check(!r.trace.observerFailure().equals("NONE"),"Final observer failure stays latched");
        }
        System.out.println("Counter peer admission checks: "+checks+" passed; native clients not run");
    }
    private static void mutation(Consumer<Run> mutation,String why){Run r=new Run();r.trace.observe(()->mutation.accept(r));rejected(r.trace::requireAccepted,why);r.trace.observe(r::normal);rejected(r.trace::requireAccepted,why+" cannot be rescued by later events");}
    private static void rejected(Runnable action,String why){boolean rejected=false;try{action.run();}catch(AssertionError expected){rejected=true;}check(rejected,why);}
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
}
