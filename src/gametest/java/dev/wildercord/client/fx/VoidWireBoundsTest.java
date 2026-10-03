package dev.wildercord.client.fx;
import dev.wildercord.content.VoidOption;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Exact network data round trips and rejected resource-exhausting/nonfinite inputs. */
public final class VoidWireBoundsTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){
  for(Runnable invalid:List.<Runnable>of(
   ()->new VoidOption(-1,0,.1F,5,Vec3.ZERO,0),()->new VoidOption(11,0,.1F,5,Vec3.ZERO,0),
   ()->new VoidOption(0,0,Float.NaN,5,Vec3.ZERO,0),()->new VoidOption(0,0,.7F,5,Vec3.ZERO,0),
   ()->new VoidOption(0,0,.1F,1,Vec3.ZERO,0),()->new VoidOption(0,0,.1F,21,Vec3.ZERO,0),
   ()->new VoidOption(0,0,.1F,5,new Vec3(Double.NaN,0,0),0),()->new VoidOption(0,0,.1F,5,new Vec3(1,0,0),0),
   ()->new VoidOption(0,0,.1F,5,null,0),()->new VoidOption(0,0,.1F,5,Vec3.ZERO,Float.POSITIVE_INFINITY),
   ()->new VoidOption(0,0,.1F,5,Vec3.ZERO,.6F))) {
   boolean refused=false;try{invalid.run();}catch(IllegalArgumentException e){refused=true;}check(refused,"Malformed void material rejected before rendering");
  }
  try(var world=c.worldBuilder().create()){
   c.waitTicks(35);world.getServer().runOnServer(s->{var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),s.registryAccess());
    try{for(int style=0;style<=VoidOption.HOUND;style++){
     var option=new VoidOption(style,0xABCDEF,.17F,8,new Vec3(.01,-.02,.03),-.13F);VoidOption.STREAM_CODEC.encode(b,option);
     check(option.equals(VoidOption.STREAM_CODEC.decode(b)),"Exact material/color/drift/spin wire round trip style="+style);
    }
    b.writeVarInt(11);b.writeInt(0);b.writeFloat(.1F);b.writeVarInt(5);b.writeDouble(0);b.writeDouble(0);b.writeDouble(0);b.writeFloat(0);
    boolean refused=false;try{VoidOption.STREAM_CODEC.decode(b);}catch(IllegalArgumentException e){refused=true;}check(refused,"Unknown network style fails constructor bounds");
    }finally{b.release();}
   });
  }
 }
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
