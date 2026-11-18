package com.sorrowed.story;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The papers. Every note in the world is one wrinkled sheet: either his handwriting or a pencil
 * drawing. He never used capitals. He never used them in chat either.
 */
public final class Papers {
    private Papers() {}

    /** drawing is the name of a texture under textures/gui/drawing_<name>.png, or null for text. */
    public record Page(String id, String title, String text, String drawing) {}

    private static final Map<String, Page> PAGES = new LinkedHashMap<>();

    private static void text(String id, String title, String body) {
        PAGES.put(id, new Page(id, title, body, null));
    }

    private static void drawing(String id, String title, String drawing, String caption) {
        PAGES.put(id, new Page(id, title, caption, drawing));
    }

    static {
        text("welcome_1", "for you",
                "hey. if you're reading this you got in. i'm not here to say hi, sorry.\n\n"
                + "i'm sorry about the rain too. i didn't do that. it started the day i stopped logging in and "
                + "it hasn't stopped. i think the world is doing it. i think it's doing a lot of things now.\n\n"
                + "everything looks old on purpose. i wanted you to see it the way we first did. ugly. crooked. ours.");
        text("welcome_2", "for you (2)",
                "this is the backup. the world the day before. i made it so there'd be a version where nothing happened yet.\n\n"
                + "it's not the same though. nothing moves. i tried to make the grass move and it wouldn't.\n\n"
                + "everything i built here has a door in it. i didn't put them there. they were just there one morning, "
                + "in the back of the house, down the well, inside the tree. i think the world made them out of me.");
        text("welcome_3", "for you (3)",
                "don't go in the clinic one first. please. go in the others. the house is fine. the house is the hallway "
                + "from my old place and it just goes on but it's fine.\n\n"
                + "the street is my street. the one window that's on is mine. i'm not in it. i've checked.\n\n"
                + "the orchard was grandads. the scarecrow was his. it didn't used to look at you.");
        text("welcome_4", "for you (4)",
                "if someone is standing far off in the rain it's just an echo of me. it can't hurt you. it doesn't know how to leave.\n\n"
                + "the ones with no faces are everything i never said. don't let them hold you.\n\n"
                + "there's a thing in the archive that reads and never turns the page. that's me too, i think. every version of me "
                + "that didn't say it.\n\n"
                + "thank you for coming back. keep it a little loud in here. it's so quiet.\n\n- alex");
        drawing("first_night", "(a drawing)", "first_night", "us. first night. it rained then too. i drew this in the ward.");

        text("notes", "notes, in the chest",
                "todo\n- finish the roof\n- finish the door\n- tell them\n- tell them\n- tell them\n\n"
                + "they let me have the laptop for an hour a day. i spent it here. building you a world where nothing's wrong.\n\n"
                + "i'll leave the lamp on in case you forget the way back.");
        text("last", "the last page",
                "i didn't get to finish. you did. that was the whole point.\n\n"
                + "go home. leave the door open for whoever comes next. somebody always comes next.\n\n- alex");
        text("keeper", "the keeper's log",
                "the lamp has never been lit. i keep it dark on purpose. if i light it someone will see this place from the sea "
                + "and come looking for what's in it.\n\n"
                + "day 1. day 2. day 3. the rain has no day. i stopped counting. you would have kept counting.\n\n"
                + "the water came up to the house. the house still has air in it. i don't know why. i go down and sit in it.");
        text("names", "names",
                "alex\n(crossed out)\n(crossed out)\n(crossed out)\n(crossed out)\n\n"
                + "these are the people who stayed. none of them stayed long. i don't blame them. i was hard to stay with.\n\n"
                + "the last name on the page is yours. i wrote it in pencil in case you wanted out.");
        text("ticket", "one way",
                "ONE WAY\n\ndeparts: never\narrives: here\npassenger: you\n\n"
                + "please remain seated. the bus is coming. the bus has always been coming. i waited for it every day after school. "
                + "i was always the last one at the stop.");
        text("unsaid", "what i never said",
                "i'm sorry i stopped answering.\n\ni'm sorry i said i was fine.\n\ni'm sorry i made you carry the quiet.\n\n"
                + "i knew in march. i didn't tell anyone until july. you were the person i most wanted to tell and the person "
                + "i least could.\n\ni'm not sorry i let you in.");
        text("reply", "the reply",
                "i knew you'd come. i set the table just in case. two cakes. i couldn't remember which one you liked.\n\n"
                + "this place is only what i remember so it's smaller than it should be. nothing here can hurt you.\n\n"
                + "that's how you know it isn't me. i would have found some way to hurt you. i always did.\n\n- alex");

        drawing("archivist", "(a page)", "scribble", "");
        drawing("gate", "(a drawing)", "gate", "the gate at the end of the path. i drew it before it was there.");
        text("neighbour", "(a note, folded twice)",
                "you're standing where he used to stand.\n\nthe light in that window is on a timer. nobody set it. nobody turned it off.\n\n"
                + "go inside. you shouldn't be out in this. he shouldn't have been either.\n\n- a neighbour");
        drawing("street", "(a drawing)", "street", "my street. one window on. it's always on. i used to leave it on so you'd know i was home.");
        text("orchard", "(a note, pinned to the fence)",
                "grandad said the scarecrow was there to keep things off the trees. he never said what things.\n\n"
                + "after he went the apples stopped being apples. they're stone now. i picked one up and it was warm.\n\n"
                + "don't go in without a light. it doesn't like the light. it's the only thing it doesn't like.");
        text("ward", "(a note, in a hospital hand)",
                "visiting hours are 2 till 8. he's in the bed by the window. he asked us to leave the laptop.\n\n"
                + "he keeps saying someone is coming to visit. nobody has come.\n\n"
                + "if you are reading this you are the someone. he waited. he waited the whole time.");
        text("ward_2", "(the last note)",
                "they moved me to the room at the end. the one with the lamp that doesn't work.\n\n"
                + "the orderly comes at night and pushes the empty bed past. up the corridor and back. up and back. "
                + "i asked who it was for. he said it's always for someone.\n\n"
                + "i built the backup so there'd be a world where i was still in the house. please stay in that one. "
                + "please don't come looking for me in here.\n\n- alex");
    }

    static {
        text("server_log", "latest.log (printed)",
                "[19:02:11] alex joined the game\n[19:02:40] <alex> testing\n[19:40:03] <alex> ok it works\n"
                + "[23:58:12] <alex> are you on\n[23:58:50] <alex> its fine\n[00:14:31] alex left the game\n"
                + "[00:14:31] saving world\n[00:14:32] saving world\n[00:14:32] saving world\n"
                + "[00:14:33] alex joined the game\n[00:14:33] alex joined the game\n[00:14:33] alex joined the game\n\n"
                + "(the rest of the sheet is that last line, over and over, until the printer ran out)");
        text("server_readme", "readme.txt",
                "if this box is still on, don't turn it off. the world only stays the way it is while something is running it.\n\n"
                + "i made a backup every night. i don't know which one is the real one anymore. i think the backups started "
                + "keeping themselves.\n\n"
                + "if you see me in here, don't walk toward me. it isn't me. it's the copy from before i knew. it doesn't know yet. "
                + "please don't be the one that tells it.\n\n- alex");
    }

    public static Page get(String id) {
        return PAGES.get(id);
    }
}
