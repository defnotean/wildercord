package dev.wildercord.cast.feel;
import dev.wildercord.spell.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
/** Developer inventory command. Presence proves registry wiring, never unique art or native behavior. */
public final class SpellPresentationAudit {
 public static void main(String[] args) throws Exception {
  ShapeFeels.register();FireFeels.register();FrostFeels.register();StormFeels.register();WindFeels.register();EarthFeels.register();LifeFeels.register();VoidFeels.register();ArcaneFeels.register();TimeFeels.register();BloodFeels.register();
  var root=new JsonObject();var rows=new JsonArray();var counts=new TreeMap<String,Integer>();int signatures=0,hooks=0;
  for(var rune:Runes.all().stream().filter(r -> r.id().startsWith("wildercord:")).sorted(Comparator.comparing(RuneDef::id)).toList()) {
   var row=new JsonObject();row.addProperty("id",rune.id());row.addProperty("name",rune.name());row.addProperty("family",rune.family().name());row.addProperty("tier",rune.tier());row.addProperty("element",rune.element());row.addProperty("kind",rune.kind().name());row.addProperty("category",rune.category());counts.merge(rune.family().name(),1,Integer::sum);
   var signature=Signatures.get(rune.id());row.addProperty("registered_signature",signature!=null);var phases=new JsonObject();
   if(signature!=null) {
    signatures++;row.addProperty("motion_override",signature.motion==null?"":signature.motion.name());row.addProperty("scale",signature.scale);row.addProperty("accent",signature.accent==null?"":String.format("%06X",signature.accent));
    for(var phase:Phase.values()) {var data=new JsonObject();data.addProperty("hook",signature.hookOf(phase)!=null);data.addProperty("replaces_default",signature.replaces(phase));var cue=signature.soundOf(phase);data.addProperty("sound",cue==null?"":cue.name());if(cue!=null) {data.addProperty("volume",cue.volume());data.addProperty("pitch",cue.pitch());}if(signature.hookOf(phase)!=null)hooks++;phases.add(phase.name(),data);}
   }
   row.add("phases",phases);row.addProperty("native_review","pending");row.addProperty("formation_review","shape and element layers present; per-effect choreography unverified");rows.add(row);
  }
  root.add("runes",rows);root.add("family_counts",new Gson().toJsonTree(counts));root.addProperty("registered_signatures",signatures);root.addProperty("registered_phase_hooks",hooks);
  root.addProperty("scope","Finite built-in runtime rune registry. Dynamic Knots/Woven runes, Aura techniques, source-dispatched effects and native presentation require separate review. Registry presence does not prove unique art, sound or mechanics.");
  var deliveries=new JsonArray();for(var shape:Runes.all().stream().filter(r -> r.family()==RuneFamily.SHAPE).sorted(Comparator.comparing(RuneDef::id)).toList()) {var row=new JsonObject();row.addProperty("shape",shape.id());row.addProperty("formation",ShapeFormation.of(shape.path()).name());row.addProperty("base_motion",Motion.of(shape.id()).name());row.addProperty("native_review","pending");deliveries.add(row);}root.add("deliveries",deliveries);
  var directory=Path.of(args.length==0?"build/reports/spell-presentation":args[0]);Files.createDirectories(directory);Files.writeString(directory.resolve("runtime-roster.json"),new GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n",StandardCharsets.UTF_8);
  StringBuilder table=new StringBuilder("# Runtime spell presentation inventory\n\nRegistry presence is wiring evidence. Every row still needs source review and actual presentation evidence. Dynamic Knots/Woven runes and Aura are separate scopes.\n\n| Rune | Family | Element | Signature | Hooks | Sounds | Native review |\n|---|---|---|---|---|---|---|\n");
  for(var element:rows) {var row=element.getAsJsonObject();var hp=new ArrayList<String>();var sp=new ArrayList<String>();for(var entry:row.getAsJsonObject("phases").entrySet()) {var data=entry.getValue().getAsJsonObject();if(data.get("hook").getAsBoolean())hp.add(entry.getKey());if(!data.get("sound").getAsString().isEmpty())sp.add(entry.getKey()+":"+data.get("sound").getAsString());}table.append("| ").append(row.get("id").getAsString()).append(" | ").append(row.get("family").getAsString()).append(" | ").append(row.get("element").getAsString()).append(" | ").append(row.get("registered_signature").getAsBoolean()).append(" | ").append(String.join(", ",hp)).append(" | ").append(String.join(", ",sp)).append(" | pending |\n");}
  Files.writeString(directory.resolve("runtime-roster.md"),table.toString(),StandardCharsets.UTF_8);System.out.println("Runtime roster: "+rows.size()+" runes; "+counts+"; "+signatures+" signatures; "+hooks+" phase hooks; "+deliveries.size()+" shape deliveries. Native review remains pending.");
 }
}
