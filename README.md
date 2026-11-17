# Fumes

Dangerous gases with real physics for Minecraft 26.3 (Fabric). Inspired by the gas mechanics of EnviroMine and Glenn's Gases, written from scratch.

**Requirements:** Fabric Loader 0.20.1+, Fabric API 0.166.2+26.3, Java 25. Install on the server and on every client.

## Gases
| Gas | Moves | Danger | Where it comes from |
|---|---|---|---|
| Methane | rises | **explodes** near flames; chokes when dense | coal ore (30% chance when mined), rock pockets above Y 0 |
| Hydrogen sulfide | sinks | **explodes** (weaker), poison, nausea, blindness | rock pockets below Y −32 |
| Carbon dioxide | sinks | suffocation, slowness; puts out fires | rock pockets below Y 0 |
| Carbon monoxide | drifts | weakness, fatigue, then damage | fire, lit furnaces and campfires in rooms without a chimney |
| Steam | rises fast | scalds (a mask doesn't help) | lava meeting water |

Gas shows as a faint coloured haze (denser = more opaque) with particles. It spreads until the concentration evens out, blows away under open sky and stays underground.
Flames that set off gas: fire, lava, torches, magma, jack o'lanterns, and lit campfires, candles and furnaces. **Lanterns are safe.**

## Gas mask
Crafted from leather, an iron ingot, glass panes and charcoal. It protects from every gas except steam and loses 1 durability per second in gas (1200 seconds). Repair it with charcoal on an anvil.

## Commands (operators)
- `/gas spawn <gas> [cells] [amount]` releases a cloud
- `/gas clear [radius]` removes gas around you
- `/gas ignite` sets off the flammable cloud next to you
