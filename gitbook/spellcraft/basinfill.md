# Water for a little hollow

**New in 0.10.0-alpha.**

Basinfill is a rank-I frost/water utility rune. It creates permanent ordinary source water in
an enclosed shallow hole. Craft its rune from a **Blank Rune, Clay Ball and Water Bucket**.
The bucket follows normal crafting-container behavior.

![A paid Basinfill spell has filled a small enclosed stone basin with source water](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/screenshots/basinfill/filled-pool.png)

This capture uses a controlled gameplay-test basin. The water is ordinary persistent water.

## Make a spell

Thread **Touch → Basinfill** for a nearby basin, or **Bolt → Basinfill** to reach its floor
from farther away. Aim at the floor inside the hole. The effect has a base cost of six mana;
your delivery and ordinary casting modifiers determine the complete spell price.

The basin must be one block deep with a solid floor and watertight sides. It can contain up to
sixteen connected cells, each within three horizontal blocks of the impact cell. A 4×4 basin
works when aimed at a corner; smaller irregular hollows work too. Existing water can remain
inside the same enclosed basin. Solid objects and plants are preserved.

Basinfill refuses open edges, deeper pits, oversized hollows, unloaded/protected ground,
Adventure restrictions and places where water evaporates. Server block-edit settings and the
cast's block budget apply. It fills one basin per paid cast; repeats cannot flood new basins.
Power and radius modifiers do not enlarge this safety limit. Self is not a terrain-targeting
recipe for this rune.

Four pouring threads gather into a little vessel; projectile delivery carries a cupped water
parcel, and descending drops splash into the filled cells. Water persists after saving and
reopening your world and follows ordinary Minecraft fluid rules if you later alter the banks.
