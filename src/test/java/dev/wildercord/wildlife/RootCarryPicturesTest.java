package dev.wildercord.wildlife;
import dev.wildercord.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Pure authored recipe/wire bounds; does not claim runtime quality or native gameplay acceptance. */
final class RootCarryPicturesTest {
 private record Piece(ParticleOptions option,Vec3 at){}
 private static final UUID SOURCE=UUID.fromString("9c77e87e-b8af-4ead-a11a-d0cfcb4d9d20");
 private static RootCarryFx.Event event(boolean moved){return new RootCarryFx.Event(SOURCE,"minecraft:overworld",new BlockPos(0,101,0),new BlockPos(moved?3:0,101,0),moved,4);}
 private static List<Piece> draw(boolean minimal,int beat){var out=new ArrayList<Piece>();RootCarryPictures.transfer(event(true),beat,minimal,(p,v)->out.add(new Piece(p,v)));return out;}
 @Test void fullAndMinimalRetainClodAndRoot(){for(boolean minimal:new boolean[]{false,true})for(int beat=0;beat<=2;beat++){var pieces=draw(minimal,beat);assertEquals(minimal?2:6,pieces.size());assertTrue(pieces.stream().anyMatch(p->p.option() instanceof MaterialOption m&&m.style()==MaterialOption.STONE));assertTrue(pieces.stream().anyMatch(p->p.option() instanceof LifeOption l&&l.style()==LifeOption.VINE));}}
 @Test void threeBeatsLiftTransitSettleAtActualCells(){var a=draw(true,0).getFirst().at();var m=draw(true,1).getFirst().at();var b=draw(true,2).getFirst().at();assertEquals(.5,a.x,1e-8);assertEquals(3.5,b.x,1e-8);assertEquals(2,m.x,1e-8);assertTrue(m.y>a.y+.6);assertEquals(a.y,b.y,1e-8);}
 @Test void preparationAndTravelKeepEarthLifeIdentity(){for(boolean minimal:new boolean[]{false,true}){var out=new ArrayList<Piece>();assertTrue(RootCarryPictures.prepare("wildercord:root_carry",0,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),minimal,(p,v)->out.add(new Piece(p,v))));assertEquals(minimal?2:6,out.size());out.clear();assertTrue(RootCarryPictures.fly("wildercord:root_carry",13,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),minimal,(p,v)->out.add(new Piece(p,v))));assertEquals(minimal?2:6,out.size());assertFalse(RootCarryPictures.supports("wildercord:grow"));}}
 @Test void rejectsMalformedOrUnboundedWire(){assertThrows(IllegalArgumentException.class,()->new RootCarryFx.Event(SOURCE,"minecraft:overworld",new BlockPos(Integer.MIN_VALUE,101,0),new BlockPos(Integer.MIN_VALUE+1,101,0),true,1));assertThrows(IllegalArgumentException.class,()->new RootCarryFx.Event(SOURCE,"minecraft:overworld",new BlockPos(0,101,0),new BlockPos(9,101,0),true,1));assertThrows(IllegalArgumentException.class,()->new RootCarryFx.Event(SOURCE,"minecraft:overworld",new BlockPos(0,101,0),new BlockPos(1,101,0),false,1));assertThrows(IllegalArgumentException.class,()->RootCarryPictures.transfer(event(true),3,true,(p,v)->{}));}
 @Test void minimalFlightActuallyMovesAndUsesExistingFiveTickContract(){var samples=new HashSet<String>();for(int age:new int[]{4,8,12}){var trace=new ArrayList<Piece>();RootCarryPictures.fly("wildercord:root_carry",age,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),true,(p,v)->trace.add(new Piece(p,v)));for(var piece:trace)if(piece.option() instanceof LifeOption l)assertEquals(5,l.lifetime());else if(piece.option() instanceof MaterialOption m)assertEquals(5,m.lifetime());samples.add(trace.toString());}assertEquals(3,samples.size());}
}
