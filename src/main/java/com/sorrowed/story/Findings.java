package com.sorrowed.story;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The pages you find lying around in each world. Which one you get depends on where you found
 * it, so the same spot always gives the same page and walking further finds more of them.
 */
public final class Findings {
    private Findings() {}

    private static final Map<String, String[]> POOLS = new LinkedHashMap<>();

    private static void pool(String world, String[][] pages) {
        String[] ids = new String[pages.length];
        for (int i = 0; i < pages.length; i++) {
            ids[i] = "found_" + world + "_" + i;
            Papers.add(ids[i], pages[i][0], pages[i][1], pages[i].length > 2 ? pages[i][2] : null);
        }
        POOLS.put(world, ids);
    }

    /** Registers every found page with Papers. Called from Papers' own static setup. */
    static void registerAll() {
        pool("backup", new String[][] {
                {"(a note, by the pond)", "we used to fish here. we never caught anything. you said the fish were fake and i said everything here is fake and you said not us.\n\nthe pond is still. the float doesn't bob. i sat here for an hour and nothing moved, not even me."},
                {"(a note, in the dirt house)", "first night. we dug in because of the zombies. you put a torch on the wall and said that's our house now.\n\ni rebuilt it exactly. same dirt. same torch. i didn't put the door back. we never had a door. we didn't need one, there were two of us."},
                {"(a note, in the treehouse)", "you can see the whole backup from up here. the house, the well, the oak, the shed. everything i made doors in.\n\nat night, from up here, i can see someone standing in the field. they don't come closer. they don't have to."},
                {"(a note, by the campfire)", "this is where we sat when the server was new and we didn't have a plan. i miss not having a plan.\n\nthe fire doesn't go out. i tried. i poured a whole bucket on it. it's still going."},
                {"(a note, folded small)", "if the backup ever starts moving, if the grass starts swaying or a cloud goes past, get out. go through the arch at the end of the path.\n\nit means the backup has caught up with the real one. it means it's become the day after."},
                {"(a drawing)", "", "first_night"},
        });
        pool("hush", new String[][] {
                {"(a note, pinned to a tree)", "i didn't build this forest. it was just there one morning, behind the oak, like the world grew it in the night.\n\nit's always midnight here. i think it's the midnight i stopped answering everyone."},
                {"(a note, wet)", "the stones in the clearing are in a circle and there's always one more than i counted.\n\ni counted again. there's always one more."},
                {"(a note, on the chapel step)", "i wrote everything i couldn't say and left it on the altar. it's still there. nobody came to read it.\n\nthat's not true. something came. it took the candle."},
                {"(a note, half torn)", "the ones with no faces stand where the rain hits the ground. they don't move toward you. they move toward where you're going to be."},
        });
        pool("drowned", new String[][] {
                {"(a note, in a bottle)", "the water came up through the floor first. then the walls. the house kept the air because i asked it to.\n\ni sit in the air pocket and listen to the sea press on the windows like it wants to come in and say something."},
                {"(a note, waterlogged)", "the lights on the seabed are from my road. i don't know how they got down here. they're still on. they're always on.\n\nsomething big swims between them. it's patient. it's been patient for a long time."},
                {"(a note, on the ribs)", "i found its bones. i don't know what it was. it was bigger than the house.\n\ni think it drowned the same way everything here drowned. slowly. while it was busy with something else."},
                {"(a note, in the sunken room)", "the door is shut. it's always been shut. i never opened it because i knew what was on the other side.\n\nmore water. just more water. that's the worst thing it could have been."},
        });
        pool("hallway", new String[][] {
                {"(a note, on the carpet)", "this is the hallway from the old place. it's longer now. every time i walk it, it's longer.\n\nif you hear the lights buzzing louder, it's adding rooms."},
                {"(a note, in the pool room)", "i don't know why there's a pool. we never had a pool. i think this is a room from somebody else's house.\n\nthe water's warm. something's keeping it warm."},
                {"(a note, in the office)", "rows of desks. every computer is on. every screen shows the same thing: a chat window, and the cursor blinking where someone was about to type.\n\nnobody typed."},
                {"(a note, in a child's hand)", "MY ROOM\nkeep out\n\n(under it, in alex's hand:) i don't remember this room. i don't remember being this small. but the drawings on the wall are mine."},
                {"(a note, at the top of the stairs)", "the stairs go up into the ceiling and stop. there's nothing up there. i've checked.\n\ni still hear someone walking around up there at night."},
        });
        pool("archive", new String[][] {
                {"(a library card)", "BORROWED: everything you meant to say\nDUE: you're overdue\nRENEWALS: none left\n\n(stamped, over and over, in red:) NOT RETURNED"},
                {"(a page, torn from a book)", "every book here is blank because i never wrote any of it down. the shelves just fill up with what i didn't say.\n\nthe burnt ones are the things i said and wished i hadn't."},
                {"(a note, in the catalogue drawer)", "i looked myself up in the catalogue. there's a card for every day. most of them say the same thing.\n\n'fine.'\n'fine.'\n'fine.'"},
                {"(a note, by the pit)", "there's a hole in the floor and a ladder going down. i went down for a long time. there were shelves down there too, and someone reading.\n\nthey didn't look up. i came back up. i'm not going down again."},
                {"(a note, on the map table)", "the archive has a map. the map has the archive on it. on the archive on the map there's a tiny x where i'm standing.\n\nthe x moved when i moved."},
        });
        pool("street", new String[][] {
                {"(a note, through a letterbox)", "the light in my window is on a timer. i set it so people would think someone was home.\n\nit's still on. people still think someone is home. people are right."},
                {"(a flyer, in the shop window)", "MISSING\n\nhave you seen him? last seen online, then not.\n\n(the face on the flyer is torn off. the tear is clean, like it was cut.)"},
                {"(a note, in the school)", "the school is empty but the clock in the hall still ticks. it's always ten to three. ten minutes from going home.\n\ni spent every day here waiting for those ten minutes."},
                {"(a note, by the pond in the park)", "i fed the ducks here. there are no ducks now. there's bread floating on the water anyway, fresh, like someone just threw it."},
                {"(a note, under a car wiper)", "the car's engine is running. the lights are on. the driver's door is open and the seat is warm.\n\ni sat in it. it didn't go anywhere. none of them do."},
        });
        pool("static", new String[][] {
                {"(a page of noise)", "", "scribble"},
                {"(a note, by the wall of screens)", "every screen shows a different room of the backup. one of them shows this room. in that one, there's someone standing behind me.\n\ni didn't turn around."},
                {"(a note, near the mast)", "the red light at the top blinks a pattern. i wrote it down. it's my name. it's been blinking my name since before i got here."},
                {"(a note, at the edge of the hole)", "the black goes down. it's not a hole. it's where the picture didn't load.\n\nif you stand at the edge you can hear the world buffering."},
        });
        pool("orchard", new String[][] {
                {"(a note, in grandad's hand)", "Kept the trees forty years. Never lost a crop. This year the apples came down hard as stones and warm as hands. Not picking them.\n\nThe scarecrow is facing the house again. I did not turn it."},
                {"(a note, in the farmhouse)", "Alex visits on Sundays. Quiet lad. Sits in the orchard with a laptop. Says he is building something. Says he wants to get it finished in time.\n\nIn time for what, I asked. He did not say."},
                {"(a note, in the pumpkin patch)", "every pumpkin here has a face cut in it. i didn't cut them. grandad didn't. they all have the same face.\n\nit's the face from the scarecrow."},
                {"(a note, in the barn)", "the scarecrow in the barn is the first one. grandad took it down when i was little because i cried at it.\n\nit's back on its pole. i'm still crying at it. i'm twenty."},
                {"(a note, on the well)", "the well in the orchard is dry. if you drop a stone in you don't hear it land. you hear it land later. you hear it land behind you."},
        });
        pool("ward", new String[][] {
                {"(a chart, clipped to a bed)", "PATIENT: (name scratched out)\nCONDITION: stable\nVISITORS: none\n\n(in the margin, in alex's hand:) stable means nothing changes. that's the scary part."},
                {"(a note, in the waiting room)", "i sat here for three hours the day they told me. the tv was on. it was a cooking show. i remember every recipe.\n\ni don't remember what they said. i remember the recipes."},
                {"(a note, in the chapel)", "i don't pray. i came in here because it was the only room without a monitor beeping.\n\nit's beeping in here now too."},
                {"(a tag, cold)", "the drawers in the cold room are labelled. one of them has my name on it. it's empty.\n\nit's not empty yet."},
                {"(a nurse's note)", "Bed 4 asked again whether anyone has called. Told him not yet. He said that's alright, his friend is slow, his friend always gets there in the end.\n\nPlease, if you are his friend, come quickly."},
        });
        pool("server", new String[][] {
                {"(a printout)", "[backup] nightly snapshot complete (1 of 1)\n[backup] nightly snapshot complete (2 of 1)\n[backup] nightly snapshot complete (3 of 1)\n\nthere was only ever supposed to be one."},
                {"(a sticky note, on a rack)", "DO NOT POWER OFF\n\n(under it:) if this turns off, the backup stops. if the backup stops, it was never there. if it was never there, neither was i."},
                {"(a note, in the tape room)", "every tape is labelled with a date. every date is the same day. the day before.\n\ni've been backing up the same day over and over so it can never become the next one."},
                {"(a note, by the core)", "the core is warm like something alive. i put my hand on it and felt a heartbeat.\n\nit was mine. it was a few seconds behind."},
                {"(a note, at the monitor wall)", "one screen shows the player list. it says two players online.\n\nyou, and alex.\n\nalex is not here. i've looked."},
        });
    }

    public static String pick(ResourceKey<Level> dim, long salt) {
        Papers.get("");   // make sure the pages (and these pools) are set up
        String world = dim.location().getPath().startsWith("the_") ? dim.location().getPath().substring(4) : "backup";
        String[] pool = POOLS.getOrDefault(world, POOLS.get("backup"));
        long h = salt * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return pool[(int) Math.floorMod(h, (long) pool.length)];
    }
}
