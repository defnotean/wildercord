package dev.wildercord.presentation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Read/merge/atomic replace for the existing local visuals file. Failed reads never become writes. */
public final class VisualPreferencesFile {
 private final Path path;
 public VisualPreferencesFile(Path path){this.path=path;}
 public Path path(){return path;}
 public JsonObject read() throws IOException {
  if(!Files.exists(path))return new JsonObject();
  try {
   var parsed=JsonParser.parseString(Files.readString(path));
   if(!parsed.isJsonObject())throw new IOException("Visual preferences must be a JSON object");
   return parsed.getAsJsonObject();
  }catch(com.google.gson.JsonParseException|IllegalStateException ex){throw new IOException("Unreadable visual preferences",ex);}
 }
 public void write(JsonObject legacy, CombatPresentationOptions.Saved combat) throws IOException {
  JsonObject merged=read(); // Re-read at Apply: preserve unrelated/manual changes since screen opening.
  if(combat!=null&&CombatPresentationOptions.parse(merged.get(CombatPresentationOptions.GROUP)).warning()==CombatPresentationOptions.Warning.FUTURE_VERSION)
   throw new IOException("Newer combat preferences are preserved; this version cannot replace them");
  for(var entry:legacy.entrySet())merged.add(entry.getKey(),entry.getValue().deepCopy());
  if(combat!=null)merged.add(CombatPresentationOptions.GROUP,CombatPresentationOptions.encode(combat,merged.get(CombatPresentationOptions.GROUP)));
  Files.createDirectories(path.toAbsolutePath().getParent());
  Path temporary=Files.createTempFile(path.toAbsolutePath().getParent(),"wildercord-visuals-",".tmp");
  try {
   Files.writeString(temporary,new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(merged));
   // Do not fall back to a non-atomic overwrite: report failure and leave the original intact.
   Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
  }finally{Files.deleteIfExists(temporary);}
 }
}
