package com.sorrowed.cutscene;

import java.util.HashMap;
import java.util.Map;

/** Every cutscene. ids "arrive_<dimension path>" play the first time you enter a dimension. */
public final class CutsceneLibrary {
    private CutsceneLibrary() {}

    private static final float P = Float.NaN;
    private static final Map<String, Cutscene> MAP = new HashMap<>();

    public static Cutscene get(String id) {
        return MAP.get(id);
    }

    private static void add(Cutscene c) {
        MAP.put(c.id(), c);
    }

    static {
        // First join: the camera looks you in the face, then tips up into the rain.
        add(Cutscene.builder("awakening", 230).blendOut(30).fade(14, 0, 0)
                .key(0, 0, 3.4, 1.7, 180, 0, 50)
                .key(70, 0, 4.2, 2.0, 180, -4, 52)
                .key(135, 0, 2.2, 0.9, 180, -70, 85)
                .key(195, 0, 0, 1.62, 0, P, 70)
                .caption(14, "...it's raining.")
                .caption(85, "That's wrong. It was clear a second ago.")
                .caption(155, "This was {friend}'s world.")
                .cue(0, "minecraft:weather.rain", 1.0F, 1.0F)
                .build());

        // The Hush: a slow circle around you that ends with the camera standing behind you.
        add(Cutscene.builder("arrive_the_hush", 250).blendOut(25).fade(15, 0, 0)
                .orbit(0, 145, 3.6, 2.2, 0, 180, 1.0, 62, 14)
                .key(195, 0, 0, 1.62, 360, P, 70)
                .caption(20, "Why is it so quiet? The rain should be louder.")
                .caption(105, "I don't think this was him. Something else made this.")
                .caption(175, "Don't look back.")
                .cue(0, "minecraft:ambient.cave", 0.8F, 1.0F)
                .cue(150, "minecraft:entity.warden.heartbeat", 0.9F, 1.0F)
                .build());

        // The Drowned Hall: sink beside the platform and look down into it.
        add(Cutscene.builder("arrive_the_drowned", 270).blendOut(30).fade(10, 0, 0)
                .key(0, 0, 0, 1.62, 0, P, 70)
                .key(50, 5, 0, -2.5, 0, 20, 75)
                .key(120, 6, 1, -14, 0, 55, 85)
                .key(195, 6, 1, -24, -30, 70, 90)
                .key(245, 4, 0, -1, 0, 15, 72)
                .caption(25, "Everything's heavy. Even the dark is wet.")
                .caption(115, "There's a house down there. It still has air in it.")
                .caption(195, "I can still hear the rain. Even down here.")
                .cue(8, "minecraft:ambient.underwater.enter", 0.8F, 1.0F)
                .build());

        // The Hallway: a dolly zoom. The corridor stretches while you stand still.
        add(Cutscene.builder("arrive_the_hallway", 270).blendOut(30).fade(12, 0, 0)
                .key(0, 0, 0, 1.62, 0, P, 70, 0)
                .key(110, 0, -6, 1.62, 0, 0, 38, 0)
                .key(175, 0, -6, 1.62, 0, 0, 38, 2)
                .key(235, 0, 0, 1.62, 0, P, 70, 0)
                .caption(15, "I know this carpet.")
                .caption(100, "It's the hallway from his old house. It just never ends.")
                .caption(180, "It stretches when I look at it.")
                .cue(0, "minecraft:block.conduit.ambient", 0.7F, 0.8F)
                .build());

        // The Backup, Alex's world: sunlight, a lazy orbit around his house, and one thing wrong.
        add(Cutscene.builder("arrive_the_backup", 270).blendOut(30).fade(20, 0, 0)
                .orbit(0, 205, 5.0, 2.6, 0, 360, 1.0, 58, 20)
                .key(245, 0, 0, 1.62, 360, P, 70)
                .caption(20, "It's perfect.")
                .caption(100, "Nothing here is moving. Not even the grass.")
                .caption(175, "Even the clouds are holding still.")
                .build());

        // The Archive: the camera tilts up the shelves, then slowly down an aisle that does not end.
        add(Cutscene.builder("arrive_the_archive", 280).blendOut(30).fade(16, 0, 0)
                .key(0, 0, 0.5, 1.2, 0, 40, 60, 0)
                .key(90, 0, 0.5, 1.2, 0, -30, 60, 0)
                .key(170, 0, -8, 1.6, 0, 0, 44, 0)
                .key(245, 0, 0, 1.62, 0, P, 70, 0)
                .caption(20, "Books. Every wall, floor to ceiling.")
                .caption(110, "I opened one. Every page is blank. They all are.")
                .caption(190, "Somewhere in here is everything he never said.")
                .cue(0, "minecraft:item.book.page_turn", 0.8F, 0.7F)
                .build());

        // The Street: from above the road, down past the streetlight, into the rain.
        add(Cutscene.builder("arrive_the_street", 290).blendOut(30).fade(20, 0, 0)
                .key(0, 0, 12, 14, 180, 55, 70, 0)
                .key(120, 3, 6, 4, 160, 20, 62, 0)
                .key(200, 1, 3, 1.8, 180, 4, 66, 1)
                .key(255, 0, 0, 1.62, 0, P, 70, 0)
                .caption(20, "This is {friend}'s street.")
                .caption(110, "I only ever saw it through a webcam.")
                .caption(195, "Every house is dark. Except one.")
                .cue(0, "minecraft:weather.rain", 0.6F, 0.9F)
                .build());

        // The Static: the horizon hisses. The camera shakes a little and cannot settle.
        add(Cutscene.builder("arrive_the_static", 250).blendOut(24).fade(10, 0, 0)
                .key(0, 0, 0.5, 1.62, 0, 6, 70, 0)
                .key(110, 0, -4, 2.2, 0, -4, 52, 0)
                .key(225, 0, 0, 1.62, 0, P, 70, 0)
                .caption(15, "It's not fog. It's noise.")
                .caption(95, "Snow. That's what he used to call it.")
                .caption(170, "A channel nobody is broadcasting on. And I'm standing in it.")
                .cue(0, "minecraft:block.conduit.ambient.short", 1.0F, 0.5F)
                .cue(120, "minecraft:block.conduit.ambient.short", 1.0F, 0.45F)
                .build());

        // The Orchard: low through the rows, and the scarecrow turns its head as the camera passes.
        add(Cutscene.builder("arrive_the_orchard", 270).blendOut(30).fade(16, 0, 0)
                .key(0, 2, 2, 0.9, 20, 4, 62, 0)
                .key(130, -2, -6, 1.3, -15, 2, 56, 0)
                .key(235, 0, 0, 1.62, 0, P, 70, 0)
                .caption(20, "The orchard. Grandad's orchard.")
                .caption(105, "Every tree is dead. Every apple is stone.")
                .caption(185, "The scarecrow is facing me. It was facing the other way.")
                .cue(0, "minecraft:entity.phantom.ambient", 0.4F, 0.4F)
                .build());

        // The Ward: a slow dolly down the corridor under the lamps that are still working.
        add(Cutscene.builder("arrive_the_ward", 290).blendOut(30).fade(18, 0, 0)
                .key(0, 0, 0.5, 1.5, 0, 2, 64, 0)
                .key(150, 0, -10, 1.5, 0, 0, 46, 0)
                .key(255, 0, 0, 1.62, 0, P, 70, 0)
                .caption(20, "A hospital. Why is there a hospital.")
                .caption(110, "Half the lights are out. Something is beeping.")
                .caption(200, "There's a bed by the window. It's been made.")
                .cue(0, "minecraft:block.note_block.bit", 0.5F, 1.9F)
                .cue(60, "minecraft:block.note_block.bit", 0.5F, 1.9F)
                .cue(120, "minecraft:block.note_block.bit", 0.5F, 1.9F)
                .cue(180, "minecraft:block.note_block.bit", 0.5F, 1.9F)
                .build());

        // The Server: along the row, the lights blinking past, then turning to find something at the end of it.
        add(Cutscene.builder("arrive_the_server", 280).blendOut(30).fade(14, 0, 0)
                .key(0, 0, 0.5, 1.4, 0, 0, 66, 0)
                .key(140, 0, -9, 1.4, 0, 0, 58, 0)
                .key(200, 0, -9, 1.4, 25, 2, 50, 0)
                .key(250, 0, 0, 1.62, 0, P, 70, 0)
                .caption(15, "Fans. Hundreds of them. And the lights.")
                .caption(100, "This is where it was kept. His world. Ours.")
                .caption(190, "There's someone at the end of the row. They're standing exactly like me.")
                .cue(0, "minecraft:block.beacon.ambient", 0.8F, 0.5F)
                .build());

        // The grave (anchored on the stone): circle it once.
        add(Cutscene.builder("grave", 300).blendOut(30).fade(8, 0, 0)
                .orbit(0, 235, 4.5, 2.4, 30, 250, 0.8, 55, 18)
                .caption(30, "A stone with no name on it.")
                .caption(110, "He wouldn't have needed one.")
                .caption(190, "{friend}. I came back.")
                .cue(200, "minecraft:block.bell.use", 0.4F, 0.6F)
                .build());

        // An event: the camera turns you around to look at the door that was not there.
        add(Cutscene.builder("door_behind", 175).blendOut(30)
                .key(0, 0, 0, 1.62, 0, P, 70)
                .key(45, 0, 0, 1.62, 180, 2, 62)
                .key(115, 0, 0, 1.62, 180, 2, 48)
                .caption(55, "That door wasn't there a minute ago.")
                .cue(5, "minecraft:entity.warden.heartbeat", 0.9F, 1.0F)
                .cue(45, "minecraft:entity.warden.heartbeat", 1.0F, 1.0F)
                .build());

        // Sorrow reaches 100: the world tilts, drops, and goes dark.
        add(Cutscene.builder("breakdown", 220).blendOut(0).fade(0, 60, 50)
                .key(0, 0, 0, 1.62, 0, P, 70, 0)
                .key(50, 0, 0, 0.5, 0, 25, 45, 25)
                .key(110, 0, 0, 0.15, 15, 40, 30, 60)
                .key(170, 0, 0, 0.1, 30, 50, 24, 85)
                .caption(20, "I can't tell if I'm remembering him or being remembered.")
                .caption(95, "stop looking at me")
                .caption(150, "{friend}?")
                .cue(0, "minecraft:entity.warden.heartbeat", 1.0F, 0.9F)
                .cue(60, "minecraft:entity.elder_guardian.curse", 0.5F, 0.7F)
                .build());

        // The end (anchored on him, at the end of the dock, facing the water). Over his shoulder,
        // round the front of him, his face, the water, and up over the pond into the white.
        add(Cutscene.builder("goodbye", 900).blendOut(0).fade(24, 0, 0)
                .key(0, 3.6, 0.9, 1.9, -90, 16, 60, 0)
                .orbit(60, 420, 3.4, 1.7, 90, -60, 1.0, 58, 16)
                .key(520, -2.0, 0.3, 1.3, 90, 6, 48, 0)
                .key(600, -2.1, 0.3, 1.3, 90, 6, 46, 0)
                .key(690, 0.7, -1.2, 1.35, -70, 14, 62, 0)
                .key(780, 2.5, -3.5, 4.0, -55, 24, 64, 0)
                .key(900, 6.0, -8.0, 9.0, -40, 32, 66, 0)
                .caption(30, "...{friend}?")
                .caption(100, "hey. you took so long.")
                .caption(190, "i'm sorry i made you look in all of them. i couldn't say it, so i built it.")
                .caption(360, "look. the float's moving. it's never done that.")
                .caption(490, "it's the day after. it was always going to be.")
                .caption(615, "go home. it stopped raining.")
                .caption(715, "leave the door open for whoever comes next.")
                .cue(0, "minecraft:block.water.ambient", 0.6F, 0.8F)
                .cue(470, "minecraft:entity.fishing_bobber.splash", 0.9F, 1.0F)
                .cue(520, "minecraft:entity.fishing_bobber.splash", 0.5F, 1.2F)
                .cue(760, "minecraft:block.amethyst_block.resonate", 0.8F, 0.5F)
                .build());
    }
}
