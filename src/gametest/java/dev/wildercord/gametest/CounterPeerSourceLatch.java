package dev.wildercord.gametest;

/** Passive first-receive identity. A later equal-valued object never rescues a replaced source. */
public final class CounterPeerSourceLatch {
    private final int actor,move,windup,recovery;
    private Object first;
    private long activation,identity;
    private AssertionError failure;
    public CounterPeerSourceLatch(int actor,int move,int windup,int recovery){this.actor=actor;this.move=move;this.windup=windup;this.recovery=recovery;}
    public synchronized void received(Object payload,int entity,int move,long activation,int windup,int recovery,long sequence){
        if(entity!=actor)return;
        require(failure==null,"A rejected first source cannot be rescued");
        require(first==null,"Duplicate or replaced native performed source");
        require(payload!=null&&move==this.move&&windup==this.windup&&recovery==this.recovery&&sequence>0,"Wrong first native performed source");
        first=payload;this.activation=activation;identity=sequence;
    }
    public synchronized long require(Object current,long accepted){
        require(failure==null,"A rejected native source remains rejected");
        require(first!=null&&current==first&&activation==accepted,"Missing or replaced exact native performed object");
        return identity;
    }
    private void require(boolean yes,String reason){if(!yes){if(failure==null)failure=new AssertionError(reason);throw failure;}}
}
