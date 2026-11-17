# Fumes

Dangerous gases with real physics for Minecraft 26.3 (Fabric). Inspired by the gas mechanics of EnviroMine and Glenn's Gases, written from scratch.

**Requirements:** Fabric Loader 0.20.1+, Fabric API 0.166.2+26.3, Java 25. Install on the server and on every client.

## Gases
| Gas | Moves | Danger | Where it comes from |
|---|---|---|---|
| Methane | rises | **explodes** near flames; Asphyxia when dense (6+) | coal ore (30% chance when mined), rock pockets above Y 0 |
| Hydrogen sulfide | sinks | **explodes** (weaker); Hydrogen Sulfide Poisoning, nausea, blindness | rock pockets below Y −32 |
| Carbon dioxide | sinks | Asphyxia (3+); puts out fires | rock pockets below Y 0 |
| Carbon monoxide | drifts | Carbon Monoxide Poisoning | fire, lit furnaces and campfires in rooms without a chimney |
| Steam | rises fast | Scalded (a mask doesn't help) | lava meeting water |

## Effects
| Effect | RU | What it does |
|---|---|---|
| Asphyxia | Удушье | 1 damage per second (+1 per level), 15% slower per level |
| Carbon Monoxide Poisoning | Отравление угарным газом | weaker hits, slower mining and walking; from level II, damage every 2 seconds |
| Hydrogen Sulfide Poisoning | Отравление сероводородом | 1 damage every 1.5 s (every second at level II); unlike poison, it can kill |
| Scalded | Ожог паром | 1 damage per second (+1 per level) |

Effects last a few seconds after leaving the gas. Damage ignores armor.

## Advancements
A separate **Fumes** tab: get a gas mask, walk through thick gas in it, choke, get carbon monoxide or hydrogen sulfide poisoning, get scalded by steam, and the goal **Firedamp**: be near a gas cloud when it explodes.

Gas shows as a faint coloured haze (denser = more opaque) with particles. It spreads until the concentration evens out, blows away under open sky and stays underground.
Flames that set off gas: fire, lava, torches, magma, jack o'lanterns, and lit campfires, candles and furnaces. **Lanterns are safe.**

## Gas mask
Crafted from leather, an iron ingot, glass panes and charcoal. It protects from every gas except steam and loses 1 durability per second in gas (1200 seconds). Repair it with charcoal on an anvil.

## Commands (operators)
- `/gas spawn <gas> [cells] [amount]` releases a cloud
- `/gas clear [radius]` removes gas around you
- `/gas ignite` sets off the flammable cloud next to you
