package dev.wildercord.wildlife;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** World-read contracts, including an independent old-volume oracle, without entity/AI substitutes. */
final class MoonreedSearchTest {
 private record Column(int x,int z) {}
 private static final class World implements MoonreedSearch.View {
  final Map<Column,Integer> tops=new HashMap<>();
  final Set<MoonreedSearch.Cell> buds=new HashSet<>(),water=new HashSet<>(),rain=new HashSet<>();
  final Set<Column> absent=new HashSet<>();
  final Map<Column,Integer> heightsRead=new HashMap<>(),candidatesRead=new HashMap<>();
  int loadedChecks,fluidReads,rainReads;
  World plant(int x,int y,int z){var at=new MoonreedSearch.Cell(x,y,z);buds.add(at);tops.merge(new Column(x,z),y,Math::max);return this;}
  World wet(int x,int y,int z){water.add(new MoonreedSearch.Cell(x+1,y-1,z));return this;}
  World raining(int x,int y,int z,boolean value){if(value)rain.add(new MoonreedSearch.Cell(x,y,z));return this;}
  public boolean loaded(int x,int z){loadedChecks++;return !absent.contains(new Column(x,z));}
  private void resident(int x,int z){assertFalse(absent.contains(new Column(x,z)),"No terrain/fluid/rain read may load an absent column");}
  public int surfaceHeight(int x,int z){resident(x,z);heightsRead.merge(new Column(x,z),1,Integer::sum);return tops.getOrDefault(new Column(x,z),-100)+1;}
  public boolean bud(int x,int y,int z){resident(x,z);candidatesRead.merge(new Column(x,z),1,Integer::sum);return buds.contains(new MoonreedSearch.Cell(x,y,z));}
  public boolean water(int x,int y,int z){resident(x,z);fluidReads++;return water.contains(new MoonreedSearch.Cell(x,y,z));}
  public boolean raining(int x,int y,int z){resident(x,z);rainReads++;return rain.contains(new MoonreedSearch.Cell(x,y,z));}
 }
 private static MoonreedSearch.Cell find(World world){return MoonreedSearch.find(world,0,30,0,18000);}
 private static MoonreedSearch.Cell cell(int x,int y,int z){return new MoonreedSearch.Cell(x,y,z);}

 @Test void everyOldVerticalAndHorizontalEdgeRemainsReachable(){
  for(int y=29;y<=31;y++)for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
   var w=new World().plant(x,y,z).wet(x,y,z);
   assertEquals(cell(x,y,z),find(w),"Old-band root "+cell(x,y,z));
  }
 }
 @Test void groundBelowNormalFlightIsReachableWithoutExpandingHorizontalOrUpperBounds(){
  for(int y=26;y<=28;y++)assertEquals(cell(0,y,0),find(new World().plant(0,y,0).wet(0,y,0)));
  for(var at:List.of(cell(0,25,0),cell(0,32,0),cell(-4,30,0),cell(4,30,0),cell(0,30,-4),cell(0,30,4)))
   assertNull(find(new World().plant(at.x(),at.y(),at.z()).wet(at.x(),at.y(),at.z())));
 }
 @Test void oldBandOutranksEveryNewLowerRootAndKeepsOriginalZThenYThenXOrder(){
  var w=new World().plant(-3,26,-3).wet(-3,26,-3).plant(3,31,3).wet(3,31,3);
  assertEquals(cell(3,31,3),find(w),"Existing raised root outranks earlier lower-column root");
  w=new World().plant(3,31,-2).wet(3,31,-2).plant(-3,29,-1).wet(-3,29,-1);
  assertEquals(cell(3,31,-2),find(w),"Z is the outer old iterator axis");
  w=new World().plant(-3,31,0).wet(-3,31,0).plant(3,29,0).wet(3,29,0);
  assertEquals(cell(3,29,0),find(w),"Within Z, Y outranks X");
  w=new World().plant(-3,30,0).wet(-3,30,0).plant(3,30,0).wet(3,30,0);
  assertEquals(cell(-3,30,0),find(w));
 }
 @Test void exposedTopEquivalenceMatchesIndependentOldVolumeOracle(){
  // This test's random stream generates worlds only; production search has no RNG capability.
  var random=new Random(6230147);
  for(int sample=0;sample<200;sample++){
   var w=new World();
   for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(random.nextBoolean()){
    int y=29+random.nextInt(3);w.plant(x,y,z);if(random.nextBoolean())w.wet(x,y,z);
    if(random.nextInt(4)==0)w.tops.put(new Column(x,z),y+2); // Any roof invalidates the old openSky gate.
   }
   assertEquals(oldOracle(w),find(w),"Exposed old-band eligibility/order sample "+sample);
  }
 }
 private static MoonreedSearch.Cell oldOracle(World w){
  for(int z=-3;z<=3;z++)for(int y=29;y<=31;y++)for(int x=-3;x<=3;x++){
   var at=cell(x,y,z);if(!w.buds.contains(at)||w.tops.get(new Column(x,z))+1>y+1)continue;
   boolean moist=w.rain.contains(at);
   for(var water:w.water)if(Math.abs(water.x()-x)+Math.abs(water.z()-z)==1&&water.y()<=y&&water.y()>=y-2)moist=true;
   if(moist)return at;
  }
  return null;
 }
 @Test void dryRoofedAndDaytimeBudsRefuseWhileActualRainProofAdmits(){
  assertNull(find(new World().plant(0,30,0)));
  var roof=new World().plant(0,30,0).wet(0,30,0);roof.tops.put(new Column(0,0),31);assertNull(find(roof));
  assertEquals(cell(0,30,0),find(new World().plant(0,30,0).raining(0,30,0,true)));
  var day=new World().plant(0,30,0).wet(0,30,0);assertNull(MoonreedSearch.find(day,0,30,0,6000));assertTrue(day.heightsRead.isEmpty());
 }
 @Test void absentCandidateAndUnknownWetNeighborNeverCauseARead(){
  var w=new World().plant(0,30,0).wet(0,30,0);w.absent.add(new Column(0,0));assertNull(find(w));
  assertFalse(w.heightsRead.containsKey(new Column(0,0)));assertEquals(0,w.fluidReads);
  w=new World().plant(0,30,0).wet(0,30,0);w.absent.add(new Column(1,0));assertNull(find(w),"Unloaded water is not evidence");
  w.absent.clear();assertEquals(cell(0,30,0),find(w),"The same root becomes visible when its wet column is resident");
 }
 @Test void residentWaterOrRainSufficesWithOtherNeighborsAbsent(){
  var w=new World().plant(0,30,0).wet(0,30,0);w.absent.add(new Column(-1,0));w.absent.add(new Column(0,-1));w.absent.add(new Column(0,1));
  assertEquals(cell(0,30,0),find(w));
  w=new World().plant(0,30,0).raining(0,30,0,true);for(var c:List.of(new Column(-1,0),new Column(1,0),new Column(0,-1),new Column(0,1)))w.absent.add(c);
  assertEquals(cell(0,30,0),find(w));assertEquals(0,w.fluidReads);
 }
 @Test void allThreeWaterDepthsAndFourDirectionsRetainMoistureSemantics(){
  for(int depth=0;depth<=2;depth++)for(var c:List.of(new Column(-1,0),new Column(1,0),new Column(0,-1),new Column(0,1))){
   var w=new World().plant(0,30,0);w.water.add(cell(c.x,30-depth,c.z));assertEquals(cell(0,30,0),find(w));
  }
  var w=new World().plant(0,30,0);w.water.add(cell(1,27,0));assertNull(find(w));
 }
 @Test void crowdedSearchReadsEachCandidateOnceAndHasFixedCosts(){
  var w=new World();for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)w.plant(x,30,z);
  assertNull(find(w));assertEquals(49,w.heightsRead.size());assertEquals(49,w.candidatesRead.size());
  assertTrue(w.heightsRead.values().stream().allMatch(n->n==1));assertTrue(w.candidatesRead.values().stream().allMatch(n->n==1));
  assertEquals(49+49*4,w.loadedChecks);assertEquals(49*12,w.fluidReads);assertEquals(49,w.rainReads);
 }
}
