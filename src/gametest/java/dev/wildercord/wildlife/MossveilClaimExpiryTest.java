package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.MossveilNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.UUID;
/** Supplied second Survival actor challenges an actual paid claim after more than6000 ordinary server ticks. */
public final class MossveilClaimExpiryTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");var pet=new MossveilDormouse[1];var challenger=new net.minecraft.server.level.ServerPlayer[1];var original=new UUID[1];var deadline=new long[1];var initial=new long[1];
  w.getServer().runOnServer(s->{floor(s.overworld());var p=player(s);p.setGameMode(GameType.SURVIVAL);place(p,1.5,-.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FungalGarden.GILLS,4));pet[0]=mouse(s.overworld(),.5,.5);original[0]=p.getUUID();});c.waitTicks(4);interact(c,pet[0].getId());
  w.getServer().runOnServer(s->{var p=player(s);check(pet[0].feedings()==1&&!pet[0].isTame()&&pet[0].claimant().equals(original[0])&&p.getMainHandItem().getCount()==3,"Actual client Gill paid original partial claim");deadline[0]=pet[0].claimUntil();initial[0]=MossveilDormouse.clock(s.overworld());challenger[0]=guest(p);challenger[0].setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FungalGarden.GILLS,4));});
  await(c,w,s->MossveilDormouse.clock(s.overworld())>=pet[0].feedReady(),130,"Actual feeding rest elapses before reserved-claim control");
  w.getServer().runOnServer(s->{var e=pet[0];var p=challenger[0];place(p,e.getX()+1,e.getZ());check(!e.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction()&&p.getMainHandItem().getCount()==4&&e.feedings()==1&&e.claimant().equals(original[0]),"Fresh rested foreign actor still refuses before genuine claim expiry");});
  // No timestamp writes, time command, pose override or server speed manipulation. Ordinary loaded AI continues.
  for(int n=0;n<32;n++){if(w.getServer().computeOnServer(s->MossveilDormouse.clock(s.overworld())>deadline[0]))break;c.waitTicks(200);}
  w.getServer().runOnServer(s->{long now=MossveilDormouse.clock(s.overworld());check(now>deadline[0]&&now-(deadline[0]-MossveilDormouse.CLAIM_LIFE)>6000,"Actual saved-clock claim lifetime elapsed across ordinary ticks");var e=pet[0];var p=challenger[0];check(e.live(s.overworld())&&!e.isNoAi()&&!e.isTame(),"Actual original unpaused companion survives the claim window");place(p,e.getX()+1,e.getZ());check(e.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction()&&p.getMainHandItem().getCount()==3&&e.feedings()==1&&e.claimant().equals(p.getUUID()),"Expired original progress resets before precisely one genuine new-owner Gill");check(e.claimUntil()==now+MossveilDormouse.CLAIM_LIFE,"New claimant earns a fresh bounded deadline, without inheriting original partial food");});
  for(int i=0;i<2;i++){await(c,w,s->MossveilDormouse.clock(s.overworld())>=pet[0].feedReady(),130,"New claimant's actual finite feeding rest");w.getServer().runOnServer(s->{var e=pet[0];var p=challenger[0];place(p,e.getX()+1,e.getZ());check(e.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction(),"New owner supplies next actual Gill");});}
  w.getServer().runOnServer(s->{check(pet[0].isTame()&&pet[0].isOwnedBy(challenger[0])&&!pet[0].isOwnedBy(player(s))&&challenger[0].getMainHandItem().getCount()==1&&player(s).getMainHandItem().getCount()==3,"Three newly paid Gills own companion; original one Gill cannot reduce new price or steal owner");challenger[0].discard();});
 }}
}
