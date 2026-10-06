package dev.wildercord.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.client.combat.ArticulatedCombat;
import dev.wildercord.client.combat.ArticulatedArmorRenderer;
import dev.wildercord.client.fx.HitStop;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.client.fx.ScreenEffects;
import dev.wildercord.presentation.CombatPresentationOptions;
import dev.wildercord.presentation.CombatPresentationOptions.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Native staged UI, real local file persistence, camera independence and conservative fallback. */
public final class CombatPresentationSettingsTest implements FabricClientGameTest {
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 private static String label(String key,Object...args){return Component.translatable("screen.wildercord.combat."+key,args).getString();}
 private static void click(ClientGameTestContext c,String key){c.clickScreenButton(c.computeOnClient(mc->label(key)));c.waitTicks(2);}
 private static void escape(ClientGameTestContext c){try{c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitTicks(2);}finally{c.getInput().releaseKey(InputConstants.KEY_ESCAPE);}c.waitTicks(2);}
 private static void choose(ClientGameTestContext c,String key){
  String name=c.computeOnClient(mc->mc.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast).map(b->b.getMessage().getString()).filter(s->s.startsWith(label(key,"").strip())).findFirst().orElseThrow());
  c.clickScreenButton(name);c.waitTicks(2);
 }
 private static void layout(ClientGameTestContext c){check(c.computeOnClient(mc->{
  var s=mc.gui.screen();var buttons=s.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
  for(var b:buttons)if(b.getX()<0||b.getY()<0||b.getX()+b.getWidth()>s.width||b.getY()+b.getHeight()>s.height)return false;
  for(int i=0;i<buttons.size();i++)for(int j=i+1;j<buttons.size();j++){
   var a=buttons.get(i);var b=buttons.get(j);if(a.getX()<b.getX()+b.getWidth()&&b.getX()<a.getX()+a.getWidth()&&a.getY()<b.getY()+b.getHeight()&&b.getY()<a.getY()+a.getHeight())return false;
  }return true;
 }),"Every visible control fits and is disjoint at this GUI size");}
 private static void write(Path file,String text){try{Files.createDirectories(file.getParent());Files.writeString(file,text);}catch(Exception e){throw new RuntimeException(e);}}
 private static String read(Path file){try{return Files.readString(file);}catch(Exception e){throw new RuntimeException(e);}}
 @Override public void runTest(ClientGameTestContext c){
  Path file=FabricLoader.getInstance().getConfigDir().resolve("wildercord-visuals.json");
  byte[] original;try{original=Files.exists(file)?Files.readAllBytes(file):null;}catch(Exception e){throw new RuntimeException(e);}
  String[] keys={CombatPresentationOptions.MASTER,CombatPresentationOptions.CAMERA,CombatPresentationOptions.ARMOR,CombatPresentationOptions.ARMS,CombatPresentationOptions.SHELL};
  String[] properties=Arrays.stream(keys).map(System::getProperty).toArray(String[]::new);
  Quality quality=c.computeOnClient(mc->Quality.capture());
  Parsed previousCombat=c.computeOnClient(mc->new Parsed(CombatPresentation.saved(),CombatPresentation.warning()));
  int[] window=c.computeOnClient(mc->new int[]{mc.getWindow().getScreenWidth(),mc.getWindow().getScreenHeight(),mc.options.guiScale().get()});
  boolean fullscreen=c.computeOnClient(mc->mc.options.fullscreen().get());
  try(var world=c.worldBuilder().create()){
   c.waitTicks(25);
   c.runOnClient(mc->{for(String key:keys)System.clearProperty(key);write(file,"{\"unrelated\":{\"keep\":17},\"own\":\"FULL\"}");MagicQuality.load();check(!ArticulatedCombat.enabled()&&!ArticulatedCombat.stableCamera(),"An ordinary old config retains Classic and Default camera");});
   MagicSettingsScreen parent=new MagicSettingsScreen();c.setScreen(()->parent);click(c,"title");
   c.runOnClient(mc->{check(mc.gui.screen() instanceof CombatPresentationScreen,"The ordinary settings entry opens the child");check(!button(mc,"apply").active,"Opening the draft does not enable Apply");});
   choose(c,"animation");escape(c);
   c.runOnClient(mc->{check(mc.gui.screen()==parent,"Escape returns to the exact parent");check(!ArticulatedCombat.enabled()&&!read(file).contains("combat_presentation"),"Escape discards all changes without writing");});
   click(c,"title");choose(c,"animation");
   // Resizing must preserve the opt-in draft and keep every control reachable.
   for(int[] size:new int[][]{{1920,1080},{854,480},{640,480}}){
    c.runOnClient(mc->{mc.getWindow().setWindowed(size[0],size[1]);mc.options.guiScale().set(2);mc.resizeGui();});c.waitTicks(3);layout(c);
    c.runOnClient(mc->check(button(mc,"apply").active,"Resize preserves the dirty draft"));
    c.takeScreenshot(TestScreenshotOptions.of("combat_settings_"+size[0]).disableCounterPrefix());
   }
   // Native keyboard activation of the focused Apply, with a real file write.
   c.runOnClient(mc->mc.gui.screen().setFocused(button(mc,"apply")));
   try{c.getInput().pressKey(InputConstants.KEY_RETURN);c.waitTicks(2);}finally{c.getInput().releaseKey(InputConstants.KEY_RETURN);}c.waitTicks(2);
   c.runOnClient(mc->{
    check(ArticulatedCombat.enabled()&&ArticulatedCombat.stableCamera()&&ArticulatedArmorRenderer.enabled()&&ArticulatedArmorRenderer.viewEnabled(),"Explicit opt-in selects the complete normal profile and offered Stable camera");
    check(!button(mc,"apply").active,"Repeated Apply is disabled once saved");
    var json=JsonParser.parseString(read(file)).getAsJsonObject();check(json.getAsJsonObject("unrelated").get("keep").getAsInt()==17,"Apply preserves unrelated members");
    MagicQuality.load();check(CombatPresentation.saved().equals(new Saved(Animation.ARTICULATED,Camera.STABLE)),"Actual file restart round-trip retains both choices");
   });
   choose(c,"animation");click(c,"apply");
   c.runOnClient(mc->check(!ArticulatedCombat.enabled()&&ArticulatedCombat.stableCamera(),"Switching to Classic preserves Stable camera"));
   choose(c,"camera");click(c,"apply");
   c.runOnClient(mc->{MagicQuality.cameraShake=true;ScreenEffects.nudge(1,1,10000);HitStop.hold(10000,mc.player.getId());check(ScreenEffects.nudging()&&HitStop.holding(),"Camera and cosmetic hold fixtures are live");});
   click(c,"reset");click(c,"apply");
   c.runOnClient(mc->{check(CombatPresentation.saved().equals(Saved.RESET),"Scoped reset persists Classic + Stable");check(!ScreenEffects.nudging()&&!HitStop.holding(),"Apply discards old camera motion and cosmetic holds");});
   click(c,"back");
   for(String preset:new String[]{"performance","balanced","cinematic"}){
    c.clickScreenButton(c.computeOnClient(mc->Component.translatable("screen.wildercord.profile."+preset).getString()));c.waitTicks(2);
    c.runOnClient(mc->{MagicQuality.load();check(CombatPresentation.saved().equals(Saved.RESET),"Every legacy preset leaves independent combat choices unchanged on disk");});
   }
   click(c,"title");click(c,"help");layout(c);for(int i=0;i<6;i++){click(c,"next");layout(c);}escape(c);
   c.runOnClient(mc->check(mc.gui.screen() instanceof CombatPresentationScreen,"Help Escape retains the same settings draft"));
   // A failed disk read must keep the draft editable and the previous live selection unchanged.
   choose(c,"animation");c.runOnClient(mc->write(file,"{broken"));click(c,"apply");
   c.runOnClient(mc->{check(!ArticulatedCombat.enabled()&&button(mc,"apply").active,"Failed Apply preserves prior live options and editable draft");check(read(file).equals("{broken"),"Failed Apply preserves the malformed file exactly");write(file,"{}");});
   click(c,"apply");
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);});
   click(c,"back");c.runOnClient(mc->mc.gui.setScreen(null));c.waitTicks(8);
   c.runOnClient(mc->{
    check(ArticulatedCombat.enabled(),"Saved normal opt-in remains enabled without JVM flags");
    var live=state(mc);check(ArticulatedCombat.viewFrame(live)!=null,"Supported live sword idle is eligible under the saved profile");
    live.isCrouching=true;fallback(mc,live,"Crouching retains complete fallback");
    live=state(mc);live.chestEquipment=new ItemStack(Items.DIAMOND_CHESTPLATE);fallback(mc,live,"Other armor cannot borrow normal profile ownership");
    live=state(mc);if(live.mainArm==HumanoidArm.LEFT)live.rightHandItemStack=new ItemStack(Items.SHIELD);else live.leftHandItemStack=new ItemStack(Items.SHIELD);
    fallback(mc,live,"Occupied offhand rejects ownership");check(ArticulatedCombat.stableCamera(),"Fallback does not change independent Stable camera");
    live=state(mc);live.setData(ArticulatedCombat.KNOWN_LAYERS,false);fallback(mc,live,"Unknown layers still fail closed");
    check(ArticulatedCombat.viewFrame(state(mc))!=null,"Eligibility recovers using current live state");
    var before=CombatPresentation.effective();CombatPresentation.beginFrame();CombatPresentation.applied(Saved.RESET);
    check(CombatPresentation.effective()==before,"A complete extraction/submission frame retains one immutable choice");CombatPresentation.endFrame();
    check(!ArticulatedCombat.enabled(),"The next frame sees Classic without restarting a timeline");
   });
   c.runOnClient(mc->{System.setProperty(CombatPresentationOptions.MASTER,"false");check(MagicQuality.saveCombat(new Saved(Animation.ARTICULATED,Camera.DEFAULT)),"Override test saves a preference");});
   c.setScreen(()->new CombatPresentationScreen(parent));c.waitTicks(2);
   c.runOnClient(mc->{check(!ArticulatedCombat.enabled()&&!firstButton(mc).active,"Explicit master false disables the renderer and its overridden UI choice");check(CombatPresentation.saved().animation()==Animation.ARTICULATED,"Launch overrides never rewrite saved preference");});
   click(c,"help");click(c,"previous");
   c.runOnClient(mc->check(mc.gui.screen().getNarrationMessage().getString().contains(CombatPresentationOptions.MASTER+" = false"),"Help names the exact active override and its value"));
  }finally{
   c.getInput().releaseKey(InputConstants.KEY_ESCAPE);c.getInput().releaseKey(InputConstants.KEY_RETURN);
   c.runOnClient(mc->{
    CombatPresentation.endFrame();for(int i=0;i<keys.length;i++){if(properties[i]==null)System.clearProperty(keys[i]);else System.setProperty(keys[i],properties[i]);}
    try{if(original==null)Files.deleteIfExists(file);else Files.write(file,original);}catch(Exception e){throw new RuntimeException(e);}
    MagicQuality.load();quality.restore();CombatPresentation.loaded(previousCombat);HitStop.clear();ScreenEffects.clearCameraMotion();mc.gui.setScreen(null);mc.getWindow().setWindowed(window[0],window[1]);mc.getWindow().setFullscreen(fullscreen);mc.options.guiScale().set(window[2]);mc.resizeGui();
   });
   // Window mode changes are applied on the following frame, so verify the settled native state.
   c.waitFor(mc->mc.options.fullscreen().get()==fullscreen&&mc.getWindow().getScreenWidth()==window[0]&&mc.getWindow().getScreenHeight()==window[1]&&mc.options.guiScale().get()==window[2],120);
   c.runOnClient(mc->{
    check(Quality.capture().equals(quality)&&new Parsed(CombatPresentation.saved(),CombatPresentation.warning()).equals(previousCombat),"Cleanup restores every live visual and combat preference");
    try{check(original==null?!Files.exists(file):Arrays.equals(Files.readAllBytes(file),original),"Cleanup restores the exact original preferences file bytes or original absence");}catch(Exception e){throw new RuntimeException(e);}
   });
  }
 }
 private record Quality(MagicQuality.Level own,MagicQuality.Level others,boolean flash,boolean motion,boolean titles,
  MagicQuality.StringIndicator string,MagicQuality.Trails trails,MagicQuality.BodyAura body,MagicQuality.Impact impact,MagicQuality.Banners banners) {
  static Quality capture(){return new Quality(MagicQuality.own,MagicQuality.others,MagicQuality.reducedFlash,MagicQuality.cameraShake,MagicQuality.spellTitles,MagicQuality.stringIndicator,MagicQuality.bladeTrails,MagicQuality.bodyAura,MagicQuality.impact,MagicQuality.banners);}
  void restore(){MagicQuality.own=own;MagicQuality.others=others;MagicQuality.reducedFlash=flash;MagicQuality.cameraShake=motion;MagicQuality.spellTitles=titles;MagicQuality.stringIndicator=string;MagicQuality.bladeTrails=trails;MagicQuality.bodyAura=body;MagicQuality.impact=impact;MagicQuality.banners=banners;}
 }
 private static Button button(Minecraft mc,String key){return mc.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().getString().equals(label(key))).findFirst().orElseThrow();}
 private static Button firstButton(Minecraft mc){return mc.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast).findFirst().orElseThrow();}
 private static void fallback(Minecraft mc,AvatarRenderState state,String reason){
  var body=renderer(mc).getModel();body.setupAnim(state);
  check(ArticulatedCombat.frame(state)==null&&ArticulatedCombat.viewFrame(state)==null,reason);
  check(body.body.visible&&body.leftArm.visible&&body.rightArm.visible&&body.leftLeg.visible&&body.rightLeg.visible&&!((dev.wildercord.client.combat.ArticulatedModelAccess)body).wildercord$rig().root.visible,reason+": the complete original body owns the draw");
 }
 @SuppressWarnings("unchecked") private static AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> renderer(Minecraft mc){return (AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer>)mc.getEntityRenderDispatcher().getRenderer(mc.player);}
 private static AvatarRenderState state(Minecraft mc){return renderer(mc).createRenderState(mc.player,.5F);}
}
