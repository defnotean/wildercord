package dev.wildercord.client.fx;
/** Void preparation preserves the server's shape-dependent assembly anchor and its rear circle. */
final class VoidFormations {
 private VoidFormations() {}
 static boolean supports(String id){return VoidForms.supports(id);}
 static boolean draw(SpellFormations.Canvas c,int beat){
  boolean authored=false;
  for(String id:c.event.runes())if(supports(id)){
   authored=true;VoidForms.prepare(id,beat,c.event.scale(),c.assembly(),c.right,c.up,c.forward,
    c.quality==MagicQuality.Level.MINIMAL,c::emit);
  }
  return authored;
 }
}
