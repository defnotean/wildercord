package dev.wildercord.cast;

import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Transport-only diagnostics with explicit test events; never evidence of a paid gameplay outcome. */
public final class LifeOutcomeTransportTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){
  try(var w=c.worldBuilder().create()){
   c.waitTicks(30);UUID id=c.computeOnClient(mc->mc.player.getUUID());
   w.getServer().runCommand("gamerule spawn_mobs false");
   w.getServer().runCommand("fill -4 100 -4 4 100 4 stone");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayer(id);check(p!=null&&p.connection.player==p,"Exact connected transport recipient");NextSignatureNative.pose(p,.5,.5,0);});c.waitTicks(3);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayer(id);var eye=p.getEyePosition();
    long before=deliveries();
    LifeOutcomeTransport.send(event(p,null,eye,LifeOwnerEvents.Moment.APPLY));
    LifeOutcomeTransport.send(event(p,new UUID(4,2),eye,LifeOwnerEvents.Moment.APPLY));
    check(deliveries()==before,"Unknown and foreign source retain server eye clearance");
    LifeOutcomeTransport.send(event(p,id,eye,LifeOwnerEvents.Moment.REFUSED));
    Fx.quietly(()->LifeOutcomeTransport.send(event(p,id,eye,LifeOwnerEvents.Moment.APPLY)));
    LifeOutcomeTransport.send(event(p,id,eye.add(40,0,0),LifeOwnerEvents.Moment.APPLY));
    check(deliveries()==before,"Refusal, quiet renewal and out-of-range owner get no outcome packet");
    int[] authored={0};LifeOutcomes.draw(new LifeOutcomes.Observation("heal",LifeOutcomes.Moment.APPLY,eye,null,1,2,0),false,(option,at)->authored[0]++);
    check(authored[0]>0&&authored[0]<=dev.wildercord.net.LifeOutcomePayload.MAX_PIECES,"Full owner recipe remains bounded before admission");
    check(DecorationBudget.accept(p,1),"Diagnostic establishes the current recipient window");
    int spent=spent(p);LifeOutcomeTransport.send(event(p,id,eye,LifeOwnerEvents.Moment.APPLY));
    check(deliveries()==before+1&&spent(p)-spent==authored[0],"Known owner gets exactly one packet charged for every Full recipe piece");
    // Fill the real existing per-recipient window. This sends no particles and changes no limit.
    for(int i=0;i<4;i++)DecorationBudget.accept(p,128);
    for(int i=0;i<128&&DecorationBudget.accept(p,1);i++){}
    check(spent(p)==512,"The same existing recipient window reaches its 512-piece limit");
    before=deliveries();LifeOutcomeTransport.send(event(p,id,eye,LifeOwnerEvents.Moment.APPLY));
    check(deliveries()==before&&spent(p)==512,"Source-owner exemption never bypasses exhausted recipient budget");
   });
  }finally{c.runOnClient(mc->mc.particleEngine.clearParticles());}
 }
 private static LifeOwnerEvents.Event event(ServerPlayer p,UUID source,Vec3 at,LifeOwnerEvents.Moment moment){return new LifeOwnerEvents.Event(p.level(),"heal",moment,p.getUUID(),at,null,1,2,p.level().getGameTime(),"transport_diagnostic",new Vec3(0,0,1),0,source);}
 private static int spent(ServerPlayer p){var windows=(Map<?,?>)field(null,DecorationBudget.class,"WINDOWS");var window=windows.get(p);return window==null?0:(Integer)field(window,window.getClass(),"used");}
 private static long deliveries(){return (Long)field(null,VisualMetrics.class,"recipients");}
 private static Object field(Object o,Class<?> type,String name){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
