package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.MossveilNative.*;
import dev.wildercord.client.fx.*;
import dev.wildercord.client.wildlife.*;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Actual tracked original model, naturally timed sniff/walk/curl, real worn head layer and paid Full/Minimal filter. */
public final class MossveilPresentationTest implements FabricClientGameTest{
 private MossveilDormouse mouse;
 private record Rig(float curl,float sniff,float noseDepth,float headTilt,float[] pawAngles,float[] tailAngles){}
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("gamerule natural_health_regeneration false");
  w.getServer().runOnServer(s->{floor(s.overworld());s.overworld().setBlock(CANOPY,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);var p=player(s);p.setGameMode(GameType.SURVIVAL);place(p,1.5,-.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FungalGarden.GILLS,6));mouse=mouse(s.overworld(),.5,.5);aim(p,mouse);});c.waitTicks(5);int id=mouse.getId();
  Rig idle=inspect(c,id,true);check(idle.noseDepth()<-3.7F,"Original muzzle retains its actual authored forward pivot");shot(c,"mossveil_supplied_original_idle");
  await(c,w,s->mouse.tickCount>80,100,"Ordinary actor sniff schedule advances without forced clock");
  for(int n=0;n<120&&c.computeOnClient(mc->((MossveilDormouse)mc.level.getEntity(id)).sniff)<.5F;n++)c.waitTicks(1);
  Rig sniff=inspect(c,id,false);check(sniff.sniff()>=.5F,"Actual synchronized nose sniff animates the original whiskers");shot(c,"mossveil_actual_sniff");
  // Keep the actual moving wild source close through ordinary temptation, then pay the real tame packets.
  w.getServer().runOnServer(s->{var p=player(s);place(p,mouse.getX()+1,mouse.getZ()-.8);});c.waitTicks(3);
  for(int i=0;i<3;i++){await(c,w,s->MossveilDormouse.clock(s.overworld())>=mouse.feedReady(),130,"Real picture-scene feeding rest");interact(c,id);}await(c,w,s->mouse.isOwnedBy(player(s))&&mouse.isInSittingPose(),30,"Real three-Gill owner curl settles");
  for(int n=0;n<30&&c.computeOnClient(mc->((MossveilDormouse)mc.level.getEntity(id)).curl)<.8F;n++)c.waitTicks(1);
  Rig curled=inspect(c,id,false);check(curled.curl()>.8F&&curled.headTilt()>.4F&&Arrays.stream(toDouble(curled.tailAngles())).sum()>2.8,"Actual owner curl folds head and all three original tail hinges");w.getServer().runOnServer(s->aim(player(s),mouse));c.waitTicks(2);shot(c,"mossveil_actual_owner_curl");
  w.getServer().runOnServer(s->player(s).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY));c.waitTicks(3);interact(c,id);w.getServer().runOnServer(s->{place(player(s),mouse.getX()+6,mouse.getZ());aim(player(s),mouse);});
  for(int n=0;n<120;n++){c.waitTicks(1);Rig rig=inspect(c,id,false);if(rig.curl()<.2F&&Arrays.stream(toDouble(rig.pawAngles())).map(Math::abs).sum()>.03)break;}
  Rig walking=inspect(c,id,false);check(walking.curl()<.2F&&Arrays.stream(toDouble(walking.pawAngles())).map(Math::abs).sum()>.03,"Actual ordinary owner-follow gait moves four authored paws");shot(c,"mossveil_actual_owner_follow");
  await(c,w,s->mouse.distanceToSqr(player(s))<9,160,"Real physical follow reaches owner");w.getServer().runOnServer(s->{place(player(s),mouse.getX()+1,mouse.getZ()-.8);aim(player(s),mouse);});c.waitTicks(3);interact(c,id);await(c,w,s->mouse.isInSittingPose(),30,"Real owner re-curl before equipped view");
  w.getServer().runOnServer(s->{var p=player(s);var roof=mouse.blockPosition().offset(1,1,1);s.overworld().setBlock(roof,FungalGarden.NURSERY.defaultBlockState(),2);check(MossveilHome.supported(s.overworld(),roof),"Actual supplied supported home follows this scene's physically moved source");p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(MossveilCowl.ITEM));});c.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));c.waitTicks(3);
  c.runOnClient(mc->{try(var stream=mc.getResourceManager().getResource(Wildercord.id("textures/entity/equipment/humanoid/mossveil.png")).orElseThrow().open()){var image=javax.imageio.ImageIO.read(stream);check(image!=null&&image.getWidth()==64&&image.getHeight()==32,"Actual original worn cowl head layer loads");}catch(java.io.IOException e){throw new AssertionError(e);}});shot(c,"mossveil_actual_worn_cowl");
  for(var quality:new MagicQuality.Level[]{MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL}){
   c.runOnClient(mc->{MagicQuality.own=quality;MagicQuality.others=quality==MagicQuality.Level.FULL?MagicQuality.Level.MINIMAL:MagicQuality.Level.FULL;mc.particleEngine.clearParticles();});
   await(c,w,s->MossveilDormouse.clock(s.overworld())>=player(s).getAttachedOrElse(MossveilCowl.READY,0L),230,"Real saved filter rest elapsed before next paid quality pulse");
   long accepted=c.computeOnClient(mc->MossveilFilterClient.accepted()),pieces=c.computeOnClient(mc->MossveilFilterClient.pieces()),retired=c.computeOnClient(mc->MossveilFilterClient.retired());int wear=w.getServer().computeOnServer(s->player(s).getItemBySlot(EquipmentSlot.HEAD).getDamageValue());
   w.getServer().runOnServer(s->{player(s).removeEffect(MobEffects.POISON);player(s).addEffect(new MobEffectInstance(MobEffects.POISON,600,0));});c.runOnClient(mc->mc.options.keyShift.setDown(true));
   for(int n=0;n<90&&c.computeOnClient(mc->MossveilFilterClient.accepted())==accepted;n++)c.waitTicks(1);
   w.getServer().runOnServer(s->{var p=player(s);System.out.println("MOSSVEIL_FILTER_DIAGNOSTIC actor="+p.position()+" pet="+mouse.position()+" ground="+p.onGround()+" crouched="+p.isShiftKeyDown()+" sight="+p.hasLineOfSight(mouse)+" curl="+mouse.isInSittingPose()+" distance="+p.distanceToSqr(mouse)+" home="+MossveilHome.find(s.overworld(),p.blockPosition())+" wear="+p.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()+" rest="+p.getAttachedOrElse(MossveilCowl.READY,0L)+" now="+MossveilDormouse.clock(s.overworld()));});check(c.computeOnClient(mc->MossveilFilterClient.accepted())==accepted+1,"Actual admitted paid owner cue reaches registered client receiver");c.waitTicks(2);shot(c,"mossveil_actual_paid_filter_"+quality.name().toLowerCase(Locale.ROOT));c.waitTicks(20);
   check(c.computeOnClient(mc->MossveilFilterClient.pieces())-pieces==(quality==MagicQuality.Level.FULL?8:4),"Actual private receiver follows own Full/Minimal with exactly eight/four original physical floss pieces");check(c.computeOnClient(mc->MossveilFilterClient.retired())==retired+1,"Actual local finite pulse retires after sixteen ticks");
   w.getServer().runOnServer(s->check(player(s).getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==wear+1,"Actual quality pulse carries one native wear payment"));long ended=c.computeOnClient(mc->MossveilFilterClient.pieces());c.waitTicks(20);check(c.computeOnClient(mc->MossveilFilterClient.pieces())==ended,"Retired pulse does not replay its material on persistent world age");c.runOnClient(mc->mc.options.keyShift.setDown(false));c.waitTicks(3);
  }
 }}
 private static void aim(net.minecraft.server.level.ServerPlayer p,MossveilDormouse e){var d=e.getBoundingBox().getCenter().subtract(p.getEyePosition());p.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));p.setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));p.teleportTo(p.level(),p.getX(),p.getY(),p.getZ(),Set.<Relative>of(),p.getYRot(),p.getXRot(),false);}
 private static Rig inspect(ClientGameTestContext c,int id,boolean skin){return c.computeOnClient(mc->{check(mc.level.getEntity(id)instanceof MossveilDormouse,"Actual tracked original dormouse");var e=(MossveilDormouse)mc.level.getEntity(id);var r=mc.getEntityRenderDispatcher().getRenderer(e);check(r instanceof MossveilDormouseRenderer,"Registered original renderer, never Wolf/creature mesh replacement");var renderer=(MossveilDormouseRenderer)r;var state=renderer.createRenderState(e,1);var model=renderer.getModel();model.setupAnim(state);var paws=field(model,"feet",ModelPart[].class);var tails=field(model,"tail",ModelPart[].class);var whiskers=field(model,"whiskers",ModelPart[].class);var ears=field(model,"ears",ModelPart[].class);check(paws.length==4&&tails.length==3&&whiskers.length==2&&ears.length==2,"Original four paws, three tail hinges, paired whisker fans and ear cups");
  if(skin)try(var stream=mc.getResourceManager().getResource(renderer.getTextureLocation(state)).orElseThrow().open()){var image=javax.imageio.ImageIO.read(stream);check(image!=null&&image.getWidth()==96&&image.getHeight()==64,"Actual original shaded skin loads");var colors=new HashSet<Integer>();int opaque=0;for(int y=0;y<64;y++)for(int x=0;x<96;x++){int pixel=image.getRGB(x,y);if(pixel>>>24!=0){opaque++;colors.add(pixel);}}check(opaque>600&&colors.size()>24,"Original material has actual painted shaded detail");}catch(java.io.IOException ex){throw new AssertionError(ex);}
  float[] pa=new float[4],ta=new float[3];for(int i=0;i<4;i++)pa[i]=paws[i].xRot;for(int i=0;i<3;i++)ta[i]=tails[i].yRot;var out=new Rig(state.curl,state.sniff,field(model,"nose",ModelPart.class).z,field(model,"head",ModelPart.class).xRot,pa,ta);System.out.println("MOSSVEIL_RIG "+out+" paws="+Arrays.toString(pa)+" tails="+Arrays.toString(ta));return out;
 });}
 private static<T>T field(Object o,String n,Class<T> t){try{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return t.cast(f.get(o));}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static double[] toDouble(float[] a){double[] out=new double[a.length];for(int i=0;i<a.length;i++)out[i]=a[i];return out;}
 private static void shot(ClientGameTestContext c,String name){c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
}
