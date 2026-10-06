package dev.wildercord.client;

import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.presentation.CombatPresentationOptions;
import dev.wildercord.presentation.CombatPresentationOptions.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Small, staged local preference page. Opening, resizing and Back never apply the draft. */
public final class CombatPresentationScreen extends Screen {
 private final Screen returnTo;
 private Draft draft;
 private boolean failed;
 public CombatPresentationScreen(Screen returnTo){super(text("title"));this.returnTo=returnTo;draft=new Draft(CombatPresentation.saved());}
 private static Component text(String key,Object...args){return Component.translatable("screen.wildercord.combat."+key,args);}
 private static Component value(Enum<?> value){return text(value.name().toLowerCase(Locale.ROOT));}
 @Override public void onClose(){minecraft.gui.setScreen(returnTo);}
 @Override protected void init(){
  int w=Math.min(360,width-24),x=(width-w)/2;
  Launch launch=CombatPresentation.launch();
  var animation=addRenderableWidget(Button.builder(text("animation",value(draft.animation())),b->{draft.toggleAnimation();failed=false;rebuildWidgets();}).bounds(x,26,w,20).build());
  animation.active=!launch.master().present()&&CombatPresentation.warning()!=Warning.FUTURE_VERSION;
  var camera=addRenderableWidget(Button.builder(text("camera",value(draft.camera())),b->{draft.toggleCamera();failed=false;rebuildWidgets();}).bounds(x,50,w,20).build());
  camera.active=!launch.camera().present()&&CombatPresentation.warning()!=Warning.FUTURE_VERSION;
  addRenderableWidget(Button.builder(text("help"),b->minecraft.gui.setScreen(new Help(this))).bounds(x,height-74,w,20).build());
  var reset=addRenderableWidget(Button.builder(text("reset"),b->{draft.reset();failed=false;rebuildWidgets();}).bounds(x,height-50,w,20).build());
  reset.active=CombatPresentation.warning()!=Warning.FUTURE_VERSION;
  int half=(w-4)/2;
  var apply=addRenderableWidget(Button.builder(text("apply"),b->{
   if(MagicQuality.saveCombat(draft.selection())){draft=new Draft(CombatPresentation.saved());failed=false;}
   else failed=true;
   rebuildWidgets();
  }).bounds(x,height-26,half,20).build());
  apply.active=draft.dirty()&&CombatPresentation.warning()!=Warning.FUTURE_VERSION;
  addRenderableWidget(Button.builder(text("back"),b->onClose()).bounds(x+half+4,height-26,w-half-4,20).build());
 }
 private Component current(){var e=CombatPresentation.effective();return text("current",value(e.articulated()?Animation.ARTICULATED:Animation.CLASSIC),value(e.stableCamera()?Camera.STABLE:Camera.DEFAULT));}
 private Component status(){
  if(failed)return text("save_failed");
  if(CombatPresentation.warning()==Warning.FUTURE_VERSION)return text("future");
  if(CombatPresentation.launch().invalid())return text("invalid_override");
  if(CombatPresentation.launch().any())return text("override");
  if(CombatPresentation.warning()==Warning.INVALID_SAVED)return text("invalid_saved");
  if(draft.firstCamera())return text("first_camera");
  return text(CombatPresentation.effective().articulated()?"limited":"classic_status");
 }
 @Override public Component getNarrationMessage(){return title.copy().append(". ").append(current()).append(". ").append(status()).append(". ").append(text("draft",value(draft.animation()),value(draft.camera())));}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
  super.extractRenderState(g,mouseX,mouseY,partial);
  g.centeredText(font,title,width/2,10,0xFFE8C46A);
  int x=(width-Math.min(360,width-24))/2,w=Math.min(360,width-24),y=77;
  for(var line:font.split(current(),w)){g.text(font,line,x,y,0xFFFFFFFF,false);y+=10;}
  y+=3;
  for(var line:font.split(status(),w)){if(y+9>height-80)break;g.text(font,line,x,y,failed?0xFFFFAAAA:0xFFCCCCCC,false);y+=10;}
 }
 /** Always-visible paged help: readable at minimum GUI size, keyboard and narrator accessible. */
 private static final class Help extends Screen {
  private final CombatPresentationScreen parent;
  private int page;
  private static final int PAGES=6;
  Help(CombatPresentationScreen parent){super(text("help"));this.parent=parent;}
  private Component body(){
   if(page!=5)return text("help."+page);
   var flags=CombatPresentation.launch();var out=text("launch_details").copy();
   if(!flags.any())return out.append("\n").append(text("no_overrides"));
   String[] keys={CombatPresentationOptions.MASTER,CombatPresentationOptions.CAMERA,CombatPresentationOptions.ARMOR,CombatPresentationOptions.ARMS,CombatPresentationOptions.SHELL};
   LaunchOverride[] values={flags.master(),flags.camera(),flags.armor(),flags.arms(),flags.shell()};
   for(int i=0;i<keys.length;i++)if(values[i].present())out.append("\n").append(Component.literal(keys[i]+" = ")).append(values[i]==LaunchOverride.INVALID?text("invalid_option"):Component.literal(values[i]==LaunchOverride.TRUE?"true":"false"));
   return out;
  }
  @Override protected void init(){
   int w=Math.min(360,width-24),x=(width-w)/2,half=(w-4)/2;
   addRenderableWidget(Button.builder(text("previous"),b->{page=(page+PAGES-1)%PAGES;rebuildWidgets();}).bounds(x,height-50,half,20).build());
   addRenderableWidget(Button.builder(text("next"),b->{page=(page+1)%PAGES;rebuildWidgets();}).bounds(x+half+4,height-50,w-half-4,20).build());
   addRenderableWidget(Button.builder(text("back_choices"),b->onClose()).bounds(x,height-26,w,20).build());
  }
  @Override public void onClose(){minecraft.gui.setScreen(parent);}
  @Override public Component getNarrationMessage(){return title.copy().append(". ").append(body());}
  @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
   super.extractRenderState(g,mouseX,mouseY,partial);g.centeredText(font,text("page",page+1,PAGES),width/2,12,0xFFE8C46A);
   int w=Math.min(360,width-24),x=(width-w)/2,y=34;
   for(var line:font.split(body(),w)){g.text(font,line,x,y,0xFFDDDDDD,false);y+=11;}
  }
 }
}
