package dev.wildercord.presentation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;

/** Client-local values and launch resolution. No entity, world, input or gameplay state. */
public final class CombatPresentationOptions {
 private CombatPresentationOptions() {}
 public static final String GROUP = "combat_presentation";
 public static final String MASTER = "wildercord.articulated", CAMERA = MASTER + ".stableCamera",
  ARMOR = MASTER + ".armor", ARMS = MASTER + ".armorArms", SHELL = MASTER + ".auraShell";
 public enum Animation { CLASSIC, ARTICULATED; public Animation next(){return this==CLASSIC?ARTICULATED:CLASSIC;} }
 public enum Camera { STABLE, DEFAULT; public Camera next(){return this==STABLE?DEFAULT:STABLE;} }
 /** A null camera is the migration state: existing camera until an explicit Apply. */
 public record Saved(Animation animation, Camera camera) {
  public Saved { java.util.Objects.requireNonNull(animation); }
  public static final Saved LEGACY = new Saved(Animation.CLASSIC, null);
  public static final Saved RESET = new Saved(Animation.CLASSIC, Camera.STABLE);
 }
 public enum Warning { NONE, INVALID_SAVED, FUTURE_VERSION }
 public record Parsed(Saved saved, Warning warning) {}
 public enum LaunchOverride { ABSENT, TRUE, FALSE, INVALID;
  public static LaunchOverride parse(String value) {
   if(value==null)return ABSENT;
   if(value.equalsIgnoreCase("true"))return TRUE;
   if(value.equalsIgnoreCase("false"))return FALSE;
   return INVALID;
  }
  public boolean choose(boolean fallback){return this==ABSENT?fallback:this==TRUE;}
  public boolean present(){return this!=ABSENT;}
 }
 public record Launch(LaunchOverride master, LaunchOverride camera, LaunchOverride armor, LaunchOverride arms, LaunchOverride shell) {
  public static final Launch NONE = new Launch(LaunchOverride.ABSENT,LaunchOverride.ABSENT,LaunchOverride.ABSENT,LaunchOverride.ABSENT,LaunchOverride.ABSENT);
  public boolean any(){return master.present()||camera.present()||armor.present()||arms.present()||shell.present();}
  public boolean invalid(){return master==LaunchOverride.INVALID||camera==LaunchOverride.INVALID||armor==LaunchOverride.INVALID||arms==LaunchOverride.INVALID||shell==LaunchOverride.INVALID;}
 }
 public record Effective(boolean articulated, boolean stableCamera, boolean armor, boolean armorArms, boolean auraShell) {}
 public static Effective resolve(Saved saved, Launch launch) {
  boolean profile=saved.animation()==Animation.ARTICULATED;
  boolean enabled=launch.master().choose(profile);
  // Legacy explicit developer launches retain their original adapter defaults.
  boolean armor=enabled&&launch.armor().choose(profile);
  boolean camera=saved.camera()!=null?saved.camera()==Camera.STABLE:launch.master()==LaunchOverride.TRUE;
  return new Effective(enabled,launch.camera().choose(camera),armor,armor&&launch.arms().choose(profile),enabled&&launch.shell().choose(true));
 }
 public static Parsed parse(JsonElement raw) {
  if(raw==null)return new Parsed(Saved.LEGACY,Warning.NONE);
  if(!raw.isJsonObject())return new Parsed(Saved.LEGACY,Warning.INVALID_SAVED);
  JsonObject group=raw.getAsJsonObject();
  try {
   if(group.has("version")) {
    JsonElement version=group.get("version");
    if(!version.isJsonPrimitive()||!version.getAsJsonPrimitive().isNumber())return new Parsed(Saved.LEGACY,Warning.INVALID_SAVED);
    if(version.getAsBigDecimal().compareTo(java.math.BigDecimal.ONE)>0)return new Parsed(Saved.LEGACY,Warning.FUTURE_VERSION);
    if(version.getAsBigDecimal().compareTo(java.math.BigDecimal.ONE)!=0)return new Parsed(Saved.LEGACY,Warning.INVALID_SAVED);
   }
  }catch(RuntimeException invalid){return new Parsed(Saved.LEGACY,Warning.INVALID_SAVED);}
  Warning warning=Warning.NONE;
  Animation animation=Animation.CLASSIC;
  String choice=string(group.get("animation"));
  if("articulated".equals(choice))animation=Animation.ARTICULATED;
  else if(!"classic".equals(choice))warning=Warning.INVALID_SAVED;
  Camera camera=null;
  if(group.has("camera")) {
   String value=string(group.get("camera"));
   if("default".equals(value))camera=Camera.DEFAULT;
   else {camera=Camera.STABLE;if(!"stable".equals(value))warning=Warning.INVALID_SAVED;}
  }
  return new Parsed(new Saved(animation,camera),warning);
 }
 private static String string(JsonElement value){return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()?value.getAsString():null;}
 /** Preserve unknown members within a known-version group, too. */
 public static JsonObject encode(Saved saved, JsonElement previous) {
  JsonObject group=previous!=null&&previous.isJsonObject()?previous.getAsJsonObject().deepCopy():new JsonObject();
  group.addProperty("version",1);group.addProperty("animation",saved.animation().name().toLowerCase(Locale.ROOT));
  if(saved.camera()!=null)group.addProperty("camera",saved.camera().name().toLowerCase(Locale.ROOT));else group.remove("camera");
  return group;
 }
 /** A screen draft never changes live or persisted values until Apply succeeds. */
 public static final class Draft {
  private final Saved original;
  private Animation animation;
  private Camera camera;
  private boolean cameraChosen, reset;
  public Draft(Saved saved){original=saved;animation=saved.animation();camera=saved.camera()==null?Camera.STABLE:saved.camera();}
  public Animation animation(){return animation;}
  public Camera camera(){return camera;}
  public void toggleAnimation(){animation=animation.next();}
  public void toggleCamera(){camera=camera.next();cameraChosen=true;}
  public void reset(){animation=Animation.CLASSIC;camera=Camera.STABLE;cameraChosen=true;reset=true;}
  public boolean dirty(){return reset||animation!=original.animation()||(original.camera()==null?cameraChosen:camera!=original.camera());}
  public Saved selection(){return new Saved(animation,camera);}
  public boolean firstCamera(){return original.camera()==null;}
 }
}
