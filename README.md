# Sorrowed (Forge 1.20.1)

It is raining the moment you join. It never stops. This was your friend's world, he is gone, and he left it open for you.

## What it does
- **Programmer Art** switches on when you join (client side, once).
- **Eternal rain** in the overworld, with two exceptions the events can cause: a sudden *silence*, and thunder.
- **Typewriter thoughts** above the hotbar: monospaced letters, blinking cursor, soft key clicks. At high Sorrow, letters glitch.
- **Book and Quill** on first join, a letter from him explaining why he wanted you here.
- **A grave** a few steps from the first player on first join.

## The psychological layer: Sorrow
Every player has a hidden **Sorrow** meter (0-100). Rain on open sky, night, the Hush, the Hallway, the Drowned Hall and nearby Hollows/Listeners raise it. Shelter, graves, the dry Unfinished, sleeping and the Mason lower it.

| Tier | Sorrow | What changes |
|---|---|---|
| 1 Uneasy | 30+ | Thoughts turn inward; footsteps behind you; the rain can stop dead |
| 2 Haunted | 55+ | Text glitches; vignette closes in; your "friend" types in chat; fake grave markers; Reflection; Listeners; thunder |
| 3 Breaking | 80+ | Heartbeat, Darkness pulses, static; fake "joined/left the game"; a gate appears behind you; ring of Hollows; an Echo right behind you |

Events roll every 10 seconds per player with a cooldown. Test with `/sorrowed sorrow <0-100>` and `/sorrowed event <name>` (op only): footsteps, silence, whisper, thunder, ghostgrave, reflection, listener, fakejoin, doorbehind, hollowcircle, echoclose, mason.

## Entities: new names, custom bodies, one gimmick each
Registry ids are unchanged (`sorrowed:echo` etc.); only the display names changed. Seven have hand-built models (`client/ModModels.java`) with their own animation; the Afterwalker wears your skin on a player-shaped body.

| Name | Id | Body | Gimmick |
|---|---|---|---|
| **Vigil** | echo | thin, 2.3 blocks, head bowed, arms to the knees, see-through | Keeps its distance in the rain. **When the rain stops it walks straight at you.** Never attacks; fades on arrival (+Sorrow). |
| **The Unsaid** | hollow | hunched, faceless, 2.6 blocks, arms long enough to drag | Spawns on open ground only in rain. **Won't attack anyone holding a lantern.** |
| **Hark** | listener | huge round head, sewn-shut eyes, twitching ears, stooped | Blind. **Only moves toward noise** (sprinting, jumping, swinging, using items). Stay quiet and it stands beside you. |
| **Afterwalker** | reflection | your own skin, translucent | **Walks the exact path you walked, 10 seconds behind.** Stop and it catches up. Stare for 3s or let it near and it's gone. |
| **The Finisher** | mason | stocky, yellow hard hat, tool belt, hammer | In the Unfinished. **Lays a plank bridge under his own feet.** Near him, Sorrow falls. Hit him: "I wasn't done." |
| **The Archivist** | archivist | 2.7 blocks of ink-dark robe, long neck, small bowed head, a book held up to read | In the Archive. **Only moves while you are not looking at it.** When it reaches you it presses a blank page into your hand (+Sorrow) and is gone. |
| **The Neighbour** | neighbour | ordinary height, long tan coat, wide hat, face in shadow | On the Street, under the lights. **Always looks at you; walks away as you approach, never hurrying.** Sprint at it and a door closes somewhere, and it is gone. |
| **The Carrier** | carrier | a wood-veneer television for a head, two bent antennae, grey suit, 2.4 blocks | In the Static. **Watching the screen raises Sorrow**; stare long enough and it is suddenly behind you. The third time, it switches off for good. |

Other name ideas, if you want to swap: Echo: *The Lingerer, Standing Grief*. Hollow: *Faceless Regret, Hush-Born*. Listener: *The Still One, Earless*. Reflection: *Ten Seconds Ago, The Retrace*. Mason: *Last Builder, Foreman of Nothing*. Edit `assets/sorrowed/lang/en_us.json`.

## Cutscenes
A real camera system: the camera swaps to an invisible armor stand and follows keyframes (`cutscene/CutsceneLibrary.java`). The HUD and hand are hidden, controls locked, letterbox bars and fades drawn, and the player gets Resistance V so nothing can hurt them. Captions use fast typewriter text.
- **awakening**: first join. The camera looks you in the face, then tips up into the rain.
- **arrive_the_unfinished / the_hush / the_drowned / the_hallway / the_backup / the_archive / the_street / the_static**: first arrival in each dimension. Pull-up over the floating chunks; a slow circle that ends with the camera behind you; a dive into the water; a dolly zoom down the corridor; a lazy orbit in the sun; a tilt up the shelves and a drift down an aisle; a descent from above the road past the streetlight; a shaky stare into the noise.
- **grave**: first time you touch a grave marker. Circles the stone, then lowers Sorrow.
- **door_behind**: the camera turns you around to see the gate that wasn't there.
- **breakdown**: Sorrow hits 100. The camera drops and rolls, fades to black, and Sorrow resets to 55.
Turn off with `cutscenes = false`.

## Typewriter text is now rare
Idle thoughts: at most one every `ambientThoughtMinutes` (default 8), at a low chance per check. Dimension-arrival thoughts happen once (as the cutscene). Returning to the world speaks one time in three. The overlay drops extra thoughts if two are already waiting. Thoughts caused by events, entities and graves still fire.

## Dimensions
All of them except the Hush come from one chunk generator (`worldgen/LiminalGenerator.java`, `"mode"` in the dimension JSON). Every block is a pure function of its coordinates, so worlds are identical across seeds and nothing depends on neighbouring chunks. The hub in the Unfinished has a gate to each of the other seven.

- **The Unfinished**: floating monoliths, slabs and long ribbons. The monoliths now carry **half-built wall rings** he stopped mid-row, scaffold posts with torches at the corners; the slabs have crafting tables and ladders to nothing; pale ash drifts in the air.
- **The Hush**: now its own biome (`sorrowed:hush`): a dark forest with the music gone, a black sky, ash in the air, and only Hollows and Harks spawning. No sky (custom dimension effects), close black-green fog, standing stones around a soul fire, and a **Chapel of Quiet** at the hub.
- **The Drowned Hall**: a 100-block-deep sea with a rolling seabed, **long trenches**, sand shallows and clay flats, **kelp forests**, seagrass and sea pickles, rock spires breaking the surface, and **the streetlights from his road standing lit on the seabed**; his house on the sea floor with an air pocket, and a **lighthouse that never lit**.
- **The Hallway**: no sky, yellow haze, and now four kinds of room: the **living room** with the television showing static (1 in 17), a **bedroom** with the bed made and the lamp on (1 in 13), a **kitchen** nobody has cooked in (1 in 11), and the **wrong room** (1 in 41): black carpet, black walls, lights off.
- **The Backup**: perfectly smooth meadow, **the same tree dead-centre in every chunk**, six flower kinds on a rigid grid, **a gravel path grid with white picket fences and lit lamp posts at every crossing**, warm bright fog, and **The Set Table** cottage.
- **The Archive** (new): an endless library under a low beamed ceiling. Two aisles of bookshelves per chunk, six high, with gaps to slip through; shelf walls with a doorway one chunk in five; hanging lanterns; **candles that never burn down**; ladders that reach nothing; and reading rooms (1 in 7) with a long table, chairs pulled out, and book stacks on the floor. Brown fog, no sky, every page blank. Home of **The Archivist**.
- **The Street** (new): the road he grew up on, at night, in the rain. A road with a dashed line and kerbs every 48 blocks, one identical house per lot on both sides (white walls, gabled roof, a cold chimney, letterbox, the odd spruce), **sodium streetlights**, and **one window lit in seven**. Rain falls here. Home of **The Neighbour**.
- **The Static** (new): a flat plain of black, white and grey noise that goes on forever, like a television tuned to nothing. **Antenna masts with one red light each**, dead pixels standing up out of the ground, a hiss on the wind, and fog that **breathes and flickers**. Home of **The Carrier**.

## Structures
Overworld: **Stone Gate**, **Half-Built House**, **Grave**, **Lighthouse** (dark lamp, a keeper's log in a chest at the top), **Vigil Ring** (18 lit candles around a lectern holding a book of names; the last name is yours), **Bus Stop** (a ticket in the lost-property chest). Hush: **Standing Stones**, **Vigil Rings**, **Graves**, **Chapel of Quiet**. Plus the gate that appears behind you at Sorrow 80+.

## Config (`config/sorrowed-common.toml`)
`friendName` (default "Remi"), `eternalRain`, `programmerArt`, `giveBook`, `echoes`, `psychologicalEvents`, `cutscenes`, `ambientThoughtMinutes`.

## Building
Push to GitHub, **upload the extracted files** (not the zip), and the workflow in `.github/workflows/build.yml` builds the jar. Download it from the run's **Artifacts** (`sorrowed-jar`).
Locally: JDK 17 and `gradle build` (Gradle 8.1.1). The jar lands in `build/libs/sorrowed-1.0.0.jar`.

## If the jar errors out
| Symptom | Cause / fix |
|---|---|
| `No settings.gradle found` | The repo holds the zip or a nested folder. The workflow now auto-extracts a zip and finds a nested root; still best to upload the extracted contents. `.github` is a hidden folder, make sure it uploaded. |
| `Unsupported class file major version` / toolchain errors | Not building on JDK 17. |
| `Could not find net.minecraftforge:forge:1.20.1-...` | Bump `forge_version` in `gradle.properties` to a current 47.x. |
| Game crashes with `NoSuchMethodError` / `NoSuchFieldError` | Jar not reobfuscated. `build.gradle` must have `finalizedBy 'reobfJar'` on the jar task. |
| `Mod file ... has missing/invalid metadata` | Unexpanded `${...}` in `mods.toml`, or `mod_id` in `gradle.properties` does not match `@Mod("sorrowed")`. The workflow checks both. |
| Missing textures (purple/black) or `Missing model` in the log | Typo in a model/blockstate path. The workflow validates every reference. |
| World fails to load with a registry/dimension error | Bad JSON in `data/sorrowed/`. The workflow parses all JSON before building. |
| `Duplicate registry key` / `Failed to load registry` on a world made before the mod | Back up the world; a datapack-style dimension added later loads fine, but a broken JSON blocks the whole world. |

Textures are generated placeholders (`python3 tools/gen_textures.py`); replace the PNGs freely.

## More ideas (not built yet)
- **Sorrow**: a lantern item that lowers it, a journal that records what you've seen, a dimension that only opens at tier 3.
- **Entities**: *The Typist* (hunched at a desk in the Hush, types your thoughts back), *Umbrella Man* (stands in the rain holding an umbrella, shelters you), *Reflection* (appears in puddles), *The Kept* (mobs that only stare).
- **Dimensions**: *The Drowned Hall* (a flooded copy of his house), *The Static* (the world glitching between versions), *The Backup* (the world before he left: sunny, perfect, frozen).
- **Portals**: puddle gates that open on stormy nights, sleeping in his bed, typing a sign at the right place.
- **Structures**: flooded library, a lighthouse that never lights, memorial wall of names, a server-room ruin, an unfinished tower.
