import json, math, os
from PIL import Image, ImageDraw, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "..", "docs", "audit", "evidence", "2026-11-23-oathbound-refs")
os.makedirs(OUT, exist_ok=True)

KNIGHTS = {
    "Bastion": {
        "color": (255, 208, 72),
        "weapon": "mace+shield",
        "moves": [
            ("Aegis Toss","OVERHAND_THROW","LINE","throw shield forward, recover"),
            ("Earthsplit Stomp","STOMP","RING","lift leg, slam shockwave ring"),
            ("Mace Overhead","OVERHEAD_SLAM","CONE","both arms high, vertical slam"),
            ("Shield Wall Slide","SHIELD_PUSH","LINE/DASH","shield-first dash, planted feet"),
            ("Whirling Bulwark","SPIN_SWEEP","RADIUS","full spin, arms out"),
            ("Gauntlet Uppercut","UPPERCUT","CONE","crouch then rising punch/launch"),
            ("Anvil Drop","LEAP_SLAM","RADIUS/LEAP","leap, both arms overhead, crash"),
            ("Cross-Guard Counter","CROSS_CUT","CONE","crossed guard then X cut"),
            ("Shield Ricochet","WEAPON_TWIRL","MULTI_HIT","shield twirl, repeated hits"),
            ("Mace Sweep Low","HORIZONTAL_SWEEP","WIDE_ARC","low wide sweep, slow debuff"),
            ("Bulwark Roar","ROAR","RADIUS","arms wide, chest out, fear roar"),
            ("Seismic Line","KNEEL_STRIKE","LINE","kneel, slam ground line crack"),
            ("Sun-Anvil Judgment","OVERHEAD_SLAM","CROSS","empowered overhead cross slam"),
        ],
    },
    "BladeChampion": {
        "color": (230, 56, 48),
        "weapon": "greatsword",
        "moves": [
            ("Lunge Thrust","THRUST","LINE/DASH","forward lunge, blade point first"),
            ("Rising Slash","UPPERCUT","CONE","low to high rising slash, launch"),
            ("Cross Cut","CROSS_CUT","WIDE_ARC","two diagonal cuts form X"),
            ("Blade Flurry","FLURRY","MULTI_HIT","rapid alternating cuts"),
            ("Spin Slash","SPIN_SWEEP","RADIUS","360 spin slash"),
            ("Backstep Slash","BACKSTEP","CONE/BACKSTEP","step back, counter slash"),
            ("Mirror Stance","GUARD_BRACE","CONE","guard brace, riposte pose"),
            ("Severing Arc","HORIZONTAL_SWEEP","WIDE_ARC","huge horizontal sever arc"),
            ("Piercing Rain","LEAP_SLAM","RADIUS/LEAP","leap, downward stab rain"),
            ("Blade Bind","SHIELD_PUSH","CONE","bind/push, weakness debuff"),
            ("Crimson Tempest","SPIN_SWEEP","MULTI_HIT","empowered multi-spin tempest"),
            ("Executioner Drop","OVERHEAD_SLAM","LINE","huge overhead execution chop"),
            ("Phantom Strike","DIAGONAL_SLASH","TWIN_LINE/DASH","dash through, twin-line slash"),
        ],
    },
    "Halberdier": {
        "color": (72, 240, 255),
        "weapon": "halberd",
        "moves": [
            ("Glaive Sweep","HORIZONTAL_SWEEP","WIDE_ARC","wide halberd sweep"),
            ("Pole-Vault Kick","UPPERCUT","CONE","pole plant, rising kick/launch"),
            ("Storm Jab","FLURRY","MULTI_HIT","rapid jab flurry"),
            ("Cyclone Dance","SPIN_SWEEP","MULTI_HIT/DASH","spinning dash cyclone"),
            ("Lightning Lunge","THRUST","LINE/DASH","fast lightning thrust lunge"),
            ("Reaping Circle","WEAPON_TWIRL","PULL_RADIUS","twirl halberd, pull inward"),
            ("Glaive Throw","OVERHAND_THROW","LINE","throw halberd forward"),
            ("Thunder Stomp","STOMP","CROSS","stomp, cross shockwave"),
            ("Rising Glaive","UPPERCUT","CONE","rising glaive uppercut"),
            ("Shadow Clone Strike","DIAGONAL_SLASH","TWIN_LINE","afterimage twin diagonal"),
            ("Whirlwind Pull","SPIN_SWEEP","PULL_RADIUS","spin pull, weakness"),
            ("Skyfall Cleave","LEAP_SLAM","LINE/LEAP","leap and cleave line"),
            ("Stormbreaker","OVERHEAD_SLAM","CROSS","empowered overhead cross break"),
        ],
    },
    "BannerMarshal": {
        "color": (255, 224, 102),
        "weapon": "sunblade+aegis",
        "moves": [
            ("Banner Sweep","HORIZONTAL_SWEEP","WIDE_ARC","banner/sword wide sweep"),
            ("Sunlance Thrust","THRUST","LINE","straight sunlance thrust"),
            ("Rallying Cry","ROAR","RADIUS","raise banner, rallying roar"),
            ("Solar Flare","CAST_RAISE","RADIUS","arms raised, solar blind flare"),
            ("Banner Charge","SHIELD_PUSH","LINE/DASH","banner-forward charge"),
            ("Judgment Beam","CAST_RAISE","LINE","raise weapon, long beam line"),
            ("Aegis Bash","SHIELD_PUSH","CONE","shield bash, slow"),
            ("Dawn Uppercut","UPPERCUT","CONE","rising dawn uppercut"),
            ("Consecrated Ground","KNEEL_STRIKE","RING","kneel, consecrate ring"),
            ("Heavenly Lance","OVERHAND_THROW","RADIUS","throw lance down, radius impact"),
            ("Sovereign Riposte","GUARD_BRACE","CONE","guard brace riposte"),
            ("Banner Plant","CAST_RAISE","RADIUS","plant banner, buff/heal aura"),
            ("Solar Eclipse","OVERHEAD_SLAM","CROSS","empowered overhead eclipse cross"),
        ],
    },
}

def font(size):
    for name in ["arial.ttf", "segoeui.ttf", "DejaVuSans.ttf"]:
        try:
            return ImageFont.truetype(name, size)
        except Exception:
            pass
    return ImageFont.load_default()

F_T = font(28)
F_M = font(18)
F_S = font(14)
F_X = font(12)

# Simple stick-rig key pose sketches by archetype. Returns list of draw ops.
# Coordinates in a 280x180 cell.

def draw_pose(d, ox, oy, arch, color):
    # body base
    cx, cy = ox+140, oy+95
    head=(cx,cy-38); neck=(cx,cy-28); hip=(cx,cy+8)
    shL=(cx-18,cy-22); shR=(cx+18,cy-22)
    elL=(cx-30,cy-5); elR=(cx+30,cy-5)
    haL=(cx-34,cy+12); haR=(cx+34,cy+12)
    knL=(cx-10,cy+32); knR=(cx+10,cy+32)
    ftL=(cx-12,cy+58); ftR=(cx+12,cy+58)
    wpnA, wpnB = (cx+34,cy+12), (cx+34,cy-55)
    def L(a,b,w=3,c=(220,220,220)): d.line([a,b], fill=c, width=w)
    def C(p,r,c=(220,220,220)): d.ellipse([p[0]-r,p[1]-r,p[0]+r,p[1]+r], outline=c, width=2)
    # archetype-specific overrides
    if arch=="OVERHEAD_SLAM":
        elL,elR,haL,haR=(cx-10,cy-45),(cx+10,cy-45),(cx-8,cy-70),(cx+8,cy-70); wpnA,wpnB=(cx,cy-70),(cx,cy-120)
        hip=(cx,cy+12); knL,knR,ftL,ftR=(cx-12,cy+36),(cx+12,cy+36),(cx-14,cy+58),(cx+14,cy+58)
    elif arch=="HORIZONTAL_SWEEP":
        elR,haR=(cx+45,cy-15),(cx+70,cy-18); wpnA,wpnB=(cx+70,cy-18),(cx+120,cy-25); neck=(cx-4,cy-28); head=(cx-4,cy-38)
    elif arch=="DIAGONAL_SLASH":
        elR,haR=(cx+35,cy-35),(cx+55,cy-55); wpnA,wpnB=(cx+55,cy-55),(cx+85,cy-95)
    elif arch=="THRUST":
        elR,haR=(cx+35,cy-18),(cx+60,cy-12); wpnA,wpnB=(cx+60,cy-12),(cx+125,cy-12)
        knL,ftL=(cx-18,cy+30),(cx-28,cy+58); knR,ftR=(cx+18,cy+35),(cx+28,cy+58)
    elif arch=="UPPERCUT":
        elR,haR=(cx+20,cy+5),(cx+24,cy-45); wpnA,wpnB=(cx+24,cy-45),(cx+28,cy-100)
        hip=(cx,cy+14); knL,knR=(cx-10,cy+38),(cx+10,cy+38)
    elif arch=="SPIN_SWEEP":
        elL,elR,haL,haR=(cx-45,cy-15),(cx+45,cy-15),(cx-75,cy-10),(cx+75,cy-10)
        wpnA,wpnB=(cx-75,cy-10),(cx+75,cy-10)
    elif arch=="LEAP_SLAM":
        hip=(cx,cy-5); knL,knR,ftL,ftR=(cx-18,cy+10),(cx+18,cy+18),(cx-24,cy+30),(cx+24,cy+38)
        elL,elR,haL,haR=(cx-12,cy-35),(cx+12,cy-35),(cx-8,cy-60),(cx+8,cy-60); wpnA,wpnB=(cx,cy-60),(cx,cy-115)
    elif arch=="SHIELD_PUSH":
        elL,haL=(cx-30,cy-10),(cx-45,cy-8); wpnA,wpnB=(cx-45,cy-8),(cx-75,cy-8)
        knL,ftL=(cx-18,cy+30),(cx-30,cy+58); knR,ftR=(cx+16,cy+36),(cx+24,cy+58)
    elif arch=="STOMP":
        knR,ftR=(cx+18,cy+12),(cx+22,cy+28); knL,ftL=(cx-10,cy+35),(cx-12,cy+58)
        hip=(cx,cy+12)
    elif arch=="ROAR":
        elL,elR,haL,haR=(cx-40,cy-30),(cx+40,cy-30),(cx-58,cy-45),(cx+58,cy-45)
        wpnA,wpnB=(cx+58,cy-45),(cx+58,cy-95); head=(cx,cy-44)
    elif arch=="OVERHAND_THROW":
        elR,haR=(cx+25,cy-40),(cx+35,cy-65); wpnA,wpnB=(cx+35,cy-65),(cx+75,cy-95)
    elif arch=="GUARD_BRACE":
        elL,haL=(cx-28,cy-12),(cx-40,cy+5); elR,haR=(cx+25,cy+5),(cx+30,cy+25)
        wpnA,wpnB=(cx+30,cy+25),(cx+30,cy-30)
    elif arch=="CAST_RAISE":
        elL,elR,haL,haR=(cx-15,cy-45),(cx+15,cy-45),(cx-12,cy-72),(cx+12,cy-72)
        wpnA,wpnB=(cx+12,cy-72),(cx+12,cy-120)
    elif arch=="FLURRY":
        elL,elR,haL,haR=(cx-35,cy-10),(cx+35,cy-20),(cx-55,cy+5),(cx+58,cy-30)
        wpnA,wpnB=(cx+58,cy-30),(cx+95,cy-45)
    elif arch=="BACKSTEP":
        knL,ftL=(cx-22,cy+30),(cx-34,cy+58); knR,ftR=(cx+16,cy+38),(cx+26,cy+58)
        elR,haR=(cx+30,cy-12),(cx+50,cy-5); wpnA,wpnB=(cx+50,cy-5),(cx+90,cy-15)
    elif arch=="WEAPON_TWIRL":
        elR,haR=(cx+30,cy-25),(cx+45,cy-35); wpnA,wpnB=(cx+45,cy-35),(cx+45,cy+35)
        d.arc([cx+20,cy-60,cx+70,cy-10], 0, 360, fill=color, width=2)
    elif arch=="KNEEL_STRIKE":
        hip=(cx,cy+20); knL,knR,ftL,ftR=(cx-14,cy+38),(cx+18,cy+34),(cx-18,cy+58),(cx+28,cy+52)
        elR,haR=(cx+30,cy-5),(cx+55,cy+25); wpnA,wpnB=(cx+55,cy+25),(cx+95,cy+55)
    elif arch=="CROSS_CUT":
        elL,elR,haL,haR=(cx-30,cy-30),(cx+30,cy-30),(cx-50,cy-55),(cx+50,cy-5)
        wpnA,wpnB=(cx-50,cy-55),(cx+70,cy+25); d.line([(cx+50,cy-55),(cx-40,cy+20)], fill=color, width=2)

    for a,b in [(neck,hip),(neck,shL),(neck,shR),(shL,elL),(elL,haL),(shR,elR),(elR,haR),(hip,knL),(knL,ftL),(hip,knR),(knR,ftR)]:
        L(a,b,3)
    C(head,8)
    d.line([wpnA,wpnB], fill=color, width=4)
    d.ellipse([wpnB[0]-4,wpnB[1]-4,wpnB[0]+4,wpnB[1]+4], fill=color)


for knight, spec in KNIGHTS.items():
    moves = spec["moves"]
    cols, rows = 4, 4
    pad = 20
    cw, ch = 320, 250
    W = pad + cols*(cw+pad)
    H = 110 + rows*(ch+pad) + 40
    img = Image.new("RGB", (W,H), (18,18,22))
    d = ImageDraw.Draw(img)
    d.text((pad,20), f"Oathbound {knight} - 13 Technique Reference Sheet", fill=spec["color"], font=F_T)
    d.text((pad,58), f"Weapon: {spec['weapon']}  |  1:1 key-pose targets for OathboundPose archetypes", fill=(180,180,180), font=F_M)
    for i,(name,arch,shape,note) in enumerate(moves):
        r,c = divmod(i, cols)
        x = pad + c*(cw+pad)
        y = 100 + r*(ch+pad)
        d.rectangle([x,y,x+cw,y+ch], outline=(70,70,80), width=2)
        d.rectangle([x,y,x+cw,y+34], fill=(35,35,42))
        d.text((x+8,y+7), f"{i+1}. {name}", fill=(245,245,245), font=F_S)
        draw_pose(d, x+20, y+30, arch, spec["color"])
        d.text((x+8,y+ch-38), f"{arch} | {shape}", fill=spec["color"], font=F_X)
        # wrap note
        d.text((x+8,y+ch-22), note[:46], fill=(170,170,170), font=F_X)
    out = os.path.join(OUT, f"{knight.lower()}-reference-sheet.png")
    img.save(out)
    print(out)
