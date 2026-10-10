"""The fifth hundred pairs: the rest of the world and craft runes, each crossed with a fighting rune so the pair
is a spell that wears the craft's theme. Chosen by hand; a pair already written is refused. Prints ten groups of ten,
one per agent file."""
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "src", "main", "java", "dev", "wildercord", "pairs")

PAIRS = """
accord truce
appraise reveal
bait_blessing tidehook
beacon_swell starshard
bellward resonant_shriek
bobber_bell tidewrit
camp_ward emberguard
chalk_line zipper
chalkline decree
courtship soulbond
fathom sounding
fieldsense heartsense
gloomsight shades
gravefinder quietus
haggle red_ledger
headlamp prismatic_burst
hearthpath homeward
herdsense corral
lantern_soul last_lantern
lapis_thrift manawell
lore_reading foresight
lostfind riftcall
lumenpath starlight_tether
lux_reading twin_star
pearl_sight warp
school_sight porpoise
shard_compass cometfall
span skylatch
stand_pose brace
tackle_mend staunch
tide_lantern manatide
tide_marker thunder_tide
toolmend tinker_hum
treasure_sense geode
watchweft stormweave
water_reading quench
waymark wayfarer_hymn
barkstrip barkhide
berrybless lifebloom
blastward basalt_surge
blockpack ironhold
brickwork shellback
compost infest
concreteset fossilize
coppice clockroot
deepsound deepwarn
fell gash
fodder steedmend
gangue magma
glyph_carve hexguard
gourdcall feastday
holefill weigh
keepsafe guardlink
land_reading surefoot
levelground thunderquake
millstone bonespur
nest_tend tend
orepluck long_arm
plankway skimstep
plowline tusk_charge
plumbline lodepull
prospect primer
reed_cut disarm
relic_sense entropy
sandbar shrug_off
saplingrise bloom
shoreup stoutheart
siftfall downdraft
stalkrise soar
stilt summit_wind
strata_rise aftercare
tillage sapflow
tunnel warp_step
vein blood_thread
wildflower honeydew
basinfill boiling_surge
brimming dew_drink
dewcatch frostbloom
lily_path bloomstep
milkmaid restore
mooring_call nullcatch
pocket_current current
refloat upwell
shipwreck_sense inkveil
shoal_herd morale
springbed drowse
sign_glow nullify
slime_sense shulkershell
trailblaze fieldstride
dewkeep frostwire
henhouse second_bell
chestsort worst_first
night_seam veil
packtidy infinity
restock savor
stow hollow
hollowsense softfoot
skyglyph glidewind
snuff_out ashen_veil
village_sense beastguard
"""


def written():
    have = set()
    for f in glob.glob(os.path.join(SRC, "b*", "*.java")):
        with open(f, encoding="utf-8") as src:
            have.update(re.findall(r'@Pair\(a = "(\w+)", b = "(\w+)"', src.read()))
    return have


def main():
    known = {}
    with open(os.path.join(HERE, "runes.tsv"), encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or not line.strip():
                continue
            r = line.rstrip("\n").split("\t")
            known[r[0]] = r
    have = written() if "--groups" not in sys.argv or "--fresh" in sys.argv else set()
    seen, out, bad = set(), [], []
    for line in PAIRS.split("\n"):
        if not line.strip():
            continue
        a, b = sorted(line.split())
        if a not in known or b not in known or a == b:
            bad.append(line)
            continue
        if (a, b) in seen or (a, b) in have:
            bad.append("dup " + line)
            continue
        seen.add((a, b))
        out.append((a, b))
    if bad:
        print("bad:", bad)
        return 1
    print(len(out), "pairs")
    if "--groups" in sys.argv:
        for g in range(0, len(out), 10):
            print(f"group {g // 10 + 1}:")
            for a, b in out[g:g + 10]:
                ra, rb = known[a], known[b]
                print(f"  {a} + {b}")
                print(f"    {a} ({ra[1]}, {ra[4]}, {ra[5]}): {ra[7]}")
                print(f"    {b} ({rb[1]}, {rb[4]}, {rb[5]}): {rb[7]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
