package dev.wildercord.spell;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VisualElementsTest {
 @Test void knotsKeepFusionIngredientsAndNestedShape(){var id=Knots.id(List.of(Runes.BEAM,Runes.FIRESTORM),"Test");var knot=Runes.get(id).orElseThrow();assertEquals(List.of("fire","wind"),VisualElements.of(List.of(knot)));}
 @Test void everyNamedFusionKeepsBothIngredients() {
  for (RuneDef rune : Runes.all()) {
   var recipe = Fusions.recipeFor(rune);
   if (recipe.isEmpty()) continue;
   var materials = VisualElements.of(List.of(rune));
   assertTrue(materials.contains(recipe.get().first()), rune.id());
   assertTrue(materials.contains(recipe.get().second()), rune.id());
  }
 }
 @Test void everyBuiltinShapeHasAnExplicitFormation() {
  long shapes = Runes.all().stream().filter(r -> r.family() == RuneFamily.SHAPE).count();
  for (RuneDef rune : Runes.all()) {
   if (rune.family() != RuneFamily.SHAPE) continue;
   assertEquals(rune.path().toUpperCase(java.util.Locale.ROOT), ShapeFormation.of(rune.path()).name());
  }
  assertEquals(ShapeFormation.values().length, shapes);
 }
}
