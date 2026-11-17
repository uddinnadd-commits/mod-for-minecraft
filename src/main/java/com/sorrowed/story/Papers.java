package com.sorrowed.story;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The papers. Every note in the world is one wrinkled sheet: either a few paragraphs of his
 * handwriting or a pencil drawing. A sheet is identified by id; the item carries only the id.
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
                "If you're reading this, you got in, and I'm not here to say hi. I'm sorry about the rain. I didn't do that. "
                + "It started the day I stopped logging in, and it hasn't stopped since.\n\n"
                + "Everything looks older than it should. That's on purpose. I wanted you to see it the way we first did, "
                + "before the nice textures. Ugly, crooked, ours.");
        text("welcome_2", "for you (2)",
                "This was our world. The dirt house, the first night, the creeper you swore was a bush. I never deleted it. "
                + "I left it open. I told myself it was a backup.\n\n"
                + "Here's why I wanted you here. I never finished anything. Half-built houses, half-written messages. "
                + "You were always the one who dragged me to the end of a thing.");
        text("welcome_3", "for you (3)",
                "I can't finish this one. So I'm asking. Walk it once more.\n\n"
                + "You'll be taken somewhere dry. I built that place first, back when I thought there was time. It's empty. "
                + "Don't be scared of that. Empty isn't the same as abandoned.\n\n"
                + "The planks go out into nothing. Follow them. I was going somewhere with every one of them.");
        text("welcome_4", "for you (4)",
                "If someone is standing far off in the rain, don't run. It's only an echo of me, or what the world thinks I was. "
                + "It can't hurt you. It just doesn't know how to leave.\n\n"
                + "The ones with no faces are different. They're everything I never said. Don't let them hold you. Keep walking.\n\n"
                + "Thank you for coming back. Finish it, or don't. Just keep it a little loud in here for me.\n\n- {friend}");
        drawing("first_night", "(a drawing)", "first_night", "us. the first night. it was raining then too.");

        text("notes", "unfinished notes",
                "TODO\n- finish the roof\n- finish the door\n- finish the tower\n- tell them\n- tell them\n- tell them\n\n"
                + "The rain started when the server went quiet. I think this place only knows how to wait. It's learning how to miss people.\n\n"
                + "I'll leave the lantern on. In case you forget the way back.");
        text("last", "the last page",
                "I didn't get to finish. You did. That was the whole point.\n\nGo home. Leave the door open for whoever comes next.\n\n- {friend}");
        text("keeper", "the keeper's log",
                "The lamp has never been lit. I keep it dark on purpose. If I light it, someone will see this place from the sea, "
                + "and then they will come looking for what is in it.\n\n"
                + "Day 1. Day 2. Day 3. The rain has no day. I stopped counting. {friend} would have kept counting.\n\n"
                + "The ladder is the only way up. The only way down is the same ladder. I think that is the point.");
        text("names", "names",
                "{friend}\n(crossed out)\n(crossed out)\n(crossed out)\n\n"
                + "These are the people who stayed. Most of them did not stay long.\n\n"
                + "The last name on the page is yours. I wrote it in pencil, in case you wanted out.");
        text("ticket", "one way",
                "ONE WAY\n\nDeparts: never\nArrives: here\nPassenger: you\n\n"
                + "Please remain seated. The bus is coming. The bus has always been coming.");
        text("unsaid", "what he never said",
                "I'm sorry I stopped answering.\n\nI'm sorry I said I was fine.\n\nI'm sorry I made you carry the quiet.\n\n"
                + "I'm not sorry I let you in.\n\n(The rest of the page is blank. Maybe you can fill it in.)");
        text("reply", "the reply",
                "I knew you'd come. I set the table, just in case.\n\n"
                + "This place is only what I remember, so it is smaller than it should be. Nothing here can hurt you.\n\n"
                + "That is how you know it isn't me. I would have found some way to hurt you. I always did.\n\n- {friend}");

        drawing("archivist", "(a page)", "scribble", "");
        drawing("gate", "(a drawing)", "gate", "the planks. the gate at the end. he drew it before he built it.");
        text("neighbour", "(a note, folded twice)",
                "You're standing where he used to stand.\n\nThe light in that window is on a timer. Nobody set it. "
                + "Nobody turned it off.\n\nGo inside. You shouldn't be out in this.\n\n- a neighbour");
        drawing("street", "(a drawing)", "street", "his street. one window on. he said it was always on.");
    }

    public static Page get(String id) {
        return PAGES.get(id);
    }
}
