package net.minecraft.client.resources.model.geometry;
public class ItemQuads {
 public boolean empty;
 public boolean isEmpty() { return empty; }
 public java.util.List<BakedQuad> all() { return empty ? java.util.List.of() : java.util.List.of(new BakedQuad()); }
}
