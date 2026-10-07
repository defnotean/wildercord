package dev.wildercord.gametest;

import java.util.List;
import java.util.function.Consumer;

/** Pure one-shot causal identity. A rejected event can never be replaced by a later good event. */
public final class CounterPeerAdmissionTrace {
    public record Payload(String art,List<Integer> marks){public Payload{marks=List.copyOf(marks);}}
    private final String generation;
    private final Object body,level,connection;
    private final String art;
    private Object sent,request,paidAction,sourceAction;
    private Payload sentFields;
    private int sends,requests,checks,payments,sources,returns;
    private String checked="NOT_REACHED",refused="NONE";
    private boolean committed;
    private Throwable failure,nativeFailure;

    public CounterPeerAdmissionTrace(String generation,Object body,Object level,Object connection,String art){
        require(generation!=null&&!generation.isBlank()&&body!=null&&level!=null&&connection!=null,"Complete original admission identity");
        this.generation=generation;this.body=body;this.level=level;this.connection=connection;this.art=art;
    }
    /** Observer exceptions never escape into the unchanged native call. */
    public synchronized void observe(Runnable observation){try{observation.run();}catch(Throwable problem){if(failure==null)failure=problem;}}
    public synchronized void sent(Object original,Payload fields){
        require(++sends==1&&requests==0&&original!=null&&art.equals(fields.art()),"One original send before server admission");
        sent=original;sentFields=fields;
    }
    public synchronized void entered(Object decoded,Payload fields,Object body,Object level,Object connection){
        require(++requests==1&&sends==1&&decoded!=null&&sent!=null&&fields.equals(sentFields),"First decoded request must match the single original send");
        require(body==this.body&&level==this.level&&connection==this.connection,"Original body, world and connection at server entry");request=decoded;
    }
    private void active(Object decoded){require(request!=null&&request==decoded&&returns==0,"Exact original in-flight request scope");}
    public synchronized void checked(Object decoded,String result){active(decoded);require(++checks==1,"Native admission check exactly once");checked=result;}
    public synchronized void refused(Object decoded,String why){active(decoded);require(refused.equals("NONE"),"At most one native refusal");refused=why;}
    public synchronized void paid(Object decoded,Object action){active(decoded);require(++payments==1&&action!=null,"One native payment on its original committed action");paidAction=action;}
    public synchronized void source(Object decoded,Object action){active(decoded);require(++sources==1&&payments==1&&action==paidAction,"Original paid action broadcasts exactly one source");sourceAction=action;}
    public synchronized void returned(Object decoded,Object body,Object level,Object connection,Object action,boolean committed,Throwable thrown){
        nativeFailure=thrown;this.committed=committed;
        require(++returns==1&&request==decoded,"Exactly one original native request return");
        require(body==this.body&&level==this.level&&connection==this.connection,"Original body, world and connection at native return");
        if(thrown==null&&refused.equals("NONE")&&checked.equals("ACCEPTED"))
            require(payments==1&&sources==1&&action!=null&&action==paidAction&&action==sourceAction&&committed,"Native accepted return retains its exact paid action");
    }
    public synchronized boolean ready(){return returns>0||failure!=null;}
    public synchronized void requireAccepted(){
        if(failure!=null)throw new AssertionError("Admission observer rejected "+description(),failure);
        if(nativeFailure!=null)throw new AssertionError("Native request threw "+description(),nativeFailure);
        require(sends==1&&requests==1&&checks==1&&checked.equals("ACCEPTED")&&refused.equals("NONE")&&payments==1&&sources==1&&returns==1&&committed,
            "Original native request was not accepted: "+description());
    }
    public synchronized String description(){return "generation="+generation+", sends="+sends+", requests="+requests+", check="+checked+", refusal="+refused+", payments="+payments+", sources="+sources+", returns="+returns+", committed="+committed;}
    public synchronized String checkResult(){return checked;}
    public synchronized String refusal(){return refused;}
    public synchronized String observerFailure(){return failure==null?"NONE":failure.toString();}
    public synchronized String nativeFailure(){return nativeFailure==null?"NONE":nativeFailure.toString();}
    /** Original invocation occurs once; its thrown object and return path are unchanged even if final observation fails. */
    public void invoke(Runnable original,Consumer<Throwable> returned){
        Throwable failure=null;
        try{original.run();}catch(Throwable problem){failure=problem;throw problem;}
        finally{Throwable thrown=failure;observe(()->returned.accept(thrown));}
    }
    private static void require(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
