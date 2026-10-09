package dev.wildercord.client;

import dev.wildercord.presentation.CombatPresentationOptions;
import dev.wildercord.presentation.CombatPresentationOptions.*;
import java.util.Objects;

/** One immutable choice for a complete render frame; changes never recreate adapters or timelines. */
public final class CombatPresentation {
 private CombatPresentation() {}
 private static Saved saved=Saved.LEGACY;
 private static Warning warning=Warning.NONE;
 private static Launch launch=Launch.NONE;
 private static Effective effective=CombatPresentationOptions.resolve(saved,launch), frame=effective;
 private static String masterRaw,cameraRaw,armorRaw,armsRaw,shellRaw;
 private static boolean initialized, dirty=true, rendering;
 public static Saved saved(){return saved;}
 public static Warning warning(){return warning;}
 public static Launch launch(){refresh();return launch;}
 public static void loaded(Parsed parsed){saved=parsed.saved();warning=parsed.warning();dirty=true;}
 public static void applied(Saved selection) {
  saved=selection;warning=Warning.NONE;dirty=true;
  // Only local cosmetics are discarded. Accepted activations, input and server clocks remain intact.
  dev.wildercord.client.fx.HitStop.clear();
  dev.wildercord.client.fx.ScreenEffects.clearCameraMotion();
 }
 public static Effective effective(){if(rendering)return frame;refresh();return effective;}
 public static void beginFrame(){refresh();frame=effective;rendering=true;}
 public static void endFrame(){rendering=false;}
 private static void refresh() {
  String master=System.getProperty(CombatPresentationOptions.MASTER),camera=System.getProperty(CombatPresentationOptions.CAMERA),
   armor=System.getProperty(CombatPresentationOptions.ARMOR),arms=System.getProperty(CombatPresentationOptions.ARMS),shell=System.getProperty(CombatPresentationOptions.SHELL);
  boolean changed=!initialized||!Objects.equals(master,masterRaw)||!Objects.equals(camera,cameraRaw)||!Objects.equals(armor,armorRaw)||!Objects.equals(arms,armsRaw)||!Objects.equals(shell,shellRaw);
  if(changed){
   masterRaw=master;cameraRaw=camera;armorRaw=armor;armsRaw=arms;shellRaw=shell;initialized=true;
   launch=new Launch(LaunchOverride.parse(master),LaunchOverride.parse(camera),LaunchOverride.parse(armor),LaunchOverride.parse(arms),LaunchOverride.parse(shell));
   if(launch.invalid())dev.wildercord.Wildercord.LOGGER.warn("Invalid combat presentation launch option; affected switches are disabled");
  }
  if(changed||dirty){effective=CombatPresentationOptions.resolve(saved,launch);dirty=false;}
 }
}
