package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.MossveilNative.*;
import dev.wildercord.client.fx.MossveilFilterClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
/** Actual paid filter with an earlier supported but unusable nursery retained beside a later jointly qualifying home. */
public final class MossveilJointHomeTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");var pet=new MossveilDormouse[1];var helmet=new ItemStack[1];var at=new BlockPos(3,102,1);var beginning=new long[1];
  w.getServer().runOnServer(s->{floor(s.overworld());pet[0]=mouse(s.overworld(),2.5,.5);});tame(c,w,pet[0]);
  w.getServer().runOnServer(s->{var p=player(s);place(p,3.5,-.5);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);helmet[0]=new ItemStack(MossveilCowl.ITEM);p.setItemSlot(EquipmentSlot.HEAD,helmet[0]);p.addEffect(new MobEffectInstance(MobEffects.POISON,600,0));
   check(MossveilHome.supported(s.overworld(),CANOPY)&&Vec3.atCenterOf(CANOPY).distanceToSqr(p.position())>9,"Earlier actual supported nursery is outside unchanged payer radius");check(p.distanceToSqr(pet[0])<=9&&p.hasLineOfSight(pet[0])&&pet[0].isInSittingPose(),"Actual owned curled creature stays near and visible while home eligibility is isolated");check(MossveilHome.find(s.overworld(),p.blockPosition(),p.position(),pet[0].position())==null,"Complete bounded search refuses when no jointly qualifying home exists");});
  long before=c.computeOnClient(mc->MossveilFilterClient.accepted());c.runOnClient(mc->mc.options.keyShift.setDown(true));c.waitTicks(60);
  w.getServer().runOnServer(s->{var p=player(s);check(helmet[0].getDamageValue()==0&&p.getAttachedOrElse(MossveilCowl.READY,0L)==0&&pet[0].filterReady()==0,"Unusable supported site alone grants no wear/rest/paid outcome");s.overworld().setBlock(at,FungalGarden.NURSERY.defaultBlockState(),2);
   check(MossveilHome.supported(s.overworld(),at)&&MossveilHome.supported(s.overworld(),CANOPY),"Both real supported homes remain present for the regression");check(MossveilHome.find(s.overworld(),p.blockPosition()).canopy().equals(CANOPY),"Legacy first enumeration would continue selecting the old out-of-range site");var selected=MossveilHome.find(s.overworld(),p.blockPosition(),p.position(),pet[0].position());check(selected!=null&&selected.canopy().equals(at),"New bounded search selects actual jointly qualifying home despite earlier valid support");p.removeEffect(MobEffects.POISON);p.addEffect(new MobEffectInstance(MobEffects.POISON,600,0));beginning[0]=s.overworld().getGameTime();});
  await(c,w,server->player(server).getAttachedOrElse(MossveilCowl.READY,0L)>0,90,"Genuine forty-tick preparation pays without deleting the earlier supported home");
  w.getServer().runOnServer(server->{var p=player(server);var poison=p.getEffect(MobEffects.POISON);check(MossveilHome.supported(server.overworld(),CANOPY)&&MossveilHome.supported(server.overworld(),at),"No home was removed or flattened to make the paid control pass");check(helmet[0].getDamageValue()==1&&p.getAttachedOrElse(MossveilCowl.READY,0L)==pet[0].filterReady(),"One actual price and shared finite rest own the admitted outcome");check(poison!=null&&Math.abs((600-poison.getDuration())-(server.overworld().getGameTime()-beginning[0])-60)<=2&&p.hasEffect(MobEffects.SLOWNESS),"Actual admitted home enables bounded sixty-tick Poison trim plus vulnerable slow breath");});
  c.waitFor(mc->MossveilFilterClient.accepted()==before+1,100);check(c.computeOnClient(mc->MossveilFilterClient.accepted())==before+1,"Exactly one real private paid cue reaches client");c.runOnClient(mc->mc.options.keyShift.setDown(false));
 }}
}
