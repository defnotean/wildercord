package dev.wildercord.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.wildercord.net.NotebookPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Named builds and three permanent research experiments, with server-validated actions. */
public final class RuneNotebookScreen extends Screen {
 private final NotebookPayload snapshot;
 private EditBox name;
 private String draft="";
 private int slot=1,page;
 public RuneNotebookScreen(NotebookPayload snapshot,RuneNotebookScreen previous){super(Component.translatable("screen.wildercord.notebook"));this.snapshot=snapshot;
  if(previous!=null){draft=previous.name==null?previous.draft:previous.name.getValue();slot=previous.slot;page=previous.page;}}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){if(name!=null)draft=name.getValue();int x=width/2-145,y=Math.max(20,height/2-111);page=Math.min(page,Math.max(0,(snapshot.builds().size()-1)/3));
  name=new EditBox(font,x,y+73,190,20,Component.translatable("screen.wildercord.notebook.name"));name.setMaxLength(28);name.setValue(draft);addRenderableWidget(name);
  addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.notebook.slot",slot),b->{slot=slot%5+1;b.setMessage(Component.translatable("screen.wildercord.notebook.slot",slot));}).bounds(x+195,y+73,95,20).build());
  String[] actions={"save","load","delete"};for(int i=0;i<actions.length;i++){String action=actions[i];addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.notebook."+action),b->act(action)).bounds(x+i*98,y+98,94,20).build());}
  for(int i=page*3;i<Math.min(snapshot.builds().size(),page*3+3);i++){var entry=snapshot.builds().get(i);addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.notebook.build",entry.name(),entry.sockets()),b->name.setValue(entry.name())).bounds(x,y+124+(i%3)*21,290,20).build());}
  addRenderableWidget(Button.builder(Component.literal("<"),b->{draft=name.getValue();page=Math.max(0,page-1);rebuildWidgets();}).bounds(x,y+192,25,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->{draft=name.getValue();page=Math.min(Math.max(0,(snapshot.builds().size()-1)/3),page+1);rebuildWidgets();}).bounds(x+30,y+192,25,20).build());
  addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.notebook.hint"),b->{command("runelab research");onClose();}).bounds(x+60,y+192,115,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(x+180,y+192,110,20).build());
 }
 private void act(String action){if(name.getValue().isBlank())return;String value=StringArgumentType.escapeIfRequired(name.getValue());command("runelab builds "+action+" "+value+(action.equals("delete")?"":" "+slot));command("runelab notebook");}
 private void command(String command){if(minecraft.player!=null)minecraft.player.connection.sendCommand(command);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){super.extractRenderState(g,mx,my,partial);int x=width/2-145,y=Math.max(20,height/2-111);
  g.centeredText(font,title,width/2,y-14,0xFFE8C46A);
  g.text(font,Component.translatable("screen.wildercord.notebook.shapes",Math.min(5,snapshot.shapes())),x,y,0xFFB0DCDD,false);
  g.text(font,Component.translatable("screen.wildercord.notebook.fusions",Math.min(3,snapshot.fusions())),x,y+15,0xFFB0DCDD,false);
  g.text(font,Component.translatable("screen.wildercord.notebook.garden",snapshot.garden()?Component.translatable("screen.wildercord.notebook.done"):Component.translatable("screen.wildercord.notebook.pending")),x,y+30,0xFFB0DCDD,false);
  g.text(font,Component.translatable("screen.wildercord.notebook.saved",snapshot.builds().size()),x,y+54,0xFFE8C46A,false);
 }
}
