# Mountain rune and observatory review

## Summit Wind

Meditate while wearing a Cord and holding a Blank Rune, standing still at **Y200 or higher** in Jagged Peaks, Frozen Peaks, Stony Peaks, Snowy Slopes, Grove, Windswept Hills, Windswept Gravelly Hills or Windswept Forest. After the existing twenty-second ritual, one blank becomes Summit Wind. A high plains tower is not a mountain habitat. The per-player reward rest remains one in-game day and persists across restart.

Refusal messages now give the biome's missing condition, or name the biome and height when no land attunement exists. The native test uses real crouch input in Stony Peaks at Y254 and verifies the complete reward.

## Storm Spire

The new 23 × 43 × 23 piece has an octagonal plinth and tower, dark buttresses, deterministic worn masonry, oxidized copper bands, inset cyan windows, an arched entrance, a central glass-insulated conductor, archive benches, induction coils, a wind-instrument gallery, an eight-rib crown and lightning mast. Three two-block-wide switchback stair flights replace the scaffold climb.

The two hall caches, two vault chests, three Runebound guards, Storm/Wind seal, Storm Conductor altar and grounding rods retain their existing functions. The upper vault remains warded. The crown is decorative. Generation skips peaks that cannot fit the recorded height under the build ceiling.

A persistent **StormLayout** version keeps old saved pieces on their original 29-block layout, including chunks not yet generated. Existing placed towers are not replaced. New saved pieces retain the taller layout.

## Evidence and limits

MountainStormTest passes actual meditation, all four orientations generated through chunk-sized calls, stair facing and headroom, real Survival walking, exactly three guards, all four chests, boss altar/seal preservation, old/new piece serialization, a full world restart and registered command placement on normal mountain terrain. Guards are paused during the staircase test to isolate traversal from their spell knockback. Five native screenshots and the review JAR are in artifacts/review/mountain-observatory.

This proves one client and integrated server. It does not measure natural frequency across seeds, dedicated-server multi-client play, sustained performance, or a fresh boss-fight playthrough. The wider living-world roadmap remains active.
