# Lair Bosses


Seven bosses sleep in lairs of their own, each in its own kind of land. Wake one and it fights with a whole moveset:

- **No move twice running.** It picks each next move from what suits where you stand: in front of it, at its back or
  a way off. Move, and you change what comes.
- **Every move has a tell.** A pose, a sound, and often a mark on the ground. Its boss bar names the move it's making.
- **Openings.** Many moves leave it stuck, sprawled, dazed or panting. That's when it takes more.
- **Armour only goes so far.** Every boss, not only these seven, hits players 35% harder than its numbers suggest, and
  at least 40% of what armour, Protection and Resistance would stop gets through. Full diamond with Protection IV
  still helps a lot, but it won't make you safe.
- **Half beaten, it turns fiercer.** It stops for one big move, then fights on quicker, often with its moves in pairs.


## At a glance

Roughly from easiest to hardest:

| | Boss | Lair | Where | Health | What it gives |
|---|---|---|---|---|---|
| 1 | [Rootcoil Serpent](#rootcoil-serpent) | A sinkhole down to a flooded cavern | Jungles | 200 | **Coilroot**: aura comes more easily |
| 2 | [Lantern-Bearer](#lantern-bearer) | The Barrow Field, after dark | Plains, meadows, savannas | 250 | **Bearer's Lantern**: calls the Barrow Steed |
| 3 | [Hollow Matron](#hollow-matron) | An abandoned mine | Forests, taigas, windswept hills, plains | 300 | **Weaver's Silk**: climb walls |
| 4 | [Ashen Stag](#ashen-stag) | A burnt clearing | Forests and taigas | 350 | **Cinder Antler**: gore after a sprint |
| 5 | [Frost Wyrmling](#frost-wyrmling) | The Glacier Den | Snowy mountains, groves, snowy plains and taigas | 410 | **Rimescale**: frost can't hurt you |
| 6 | [Mojito](#mojito) | A misty grove | Forests, meadows, plains | 440 | **Mojito's Fetchblade**, bones, emeralds and diamonds |
| 7 | [Drowned Bell-Warden](#drowned-bell-warden) | A chapel sunk to the sea floor | Deep oceans | 470 | **Bell Shard**: tolls when you're nearly down |

## How every lair boss works

- **New land only.** Lairs appear only in newly generated chunks. Each kind is spread about 700 blocks from the next.
- **It sleeps until you come close.** While it rises it can't be hurt and shows no bar.
- **It guards its lair.** Hostile monsters that wander in are killed.
- **It stays home.** It's put back if it strays from its lair. With nobody to fight, it walks back after 5 seconds and
  sleeps again after 10, healed.
- **It heals when left alone.** After 3 seconds without being hurt, it heals 1.5% of its health a second. Keep at it.
- **Its blows bite through armour.** Part of every hit ignores armour.
- **No trees in the arena.** The forest around a lair doesn't grow into it or hang over its edge. Trees that had
  already grown inside an older lair are cut down the first time the whole lair is loaded. The lair's own dead trees,
  posts and planted leaves stay. Saplings planted within 4 blocks of a lair (or a dungeon) won't grow, even with bone
  meal.
- **Not on Peaceful.** None of them wake.
- **A chest and a boon.** Each lair has a chest. Six of the seven also guard a **boon**: a block in the lair you use to
  take its gift for good. You can't take it while its boss is alive nearby. Each player can take it once, and it stays
  for whoever comes after.

Server owners can turn new lairs off with `aura_world.guardians`. Boons already taken are kept.

## Rootcoil Serpent

**Where:** a sinkhole in the jungle floor (jungles, sparse jungles and bamboo jungles) drops to a cavern with a ring of
water round a mossy island and a tree. The serpent, thirty blocks long, lies coiled round the tree. Come within about 7
blocks of the tree and it uncoils and slides down to meet you.

### How it fights

| Move | The tell | What to do |
|---|---|---|
| **Striking** | Drawn back with a hiss | It bites straight out. Step aside. |
| **Lunging** | Wound low, dust stirring before it, a line marked | It throws itself 9 blocks along the line. Step aside. If it finds nobody it lies **sprawled**, an opening. |
| **Coiling to grab** | Gathering its coils | It grabs and **constricts** you for up to 3.5 seconds. Press jump or a strafe key six times to struggle free, or have friends hit it hard. It won't grab again for 8 seconds. |
| **Tail slam** | Tail rises over its back | It comes down in front of it with a shock. Anyone keeping off has 5 clods of the island flung at them. |
| **Tail sweep** | Head turned back over its shoulder, tail rattling (when you're behind it) | One full turn at ankle height, 7.5 blocks out. Jump it. |
| **Driving roots** | Reared tall, staring down as the earth cracks along its aim | Roots burst up along a 15-block line, too tall to jump. Step off the line. Its **head sticks in the earth** after, an opening. |
| **Hood spread** | Hood spread and dripping | A jet of sap sweeps across its front, 8.5 blocks out, stinging and slowing. Get round its side or out of reach. Its belly is bare meanwhile. |
| **Coiling round** | You hang about near it | It slides out round you in a ring, laps it once and draws it shut. Get out before it closes. |

- Its scales turn blows from the front: it takes half there and a little less on its flanks. It slips aside from some
  blows to its face.
- Spent, sprawled or stuck in the earth, it takes a third more, and half again as much while sprawled or stuck. Reared
  to spray, it takes a quarter more.

### Half beaten

It towers with its hood spread, earth pouring off it, and crashes down, throwing back everyone near. Roots ripple out
across the island, knee high: jump them. Then it dives into the water ring, where it takes only a quarter, and bursts out
three times. Each time the water boils for half a second where it will come out, then it arcs out at where you stood.
Then it comes back ashore, roused:

- More than half the time, one move runs straight into another.
- Its roots come three lines at a time, fanned out.
- Its sap spray swings wider.

Leave the grove and it climbs back to its tree, healed.

### What it gives

**Coilroot**, growing by its tree. Eat it once, for good: aura comes a third faster in a
[breathing stance](../progression/breathing-methods.md), and you gather half a point of
[aura](../progression/aura.md) a second at all times. It can't be eaten while the serpent is alive
within 40 blocks.

## Lantern-Bearer

**Where:** the Barrow Field, an old battlefield on plains, sunflower plains, meadows and savannas, with graves, a broken
rampart and a gibbet at its heart. A gaunt hooded spirit lies in the open grave under the gibbet. By day it doesn't stir.
**Come after dark** and walk within about 11 blocks of its grave and it climbs out. Dawn sends it back to its grave,
healed.

### How it fights

| Move | The tell | What to do |
|---|---|---|
| **Low Reap** | Crouched, spade scraping the ground | Swept at ankle height round its front. Jump it. |
| **Hooking** | Crook drawn back, a soul-fire line running to you | Step off the line. If it misses, its crook is **snagged**, an opening. |
| **Soul Wisps** | Lantern whirled over its hood | 3 wisps home in on you. Break line of sight and they gutter out. |
| **Open Graves** | Spade raised, a closing ring under each player | Be out of your ring before it closes. Its spade sticks after, so strike. |
| **Lantern Flare** | Hunched round its lantern as souls pour in | It flares out all round it, 4.5 blocks. Get clear. |
| **Grave Earth** | Bent over its spade at its right foot | Earth is flung round you, each landing ringed. Step off the rings, or close in under it. |
| **Backsweep** | Looks over its shoulder at whoever is behind | Its crook whips low behind it. Get to its flank. |
| **Soul Breath** | Lantern raised to its hood, head cocked | A cone that turns the way its head leans. Go the other way, or round its back, where it takes a quarter more. Strike while it coughs after. |

- It takes less from the front. Spent, snagged, stuck or coughing, it takes a third more, and 40% more while snagged.
- It fades to smoke from some blows and can't be touched until it's back. Hurt too fast, it drifts away out of reach.

### Half beaten

**Rite of Candles.** It stands in a long rite while the candles flare and the field goes dark, taking only half. Be away
from it before the burst, then jump the ring of fire that rolls out across the field. Then it fights on roused:

- One move can run straight into another.
- Its reap sweeps back the way it came, and it throws its hook twice.
- More wisps, 5 at a time.
- A grave opens where you're going, not just where you stand.
- A ring of fire rolls out after its flare, and a second fan of earth follows the first.
- Its breath lasts longer and sweeps back across.

**Lights Out**, roused only. It snuffs its lantern and the field goes dark. Then it stands up behind one player after
another, its lantern glimmering low, and reaps, three rounds of it. Turn round and strike it as it glimmers, and its
lantern flares back up.

### What it gives

**Bearer's Lantern**, standing at the gibbet's foot once the Bearer is dead. Take it down and use it to call up the
**Barrow Steed**, which carries you until you get down. The lantern rests 10 seconds after. The first time you take it,
you also get the book *The Barrow Field*.

## Hollow Matron

**Where:** an abandoned mine in dark forests, forests, birch forests, taigas, old-growth spruce taigas, the windswept
hills and plains. A headframe stands over the shaft, and 32 blocks down it is a hall choked with webs, where a great dusky
spider waits. Come within about 13 blocks and she rises.

### How it fights

| Move | The tell | What to do |
|---|---|---|
| **Biting** | Reared back, fangs dripping, settling a line | Sidestep. A bite poisons for 3 seconds. If she misses, her **fangs stick fast** and her face is bare: half again as much damage. |
| **Scything** | Forelegs raised and drawn back | Swept across at chest height. Crouch under it or step back. |
| **Spitting** | Head down, fangs pumping | A glob of web: Slowness III for 3 seconds and a sticky floor. Step aside. |
| **Leaping** | Crouched, a closing ring marking the spot | Get out of the ring. She lies spent after. |
| **Drumming** | Reared, forelegs drumming faster and faster | A ring of dust runs out along the floor. Jump it. She's low and open after. |
| **Casting her thread** | Abdomen curled under, head low | A thread shoots along a line and **hauls in** whoever it catches. Step off the line. Caught, two blows snap it and she reels. |
| **Bristling** | Raised abdomen swung at whoever is behind | Stinging hairs fly out in a cone behind her: Weakness for 4 seconds. Get to her side. Her abdomen is bare after: strike her rear for nearly double. |
| **Raining silk** | Abdomen thrown up at the roof | Rings close under everyone and round the hall, and clumps of web drop, holding you fast for 2 seconds. Keep moving. |

- She takes less from the front and a little more from behind.
- She skitters away from some blows. Hurt too fast, she backs off.

**Up the silk.** Once she's down to 70% health, and every 26 seconds after, she makes for a silk pillar and climbs it,
waking her brood: two cave spiders, one more for each extra player, up to five. Then she **plunges** on one player, with
dust falling where she'll land. Get out from under it. Three strikes on her pillar's silk bring her down: she falls and
lies stunned about 3.5 seconds, the best time to strike.

### Half beaten

She hunches quivering, silk streaming up off her, with a ring marking how far the blast will reach. Get out of the ring.
She throws herself wide, flinging everyone back and leaving web all about. Then she fights on roused:

- One move can run straight into another.
- She swings her scythe back the way it came.
- Three globs of web in a fan, and two rings of drumming.
- A second, wider flick of hairs.
- A longer lunge.
- More web, some of it laid ahead of anyone running.

### What it gives

**Weaver's Silk**, wound round a bone under her nest. Take it and from then on you climb walls the way she does:

- Walk into a wall to climb it. Stop and you slide slowly down. Sneak to cling fast.
- No fall damage while you cling.
- The silk holds you for 6 seconds off the ground, then lets go until you've stood on the ground again.
- If it keeps catching you on walls while you jump about, press `G` (or type `/silk`) to put it away. Press it again to take it out. It stays put away through death and relogs.

## Ashen Stag

**Where:** a burnt clearing in forests, birch forests, dark forests, old-growth birch forests, taigas and old-growth
taigas, ringed by dead trees with a smouldering stump at its heart. A great stag burnt black lies bedded in the ash. Come
within about 12 blocks and it rises. Fire can't hurt it.

### How it fights

| Move | The tell | What to do |
|---|---|---|
| **Tossing** | Head low and twisted, a tine scraping sparks | Sidestep as its head comes up. Its chest is bare after. |
| **Bucking** | Tail flagged, head down to its knees (at whoever is behind) | It kicks embers out behind. Don't linger at its tail. |
| **Rearing** | Up on its hind legs, belly bare, the ground before it ringed with flame | Get out from under its hooves, then jump the ring of fire. |
| **Charging** | Pawing, snorting smoke, settling a line | It charges, leaving a burning wake. Step aside. Out of room, it **skids** with its flank turned (40% more). Led into a trunk or the bank, it's **stuck fast** (half again as much). |
| **Raking Cinders** | Brow down, antlers dragged side to side | Cinders land where you're going, each in a closing ring of embers. Change your line. |
| **Calling the Herd** | Head thrown back, bellowing, throat bare | Deer of embers charge down marked lanes. Stand in the gaps. |
| **Smoking** | It shakes off a pall of smoke | The smoke blinds you. Get out of it. Then it **stalks** low with its glow banked, snorting softly, and **lunges**: sidestep the spring. |
| **Locking Antlers** | Antlers levelled, forehoof scraping | Step aside. Caught, two blows to its throat from the front free you, and it reels 2 seconds, taking half again as much. |

- It takes about a third less from the front. Its flanks take full damage, and recovering after a move it takes a third
  more.
- It bounds aside from some blows. Hurt too fast, it leaps away.

### Half beaten

**Kindling.** It rears and bellows, fire running up its antlers, with a ring marking the blast. Get out of the ring, then
jump the great wave that follows. Then it fights on kindled:

- One move can run straight into another.
- Its charge becomes a **Cinder Rush**, three charges one after another.
- It rears, bucks and lunges twice over.
- A second herd comes half a lane over from the first.
- A second cinder lands where you stand.

### What it gives

**Cinder Antler.** Take it and from then on, after sprinting for 1.5 seconds, the first hostile you strike is gored:
knocked back and set burning for 5 seconds. Then it cools for 20 seconds.

## Frost Wyrmling

**Where:** the Glacier Den, a crevasse cut down into the ice under snowy slopes, frozen and jagged peaks, groves, ice
spikes, snowy plains and snowy taigas, with a hall of ice past it. A young drake lies curled on a hoard. Come within
about 12 blocks of the hoard and it wakes.

### How it fights

| Move | The tell | What to do |
|---|---|---|
| **Biting** | Neck coiled in an S, frost steaming from its jaws | It lunges and snaps 5.5 blocks out. Sidestep. |
| **Sweeping** | Looks over its shoulder, tail raised, fin frosting | A low lash through everywhere but its front. Jump it. |
| **Wing Gale** | Wings mantled forward and cupped | Three beats, each a gale that shoves you. Get to its side, or crouch and take only a quarter of the shove. |
| **Frost Breath** | Head high, chest aglow, ice pillars rising beside you | A beam sweeps across the hall, 10 blocks out, chilling and slowing. Hide behind a pillar or get behind it. |
| **Pouncing** | Crouched, rump wiggling | Three hops, each landing marked, the last widest. Keep moving sideways. |
| **Diving** | Hanging under the dome, a pale line drawn to you | When the line flares it dives down it and skids on. Step off the line. It lies **sprawled** 1.5 seconds after, belly bare (a third more). |
| **Tobogganing** | Head low, it breathes a strip of ice at you and turns its back | It slides down the strip tail first. Get off the strip. It lies sprawled on its side after. |
| **Icefall** | Reared back, head up to the dome | Icicles fall on frost marks by you, three for each player. Keep moving. Its belly is bare throughout. |

- It takes less from the front. Its flanks take full damage, and recovering after a move it takes a third more.
- It hops aside from some blows. Hurt too fast, it beats away across the hall.

### Half beaten

**Ice Shell.** It wraps its wings round itself in a glowing, cracking shell and takes only a fifth. Then it bursts out
across a white ring (get clear), a frost ring rolls out across the hall (jump it), and it stands dazed 2 seconds, taking
40% more. Then it fights on fierce:

- One move can run straight into another.
- It bites twice.
- One more hop to its pounce and one more beat to its gale.
- Its breath sweeps back the other way.
- More icicles, five for each player.

**Rime Lances**, fierce only. It rears and slams its forefeet down, and cracks race out across the floor, one of them
straight at each player. Lances burst up along every crack out to 14 blocks and fling you up. Step off the crack. Its
belly is bare while it does.

### What it gives

**Rimescale.** Take it and frost can't hurt you. A hostile that hits you in melee is stung, chilled and slowed for 4
seconds. The scale re-rimes after 5 seconds.

## Mojito

**Where:** a misty grove ringed by old trees in forests, flower forests, birch forests, meadows, plains and sunflower
plains, a greatsword lying in the grass at its heart. Before it sleeps a black poodle grown as tall as a house. Come
within about 14 blocks and he takes the sword up in his mouth, shakes himself out and barks.

### How he fights

| Move | The tell | What to do |
|---|---|---|
| **Sweeping low** | Crouched, head wrenched round | The blade sweeps low along the grass. Jump it. |
| **Rearing up** | Up on his hind legs, belly bare | He crashes down head first and the ground cracks on along the line. Step aside. The blade sticks a moment after. |
| **Chasing his tail** | Turns to look at his tail | He spins round and round, sword out, drifting after you. Keep clear. He comes out of it **dizzy** (40% more). |
| **Pouncing** | Rump up and wiggling, his shadow marking the spot | Get off the shadow, then jump the ring the landing throws out. |
| **Fetch!** | Head cocked back | He throws the sword spinning along a line and bounds after it. Step off the line. Caught, he prances about, pleased with himself and open (a third more). Out of reach, it comes back to him on its own. |
| **Digging** | Turns his back and paws | Clods fall on marked spots. Step off them. His face is open meanwhile (half again as much), so get round to it; his rump turns blows. |
| **Casting** | Magic swells at his sides | Three volleys: straight, then leading you, then a fan. Keep moving. |
| **Shaking off** | He shivers | Crowd him or hit him too often and he shakes himself out hard, flinging everyone close away. Back off when he shivers. |

- His brow turns blows, about a third less from the front. His flanks take full damage.
- He hops back from some blows. Panting after a move, he takes a third more.

### Half beaten

**Howling.** He sits back and howls, and rings roll out along the ground. Jump them. Then he goes wild: quicker, with his
moves in pairs.

**Zoomies!**, wild only. A play-bow, then he races round and round the grove, leaving a stinging trail of torn-up grass.
Now and then he skids and barks, then cuts straight across at you. When he's done he **flops over** panting for 3.5
seconds, the biggest opening of the fight.

### What he gives

Mojito guards no boon. He drops **Mojito's Fetchblade**, a netherite sword, with 6 to 12 bones, 4 to 9 emeralds and 2
to 4 diamonds.

## Drowned Bell-Warden

**Where:** a chapel sunk whole to the deep ocean floor, in deep oceans of every temperature, kept dry inside by a moon
pool under its belfry. A drowned giant wearing a cracked church bell over its head kneels in vigil before the broken
bell-frame. Walk about halfway up the nave and it rises. It rings anything hostile out of its hall. Leave the chapel and
it kneels again, healed. Bell chapels are never built close to a Drowned Scriptorium.

### How it fights

| Move | The tell | What to do |
|---|---|---|
| **Whirling** | Feet planted, twisted far round, its clapper scraping a ring of sparks | It whirls all the way round, catching everyone inside the ring. Get outside the sparks. It staggers after. |
| **Hammering** | Clapper raised overhead, a glint and a line of bubbles | The floor splits on along the line, 9 blocks out. Step off it. The clapper **sticks fast** about 2 seconds, its chest bare (half again as much). |
| **Pounding the floor** | Both fists up, water running off them | Marks bubble up under you and round about, each spouting a moment later. Step off the marks. |
| **Charging** | Bell lowered, pawing the floor, bell ringing low | It charges along the line it settled on. Step aside. Lead it into a wall or a pillar and it **rings its bell** on the stone, dazed 3 seconds (half again as much). |
| **Tolling** | Clapper raised to its own bell | A ring rolls out along the floor from its feet. Jump it. |
| **Lashing its chain** | Chain whirled overhead faster and faster, bubbles running along the line | It lashes the chain out and drags whoever it catches back to its feet. Step off the line, or stand right inside its reach. |
| **Bringing its bell down** | It lifts its bell off its head while a circle bubbles on the floor ahead | It brings the bell down mouth first over the circle. Leave it. Then it hunches over the bell **bare-headed**, taking half again as much from anywhere. |
| **Pealing** | Hands on its bell's rim, humming louder and louder | A cone of sound, 10 blocks long, that slowly follows you round, stinging and slowing. Get behind a pillar, or round to its back, which takes a third more meanwhile. |
| **Drawing the tide** | Arms flung wide, water pouring off its bell | It drags the whole floor in toward it while it gathers its fists, then brings them down at its feet. Run out against the pull. Its fists are stuck in the floor a while after. |

- Its bell turns blows from the front, about a third less. Its back and sides take full damage, and recovering after a
  move it takes a third more.
- Struck from the front, it sometimes steps back out of reach. Hurt too fast, it hunches down **under its bell** for 2
  seconds, where blows only ring it and throw you back, and comes out tolling.

### Half beaten

**Cracking its bell.** It grips its bell and wrenches at it, light leaking from a widening crack and a glowing ring
marking the blast. Get out of the ring, then jump the great ring that rolls out after. Then it fights on cracked,
quicker, with its moves in pairs:

- It whirls, hammers and lashes twice over.
- It charges again.
- A second volley of spouts lands where you then stand.
- A second, faster ring rolls off its bell when it tolls, and another when its bell comes down.
- Its peal is wider and turns faster.
- Its undertow drags harder and crushes wider.

### What it gives

**Bell Shard.** Touch it and it tolls for you from then on. When a blow leaves you at or below 30% health, it tolls,
at most once every 3 minutes: hostiles within 6 blocks are thrown back, and you get Absorption II for 8 seconds and
Resistance I for 3 seconds.

## Elites of the wilds

Elites are rare monsters that roam the wilds alone, a step up from the rest, each with a yellow boss bar of its own. They
have no lair and guard no boon. So far there are two.

### Sandmaw Wyrm

A great ringed worm, sand-hided, with a ridge of spines down its back and four jaw-plates round its mouth. 90 health.

**Where:** alone and rarely in deserts, badlands, eroded badlands and wooded badlands. It follows the server's
[monster](monsters.md) settings, `monsters.enabled` and `monsters.spawn_rate`, and has no
switch of its own.

**Under the sand.** It feels footsteps from 16 blocks away, but only 5 if you sneak. A furrow trails it as it swims
after you, and nothing can touch it while it's under.

| Move | The tell | What to do |
|---|---|---|
| **Rising** | The ground under you rumbles for a second | It bursts up and flings everyone about it into the air. Get off the rumbling ground. |
| **Breaching** | A way off, a furrow runs out at you | It leaps 8 to 16 blocks out along the furrow and crashes down. Step off it. It lies **beached** 1.8 seconds after, taking half again as much. |

**Up.** It never makes the same move twice running:

| Move | The tell | What to do |
|---|---|---|
| **Slam** | Rearing back, a line marked | Its head slams down along the line. Step off it. It sticks there 1.5 seconds, taking half again as much. |
| **Spit** | Throat swelling, the edges of a cone marked | A cone of sand: Blindness and Slowness. Step out of the marked edges. |
| **Gulp** | Reared over a marked spot, jaws gaping | It plunges down and swallows whoever is still there, chewing every second for up to 3 seconds. Friends who hit it hard make it choke you back up. If it catches nothing it chokes, open, for 1.8 seconds (half again as much). |
| **Sinkhole** | Head buried, its jaws marked on the sand | Everything within 9 blocks is dragged toward its jaws. Walk out against the pull before they snap. |
| **Geysers** | Plunged into the sand | Geysers burst up under you one after another, five of them. Keep moving. |
| **Spines** | Coiled tight | It looses its spines along 8 rays all round it. Stand in the gaps. |

- While it gulps, drags or sends up geysers, it takes a quarter more.
- After 13 seconds up, or once it has lost a quarter of its health, it dives again.

**Half beaten** it thrashes, throwing back everyone within 7 blocks and taking half meanwhile, then fights on roused: it
slams twice, spits three gouts in a fan, looses a second volley of spines into the gaps, sends up seven geysers, drags
for longer, and breaches again straight back at you.

**What it gives:** 3 to 6 bones, 2 to 4 gold ingots, 1 to 3 emeralds, and a 35% chance of a diamond.

### Timbermaul Bear

A great brown bear half again a bear's size, moss grown thick on its back, a ridge of dark hackles down its spine and old
claw scars down its flanks. A champion, a step above the other elites: 120 health, 6 armour, and a red boss bar.

**Where:** alone and rarely in the taigas. It follows the server's [monster](monsters.md)
settings and has no switch of its own.

**Its warning.** It notices you from 10 blocks away, but only 5 if you sneak. Then it stands up and huffs for two and a
half seconds. Back off 14 blocks and it lets you be, and stays calm a while. Come within 5 blocks, or hit it, and it
fights.

**Its moves.** It never makes the same move twice running:

| Move | The tell | What to do |
|---|---|---|
| **Rake** | A paw drawn up and out to one side | It rakes the paw across its front, from the side it was raised on. Back off out of reach, or get behind it. |
| **Snap** | Head drawn back, jaws wide | It lunges and snaps down a short lane before it. Step aside. |
| **Crush** | Reared up tall | It crushes down on a fan before it, and slows whoever it catches. Get out of the fan, or hit it hard while it's up: a heavy blow **topples** it onto its back for 2.5 seconds. |
| **Pounce** | Crouched low, rump wiggling, a spot marked | It leaps 3 to 10 blocks onto the spot. Leave it. It lies **sprawled** 1.7 seconds after. |
| **Charge** | Head down, pawing the ground | It charges 15 blocks along a line. Step aside. If it runs into a tree or a wall it stands **dazed** 2.2 seconds. |
| **Log** | Claws hooked under a mossy trunk, straining | It heaves the log out of the forest floor and rolls it down a wide lane. Jump it. |
| **Dig** | Rump up, head down, scrabbling (only when you're behind it) | It kicks a spray of earth and stones back: Blindness and Slowness. Get out from behind it. |

- Toppled, sprawled or dazed, it takes **half again** as much. Pinning someone, a quarter more.
- From up close it rakes, snaps and crushes; crowd round it and it crushes more. From a way off it pounces, charges and
  rolls logs. Stand behind it and it digs.

**Half beaten** it rears and roars, throwing back and weakening everyone within 7 blocks and taking half meanwhile, then
fights on roused: its hackles up, its tells quicker, its moves in pairs. It backhands the other way after a rake, snaps
twice, crushes twice, pounces again from where it lands, wheels round and charges again, rolls a second log, and digs a
wider spray. And now it can **hug**: it rears with its forelegs spread wide and falls on whoever stands before it,
pinning them and mauling them for up to 2.5 seconds. Back away before it falls, or topple it with a heavy blow while
it's reared. Once you're pinned, you or a friend can strike it hard to break you free, and it throws you off.

**What it gives:** 4 to 8 leather, 2 to 4 bones, 1 to 3 moss blocks, 2 to 4 emeralds, and a 50% chance of 1 or 2
diamonds.

## Tips and counterplay

- Watch the tell, then the ground. Most moves mark where they'll land: a ring, a line, a shadow or a bubbling spot.
- Where you stand shapes its next move. Get behind it and it reaches back; hang off and it reaches out.
- Save your big hits for the openings. Every boss takes more while it's stuck, sprawled or dazed.
- Don't stop to rest. Three seconds unhurt and it starts to heal.

Related: [Roaming Giants](roaming-giants.md) ·
[The Sword Tombs](../progression/sword-tombs.md) ·
[Monsters of the Wilds](monsters.md)
