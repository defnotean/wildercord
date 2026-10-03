package dev.wildercord.content;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AirflowOptionTest {
    @Test void curveSurvivesCodec(){
        var o=new AirflowOption(0xFFE4D6C5,new Vec3(-.3,.6,.1),new Vec3(.7,.2,-.1),.08F,8,false);
        assertEquals(0xE4D6C5,o.color());
        var encoded=AirflowOption.CODEC.codec().encodeStart(JsonOps.INSTANCE,o).getOrThrow();
        assertEquals(o,AirflowOption.CODEC.codec().parse(JsonOps.INSTANCE,encoded).getOrThrow());
    }
    @Test void nonfiniteVectorsAndWidthAreRefused(){
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}){
            assertThrows(IllegalArgumentException.class,()->new AirflowOption(0,new Vec3(bad,0,0),Vec3.ZERO,.04F,8,false));
            assertThrows(IllegalArgumentException.class,()->new AirflowOption(0,Vec3.ZERO,new Vec3(0,bad,0),.04F,8,false));
        }
        assertThrows(IllegalArgumentException.class,()->new AirflowOption(0,Vec3.ZERO,Vec3.ZERO,Float.NaN,8,false));
    }
    @Test void overlongCurvesCannotCreateUnboundedCulling(){
        assertThrows(IllegalArgumentException.class,()->new AirflowOption(0,new Vec3(7,0,0),Vec3.ZERO,.04F,8,false));
        assertThrows(IllegalArgumentException.class,()->new AirflowOption(0,Vec3.ZERO,new Vec3(0,7,0),.04F,8,false));
        assertDoesNotThrow(()->new AirflowOption(0,new Vec3(6,0,0),new Vec3(0,6,0),.3F,40,true));
    }
    @Test void widthsAndLifetimesAreBounded(){
        for(float width:new float[]{0,.014F,.31F})assertThrows(IllegalArgumentException.class,()->new AirflowOption(0,Vec3.ZERO,Vec3.ZERO,width,8,false));
        for(int life:new int[]{0,1,41})assertThrows(IllegalArgumentException.class,()->new AirflowOption(0,Vec3.ZERO,Vec3.ZERO,.04F,life,false));
    }
}
